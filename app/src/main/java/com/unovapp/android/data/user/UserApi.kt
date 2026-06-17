package com.unovapp.android.data.user

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
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
}
