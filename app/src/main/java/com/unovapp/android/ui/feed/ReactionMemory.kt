package com.unovapp.android.ui.feed

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf

/**
 * Mémoire des réactions choisies par vidéo. Le backend n'expose qu'un like booléen — le TYPE de
 * réaction (cœur, feu, éclair…) est donc conservé côté client.
 *
 * **Persisté sur disque** (SharedPreferences) : le sticker choisi survit désormais à la fermeture
 * et à la réouverture de l'app (avant, tout revenait au cœur car c'était en mémoire seule).
 * Seuls les choix EXPLICITES sont persistés (pas le cœur par défaut appliqué aux vidéos déjà likées).
 */
object ReactionMemory {
    /** Map observable (Compose) videoId → réaction. Lecture directe pour l'affichage. */
    val map = mutableStateMapOf<String, Reaction>()

    private var prefs: SharedPreferences? = null

    /** À appeler une fois au démarrage de l'app (charge les réactions persistées). */
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("reactions", Context.MODE_PRIVATE)
        prefs = p
        p.all.forEach { (key, value) ->
            (value as? String)?.let { name ->
                runCatching { Reaction.valueOf(name) }.getOrNull()?.let { map[key] = it }
            }
        }
    }

    /** Choix explicite → mémorisé ET persisté. */
    fun set(videoId: String, reaction: Reaction) {
        map[videoId] = reaction
        prefs?.edit()?.putString(videoId, reaction.name)?.apply()
    }

    /** Retire la réaction (unlike) → mémoire + disque. */
    fun clear(videoId: String) {
        map.remove(videoId)
        prefs?.edit()?.remove(videoId)?.apply()
    }

    /** Défaut cœur pour une vidéo déjà likée : mémoire seule (non persisté, se recalcule au besoin). */
    fun setDefault(videoId: String, reaction: Reaction) {
        if (map[videoId] == null) map[videoId] = reaction
    }
}
