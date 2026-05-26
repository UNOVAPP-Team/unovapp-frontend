package com.unovapp.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.unovapp.android.ui.theme.UnovColors

/**
 * Gold arc that fills as time elapses. `progress` is 0..1 (fraction of time consumed).
 */
@Composable
fun CountdownRing(
    progress: Float,
    modifier: Modifier = Modifier.size(28.dp),
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val pad = strokeWidth.dp.toPx()
        val arcSize = Size(size.width - 2 * pad, size.height - 2 * pad)
        val topLeft = Offset(pad, pad)
        val stroke = Stroke(width = pad, cap = StrokeCap.Round)

        // background ring
        drawArc(
            color = UnovColors.Accent.copy(alpha = 0.16f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        // progress arc
        drawArc(
            color = UnovColors.Accent,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
    }
}
