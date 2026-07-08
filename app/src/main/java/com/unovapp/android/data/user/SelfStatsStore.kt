package com.unovapp.android.data.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deltas locaux des stats de l'utilisateur connecté, propagés entre écrans sans recharger.
 * Ex. : liker sa propre vidéo (depuis le feed ou la grille du profil) → +1 sur « J'aime reçus »
 * affiché sur le profil, instantanément. Remis à zéro à chaque `GET /users/me`.
 */
@Singleton
class SelfStatsStore @Inject constructor() {
    private val _likesReceivedDelta = MutableStateFlow(0)
    val likesReceivedDelta: StateFlow<Int> = _likesReceivedDelta.asStateFlow()

    fun addLikeReceived(delta: Int) { _likesReceivedDelta.update { it + delta } }
    fun reset() { _likesReceivedDelta.value = 0 }
}
