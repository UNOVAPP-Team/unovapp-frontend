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
import com.unovapp.android.ui.theme.UnovColors

/** Jeu de réactions façon LinkedIn. */
enum class Reaction(val emoji: String, val label: String, val color: Color) {
    Like("👍", "J'aime", Color(0xFF4A90E2)),
    Love("❤️", "J'adore", Color(0xFFE0245E)),
    Celebrate("👏", "Bravo", UnovColors.Accent),
    Support("🙌", "Soutien", Color(0xFF6FCF97)),
    Insightful("💡", "Inspirant", Color(0xFFF2C94C)),
    Funny("😂", "Haha", Color(0xFFF2994A))
}

private val BackEasing = Easing { t -> val s = 1.9f; val u = t - 1f; u * u * ((s + 1f) * u + s) + 1f }

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
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onSelect(if (current == null) Reaction.Like else null) },
                onLongPress = { pickerOpen = true }
            )
        }
    ) {
        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            if (current == null) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Réagir",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Text(
                    text = current.emoji,
                    fontSize = 27.sp,
                    modifier = Modifier.graphicsLayer { scaleX = pop; scaleY = pop }
                )
            }
        }
        // Compteur toujours visible (épuré, façon TikTok) — pas de label texte.
        Text(
            text = countFmt,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
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
    // Bob continu désynchronisé.
    val inf = rememberInfiniteTransition(label = "bob$index")
    val bob by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1400 + index * 120),
            RepeatMode.Reverse,
            initialStartOffset = StartOffset(index * 100)
        ),
        label = "bobV$index"
    )
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
                val s = (0.4f + 0.6f * entrance) * magnify
                scaleX = s
                scaleY = s
                translationY = -bob * 5f
            }
            .size(40.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = reaction.emoji, fontSize = 26.sp)
    }
}
