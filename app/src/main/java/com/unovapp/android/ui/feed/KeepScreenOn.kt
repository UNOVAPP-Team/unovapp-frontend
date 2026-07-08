package com.unovapp.android.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Garde l'écran allumé tant que [enabled] est vrai (une vidéo est en cours de lecture) → l'écran
 * ne se met plus en veille pendant qu'on regarde une vidéo. Rétabli automatiquement à la sortie.
 */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
