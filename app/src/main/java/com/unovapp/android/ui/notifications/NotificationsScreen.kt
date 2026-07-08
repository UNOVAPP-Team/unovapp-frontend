package com.unovapp.android.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unovapp.android.data.notification.NotificationItemDto
import com.unovapp.android.ui.components.enterFadeSlide
import com.unovapp.android.ui.theme.UnovColors

@Composable
fun NotificationsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    vm: NotificationsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UnovColors.BgBase)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // En-tête : titre + compteur non-lus + tout marquer lu.
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notifications", color = UnovColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (state.unreadCount > 0) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(UnovColors.Accent).padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${state.unreadCount}", color = Color(0xFF0D0D0D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (state.items.any { !it.isRead }) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable { vm.markAllRead() }.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.DoneAll, null, tint = UnovColors.Accent, modifier = Modifier.size(16.dp))
                    Text("Tout lire", color = UnovColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        when {
            state.isLoading && state.items.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = UnovColors.Accent) }
            state.items.isEmpty() ->
                EmptyNotifications(state.error)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                items(state.items, key = { it.id }) { n ->
                    NotificationRow(n, onClick = { vm.markRead(n.id) })
                }
                item {
                    // Pagination : charge plus en atteignant le bas.
                    if (state.hasMore) {
                        LaunchedLoadMore(onReach = vm::loadMore)
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            CircularProgressIndicator(color = UnovColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun LaunchedLoadMore(onReach: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onReach() }
}

@Composable
private fun EmptyNotifications(error: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(UnovColors.Surface),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Notifications, null, tint = UnovColors.TextMute, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(
            error ?: "Aucune notification pour l'instant",
            color = UnovColors.TextDim, fontSize = 14.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NotificationRow(n: NotificationItemDto, onClick: () -> Unit) {
    val (icon, tint) = iconFor(n.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (n.isRead) Color.Transparent else UnovColors.Accent.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp)) }

        Column(Modifier.weight(1f)) {
            Text(
                n.title,
                color = UnovColors.Text,
                fontSize = 14.sp,
                fontWeight = if (n.isRead) FontWeight.Medium else FontWeight.SemiBold,
                maxLines = 2
            )
            if (!n.body.isNullOrBlank()) {
                Text(n.body, color = UnovColors.TextMute, fontSize = 12.sp, maxLines = 1)
            }
            Text(relativeTime(n.createdAt), color = UnovColors.TextMute, fontSize = 11.sp)
        }

        if (!n.isRead) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(UnovColors.Accent))
        }
    }
}

/** Icône + teinte selon le type backend (video.liked, video.commented, user.followed, comment_mentioned, video.transcoded…). */
private fun iconFor(type: String): Pair<ImageVector, Color> = when {
    type.contains("lik", true)     -> Icons.Filled.Favorite to Color(0xFFFF4D6A)
    type.contains("comment", true) && type.contains("mention", true) -> Icons.Outlined.AlternateEmail to UnovColors.AccentLight
    type.contains("comment", true) -> Icons.Outlined.ChatBubbleOutline to UnovColors.Accent
    type.contains("follow", true)  -> Icons.Filled.PersonAdd to UnovColors.Accent
    type.contains("mention", true) -> Icons.Outlined.AlternateEmail to UnovColors.AccentLight
    type.contains("video", true)   -> Icons.Outlined.Videocam to UnovColors.AccentLight
    else                           -> Icons.Outlined.Notifications to UnovColors.TextMute
}

private fun relativeTime(iso: String): String {
    val t = parseIso(iso) ?: return ""
    val diff = (System.currentTimeMillis() - t).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> "à l'instant"
        diff < 3_600_000L -> "il y a ${diff / 60_000L} min"
        diff < 86_400_000L -> "il y a ${diff / 3_600_000L} h"
        else -> "il y a ${diff / 86_400_000L} j"
    }
}

private fun parseIso(v: String): Long? {
    if (v.isBlank()) return null
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ssXXX")
    return patterns.firstNotNullOfOrNull { p ->
        runCatching {
            java.text.SimpleDateFormat(p, java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(v)?.time
        }.getOrNull()
    }
}

