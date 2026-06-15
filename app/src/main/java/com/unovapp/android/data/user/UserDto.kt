package com.unovapp.android.data.user

import com.google.gson.annotations.SerializedName

/**
 * Profil complet renvoyé par `GET /users/me` (service user, sérialisé en snake_case).
 * Voir : https://unovapp-user.onrender.com/api/docs
 */
data class UserProfileDto(
    val id: String,
    val username: String,
    @SerializedName("display_name") val displayName: String?,
    val bio: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("subscription_tier") val subscriptionTier: String = "free",
    val email: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("wallet_balance") val walletBalance: Double = 0.0
)

/** Version allégée renvoyée par la recherche / les listes d'abonnés. */
data class UserSummaryDto(
    val id: String,
    val username: String,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false
)

/** Enveloppe paginée standard du backend : { data, total, page, limit }. */
data class PagedResponse<T>(
    val data: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)
