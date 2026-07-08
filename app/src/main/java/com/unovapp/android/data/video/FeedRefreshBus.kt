package com.unovapp.android.data.video

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bus léger pour signaler qu'une nouvelle vidéo vient d'être publiée. Le [FeedViewModel]
 * l'observe pour recharger le feed automatiquement, de sorte que la vidéo qu'on vient
 * d'uploader apparaisse sans relancer l'app. Singleton → partagé entre les ViewModels.
 */
@Singleton
class FeedRefreshBus @Inject constructor() {

    // replay=0, buffer=1 : une publication déclenche un rechargement, sans rejouer
    // d'anciens événements à un nouvel abonné.
    private val _events = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    /** Signale qu'une vidéo a été publiée → recharge le feed. */
    fun signalNewVideo() {
        _events.tryEmit(Unit)
    }
}
