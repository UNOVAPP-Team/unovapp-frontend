package com.unovapp.android.data.video

import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Cible d'un préfetch : l'id de la vidéo + l'URL de sa playlist de rendition légère. */
data class PrefetchTarget(val id: String, val playlistUrl: String)

/**
 * Pré-télécharge en arrière-plan les **premières secondes** de quelques vidéos dans le
 * **cache disque partagé** (le même [androidx.media3.datasource.cache.SimpleCache] 1 Go que le
 * lecteur). Quand l'utilisateur arrive ensuite sur ces vidéos, le player lit les segments
 * depuis le disque → **démarrage instantané, zéro réseau, aucune saccade**.
 *
 * Stratégie économe (le préfetch peut tourner sur données mobiles) :
 *  - on cible la rendition la PLUS LÉGÈRE (celle sur laquelle l'ABR démarre) → cache-hit garanti
 *    sur les segments réellement demandés au lancement, pour un minimum de données ;
 *  - on borne à [DEFAULT_MAX] vidéos × [SEGMENTS_PER_VIDEO] segments (≈ 8–12 s chacune) ;
 *  - on saute les vidéos déjà réchauffées ([PrefetchStore]).
 *
 * Le [SimpleCache] étant un singleton de processus, ce que ce préfetcheur écrit (worker de fond
 * OU appel in-app) est immédiatement visible par le lecteur. Un [Mutex] évite deux salves
 * concurrentes qui se marcheraient dessus.
 */
@Singleton
class VideoPrefetcher @Inject constructor(
    private val cacheDataSourceFactory: CacheDataSource.Factory,
    private val store: PrefetchStore
) {
    private val http = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val mutex = Mutex()

    /** IDs déjà réchauffés → le feed peut les présenter en premier. */
    fun warmIds(): Set<String> = store.warmIds()
    fun isWarm(id: String): Boolean = store.contains(id)

    /**
     * Réchauffe au plus [max] vidéos parmi [candidates] (choisies **au hasard** parmi celles pas
     * encore en cache). Retourne le nombre réellement réchauffé. Suspend, sûr à appeler depuis
     * un worker ou un ViewModel.
     */
    suspend fun prefetch(
        candidates: List<PrefetchTarget>,
        max: Int = DEFAULT_MAX,
        segmentsPerVideo: Int = SEGMENTS_PER_VIDEO
    ): Int = mutex.withLock {
        val pick = candidates
            .filter { it.playlistUrl.isNotBlank() && it.id.isNotBlank() && !store.contains(it.id) }
            .distinctBy { it.id }
            .shuffled()
            .take(max)

        var warmed = 0
        for (target in pick) {
            val ok = withContext(Dispatchers.IO) {
                runCatching { warmOne(target, segmentsPerVideo) }.getOrDefault(false)
            }
            if (ok) {
                store.add(target.id)
                warmed++
            }
        }
        warmed
    }

    /** Télécharge dans le cache les [n] premiers segments de la playlist de [target]. */
    private fun warmOne(target: PrefetchTarget, n: Int): Boolean {
        val segments = fetchSegmentUrls(target.playlistUrl, n)
        if (segments.isEmpty()) return false
        segments.forEach { url ->
            val dataSource = cacheDataSourceFactory.createDataSource()
            val spec = DataSpec(Uri.parse(url))
            // Bloquant : télécharge tout le segment (quelques centaines de Ko en rendition basse)
            // et l'écrit dans le SimpleCache partagé. En cas d'échec réseau → exception ignorée
            // par le runCatching appelant (le préfetch est best-effort, jamais bloquant).
            CacheWriter(dataSource, spec, null, null).cache()
        }
        return true
    }

    /** GET de la playlist de rendition + extraction des [n] premières URLs de segment (.ts). */
    private fun fetchSegmentUrls(playlistUrl: String, n: Int): List<String> {
        val request = Request.Builder().url(playlistUrl).get().build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val base = playlistUrl.substringBeforeLast('/', "")
            return body.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }   // lignes de segment uniquement
                .map { line -> if (line.startsWith("http")) line else "$base/$line" }
                .take(n)
                .toList()
        }
    }

    private companion object {
        const val DEFAULT_MAX = 2          // « au plus 2 vidéos » (demande produit)
        const val SEGMENTS_PER_VIDEO = 2   // ≈ 8–12 s → démarrage instantané, data bornée
    }
}
