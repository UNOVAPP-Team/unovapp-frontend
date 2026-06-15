package com.unovapp.android.ui.feed

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import kotlinx.coroutines.delay

/** Un cadeau-sticker : emoji animé, nom, prix (FCFA). */
data class GiftSticker(val emoji: String, val name: String, val price: Int)

private val GIFTS = listOf(
    GiftSticker("🌹", "Rose", 50),
    GiftSticker("❤️", "Cœur", 100),
    GiftSticker("🔥", "Flamme", 200),
    GiftSticker("⭐", "Étoile", 500),
    GiftSticker("🎁", "Surprise", 800),
    GiftSticker("💎", "Diamant", 1_500),
    GiftSticker("👑", "Couronne", 3_000),
    GiftSticker("🦁", "Lion", 5_000),
    GiftSticker("🚀", "Fusée", 10_000),
)

/**
 * Panneau de cadeaux premium façon TikTok (full Compose, sans Lottie).
 *  - Entrée du panneau : slide-up + scrim.
 *  - Stickers : apparition en cascade, flottement + pulsation continus, rebond + glow à la
 *    sélection, puis redirection vers le paiement.
 */
@Composable
fun GiftSheet(
    onDismiss: () -> Unit,
    onSelectGift: (GiftSticker) -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) { shown = true }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (shown) 0.62f else 0f,
        animationSpec = tween(260),
        label = "scrim"
    )
    val panelOffset by animateDpAsState(
        targetValue = if (shown) 0.dp else 720.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "panel"
    )

    // Après sélection : petit délai pour laisser jouer le rebond, puis paiement.
    LaunchedEffect(selected) {
        if (selected >= 0) {
            delay(420)
            onSelectGift(GIFTS[selected])
        }
    }

    val noRipple = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim cliquable pour fermer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(interactionSource = noRipple, indication = null, onClick = onDismiss)
        )

        // Panneau bas
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = panelOffset)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF141414), Color(0xFF0A0A0A))
                    )
                )
                .border(
                    1.dp,
                    UnovColors.Accent.copy(alpha = 0.18f),
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(interactionSource = noRipple, indication = null, onClick = {})
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp)
        ) {
            // Poignée
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(UnovColors.LineStrong)
            )
            Spacer(Modifier.height(16.dp))

            // En-tête
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Envoyer un cadeau",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Soutiens ton créateur favori ✨",
                        color = UnovColors.TextMute,
                        fontSize = 12.sp
                    )
                }
                // Chip solde (Mobile Money)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(UnovColors.Accent.copy(alpha = 0.12f))
                        .border(1.dp, UnovColors.Accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = UnovColors.Accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "MoMo / Moov",
                        color = UnovColors.Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Grille 3 colonnes
            GIFTS.chunked(3).forEachIndexed { rowIdx, rowGifts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowGifts.forEachIndexed { colIdx, gift ->
                        val index = rowIdx * 3 + colIdx
                        GiftTile(
                            gift = gift,
                            index = index,
                            shown = shown,
                            selected = selected == index,
                            modifier = Modifier.weight(1f),
                            onClick = { if (selected < 0) selected = index }
                        )
                    }
                    repeat(3 - rowGifts.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = "Choisis un cadeau → paiement Mobile Money sécurisé",
                color = UnovColors.TextMute,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun GiftTile(
    gift: GiftSticker,
    index: Int,
    shown: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val noRipple = remember { MutableInteractionSource() }

    // Entrée en cascade
    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 340, delayMillis = index * 40, easing = FastOutSlowInEasing),
        label = "entrance$index"
    )

    // Flottement + pulsation continus (durées légèrement différentes → organique)
    val inf = rememberInfiniteTransition(label = "idle$index")
    val floatY by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1500 + index * 110, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "floatY$index"
    )
    val pulse by inf.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(1300 + index * 80, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse$index"
    )

    // Rebond à la sélection
    val selScale by animateFloatAsState(
        targetValue = if (selected) 1.22f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "sel$index"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                alpha = entrance
                scaleX = 0.7f + 0.3f * entrance
                scaleY = 0.7f + 0.3f * entrance
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) UnovColors.Accent.copy(alpha = 0.14f) else UnovColors.Surface)
            .border(
                1.dp,
                if (selected) UnovColors.Accent else UnovColors.Line,
                RoundedCornerShape(18.dp)
            )
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow doré derrière le sticker
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                UnovColors.Accent.copy(alpha = if (selected) 0.45f else 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Text(
                text = gift.emoji,
                fontSize = 32.sp,
                modifier = Modifier.graphicsLayer {
                    translationY = -floatY * 5f
                    val s = pulse * selScale
                    scaleX = s
                    scaleY = s
                }
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = gift.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(3.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(UnovGradients.Gold)
            )
            Text(
                text = "${gift.price} F",
                color = UnovColors.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
