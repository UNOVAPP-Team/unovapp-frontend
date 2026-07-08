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

/** Vérification du code OTP reçu par email — finalise l'inscription. */
data class VerifyEmailRequest(
    val email: String,
    @SerializedName("otp_code") val otpCode: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class GoogleSignInRequest(
    @SerializedName("id_token") val idToken: String
)

data class ForgotPasswordRequest(
    val email: String
)

/**
 * Réinitialisation en une étape : email + code OTP (6 chiffres) reçu par email + nouveau
 * mot de passe. Cf. POST /auth/reset-password — body { email, otp_code, newPassword }.
 */
data class ResetPasswordRequest(
    val email: String,
    @SerializedName("otp_code") val otpCode: String,
    val newPassword: String
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)

/* ---------- Sprint 2 : changement d'email, vérif téléphone, sessions ---------- */

data class ChangeEmailRequest(val newEmail: String)

/** Confirmation par code OTP (changement d'email). */
data class OtpCodeRequest(@SerializedName("otp_code") val otpCode: String)

data class EmailResponse(val email: String)

data class VerifiedResponse(val verified: Boolean = false)

/** Une session/appareil connecté (GET /auth/sessions). */
data class SessionDto(
    val id: String,
    @SerializedName("device_info") val deviceInfo: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("last_used_at") val lastUsedAt: String? = null,
    @SerializedName("is_current") val isCurrent: Boolean = false
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
