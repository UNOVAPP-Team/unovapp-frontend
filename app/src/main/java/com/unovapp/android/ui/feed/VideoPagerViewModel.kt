package com.unovapp.android.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.TokenDataStore
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.social.CommentBus
import com.unovapp.android.data.social.SocialRepository
import com.unovapp.android.data.user.FollowManager
import com.unovapp.android.data.user.FollowStore
import com.unovapp.android.data.user.SelfStatsStore
import com.unovapp.android.data.user.UserRepository
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.BandwidthMeter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lecture d'une liste vidéo isolée (grilles du profil : mes vidéos, aimées, sauvegardées).
 * Réutilise [FeedItem] + le lecteur unique partagé. Les actions like/save/share/report/suivi
 * sont identiques au feed mais opèrent sur la liste fournie via [setVideos].
 */
@HiltViewModel
class VideoPagerViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository,
    private val followManager: FollowManager,
    private val tokenStore: TokenDataStore,
    private val commentBus: CommentBus,
    private val selfStatsStore: SelfStatsStore,
    /** Fabrique cache-backed pour le pool de lecteurs (préfetch). */
    val mediaSourceFactory: MediaSource.Factory,
    /** Estimation de débit partagée + persistée entre sessions — passée au pool de lecteurs. */
    val bandwidthMeter: BandwidthMeter,
    followStore: FollowStore
) : ViewModel() {

    private val _videos = MutableStateFlow<List<FeedVideoUi>>(emptyList())
    val videos: StateFlow<List<FeedVideoUi>> = _videos.asStateFlow()

    val following: StateFlow<Set<String>> = followStore.following

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val creatorCache = mutableMapOf<String, Pair<String, String?>>()

    init {
        viewModelScope.launch { _currentUserId.value = tokenStore.readUserId() }
        // Compteur de commentaires synchro dans la grille/lecture profil aussi.
        viewModelScope.launch {
            commentBus.delta.collect { (videoId, delta) ->
                update(videoId) {
                    val c = (it.commentsCount + delta).coerceAtLeast(0)
                    it.copy(commentsCount = c, commentsFmt = formatFeedCount(c))
                }
            }
        }
    }

    /**
     * Amorce la liste puis résout pseudo/avatar des créateurs manquants. Ce VM peut être
     * retenu (scope Activity) : on ré-amorce donc si une liste différente est fournie, mais on
     * conserve l'état optimiste (likes/saves) quand la même liste est ré-ouverte.
     */
    fun setVideos(list: List<FeedVideoUi>) {
        val sameIds = _videos.value.map { it.id } == list.map { it.id }
        if (_videos.value.isNotEmpty() && sameIds) return
        _videos.value = list
        resolveCreators()
    }

    private fun resolveCreators() {
        _videos.value.map { it.creatorId }.filter { it.isNotBlank() }.distinct().forEach { id ->
            creatorCache[id]?.let { (u, a) -> applyCreator(id, u, a); return@forEach }
            viewModelScope.launch {
                when (val r = userRepository.getUser(id)) {
                    is NetworkResult.Success -> {
                        creatorCache[id] = r.data.username to r.data.avatarUrl
                        applyCreator(id, r.data.username, r.data.avatarUrl)
                    }
                    is NetworkResult.Failure -> Unit
                }
            }
        }
    }

    /** N'écrase que les pseudos encore au repli (id.take(8)) → mes vidéos gardent le mien. */
    private fun applyCreator(id: String, username: String, avatarUrl: String?) {
        _videos.update { list ->
            list.map {
                if (it.creatorId == id && it.creatorUsername == id.take(8))
                    it.copy(creatorUsername = username, avatarUrl = avatarUrl)
                else it
            }
        }
    }

    fun follow(creatorId: String) { if (creatorId.isNotBlank()) followManager.setFollowing(creatorId, true) }

    fun toggleLike(videoId: String) {
        val idx = _videos.value.indexOfFirst { it.id == videoId }
        if (idx == -1) return
        val before = _videos.value[idx]
        val liked = !before.isLiked
        val count = (before.likesCount + if (liked) 1 else -1).coerceAtLeast(0)
        update(videoId) { it.copy(isLiked = liked, likesCount = count, likesFmt = formatFeedCount(count)) }

        // Liker MA propre vidéo (grille profil) → +1 sur « J'aime reçus » du profil, en direct.
        val isMine = before.creatorId.isNotBlank() && before.creatorId == _currentUserId.value
        if (isMine) selfStatsStore.addLikeReceived(if (liked) 1 else -1)

        viewModelScope.launch {
            when (val r = socialRepository.like(videoId)) {
                is NetworkResult.Success -> update(videoId) {
                    it.copy(isLiked = r.data.liked, likesCount = r.data.likesCount, likesFmt = formatFeedCount(r.data.likesCount))
                }
                is NetworkResult.Failure -> {
                    update(videoId) {
                        it.copy(isLiked = before.isLiked, likesCount = before.likesCount, likesFmt = before.likesFmt)
                    }
                    if (isMine) selfStatsStore.addLikeReceived(if (liked) -1 else 1)
                }
            }
        }
    }

    fun toggleSave(videoId: String) {
        val idx = _videos.value.indexOfFirst { it.id == videoId }
        if (idx == -1) return
        val before = _videos.value[idx].isSaved
        update(videoId) { it.copy(isSaved = !it.isSaved) }
        viewModelScope.launch {
            when (val r = socialRepository.save(videoId)) {
                is NetworkResult.Success -> update(videoId) { it.copy(isSaved = r.data.saved) }
                is NetworkResult.Failure -> update(videoId) { it.copy(isSaved = before) }
            }
        }
    }

    fun share(videoId: String) {
        viewModelScope.launch {
            when (val r = socialRepository.share(videoId)) {
                is NetworkResult.Success -> update(videoId) {
                    it.copy(sharesCount = r.data.sharesCount, sharesFmt = formatFeedCount(r.data.sharesCount))
                }
                is NetworkResult.Failure -> Unit
            }
        }
    }

    fun reportVideo(videoId: String, reason: String) {
        viewModelScope.launch { socialRepository.reportVideo(videoId, reason) }
    }

    fun addGift(videoId: String, count: Int = 1) {
        update(videoId) { val t = it.giftsCount + count; it.copy(giftsCount = t, giftsFmt = formatFeedCount(t)) }
    }

    fun recordView(videoId: String) {
        if (videoId.length < 10) return
        viewModelScope.launch { socialRepository.view(videoId) }
    }

    private inline fun update(videoId: String, crossinline transform: (FeedVideoUi) -> FeedVideoUi) {
        _videos.update { list -> list.map { if (it.id == videoId) transform(it) else it } }
    }
}
