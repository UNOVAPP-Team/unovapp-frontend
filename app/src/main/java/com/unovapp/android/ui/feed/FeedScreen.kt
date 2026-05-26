@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.unovapp.android.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.comments.CommentsSheet
import com.unovapp.android.ui.theme.UnovColors

private enum class FeedTab(val label: String) {
    ForYou("Pour Toi"),
    Following("Abonnements")
}

@Composable
fun FeedScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onOpenWallet: () -> Unit = {}
) {
    val videos = remember { MockFeedVideos }
    val pagerState = rememberPagerState(pageCount = { videos.size })
    var tab by remember { mutableStateOf(FeedTab.ForYou) }
    var commentsForVideoId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(contentPadding)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            FeedItem(
                video = videos[page],
                isCurrentPage = page == pagerState.currentPage,
                onCommentClick = { commentsForVideoId = videos[page].id },
                onGiftClick = onOpenWallet
            )
        }

        // Top translucent header with tabs
        FeedHeader(
            tab = tab,
            onTabChange = { tab = it },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    commentsForVideoId?.let { videoId ->
        val video = videos.first { it.id == videoId }
        CommentsSheet(
            commentCountFmt = video.commentsFmt,
            onDismiss = { commentsForVideoId = null }
        )
    }
}

@Composable
private fun FeedHeader(
    tab: FeedTab,
    onTabChange: (FeedTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141414).copy(alpha = 0.82f))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .background(UnovColors.Accent.copy(alpha = 0.05f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HdChip()
            TabsRow(tab = tab, onTabChange = onTabChange)
            HeaderActions()
        }
    }
}

@Composable
private fun HdChip() {
    val transition = rememberInfiniteTransition(label = "hdPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hdAlpha"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(UnovColors.Accent.copy(alpha = 0.08f))
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(UnovColors.Accent.copy(alpha = alpha))
        )
        Text(
            text = "HD · ÉCO",
            color = UnovColors.Accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.0.sp
        )
    }
}

@Composable
private fun TabsRow(tab: FeedTab, onTabChange: (FeedTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        FeedTab.values().reversed().forEach { t ->  // "Abonnements" before "Pour Toi" visually
            TabItem(
                label = t.label,
                isActive = tab == t,
                onClick = { onTabChange(t) }
            )
        }
    }
}

@Composable
private fun TabItem(label: String, isActive: Boolean, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else UnovColors.TextMute,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        AnimatedVisibility(visible = isActive) {
            Box(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .width(24.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovColors.Accent)
            )
        }
    }
}

@Composable
private fun HeaderActions() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        HeaderIconButton(icon = Icons.Outlined.Search, contentDescription = "Recherche")
        HeaderIconButton(
            icon = Icons.Outlined.NotificationsNone,
            contentDescription = "Notifications",
            badge = "3"
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    badge: String? = null
) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(interactionSource = noRipple, indication = null) {}
            .padding(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 0.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE54646))
                    .border(2.dp, Color(0xFF141414), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
