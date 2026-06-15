package com.unovapp.android.data.user

import retrofit2.http.GET

/**
 * Endpoints du service User UNOVAPP. Le Bearer est ajouté automatiquement par
 * l'AuthInterceptor (OkHttpClient partagé).
 */
interface UserApi {

    /** Profil complet de l'utilisateur connecté. */
    @GET("users/me")
    suspend fun me(): UserProfileDto
}
