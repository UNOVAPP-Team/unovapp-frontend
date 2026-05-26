package com.unovapp.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unovapp.android.ui.theme.UnovColors

/**
 * Mini-trend ligne dorée + fill dégradé sous la courbe, façon dashboard analytics.
 * Les `values` sont normalisées entre min et max pour remplir la hauteur disponible.
 */
@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = 16.dp,
    strokeColor: Color = UnovColors.Accent,
    fillColor: Color = UnovColors.Accent
) {
    Canvas(modifier = modifier.size(width = width, height = height)) {
        if (values.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val maxV = values.max()
        val minV = values.min()
        val range = (maxV - minV).coerceAtLeast(0.0001f)
        val step = w / (values.size - 1)

        val linePath = Path()
        val fillPath = Path().apply { moveTo(0f, h) }

        values.forEachIndexed { i, v ->
            val x = i * step
            val y = h - ((v - minV) / range) * h
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                0f to fillColor.copy(alpha = 0.35f),
                1f to fillColor.copy(alpha = 0f)
            )
        )
        drawPath(
            path = linePath,
            color = strokeColor,
            style = Stroke(
                width = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
