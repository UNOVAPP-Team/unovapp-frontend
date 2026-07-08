package com.unovapp.android.data.social

import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall
import com.unovapp.android.data.video.LikeResponse
import com.unovapp.android.data.video.PinResponse
import com.unovapp.android.data.video.ReportRequest
import com.unovapp.android.data.video.SaveResponse
import com.unovapp.android.data.video.ShareResponse
import com.unovapp.android.data.video.ViewRequest

interface SocialRepository {
    suspend fun comments(videoId: String, cursor: String? = null): NetworkResult<CommentPageDto>
    suspend fun postComment(videoId: String, content: String, parentId: String? = null): NetworkResult<CommentDto>
    suspend fun deleteComment(videoId: String, commentId: String): NetworkResult<Unit>
    suspend fun like(videoId: String): NetworkResult<LikeResponse>
    suspend fun view(videoId: String): NetworkResult<Unit>
    suspend fun viewWithProgress(videoId: String, watchedSeconds: Int, totalSeconds: Int): NetworkResult<Unit>
    suspend fun save(videoId: String): NetworkResult<SaveResponse>
    suspend fun share(videoId: String): NetworkResult<ShareResponse>
    suspend fun reportVideo(videoId: String, reason: String, description: String? = null): NetworkResult<Unit>
    suspend fun likeComment(videoId: String, commentId: String): NetworkResult<LikeResponse>
    suspend fun reportComment(videoId: String, commentId: String, reason: String, description: String? = null): NetworkResult<Unit>
    suspend fun pinComment(videoId: String, commentId: String): NetworkResult<PinResponse>
    suspend fun search(query: String, cursor: String? = null): NetworkResult<SearchResultDto>
    suspend fun searchAdvanced(
        query: String? = null,
        hashtag: String? = null,
        minDuration: Int? = null,
        maxDuration: Int? = null,
        sort: String? = null,
        cursor: String? = null
    ): NetworkResult<SearchResultDto>
}

class SocialRepositoryImpl(
    private val api: SocialApi
) : SocialRepository {

    override suspend fun comments(videoId: String, cursor: String?): NetworkResult<CommentPageDto> =
        safeCall { api.comments(videoId, cursor) }

    override suspend fun postComment(videoId: String, content: String, parentId: String?): NetworkResult<CommentDto> =
        safeCall { api.postComment(videoId, CommentRequest(content, parentId)) }

    override suspend fun deleteComment(videoId: String, commentId: String): NetworkResult<Unit> =
        safeCall { api.deleteComment(videoId, commentId).close(); Unit }

    override suspend fun like(videoId: String): NetworkResult<LikeResponse> =
        safeCall { api.like(videoId) }

    override suspend fun view(videoId: String): NetworkResult<Unit> =
        safeCall { api.view(videoId).close(); Unit }

    override suspend fun viewWithProgress(videoId: String, watchedSeconds: Int, totalSeconds: Int): NetworkResult<Unit> =
        safeCall { api.viewWithProgress(videoId, ViewRequest(watchedSeconds, totalSeconds)).close(); Unit }

    override suspend fun save(videoId: String): NetworkResult<SaveResponse> =
        safeCall { api.save(videoId) }

    override suspend fun share(videoId: String): NetworkResult<ShareResponse> =
        safeCall { api.share(videoId) }

    override suspend fun reportVideo(videoId: String, reason: String, description: String?): NetworkResult<Unit> =
        safeCall { api.reportVideo(videoId, ReportRequest(reason, description)); Unit }

    override suspend fun likeComment(videoId: String, commentId: String): NetworkResult<LikeResponse> =
        safeCall { api.likeComment(videoId, commentId) }

    override suspend fun reportComment(videoId: String, commentId: String, reason: String, description: String?): NetworkResult<Unit> =
        safeCall { api.reportComment(videoId, commentId, ReportRequest(reason, description)); Unit }

    override suspend fun pinComment(videoId: String, commentId: String): NetworkResult<PinResponse> =
        safeCall { api.pinComment(videoId, commentId) }

    override suspend fun search(query: String, cursor: String?): NetworkResult<SearchResultDto> =
        safeCall { api.search(query = query, cursor = cursor) }

    override suspend fun searchAdvanced(
        query: String?, hashtag: String?, minDuration: Int?, maxDuration: Int?, sort: String?, cursor: String?
    ): NetworkResult<SearchResultDto> =
        safeCall { api.search(query, cursor, hashtag, minDuration, maxDuration, sort) }
}
