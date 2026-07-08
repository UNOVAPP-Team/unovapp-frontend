package com.unovapp.android

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.unovapp.android.notif.ActivityPollWorker
import com.unovapp.android.notif.Notifs
import com.unovapp.android.ui.feed.ReactionMemory
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
        Notifs.createChannels(this)
        ReactionMemory.init(this)
        scheduleActivityPolling()
        warmUpBackends()
    }

    /**
     * Planifie le sondage périodique des notifications d'activité (like/commentaire/abonné).
     * Intervalle minimal WorkManager = 15 min. Tourne même app fermée, uniquement en réseau.
     */
    private fun scheduleActivityPolling() {
        val request = PeriodicWorkRequestBuilder<ActivityPollWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ActivityPollWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Le VPS Hostinger ne se met plus en veille (contrairement à l'ancien plan Render Free),
     * donc plus de cold-start de 30–90 s à masquer. On garde malgré tout un ping léger sur
     * la passerelle (un GET /feed public) pour amorcer la connexion TCP/DNS dès l'ouverture
     * de l'app — fire-and-forget, sans impact si ça échoue.
     */
    private fun warmUpBackends() {
        val client = OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
        // GET public, route via la passerelle unique → réchauffe la connexion sans auth.
        val url = BuildConfig.AUTH_BASE_URL.trimEnd('/') + "/feed?limit=1"

        appScope.launch {
            runCatching {
                client.newCall(Request.Builder().url(url).get().build()).execute().close()
                Log.d(TAG, "Warm-up OK: $url")
            }.onFailure { Log.w(TAG, "Warm-up failed: $url — ${it.message}") }
        }
    }

    companion object {
        private const val TAG = "UnovApp"
    }
}
