package com.unovapp.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unovapp.android.ui.theme.UnovColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Moteur d'« explosions » UNOVAPP — salve de particules or/orange dessinée au Canvas
 * (zéro dépendance, une seule frame d'allocation par salve).
 *
 * Physique volontairement simple mais crédible :
 *  - expansion radiale décélérée (easeOutQuad) → l'énergie du départ, l'amorti de la fin ;
 *  - gravité douce en fin de course → les étincelles « retombent » ;
 *  - rotation propre des éclats rectangulaires + rétrécissement/fondu progressifs.
 *
 * Usage : superposer `ParticleBurst(trigger)` au contenu (fillMaxSize) et incrémenter
 * [trigger] pour déclencher une salve centrée. 0 = inerte, aucun coût de rendu.
 */
@Composable
fun ParticleBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    particleCount: Int = 28,
    maxRadius: Dp = 80.dp,
    durationMs: Int = 700,
    colors: List<Color> = listOf(
        UnovColors.Accent,
        UnovColors.AccentLight,
        UnovColors.AccentDeep,
        Color(0xFFFFD08A)
    )
) {
    val progress = remember { Animatable(1f) } // 1f = salve terminée (rien à dessiner)
    var seed by remember { mutableIntStateOf(0) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            seed = trigger
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
        }
    }

    // Une salve = un jeu de particules figé (regénéré à chaque trigger via seed).
    val particles = remember(seed) {
        val rnd = Random(seed * 7919 + 13)
        List(particleCount) {
            BurstParticle(
                angle = rnd.nextFloat() * 2f * PI.toFloat(),
                speed = 0.45f + rnd.nextFloat() * 0.55f,
                sizeDp = 2f + rnd.nextFloat() * 3.5f,
                color = colors[rnd.nextInt(colors.size)],
                isSpark = rnd.nextFloat() < 0.4f,
                spin = (rnd.nextFloat() - 0.5f) * 720f
            )
        }
    }

    val p = progress.value
    if (p < 1f) {
        Canvas(modifier) {
            val expand = 1f - (1f - p) * (1f - p)          // easeOutQuad — départ explosif
            val fade = (1f - p * p).coerceIn(0f, 1f)       // fondu tardif
            val gravity = p * p * 26.dp.toPx()             // retombée douce
            particles.forEach { pt ->
                val dist = expand * pt.speed * maxRadius.toPx()
                val x = center.x + cos(pt.angle) * dist
                val y = center.y + sin(pt.angle) * dist + gravity
                val s = pt.sizeDp.dp.toPx() * (1f - 0.45f * p)
                if (pt.isSpark) {
                    // Éclat rectangulaire qui tournoie (étincelle).
                    rotate(degrees = pt.spin * p, pivot = Offset(x, y)) {
                        drawRect(
                            color = pt.color.copy(alpha = fade),
                            topLeft = Offset(x - s, y - s * 0.35f),
                            size = Size(s * 2f, s * 0.7f)
                        )
                    }
                } else {
                    drawCircle(color = pt.color.copy(alpha = fade), radius = s, center = Offset(x, y))
                }
            }
        }
    }
}

private class BurstParticle(
    val angle: Float,
    val speed: Float,
    val sizeDp: Float,
    val color: Color,
    val isSpark: Boolean,
    val spin: Float
)
