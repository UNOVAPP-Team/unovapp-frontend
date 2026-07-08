package com.unovapp.android.data.notification

import com.google.gson.annotations.SerializedName
import com.unovapp.android.data.auth.MessageResponse
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Notifications in-app (Sprint 3, item 23). Historique des push reçus + compteur non lus.
 * Bearer requis. Pagination cursor.
 */
interface NotificationApi {

    @GET("notifications")
    suspend fun list(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): NotificationPageDto

    @PATCH("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): MessageResponse

    @POST("notifications/read-all")
    suspend fun markAllRead(): MessageResponse
}

data class NotificationItemDto(
    val id: String,
    val type: String = "",
    val title: String = "",
    val body: String? = null,
    val data: Map<String, String> = emptyMap(),
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String = ""
)

data class NotificationPageDto(
    val data: List<NotificationItemDto> = emptyList(),
    @SerializedName("next_cursor") val nextCursor: String? = null,
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("unread_count") val unreadCount: Int = 0
)
