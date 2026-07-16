package com.unovapp.android.notif

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unovapp.android.TokenDataStore
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.notification.NotificationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Sonde périodiquement le backend de notifications et affiche une **notification système** pour
 * chaque nouvelle activité (j'aime, commentaire, nouvel abonné). C'est ainsi que l'app « utilise
 * les notifications » sans dépendre d'un serveur push : WorkManager réveille ce worker en tâche
 * de fond (≈ toutes les 15 min) même app fermée.
 *
 * Utilise un [EntryPoint] Hilt pour récupérer ses dépendances (pas besoin de HiltWorkerFactory).
 * La requête est authentifiée automatiquement (AuthInterceptor lit le token en session).
 */
class ActivityPollWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun notificationRepository(): NotificationRepository
        fun tokenStore(): TokenDataStore
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)

        // Pas connecté → rien à faire.
        deps.tokenStore().readAccessTokenSync() ?: return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSeenId = prefs.getString(KEY_LAST_ID, null)

        return when (val r = deps.notificationRepository().list()) {
            is NetworkResult.Success -> {
                val items = r.data.data
                if (items.isEmpty()) return Result.success()

                // Premier passage : on mémorise juste la référence sans notifier (évite de spammer
                // l'historique existant au premier lancement).
                if (lastSeenId == null) {
                    prefs.edit().putString(KEY_LAST_ID, items.first().id).apply()
                    return Result.success()
                }

                // Items du plus récent au plus ancien → on prend ceux au-dessus du dernier vu
                // (nouveaux), non lus, et on notifie (du plus ancien au plus récent, max 5).
                // Filtre anti-auto-notification : le backend crée une entrée même quand c'est
                // MOI qui aime/commente ma propre vidéo ("monPseudo a aimé ta vidéo"). Le payload
                // n'expose pas actor_id → on écarte les titres qui commencent par mon pseudo.
                val myUsername = deps.tokenStore().readUsernameSync()
                items.takeWhile { it.id != lastSeenId }
                    .filter { !it.isRead }
                    .filter { item ->
                        myUsername.isNullOrBlank() ||
                            !item.title.startsWith("$myUsername ", ignoreCase = true)
                    }
                    .take(5)
                    .reversed()
                    .forEach { item ->
                        Notifs.activity(
                            applicationContext,
                            title = item.title.ifBlank { "UNOVAPP" },
                            body = item.body ?: item.title.ifBlank { "Nouvelle activité" }
                        )
                    }
                prefs.edit().putString(KEY_LAST_ID, items.first().id).apply()
                Result.success()
            }
            is NetworkResult.Failure -> Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "activity_poll"
        private const val PREFS = "notif_poll"
        private const val KEY_LAST_ID = "last_id"
    }
}
