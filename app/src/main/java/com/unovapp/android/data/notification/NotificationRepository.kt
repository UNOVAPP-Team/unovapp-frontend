package com.unovapp.android.data.notification

import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall

interface NotificationRepository {
    suspend fun list(cursor: String? = null): NetworkResult<NotificationPageDto>
    suspend fun markRead(id: String): NetworkResult<Unit>
    suspend fun markAllRead(): NetworkResult<Unit>
}

class NotificationRepositoryImpl(private val api: NotificationApi) : NotificationRepository {
    override suspend fun list(cursor: String?): NetworkResult<NotificationPageDto> =
        safeCall { api.list(cursor) }

    override suspend fun markRead(id: String): NetworkResult<Unit> =
        safeCall { api.markRead(id); Unit }

    override suspend fun markAllRead(): NetworkResult<Unit> =
        safeCall { api.markAllRead(); Unit }
}
