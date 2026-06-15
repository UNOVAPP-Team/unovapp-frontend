package com.unovapp.android.data.social

import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall
import com.unovapp.android.data.user.PagedResponse

interface SocialRepository {
    suspend fun comments(videoId: String, page: Int = 1): NetworkResult<PagedResponse<CommentDto>>
    suspend fun postComment(videoId: String, content: String): NetworkResult<CommentDto>
    suspend fun deleteComment(videoId: String, commentId: String): NetworkResult<Unit>
    suspend fun like(videoId: String): NetworkResult<Unit>
    suspend fun view(videoId: String): NetworkResult<Unit>
}

class SocialRepositoryImpl(
    private val api: SocialApi
) : SocialRepository {

    override suspend fun comments(videoId: String, page: Int): NetworkResult<PagedResponse<CommentDto>> =
        safeCall { api.comments(videoId, page) }

    override suspend fun postComment(videoId: String, content: String): NetworkResult<CommentDto> =
        safeCall { api.postComment(videoId, CommentRequest(content)) }

    override suspend fun deleteComment(videoId: String, commentId: String): NetworkResult<Unit> =
        safeCall { api.deleteComment(videoId, commentId).close(); Unit }

    override suspend fun like(videoId: String): NetworkResult<Unit> =
        safeCall { api.like(videoId).close(); Unit }

    override suspend fun view(videoId: String): NetworkResult<Unit> =
        safeCall { api.view(videoId).close(); Unit }
}
