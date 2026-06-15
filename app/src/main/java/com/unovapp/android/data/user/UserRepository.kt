package com.unovapp.android.data.user

import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall

interface UserRepository {
    suspend fun fetchMe(): NetworkResult<UserProfileDto>
}

class UserRepositoryImpl(
    private val api: UserApi
) : UserRepository {
    override suspend fun fetchMe(): NetworkResult<UserProfileDto> = safeCall { api.me() }
}
