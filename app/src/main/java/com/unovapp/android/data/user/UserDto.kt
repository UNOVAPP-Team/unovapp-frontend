package com.unovapp.android.data.user

import com.google.gson.annotations.SerializedName

/**
 * Profil complet renvoyé par `GET /users/me` (service user, sérialisé en snake_case).
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
    @SerializedName("wallet_balance") val walletBalance: Double = 0.0,
    @SerializedName("is_following") val isFollowing: Boolean = false,
    @SerializedName("is_blocked") val isBlocked: Boolean = false,
    @SerializedName("website_url") val websiteUrl: String? = null,
    // Stats agrégées (Sprint 1/3)
    @SerializedName("videos_count") val videosCount: Int = 0,
    @SerializedName("total_views_received") val totalViewsReceived: Int = 0,
    @SerializedName("total_likes_received") val totalLikesReceived: Int = 0,
    @SerializedName("total_comments_received") val totalCommentsReceived: Int = 0,
    @SerializedName("total_shares_received") val totalSharesReceived: Int = 0
)

/** Version allégée renvoyée par la recherche / les listes d'abonnés. */
data class UserSummaryDto(
    val id: String,
    val username: String,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false
)

/** Corps du PATCH /users/{id} — modification du profil (champs texte). */
data class UpdateProfileRequest(
    @SerializedName("display_name") val displayName: String? = null,
    val bio: String? = null,
    val username: String? = null,
    @SerializedName("website_url") val websiteUrl: String? = null,
    @SerializedName("avatar_s3_key") val avatarS3Key: String? = null
)

/** GET /users/check?username= → { available }. */
data class AvailabilityResponse(val available: Boolean = false)

/** Centres d'intérêt (POST/GET /users/me/interests). */
data class InterestsRequest(val categories: List<String>)
data class InterestsResponse(val categories: List<String> = emptyList())

/** Corps POST /users/me/avatar/presign */
data class AvatarPresignRequest(val contentType: String)

/** Réponse POST /users/me/avatar/presign (NestJS → camelCase). */
data class AvatarPresignResponse(
    val key: String,
    val uploadUrl: String,
    val method: String = "PUT",
    val contentType: String = "image/jpeg",
    val publicUrl: String = "",
    val expiresIn: Int = 300
)

/** Corps PUT /users/me/avatar — confirme l'avatar uploadé sur S3 */
data class AvatarConfirmRequest(val key: String)

/** Enveloppe paginée standard du backend : { data, total, page, limit }. */
data class PagedResponse<T>(
    val data: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)
