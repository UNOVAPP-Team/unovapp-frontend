package com.unovapp.android.data.user

import com.unovapp.android.data.auth.MessageResponse
import com.unovapp.android.data.video.FeedResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints du service User UNOVAPP. Le Bearer est ajouté automatiquement par
 * l'AuthInterceptor (OkHttpClient partagé).
 */
interface UserApi {

    /** Profil complet de l'utilisateur connecté. */
    @GET("users/me")
    suspend fun me(): UserProfileDto

    /** Profil public d'un utilisateur donné. */
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserProfileDto

    /** Modifier son propre profil (display_name, bio, username). Renvoie le profil à jour. */
    @PATCH("users/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body body: UpdateProfileRequest): UserProfileDto

    /** Recherche d'utilisateurs par pseudo/nom. */
    @GET("users/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PagedResponse<UserSummaryDto>

    @POST("users/{id}/follow")
    suspend fun follow(@Path("id") id: String): ResponseBody

    @DELETE("users/{id}/follow")
    suspend fun unfollow(@Path("id") id: String): ResponseBody

    @GET("users/{id}/followers")
    suspend fun followers(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PagedResponse<UserSummaryDto>

    @GET("users/{id}/following")
    suspend fun following(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PagedResponse<UserSummaryDto>

    /** Obtient une URL pré-signée S3 pour uploader un avatar. */
    @POST("users/me/avatar/presign")
    suspend fun avatarPresign(@Body body: AvatarPresignRequest): AvatarPresignResponse

    /** Confirme que l'avatar a bien été uploadé sur S3 — met à jour avatar_url dans le profil. */
    @PUT("users/me/avatar")
    suspend fun avatarConfirm(@Body body: AvatarConfirmRequest): UserProfileDto

    /** URL pré-signée pour uploader la photo de couverture (bannière du profil). */
    @POST("users/me/cover/presign")
    suspend fun coverPresign(@Body body: CoverPresignRequest): CoverPresignResponse

    /** Confirme la couverture uploadée → met à jour cover_url dans le profil. */
    @PUT("users/me/cover")
    suspend fun coverConfirm(@Body body: CoverConfirmRequest): UserProfileDto

    /* ---------- Sprint 1/2/3 ---------- */

    /** Vidéos d'un utilisateur (`:id` = UUID ou `me`). Auth optionnelle → format feed. */
    @GET("users/{id}/videos")
    suspend fun userVideos(
        @Path("id") id: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 12
    ): FeedResponse

    /** Vidéos aimées par l'utilisateur connecté (me uniquement). */
    @GET("users/me/liked")
    suspend fun likedVideos(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int = 12): FeedResponse

    /** Vidéos sauvegardées par l'utilisateur connecté (me uniquement). */
    @GET("users/me/saved")
    suspend fun savedVideos(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int = 12): FeedResponse

    /** Disponibilité d'un pseudo (temps réel pendant inscription / changement). */
    @GET("users/check")
    suspend fun checkUsername(@Query("username") username: String): AvailabilityResponse

    /** Définit les centres d'intérêt (remplace l'existant). */
    @POST("users/me/interests")
    suspend fun setInterests(@Body body: InterestsRequest): InterestsResponse

    /** Récupère les centres d'intérêt. */
    @GET("users/me/interests")
    suspend fun getInterests(): InterestsResponse

    /** Bloquer un utilisateur (rompt le follow dans les deux sens). */
    @POST("users/{id}/block")
    suspend fun blockUser(@Path("id") id: String): MessageResponse

    /** Débloquer un utilisateur. */
    @DELETE("users/{id}/block")
    suspend fun unblockUser(@Path("id") id: String): MessageResponse

    /** Suppression de compte RGPD (soft delete + anonymisation). Réponse 204. Irréversible. */
    @DELETE("users/me")
    suspend fun deleteMe()
}
