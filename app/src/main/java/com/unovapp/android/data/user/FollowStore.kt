package com.unovapp.android.data.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source unique de vérité du graphe « qui je suis » côté client.
 * Partagée par recherche, profil, listes d'abonnés/abonnements → un seul état cohérent.
 *
 * Deux signaux :
 *  - [following] : l'ensemble des IDs que l'utilisateur connecté suit (connus durant la session).
 *  - [followingDelta] : variation nette du nombre d'abonnements depuis le dernier `/users/me`,
 *    pour mettre à jour le compteur du profil **immédiatement** sans recharger le réseau.
 *
 * Le backend ne renvoie pas (encore) d'indicateur `is_following` sur les résultats de recherche ;
 * l'état des boutons reflète donc les actions de la session + les listes déjà chargées ([merge]).
 */
@Singleton
class FollowStore @Inject constructor() {

    private val _following = MutableStateFlow<Set<String>>(emptySet())
    val following: StateFlow<Set<String>> = _following.asStateFlow()

    private val _followingDelta = MutableStateFlow(0)
    val followingDelta: StateFlow<Int> = _followingDelta.asStateFlow()

    fun isFollowing(id: String): Boolean = _following.value.contains(id)

    /** Applique un suivi / désabonnement (idempotent). Ajuste le delta du compteur. */
    fun setFollowing(id: String, follow: Boolean) {
        val cur = _following.value
        when {
            follow && !cur.contains(id) -> {
                _following.value = cur + id
                _followingDelta.value += 1
            }
            !follow && cur.contains(id) -> {
                _following.value = cur - id
                _followingDelta.value -= 1
            }
        }
    }

    /**
     * Amorce l'état connu à partir d'une liste chargée (ex. mes abonnements). N'affecte PAS le
     * delta : ces relations existent déjà, elles sont juste découvertes par le client.
     */
    fun merge(ids: Collection<String>) {
        if (ids.isEmpty()) return
        _following.value = _following.value + ids
    }

    /**
     * Après un `/users/me` frais, le compteur backend intègre déjà les changements de la session :
     * on remet le delta à zéro pour éviter le double comptage.
     */
    fun resetDelta() {
        _followingDelta.value = 0
    }
}
