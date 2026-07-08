package com.unovapp.android.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.BuildConfig
import com.unovapp.android.data.auth.AuthRepository
import com.unovapp.android.data.network.ApiError
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.user.FollowStore
import com.unovapp.android.data.user.SelfStatsStore
import com.unovapp.android.data.user.UserProfileDto
import com.unovapp.android.data.user.UserRepository
import com.unovapp.android.data.video.FeedRefreshBus
import com.unovapp.android.data.video.FeedResponse
import com.unovapp.android.ui.feed.FeedVideoUi
import com.unovapp.android.ui.feed.toFeedVideoUi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

data class AvatarUploadUiState(
    val isUploading: Boolean = false,
    val error: String? = null
)

/** État réseau du profil connecté (issu de `GET /users/me`). */
data class ProfileNetworkState(
    val isLoading: Boolean = true,
    val profile: UserProfileDto? = null,
    val error: String? = null,
    /** Session invalide/expirée (401) → l'UI doit rediriger vers la connexion. */
    val sessionExpired: Boolean = false,
    // Édition du profil
    val saving: Boolean = false,
    val saveError: String? = null
)

/** Onglets de la grille du profil (adossés au backend). */
enum class ProfileVideoTab { Videos, Liked, Saved }

/** Vidéos du profil : mes vidéos, aimées, sauvegardées (chargées à la demande). */
data class ProfileVideosState(
    val tab: ProfileVideoTab = ProfileVideoTab.Videos,
    val videos: List<FeedVideoUi> = emptyList(),
    val liked: List<FeedVideoUi> = emptyList(),
    val saved: List<FeedVideoUi> = emptyList(),
    val loading: Boolean = false
) {
    /** Liste affichée pour l'onglet courant. */
    val current: List<FeedVideoUi>
        get() = when (tab) {
            ProfileVideoTab.Videos -> videos
            ProfileVideoTab.Liked -> liked
            ProfileVideoTab.Saved -> saved
        }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val followStore: FollowStore,
    private val selfStatsStore: SelfStatsStore,
    feedRefreshBus: FeedRefreshBus
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileNetworkState())
    val state: StateFlow<ProfileNetworkState> = _state.asStateFlow()

    private val _videos = MutableStateFlow(ProfileVideosState())
    val videos: StateFlow<ProfileVideosState> = _videos.asStateFlow()

    private val _changePwState = MutableStateFlow(ChangePasswordUiState())
    val changePwState: StateFlow<ChangePasswordUiState> = _changePwState.asStateFlow()

    private val _avatarState = MutableStateFlow(AvatarUploadUiState())
    val avatarState: StateFlow<AvatarUploadUiState> = _avatarState.asStateFlow()

    /**
     * Variation du nombre d'abonnements depuis le dernier `/users/me` — permet d'incrémenter
     * « Suivis » instantanément quand on suit quelqu'un (recherche, autre profil…) sans recharger.
     */
    val followingDelta: StateFlow<Int> = followStore.followingDelta

    /** Delta local de « J'aime reçus » (like sur ses propres vidéos) → stat profil en direct. */
    val likesReceivedDelta: StateFlow<Int> = selfStatsStore.likesReceivedDelta

    init {
        load()
        // Nouvelle vidéo publiée → on rafraîchit stats + grille « Vidéos » (nouvelle vidéo en tête)
        // en temps réel, sans relancer l'app.
        viewModelScope.launch {
            feedRefreshBus.events.collect { refreshAfterPublish() }
        }
    }

    /** Recharge le profil (compteurs) + la grille de mes vidéos après une publication. */
    private fun refreshAfterPublish() {
        viewModelScope.launch {
            when (val r = userRepository.fetchMe()) {
                is NetworkResult.Success -> _state.update { it.copy(profile = r.data) }
                is NetworkResult.Failure -> Unit
            }
            loadMyVideos()
        }
    }

    /** Recharge le profil complet sans spinner (stats autoritatives) — ex. après upload avatar. */
    private fun refreshProfileSilently() {
        viewModelScope.launch {
            when (val r = userRepository.fetchMe()) {
                is NetworkResult.Success -> _state.update { it.copy(profile = r.data) }
                is NetworkResult.Failure -> Unit
            }
        }
    }

    fun load() {
        viewModelScope.launch { doLoad(attempt = 0) }
    }

    private suspend fun doLoad(attempt: Int) {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val r = userRepository.fetchMe()) {
            is NetworkResult.Success -> {
                followStore.resetDelta()
                selfStatsStore.reset()   // les likes reçus autoritatifs sont dans r.data
                _state.update { it.copy(isLoading = false, profile = r.data, error = null) }
                loadMyVideos()
            }
            is NetworkResult.Failure -> when (r.error) {
                is ApiError.Unauthorized -> {
                    // TokenAuthenticator a déjà tenté le refresh :
                    //   • AuthInvalid (400-403 du backend) → tokens purgés → tokenGone = true → login.
                    //   • NetworkError (timeout/503 cold-start) → tokens gardés → on réessaie.
                    val tokenGone = !authRepository.isAuthenticated().first()
                    when {
                        tokenGone -> _state.update { it.copy(isLoading = false, sessionExpired = true) }
                        attempt < MAX_RETRY -> {
                            // isLoading reste true : l'UI affiche le spinner, pas d'erreur.
                            delay(retryDelay(attempt))
                            doLoad(attempt + 1)
                        }
                        else -> _state.update {
                            it.copy(isLoading = false, error = "Service temporairement indisponible. Réessayez.")
                        }
                    }
                }
                is ApiError.Timeout, is ApiError.Network, is ApiError.Server -> {
                    // 429 rate-limit, cold-start Render.com (503) ou réseau faible → on réessaie.
                    if (attempt < MAX_RETRY) {
                        val isRateLimit = (r.error as? ApiError.Server)?.httpStatus == 429
                        delay(if (isRateLimit) retryDelayRateLimit(attempt) else retryDelay(attempt))
                        doLoad(attempt + 1)
                    } else {
                        _state.update {
                            it.copy(isLoading = false, error = "Service temporairement indisponible. Réessayez.")
                        }
                    }
                }
                else -> _state.update { it.copy(isLoading = false, error = r.error.debugDetail) }
            }
        }
    }

    /** Cold-start Render.com : 10 s puis 25 s entre les tentatives. */
    private fun retryDelay(attempt: Int): Long = when (attempt) {
        0 -> 10_000L
        else -> 25_000L
    }

    /** Rate-limit (429) : backoff plus court — 5 s puis 15 s. */
    private fun retryDelayRateLimit(attempt: Int): Long = when (attempt) {
        0 -> 5_000L
        else -> 15_000L
    }

    companion object {
        private const val MAX_RETRY = 2  // 3 tentatives au total : attempt 0, 1, 2
    }

    /** Met à jour le profil (display_name, bio, username) via PATCH /users/{id}. */
    fun updateProfile(displayName: String, bio: String, username: String, onSaved: () -> Unit) {
        val id = _state.value.profile?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, saveError = null) }
            when (val r = userRepository.updateProfile(id, displayName, bio, username)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(saving = false, profile = r.data, saveError = null) }
                    onSaved()
                }
                is NetworkResult.Failure ->
                    _state.update { it.copy(saving = false, saveError = r.error.debugDetail) }
            }
        }
    }

    fun clearSaveError() = _state.update { it.copy(saveError = null) }

    /* ---------- Changement de mot de passe ---------- */

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _changePwState.update { it.copy(isLoading = true, error = null, success = false) }
            when (val r = authRepository.changePassword(currentPassword, newPassword, confirmPassword)) {
                is NetworkResult.Success -> _changePwState.update { it.copy(isLoading = false, success = true) }
                is NetworkResult.Failure -> _changePwState.update {
                    it.copy(isLoading = false, error = r.error.userMessage)
                }
            }
        }
    }

    fun clearChangePwState() = _changePwState.update { ChangePasswordUiState() }

    /* ---------- Upload avatar ---------- */

    fun uploadAvatar(contentType: String, uri: Uri) {
        viewModelScope.launch {
            _avatarState.update { it.copy(isUploading = true, error = null) }
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            if (bytes == null) {
                _avatarState.update { it.copy(isUploading = false, error = "Impossible de lire l'image.") }
                return@launch
            }
            when (val r = userRepository.uploadAvatar(contentType, bytes)) {
                is NetworkResult.Success -> {
                    // 1) MAJ immédiate de la photo (retour visuel instantané) sans toucher aux stats.
                    _state.update { st ->
                        val merged = st.profile?.copy(avatarUrl = r.data.avatarUrl) ?: r.data
                        st.copy(profile = merged)
                    }
                    _avatarState.update { it.copy(isUploading = false) }
                    // 2) Recharge le profil COMPLET en silence → stats autoritatives garanties, sans
                    //    devoir fermer/rouvrir l'app (la réponse d'upload ne renvoie pas les compteurs).
                    refreshProfileSilently()
                }
                is NetworkResult.Failure -> _avatarState.update {
                    it.copy(isUploading = false, error = r.error.userMessage)
                }
            }
        }
    }

    fun clearAvatarError() = _avatarState.update { it.copy(error = null) }

    /** Déconnexion : efface la session (best-effort backend) puis notifie l'UI. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }

    /* ---------- Grille vidéos du profil (mes vidéos / aimées / sauvegardées) ---------- */

    /** Sélectionne un onglet et charge sa liste à la demande (une seule fois). */
    fun selectTab(tab: ProfileVideoTab) {
        _videos.update { it.copy(tab = tab) }
        when (tab) {
            ProfileVideoTab.Videos -> if (_videos.value.videos.isEmpty()) loadMyVideos()
            ProfileVideoTab.Liked -> if (_videos.value.liked.isEmpty()) loadLiked()
            ProfileVideoTab.Saved -> if (_videos.value.saved.isEmpty()) loadSaved()
        }
    }

    private fun loadMyVideos() {
        viewModelScope.launch {
            _videos.update { it.copy(loading = true) }
            val list = mapVideos(userRepository.userVideos("me"))
            _videos.update { it.copy(loading = false, videos = list) }
        }
    }

    private fun loadLiked() {
        viewModelScope.launch {
            _videos.update { it.copy(loading = true) }
            val list = mapVideos(userRepository.likedVideos())
            _videos.update { it.copy(loading = false, liked = list) }
        }
    }

    private fun loadSaved() {
        viewModelScope.launch {
            _videos.update { it.copy(loading = true) }
            val list = mapVideos(userRepository.savedVideos())
            _videos.update { it.copy(loading = false, saved = list) }
        }
    }

    /** Mappe une page feed → UI. Mes propres vidéos héritent de mon pseudo/avatar. */
    private fun mapVideos(r: NetworkResult<FeedResponse>): List<FeedVideoUi> = when (r) {
        is NetworkResult.Success -> {
            val me = _state.value.profile
            r.data.data.map { dto ->
                if (me != null && dto.creatorId == me.id)
                    dto.toFeedVideoUi(BuildConfig.VIDEO_BASE_URL, me.username, me.avatarUrl)
                else
                    dto.toFeedVideoUi(BuildConfig.VIDEO_BASE_URL)
            }
        }
        is NetworkResult.Failure -> emptyList()
    }
}
