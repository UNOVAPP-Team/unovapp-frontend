package com.unovapp.android.data.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.unovapp.android.BuildConfig
import com.unovapp.android.TokenDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/**
 * Rafraîchit automatiquement l'access token sur un 401 via `POST /auth/refresh`, puis rejoue
 * la requête. Si le refresh échoue, la session est purgée (l'utilisateur sera renvoyé au login).
 *
 * Utilise un client OkHttp dédié (sans intercepteur d'auth) pour éviter toute boucle.
 */
class TokenAuthenticator(
    private val tokenStore: TokenDataStore
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Déjà retenté une fois → on abandonne (évite les boucles infinies).
        if (responseCount(response) >= 2) return null

        val refreshToken = runBlocking { tokenStore.readRefreshToken() } ?: return null
        val newTokens = refreshTokens(refreshToken) ?: run {
            runBlocking { tokenStore.clear() }
            return null
        }
        runBlocking { tokenStore.saveTokens(newTokens.first, newTokens.second) }

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.first}")
            .build()
    }

    /** Appel direct au endpoint refresh. Retourne (accessToken, refreshToken) ou null. */
    private fun refreshTokens(refreshToken: String): Pair<String, String>? = try {
        // Timeouts généreux : Render free tier peut être en cold start (~30-90 s).
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val body = """{"refresh_token":"$refreshToken"}"""
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(BuildConfig.AUTH_BASE_URL + "auth/refresh")
            .post(body)
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = resp.body?.string() ?: return null
            val obj = Gson().fromJson(json, JsonObject::class.java)
            val access = obj.get("accessToken")?.asString ?: return null
            val refresh = obj.get("refreshToken")?.asString ?: refreshToken
            access to refresh
        }
    } catch (e: Exception) {
        null
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) {
            count++
            r = r.priorResponse
        }
        return count
    }
}
