package com.unovapp.android.data.auth

import com.google.gson.annotations.SerializedName

/* ---------- Requêtes ---------- */

data class SendOtpRequest(
    val phone: String,
    @SerializedName("country_code") val countryCode: String
)

data class VerifyOtpRequest(
    val phone: String,
    val code: String
)

data class GoogleSignInRequest(
    @SerializedName("id_token") val idToken: String
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

/* ---------- Réponses ---------- */

data class AuthSessionDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("wallet_balance") val walletBalance: Int
)

data class SendOtpResponse(
    @SerializedName("expires_in") val expiresIn: Int
)

/* ---------- Modèle domaine ---------- */

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val walletBalance: Int
)

fun AuthSessionDto.toDomain() = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresInSeconds = expiresIn,
    userId = user.id,
    username = user.username,
    displayName = user.displayName,
    avatarUrl = user.avatarUrl,
    walletBalance = user.walletBalance
)
