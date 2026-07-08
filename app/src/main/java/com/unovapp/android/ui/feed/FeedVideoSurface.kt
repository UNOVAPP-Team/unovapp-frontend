package com.unovapp.android.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.unovapp.android.R

/**
 * Surface de rendu d'une page du feed. **Ne possède PAS de lecteur** : elle attache le
 * lecteur UNIQUE partagé ([player]) fourni par [FeedScreen] uniquement à la page courante.
 *
 * - [player] non-null (page courante) → PlayerView en FIT par-dessus le fond flouté.
 * - [player] null (page voisine, non courante) → uniquement la miniature floutée (aucune
 *   lecture possible → plus jamais deux vidéos en même temps).
 *
 * Format : FIT (ratio préservé, aucune déformation ni sur-zoom) + fond flouté plein écran
 * (façon TikTok pour les vidéos qui ne font pas pile le ratio de l'écran).
 */
@Composable
fun FeedVideoSurface(
    player: ExoPlayer?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                // Décodage volontairement minuscule : l'image est floutée puis agrandie plein
                // écran de toute façon → décodage/mémoire ~100× moindres, et l'upscale ajoute
                // un flou naturel sur les APIs < 31 où Modifier.blur est sans effet.
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .size(96, 192)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(32.dp)
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
        }

        // Le PlayerView n'est monté que pour la page courante → le lecteur unique n'est
        // attaché qu'à une seule surface à la fois.
        if (player != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val view = android.view.LayoutInflater.from(ctx)
                        .inflate(R.layout.video_player, null) as PlayerView
                    view.player = player
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    view
                },
                update = { view ->
                    if (view.player !== player) view.player = player
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                onRelease = { view -> view.player = null }
            )
        }
    }
}
