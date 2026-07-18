package com.unovapp.android.data.video

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mémorise les vidéos dont les premiers segments ont été **pré-téléchargés dans le cache**
 * (par [VideoPrefetcher]). Sert à deux choses :
 *  - éviter de re-préfetcher une vidéo déjà réchauffée,
 *  - permettre au feed d'AFFICHER EN PREMIER ces vidéos (démarrage instantané garanti).
 *
 * Liste bornée (les plus récentes en fin) : au-delà de [CAP], on oublie les plus anciennes —
 * le cache disque ExoPlayer (LRU 1 Go) les aura de toute façon évincées en premier.
 */
@Singleton
class PrefetchStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun warmIds(): Set<String> = read().toSet()

    @Synchronized
    fun contains(id: String): Boolean = read().contains(id)

    @Synchronized
    fun add(id: String) {
        val list = read().toMutableList()
        list.remove(id)      // remet en tête de fraîcheur
        list.add(id)
        while (list.size > CAP) list.removeAt(0)
        prefs.edit().putString(KEY, list.joinToString(SEP)).apply()
    }

    private fun read(): List<String> =
        prefs.getString(KEY, "").orEmpty().split(SEP).filter { it.isNotBlank() }

    private companion object {
        const val PREFS = "video_prefetch"
        const val KEY = "warm_ids"
        const val SEP = ","
        const val CAP = 80
    }
}
