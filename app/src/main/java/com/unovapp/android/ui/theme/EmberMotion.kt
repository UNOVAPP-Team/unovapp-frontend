package com.unovapp.android.ui.theme

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ══════════════════════════════════════════════════════════════════════════
 *  EmberMotion — la grammaire du mouvement de « La braise, pas le bruit ».
 *
 *  Source unique de vérité pour l'animation (cf. SPEC_ANIMATION). Toute durée,
 *  courbe et primitive vient d'ici. Les règles absolues sont câblées :
 *   · R5 — la lagune (IA) apparaît en fondu seul, jamais en glissement.
 *   · R7 — micro-retours ≤ 200 ms, entrées ≤ 400 ms, récit ≤ 900 ms.
 *   · Repos = rien ne bouge sauf `breathe` et la vidéo.
 *   · `emberBurst` est le seul usage de `EaseSnap` (léger dépassement).
 *  Accessibilité : `prefers-reduced-motion` et le mode Éco dégradent tout ici.
 * ══════════════════════════════════════════════════════════════════════════ */

object EmberMotion {
    // §2.1 — Durées (ms).
    const val DurInstant = 90
    const val DurQuick = 160
    const val DurBase = 240
    const val DurSheet = 320
    const val DurSlow = 520
    const val DurStory = 900
    const val DurBurst = 420

    // §2.2 — Courbes.
    val EaseOut = CubicBezierEasing(0.22f, 0.9f, 0.28f, 1f)
    val EaseIn = CubicBezierEasing(0.5f, 0f, 0.9f, 0.3f)
    val EaseSoft = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val EaseBreath = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
    val EaseSnap = CubicBezierEasing(0.2f, 1.3f, 0.4f, 1f)
}

/** Vrai si le système demande une réduction des animations (accessibilité §8). */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Vrai en mode Éco / appareil lent : on coupe les boucles et le stagger (§7). */
val LocalEcoMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        val scale = try {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } catch (_: Throwable) { 1f }
        scale == 0f
    }
}

/* ---------- Haptique (canal distinct, conservé même en reduced-motion §8) ---------- */

object Haptics {
    fun light(v: View) = v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    fun medium(v: View) = v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    fun tick(v: View) = v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    fun error(v: View) = v.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
    )
}

/* ---------- Stagger (§2.4, plafonné à 8 items, coupé en Éco/reduced) ---------- */

@Composable
fun staggerDelay(index: Int, stepMs: Int = 40, cap: Int = 7): Int {
    val reduced = LocalReducedMotion.current
    val eco = LocalEcoMotion.current
    if (reduced || eco) return 0
    return min(index, cap) * stepMs
}

/* ══════════════════ 3.2/3.3/3.4 — Entrées (riseIn, revealAI, arcOpen) ══════════════════
 * Une seule mécanique paramétrée. En reduced-motion : opacité seule, 160 ms, sans
 * translation ni échelle. `revealAI` = dx=dy=0 & scale=1 → fondu pur (R5). */

@Composable
fun Modifier.appear(
    dxFrom: Dp = 0.dp,
    dyFrom: Dp = 14.dp,
    scaleFrom: Float = 1f,
    durationMs: Int = EmberMotion.DurBase,
    easing: Easing = EmberMotion.EaseOut,
    delayMs: Int = 0,
    key: Any? = Unit
): Modifier {
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        if (delayMs > 0) delay(delayMs.toLong())
        p.animateTo(1f, tween(if (reduced) EmberMotion.DurQuick else durationMs, easing = easing))
    }
    val dxPx = with(density) { dxFrom.toPx() }
    val dyPx = with(density) { dyFrom.toPx() }
    return this.graphicsLayer {
        val v = p.value
        alpha = v
        if (!reduced) {
            translationX = dxPx * (1f - v)
            translationY = dyPx * (1f - v)
            val s = scaleFrom + (1f - scaleFrom) * v
            scaleX = s
            scaleY = s
        }
    }
}

/** Entrée standard — 14 px, 240 ms (§3.2). */
@Composable
fun Modifier.riseIn(delayMs: Int = 0, key: Any? = Unit) =
    appear(dyFrom = 14.dp, delayMs = delayMs, key = key)

/** Entrée d'un petit élément (chip, ligne) — 8 px. */
@Composable
fun Modifier.riseInSmall(delayMs: Int = 0, key: Any? = Unit) =
    appear(dyFrom = 8.dp, delayMs = delayMs, key = key)

/** Entrée d'une section/panneau — 20 px. */
@Composable
fun Modifier.riseInSection(delayMs: Int = 0, key: Any? = Unit) =
    appear(dyFrom = 20.dp, delayMs = delayMs, key = key)

/** Apparition lagune (IA) — opacité seule, 320 ms, aucune translation (R5, §3.3). */
@Composable
fun Modifier.revealAI(delayMs: Int = 0, key: Any? = Unit) =
    appear(dxFrom = 0.dp, dyFrom = 0.dp, scaleFrom = 1f, durationMs = EmberMotion.DurSheet, delayMs = delayMs, key = key)

/** Dépliement d'arc — grandit depuis l'origine (18/10 px, scale .82), 240 ms (§3.4). */
@Composable
fun Modifier.arcOpen(delayMs: Int = 0, key: Any? = Unit) =
    appear(dxFrom = 18.dp, dyFrom = 10.dp, scaleFrom = 0.82f, delayMs = delayMs, key = key)

/* ══════════════════ 3.5 — sheetUp (feuille remontante) ══════════════════ */

@Composable
fun Modifier.sheetUp(delayMs: Int = 0, key: Any? = Unit): Modifier {
    val reduced = LocalReducedMotion.current
    val p = remember(key) { Animatable(0f) }
    var h by remember(key) { mutableStateOf(0f) }
    LaunchedEffect(key) {
        if (delayMs > 0) delay(delayMs.toLong())
        p.animateTo(1f, tween(if (reduced) EmberMotion.DurQuick else EmberMotion.DurSheet, easing = EmberMotion.EaseOut))
    }
    return this
        .onSizeChanged { h = it.height.toFloat() }
        .graphicsLayer {
            if (reduced) {
                alpha = p.value
            } else {
                translationY = h * (1f - p.value)
            }
        }
}

/* ══════════════════ 3.1 — breathe (la seule boucle au repos, R2) ══════════════════ */

@Composable
fun Modifier.breathe(
    amplitude: Float = 1.035f,
    minAlpha: Float = 0.75f,
    periodMs: Int = 3400,
    enabled: Boolean = true
): Modifier {
    val reduced = LocalReducedMotion.current
    val eco = LocalEcoMotion.current
    if (!enabled || reduced || eco) return this // état statique
    val t = rememberInfiniteTransition(label = "breathe")
    val v by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(periodMs, easing = EmberMotion.EaseBreath), RepeatMode.Reverse),
        label = "breatheV"
    )
    return this.graphicsLayer {
        val s = 1f + (amplitude - 1f) * v
        scaleX = s
        scaleY = s
        alpha = minAlpha + (1f - minAlpha) * v
    }
}

/* ══════════════════ 3.11 — emberSkeleton (la braise qui couve) ══════════════════ */

@Composable
fun Modifier.emberSkeleton(): Modifier {
    val reduced = LocalReducedMotion.current
    if (reduced) return this.graphicsLayer { alpha = 0.45f }
    val t = rememberInfiniteTransition(label = "skeleton")
    val v by t.animateFloat(
        0.28f, 0.62f,
        infiniteRepeatable(tween(1600, easing = EmberMotion.EaseBreath), RepeatMode.Reverse),
        label = "skeletonV"
    )
    return this.graphicsLayer { alpha = v }
}

/* ══════════════════ 3.6/3.10 — progression animée (ringFill, waveDraw, barres) ══════════════════
 * Renvoie une valeur 0→target qui s'anime à l'apparition ; le tracé est fait par
 * l'appelant (Canvas). En reduced-motion : état final immédiat. */

@Composable
fun animatedProgress(
    target: Float,
    durationMs: Int = EmberMotion.DurSlow,
    easing: Easing = EmberMotion.EaseOut,
    delayMs: Int = 0,
    key: Any? = target
): State<Float> {
    val reduced = LocalReducedMotion.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (reduced) {
            p.snapTo(target)
        } else {
            if (delayMs > 0) delay(delayMs.toLong())
            p.animateTo(target, tween(durationMs, easing = easing))
        }
    }
    return p.asState()
}

/* ══════════════════ 3.9 — countUp (compteurs interpolés) ══════════════════ */

@Composable
fun countUp(
    target: Int,
    fromZero: Boolean = true,
    durationMs: Int = EmberMotion.DurSlow
): Int {
    val reduced = LocalReducedMotion.current
    val a = remember { Animatable((if (fromZero) 0 else target).toFloat()) }
    LaunchedEffect(target) {
        if (reduced) a.snapTo(target.toFloat())
        else a.animateTo(target.toFloat(), tween(durationMs, easing = EmberMotion.EaseOut))
    }
    return a.value.roundToInt()
}

/* ══════════════════ 3.7 — emberBurst (confirmation de Braise, seul EaseSnap) ══════════════════ */

@Composable
fun Modifier.emberBurst(trigger: Int, ringColor: Color = Ember.Braise): Modifier {
    val reduced = LocalReducedMotion.current
    val scale = remember { Animatable(1f) }
    val ring = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        if (reduced) return@LaunchedEffect
        ring.snapTo(0f)
        launch {
            scale.snapTo(1f)
            scale.animateTo(1.12f, tween((EmberMotion.DurBurst * 0.4f).toInt(), easing = EmberMotion.EaseSnap))
            scale.animateTo(1f, tween((EmberMotion.DurBurst * 0.6f).toInt(), easing = EmberMotion.EaseSnap))
        }
        ring.animateTo(1f, tween(EmberMotion.DurBurst, easing = EmberMotion.EaseOut))
    }
    return this
        .drawBehind {
            val r = ring.value
            if (r in 0.001f..0.999f) {
                drawCircle(
                    color = ringColor.copy(alpha = 0.5f * (1f - r)),
                    radius = size.maxDimension / 2f + 14.dp.toPx() * r,
                    style = Stroke(2.dp.toPx())
                )
            }
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
}

/* ══════════════════ 3.8 — glowTouch (la Lueur : souffle bref et blanc) ══════════════════ */

@Composable
fun GlowPulse(
    trigger: Int,
    modifier: Modifier = Modifier,
    color: Color = Ember.BraisePale2,
    diameter: Dp = 40.dp
) {
    val reduced = LocalReducedMotion.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        p.snapTo(0f)
        if (!reduced) p.animateTo(1f, tween(EmberMotion.DurSlow, easing = EmberMotion.EaseOut))
        else { p.snapTo(1f); p.snapTo(0f) }
    }
    if (p.value in 0.001f..0.999f) {
        Canvas(modifier) {
            val prog = p.value
            val gscale = 0.6f + 0.9f * prog // .6 → 1.5
            val galpha = if (prog < 0.3f) (prog / 0.3f) * 0.9f else 0.9f * (1f - (prog - 0.3f) / 0.7f)
            drawCircle(
                color = color.copy(alpha = galpha.coerceIn(0f, 0.9f)),
                radius = diameter.toPx() / 2f * gscale,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
 *  LA CASCADE AUTOMATIQUE — pour animer « chaque élément de chaque écran »
 *  sans câbler un délai à la main sur des centaines d'appels.
 *
 *  `EmberCascade { … }` ouvre une portée : chaque `Modifier.emberEnter()`
 *  descendant réclame son index UNE seule fois (à sa première composition,
 *  donc dans l'ordre visuel) et en déduit son délai de cascade. Un écran
 *  entier se chorégraphie alors en enveloppant sa racine.
 *
 *  Le compteur est un `Int` nu (pas du state) : le réclamer ne déclenche
 *  aucune recomposition. Le plafond de 8 items de §2.4 reste appliqué.
 * ══════════════════════════════════════════════════════════════════════════ */

class CascadeScope(val stepMs: Int) {
    private var next = 0
    fun claim(): Int = next++
    /** Repart de zéro — à appeler quand l'écran recharge un nouveau jeu de données. */
    fun reset() { next = 0 }
}

val LocalCascade = staticCompositionLocalOf<CascadeScope?> { null }

/**
 * Ouvre une portée de cascade. [step] = délai entre items (§2.4 : 40 ms pour une
 * liste, 50 ms pour une grille, 60 ms pour des sections d'écran).
 * [key] relance la cascade quand il change (nouveau chargement).
 */
@Composable
fun EmberCascade(step: Int = 40, key: Any? = Unit, content: @Composable () -> Unit) {
    val scope = remember(key) { CascadeScope(step) }
    CompositionLocalProvider(LocalCascade provides scope) { content() }
}

/**
 * Entrée universelle : se cascade toute seule si elle est dans un [EmberCascade],
 * sinon entre immédiatement. C'est le modifieur à poser sur n'importe quel élément
 * d'écran pour qu'il « arrive » au lieu d'apparaître sec.
 */
@Composable
fun Modifier.emberEnter(
    dyFrom: Dp = 14.dp,
    scaleFrom: Float = 1f,
    durationMs: Int = EmberMotion.DurBase,
    extraDelayMs: Int = 0
): Modifier {
    val scope = LocalCascade.current
    val index = remember { scope?.claim() ?: 0 }
    val step = scope?.stepMs ?: 40
    return appear(
        dyFrom = dyFrom,
        scaleFrom = scaleFrom,
        durationMs = durationMs,
        delayMs = staggerDelay(index, step) + extraDelayMs
    )
}

/** Variante lagune de la cascade : fondu pur, jamais de translation (R5). */
@Composable
fun Modifier.emberEnterAI(extraDelayMs: Int = 0): Modifier =
    emberEnter(dyFrom = 0.dp, durationMs = EmberMotion.DurSheet, extraDelayMs = extraDelayMs)

/* ══════════════════ Le retour tactile universel (§ « le mouvement suit le doigt ») ══════════════════
 * Toute zone tappable devrait le porter : contraction 90 ms au doigt (--dur-instant),
 * détente EaseOut, haptique léger. Jamais coupé, même en reduced-motion (§8 : le retour
 * d'action est un canal indispensable) — seule l'échelle est neutralisée. */

@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    haptic: Boolean = true
): Modifier {
    val reduced = LocalReducedMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduced) pressedScale else 1f,
        animationSpec = tween(
            if (pressed) EmberMotion.DurInstant else EmberMotion.DurQuick,
            easing = EmberMotion.EaseOut
        ),
        label = "pressableScale"
    )
    LaunchedEffect(pressed) {
        if (pressed && enabled && haptic) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

/** Sélection animée : la bordure/teinte d'un élément actif transite au lieu de sauter. */
@Composable
fun animatedAccent(active: Boolean, activeColor: Color, idleColor: Color, durationMs: Int = EmberMotion.DurQuick): Color {
    val c by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) activeColor else idleColor,
        animationSpec = tween(durationMs, easing = EmberMotion.EaseOut),
        label = "animatedAccent"
    )
    return c
}

/* ══════════════════ Le doigt fantôme (leçon de geste — Onb 01/02/10) ══════════════════
 * Descend depuis le haut, effleure/maintient au centre de son cadre, remonte, boucle
 * avec pause. S'arrête dès que [active] passe à false (geste appris) ou après
 * [maxLoops]. En reduced-motion : rien (la consigne texte prend le relais §8). */

@Composable
fun GhostFinger(
    active: Boolean,
    modifier: Modifier = Modifier,
    hold: Boolean = false,
    maxLoops: Int = Int.MAX_VALUE,
    holdMs: Int = EmberMotion.DurStory,
    ringRadius: Dp = 60.dp,
    pauseMs: Long = 1400L
) {
    val reduced = LocalReducedMotion.current
    if (!active || reduced) return
    val travel = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(active, hold) {
        var loop = 0
        while (loop < maxLoops) {
            travel.snapTo(0f); glow.snapTo(0f); fill.snapTo(0f); burst.snapTo(0f)
            travel.animateTo(1f, tween(EmberMotion.DurStory, easing = EmberMotion.EaseSoft))
            launch { glow.snapTo(0f); glow.animateTo(1f, tween(EmberMotion.DurSlow, easing = EmberMotion.EaseOut)) }
            if (hold) {
                fill.animateTo(1f, tween(holdMs, easing = EmberMotion.EaseOut))
                launch { burst.snapTo(0f); burst.animateTo(1f, tween(EmberMotion.DurBurst, easing = EmberMotion.EaseSnap)) }
                delay(EmberMotion.DurBurst.toLong())
            } else {
                delay(EmberMotion.DurQuick.toLong())
            }
            travel.animateTo(0f, tween(EmberMotion.DurStory, easing = EmberMotion.EaseSoft))
            delay(pauseMs)
            loop++
        }
    }
    Canvas(modifier) {
        val cx = size.width / 2f
        val contactY = size.height / 2f
        val topY = -24.dp.toPx()
        val y = topY + (contactY - topY) * travel.value
        val rr = ringRadius.toPx()
        // Anneau de maintien (Braise).
        if (hold && fill.value > 0.001f) {
            drawArc(
                color = Ember.Braise,
                startAngle = -90f, sweepAngle = 360f * fill.value, useCenter = false,
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(cx - rr, contactY - rr), size = Size(rr * 2, rr * 2)
            )
        }
        // emberBurst en fin de maintien.
        if (burst.value in 0.001f..0.999f) {
            drawCircle(
                color = Ember.Braise.copy(alpha = 0.5f * (1f - burst.value)),
                radius = rr + 14.dp.toPx() * burst.value,
                center = Offset(cx, contactY), style = Stroke(2.dp.toPx())
            )
        }
        // glowTouch au contact.
        if (glow.value in 0.001f..0.999f) {
            val prog = glow.value
            val gscale = 0.6f + 0.9f * prog
            val galpha = if (prog < 0.3f) (prog / 0.3f) * 0.9f else 0.9f * (1f - (prog - 0.3f) / 0.7f)
            drawCircle(Ember.BraisePale2.copy(alpha = galpha.coerceIn(0f, 0.9f)), radius = 22.dp.toPx() * gscale, center = Offset(cx, contactY))
        }
        // Le doigt.
        drawCircle(Color(0x22FFE9CF), radius = 30.dp.toPx(), center = Offset(cx, y))
        drawCircle(Color(0x59FFE9CF), radius = 22.dp.toPx(), center = Offset(cx, y))
    }
}
