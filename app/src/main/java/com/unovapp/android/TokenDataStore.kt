package com.unovapp.android

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistance chiffrée des jetons de session via EncryptedSharedPreferences (AES-256-GCM).
 *
 * Robustesse :
 *  - Le StateFlow [_accessToken] est initialisé depuis les prefs persistées au démarrage
 *    (et non à null), donc [isAuthenticated] reflète correctement l'état réel dès la
 *    première émission — sans attendre un login ou un appel réseau.
 *  - La création des EncryptedSharedPreferences est entourée d'un try/catch : si la clé
 *    Keystore est inaccessible (backup/restore, migration d'appareil, corruption sur
 *    certains Samsung/Pixel), on supprime le fichier corrompu et on en recrée un propre.
 *    L'utilisateur doit se reconnecter une fois — mais l'app ne plante pas.
 */
@Singleton
class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { openOrRecreate() }

    /**
     * Initialisé depuis les prefs persistées, pas à null.
     * Sans ça, [getAccessToken] / [isAuthenticated] retournent null/false au démarrage
     * même si un token valide existe sur disque — ce qui provoque une redirection inutile
     * vers l'écran de login.
     */
    private val _accessToken = MutableStateFlow(runCatching { readAccessTokenSync() }.getOrNull())

    // -------------------------------------------------------------------------
    // API synchrone — utilisée par AuthInterceptor et TokenAuthenticator (OkHttp)
    // -------------------------------------------------------------------------

    fun readAccessTokenSync(): String? = runCatching { prefs.getString(KEY_ACCESS, null) }.getOrNull()
    fun readRefreshTokenSync(): String? = runCatching { prefs.getString(KEY_REFRESH, null) }.getOrNull()

    fun saveTokensSync(accessToken: String, refreshToken: String) {
        runCatching {
            prefs.edit()
                .putString(KEY_ACCESS, accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .commit()
        }
        _accessToken.value = accessToken
    }

    fun clearSync() {
        runCatching { prefs.edit().clear().commit() }
        _accessToken.value = null
    }

    // -------------------------------------------------------------------------
    // API suspend — utilisée par les ViewModels, repositories et Application
    // -------------------------------------------------------------------------

    suspend fun saveSession(accessToken: String, refreshToken: String, userId: String?) {
        withContext(Dispatchers.IO) {
            runCatching {
                prefs.edit()
                    .putString(KEY_ACCESS, accessToken)
                    .putString(KEY_REFRESH, refreshToken)
                    .apply { if (userId != null) putString(KEY_USER_ID, userId) }
                    .commit()
            }
        }
        _accessToken.value = accessToken
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        withContext(Dispatchers.IO) { saveTokensSync(accessToken, refreshToken) }
    }

    suspend fun readAccessToken(): String? =
        withContext(Dispatchers.IO) { readAccessTokenSync() }

    suspend fun readRefreshToken(): String? =
        withContext(Dispatchers.IO) { readRefreshTokenSync() }

    suspend fun readUserId(): String? =
        withContext(Dispatchers.IO) { runCatching { prefs.getString(KEY_USER_ID, null) }.getOrNull() }

    /** Pseudo de l'utilisateur connecté — sert notamment à filtrer ses propres activités
     *  dans les notifications (le backend n'expose pas encore actor_id). */
    suspend fun saveUsername(username: String) {
        withContext(Dispatchers.IO) {
            runCatching { prefs.edit().putString(KEY_USERNAME, username).commit() }
        }
    }

    fun readUsernameSync(): String? =
        runCatching { prefs.getString(KEY_USERNAME, null) }.getOrNull()

    fun getAccessToken(): Flow<String?> = _accessToken.asStateFlow()

    suspend fun clear() = withContext(Dispatchers.IO) { clearSync() }

    // -------------------------------------------------------------------------
    // Initialisation privée
    // -------------------------------------------------------------------------

    private fun openOrRecreate(): SharedPreferences {
        return try {
            buildPrefs()
        } catch (_: Exception) {
            // La clé Keystore est corrompue ou inaccessible.
            // On efface le fichier illisible et on recrée un store vide.
            context.deleteSharedPreferences(PREFS_NAME)
            try {
                buildPrefs()
            } catch (_: Exception) {
                // Dernier recours si même la création échoue (appareil très restrictif).
                context.getSharedPreferences("${PREFS_NAME}_plain", Context.MODE_PRIVATE)
            }
        }
    }

    private fun buildPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREFS_NAME  = "auth_secure_prefs"
        private const val KEY_ACCESS  = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
    }
}
