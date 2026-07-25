package com.unovapp.android.data.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source unique de vérité du **nombre de notifications non lues**, partagée entre l'écran
 * Inbox et le badge de la barre de navigation.
 *
 * Détenteur d'état pur (aucune dépendance réseau) → injectable partout en singleton. Alimenté :
 *  - par l'écran Inbox (valeur autoritative `unread_count` du backend, décrément au fil des
 *    lectures, remise à zéro sur « Tout lire ») ;
 *  - par le badge de la bottom bar (rafraîchi à l'ouverture de l'app / retour au premier plan) ;
 *  - par le worker de sondage périodique (badge frais même après un poll en arrière-plan).
 */
@Singleton
class UnreadNotificationsStore @Inject constructor() {

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    /** Fixe la valeur autoritative renvoyée par le backend. */
    fun set(value: Int) {
        _count.value = value.coerceAtLeast(0)
    }

    /** Une notification vient d'être lue → un de moins (jamais négatif). */
    fun decrement(by: Int = 1) {
        _count.value = (_count.value - by).coerceAtLeast(0)
    }

    /** « Tout marquer comme lu » → plus aucune non lue. */
    fun clear() {
        _count.value = 0
    }
}
