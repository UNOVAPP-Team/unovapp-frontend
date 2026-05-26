package com.unovapp.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.unovapp.android.ui.theme.UnovColors

/**
 * Motif décoratif "filigrane" doré : 6 courbes ondulées + 12 points lumineux.
 * Posé en background sur la cover du profil, mode multiply visuel via alpha.
 *
 * Le viewBox source est 400×200 — on rescale dynamiquement à la taille du canvas
 * pour garder la même densité visuelle quelle que soit la hauteur de la cover.
 */
@Composable
fun Filigree(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.alpha(0.22f)) {
        val w = size.width
        val h = size.height
        val sx = w / 400f
        val sy = h / 200f

        val brush = Brush.linearGradient(
            colors = listOf(UnovColors.Accent, UnovColors.AccentDeep),
            start = Offset.Zero,
            end = Offset(w, h)
        )
        val stroke = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round)

        // 6 courbes harmoniques cubiques
        repeat(6) { i ->
            val baseOffset = (i % 2) * 40f
            val verticalOffset = (i % 2) * 60f
            val tailOffset = (i % 2) * 20f

            val startX = (-50f + i * 80f) * sx
            val startY = (20f + baseOffset) * sy
            val cx1 = (i * 80f + 30f) * sx
            val cy1 = (10f + verticalOffset) * sy
            val cx2 = (i * 80f + 110f) * sx
            val cy2 = (50f + tailOffset) * sy
            val endX = (i * 80f + 240f) * sx
            val endY = 30f * sy

            val path = Path().apply {
                moveTo(startX, startY)
                cubicTo(cx1, cy1, cx2, cy2, endX, endY)
            }

            drawPath(
                path = path,
                brush = brush,
                style = stroke,
                alpha = 0.5f - i * 0.05f
            )
        }

        // 12 points lumineux dorés
        repeat(12) { i ->
            val cx = (i * 36f + 10f) * sx
            val cy = ((i % 3) * 50f + 30f) * sy
            val opacity = 0.25f + (i % 4) * 0.15f
            drawCircle(
                color = UnovColors.Accent.copy(alpha = opacity),
                radius = 1.2.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
    }
}
