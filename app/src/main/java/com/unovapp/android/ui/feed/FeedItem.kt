package com.unovapp.android.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import kotlinx.coroutines.delay

/**
 * Une page du feed (pleine hauteur). Joue la vidéo si [isCurrentPage] et superpose le HUD.
 *
 * État partagé hoisté par [FeedScreen] : [muted] (couper le son est une décision feed-wide).
 * État propre à l'item : like / follow / save / caption expanded.
 */
@Composable
fun FeedItem(
    video: FeedVideoUi,
    isCurrentPage: Boolean,
    muted: Boolean,
    onCommentClick: () -> Unit = {},
    onGiftClick: () -> Unit = {},
    onChallengeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var reaction by remember(video.id) { mutableStateOf<Reaction?>(if (video.isLiked) Reaction.Like else null) }
    var followed by remember(video.id) { mutableStateOf(video.isFollowing) }
    var captionExpanded by remember(video.id) { mutableStateOf(false) }
    var progress by remember(video.id) { mutableFloatStateOf(0f) }

    // Pause manuelle (tap sur la vidéo, façon TikTok). Réinitialisée quand on quitte la page.
    var userPaused by remember(video.id) { mutableStateOf(false) }
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) userPaused = false
    }

    // Révélation premium du HUD (rail + infos) quand la vidéo devient active.
    val hud by animateFloatAsState(
        targetValue = if (isCurrentPage) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "hudReveal"
    )

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
                    onTap = { userPaused = !userPaused },
                    onDoubleTap = {
                        if (reaction == null) reaction = Reaction.Love
                        heartPopKey++
                    }
                )
            }
    ) {
        VideoPlayer(
            url = video.hlsUrl,
            isPlaying = isCurrentPage && !userPaused,
            muted = muted,
            onProgress = { progress = it },
            modifier = Modifier.fillMaxSize()
        )

        // Indicateur de pause central (façon TikTok) — visible uniquement en pause manuelle.
        androidx.compose.animation.AnimatedVisibility(
            visible = userPaused && isCurrentPage,
            enter = scaleIn(initialScale = 0.6f) + fadeIn(tween(120)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Lecture",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Scrims cinématiques : voile haut léger (header), dégradé bas riche (texte),
        // + vignette droite discrète pour détacher le rail d'actions du fond clair.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.34f),
                        0.16f to Color.Transparent,
                        0.52f to Color.Transparent,
                        0.82f to Color.Black.copy(alpha = 0.55f),
                        1f to Color.Black.copy(alpha = 0.90f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.26f)
                    )
                )
        )

        // Bloc tendance + Défier (top-start, sous le header)
        if (video.id == "v1") {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 72.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                TrendingBadge()
                ChallengeButton(onClick = onChallengeClick)
            }
        }

        // Heart pop sur double-tap
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

        // Rail droit épuré : Avatar+Follow, Like, Comment, Gift (MoMo), Send, More
        ActionRail(
            video = video,
            reaction = reaction,
            followed = followed,
            onReact = { reaction = it },
            onToggleFollow = { followed = true },
            onCommentClick = onCommentClick,
            onGiftClick = onGiftClick,
            onShareClick = { shareVideo(context, video) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    alpha = hud
                    translationX = (1f - hud) * 36.dp.toPx()
                }
                .padding(end = 10.dp, bottom = 110.dp)
        )

        // Bottom info compact
        BottomInfo(
            video = video,
            followed = followed,
            captionExpanded = captionExpanded,
            onToggleCaption = { captionExpanded = !captionExpanded },
            onToggleFollow = { followed = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .graphicsLayer {
                    alpha = hud
                    translationY = (1f - hud) * 24.dp.toPx()
                }
                .padding(start = 14.dp, end = 78.dp, bottom = 18.dp)
                .fillMaxWidth(0.82f)
        )

        // Progress vidéo discrète — 1.5dp en or, fond ardoise. Suit la position d'ExoPlayer.
        VideoProgressBar(
            progress = progress,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp)
        )
    }
}

/* ---------- Trending + Défier ---------- */

@Composable
private fun TrendingBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF0A0A0A).copy(alpha = 0.82f))
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.32f), RoundedCornerShape(999.dp))
            .padding(start = 6.dp, top = 5.dp, end = 12.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(UnovGradients.Gold),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFF0D0D0D),
                modifier = Modifier.size(12.dp)
            )
        }
        Column {
            Text(
                text = "TENDANCE",
                color = UnovColors.TextMute,
                fontSize = 8.5.sp,
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

/**
 * CTA Battle contextuel — n'apparaît que sur les vidéos tendance.
 * Tap → ouvre le flow de création de Battle avec la vidéo pré-sélectionnée comme adversaire.
 *
 * Pulse subtil pour attirer l'œil sans agresser.
 */
@Composable
private fun ChallengeButton(onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    val transition = rememberInfiniteTransition(label = "challengePulse")
    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "challengeGlow"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(UnovColors.Accent.copy(alpha = 0.08f))
            .border(1.dp, UnovColors.Accent.copy(alpha = glow), RoundedCornerShape(999.dp))
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "⚔",
            color = UnovColors.Accent,
            fontSize = 12.sp
        )
        Text(
            text = "DÉFIER",
            color = UnovColors.Accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

/* ---------- Action rail (right side) ---------- */

@Composable
private fun ActionRail(
    video: FeedVideoUi,
    reaction: Reaction?,
    followed: Boolean,
    onReact: (Reaction?) -> Unit,
    onToggleFollow: () -> Unit,
    onCommentClick: () -> Unit,
    onGiftClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarWithFollow(
            avatarIdx = video.creatorAvatarIdx,
            username = video.creatorUsername,
            followed = followed,
            onToggleFollow = onToggleFollow
        )

        ReactionAction(
            current = reaction,
            countFmt = video.likesFmt,
            onSelect = onReact
        )

        ActionPill(
            icon = Icons.Outlined.ChatBubbleOutline,
            tint = Color.White,
            count = video.commentsFmt,
            onClick = onCommentClick,
            label = "Commentaires"
        )

        // Bouton Cadeau = différenciateur Mobile Money — gradient or premium.
        GiftButton(onClick = onGiftClick)

        ActionPill(
            icon = Icons.AutoMirrored.Outlined.Send,
            tint = Color.White,
            count = video.sharesFmt,
            onClick = onShareClick,
            label = "Partager"
        )

        // Overflow secondaire : Save, Signaler, Pas intéressé, Copier le lien
        MoreButton(onClick = {})
    }
}

@Composable
private fun AvatarWithFollow(
    avatarIdx: Int,
    username: String,
    followed: Boolean,
    onToggleFollow: () -> Unit
) {
    Box(modifier = Modifier.size(58.dp)) {
        // Ring "tissé" — sweep multi-stops évoque le tissage kente sans figurer un drapeau.
        // Couleurs : or → ocre brûlée → terre cuite → brun profond → or. Lisible noir sur lumière.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    if (followed) {
                        Brush.linearGradient(listOf(UnovColors.AccentDeep, UnovColors.AccentDeep))
                    } else {
                        Brush.sweepGradient(
                            colors = listOf(
                                UnovColors.Accent,
                                Color(0xFFC9851F), // ocre brûlée
                                Color(0xFF8B3A2E), // terre cuite
                                Color(0xFF3E1F12), // brun profond
                                UnovColors.AccentDeep,
                                UnovColors.Accent
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                Avatar(idx = avatarIdx, name = username, size = 41.dp)
            }
        }
        // Bouton + / check (suivre)
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
    label: String,
    pop: Boolean = false
) {
    val noRipple = remember { MutableInteractionSource() }
    val pressed by noRipple.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pillPress"
    )
    val popScale by animateFloatAsState(
        targetValue = if (pop) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillPop"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .scale(pressScale)
            .clickable(
                interactionSource = noRipple,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .size(34.dp)
                .scale(popScale)
        )
        Text(
            text = count,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
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
                .size(48.dp)
                .clip(CircleShape)
                .background(UnovGradients.Gold),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Redeem,
                contentDescription = "Envoyer un cadeau (Mobile Money)",
                tint = Color(0xFF0D0D0D),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = "Cadeau",
            color = UnovColors.Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MoreButton(onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = "Plus d'options",
            tint = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(22.dp)
        )
    }
}

/* ---------- Bottom info ---------- */

@Composable
private fun BottomInfo(
    video: FeedVideoUi,
    followed: Boolean,
    captionExpanded: Boolean,
    onToggleCaption: () -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noRipple = remember { MutableInteractionSource() }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Ligne créateur — nom, vérifié, bouton Suivre inline
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = "@${video.creatorUsername}",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Vérifié",
                tint = UnovColors.Accent,
                modifier = Modifier.size(15.dp)
            )
            if (!followed) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .clickable(
                            interactionSource = noRipple,
                            indication = null,
                            onClick = onToggleFollow
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Suivre",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }

        // Caption tronquée à 2 lignes par défaut → tap pour étendre. Hashtags/mentions en or.
        Text(
            text = highlightCaption(video.description),
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            maxLines = if (captionExpanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(
                interactionSource = noRipple,
                indication = null,
                onClick = onToggleCaption
            )
        )

        // Méta-ligne fusionnée : ville · temps · son original (avec mini-disque qui tourne)
        MetaRow(creatorUsername = video.creatorUsername)
    }
}

/**
 * Méta-ligne unique : 📍 Cotonou · il y a 2 h · ♬ Son original — @creator
 * Remplace les anciens pills/waveform empilés. Le mini-disque rotatif donne le signal "audio".
 */
@Composable
private fun MetaRow(creatorUsername: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = UnovColors.Accent,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = "Cotonou",
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Dot()
        Text(
            text = "il y a 2 h",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
        Dot()
        MiniMusicDisc()
        Text(
            text = "Son original · @$creatorUsername",
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .size(2.dp)
            .clip(CircleShape)
            .background(UnovColors.TextMute)
    )
}

@Composable
private fun MiniMusicDisc() {
    val transition = rememberInfiniteTransition(label = "miniDisc")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "miniDiscRot"
    )
    Box(
        modifier = Modifier
            .size(16.dp)
            .rotate(rotation)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1F1F1F), Color(0xFF3A3A3A), Color(0xFF1F1F1F))
                )
            )
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = UnovColors.Accent,
            modifier = Modifier.size(8.dp)
        )
    }
}

/* ---------- Progress bar ---------- */

/**
 * Barre de progression 1.5dp en bas de l'item. Or sur ardoise. Suit ExoPlayer en temps réel
 * (via tick 4 Hz dans VideoPlayer). Discrète mais informative — clé pour le contexte 2G/3G où
 * l'utilisateur veut savoir s'il vaut le coup d'attendre la fin.
 */
@Composable
private fun VideoProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(999.dp))
                .background(UnovGradients.Gold)
        )
    }
}

/** Colore les `#hashtag` et `@mentions` en or. */
private fun highlightCaption(text: String): AnnotatedString = buildAnnotatedString {
    val tokens = text.split(' ')
    tokens.forEachIndexed { i, t ->
        if (i > 0) append(' ')
        when {
            t.startsWith('#') -> withStyle(
                SpanStyle(color = UnovColors.Accent, fontWeight = FontWeight.SemiBold)
            ) { append(t) }
            t.startsWith('@') -> withStyle(
                SpanStyle(color = UnovColors.Accent, fontWeight = FontWeight.SemiBold)
            ) { append(t) }
            else -> append(t)
        }
    }
}
