package com.unovapp.android.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.social.CommentBus
import com.unovapp.android.data.social.CommentDto
import com.unovapp.android.data.social.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class CommentsState(
    val comments: List<CommentUi> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val commentBus: CommentBus
) : ViewModel() {

    private var loadedForVideoId: String = ""

    private val _state = MutableStateFlow(CommentsState())
    val state: StateFlow<CommentsState> = _state.asStateFlow()

    fun load(videoId: String) {
        if (videoId.isBlank() || videoId == loadedForVideoId) return
        loadedForVideoId = videoId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, comments = emptyList(), nextCursor = null, hasMore = false) }
            when (val r = socialRepository.comments(videoId)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        comments = r.data.data.map { dto -> dto.toUi() },
                        nextCursor = r.data.nextCursor,
                        hasMore = r.data.hasMore
                    )
                }
                is NetworkResult.Failure -> _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun send(videoId: String, content: String) {
        if (content.isBlank() || videoId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            when (val r = socialRepository.postComment(videoId, content)) {
                is NetworkResult.Success -> {
                    // Un commentaire fraîchement posté est « à l'instant » si le backend ne renvoie
                    // pas (ou pas dans un format lisible) sa date de création.
                    val newComment = r.data.toUi().let { c ->
                        if (c.time.isBlank()) c.copy(time = "à l'instant") else c
                    }
                    _state.update {
                        it.copy(
                            isSending = false,
                            comments = listOf(newComment) + it.comments
                        )
                    }
                    // Notifie le feed → le compteur de commentaires de la vidéo se met à jour.
                    commentBus.notifyAdded(videoId)
                }
                is NetworkResult.Failure -> _state.update { it.copy(isSending = false) }
            }
        }
    }

    /** Like/unlike un commentaire (optimiste + réconciliation backend). */
    fun toggleCommentLike(commentId: String) {
        val vid = loadedForVideoId
        val before = _state.value.comments.firstOrNull { it.id == commentId } ?: return
        val optimistic = !before.isLiked
        val count = (before.likesCount + if (optimistic) 1 else -1).coerceAtLeast(0)
        updateComment(commentId) { it.copy(isLiked = optimistic, likesCount = count, likesFmt = formatLikes(count)) }
        viewModelScope.launch {
            when (val r = socialRepository.likeComment(vid, commentId)) {
                is NetworkResult.Success -> updateComment(commentId) {
                    it.copy(isLiked = r.data.liked, likesCount = r.data.likesCount, likesFmt = formatLikes(r.data.likesCount))
                }
                is NetworkResult.Failure -> updateComment(commentId) {
                    it.copy(isLiked = before.isLiked, likesCount = before.likesCount, likesFmt = before.likesFmt)
                }
            }
        }
    }

    /** Épingle / désépingle un commentaire (créateur de la vidéo uniquement). Recharge la liste (tri). */
    fun togglePin(commentId: String) {
        val vid = loadedForVideoId
        viewModelScope.launch {
            when (socialRepository.pinComment(vid, commentId)) {
                is NetworkResult.Success -> {
                    loadedForVideoId = ""   // force le rechargement (le backend re-trie l'épinglé en tête)
                    load(vid)
                }
                is NetworkResult.Failure -> Unit
            }
        }
    }

    private inline fun updateComment(id: String, crossinline transform: (CommentUi) -> CommentUi) {
        _state.update { s -> s.copy(comments = s.comments.map { if (it.id == id) transform(it) else it }) }
    }

    private fun formatLikes(n: Int): String = when {
        n >= 1_000_000 -> String.format("%.1f M", n / 1_000_000f).replace('.', ',')
        n >= 1_000     -> String.format("%.1f K", n / 1_000f).replace('.', ',')
        else           -> n.toString()
    }

    private fun CommentDto.toUi(): CommentUi = CommentUi(
        id = id,
        avatarIdx = ((userId ?: id).hashCode() and 0x7FFFFFFF) % 6,
        username = username ?: "utilisateur",
        verified = false,
        pinned = isPinned,
        text = content ?: "",
        time = formatRelativeTime(createdAt),
        likesFmt = formatLikes(likesCount),
        avatarUrl = avatarUrl,
        likesCount = likesCount,
        isLiked = isLiked,
        repliesCount = repliesCount,
        isAuthor = isAuthor,
        mentions = mentions
    )

    private fun formatRelativeTime(createdAt: String?): String {
        if (createdAt.isNullOrBlank()) return ""
        val time = parseIsoUtc(createdAt) ?: return ""
        val diff = (System.currentTimeMillis() - time).coerceAtLeast(0L)
        return when {
            diff < 60_000L       -> "à l'instant"
            diff < 3_600_000L    -> "${diff / 60_000} min"
            diff < 86_400_000L   -> "${diff / 3_600_000} h"
            diff < 7L * 86_400_000L -> "${diff / 86_400_000} j"
            else -> SimpleDateFormat("d MMM", Locale.FRENCH).format(java.util.Date(time))
        }
    }

    /** Parse tolérant : avec/sans millisecondes, suffixe Z ou offset. */
    private fun parseIsoUtc(value: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        return patterns.firstNotNullOfOrNull { p ->
            runCatching {
                SimpleDateFormat(p, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(value)?.time
            }.getOrNull()
        }
    }
}
