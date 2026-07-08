package com.unovapp.android.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Battle live : split-screen vertical, votes en temps réel, cadeaux flottants.
 *
 * Pour le démo on simule :
 *  - votes A/B incrémentés au hasard toutes les 700ms
 *  - countdown qui descend de 48s
 *  - gifts emoji spawnent toutes les ~900ms, montent et s'effacent
 *
 * En prod (Mois 4 backend Go WebSocket) ces états seront pilotés par les events
 * push `vote.added`, `gift.sent` et `battle.tick` du Battle Service.
 */
@Composable
fun BattleScreen(onClose: () -> Unit) {
    UnovAppTheme {
        var votesA by remember { mutableIntStateOf(2840) }
        var votesB by remember { mutableIntStateOf(2210) }
        var seconds by remember { mutableIntStateOf(48) }
        val gifts = remember { mutableStateListOf<Gift>() }

        // Simulation des votes + countdown
        LaunchedEffect(Unit) {
            while (seconds > 0) {
                delay(700)
                votesA += Random.nextInt(0, 8)
                votesB += Random.nextInt(0, 7)
                seconds = (seconds - 1).coerceAtLeast(0)
            }
        }

        // Spawn de gifts toutes les ~900ms
        LaunchedEffect(Unit) {
            while (true) {
                delay(900)
                val emoji = listOf("🎁", "💎", "🌹", "🔥", "💰").random()
                val leftSide = Random.nextFloat() < 0.55f
                gifts += Gift(
                    id = System.currentTimeMillis() + Random.nextInt(),
                    emoji = emoji,
                    leftSide = leftSide
                )
                // Garde au max 8 gifts simultanés
                while (gifts.size > 8) gifts.removeAt(0)
            }
        }

        val total = (votesA + votesB).coerceAtLeast(1)
        val pctA = votesA.toFloat() / total
        val animPctA by animateFloatAsState(
            targetValue = pctA,
            animationSpec = tween(durationMillis = 400),
            label = "voteBarA"
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Split videos — placeholders gradients (Mois 4 prod : 2 streams WebRTC ou RTMP)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(UnovGradients.videoBg(4))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(UnovGradients.videoBg(2))
                )
            }

            // Mid white divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.4f))
            )

            // VS badge centered with infinite glow
            VsBadge(modifier = Modifier.align(Alignment.Center))

            // Vote bar (just below VS)
            VoteBar(
                pctA = animPctA,
                votesA = votesA,
                votesB = votesB,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 50.dp)
                    .padding(horizontal = 14.dp)
            )

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CloseButton(onClose = onClose)
                BattleStatusPill(seconds = seconds)
                Spacer(modifier = Modifier.size(36.dp))
            }

            // Player A info (top-left below top bar)
            PlayerInfo(
                avatarIdx = 4,
                username = "reezy_naija",
                location = "🇳🇬 Lagos",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 14.dp, top = 60.dp)
            )

            // Player B info (bottom-right above bottom CTA), mirrored alignment
            PlayerInfo(
                avatarIdx = 2,
                username = "le_chef_moise",
                location = "🇧🇯 Porto-Novo",
                trailing = true,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 200.dp)
            )

            // Floating gifts
            gifts.forEach { gift ->
                FloatingGift(
                    gift = gift,
                    onComplete = { gifts.remove(gift) }
                )
            }

            // Bottom CTA bar
            BottomCtaBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 14.dp, end = 14.dp, bottom = 16.dp)
            )
        }
    }
}

/* ---------- Sub-components ---------- */

@Composable
private fun VsBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "vsGlow")
    val glow by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vsGlowScale"
    )
    Box(
        modifier = modifier
            .scale(glow)
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .border(2.dp, UnovColors.Accent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "VS",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.4).sp
        )
    }
}

@Composable
private fun VoteBar(
    pctA: Float,
    votesA: Int,
    votesB: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(pctA.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF944D), UnovColors.Accent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .weight((1f - pctA).coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2A1606), Color(0xFF1A0E00))
                        )
                    )
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val pctAInt = (pctA * 100).toInt()
            Text(
                text = "$pctAInt% · ${formatVotes(votesA)}",
                color = Color(0xFFFF944D),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${formatVotes(votesB)} · ${100 - pctAInt}%",
                color = Color(0xFFFFD2A6),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PlayerInfo(
    avatarIdx: Int,
    username: String,
    location: String,
    trailing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (trailing) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "@$username",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = location,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            RingedAvatar(idx = avatarIdx, username = username)
        } else {
            RingedAvatar(idx = avatarIdx, username = username)
            Column {
                Text(
                    text = "@$username",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = location,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun RingedAvatar(idx: Int, username: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(
                        UnovColors.Accent,
                        Color(0xFFFF944D),
                        UnovColors.AccentDeep,
                        UnovColors.Accent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Avatar(idx = idx, name = username, size = 34.dp)
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = noRipple,
                indication = null,
                onClick = onClose
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Fermer",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BattleStatusPill(seconds: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Bolt,
            contentDescription = null,
            tint = UnovColors.Accent,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "BATTLE · 0:${seconds.toString().padStart(2, '0')}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun BoxScope.FloatingGift(
    gift: Gift,
    onComplete: () -> Unit
) {
    val offsetY = remember(gift.id) { Animatable(0f) }
    LaunchedEffect(gift.id) {
        offsetY.animateTo(
            targetValue = -180f,
            animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
        )
        onComplete()
    }

    val progress = (-offsetY.value / 180f).coerceIn(0f, 1f)
    val alpha = (1f - progress * 0.95f).coerceIn(0f, 1f)

    Text(
        text = gift.emoji,
        fontSize = 28.sp,
        modifier = Modifier
            .align(if (gift.leftSide) Alignment.BottomStart else Alignment.BottomEnd)
            .padding(
                start = if (gift.leftSide) 60.dp else 0.dp,
                end = if (gift.leftSide) 0.dp else 60.dp,
                bottom = 220.dp
            )
            .graphicsLayer {
                translationY = offsetY.value
                rotationZ = offsetY.value * -0.08f
                this.alpha = alpha
            }
    )
}

@Composable
private fun BottomCtaBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VoteButton(
            label = "Voter A",
            background = Brush.linearGradient(
                listOf(Color(0xFFFF944D), UnovColors.Accent)
            ),
            textColor = Color(0xFF0D0D0D),
            modifier = Modifier.weight(1f)
        )
        GiftButton(modifier = Modifier)
        VoteButton(
            label = "Voter B",
            background = Brush.linearGradient(
                listOf(Color(0xFF2A1606), Color(0xFF1A0E00))
            ),
            textColor = Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VoteButton(
    label: String,
    background: Brush,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Spacer(modifier = Modifier.width(0.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GiftButton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(UnovGradients.Gold)
            .clickable {}
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CardGiftcard,
            contentDescription = null,
            tint = Color(0xFF0D0D0D),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "50",
            color = Color(0xFF0D0D0D),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ---------- Data ---------- */

internal data class Gift(
    val id: Long,
    val emoji: String,
    val leftSide: Boolean
)

private fun formatVotes(n: Int): String =
    "%,d".format(n).replace(',', ' ')
