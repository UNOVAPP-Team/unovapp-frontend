package com.unovapp.android.ui.inbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LiveTv
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.components.unovTap
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import com.unovapp.android.ui.theme.UnovMotion
import kotlinx.coroutines.delay

enum class NotifType { Like, Gift, Battle, Comment, Follow, Live }

data class NotificationUi(
    val id: String,
    val avatarIdx: Int,
    val user: String,
    val verified: Boolean,
    val type: NotifType,
    val text: String,
    val time: String
)

private val INBOX_TABS = listOf("Tout", "Mentions", "Likes", "Cadeaux", "Battles", "Messages")

private val MOCK_NOTIFICATIONS = listOf(
    NotificationUi("n1", 1, "kossi.dance", false, NotifType.Like,
        "a aimé ta vidéo \"Quand maman entend…\"", "2 m"),
    NotificationUi("n2", 4, "reezy_naija", true, NotifType.Follow,
        "a commencé à te suivre", "12 m"),
    NotificationUi("n3", 4, "samuel228", false, NotifType.Gift,
        "t'a envoyé 50 jetons 🎁", "1 h"),
    NotificationUi("n4", 3, "fatou_ben", false, NotifType.Comment,
        "a commenté : « Trop bien ! »", "2 h"),
    NotificationUi("n5", 2, "le_chef_moise", false, NotifType.Battle,
        "te défie en Battle — accepte ?", "3 h"),
    NotificationUi("n6", 0, "aminata.cot", true, NotifType.Live,
        "est en direct maintenant", "5 h"),
    NotificationUi("n7", 5, "mariama_dak", false, NotifType.Like,
        "a aimé 6 de tes vidéos", "12 h"),
    NotificationUi("n8", 3, "ange_ptn", false, NotifType.Follow,
        "a commencé à te suivre", "1 j"),
    NotificationUi("n9", 5, "boutique_zola", false, NotifType.Gift,
        "t'a envoyé un cadeau premium 💎", "1 j")
)

private val SuccessGreen = Color(0xFF4ADE80)

@Composable
fun InboxScreen(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    UnovAppTheme {
        var activeTab by remember { mutableStateOf(INBOX_TABS.first()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(contentPadding)
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
                Text(
                    text = "Boîte",
                    color = UnovColors.Text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp
                )
                TabPillsRow(
                    tabs = INBOX_TABS,
                    active = activeTab,
                    onSelect = { activeTab = it },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                itemsIndexed(MOCK_NOTIFICATIONS, key = { _, n -> n.id }) { index, n ->
                    StaggeredRow(index = index) {
                        NotificationRow(n)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPillsRow(
    tabs: List<String>,
    active: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        tabs.forEach { tab ->
            val isActive = tab == active
            val tint by animateColorAsState(
                targetValue = if (isActive) UnovColors.Text else UnovColors.TextMute,
                animationSpec = UnovMotion.standard(),
                label = "tabTint"
            )
            Box(
                modifier = Modifier
                    .unovTap(onClick = { onSelect(tab) }, pressedScale = 0.94f)
                    .padding(vertical = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tab,
                        color = tint,
                        fontSize = 14.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                    )
                    AnimatedVisibility(
                        visible = isActive,
                        enter = scaleIn(initialScale = 0.3f, animationSpec = UnovMotion.bouncy()) +
                            fadeIn(UnovMotion.fast()),
                        exit = scaleOut(targetScale = 0.3f, animationSpec = UnovMotion.snappy()) +
                            fadeOut(UnovMotion.fast())
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(width = 20.dp, height = 2.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(UnovColors.Accent)
                        )
                    }
                }
            }
        }
    }
}

/* ---------- Notification row + stagger entry ---------- */

@Composable
private fun StaggeredRow(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val capped = index.coerceAtMost(UnovMotion.StaggerMaxItems)
    LaunchedEffect(Unit) {
        delay((capped * UnovMotion.StaggerDelayMs).toLong())
        visible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = UnovMotion.smooth(),
        label = "rowScale"
    )
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(UnovMotion.standard()) +
            slideInVertically(UnovMotion.decelerate()) { it / 3 }
    ) {
        Box(modifier = Modifier.scale(scale)) {
            content()
        }
    }
}

@Composable
private fun NotificationRow(n: NotificationUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .unovTap(onClick = {})
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(54.dp)) {
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                Avatar(idx = n.avatarIdx, name = n.user, size = 48.dp)
            }
            TypeBadge(
                type = n.type,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            val verifiedId = "verified-checkmark"
            val inlineContent = mapOf(
                verifiedId to InlineTextContent(
                    placeholder = Placeholder(
                        width = 16.sp,
                        height = 14.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    ),
                    children = {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Vérifié",
                            tint = UnovColors.Accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                )
            )
            Text(
                text = buildNotifText(n, verifiedId),
                inlineContent = inlineContent,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
            Text(
                text = n.time,
                color = UnovColors.TextMute,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        when (n.type) {
            NotifType.Follow -> FollowBackButton()
            NotifType.Live -> WatchLiveButton()
            else -> Unit
        }
    }
}

/**
 * Badge type animé. Like : cœur qui pulse en boucle (sang dans le bouton).
 * Live : point rouge pulsant pour rappeler une diffusion en direct. Reste statique sinon.
 */
@Composable
private fun TypeBadge(type: NotifType, modifier: Modifier = Modifier) {
    val (bg, icon, tint) = when (type) {
        NotifType.Like -> Triple(SolidBg(UnovColors.Danger), Icons.Filled.Favorite, Color.White)
        NotifType.Gift -> Triple(UnovGradients.Gold, Icons.Outlined.CardGiftcard, Color(0xFF0D0D0D))
        NotifType.Battle -> Triple(SolidBg(UnovColors.Accent), Icons.Outlined.Bolt, Color(0xFF0D0D0D))
        NotifType.Comment -> Triple(SolidBg(SuccessGreen), Icons.Outlined.ChatBubbleOutline, Color(0xFF0D0D0D))
        NotifType.Follow -> Triple(SolidBg(SuccessGreen), Icons.Filled.Add, Color(0xFF0D0D0D))
        NotifType.Live -> Triple(SolidBg(UnovColors.Danger), Icons.Outlined.LiveTv, Color.White)
    }

    // Pulse infini pour Like et Live — capte l'attention sans agresser
    val pulseScale = if (type == NotifType.Like || type == NotifType.Live) {
        val transition = rememberInfiniteTransition(label = "badgePulse")
        val s by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        s
    } else 1f

    Box(
        modifier = modifier
            .size(22.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, Color(0xFF121212), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp)
        )
    }
}

private fun SolidBg(color: Color): Brush =
    Brush.linearGradient(colors = listOf(color, color))

@Composable
private fun FollowBackButton() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(UnovColors.Accent)
            .unovTap(onClick = {}, pressedScale = 0.92f)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Suivre",
            color = Color(0xFF0D0D0D),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WatchLiveButton() {
    // Live dot pulsant à gauche pour appuyer le "en direct"
    val transition = rememberInfiniteTransition(label = "liveDot")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(UnovColors.Danger)
            .unovTap(onClick = {}, pressedScale = 0.92f)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = dotAlpha))
        )
        Text(
            text = "Voir",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun buildNotifText(n: NotificationUi, verifiedInlineId: String): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = UnovColors.Text, fontWeight = FontWeight.Bold)) {
            append("@${n.user}")
        }
        if (n.verified) {
            append(" ")
            appendInlineContent(verifiedInlineId, "[v]")
        }
        withStyle(SpanStyle(color = UnovColors.TextDim)) {
            append(" ${n.text}")
        }
    }
