package com.unovapp.android.data.video

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unovapp.android.data.network.NetworkResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Réchauffe périodiquement le cache vidéo **même app fermée** : récupère le feed public et
 * pré-télécharge (au plus 2) vidéos au hasard dans le cache disque partagé. La seule condition
 * est le réseau (contrainte WorkManager `CONNECTED`, cf. UnovApp) → dès que l'utilisateur a des
 * données, quelques vidéos sont prêtes à démarrer instantanément à la prochaine ouverture.
 *
 * Récupère ses dépendances via un [EntryPoint] Hilt (même schéma que ActivityPollWorker), car
 * un CoroutineWorker classique n'est pas injectable directement.
 */
class FeedPrefetchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun videoRepository(): VideoRepository
        fun videoPrefetcher(): VideoPrefetcher
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)

        return when (val r = deps.videoRepository().feed(limit = 12, type = "foryou")) {
            is NetworkResult.Success -> {
                val targets = r.data.data.mapNotNull { dto ->
                    // On saute les vidéos sans rendition (mock). Un id backend fait 36 caractères.
                    dto.lowestRenditionUrl()?.let { url -> PrefetchTarget(dto.id, url) }
                }
                deps.videoPrefetcher().prefetch(targets, max = 2)
                Result.success()
            }
            // Réseau indisponible/instable → WorkManager réessaiera (backoff par défaut).
            is NetworkResult.Failure -> Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "feed_prefetch"
    }
}
