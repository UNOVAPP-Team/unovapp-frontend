package com.unovapp.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("auth_prefs")

/**
 * Persistance des jetons de session. On stocke trois choses :
 *  - `access_token` : JWT court-vivant (~1 h) lu par l'AuthInterceptor.
 *  - `refresh_token` : JWT long-vivant (~7 j) utilisé pour rafraîchir l'access token.
 *  - `user_id` : pratique pour les appels `GET /users/{id}` sans refaire `/auth/me`.
 */
@Singleton
class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val USER_ID = stringPreferencesKey("user_id")

    suspend fun saveSession(accessToken: String, refreshToken: String, userId: String?) {
        context.dataStore.edit { p ->
            p[ACCESS_TOKEN] = accessToken
            p[REFRESH_TOKEN] = refreshToken
            if (userId != null) p[USER_ID] = userId
        }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { p ->
            p[ACCESS_TOKEN] = accessToken
            p[REFRESH_TOKEN] = refreshToken
        }
    }

    fun getAccessToken(): Flow<String?> =
        context.dataStore.data.map { it[ACCESS_TOKEN] }

    suspend fun readAccessToken(): String? =
        context.dataStore.data.first()[ACCESS_TOKEN]

    suspend fun readRefreshToken(): String? =
        context.dataStore.data.first()[REFRESH_TOKEN]

    suspend fun readUserId(): String? =
        context.dataStore.data.first()[USER_ID]

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
