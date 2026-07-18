package com.unovapp.android.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.BuildConfig
import com.unovapp.android.TokenDataStore
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.social.CommentBus
import com.unovapp.android.data.social.SocialRepository
import com.unovapp.android.data.user.FollowManager
import com.unovapp.android.data.user.FollowStore
import com.unovapp.android.data.user.SelfStatsStore
import com.unovapp.android.data.user.UserRepository
import com.unovapp.android.data.user.UserProfileStore
import com.unovapp.android.data.video.FeedDiskCache
import com.unovapp.android.data.video.FeedRefreshBus
import com.unovapp.android.data.video.FeedVideoDto
import com.unovapp.android.data.video.VideoRepository
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.BandwidthMeter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

data class FeedState(
    val videos: List<FeedVideoUi>  = emptyList(),
    val isLoading: Boolean         = true,
    val isLoadingMore: Boolean     = false,
    val nextCursor: String?        = null,
    val hasMore: Boolean           = true,
    val currentPage: Int           = 0,
    val currentVideoId: String?    = null
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository,
    private val followManager: FollowManager,
    private val tokenStore: TokenDataStore,
    private val followStore: FollowStore,
    private val profileStore: UserProfileStore,
    private val commentBus: CommentBus,
    private val selfStatsStore: SelfStatsStore,
    /** Fabrique de sources média cache-backed (préfetch disque) — passée au pool de lecteurs. */
    val mediaSourceFactory: MediaSource.Factory,
    /** Estimation de débit partagée + persistée entre sessions — passée au pool de lecteurs. */
    val bandwidthMeter: BandwidthMeter,
    private val feedDiskCache: FeedDiskCache,
    /** Pré-téléchargement des 1ʳᵉˢ secondes de vidéos dans le cache → démarrage instantané. */
    private val prefetcher: com.unovapp.android.data.video.VideoPrefetcher,
    feedRefreshBus: FeedRefreshBus
) : ViewModel() {

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    /** Ensemble des créateurs suivis (partagé avec profil/recherche). Persiste durant la session. */
    val following: StateFlow<Set<String>> = followStore.following

    /** UUID de l'utilisateur connecté → permet de masquer « Suivre » sur ses propres vidéos. */
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    /** Cache des profils créateurs résolus (id → pseudo + avatar_url). */
    private val creatorCache = mutableMapOf<String, Pair<String, String?>>()

    /** Créateurs dont l'état de SUIVI a déjà été confirmé par le backend cette session. */
    private val followSynced = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            val uid = tokenStore.readUserId()
            _currentUserId.value = uid
            // Hydrate la liste de suivis persistée de CE compte → boutons corrects dès le
            // démarrage, même si l'enrichment du feed est absent (token expiré, JWT optionnel).
            followStore.onSession(uid)
        }
        viewModelScope.launch {
            profileStore.profiles.collect { profiles ->
                profiles.values.forEach { profile ->
                    creatorCache[profile.id] = profile.username to profile.avatarUrl
                    applyCreator(profile.id, profile.username, profile.avatarUrl)
                    // Le profil visité porte l'état de suivi → on le verse dans le store au
                    // lieu de le jeter (avant : seuls pseudo/avatar étaient récupérés).
                    if (profile.isFollowing) followStore.merge(listOf(profile.id))
                }
            }
        }
        // Démarrage instantané à froid : affiche le dernier feed connu pendant que le réseau
        // répond (~1 s d'API + Supabase). Les premières secondes de ces vidéos sont souvent
        // encore dans le cache disque ExoPlayer → la 1ʳᵉ vidéo démarre sans réseau.
        // Dès que le feed frais arrive, loadFeed() remplace la liste (position préservée par id).
        viewModelScope.launch {
            val cached = feedDiskCache.load()
            if (cached.isNotEmpty()) {
                // Le cache disque vient d'une session où le token était valide → son
                // enrichment de suivi est fiable, on le récupère aussi.
                mergeFollowEnrichment(cached)
                val ordered = orderWarmFirst(cached.shuffled())
                _state.update { s ->
                    if (s.videos.isEmpty()) {
                        // Mélangé + vidéos réchauffées en tête : le feed restauré démarre
                        // instantanément (1ʳᵉˢ secondes déjà en cache disque) et sans répéter
                        // le même ordre à chaque ouverture.
                        s.copy(isLoading = false, videos = ordered.map { dto -> dto.toUi() })
                    } else s // le réseau a déjà répondu → il fait foi
                }
                resolveCreators()
                triggerPrefetch(ordered)
            }
        }
        loadFeed()
        // Recharge le feed dès qu'une nouvelle vidéo est publiée → elle apparaît en tête.
        viewModelScope.launch {
            feedRefreshBus.events.collect { loadFeed() }
        }
        // Compteur de commentaires : +1 sur la vidéo dès qu'un commentaire est posté (depuis la
        // feuille de commentaires) → le rail du feed reste synchro sans rechargement.
        viewModelScope.launch {
            commentBus.delta.collect { (videoId, delta) ->
                updateVideo(videoId) {
                    val c = (it.commentsCount + delta).coerceAtLeast(0)
                    it.copy(commentsCount = c, commentsFmt = formatCount(c))
                }
            }
        }
    }

    /**
     * Résout pseudo + photo de profil des créateurs (le `/feed` ne les renvoie pas) via
     * `GET /users/:id`, avec cache. Met à jour les items concernés dès qu'un profil arrive.
     */
    private fun resolveCreators() {
        val ids = _state.value.videos
            .map { it.creatorId }
            .filter { it.isNotBlank() }
            .distinct()
        ids.forEach { id ->
            // Identité (pseudo/avatar) depuis le cache si connue — affichage immédiat.
            creatorCache[id]?.let { (uname, avatar) -> applyCreator(id, uname, avatar) }
            // ⚠️ Le cache d'identité ne dispense PAS de synchroniser le SUIVI : avant, un
            // créateur présent dans le cache (ex. via un profil visité) court-circuitait le
            // GET /users/:id → son état de suivi n'était JAMAIS vérifié → bouton « Suivre »
            // affiché à tort. On ne saute l'appel que si le suivi a déjà été confirmé.
            if (creatorCache.containsKey(id) && id in followSynced) return@forEach
            viewModelScope.launch {
                when (val r = userRepository.getUser(id)) {
                    is NetworkResult.Success -> {
                        followSynced += id
                        val uname = r.data.username
                        val avatar = r.data.avatarUrl
                        creatorCache[id] = uname to avatar
                        applyCreator(id, uname, avatar)
                        // Synchronise le suivi depuis le backend → bouton « Suivre » correct au démarrage.
                        if (r.data.isFollowing) followStore.merge(listOf(id))
                    }
                    is NetworkResult.Failure -> Unit
                }
            }
        }
    }

    private fun applyCreator(id: String, username: String, avatarUrl: String?) {
        _state.update { s ->
            s.copy(videos = s.videos.map {
                if (it.creatorId == id) it.copy(creatorUsername = username, avatarUrl = avatarUrl) else it
            })
        }
    }

    /**
     * Suivre un créateur depuis le feed. Passe par le [FollowManager] partagé :
     *  - met à jour le store immédiatement (le bouton reste « suivi » au scroll),
     *  - appelle le backend (POST /users/{id}/follow, idempotent), rollback si échec,
     *  - incrémente le compteur « Suivis » du profil via le delta partagé.
     */
    fun follow(creatorId: String) {
        if (creatorId.isBlank()) return // vidéos mock : pas d'appel réseau
        followManager.setFollowing(creatorId, true)
    }

    fun rememberCurrentPage(page: Int, videoId: String?) {
        _state.update { it.copy(currentPage = page.coerceAtLeast(0), currentVideoId = videoId) }
    }

    /**
     * Met en tête les vidéos DÉJÀ pré-téléchargées (démarrage instantané garanti), le reste
     * derrière — chaque groupe conservant l'ordre reçu (déjà mélangé). Ainsi l'utilisateur
     * tombe d'abord sur du contenu qui démarre sans attendre.
     */
    private fun orderWarmFirst(dtos: List<FeedVideoDto>): List<FeedVideoDto> {
        if (dtos.isEmpty()) return dtos
        val warm = prefetcher.warmIds()
        if (warm.isEmpty()) return dtos
        val (ready, rest) = dtos.partition { it.id in warm }
        return ready + rest
    }

    /**
     * Verse l'enrichment de suivi du feed (`is_following_creator`) dans le [FollowStore] —
     * source de vérité UNIQUE des boutons. Positif seulement : un `false` peut venir d'une
     * réponse non authentifiée (token expiré sur endpoint à JWT optionnel), il ne doit jamais
     * écraser un suivi connu.
     */
    private fun mergeFollowEnrichment(dtos: List<FeedVideoDto>) {
        followStore.merge(dtos.filter { it.isFollowingCreator }.map { it.creatorId })
    }

    /** Déclenche un préfetch (au plus 2) sur les vidéos pas encore réchauffées — fire-and-forget. */
    private fun triggerPrefetch(dtos: List<FeedVideoDto>) {
        val targets = dtos.mapNotNull { dto ->
            dto.lowestRenditionUrl()?.let { url ->
                com.unovapp.android.data.video.PrefetchTarget(dto.id, url)
            }
        }
        if (targets.isEmpty()) return
        viewModelScope.launch { runCatching { prefetcher.prefetch(targets, max = 2) } }
    }

    fun loadFeed() {
        viewModelScope.launch {
            val rememberedVideoId = _state.value.currentVideoId
            _state.update { it.copy(isLoading = true) }
            when (val r = videoRepository.feed()) {
                is NetworkResult.Success -> {
                    // Ordre ALÉATOIRE (mesure d'attente avant l'algo backend) puis les vidéos
                    // DÉJÀ pré-téléchargées remontées en tête → l'utilisateur voit d'abord du
                    // contenu qui démarre instantanément.
                    mergeFollowEnrichment(r.data.data)
                    val ordered = orderWarmFirst(r.data.data.shuffled())
                    _state.update { it.copy(
                        isLoading    = false,
                        videos       = ordered.map { dto -> dto.toUi() },
                        nextCursor   = r.data.nextCursor,
                        hasMore      = r.data.hasMore,
                        currentPage  = ordered.indexOfFirst { it.id == rememberedVideoId }.takeIf { it >= 0 }
                            ?: it.currentPage.coerceAtMost((ordered.size - 1).coerceAtLeast(0))
                    )}
                    // Persiste la 1ʳᵉ page → démarrage instantané à la prochaine ouverture.
                    feedDiskCache.save(ordered)
                    resolveCreators()
                    // ...et pendant qu'il regarde, on réchauffe 2 vidéos de plus.
                    triggerPrefetch(ordered)
                }
                // Échec réseau : on garde ce qui est affiché (souvent le cache disque).
                // Si la liste est vide, FeedScreen montre l'état erreur + « Réessayer » —
                // plus de repli sur des vidéos mockées (trompeur + bande passante gaspillée).
                is NetworkResult.Failure -> _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (!s.hasMore || s.isLoadingMore || s.isLoading) return
        val cursor = s.nextCursor ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            when (val r = videoRepository.feed(cursor = cursor)) {
                is NetworkResult.Success -> {
                    mergeFollowEnrichment(r.data.data)
                    // Mélange la page suivante ET écarte les doublons : le backend paginant
                    // sur une liste non personnalisée, une même vidéo peut revenir.
                    val known = _state.value.videos.mapTo(HashSet()) { it.id }
                    val freshDtos = r.data.data.filter { it.id !in known }.shuffled()
                    _state.update { st ->
                        st.copy(
                            isLoadingMore = false,
                            videos        = st.videos + freshDtos.map { dto -> dto.toUi() },
                            nextCursor    = r.data.nextCursor,
                            hasMore       = r.data.hasMore
                        )
                    }
                    resolveCreators()
                    // Réchauffe aussi quelques vidéos de la nouvelle page.
                    triggerPrefetch(freshDtos)
                }
                is NetworkResult.Failure -> _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    /**
     * Toggle like optimiste : on bascule l'état + le compteur immédiatement dans l'UI, on
     * appelle l'API (POST /videos/:id/like = toggle serveur), puis on réconcilie avec le
     * compteur autoritatif renvoyé. En cas d'échec → rollback.
     */
    fun toggleLike(videoId: String) {
        val current = _state.value.videos
        val idx = current.indexOfFirst { it.id == videoId }
        if (idx == -1) return // vidéo mock / absente du state → pas d'appel réseau

        val before = current[idx]
        val optimisticLiked = !before.isLiked
        val optimisticCount = (before.likesCount + if (optimisticLiked) 1 else -1).coerceAtLeast(0)
        updateVideo(videoId) { it.copy(isLiked = optimisticLiked, likesCount = optimisticCount, likesFmt = formatCount(optimisticCount)) }

        // Si je like/délike MA propre vidéo → répercute sur « J'aime reçus » du profil, en direct.
        val isMine = before.creatorId.isNotBlank() && before.creatorId == _currentUserId.value
        if (isMine) selfStatsStore.addLikeReceived(if (optimisticLiked) 1 else -1)

        viewModelScope.launch {
            when (val r = socialRepository.like(videoId)) {
                is NetworkResult.Success -> {
                    // Réconcilie avec le serveur (compteur + état liked autoritatifs).
                    val serverCount = r.data.likesCount
                    updateVideo(videoId) {
                        it.copy(isLiked = r.data.liked, likesCount = serverCount, likesFmt = formatCount(serverCount))
                    }
                }
                is NetworkResult.Failure -> {
                    // Rollback de l'état optimiste.
                    updateVideo(videoId) {
                        it.copy(isLiked = before.isLiked, likesCount = before.likesCount, likesFmt = before.likesFmt)
                    }
                    if (isMine) selfStatsStore.addLikeReceived(if (optimisticLiked) -1 else 1)
                }
            }
        }
    }

    /** Sauvegarder / retirer des favoris (optimiste + réconciliation backend). */
    fun toggleSave(videoId: String) {
        val idx = _state.value.videos.indexOfFirst { it.id == videoId }
        if (idx == -1) return
        val before = _state.value.videos[idx].isSaved
        updateVideo(videoId) { it.copy(isSaved = !it.isSaved) }
        viewModelScope.launch {
            when (val r = socialRepository.save(videoId)) {
                is NetworkResult.Success -> updateVideo(videoId) { it.copy(isSaved = r.data.saved) }
                is NetworkResult.Failure -> updateVideo(videoId) { it.copy(isSaved = before) }
            }
        }
    }

    /** Signale une vidéo (reason ∈ spam|violence|nudity|harassment|other). */
    fun reportVideo(videoId: String, reason: String) {
        viewModelScope.launch { socialRepository.reportVideo(videoId, reason) }
    }

    /** Enregistre un partage côté backend (compteur réel) + met à jour l'UI. */
    fun share(videoId: String) {
        viewModelScope.launch {
            when (val r = socialRepository.share(videoId)) {
                is NetworkResult.Success -> updateVideo(videoId) {
                    it.copy(sharesCount = r.data.sharesCount, sharesFmt = formatCount(r.data.sharesCount))
                }
                is NetworkResult.Failure -> Unit
            }
        }
    }

    private inline fun updateVideo(videoId: String, crossinline transform: (FeedVideoUi) -> FeedVideoUi) {
        _state.update { s ->
            s.copy(videos = s.videos.map { if (it.id == videoId) transform(it) else it })
        }
    }

    /**
     * Incrémente le compteur de cadeaux d'une vidéo (optimiste, local). Le backend n'expose
     * pas encore d'endpoint cadeau → le compteur n'est pas persisté ni partagé pour l'instant.
     */
    fun addGift(videoId: String, count: Int = 1) {
        updateVideo(videoId) {
            val total = it.giftsCount + count
            it.copy(giftsCount = total, giftsFmt = formatCount(total))
        }
    }

    /** Enregistre une vue (fire-and-forget). */
    fun recordView(videoId: String) {
        // N'appelle pas l'API pour les IDs mock (format "v1".."v5")
        if (videoId.length < 10) return
        viewModelScope.launch { socialRepository.view(videoId) }
    }

    // ──────────────────────────────────────────────
    // Mapping DTO → UI
    // ──────────────────────────────────────────────

    private fun FeedVideoDto.toUi(): FeedVideoUi {
        // Pseudo + photo depuis le cache si déjà résolu (sinon resolveCreators() les remplira).
        val cached = creatorCache[creatorId]
        return FeedVideoUi(
            id                = id,
            creatorId         = creatorId,
            hlsUrl            = playbackUrl(BuildConfig.VIDEO_BASE_URL),
            downloadUrl       = bestStreamUrl(BuildConfig.VIDEO_BASE_URL),
            durationSec       = durationSeconds,
            creatorUsername   = cached?.first ?: creatorId.take(8),
            creatorAvatarIdx  = abs(creatorId.hashCode()) % 5,
            avatarUrl         = cached?.second,
            thumbnailUrl      = thumbnailUrl,
            description       = description ?: "",
            likesCount        = likesCount,
            likesFmt          = formatCount(likesCount),
            commentsCount     = commentsCount,
            commentsFmt       = formatCount(commentsCount),
            sharesFmt         = formatCount(sharesCount),
            sharesCount       = sharesCount,
            viewsCount        = viewsCount,
            viewsFmt          = formatCount(viewsCount),
            isFollowing       = isFollowingCreator,
            isLiked           = isLiked,
            isSaved           = isSaved,
            hashtags          = hashtags,
            visibility        = visibility,
            giftsFmt          = "0",
            createdAt         = createdAt
        )
    }

    private fun formatCount(n: Int): String = when {
        n >= 1_000_000 -> String.format("%.1f M", n / 1_000_000f).replace('.', ',')
        n >= 1_000     -> String.format("%.1f K", n / 1_000f).replace('.', ',')
        else           -> n.toString()
    }
}
