package com.unovapp.android.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * État partagé (process singleton) du téléchargement en cours, alimenté par [DownloadService] et
 * observé par l'UI (feuille de partage). Le téléchargement vit dans le service → fermer la feuille
 * ne l'interrompt PAS ; l'UI se contente d'observer la progression.
 */
object DownloadCenter {
    data class State(
        val videoId: String? = null,
        val progress: Int = 0,
        val running: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun set(state: State) { _state.value = state }
    fun update(progress: Int) { _state.value = _state.value.copy(progress = progress) }
    fun clear() { _state.value = State() }
}
