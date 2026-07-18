package com.unovapp.android.data.user

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source unique de vérité du graphe « qui je suis » côté client.
 * Partagée par feed, recherche, profil, listes d'abonnés/abonnements → un seul état cohérent.
 *
 * **Persisté par utilisateur** (SharedPreferences, clé `following_<userId>`) : les boutons
 * « Suivre/Suivi » sont corrects dès le démarrage à froid, même si la réponse `/feed` n'est pas
 * enrichie (le token d'accès expire en 1 h et `/feed` étant à JWT optionnel, un token périmé ne
 * déclenche PAS de refresh — le backend répond alors comme à un anonyme, `is_following=false`
 * partout). La persistance rend l'app immunisée contre ce cas.
 *
 * Deux signaux :
 *  - [following] : l'ensemble des IDs que l'utilisateur connecté suit (connus à ce jour).
 *  - [followingDelta] : variation nette du nombre d'abonnements depuis le dernier `/users/me`,
 *    pour mettre à jour le compteur du profil **immédiatement** sans recharger le réseau.
 */
@Singleton
class FollowStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Utilisateur dont la liste est actuellement hydratée (null = personne). */
    private var sessionUserId: String? = null

    private val _following = MutableStateFlow<Set<String>>(emptySet())
    val following: StateFlow<Set<String>> = _following.asStateFlow()

    private val _followingDelta = MutableStateFlow(0)
    val followingDelta: StateFlow<Int> = _followingDelta.asStateFlow()

    /**
     * Déclare l'utilisateur de la session courante et hydrate SA liste persistée.
     * Idempotent (rappelable à chaque écran). `null` = déconnexion → état vidé, rien de
     * l'ancien compte ne fuit vers le suivant.
     */
    @Synchronized
    fun onSession(userId: String?) {
        if (userId == sessionUserId) return
        sessionUserId = userId
        _following.value = if (userId == null) emptySet()
        else prefs.getStringSet(key(userId), emptySet())?.toSet() ?: emptySet()
        _followingDelta.value = 0
    }

    fun isFollowing(id: String): Boolean = _following.value.contains(id)

    /** Applique un suivi / désabonnement (idempotent). Ajuste le delta du compteur. */
    @Synchronized
    fun setFollowing(id: String, follow: Boolean) {
        val cur = _following.value
        when {
            follow && !cur.contains(id) -> {
                _following.value = cur + id
                _followingDelta.value += 1
                persist()
            }
            !follow && cur.contains(id) -> {
                _following.value = cur - id
                _followingDelta.value -= 1
                persist()
            }
        }
    }

    /**
     * Amorce l'état connu à partir d'une source backend (liste d'abonnements, enrichment du
     * feed, `is_following` d'un profil…). N'affecte PAS le delta : ces relations existent déjà,
     * elles sont juste découvertes par le client.
     */
    @Synchronized
    fun merge(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val next = _following.value + ids
        if (next.size != _following.value.size) {
            _following.value = next
            persist()
        }
    }

    /**
     * Après un `/users/me` frais, le compteur backend intègre déjà les changements de la session :
     * on remet le delta à zéro pour éviter le double comptage.
     */
    fun resetDelta() {
        _followingDelta.value = 0
    }

    private fun persist() {
        val id = sessionUserId ?: return
        prefs.edit().putStringSet(key(id), _following.value).apply()
    }

    private fun key(userId: String) = "following_$userId"

    private companion object {
        const val PREFS = "follow_store"
    }
}
