package com.unovapp.android.ui.feed

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.unovapp.android.R
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Couleurs du symbole UNOVAPP — les animations orbitent dans les teintes du logo. */
private val BrandTeal = Color(0xFF0E6B6B)
private val BrandTerra = Color(0xFFC65A2E)

/** Étincelle ambiante : position, taille, vitesse et teinte tirées une fois au hasard. */
private class AmbientSpark(rnd: Random) {
    val x = rnd.nextFloat()
    val startY = rnd.nextFloat()
    val radius = 1.2f + rnd.nextFloat() * 2.4f          // dp
    val speed = 0.35f + rnd.nextFloat() * 0.75f         // cycles de dérive par boucle
    val swayPhase = rnd.nextFloat() * 2f * PI.toFloat()
    val color = when (rnd.nextInt(3)) {
        0 -> BrandTeal
        1 -> BrandTerra
        else -> UnovColors.Accent
    }
}

/**
 * Écran de marque affiché AVANT le feed (premier chargement, aucun cache) : uniquement le
 * logo UNOVAPP, vivant — plus aucune icône « play » générique.
 *
 * Chorégraphie (toutes les animations sont infinies) :
 *  - le SYMBOLE respire (léger scale + flottement vertical) sur un halo doré qui pulse ;
 *  - deux ARCS ORBITAUX aux couleurs du logo (sarcelle / terracotta) tournent en sens
 *    opposés à des vitesses différentes — l'attente se lit comme un mouvement, pas un blocage ;
 *  - un fin anneau doré dérive lentement en arrière-plan des arcs ;
 *  - des ÉTINCELLES ambiantes dérivent sur tout l'écran (sarcelle/terracotta/or), avec
 *    fondu d'apparition/disparition — l'écran entier est vivant, pas seulement le centre.
 *
 * Canvas pur + graphicsLayer : aucune recomposition par frame, coût négligeable.
 */
@Composable
fun FeedPlaceholder(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    UnovAppTheme {
        val inf = rememberInfiniteTransition(label = "brandSplash")

        // Rotations opposées des deux arcs orbitaux.
        val orbitA by inf.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
            label = "orbitA"
        )
        val orbitB by inf.animateFloat(
            initialValue = 360f, targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing)),
            label = "orbitB"
        )
        // Respiration du logo + pulsation du halo (0 → 1 → 0).
        val breath by inf.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
            label = "breath"
        )
        // Horloge de dérive des étincelles ambiantes (boucle continue).
        val drift by inf.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(11_000, easing = LinearEasing)),
            label = "drift"
        )

        val sparks = remember { List(16) { AmbientSpark(Random(it * 131 + 7)) } }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            // ── Étincelles ambiantes sur tout l'écran ──────────────────────────────
            Canvas(modifier = Modifier.fillMaxSize()) {
                sparks.forEach { s ->
                    // Progression verticale individuelle (remonte lentement, boucle).
                    val p = ((s.startY + drift * s.speed) % 1f)
                    val y = size.height * (1f - p)
                    val sway = sin(2f * PI.toFloat() * p * 2f + s.swayPhase)
                    val x = size.width * s.x + 14.dp.toPx() * sway
                    // Fondu d'entrée/sortie aux bords du cycle.
                    val alpha = (sin(PI.toFloat() * p) * 0.5f).coerceIn(0f, 0.5f)
                    drawCircle(
                        color = s.color.copy(alpha = alpha),
                        radius = s.radius.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // ── Halo doré qui respire, sous le logo ────────────────────────────────
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer {
                        val sc = 0.9f + 0.18f * breath
                        scaleX = sc
                        scaleY = sc
                        alpha = 0.55f + 0.45f * breath
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(UnovColors.AccentGlow, Color.Transparent)
                        ),
                        CircleShape
                    )
            )

            // ── Fin anneau doré en dérive lente (fond des arcs) ───────────────────
            Canvas(modifier = Modifier.size(238.dp)) {
                rotate(orbitB / 2f) {
                    drawArc(
                        color = UnovColors.Accent.copy(alpha = 0.14f),
                        startAngle = 0f, sweepAngle = 300f, useCenter = false,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // ── Arcs orbitaux aux couleurs du logo ────────────────────────────────
            Canvas(modifier = Modifier.size(204.dp)) {
                val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                rotate(orbitA) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.28f to BrandTeal,
                            0.33f to Color.Transparent,
                            1f to Color.Transparent
                        ),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke
                    )
                }
                rotate(orbitB) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.5f to Color.Transparent,
                            0.78f to BrandTerra,
                            0.83f to Color.Transparent,
                            1f to Color.Transparent
                        ),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke
                    )
                }
            }

            // ── Le symbole UNOVAPP, respirant ─────────────────────────────────────
            Image(
                painter = painterResource(R.drawable.brand_symbol),
                contentDescription = "UNOVAPP",
                modifier = Modifier
                    .size(118.dp)
                    .graphicsLayer {
                        val sc = 1f + 0.05f * breath
                        scaleX = sc
                        scaleY = sc
                        translationY = -3.dp.toPx() * breath
                    }
            )
        }
    }
}
