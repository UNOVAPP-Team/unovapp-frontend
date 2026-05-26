package com.unovapp.android.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

/**
 * Une page du feed (pleine hauteur). Joue la vidéo si [isCurrentPage] et superpose le HUD.
 *
 * Le state d'interaction (liked, saved, followed, muted) est local — il sera remonté au
 * ViewModel et synchronisé avec le backend Social Service (Mois 3).
 */
@Composable
fun FeedItem(
    video: FeedVideoUi,
    isCurrentPage: Boolean,
    onCommentClick: () -> Unit = {},
    onGiftClick: () -> Unit = {}
) {
    var liked by remember(video.id) { mutableStateOf(video.isLiked) }
    var saved by remember(video.id) { mutableStateOf(false) }
    var followed by remember(video.id) { mutableStateOf(video.isFollowing) }
    var muted by remember { mutableStateOf(true) }

    // Heart pop : compteur incrémenté à chaque double-tap → déclenche l'animation
    var heartPopKey by remember(video.id) { mutableIntStateOf(0) }
    var showHeartPop by remember { mutableStateOf(false) }

    LaunchedEffect(heartPopKey) {
        if (heartPopKey > 0) {
            showHeartPop = true
            delay(700)
            showHeartPop = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(video.id) {
                detectTapGestures(
                    onDoubleTap = {
                        // Le double-tap "like" même si déjà liké, et re-pop le coeur
                        if (!liked) liked = true
                        heartPopKey++
                    }
                )
            }
    ) {
        // Player full-bleed
        VideoPlayer(
            url = video.hlsUrl,
            isPlaying = isCurrentPage,
            modifier = Modifier.fillMaxSize()
        )

        // Scrim bas — assure la lisibilité du texte sur n'importe quelle vidéo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.75f)
                    )
                )
        )

        // Tendance badge (top-left, en dessous du header)
        if (video.id == "v1") {
            TrendingBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 14.dp, top = 64.dp)
            )
        }

        // Heart pop sur double-tap — coeur or géant qui pop puis fade
        AnimatedVisibility(
            visible = showHeartPop,
            enter = scaleIn(
                initialScale = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(120)),
            exit = scaleOut(targetScale = 1.3f, animationSpec = tween(300)) +
                    fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = UnovColors.Accent,
                modifier = Modifier.size(140.dp)
            )
        }

        // Right action rail
        ActionRail(
            video = video,
            liked = liked,
            saved = saved,
            followed = followed,
            muted = muted,
            onToggleMute = { muted = !muted },
            onToggleLike = { liked = !liked },
            onToggleSave = { saved = !saved },
            onToggleFollow = { followed = true },
            onCommentClick = onCommentClick,
            onGiftClick = onGiftClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 100.dp)
        )

        // Bottom info (creator + caption + sound)
        BottomInfo(
            video = video,
            followed = followed,
            onToggleFollow = { followed = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 96.dp)
                .fillMaxWidth(0.78f)
        )
    }
}

/* ---------- Trending badge ---------- */

@Composable
private fun TrendingBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF0A0A0A).copy(alpha = 0.78f))
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(start = 6.dp, top = 5.dp, end = 10.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(UnovGradients.Gold),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFF0D0D0D),
                modifier = Modifier.size(10.dp)
            )
        }
        Column {
            Text(
                text = "TENDANCE",
                color = UnovColors.TextMute,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "#1 · Bénin",
                color = UnovColors.Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.1).sp
            )
        }
    }
}

/* ---------- Action rail (right side) ---------- */

@Composable
private fun ActionRail(
    video: FeedVideoUi,
    liked: Boolean,
    saved: Boolean,
    followed: Boolean,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleFollow: () -> Unit,
    onCommentClick: () -> Unit,
    onGiftClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MuteToggle(muted = muted, onClick = onToggleMute)

        AvatarWithFollow(
            avatarIdx = video.creatorAvatarIdx,
            username = video.creatorUsername,
            followed = followed,
            onToggleFollow = onToggleFollow
        )

        ActionPill(
            icon = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            tint = if (liked) UnovColors.Accent else Color.White,
            count = video.likesFmt,
            onClick = onToggleLike,
            label = "J'aime"
        )

        ActionPill(
            icon = Icons.Outlined.ChatBubbleOutline,
            tint = Color.White,
            count = video.commentsFmt,
            onClick = onCommentClick,
            label = "Commentaires"
        )

        ActionPill(
            icon = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            tint = if (saved) Color(0xFFF5CF6E) else Color.White,
            count = "412",
            onClick = onToggleSave,
            label = "Enregistrer"
        )

        GiftButton(onClick = onGiftClick)

        ActionPill(
            icon = Icons.AutoMirrored.Outlined.Send,
            tint = Color.White,
            count = video.sharesFmt,
            onClick = {},
            label = "Partager"
        )

        MusicDisc()
    }
}

@Composable
private fun MuteToggle(muted: Boolean, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF141414).copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
            contentDescription = "Mute",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun AvatarWithFollow(
    avatarIdx: Int,
    username: String,
    followed: Boolean,
    onToggleFollow: () -> Unit
) {
    Box(modifier = Modifier.size(56.dp)) {
        // Conic ring or simple border
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (followed) Brush.linearGradient(listOf(Color(0xFF050505), Color(0xFF050505)))
                    else Brush.sweepGradient(
                        listOf(UnovColors.Accent, Color(0xFFF4C430), UnovColors.AccentDeep, UnovColors.Accent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                Avatar(idx = avatarIdx, name = username, size = 40.dp)
            }
        }
        // Follow + button or check
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (followed) Modifier
                        .background(Color(0xFF0D0D0D))
                        .border(2.dp, UnovColors.Accent, CircleShape)
                    else Modifier.background(UnovGradients.Gold)
                )
                .clickable(enabled = !followed, onClick = onToggleFollow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (followed) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = if (followed) "Abonné" else "Suivre",
                tint = if (followed) UnovColors.Accent else Color(0xFF0D0D0D),
                modifier = Modifier.size(if (followed) 11.dp else 14.dp)
            )
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    tint: Color,
    count: String,
    onClick: () -> Unit,
    label: String
) {
    val noRipple = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(
            interactionSource = noRipple,
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF141414).copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = count,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GiftButton(onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(
            interactionSource = noRipple,
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(UnovGradients.Gold),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CardGiftcard,
                contentDescription = "Cadeau",
                tint = Color(0xFF0D0D0D),
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = "Cadeau",
            color = UnovColors.Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MusicDisc() {
    val transition = rememberInfiniteTransition(label = "discSpin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "discRotation"
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .rotate(rotation)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A1A1A), Color(0xFF444444), Color(0xFF1A1A1A))
                )
            )
            .border(2.dp, UnovColors.Accent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = UnovColors.Accent,
            modifier = Modifier.size(14.dp)
        )
    }
}

/* ---------- Bottom info ---------- */

@Composable
private fun BottomInfo(
    video: FeedVideoUi,
    followed: Boolean,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Creator row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "@${video.creatorUsername}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Vérifié",
                tint = UnovColors.Accent,
                modifier = Modifier.size(16.dp)
            )
            if (!followed) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                        .clickable(onClick = onToggleFollow)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Suivre",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }

        // Caption with hashtag/mention coloring
        Text(
            text = highlightCaption(video.description),
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        // City + time pills
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF141414).copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = UnovColors.Accent,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "Cotonou",
                    color = Color.White,
                    fontSize = 10.sp,
                    letterSpacing = 0.2.sp
                )
            }
            Text(
                text = "il y a 2 h",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }

        // Sound row : animated waveform + sound name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Waveform()
            Text(
                text = "Son original · @${video.creatorUsername}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Anime des barres de hauteur variable — équivalent CSS `@keyframes wave` en Compose.
 * Chaque barre a son propre décalage de phase pour donner l'illusion d'une onde sonore.
 */
@Composable
private fun Waveform() {
    val baseHeights = listOf(6f, 10f, 4f, 8f, 12f, 5f, 9f, 7f, 11f, 4f)
    val transition = rememberInfiniteTransition(label = "wave")
    Row(
        modifier = Modifier.height(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        baseHeights.forEachIndexed { i, baseH ->
            val animatedH by transition.animateFloat(
                initialValue = baseH * 0.4f,
                targetValue = baseH,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing, delayMillis = i * 80),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(animatedH.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovColors.Accent)
            )
        }
    }
}

/** Colore les `#hashtag` et `@mentions` en or — comme le réseau d'origine. */
private fun highlightCaption(text: String): AnnotatedString = buildAnnotatedString {
    val tokens = text.split(' ')
    tokens.forEachIndexed { i, t ->
        if (i > 0) append(' ')
        when {
            t.startsWith('#') -> withStyle(
                SpanStyle(color = UnovColors.Accent, fontWeight = FontWeight.Medium)
            ) { append(t) }
            t.startsWith('@') -> withStyle(
                SpanStyle(color = UnovColors.Accent, fontWeight = FontWeight.Medium)
            ) { append(t) }
            else -> append(t)
        }
    }
}
