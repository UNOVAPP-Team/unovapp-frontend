package com.unovapp.android.data.social

import com.google.gson.annotations.SerializedName
import com.unovapp.android.data.video.LikeResponse
import com.unovapp.android.data.video.PinResponse
import com.unovapp.android.data.video.ReportRequest
import com.unovapp.android.data.video.ReportedResponse
import com.unovapp.android.data.video.SaveResponse
import com.unovapp.android.data.video.ShareResponse
import com.unovapp.android.data.video.ViewRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SocialApi {

    /** Commentaires — cursor-based pagination (backend Social NestJS). */
    @GET("videos/{id}/comments")
    suspend fun comments(
        @Path("id")       videoId: String,
        @Query("cursor")  cursor: String?  = null,
        @Query("limit")   limit: Int       = 20
    ): CommentPageDto

    @POST("videos/{id}/comments")
    suspend fun postComment(
        @Path("id") videoId: String,
        @Body body: CommentRequest
    ): CommentDto

    @DELETE("videos/{id}/comments/{commentId}")
    suspend fun deleteComment(
        @Path("id")        videoId: String,
        @Path("commentId") commentId: String
    ): ResponseBody

    @POST("videos/{id}/like")
    suspend fun like(@Path("id") videoId: String): LikeResponse

    /** Vue simple (incrément) — sans body. */
    @POST("videos/{id}/view")
    suspend fun view(@Path("id") videoId: String): ResponseBody

    /** Vue + taux de complétion (watched/total secondes). */
    @POST("videos/{id}/view")
    suspend fun viewWithProgress(@Path("id") videoId: String, @Body body: ViewRequest): ResponseBody

    /* ---- Sprint 1/2/3 : actions sociales ---- */

    /** Sauvegarder / retirer des favoris (toggle). */
    @POST("videos/{id}/save")
    suspend fun save(@Path("id") videoId: String): SaveResponse

    /** Tracking de partage (Bearer optionnel) → shares_count. */
    @POST("videos/{id}/share")
    suspend fun share(@Path("id") videoId: String): ShareResponse

    /** Signaler une vidéo. */
    @POST("videos/{id}/report")
    suspend fun reportVideo(@Path("id") videoId: String, @Body body: ReportRequest): ReportedResponse

    /** Liker / déliker un commentaire (toggle). */
    @POST("videos/{id}/comments/{commentId}/like")
    suspend fun likeComment(@Path("id") videoId: String, @Path("commentId") commentId: String): LikeResponse

    /** Signaler un commentaire. */
    @POST("videos/{id}/comments/{commentId}/report")
    suspend fun reportComment(@Path("id") videoId: String, @Path("commentId") commentId: String, @Body body: ReportRequest): ReportedResponse

    /** Épingler / désépingler un commentaire (créateur de la vidéo uniquement, toggle). */
    @PATCH("videos/{id}/comments/{commentId}/pin")
    suspend fun pinComment(@Path("id") videoId: String, @Path("commentId") commentId: String): PinResponse

    /**
     * Recherche — texte libre + filtres avancés (hashtag, durée, tri). Bearer optionnel.
     * `sort` = "recent" | "popular". Si hashtag/durée fournis, `users` est vide.
     */
    @GET("search")
    suspend fun search(
        @Query("q") query: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("hashtag") hashtag: String? = null,
        @Query("min_duration") minDuration: Int? = null,
        @Query("max_duration") maxDuration: Int? = null,
        @Query("sort") sort: String? = null
    ): SearchResultDto
}

data class CommentRequest(
    val content: String,
    @SerializedName("parent_id") val parentId: String? = null
)

/** Résultat de GET /search?q= — vidéos + utilisateurs. */
data class SearchResultDto(
    val videos: List<VideoSummaryDto> = emptyList(),
    val users: List<UserSearchDto> = emptyList()
)

data class VideoSummaryDto(
    val id: String,
    @SerializedName("creator_id")    val creatorId: String = "",
    val description: String?         = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("views_count")   val viewsCount: Int = 0,
    @SerializedName("likes_count")   val likesCount: Int = 0
)

data class UserSearchDto(
    val id: String,
    val username: String = "",
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("is_verified")  val isVerified: Boolean = false
)

/**
 * Format réel du backend Social :
 * { id, video_id, user_id, content, parent_id, username, created_at, updated_at }
 * username est au niveau RACINE (pas dans un sous-objet user).
 */
data class CommentDto(
    val id: String,
    @SerializedName("video_id")   val videoId: String?  = null,
    @SerializedName("user_id")    val userId: String?   = null,
    val content: String?          = null,
    @SerializedName("parent_id")  val parentId: String? = null,
    val username: String?         = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("likes_count")   val likesCount: Int = 0,
    @SerializedName("is_liked")      val isLiked: Boolean = false,
    @SerializedName("replies_count") val repliesCount: Int = 0,
    @SerializedName("is_pinned")     val isPinned: Boolean = false,
    @SerializedName("is_author")     val isAuthor: Boolean = false,
    val mentions: List<String>    = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null
)

/** Réponse paginée cursor-based pour GET /videos/:id/comments. */
data class CommentPageDto(
    val data: List<CommentDto>             = emptyList(),
    @SerializedName("next_cursor") val nextCursor: String? = null,
    @SerializedName("has_more")    val hasMore: Boolean    = false
)
