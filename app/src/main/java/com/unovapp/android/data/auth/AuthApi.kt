package com.unovapp.android.data.auth

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Endpoints Auth UNOVAPP — conformes au Swagger 3.0 figé en Semaine 1 du planning.
 * Voir : Chapitre 6 du Planning Technique (Contrat API).
 */
interface AuthApi {

    @POST("auth/otp/send")
    suspend fun sendOtp(@Body body: SendOtpRequest): SendOtpResponse

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): AuthSessionDto

    @POST("auth/google")
    suspend fun signInWithGoogle(@Body body: GoogleSignInRequest): AuthSessionDto

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthSessionDto

    @POST("auth/logout")
    suspend fun logout(@Header("Idempotency-Key") idempotencyKey: String)
}
