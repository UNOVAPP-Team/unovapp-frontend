package com.unovapp.android.data.auth

import com.google.gson.annotations.SerializedName

/* ---------- Requêtes (alignées sur le Swagger backend) ---------- */

data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    @SerializedName("phone_number") val phoneNumber: String?
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class SendOtpRequest(
    @SerializedName("phone_number") val phoneNumber: String
)

data class VerifyOtpRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    val code: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

/* ---------- Réponses ----------
 * Le backend NestJS sérialise en camelCase (pas snake_case) — pas de @SerializedName ici.
 */

data class TokensResponse(
    val accessToken: String,
    val refreshToken: String
)

data class MeResponse(
    val userId: String,
    val email: String
)

data class MessageResponse(
    val message: String
)

/* ---------- Modèle domaine ---------- */

/**
 * Identité minimale qu'on stocke en session côté client. Le username / displayName / avatar
 * viendront du service User (`GET /users/{id}`) — pas encore câblé.
 */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String
)

fun TokensResponse.toSessionWithMe(me: MeResponse) = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = me.userId,
    email = me.email
)
