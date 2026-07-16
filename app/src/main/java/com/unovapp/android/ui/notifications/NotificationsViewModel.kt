package com.unovapp.android.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.notification.NotificationItemDto
import com.unovapp.android.data.notification.NotificationRepository
import com.unovapp.android.ui.feed.toFeedVideoUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsState(
    val items: List<NotificationItemDto> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val unreadCount: Int = 0,
    val error: String? = null,
    /** Vidéo ouverte depuis une notification (tap) — lecture plein écran. */
    val openedVideo: com.unovapp.android.ui.feed.FeedVideoUi? = null,
    val openingVideoId: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val videoRepository: com.unovapp.android.data.video.VideoRepository,
    private val tokenStore: com.unovapp.android.TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init { load() }

    /** Écarte mes propres activités ("monPseudo a aimé ta vidéo") — le backend les crée
     *  aussi pour l'auteur ; tant qu'il n'expose pas actor_id, on filtre par le titre. */
    private fun List<NotificationItemDto>.withoutSelf(): List<NotificationItemDto> {
        val me = tokenStore.readUsernameSync() ?: return this
        return filterNot { it.title.startsWith("$me ", ignoreCase = true) }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val r = repository.list()) {
                is NetworkResult.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        items = r.data.data.withoutSelf(),
                        nextCursor = r.data.nextCursor,
                        hasMore = r.data.hasMore,
                        unreadCount = r.data.unreadCount
                    )
                }
                is NetworkResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = r.error.userMessage)
                }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (!s.hasMore || s.isLoadingMore || s.nextCursor.isNullOrBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            when (val r = repository.list(s.nextCursor)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(
                        isLoadingMore = false,
                        items = it.items + r.data.data.withoutSelf(),
                        nextCursor = r.data.nextCursor,
                        hasMore = r.data.hasMore
                    )
                }
                is NetworkResult.Failure -> _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    /**
     * Tap sur une notification liée à une vidéo → charge la vidéo (video_id du payload)
     * et l'ouvre en lecture plein écran. Le créateur voit ainsi DIRECTEMENT laquelle de
     * ses vidéos a reçu la réaction/le commentaire.
     */
    fun openVideo(videoId: String) {
        if (_state.value.openingVideoId != null) return
        viewModelScope.launch {
            _state.update { it.copy(openingVideoId = videoId) }
            when (val r = videoRepository.getVideo(videoId)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(
                        openingVideoId = null,
                        openedVideo = r.data.toFeedVideoUi(com.unovapp.android.BuildConfig.VIDEO_BASE_URL)
                    )
                }
                is NetworkResult.Failure -> _state.update { it.copy(openingVideoId = null) }
            }
        }
    }

    fun closeVideo() {
        _state.update { it.copy(openedVideo = null) }
    }

    /** Marque une notification comme lue (optimiste). */
    fun markRead(id: String) {
        val alreadyRead = _state.value.items.firstOrNull { it.id == id }?.isRead ?: true
        if (alreadyRead) return
        _state.update { s ->
            s.copy(
                items = s.items.map { if (it.id == id) it.copy(isRead = true) else it },
                unreadCount = (s.unreadCount - 1).coerceAtLeast(0)
            )
        }
        viewModelScope.launch { repository.markRead(id) }
    }

    fun markAllRead() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(isRead = true) }, unreadCount = 0) }
        viewModelScope.launch { repository.markAllRead() }
    }
}
