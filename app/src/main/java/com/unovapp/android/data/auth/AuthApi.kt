package com.unovapp.android.data.auth

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Endpoints Auth UNOVAPP — alignés sur l'OpenAPI du service `unovapp-auth`.
 * Voir : https://unovapp-auth.onrender.com/api/docs
 */
interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): TokensResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokensResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshTokenRequest): TokensResponse

    /** Bearer requis (cf. swagger). Réveille la session courante avec ses infos minimales. */
    @POST("auth/me")
    suspend fun me(): MeResponse

    @POST("auth/logout")
    suspend fun logout()

    /** Publique. Envoie un OTP 6 chiffres au numéro fourni (E.164, ex: +22961234567). */
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body body: SendOtpRequest): MessageResponse

    /** Bearer requis. Vérifie le code OTP reçu — confirme le numéro du compte courant. */
    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): MessageResponse

    /** Bearer requis. (Re)envoie l'email de vérification au compte courant. */
    @POST("auth/send-email-verification")
    suspend fun sendEmailVerification(): ResponseBody

    /**
     * Publique. Valide l'email via le token reçu par email (lien cliqué / deep link).
     * Réponse non typée (peut être HTML/texte) → on ne lit pas le corps, juste le statut 2xx.
     */
    @GET("auth/verify-email")
    suspend fun verifyEmail(@Query("token") token: String): ResponseBody
}
