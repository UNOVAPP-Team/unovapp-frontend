package com.unovapp.android.ui.feed

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.animation.core.LinearEasing
import com.unovapp.android.ui.theme.UnovColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Réactions UNOVAPP — 7 stickers, façon Facebook/LinkedIn.
 *
 * Les icônes vectorielles maison (teintées orange) servent pour les réactions « signature »
 * (J'aime, J'adore, Bravo) ; les réactions émotionnelles (Fou rire, Solidaire, Wouah, Triste)
 * utilisent l'**émoji système** ([emoji] non-null), car aucune icône Material ne rend
 * fidèlement l'émotion — et c'est ce que les utilisateurs reconnaissent instantanément.
 *
 * Le backend ne connaît qu'un like booléen : le type choisi est mémorisé côté client
 * ([ReactionMemory]) et envoyé dans `reaction` quand l'API le supportera.
 */
enum class Reaction(val icon: ImageVector, val label: String, val color: Color, val emoji: String? = null) {
    Like(Icons.Filled.ThumbUp, "J'aime", UnovColors.Accent),
    Love(Icons.Filled.Favorite, "J'adore", Color(0xFFFF4D6A)),
    Haha(Icons.Filled.SentimentVerySatisfied, "Fou rire", Color(0xFFFFC24D), emoji = "😂"),
    Care(Icons.Filled.VolunteerActivism, "Solidaire", Color(0xFFFFB35C), emoji = "🤗"),
    Wow(Icons.Filled.Bolt, "Wouah", Color(0xFFFFD166), emoji = "😮"),
    Sad(Icons.Filled.Favorite, "Triste", Color(0xFF7FB3FF), emoji = "😢"),
    Celebrate(Icons.Filled.Celebration, "Bravo", UnovColors.AccentLight)
}

private val BackEasing = Easing { t -> val s = 1.9f; val u = t - 1f; u * u * ((s + 1f) * u + s) + 1f }

/* ---------- Animation continue des stickers (façon LinkedIn) ---------- */

/** État de mouvement d'un sticker à l'instant t de sa boucle. */
private data class IdleMotion(
    val scale: Float = 1f,
    val translateY: Float = 0f,   // en dp
    val translateX: Float = 0f,   // en dp
    val rotation: Float = 0f      // en degrés
)

/** Période de la boucle — chaque émotion a son propre tempo. */
private fun idleDurationMs(r: Reaction): Int = when (r) {
    Reaction.Like -> 1800
    Reaction.Love -> 1500
    Reaction.Haha -> 1300
    Reaction.Care -> 2600
    Reaction.Wow -> 2000
    Reaction.Sad -> 3000
    Reaction.Celebrate -> 1600
}

/**
 * Mouvement du sticker à la progression [p] (0..1, boucle). Chaque courbe est choisie pour
 * *jouer* l'émotion : le pouce tape, le cœur bat, l'émoji rit, l'étonné sursaute, le triste
 * s'affaisse. Les fonctions sont pures et sans allocation → sûres à appeler à chaque frame.
 */
private fun idleMotion(r: Reaction, p: Float): IdleMotion {
    val tau = (2.0 * PI).toFloat()
    return when (r) {
        // Le pouce bascule vers l'avant puis revient (comme s'il « likait »).
        Reaction.Like -> IdleMotion(
            rotation = -14f * sin(tau * p).coerceAtLeast(0f),
            translateY = -1.5f * sin(tau * p).coerceAtLeast(0f)
        )
        // Battement de cœur : deux pulsations rapprochées puis repos (systole/diastole).
        Reaction.Love -> {
            val beat = when {
                p < 0.12f -> sin(PI.toFloat() * (p / 0.12f))
                p in 0.18f..0.30f -> 0.6f * sin(PI.toFloat() * ((p - 0.18f) / 0.12f))
                else -> 0f
            }
            IdleMotion(scale = 1f + 0.22f * beat)
        }
        // Se tord de rire : secousses latérales + roulis.
        Reaction.Haha -> IdleMotion(
            rotation = 12f * sin(tau * 2f * p),
            translateX = 1.6f * sin(tau * 2f * p),
            scale = 1f + 0.05f * sin(tau * p)
        )
        // Solidaire : respiration lente et chaleureuse.
        Reaction.Care -> IdleMotion(scale = 1f + 0.09f * sin(tau * p))
        // Wouah : sursaut d'étonnement bref, puis pause (l'attente crée la surprise).
        Reaction.Wow -> {
            val pop = if (p < 0.25f) sin(PI.toFloat() * (p / 0.25f)) else 0f
            IdleMotion(scale = 1f + 0.20f * pop, translateY = -2.5f * pop)
        }
        // Triste : s'affaisse lentement puis se redresse.
        Reaction.Sad -> {
            val droop = (1f - cos(tau * p)) / 2f     // 0 → 1 → 0, très doux
            IdleMotion(translateY = 2.5f * droop, rotation = -6f * droop, scale = 1f - 0.04f * droop)
        }
        // Bravo : rebond vertical enthousiaste (chute rapide, remontée souple).
        Reaction.Celebrate -> {
            val hop = abs(sin(PI.toFloat() * p))
            IdleMotion(translateY = -4f * hop, scale = 1f + 0.06f * hop)
        }
    }
}

/**
 * Bouton de réaction multi-choix (LinkedIn-like) :
 *  - tap court → applique/retire la réaction par défaut (J'aime),
 *  - appui long → ouvre la barre de réactions premium (cascade + bob + magnify),
 *  - tap sur une réaction → la sélectionne.
 */
@Composable
fun ReactionAction(
    current: Reaction?,
    countFmt: String,
    onSelect: (Reaction?) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // Petit pop quand la réaction change.
    val pop by animateFloatAsState(
        targetValue = if (current != null) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "reactionPop"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        // Keyé sur `current` : sans ça, le bloc pointerInput ne s'exécute qu'une fois
        // et la lambda onTap capture un `current` figé (toujours null) → unlike impossible.
        modifier = modifier.pointerInput(current) {
            detectTapGestures(
                onTap = { onSelect(if (current == null) Reaction.Love else null) },
                onLongPress = { pickerOpen = true }
            )
        }
    ) {
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            if (current == null) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Réagir",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            } else if (current.emoji != null) {
                Text(
                    text = current.emoji,
                    fontSize = 26.sp,
                    modifier = Modifier.graphicsLayer { scaleX = pop; scaleY = pop }
                )
            } else {
                Icon(
                    imageVector = current.icon,
                    contentDescription = current.label,
                    tint = current.color,
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer { scaleX = pop; scaleY = pop }
                )
            }
        }
        // Compteur toujours visible (épuré, façon TikTok) — la valeur GLISSE quand elle
        // change (l'ancien chiffre sort par le haut, le nouveau entre par le bas).
        androidx.compose.animation.AnimatedContent(
            targetState = countFmt,
            transitionSpec = {
                (androidx.compose.animation.slideInVertically { it } +
                    androidx.compose.animation.fadeIn(tween(160))) togetherWith
                    (androidx.compose.animation.slideOutVertically { -it } +
                        androidx.compose.animation.fadeOut(tween(160)))
            },
            label = "reactionCount"
        ) { value ->
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xB3000000), blurRadius = 8f
                    )
                )
            )
        }
    }

    if (pickerOpen) {
        val gapPx = with(density) { 12.dp.roundToPx() }
        val marginPx = with(density) { 8.dp.roundToPx() }
        val positionProvider = remember(gapPx, marginPx) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    // Barre au-dessus du bouton, alignée à droite (s'étend vers la gauche).
                    val x = (anchorBounds.right - popupContentSize.width).coerceIn(marginPx, (windowSize.width - popupContentSize.width).coerceAtLeast(marginPx))
                    val y = (anchorBounds.top - popupContentSize.height - gapPx).coerceAtLeast(marginPx)
                    return IntOffset(x, y)
                }
            }
        }
        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = { pickerOpen = false }
        ) {
            ReactionTray(onPick = { r ->
                onSelect(r)
                pickerOpen = false
            })
        }
    }
}

@Composable
private fun ReactionTray(onPick: (Reaction) -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val containerScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "trayScale"
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = containerScale
                scaleY = containerScale
                alpha = if (shown) 1f else 0f
            }
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF1C1C1E))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Reaction.values().forEachIndexed { index, reaction ->
            ReactionBubble(reaction = reaction, index = index, shown = shown, onClick = { onPick(reaction) })
        }
    }
}

/**
 * Sticker du picker, **animé en permanence** façon LinkedIn : chaque réaction rejoue en boucle
 * un mouvement qui exprime son émotion, désynchronisé des autres (décalage par index) pour que
 * la barre soit vivante sans être épileptique.
 *
 *  - J'aime : le pouce bascule (rotation) comme s'il tapait.
 *  - J'adore : battement de cœur (double pulsation).
 *  - Fou rire : secousse latérale + roulis, l'émoji « se tord de rire ».
 *  - Solidaire : respiration lente et chaleureuse (scale doux).
 *  - Wouah : sursauts d'étonnement (pop rapide puis pause).
 *  - Triste : affaissement lent (descend puis remonte).
 *  - Bravo : rebond vertical enthousiaste.
 */
@Composable
private fun ReactionBubble(reaction: Reaction, index: Int, shown: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Entrée en cascade avec overshoot.
    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 320, delayMillis = index * 45, easing = BackEasing),
        label = "bubbleEnter$index"
    )

    // Horloge continue propre à chaque sticker (période et déphasage distincts).
    val inf = rememberInfiniteTransition(label = "idle$index")
    val t by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(idleDurationMs(reaction), easing = LinearEasing),
            RepeatMode.Restart,
            initialStartOffset = StartOffset(index * 110)
        ),
        label = "idleT$index"
    )
    val idle = remember(reaction) { { p: Float -> idleMotion(reaction, p) } }(t)

    // Magnify quand on presse (façon dock).
    val magnify by animateFloatAsState(
        targetValue = if (pressed) 1.45f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "magnify$index"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                alpha = entrance.coerceIn(0f, 1f)
                val s = (0.4f + 0.6f * entrance) * magnify * idle.scale
                scaleX = s
                scaleY = s
                translationY = idle.translateY * density
                translationX = idle.translateX * density
                rotationZ = idle.rotation
            }
            .size(40.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (reaction.emoji != null) {
            Text(text = reaction.emoji, fontSize = 20.sp)
        } else {
            Icon(
                imageVector = reaction.icon,
                contentDescription = reaction.label,
                tint = reaction.color,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
