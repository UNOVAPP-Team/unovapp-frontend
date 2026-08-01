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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import com.unovapp.android.ui.components.BraiseLoader
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
import com.unovapp.android.ui.components.EmberActionPill
import com.unovapp.android.ui.components.EmberBigNumber
import com.unovapp.android.ui.components.EmberBody
import com.unovapp.android.ui.components.EmberCard
import com.unovapp.android.ui.components.EmberEyebrow
import com.unovapp.android.ui.components.EmberScreenScaffold
import com.unovapp.android.ui.components.EmberWave
import com.unovapp.android.ui.theme.Ember
import com.unovapp.android.ui.theme.EmberDim
import com.unovapp.android.ui.theme.EmberMotion
import com.unovapp.android.ui.theme.countUp
import com.unovapp.android.ui.theme.emberEnter
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.animatedAccent
import com.unovapp.android.ui.theme.pressable
import com.unovapp.android.ui.theme.riseIn
import com.unovapp.android.ui.theme.staggerDelay

@Composable
fun NotificationsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    vm: NotificationsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // « Pulsations » — écran 05 de la maquette : titre XXL + pilule « Apaiser »,
    // carte « POULS DU JOUR », puis les Moments en cartes.
    EmberScreenScaffold(
        title = "Pulsations",
        action = {
            if (state.items.any { !it.isRead }) {
                EmberActionPill(
                    text = "Apaiser",
                    onClick = { vm.markAllRead() },
                    modifier = Modifier.emberEnter(dyFrom = 8.dp),
                    leading = {
                        Icon(
                            Icons.Filled.LocalFireDepartment, null,
                            tint = Ember.BraiseClair, modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    ) {
        when {
            state.isLoading && state.items.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { BraiseLoader(color = Ember.Braise) }
            state.items.isEmpty() ->
                EmptyNotifications(state.error)
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Carte « POULS DU JOUR » : le bilan du jour, en tête.
                item {
                    PoulsDuJourCard(
                        braises = state.unreadCount,
                        modifier = Modifier.padding(horizontal = 20.dp).emberEnter()
                    )
                }
                item {
                    EmberEyebrow(
                        "Moments", muted = true,
                        modifier = Modifier.padding(start = 22.dp, top = 14.dp, bottom = 2.dp).emberEnter(dyFrom = 8.dp)
                    )
                }
                // Cascade d'entrée par index réel (§2.4, plafond 8) — `indexOf` était
                // en O(n) par ligne et se trompait sur les doublons.
                itemsIndexed(state.items, key = { _, n -> n.id }) { index, n ->
                    NotificationRow(
                        n = n,
                        loading = state.openingVideoId == n.data["video_id"],
                        onClick = {
                            vm.markRead(n.id)
                            n.data["video_id"]?.takeIf { it.isNotBlank() }?.let(vm::openVideo)
                        },
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .riseIn(delayMs = staggerDelay(index), key = n.id)
                    )
                }
                item {
                    // Pagination : charge plus en atteignant le bas.
                    if (state.hasMore) {
                        LaunchedLoadMore(onReach = vm::loadMore)
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            BraiseLoader(color = Ember.Braise, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    // Vidéo ouverte depuis une notification → lecture plein écran (même lecteur que le feed).
    state.openedVideo?.let { video ->
        com.unovapp.android.ui.feed.VideoDetailScreen(
            videos = listOf(video),
            startIndex = 0,
            onBack = { vm.closeVideo() }
        )
    }
}

/**
 * « POULS DU JOUR » — la carte de bilan en tête de Pulsations (maquette écran 05) :
 * eyebrow + date, grand compteur, onde qui se dessine, et la ligne de détail.
 * Le compteur monte en `countUp` : aucun chiffre n'apparaît par substitution.
 */
@Composable
private fun PoulsDuJourCard(braises: Int, modifier: Modifier = Modifier) {
    EmberCard(modifier = modifier, accent = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmberEyebrow("Pouls du jour")
            EmberBody(todayLabel(), color = Ember.TextMute, size = 14)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                EmberBigNumber("${countUp(braises)}")
                EmberBody("braises reçues", color = Ember.TextDim, size = 15)
            }
            Spacer(Modifier.width(18.dp))
            EmberWave(modifier = Modifier.weight(1f).height(56.dp))
        }
    }
}

/** Date du jour, façon « vendredi 18 juillet ». */
private fun todayLabel(): String {
    val fmt = java.text.SimpleDateFormat("EEEE d MMMM", java.util.Locale.FRENCH)
    return fmt.format(java.util.Date())
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
private fun NotificationRow(
    n: NotificationItemDto,
    loading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = iconFor(n.type)
    // Emoji du type de réaction (le backend l'envoie dans data["reaction"] quand il le connaît ;
    // à défaut il est souvent déjà dans le titre → on n'ajoute rien).
    val reactionEmoji = n.data["reaction"]?.let { reactionEmojiFor(it) }
    // Le fond « non lu » se retire en fondu quand la notification est lue.
    // Non lu : la carte s'accentue en braise, et se calme en fondu à la lecture.
    val border = animatedAccent(
        active = !n.isRead,
        activeColor = Ember.Braise.copy(alpha = 0.4f),
        idleColor = Ember.Line,
        durationMs = EmberMotion.DurSheet
    )
    val shape = RoundedCornerShape(EmberDim.CardRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ember.Surface.copy(alpha = 0.55f))
            .border(1.dp, border, shape)
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (reactionEmoji != null) {
                Text(reactionEmoji, fontSize = 20.sp)
            } else {
                Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))
            }
        }

        Column(Modifier.weight(1f)) {
            EmberBody(
                n.title,
                color = Ember.Text,
                size = 15,
                weight = if (n.isRead) FontWeight.Medium else FontWeight.Bold,
                maxLines = 2
            )
            if (!n.body.isNullOrBlank()) {
                EmberBody(n.body, color = Ember.TextDim, size = 13, maxLines = 1)
            }
            EmberBody(relativeTime(n.createdAt), color = Ember.TextMute, size = 12)
        }

        when {
            // Chargement de la vidéo liée (tap en cours).
            loading -> BraiseLoader(
                color = UnovColors.Accent,
                
                modifier = Modifier.size(18.dp)
            )
            // Affordance « ouvrir la vidéo concernée ».
            n.data["video_id"]?.isNotBlank() == true -> Icon(
                Icons.Outlined.PlayCircleOutline,
                contentDescription = "Voir la vidéo",
                tint = UnovColors.TextMute,
                modifier = Modifier.size(20.dp)
            )
            !n.isRead -> Box(Modifier.size(9.dp).clip(CircleShape).background(UnovColors.Accent))
        }
    }
}

/** Emoji correspondant au type de réaction envoyé par le backend (data["reaction"]). */
private fun reactionEmojiFor(reaction: String): String = when (reaction.lowercase()) {
    "like" -> "👍"
    "love" -> "❤️"
    "haha" -> "😂"
    "care", "support" -> "🤗"
    "wow" -> "😮"
    "sad" -> "😢"
    "celebrate" -> "🎉"
    else -> "❤️"
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

