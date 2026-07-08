package com.unovapp.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovMotion
import kotlinx.coroutines.delay

/**
 * Tap pressable façon "haute qualité" : scale down spring (0.96) + haptic léger au down,
 * scale up bouncy au release. C'est la signature physique d'UNOVAPP — toute zone cliquable
 * importante devrait l'utiliser.
 *
 * Pas de ripple Material (incompatible avec notre design noir/or premium).
 */
fun Modifier.unovTap(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    haptic: Boolean = true
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = UnovMotion.snappy(),
        label = "unovTapScale"
    )

    LaunchedEffect(pressed) {
        if (pressed && haptic && enabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = {
                if (haptic) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        )
}

/**
 * Effet shimmer (gradient lumineux qui balaie un placeholder de gauche à droite).
 * Pour les skeletons de chargement. Loop infinie tant que le composable est visible.
 *
 * À appliquer **avant** un `.background()` neutre ou sur un Box vide.
 */
fun Modifier.shimmer(
    enabled: Boolean = true,
    base: Color = Color(0xFF1A1A1A),
    highlight: Color = Color(0xFF2A1A0A)  // teinte orange très atténuée
): Modifier = composed {
    if (!enabled) return@composed this.background(base)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    this
        .background(base)
        .drawWithContent {
            drawContent()
            val width = size.width
            val sweep = width * 1.6f
            val startX = -sweep + translate * (width + sweep)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        highlight.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(startX + sweep, size.height)
                ),
                size = Size(size.width, size.height)
            )
        }
}

/**
 * Count-up animation pour les nombres (solde wallet, compteurs stats).
 * Retourne la valeur courante interpolée — usage : `Text("$value FCFA")` dans le composable.
 *
 *  - `targetValue` change → animation se relance sur 1.2 s en spring smooth.
 *  - Sur le premier rendu, on démarre à 0 pour avoir l'effet "0 → N".
 */
@Composable
fun rememberCountUp(targetValue: Int, durationMs: Int = 1200): Int {
    var current by remember { mutableStateOf(0) }
    LaunchedEffect(targetValue) {
        val start = current
        val delta = targetValue - start
        if (delta == 0) return@LaunchedEffect
        val steps = 40
        val stepDelay = (durationMs / steps).toLong()
        for (i in 1..steps) {
            // Easing decelerate : ralentit en arrivant à la cible (effet "comptoir")
            val t = i / steps.toFloat()
            val eased = 1f - (1f - t) * (1f - t)
            current = start + (delta * eased).toInt()
            delay(stepDelay)
        }
        current = targetValue
    }
    return current
}

/** Helper : couleur de skeleton accordée au thème UNOVAPP. */
object UnovSkeleton {
    val Base: Color = Color(0xFF161616)
    val Highlight: Color = UnovColors.Accent.copy(alpha = 0.10f)
}

/**
 * Cascade d'entrée : chaque item apparaît avec fade + slide-up + scale, retardé selon son index.
 * Le délai est plafonné à [UnovMotion.StaggerMaxItems] pour éviter une cascade interminable
 * sur les longues listes.
 *
 * Usage : enveloppe le composable enfant avec `index` = position dans la liste.
 */
@Composable
fun StaggerReveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val cappedIndex = index.coerceAtMost(UnovMotion.StaggerMaxItems)

    LaunchedEffect(Unit) {
        delay((cappedIndex * UnovMotion.StaggerDelayMs).toLong())
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.94f,
        animationSpec = UnovMotion.smooth(),
        label = "staggerScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(UnovMotion.standard()) +
            slideInVertically(UnovMotion.decelerate()) { it / 4 },
        modifier = modifier
    ) {
        Box(modifier = Modifier.scale(scale)) {
            content()
        }
    }
}
