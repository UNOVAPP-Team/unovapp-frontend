package com.unovapp.android.data.user

import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall

interface UserRepository {
    suspend fun fetchMe(): NetworkResult<UserProfileDto>
    suspend fun getUser(id: String): NetworkResult<UserProfileDto>
    suspend fun updateProfile(
        id: String,
        displayName: String,
        bio: String,
        username: String
    ): NetworkResult<UserProfileDto>
    suspend fun search(query: String, page: Int = 1, limit: Int = 20): NetworkResult<PagedResponse<UserSummaryDto>>
    suspend fun follow(id: String): NetworkResult<Unit>
    suspend fun unfollow(id: String): NetworkResult<Unit>
    suspend fun followers(id: String, page: Int = 1): NetworkResult<PagedResponse<UserSummaryDto>>
    suspend fun following(id: String, page: Int = 1): NetworkResult<PagedResponse<UserSummaryDto>>
}

class UserRepositoryImpl(
    private val api: UserApi
) : UserRepository {

    override suspend fun fetchMe(): NetworkResult<UserProfileDto> = safeCall { api.me() }

    override suspend fun getUser(id: String): NetworkResult<UserProfileDto> =
        safeCall { api.getUser(id) }

    override suspend fun updateProfile(
        id: String,
        displayName: String,
        bio: String,
        username: String
    ): NetworkResult<UserProfileDto> = safeCall {
        api.updateProfile(
            id,
            UpdateProfileRequest(displayName = displayName, bio = bio, username = username)
        )
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
}
