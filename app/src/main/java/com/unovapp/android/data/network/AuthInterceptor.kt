package com.unovapp.android.data.network

import com.unovapp.android.TokenDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Ajoute `Authorization: Bearer <jwt>` à toutes les requêtes sortantes — sauf les routes
 * d'auth publiques (login, register, refresh, send-otp) qui ne nécessitent pas de token.
 */
class AuthInterceptor(
    private val tokenStore: TokenDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        if (PUBLIC_PATHS.any { path.endsWith(it) }) {
            return chain.proceed(original)
        }

        val token = runBlocking { tokenStore.readAccessToken() }
        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else original

        return chain.proceed(request)
    }

    companion object {
        private val PUBLIC_PATHS = listOf(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/send-otp"
        )
    }
}
