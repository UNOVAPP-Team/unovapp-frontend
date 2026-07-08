package com.unovapp.android.data.user

import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall
import com.unovapp.android.data.video.FeedResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

interface UserRepository {
    suspend fun fetchMe(): NetworkResult<UserProfileDto>
    suspend fun getUser(id: String): NetworkResult<UserProfileDto>
    suspend fun updateProfile(
        id: String,
        displayName: String,
        bio: String,
        username: String,
        websiteUrl: String? = null
    ): NetworkResult<UserProfileDto>
    suspend fun search(query: String, page: Int = 1, limit: Int = 20): NetworkResult<PagedResponse<UserSummaryDto>>
    suspend fun follow(id: String): NetworkResult<Unit>
    suspend fun unfollow(id: String): NetworkResult<Unit>
    suspend fun followers(id: String, page: Int = 1): NetworkResult<PagedResponse<UserSummaryDto>>
    suspend fun following(id: String, page: Int = 1): NetworkResult<PagedResponse<UserSummaryDto>>
    /** Flux complet d'upload avatar : presign → PUT S3 → confirm. Retourne le profil mis à jour. */
    suspend fun uploadAvatar(contentType: String, bytes: ByteArray): NetworkResult<UserProfileDto>

    /* ---------- Sprint 1/2/3 ---------- */
    suspend fun userVideos(id: String, cursor: String? = null): NetworkResult<FeedResponse>
    suspend fun likedVideos(cursor: String? = null): NetworkResult<FeedResponse>
    suspend fun savedVideos(cursor: String? = null): NetworkResult<FeedResponse>
    suspend fun checkUsername(username: String): NetworkResult<AvailabilityResponse>
    suspend fun getInterests(): NetworkResult<InterestsResponse>
    suspend fun setInterests(categories: List<String>): NetworkResult<InterestsResponse>
    suspend fun blockUser(id: String): NetworkResult<Unit>
    suspend fun unblockUser(id: String): NetworkResult<Unit>
    suspend fun deleteAccount(): NetworkResult<Unit>
}

class UserRepositoryImpl(
    private val api: UserApi,
    private val profileStore: UserProfileStore
) : UserRepository {

    private val s3Client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun fetchMe(): NetworkResult<UserProfileDto> = safeCall {
        api.me().also(profileStore::upsert)
    }

    override suspend fun getUser(id: String): NetworkResult<UserProfileDto> =
        safeCall { api.getUser(id).also(profileStore::upsert) }

    override suspend fun updateProfile(
        id: String,
        displayName: String,
        bio: String,
        username: String,
        websiteUrl: String?
    ): NetworkResult<UserProfileDto> = safeCall {
        api.updateProfile(
            id,
            UpdateProfileRequest(displayName = displayName, bio = bio, username = username, websiteUrl = websiteUrl)
        ).also(profileStore::upsert)
    }

    override suspend fun search(query: String, page: Int, limit: Int): NetworkResult<PagedResponse<UserSummaryDto>> =
        safeCall { api.search(query, page, limit) }

    override suspend fun follow(id: String): NetworkResult<Unit> = safeCall {
        api.follow(id).close(); Unit
    }

    override suspend fun unfollow(id: String): NetworkResult<Unit> = safeCall {
        api.unfollow(id).close(); Unit
    }

    override suspend fun followers(id: String, page: Int): NetworkResult<PagedResponse<UserSummaryDto>> =
        safeCall { api.followers(id, page) }

    override suspend fun following(id: String, page: Int): NetworkResult<PagedResponse<UserSummaryDto>> =
        safeCall { api.following(id, page) }

    override suspend fun uploadAvatar(contentType: String, bytes: ByteArray): NetworkResult<UserProfileDto> =
        safeCall {
            val presign = api.avatarPresign(AvatarPresignRequest(contentType))
            val body = bytes.toRequestBody(contentType.toMediaType())
            val request = Request.Builder()
                .url(presign.uploadUrl)
                .put(body)
                .header("Content-Type", contentType)
                .build()
            withContext(Dispatchers.IO) {
                s3Client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("S3 PUT ${response.code}")
                }
            }
            api.avatarConfirm(AvatarConfirmRequest(presign.key)).also(profileStore::upsert)
        }

    override suspend fun userVideos(id: String, cursor: String?): NetworkResult<FeedResponse> =
        safeCall { api.userVideos(id, cursor) }

    override suspend fun likedVideos(cursor: String?): NetworkResult<FeedResponse> =
        safeCall { api.likedVideos(cursor) }

    override suspend fun savedVideos(cursor: String?): NetworkResult<FeedResponse> =
        safeCall { api.savedVideos(cursor) }

    override suspend fun checkUsername(username: String): NetworkResult<AvailabilityResponse> =
        safeCall { api.checkUsername(username) }

    override suspend fun getInterests(): NetworkResult<InterestsResponse> =
        safeCall { api.getInterests() }

    override suspend fun setInterests(categories: List<String>): NetworkResult<InterestsResponse> =
        safeCall { api.setInterests(InterestsRequest(categories)) }

    override suspend fun blockUser(id: String): NetworkResult<Unit> =
        safeCall { api.blockUser(id); Unit }

    override suspend fun unblockUser(id: String): NetworkResult<Unit> =
        safeCall { api.unblockUser(id); Unit }

    override suspend fun deleteAccount(): NetworkResult<Unit> =
        safeCall { api.deleteMe() }
}
