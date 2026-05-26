package com.unovapp.android.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Lecteur vidéo HLS (ou MP4) plein écran, façon TikTok/Reels.
 *
 * Caractéristiques :
 *  - Player instancié par item (key=url), libéré au leave-composition.
 *  - Boucle infinie (`REPEAT_MODE_ALL`) — vidéos courtes ≤ 90s.
 *  - Pause auto si l'app passe en background (observer Lifecycle).
 *  - Démarre **muet** : on respecte les data caps africains, l'utilisateur unmute s'il veut.
 *  - `RESIZE_MODE_ZOOM` : la vidéo remplit toujours l'écran (crop des bords si ratio diff).
 *
 * Le param [isPlaying] est piloté par le pager (true uniquement pour la page courante)
 * — c'est ce qui donne l'auto-play sur scroll.
 */
@Composable
fun VideoPlayer(
    url: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = isPlaying
        }
    }

    // Sync play/pause avec le pager
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    // Auto-pause quand l'app va en background — économise data + batterie
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> exoPlayer.playWhenReady = false
                Lifecycle.Event.ON_START -> exoPlayer.playWhenReady = isPlaying
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.BLACK)
            }
        }
    )
}
