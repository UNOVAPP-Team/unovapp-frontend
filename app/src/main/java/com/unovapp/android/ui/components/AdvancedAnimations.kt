package com.unovapp.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ══════════════════════════════════════════════════════════════════════════════
// 1. GOLD SHIMMER — balayage lumineux doré sur n'importe quel élément
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Effet shimmer doré premium — un trait de lumière balaie l'élément de gauche à droite
 * en boucle infinie. Idéal pour les boutons primaires, les cartes dorées, les titres.
 *
 * Usage: `Modifier.goldShimmer()` sur n'importe quel composable.
 */
fun Modifier.goldShimmer(
    enabled: Boolean = true,
    durationMs: Int = 2000,
    highlightColor: Color = Color(0x60FFD08A)
): Modifier = composed {
    if (!enabled) return@composed this
    val transition = rememberInfiniteTransition(label = "goldShimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    this.drawWithContent {
        drawContent()
        val w = size.width
        val sweep = w * 0.45f
        val startX = offset * w - sweep
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    highlightColor,
                    Color.Transparent
                ),
                start = Offset(startX, 0f),
                end = Offset(startX + sweep, size.height)
            ),
            size = Size(w, size.height)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 2. AVATAR GLOW — halo doré pulsant autour de l'avatar
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Halo doré animé qui pulse autour d'un avatar. Effet de "présence en ligne" premium.
 * La couleur du glow peut être personnalisée (or par défaut).
 */
@Composable
fun AvatarGlow(
    modifier: Modifier = Modifier,
    glowColor: Color = UnovColors.Accent,
    enabled: Boolean = true
) {
    if (!enabled) return
    val transition = rememberInfiniteTransition(label = "avatarGlow")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val pulseScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = pulseAlpha),
                    glowColor.copy(alpha = pulseAlpha * 0.3f),
                    Color.Transparent
                ),
                radius = radius * pulseScale
            ),
            radius = radius * pulseScale,
            center = center
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 3. STATS COUNTER (ROLLUP) — chiffres qui s'incrémentent avec animation
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Compteur animé façon "odometre" — le chiffre défile de la valeur actuelle vers la cible
 * avec un easing decelerate. Utilisable directement dans un Text via le retour.
 *
 * @param targetValue la valeur cible
 * @param prefix préfixe optionnel (ex: "1.2" pour "1.2K abonnés")
 * @param suffix suffixe optionnel (ex: "K" ou " abonnés")
 */
@Composable
fun animatedCountUp(
    targetValue: Long,
    durationMs: Int = 1200,
    prefix: String = "",
    suffix: String = ""
): String {
    var current by remember { mutableStateOf(0L) }
    LaunchedEffect(targetValue) {
        val start = current
        val delta = targetValue - start
        if (delta == 0L) return@LaunchedEffect
        val steps = 40
        val stepDelay = (durationMs / steps).toLong()
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val eased = 1f - (1f - t) * (1f - t)
            current = start + (delta * eased).toLong()
            kotlinx.coroutines.delay(stepDelay)
        }
        current = targetValue
    }
    return "$prefix$current$suffix"
}

// ══════════════════════════════════════════════════════════════════════════════
// 4. RECORDING PULSE — cercle qui pulse pendant l'enregistrement vidéo
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Cercle de recording animé : un anneau rouge pulse en permanence + un point central
 * fixe. L'ensemble commique un enregistrement en cours.
 */
@Composable
fun RecordingPulse(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFF4444),
    isActive: Boolean = true
) {
    if (!isActive) return
    val transition = rememberInfiniteTransition(label = "recPulse")
    val ringScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            // Anneau pulsant
            drawCircle(
                color = color.copy(alpha = ringAlpha),
                radius = radius * ringScale * 0.45f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
            // Point central fixe
            drawCircle(
                color = color,
                radius = radius * 0.22f,
                center = center
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 5. CAPTION REVEAL — texte qui apparaît lettre par lettre (typewriter)
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Retourne la sous-chaîne animée du texte complet. Chaque lettre apparaît
 * progressivement avec un léger délai, effet typewriter premium.
 *
 * @param fullText le texte complet à révéler
 * @param msPerChar millisecondes entre chaque caractère
 */
@Composable
fun rememberTypewriterText(
    fullText: String,
    msPerChar: Int = 35,
    trigger: Boolean = true
): String {
    var revealedLength by remember(fullText, trigger) { mutableIntStateOf(0) }
    val targetLength = if (trigger) fullText.length else 0

    LaunchedEffect(fullText, trigger) {
        if (!trigger) {
            revealedLength = 0
            return@LaunchedEffect
        }
        revealedLength = 0
        for (i in 1..fullText.length) {
            revealedLength = i
            kotlinx.coroutines.delay(msPerChar.toLong())
        }
    }
    return fullText.take(revealedLength)
}

// ══════════════════════════════════════════════════════════════════════════════
// 6. COIN SHOWER — pluie de pièces dorées pour le wallet
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Pluie de pièces dorées animées. Chaque pièce a sa propre trajectoire, vitesse
 * et rotation. Déclenchée par un compteur [trigger] — incrémenter pour lancer
 * une nouvelle salve.
 */
@Composable
fun CoinShower(
    trigger: Int,
    modifier: Modifier = Modifier,
    coinCount: Int = 20,
    durationMs: Int = 1400
) {
    if (trigger <= 0) return
    val progress = remember { Animatable(0f) }
    var seed by remember { mutableStateOf(0) }

    LaunchedEffect(trigger) {
        seed = trigger
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }

    val coins = remember(seed) {
        val rnd = Random(seed * 3571 + 7)
        List(coinCount) {
            CoinParticle(
                startX = rnd.nextFloat(),
                fallSpeed = 0.3f + rnd.nextFloat() * 0.7f,
                swayAmplitude = (rnd.nextFloat() - 0.5f) * 60f,
                swayFrequency = 2f + rnd.nextFloat() * 3f,
                sizeDp = 8f + rnd.nextFloat() * 12f,
                rotation = rnd.nextFloat() * 360f,
                spinSpeed = (rnd.nextFloat() - 0.5f) * 720f,
                delay = rnd.nextFloat() * 0.3f
            )
        }
    }

    val p = progress.value
    if (p < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            coins.forEach { coin ->
                val adjustedP = ((p - coin.delay) / (1f - coin.delay)).coerceIn(0f, 1f)
                if (adjustedP <= 0f) return@forEach
                val fall = adjustedP * coin.fallSpeed
                val x = size.width * coin.startX + sin(adjustedP * coin.swayFrequency * PI.toFloat()) * coin.swayAmplitude
                val y = -coin.sizeDp.dp.toPx() + fall * (size.height + coin.sizeDp.dp.toPx())
                val alpha = (1f - adjustedP * adjustedP).coerceIn(0f, 1f)
                val rotation = coin.rotation + coin.spinSpeed * adjustedP

                // Pièce dorée (cercle avec contour)
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = alpha),
                    radius = coin.sizeDp.dp.toPx() / 2f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color(0xFFB8860B).copy(alpha = alpha * 0.6f),
                    radius = coin.sizeDp.dp.toPx() / 2f,
                    center = Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                )
                // Symbole dollar au centre
                drawCircle(
                    color = Color(0xFFDAA520).copy(alpha = alpha * 0.8f),
                    radius = coin.sizeDp.dp.toPx() / 5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private class CoinParticle(
    val startX: Float,
    val fallSpeed: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val sizeDp: Float,
    val rotation: Float,
    val spinSpeed: Float,
    val delay: Float
)

// ══════════════════════════════════════════════════════════════════════════════
// 7. BALANCE FLIP — rotation 3D sur le solde du wallet
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Wrapper qui applique un effet de rotation 3D (flip vertical) sur son contenu.
 * Déclenché par [trigger] — incrémenter pour lancer un flip.
 */
@Composable
fun BalanceFlip(
    trigger: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val flipRotation = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            flipRotation.snapTo(0f)
            flipRotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = flipRotation.value
            cameraDistance = 12f * density
        }
    ) {
        content()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 8. WATERMARK FLOAT — filigrane qui dérive lentement en arrière-plan
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Filigrane "UNOVAPP" qui dérive lentement en diagonale dans l'arrière-plan.
 * Mouvement ambiant infini — effet de luxe subtil.
 */
@Composable
fun WatermarkFloat(
    modifier: Modifier = Modifier,
    text: String = "UNOVAPP",
    alpha: Float = 0.04f,
    color: Color = UnovColors.Accent
) {
    val transition = rememberInfiniteTransition(label = "watermark")
    val offsetX by transition.animateFloat(
        initialValue = -200f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wmX"
    )
    val offsetY by transition.animateFloat(
        initialValue = -100f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wmY"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.argb(
                (alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            textSize = 48f * density
            isFakeBoldText = true
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        drawIntoCanvas { canvas ->
            for (row in -1..3) {
                for (col in -1..3) {
                    val x = col * 300f * density + offsetX
                    val y = row * 200f * density + offsetY
                    canvas.nativeCanvas.drawText(text, x, y, paint)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 9. LOADING SKELETON GOLD — squelettes avec shimmer doré premium
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Squelette de chargement premium avec shimmer doré (au lieu du gris standard).
 * Balayage de lumière doré de gauche à droite sur fond sombre.
 */
@Composable
fun GoldShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "goldShimmer")
    val x by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            UnovColors.Surface,
            UnovColors.Accent.copy(alpha = 0.12f),
            UnovColors.Surface
        ),
        start = Offset(x, 0f),
        end = Offset(x + 400f, 0f)
    )
    Box(modifier = modifier.drawWithContent {
        drawRect(brush)
    })
}

// ══════════════════════════════════════════════════════════════════════════════
// 10. GRID STAGGER — cascade décalée pour les grilles de vidéos
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Wrapper qui révèle un item de grille avec un delay calculé par colonne et ligne,
 * créant un effet cascade diagonal (stagger diagonal).
 *
 * @param row ligne dans la grille
 * @param col colonne dans la grille
 * @param baseDelayMs délai de base entre chaque item
 */
@Composable
fun GridStaggerReveal(
    row: Int,
    col: Int,
    baseDelayMs: Int = 60,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val delayMs = ((row + col) * baseDelayMs).toLong()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "gridScale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            alpha = if (visible) 1f else 0f
            scaleX = scale
            scaleY = scale
            translationY = (1f - scale) * 20f
        }
    ) {
        content()
    }
}
