package com.unovapp.android.data.video

import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
 * Pré-télécharge **ENTIÈREMENT** (au plus 2) vidéos dans le **cache disque partagé** (le même
 * [androidx.media3.datasource.cache.SimpleCache] 1 Go que le lecteur). Quand l'utilisateur
 * arrive sur ces vidéos, TOUT est déjà sur disque — playlist comprise : lecture instantanée
 * et sans AUCUNE saccade du début à la fin, même réseau coupé.
 *
 * Règles produit :
 *  - téléchargement **complet** (tous les segments), pas seulement les premières secondes ;
 *  - **strictement séquentiel** : la vidéo en cours doit être finie avant d'entamer la
 *    suivante — jamais deux téléchargements en parallèle ;
 *  - au plus [DEFAULT_MAX] vidéos par salve, choisies au hasard parmi les non-réchauffées.
 *
 * Sobriété données : on télécharge la rendition la PLUS LÉGÈRE (144p/240p) — celle sur
 * laquelle l'ABR démarre. Vidéo entière ≈ 0,5–4 Mo selon la durée. [CacheWriter] ne
 * re-télécharge jamais les portions déjà en cache → reprendre une vidéo interrompue ne
 * coûte que ce qui manque.
 *
 * Le [SimpleCache] étant un singleton de processus, ce qui est écrit ici (worker de fond OU
 * appel in-app) est immédiatement visible par le lecteur. Un [Mutex] garantit qu'une seule
 * salve tourne à la fois (worker et in-app ne se marchent pas dessus).
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

    /** IDs déjà entièrement réchauffés → le feed peut les présenter en premier. */
    fun warmIds(): Set<String> = store.warmIds()
    fun isWarm(id: String): Boolean = store.contains(id)

    /**
     * Télécharge entièrement, l'une APRÈS l'autre, au plus [max] vidéos choisies au hasard
     * parmi [candidates] pas encore réchauffées. Retourne le nombre de vidéos complétées.
     * Annulable proprement (worker stoppé, ViewModel détruit) : l'annulation interrompt
     * entre deux segments, et la reprise ne re-télécharge pas ce qui est déjà en cache.
     */
    suspend fun prefetch(
        candidates: List<PrefetchTarget>,
        max: Int = DEFAULT_MAX
    ): Int = mutex.withLock {
        val pick = candidates
            .filter { it.playlistUrl.isNotBlank() && it.id.isNotBlank() && !store.contains(it.id) }
            .distinctBy { it.id }
            .shuffled()
            .take(max)

        var warmed = 0
        for (target in pick) {
            // SÉQUENTIEL STRICT : on ne passe à la vidéo suivante qu'une fois celle-ci
            // entièrement en cache (exigence produit) — ou définitivement en échec.
            val ok = try {
                withContext(Dispatchers.IO) { warmFully(target) }
            } catch (ce: CancellationException) {
                throw ce            // l'annulation doit remonter, pas être avalée
            } catch (_: Exception) {
                false               // best-effort : on tente la suivante
            }
            if (ok) {
                store.add(target.id)
                warmed++
            }
        }
        warmed
    }

    /**
     * Réchauffe UNE vidéo de bout en bout : la playlist d'abord (via le cache, pour que même
     * elle soit lisible hors réseau), puis TOUS ses segments dans l'ordre de lecture.
     */
    private suspend fun warmFully(target: PrefetchTarget): Boolean {
        // 1) La playlist passe aussi par le CacheWriter : au moment de lire, ExoPlayer la
        //    trouvera dans le cache — sans ça, un réseau coupé empêcherait le démarrage
        //    alors même que tous les segments seraient sur disque.
        cacheUrl(target.playlistUrl)

        // 2) Tous les segments, séquentiellement, avec point d'annulation entre chacun.
        val segments = fetchSegmentUrls(target.playlistUrl)
        if (segments.isEmpty()) return false
        for (url in segments) {
            currentCoroutineContext().ensureActive()
            cacheUrl(url)
        }
        return true
    }

    /** Écrit l'URL dans le SimpleCache partagé (ne télécharge que les portions manquantes). */
    private fun cacheUrl(url: String) {
        val dataSource = cacheDataSourceFactory.createDataSource()
        CacheWriter(dataSource, DataSpec(Uri.parse(url)), null, null).cache()
    }

    /** GET de la playlist de rendition + extraction de TOUTES les URLs de segment. */
    private fun fetchSegmentUrls(playlistUrl: String): List<String> {
        val request = Request.Builder().url(playlistUrl).get().build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val base = playlistUrl.substringBeforeLast('/', "")
            return body.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }   // lignes de segment uniquement
                .map { line -> if (line.startsWith("http")) line else "$base/$line" }
                .toList()
        }
    }

    private companion object {
        const val DEFAULT_MAX = 2   // « au plus 2 vidéos » (demande produit)
    }
}
