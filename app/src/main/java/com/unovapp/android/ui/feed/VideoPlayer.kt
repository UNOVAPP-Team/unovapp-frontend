package com.unovapp.android.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.unovapp.android.R
import kotlinx.coroutines.delay

/**
 * Lecteur vidéo plein écran façon TikTok/Reels.
 *
 * Format : la vidéo est affichée en **FIT** (ratio préservé → entièrement visible, jamais
 * sur-zoomée ni déformée) PAR-DESSUS un **fond flouté** (la miniature de la vidéo, agrandie
 * et floutée) qui remplit tout l'écran. Résultat : plein écran sans bandes noires, on voit
 * toute la vidéo, et aucune déformation — exactement comme TikTok pour les formats qui ne
 * correspondent pas pile au ratio de l'écran. Pour une vidéo verticale, le fond est masqué
 * par la vidéo elle-même.
 *
 * Le PlayerView est gonflé depuis XML (surface_type="texture_view", fond transparent) pour
 * que le flou se voie dans les zones non couvertes par la vidéo, et pour éviter le bug
 * "double vidéo" de SurfaceView.
 */
@Composable
fun VideoPlayer(
    url: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    muted: Boolean = true,
    thumbnailUrl: String? = null,
    onProgress: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            repeatMode = Player.REPEAT_MODE_ALL
            volume = if (muted || !isPlaying) 0f else 1f
            playWhenReady = isPlaying
        }
    }

    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestMuted by rememberUpdatedState(muted)

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
        exoPlayer.volume = if (muted || !isPlaying) 0f else 1f
    }
    LaunchedEffect(muted, isPlaying) {
        exoPlayer.volume = if (muted || !isPlaying) 0f else 1f
    }

    if (onProgress != null) {
        LaunchedEffect(exoPlayer, isPlaying) {
            while (true) {
                val duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                onProgress(if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f)
                delay(250)
            }
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer.playWhenReady = false
                    exoPlayer.pause()
                    exoPlayer.volume = 0f
                }
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.playWhenReady = latestIsPlaying
                    if (latestIsPlaying) exoPlayer.play() else exoPlayer.pause()
                    exoPlayer.volume = if (latestMuted || !latestIsPlaying) 0f else 1f
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
            exoPlayer.volume = 0f
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Fond flouté qui remplit l'écran (remplace les bandes noires).
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(32.dp)
            )
            // Léger voile sombre : fait ressortir la vidéo et adoucit le fond.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
        }

        // Vidéo au premier plan — FIT : entièrement visible, jamais sur-zoomée.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.video_player, null) as PlayerView
                view.player = exoPlayer
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                view
            },
            update = { view ->
                if (view.player !== exoPlayer) view.player = exoPlayer
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                view.keepScreenOn = isPlaying
            }
        )
    }
}
