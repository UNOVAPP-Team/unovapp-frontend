package com.unovapp.android.data.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.unovapp.android.BuildConfig
import com.unovapp.android.TokenDataStore
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Rafraîchit automatiquement l'access token sur un 401, puis rejoue la requête.
 *
 * Distinction critique entre les deux types d'échec du refresh :
 *
 *  - [RefreshOutcome.AuthInvalid] : le serveur rejette explicitement le refresh token
 *    (HTTP 401 ou 403). La session est définitivement invalide → on purge les tokens
 *    et l'utilisateur devra se reconnecter.
 *
 *  - [RefreshOutcome.NetworkError] : erreur réseau, timeout, serveur indisponible ou
 *    réponse inattendue (5xx, parsing échoué…). C'est une défaillance temporaire —
 *    on ne purge PAS les tokens. L'utilisateur reste "connecté" et l'app réessaiera
 *    au prochain appel.
 *
 * Cette distinction évite la déconnexion intempestive quand le backend Render.com
 * (free tier) est en cold-start ou temporairement indisponible.
 */
class TokenAuthenticator(
    private val tokenStore: TokenDataStore
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val staleToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        // Vérification rapide hors verrou : si le token a déjà été rafraîchi par
        // une requête concurrente, on rejoue directement avec le nouveau token.
        val currentToken = tokenStore.readAccessTokenSync()
        if (currentToken != null && currentToken != staleToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }

        if (!LOCK.tryLock(30, TimeUnit.SECONDS)) return null
        return try {
            // Double-vérification après acquisition du verrou.
            val latestToken = tokenStore.readAccessTokenSync()
            if (latestToken != null && latestToken != staleToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }

            val refreshToken = tokenStore.readRefreshTokenSync() ?: run {
                // Pas de refresh token stocké → session invalide, purge.
                tokenStore.clearSync()
                return null
            }

            when (val outcome = tryRefreshTokens(refreshToken)) {
                is RefreshOutcome.Success -> {
                    tokenStore.saveTokensSync(outcome.accessToken, outcome.refreshToken)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${outcome.accessToken}")
                        .build()
                }
                RefreshOutcome.AuthInvalid -> {
                    // Rejet définitif par le serveur → l'utilisateur doit se reconnecter.
                    tokenStore.clearSync()
                    null
                }
                RefreshOutcome.NetworkError -> {
                    // Erreur temporaire → on ne purge PAS les tokens.
                    // L'utilisateur reste connecté ; la requête courante échoue
                    // mais les prochaines tenteront à nouveau le refresh.
                    null
                }
            }
        } finally {
            LOCK.unlock()
        }
    }

    private sealed class RefreshOutcome {
        data class Success(val accessToken: String, val refreshToken: String) : RefreshOutcome()
        /** Le serveur a rejeté le refresh token (401 ou 403). Session définitivement invalide. */
        object AuthInvalid : RefreshOutcome()
        /** Problème réseau ou serveur temporairement indisponible. Ne pas purger les tokens. */
        object NetworkError : RefreshOutcome()
    }

    private fun tryRefreshTokens(refreshToken: String): RefreshOutcome {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            // Le contrat backend attend `refreshToken` (camelCase), pas `refresh_token`.
            // Cf. POST /auth/refresh — body { refreshToken }.
            val body = """{"refreshToken":"$refreshToken"}"""
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(BuildConfig.AUTH_BASE_URL + "auth/refresh")
                .post(body)
                .build()
            client.newCall(request).execute().use { resp ->
                // 400-403 = rejet du refresh token (expiré, invalide, révoqué…).
                // Le backend NestJS peut retourner 400 (malformed/expired) aussi bien
                // que 401/403. Tout code 4xx < 404 = session définitivement invalide.
                if (resp.code in 400..403) return RefreshOutcome.AuthInvalid
                // Toute autre erreur HTTP (5xx, timeout HTTP…) = problème temporaire.
                if (!resp.isSuccessful) return RefreshOutcome.NetworkError
                val json = resp.body?.string() ?: return RefreshOutcome.NetworkError
                val obj = runCatching { Gson().fromJson(json, JsonObject::class.java) }.getOrNull()
                    ?: return RefreshOutcome.NetworkError
                // Supporte camelCase (accessToken) ET snake_case (access_token) selon
                // la configuration de sérialisation du backend NestJS.
                val access = obj.get("accessToken")?.asString
                    ?: obj.get("access_token")?.asString
                    ?: return RefreshOutcome.NetworkError
                val newRefresh = obj.get("refreshToken")?.asString
                    ?: obj.get("refresh_token")?.asString
                    ?: refreshToken
                RefreshOutcome.Success(access, newRefresh)
            }
        } catch (_: Exception) {
            // IOException, SocketTimeoutException, etc. = réseau indisponible.
            RefreshOutcome.NetworkError
        }
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) { count++; r = r.priorResponse }
        return count
    }

    companion object {
        private val LOCK = ReentrantLock()
    }
}
