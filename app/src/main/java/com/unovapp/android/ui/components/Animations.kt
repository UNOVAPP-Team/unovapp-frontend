package com.unovapp.android.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Entrée premium « fade + slide-up » avec léger overshoot d'échelle, déclenchée par [visible].
 * Avec [delayMillis] croissant par élément → cascade sophistiquée (staggered reveal).
 */
@Composable
fun Modifier.enterFadeSlide(
    visible: Boolean,
    delayMillis: Int = 0,
    slideDp: Float = 26f,
    durationMs: Int = 520
): Modifier {
    val p by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = durationMs, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "enterFadeSlide"
    )
    val density = LocalDensity.current
    val ty = with(density) { ((1f - p) * slideDp).dp.toPx() }
    return this.graphicsLayer {
        alpha = p
        translationY = ty
        val s = 0.96f + 0.04f * p
        scaleX = s
        scaleY = s
    }
}

/**
 * Smooth vertical floating motion. Each card uses a different period / phase to
 * avoid synchronous bobbing — matches the staggered `animation: float ...` in CSS.
 */
@Composable
fun Modifier.floating(
    periodMs: Int = 3500,
    amplitudeDp: Float = 8f,
    reverse: Boolean = false
): Modifier {
    val transition = rememberInfiniteTransition(label = "float")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val density = LocalDensity.current
    val translatePx = with(density) {
        (sin(if (reverse) -phase else phase) * amplitudeDp).dp.toPx()
    }
    return this.graphicsLayer { translationY = translatePx }
}

@Composable
fun Modifier.pulseScale(
    minScale: Float = 0.96f,
    maxScale: Float = 1.04f,
    periodMs: Int = 1400
): Modifier {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    return this.scale(scale)
}
