package com.unovapp.android.data.network

import com.unovapp.android.TokenDataStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Ajoute `Authorization: Bearer <jwt>` à toutes les requêtes sortantes — sauf les routes
 * d'auth publiques (login, register, refresh, send-otp) qui ne nécessitent pas de token.
 *
 * Utilise readAccessTokenSync() (EncryptedSharedPreferences, lecture synchrone) pour éviter
 * tout runBlocking sur le thread OkHttp — supprime le risque d'ANR.
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

        val token = tokenStore.readAccessTokenSync()
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
            "/auth/verify-email",
            "/auth/refresh",
            "/auth/send-otp",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/google"
        )
    }
}
