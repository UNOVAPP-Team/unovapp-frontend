package com.unovapp.android.ui.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import kotlinx.coroutines.launch

@Composable
fun GiftBreakdownSheet(
    video: FeedVideoUi,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    var pendingExit by remember { mutableStateOf<(() -> Unit)?>(null) }
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) { visible = true; shown = true }

    fun beginExit(action: () -> Unit) {
        if (pendingExit != null) return
        pendingExit = action
        visible = false
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.62f else 0f,
        animationSpec = tween(260),
        label = "breakdownScrim"
    )
    val panelOffset by animateDpAsState(
        targetValue = if (visible) 0.dp else 600.dp,
        animationSpec = if (visible)
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        else
            tween(durationMillis = 240, easing = FastOutLinearInEasing),
        finishedListener = { if (!visible) pendingExit?.invoke() },
        label = "breakdownPanel"
    )

    val noRipple = remember { MutableInteractionSource() }
    val sorted = remember(video.giftBreakdown) { video.giftBreakdown.sortedByDescending { it.count } }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(interactionSource = noRipple, indication = null) { beginExit(onDismiss) }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, panelOffset.roundToPx() + dragOffset.value.toInt()) }
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF141414), Color(0xFF0A0A0A))))
                .border(
                    1.dp,
                    UnovColors.Accent.copy(alpha = 0.18f),
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .pointerInput(Unit) {
                    val threshold = 120.dp.toPx()
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (dragOffset.value > threshold) {
                                    dragOffset.snapTo(0f)
                                    beginExit(onDismiss)
                                } else {
                                    dragOffset.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        },
                        onVerticalDrag = { _, delta ->
                            scope.launch { dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f)) }
                        }
                    )
                }
                .clickable(interactionSource = noRipple, indication = null) {}
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(UnovColors.LineStrong)
            )
            Spacer(Modifier.height(18.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(UnovGradients.Gold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Redeem,
                            contentDescription = null,
                            tint = Color(0xFF0D0D0D),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Cadeaux reçus",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${video.creatorUsername}",
                            color = UnovColors.TextMute,
                            fontSize = 12.sp
                        )
                    }
                }
                // Total badge
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
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(UnovGradients.Gold)
                    )
                    Text(
                        text = video.giftsFmt,
                        color = UnovColors.Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (sorted.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun cadeau pour l'instant",
                        color = UnovColors.TextMute,
                        fontSize = 14.sp
                    )
                }
            } else {
                sorted.forEachIndexed { index, gift ->
                    GiftBreakdownRow(gift = gift, index = index, shown = shown)
                    if (index < sorted.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(UnovColors.LineStrong)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GiftBreakdownRow(gift: GiftReceived, index: Int, shown: Boolean) {
    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 360, delayMillis = index * 65),
        label = "rowEntrance$index"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entrance
                translationX = (1f - entrance) * 28.dp.toPx()
            }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = gift.emoji, fontSize = 30.sp)
            Text(
                text = gift.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(UnovGradients.Gold)
            )
            Text(
                text = "×${gift.count}",
                color = UnovColors.Accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
