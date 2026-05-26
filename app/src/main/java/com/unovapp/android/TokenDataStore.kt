package com.unovapp.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("auth_prefs")

@Singleton
class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ACCESS_TOKEN = stringPreferencesKey("access_token")

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[ACCESS_TOKEN] = token }
    }

    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { it[ACCESS_TOKEN] }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(ACCESS_TOKEN) }
    }
}
