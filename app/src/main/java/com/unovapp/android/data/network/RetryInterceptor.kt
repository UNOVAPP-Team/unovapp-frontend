package com.unovapp.android.data.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.math.pow

/**
 * Retry exponentiel pour les erreurs réseau et les 5xx.
 *
 * - GET, HEAD, OPTIONS : retry safe par défaut.
 * - Mutations (POST/PUT/PATCH/DELETE) : retry uniquement si le header `Idempotency-Key`
 *   est présent — sinon on risquerait un double débit Mobile Money ou un double like.
 *
 * Délais : 400ms, 1200ms, 3600ms (base 400 * 3^attempt).
 */
class RetryInterceptor(
    private val maxAttempts: Int = 3,
    private val baseDelayMs: Long = 400L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val safe = isSafeForRetry(request.method) ||
            request.header("Idempotency-Key") != null

        var attempt = 0
        var lastFailure: Throwable? = null
        var lastResponse: Response? = null

        while (attempt < maxAttempts) {
            try {
                lastResponse?.close()
                val response = chain.proceed(request)
                if (response.isSuccessful || response.code in 400..499 || !safe) {
                    return response
                }
                // 5xx → retry si safe
                lastResponse = response
                Log.w(TAG, "5xx received (code=${response.code}), retry attempt=${attempt + 1}")
            } catch (e: IOException) {
                lastFailure = e
                if (!safe) throw e
                Log.w(TAG, "IO failure: ${e.message}, retry attempt=${attempt + 1}")
            }

            attempt++
            if (attempt < maxAttempts) {
                val delay = (baseDelayMs * 3.0.pow(attempt - 1.0)).toLong()
                runCatching { Thread.sleep(delay) }
            }
        }

        lastResponse?.let { return it }
        throw lastFailure ?: IOException("Retry exhausted")
    }

    private fun isSafeForRetry(method: String): Boolean =
        method.equals("GET", true) ||
            method.equals("HEAD", true) ||
            method.equals("OPTIONS", true)

    companion object {
        private const val TAG = "RetryInterceptor"
    }
}
