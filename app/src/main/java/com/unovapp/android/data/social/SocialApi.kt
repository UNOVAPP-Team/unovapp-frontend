package com.unovapp.android.data.social

import com.google.gson.annotations.SerializedName
import com.unovapp.android.data.user.PagedResponse
import com.unovapp.android.data.user.UserSummaryDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints du service Social (commentaires, likes, vues, recherche).
 *
 * ⚠️ Ces routes opèrent sur des IDs de vidéos RÉELS. Tant que le backend n'expose pas la
 * liste du feed (`GET /videos`), elles ne peuvent pas être exercées de bout en bout.
 * Les DTO de commentaires sont provisoires — à confirmer quand le feed réel existera.
 */
interface SocialApi {

    @GET("videos/{id}/comments")
    suspend fun comments(
        @Path("id") videoId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PagedResponse<CommentDto>

    @POST("videos/{id}/comments")
    suspend fun postComment(
        @Path("id") videoId: String,
        @Body body: CommentRequest
    ): CommentDto

    @DELETE("videos/{id}/comments/{commentId}")
    suspend fun deleteComment(
        @Path("id") videoId: String,
        @Path("commentId") commentId: String
    ): ResponseBody

    @POST("videos/{id}/like")
    suspend fun like(@Path("id") videoId: String): ResponseBody

    @POST("videos/{id}/view")
    suspend fun view(@Path("id") videoId: String): ResponseBody

    @GET("search")
    suspend fun search(@Query("q") query: String): ResponseBody
}

data class CommentRequest(val content: String)

/** Provisoire — champs à confirmer avec le backend une fois le feed réel disponible. */
data class CommentDto(
    val id: String,
    val content: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val user: UserSummaryDto? = null
)
