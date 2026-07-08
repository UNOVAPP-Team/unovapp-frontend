package com.unovapp.android.data.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileStore @Inject constructor() {
    private val _profiles = MutableStateFlow<Map<String, UserProfileDto>>(emptyMap())
    val profiles: StateFlow<Map<String, UserProfileDto>> = _profiles.asStateFlow()

    fun observe(userId: String) = profiles.map { it[userId] }

    fun upsert(profile: UserProfileDto) {
        _profiles.update { it + (profile.id to profile) }
    }

    fun upsertAll(items: Iterable<UserProfileDto>) {
        _profiles.update { current ->
            current + items.associateBy { it.id }
        }
    }
}
