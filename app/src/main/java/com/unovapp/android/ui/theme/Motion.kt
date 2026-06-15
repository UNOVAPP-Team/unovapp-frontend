package com.unovapp.android.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Vocabulaire d'animation UNOVAPP — tous les écrans puisent dans ces tokens pour rester
 * cohérents. Pas d'animations ad-hoc. Quand tu hésites entre deux specs, prends celui qui
 * communique le mieux le poids physique de l'élément animé.
 *
 *  - **Springs** pour tout ce qui réagit à l'utilisateur (tap, swipe, follow, like).
 *  - **Tween + easing** pour les transitions de page et les états déclaratifs.
 *  - **Durations** alignées sur Material 3 Expressive (cycle court 180 ms, standard 320 ms,
 *    long 500 ms, ambient = boucle infinie).
 */
object UnovMotion {

    /* ---------- Springs ---------- */

    /** Réaction rapide, sans rebond — pour les micro-interactions tap, hover, focus. */
    fun <T> snappy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Feedback marqué — pour les confirmations d'action (like, follow, success). */
    fun <T> bouncy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Mouvement organique — pour les bascules d'état et l'arrivée d'éléments lourds. */
    fun <T> smooth(): SpringSpec<T> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessLow
    )

    /** Léger sur-rebond — pour les apparitions héroïques (success state, gift). */
    fun <T> wobbly(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /* ---------- Durations (ms) ---------- */

    /** Micro-feedback instantané : tap scale, ripple custom, focus border. */
    const val DurationQuick = 180

    /** Transition standard : page enter/exit, modal slide, content swap. */
    const val DurationStandard = 320

    /** Transition lourde : reveal d'une grosse zone, header collapsing. */
    const val DurationSlow = 500

    /** Boucle ambient : pulse, flottement, shimmer, ring rotation. */
    const val DurationAmbient = 1400

    /* ---------- Easings ---------- */

    /**
     * Standard Material 3 Expressive — démarrage doux, accélération franche, fin posée.
     * Convient à 80% des transitions. Plus chaleureux que le linear/EaseInOut natifs.
     */
    val Standard: Easing = CubicBezierEasing(0.20f, 0.00f, 0.00f, 1.00f)

    /** Emphasized : insiste sur la fin du mouvement — révèle un contenu attendu. */
    val Emphasized: Easing = CubicBezierEasing(0.20f, 0.00f, 0.00f, 1.00f)

    /** Decelerate : ralentit fort à la fin — pour les éléments entrants depuis le hors-écran. */
    val Decelerate: Easing = CubicBezierEasing(0.00f, 0.00f, 0.20f, 1.00f)

    /** Accelerate : accélère vers la sortie — pour les éléments qui quittent l'écran. */
    val Accelerate: Easing = CubicBezierEasing(0.40f, 0.00f, 1.00f, 1.00f)

    /* ---------- Helpers tween préformés ---------- */

    fun <T> fast() = tween<T>(durationMillis = DurationQuick, easing = Standard)
    fun <T> standard() = tween<T>(durationMillis = DurationStandard, easing = Standard)
    fun <T> slow() = tween<T>(durationMillis = DurationSlow, easing = Standard)
    fun <T> decelerate(duration: Int = DurationStandard) =
        tween<T>(durationMillis = duration, easing = Decelerate)
    fun <T> accelerate(duration: Int = DurationStandard) =
        tween<T>(durationMillis = duration, easing = Accelerate)

    /* ---------- Stagger (cascade d'entrée sur les listes) ---------- */

    /** Délai entre items d'une liste qui apparaît. ~30 ms = sensation fluide sans traîner. */
    const val StaggerDelayMs = 30

    /** Cap sur le nombre d'items qu'on stagger — au-delà tout apparaît d'un coup. */
    const val StaggerMaxItems = 12
}

/**
 * Renvoie un multiplicateur de durée selon les Settings d'accessibilité de l'utilisateur.
 *  - 0 = animations désactivées (on rend les transitions instantanées).
 *  - 0.5 = animations réduites (Material guideline : à respecter).
 *  - 1 = normal.
 *
 * Usage : `tween((UnovMotion.DurationStandard * scale).toInt())`.
 */
@Composable
fun animationScale(): Float {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE
            )
        }.getOrDefault(1f)
    }
}

/** Variante qui clamp à un minimum lisible — évite les animations sub-100 ms peu perceptibles. */
@Composable
fun scaledDuration(baseMs: Int, min: Int = 0): Int {
    val scale = animationScale()
    return (baseMs * scale).toInt().coerceAtLeast(min)
}
