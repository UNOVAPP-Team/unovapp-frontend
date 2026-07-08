package com.unovapp.android.data.social

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bus léger inter-écrans : signale qu'un commentaire vient d'être ajouté à une vidéo, pour
 * que le feed (et l'écran de lecture) mette à jour son compteur de commentaires sans recharger.
 */
@Singleton
class CommentBus @Inject constructor() {
    private val _added = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val added: SharedFlow<String> = _added.asSharedFlow()

    /** delta signé : +1 à l'ajout, -1 si un jour on gère la suppression. */
    private val _delta = MutableSharedFlow<Pair<String, Int>>(extraBufferCapacity = 16)
    val delta: SharedFlow<Pair<String, Int>> = _delta.asSharedFlow()

    fun notifyAdded(videoId: String) {
        _added.tryEmit(videoId)
        _delta.tryEmit(videoId to 1)
    }
}
