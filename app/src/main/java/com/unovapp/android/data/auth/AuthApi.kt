package com.unovapp.android.data.auth

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Endpoints Auth UNOVAPP — alignés sur l'OpenAPI du service `unovapp-auth`.
 * Voir : https://unovapp-auth.onrender.com/api/docs
 *
 * Flux d'inscription (nouveau contrat backend) :
 *  1. register(email, username, password, phone?) → envoie un CODE OTP par EMAIL (message, pas de tokens)
 *  2. verifyEmail(email, otp_code) → finalise le compte et renvoie les tokens
 */
interface AuthApi {

    /** Démarre l'inscription : envoie un code OTP par email. Ne renvoie PAS de tokens. */
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): MessageResponse

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

    /** Publique. Vérifie le code OTP reçu par email et finalise le compte → tokens. */
    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequest): TokensResponse

    /** Publique. Connexion via un ID Token Google → tokens UNOVAPP. */
    @POST("auth/google")
    suspend fun google(@Body body: GoogleSignInRequest): TokensResponse

    /** Publique. Demande un lien/token de réinitialisation par email. */
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): MessageResponse

    /** Publique. Réinitialise le mot de passe avec le token reçu par email. */
    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): MessageResponse
}
