package com.unovapp.android.ui.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.State
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

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
    balance: Long,
    onDismiss: () -> Unit,
    onSend: (GiftSticker) -> Unit,
    onRecharge: (deficit: Long) -> Unit
) {
    // `visible` pilote l'entrée ET la sortie (scrim + slide). `entered` est un verrou qui
    // ne sert qu'à la cascade d'apparition des tuiles : il reste vrai pendant la sortie
    // pour que les stickers glissent avec le panneau au lieu de re-rétrécir.
    var visible by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(-1) }
    // Action terminale jouée une fois la sortie finie (fermer / envoyer / recharger).
    var pendingExit by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(Unit) { visible = true; entered = true }

    // Lance le slide-down + fondu du scrim, puis exécute [action] à la fin (voir finishedListener).
    fun beginExit(action: () -> Unit) {
        if (pendingExit != null) return // sortie déjà en cours — on ignore les taps répétés
        pendingExit = action
        visible = false
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.62f else 0f,
        animationSpec = tween(260),
        label = "scrim"
    )
    val panelOffset by animateDpAsState(
        targetValue = if (visible) 0.dp else 760.dp,
        // Entrée : spring soyeux. Sortie : tween rapide et net.
        animationSpec = if (visible)
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        else
            tween(durationMillis = 260, easing = FastOutLinearInEasing),
        // Quand le panneau a fini de descendre (visible == false), on déclenche l'action.
        finishedListener = { if (!visible) pendingExit?.invoke() },
        label = "panel"
    )

    // Un seul moteur d'animation continu partagé par toute la grille (au lieu de ~72
    // transitions infinies indépendantes : 9 tuiles × 4 oscillations + 36 étincelles).
    // Chaque tuile dérive son flottement de cette phase via un déphasage par index, lu
    // dans les graphicsLayer → reste en phase de DESSIN (pas de recomposition par frame).
    // L'ancienne version saturait le compositeur des appareils faibles et figeait le panneau.
    val idleTransition = rememberInfiniteTransition(label = "giftIdle")
    val idlePhase = idleTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "giftIdlePhase"
    )

    val noRipple = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim cliquable pour fermer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(interactionSource = noRipple, indication = null, onClick = { beginExit(onDismiss) })
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
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(UnovGradients.Gold)
                    )
                    Text(
                        text = "${"%,d".format(balance).replace(',', ' ')} jetons",
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
                            shown = entered,
                            selected = selected == index,
                            idlePhase = idlePhase,
                            modifier = Modifier.weight(1f),
                            // Toggle : retaper le cadeau sélectionné le désélectionne,
                            // taper un autre change la sélection (avant : verrouillé au 1er tap).
                            onClick = { selected = if (selected == index) -1 else index }
                        )
                    }
                    repeat(3 - rowGifts.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Zone d'action intelligente : paiement direct si le solde suffit, sinon recharge.
            val gift = GIFTS.getOrNull(selected)
            when {
                gift == null -> Text(
                    text = "Choisis un cadeau pour l'envoyer",
                    color = UnovColors.TextMute,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                balance >= gift.price -> GiftActionButton(
                    text = "Envoyer ${gift.emoji}  ·  ${"%,d".format(gift.price).replace(',', ' ')} jetons",
                    onClick = { beginExit { onSend(gift) } }
                )
                else -> {
                    val deficit = gift.price - balance
                    Text(
                        text = "Solde insuffisant — il te manque ${"%,d".format(deficit).replace(',', ' ')} jetons",
                        color = UnovColors.Danger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                    GiftActionButton(
                        text = "Recharger mon compte",
                        onClick = { beginExit { onRecharge(deficit) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun GiftActionButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(UnovGradients.Gold)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF0D0D0D),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GiftTile(
    gift: GiftSticker,
    index: Int,
    shown: Boolean,
    selected: Boolean,
    idlePhase: State<Float>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val noRipple = remember { MutableInteractionSource() }

    // Easing "overshoot" (easeOutBack) : l'entrée dépasse puis se pose avec rebond.
    val backEasing = remember {
        Easing { t -> val s = 1.9f; val u = t - 1f; u * u * ((s + 1f) * u + s) + 1f }
    }
    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 480, delayMillis = index * 55, easing = backEasing),
        label = "entrance$index"
    )

    // Flottement / balancement / respiration dérivés de la phase partagée (déphasage par
    // index → grille "vivante" désynchronisée), lus dans les graphicsLayer ci-dessous.
    val idleOffset = index * 0.6f

    // Sélection : rebond ample + onde de choc.
    val selScale by animateFloatAsState(
        targetValue = if (selected) 1.34f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "sel$index"
    )
    val shockwave = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (selected) {
            shockwave.snapTo(0f)
            shockwave.animateTo(1f, tween(640, easing = FastOutSlowInEasing))
        } else {
            shockwave.snapTo(0f)
        }
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                alpha = entrance.coerceIn(0f, 1f)
                val sc = 0.5f + 0.5f * entrance
                scaleX = sc
                scaleY = sc
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) UnovColors.Accent.copy(alpha = 0.16f) else UnovColors.Surface)
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
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            // Onde de choc à la sélection (anneau or qui s'étend et s'efface).
            Canvas(modifier = Modifier.fillMaxSize()) {
                val p = shockwave.value
                if (p in 0.001f..0.999f) {
                    val maxR = size.minDimension / 2f
                    drawCircle(
                        color = UnovColors.Accent.copy(alpha = (1f - p) * 0.7f),
                        radius = maxR * (0.45f + p * 0.95f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Glow doré pulsant.
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        alpha = if (selected) 1f
                        else 0.4f + 0.6f * ((sin(idlePhase.value + idleOffset) + 1f) / 2f)
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                UnovColors.Accent.copy(alpha = if (selected) 0.5f else 0.18f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Étincelles scintillantes autour du sticker.
            Sparkle(offsetX = (-20).dp, offsetY = (-16).dp, idlePhase = idlePhase, phase = idleOffset)
            Sparkle(offsetX = 19.dp, offsetY = (-18).dp, idlePhase = idlePhase, phase = idleOffset + 1.6f)
            Sparkle(offsetX = (-18).dp, offsetY = 17.dp, idlePhase = idlePhase, phase = idleOffset + 3.1f)
            Sparkle(offsetX = 20.dp, offsetY = 15.dp, idlePhase = idlePhase, phase = idleOffset + 4.7f)

            // Le sticker : flottement + balancement + respiration + rebond.
            Text(
                text = gift.emoji,
                fontSize = 33.sp,
                modifier = Modifier.graphicsLayer {
                    val p = idlePhase.value + idleOffset
                    translationY = -((sin(p) + 1f) / 2f) * 6f
                    rotationZ = sin(p * 0.8f) * 5f
                    val breathe = 0.96f + 0.09f * ((sin(p * 1.1f) + 1f) / 2f)
                    val s = breathe * selScale
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
                text = "%,d".format(gift.price).replace(',', ' '),
                color = UnovColors.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Petite étincelle dorée qui scintille, déphasée. Dérive du même [idlePhase] partagé que la
 * grille (lecture en phase de dessin) — plus de transition infinie par étincelle.
 */
@Composable
private fun Sparkle(offsetX: Dp, offsetY: Dp, idlePhase: State<Float>, phase: Float) {
    Text(
        text = "✦",
        color = UnovColors.Accent,
        fontSize = 9.sp,
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                val a = (sin(idlePhase.value * 1.6f + phase) + 1f) / 2f
                alpha = a
                val s = 0.5f + a * 0.7f
                scaleX = s
                scaleY = s
            }
    )
}
