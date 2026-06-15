package com.unovapp.android

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class UnovApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        warmUpBackends()
    }

    /**
     * Render free tier endort les services après ~15 min d'inactivité. Un cold start coûte
     * 30–90 s, et tomberait pile sur le premier appel d'auth de l'utilisateur. On déclenche
     * un ping fire-and-forget sur `/health` dès l'ouverture de l'app pour que le serveur
     * soit chaud d'ici à ce que l'utilisateur passe l'onboarding.
     */
    private fun warmUpBackends() {
        val client = OkHttpClient.Builder()
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
        val urls = listOf(
            BuildConfig.AUTH_BASE_URL,
            BuildConfig.USER_BASE_URL,
            BuildConfig.SOCIAL_BASE_URL
        ).map { it.trimEnd('/') + "/health" }

        appScope.launch {
            urls.forEach { url ->
                runCatching {
                    client.newCall(Request.Builder().url(url).get().build()).execute().close()
                    Log.d(TAG, "Warm-up OK: $url")
                }.onFailure { Log.w(TAG, "Warm-up failed: $url — ${it.message}") }
            }
        }
    }

    companion object {
        private const val TAG = "UnovApp"
    }
}
