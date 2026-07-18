package com.unovapp.android.ui.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.components.ParticleBurst
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
    bottomPadding: Dp = 0.dp,
    paused: Boolean = false,
    onCommentClick: () -> Unit = {},
    onGiftClick: () -> Unit = {},
    onGiftCountClick: () -> Unit = {},
    onChallengeClick: () -> Unit = {},
    /** Appelé quand l'utilisateur like ou délike la vidéo (toggle). Fire-and-forget. */
    onLike: (videoId: String) -> Unit = {},
    /** État de suivi du créateur, issu du store partagé (persiste au scroll). */
    isFollowing: Boolean = false,
    /** Suivre le créateur (UUID). Géré par le FollowManager partagé (backend + rollback). */
    onFollow: (creatorId: String) -> Unit = {},
    /** true si la vidéo est celle de l'utilisateur connecté → on masque « Suivre ». */
    isSelf: Boolean = false,
    /** Ouvre le profil/stats du créateur (tap sur l'avatar ou le pseudo). */
    onOpenProfile: (creatorId: String) -> Unit = {},
    /** Lecteur UNIQUE partagé — non-null seulement pour la page courante. */
    player: androidx.media3.exoplayer.ExoPlayer? = null,
    /** Progression de lecture 0..1 (fournie par FeedScreen pour la page courante). */
    playbackProgress: Float = 0f,
    /** Durée totale de la vidéo (ms) — pour le chip temps pendant le scrub de la barre. */
    durationMs: Long = 0L,
    /** Seek utilisateur sur la barre de progression (fraction 0..1) — page courante. */
    onSeek: (Float) -> Unit = {},
    /** Affiche l'indicateur de pause central (l'utilisateur a mis en pause manuellement). */
    showPauseIndicator: Boolean = false,
    /** Tap sur la vidéo → bascule lecture/pause (géré par FeedScreen). */
    onTogglePlay: () -> Unit = {},
    /** Sauvegarder / retirer des favoris (toggle, backend). */
    onSave: () -> Unit = {},
    /** Tracking de partage backend (compteur réel), en plus du partage système. */
    onShare: () -> Unit = {},
    /** Signaler la vidéo (reason ∈ spam|violence|nudity|harassment|other). */
    onReport: (reason: String) -> Unit = {}
) {
    val context = LocalContext.current
    // Réaction choisie (type : cœur, feu, éclair…), persistée côté client via ReactionMemory
    // → l'icône reflète VRAIMENT le sticker choisi et survit au scroll/recomposition.
    val reaction: Reaction? = ReactionMemory.map[video.id]
    LaunchedEffect(video.id, video.isLiked) {
        // Cœur par défaut pour une vidéo déjà likée (sans écraser un sticker explicite persisté).
        if (video.isLiked) ReactionMemory.setDefault(video.id, Reaction.Love)
    }
    // SOURCE UNIQUE : le FollowStore partagé (persisté, alimenté par l'enrichment du feed,
    // les profils visités et les listes d'abonnements). L'ancien `|| video.isFollowing`
    // créait une 2ᵉ source de vérité figée : un désabonnement fait depuis le profil restait
    // affiché « Suivi » dans le feed, et les divergences étaient invisibles.
    val followed = isFollowing
    var captionExpanded by remember(video.id) { mutableStateOf(false) }
    val progress = playbackProgress
    var moreOpen by remember(video.id) { mutableStateOf(false) }
    var shareOpen by remember(video.id) { mutableStateOf(false) }

    // Révélation premium du HUD (rail + infos) quand la vidéo devient active.
    val hud by animateFloatAsState(
        targetValue = if (isCurrentPage) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "hudReveal"
    )

    // Heart pop : compteur incrémenté à chaque double-tap → déclenche l'animation.
    // Position mémorisée = le cœur surgit SOUS LE DOIGT (façon TikTok), incliné aléatoirement.
    var heartPopKey by remember(video.id) { mutableIntStateOf(0) }
    var showHeartPop by remember { mutableStateOf(false) }
    var heartPopPos by remember { mutableStateOf(Offset.Zero) }
    val heartTilt = remember(heartPopKey) {
        if (heartPopKey == 0) 0f else Random.nextInt(-16, 17).toFloat()
    }

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
                    onTap = { onTogglePlay() },
                    onDoubleTap = { pos ->
                        // Double-tap = j'aime (façon TikTok) : cœur + like si pas déjà réagi.
                        if (ReactionMemory.map[video.id] == null) {
                            ReactionMemory.set(video.id, Reaction.Love)
                            onLike(video.id)
                        }
                        heartPopPos = pos
                        heartPopKey++
                    }
                )
            }
    ) {
        // Surface partagée : n'attache le lecteur UNIQUE qu'à la page courante (player != null).
        FeedVideoSurface(
            player = player,
            thumbnailUrl = video.thumbnailUrl,
            modifier = Modifier.fillMaxSize()
        )

        // Indicateur de pause central (façon TikTok) — visible uniquement en pause manuelle.
        androidx.compose.animation.AnimatedVisibility(
            visible = showPauseIndicator,
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

        // Heart pop sur double-tap — surgit sous le doigt, incliné, avec salve d'étincelles.
        if (heartPopKey > 0) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (heartPopPos.x - 100.dp.toPx()).roundToInt(),
                            (heartPopPos.y - 110.dp.toPx()).roundToInt()
                        )
                    }
                    .size(200.dp)
            ) {
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
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer { rotationZ = heartTilt }
                    )
                }
                ParticleBurst(
                    trigger = heartPopKey,
                    particleCount = 26,
                    maxRadius = 92.dp,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        // Rail droit : Avatar+Follow, Like, Comment, Gift, Send, More, Disque musical.
        // bottom = 76dp : le disque (dernier item) s'aligne visuellement avec
        // la ligne ♬ du BottomInfo à gauche — cohérence horizontale TikTok.
        ActionRail(
            video = video,
            reaction = reaction,
            followed = followed,
            onReact = { newReaction ->
                // La réaction choisie (like/cœur/feu/…) est mémorisée telle quelle → l'icône du
                // rail affiche le bon sticker. Le backend ne connaît qu'un like booléen.
                val wasReacted = ReactionMemory.map[video.id] != null
                if (newReaction != null) ReactionMemory.set(video.id, newReaction)
                else ReactionMemory.clear(video.id)
                if ((newReaction != null) != wasReacted) onLike(video.id)
            },
            onToggleFollow = { onFollow(video.creatorId) },
            isSelf = isSelf,
            onAvatarClick = { onOpenProfile(video.creatorId) },
            isSaved = video.isSaved,
            onSaveClick = onSave,
            onCommentClick = onCommentClick,
            onGiftClick = onGiftClick,
            onGiftCountClick = onGiftCountClick,
            onShareClick = { shareOpen = true },
            onMoreClick = { moreOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    alpha = hud
                    translationX = (1f - hud) * 36.dp.toPx()
                }
                .padding(end = 10.dp, bottom = 60.dp + bottomPadding)
        )

        // Bottom info compact — 60dp du bas pour respirer au-dessus de la barre de nav.
        BottomInfo(
            video = video,
            followed = followed,
            isSelf = isSelf,
            captionExpanded = captionExpanded,
            onToggleCaption = { captionExpanded = !captionExpanded },
            onToggleFollow = { onFollow(video.creatorId) },
            onOpenProfile = { onOpenProfile(video.creatorId) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .graphicsLayer {
                    alpha = hud
                    translationY = (1f - hud) * 24.dp.toPx()
                }
                .padding(start = 14.dp, end = 82.dp, bottom = 60.dp + bottomPadding)
                .fillMaxWidth(0.82f)
        )

        // Progress vidéo scrubbable — trait or discret, s'épaissit au drag (seek façon TikTok).
        VideoProgressBar(
            progress = progress,
            // Durée du player si déjà résolue, sinon celle annoncée par l'API (chip toujours utile).
            durationMs = if (durationMs > 0) durationMs else video.durationSec * 1000L,
            enabled = isCurrentPage,
            onSeek = onSeek,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
        )

        if (moreOpen) {
            MoreActionsSheet(
                video = video,
                isSaved = video.isSaved,
                onToggleSave = onSave,
                onReport = onReport,
                onDismiss = { moreOpen = false }
            )
        }

        if (shareOpen) {
            ShareSheet(
                video = video,
                onShareTracked = onShare,
                onDismiss = { shareOpen = false }
            )
        }
    }
}

/* ---------- Action rail (right side) ---------- */

/** Ombre portée douce pour tout texte posé sur la vidéo (lisibilité sur fond clair). */
private val HudTextShadow = Shadow(color = Color(0xB3000000), blurRadius = 8f)

/**
 * Compteur animé : quand la valeur change, l'ancien chiffre glisse vers le haut et le
 * nouveau entre par le bas — au lieu de « sauter » brutalement.
 */
@Composable
private fun AnimatedCount(
    count: String,
    color: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    onClick: (() -> Unit)? = null
) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            (slideInVertically { it } + fadeIn(tween(160))) togetherWith
                (slideOutVertically { -it } + fadeOut(tween(160)))
        },
        label = "count"
    ) { value ->
        Text(
            text = value,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(shadow = HudTextShadow),
            modifier = if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
    }
}

/** Icône du rail sur socle d'ombre radiale — lisible sur n'importe quelle vidéo. */
@Composable
private fun RailIcon(
    icon: ImageVector,
    tint: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(36.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun ActionRail(
    video: FeedVideoUi,
    reaction: Reaction?,
    followed: Boolean,
    onReact: (Reaction?) -> Unit,
    onToggleFollow: () -> Unit,
    isSelf: Boolean,
    onAvatarClick: () -> Unit,
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onCommentClick: () -> Unit,
    onGiftClick: () -> Unit,
    onGiftCountClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        // Espacement resserré + suppression du disque en bas → le rail tient dans l'écran
        // (avant, la colonne était trop haute et débordait au-dessus du header).
        verticalArrangement = Arrangement.spacedBy(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarWithFollow(
            avatarIdx = video.creatorAvatarIdx,
            username = video.creatorUsername,
            avatarUrl = video.avatarUrl,
            followed = followed,
            isSelf = isSelf,
            onAvatarClick = onAvatarClick,
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

        // Enregistrer (favoris) — signet plein or quand sauvegardé (pas de label texte :
        // l'état rempli/vide EST le message, comme TikTok).
        ActionPill(
            icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            tint = if (isSaved) UnovColors.Accent else Color.White,
            count = "",
            onClick = onSaveClick,
            label = "Enregistrer"
        )

        // Bouton Cadeau = différenciateur Mobile Money — gradient or premium.
        GiftButton(
            giftsFmt = video.giftsFmt,
            onClick = onGiftClick,
            onCountClick = onGiftCountClick
        )

        ActionPill(
            icon = Icons.AutoMirrored.Outlined.Send,
            tint = Color.White,
            count = video.sharesFmt,
            onClick = onShareClick,
            label = "Partager"
        )

        // Overflow secondaire : Signaler, Pas intéressé, Copier le lien
        MoreButton(onClick = onMoreClick)
    }
}

@Composable
private fun AvatarWithFollow(
    avatarIdx: Int,
    username: String,
    avatarUrl: String?,
    followed: Boolean,
    isSelf: Boolean,
    onAvatarClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    // 0 = pas suivi (bordure blanche TikTok), 1 = suivi (ring kente qui surgit en spring).
    val followProgress by animateFloatAsState(
        targetValue = if (followed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "followRing"
    )
    // Salve d'étincelles quand le suivi est confirmé (récompense immédiate).
    var followBurst by remember { mutableIntStateOf(0) }
    var firstFollow by remember { mutableStateOf(true) }
    LaunchedEffect(followed) {
        if (firstFollow) { firstFollow = false; return@LaunchedEffect }
        if (followed) followBurst++
    }

    Box(modifier = Modifier.size(64.dp)) {
        ParticleBurst(
            trigger = followBurst,
            particleCount = 18,
            maxRadius = 46.dp,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAvatarClick
                )
        ) {
            // Avant le suivi : bordure blanche fine, lisible sur la vidéo (style TikTok).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = (1f - followProgress).coerceIn(0f, 1f) }
                    .background(Color.White.copy(alpha = 0.88f))
            )
            // Après le suivi : ring kente qui surgit avec un pop spring (récompense visuelle).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = followProgress.coerceIn(0f, 1f)
                        val s = (0.6f + 0.4f * followProgress).coerceAtLeast(0f)
                        scaleX = s
                        scaleY = s
                    }
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                UnovColors.Accent,
                                Color(0xFFE55F00),
                                Color(0xFF8B3A14),
                                Color(0xFF3E1A08),
                                UnovColors.AccentDeep,
                                UnovColors.Accent
                            )
                        )
                    )
            )
            // Fond sombre + avatar — couche finale au-dessus des deux rings.
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = username,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(46.dp).clip(CircleShape)
                    )
                } else {
                    Avatar(idx = avatarIdx, name = username, size = 46.dp)
                }
            }
        }

        // Bouton + (suivre) — masqué sur ses propres vidéos.
        if (!isSelf) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(22.dp)
                    .clip(CircleShape)
                    .then(
                        if (followed) Modifier
                            .background(Color(0xFF0D0D0D))
                            .border(1.5.dp, UnovColors.Accent, CircleShape)
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
    val pressed by noRipple.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pillPress"
    )
    // Pulse bouncy quand l'ÉTAT change (icône outline ↔ remplie : save, réaction…).
    val statePop = remember { androidx.compose.animation.core.Animatable(1f) }
    var firstIcon by remember { mutableStateOf(true) }
    LaunchedEffect(icon) {
        if (firstIcon) { firstIcon = false; return@LaunchedEffect }
        statePop.snapTo(0.72f)
        statePop.animateTo(1f, com.unovapp.android.ui.theme.UnovMotion.bouncy())
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .scale(pressScale)
            .clickable(
                interactionSource = noRipple,
                indication = null,
                onClick = onClick
            )
    ) {
        RailIcon(
            icon = icon,
            tint = tint,
            label = label,
            modifier = Modifier.scale(statePop.value)
        )
        if (count.isNotBlank()) {
            AnimatedCount(count = count)
        }
    }
}

@Composable
private fun GiftButton(
    giftsFmt: String,
    onClick: () -> Unit,
    onCountClick: () -> Unit
) {
    val noRipple = remember { MutableInteractionSource() }
    // Halo orange qui respire : le cadeau garde sa présence de différenciateur Mobile Money
    // sans le gros disque plein qui écrasait la hiérarchie du rail.
    val halo by rememberInfiniteTransition(label = "giftHalo").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "giftHaloT"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val s = 0.9f + 0.25f * halo
                        scaleX = s
                        scaleY = s
                        alpha = 1.15f - halo
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(UnovColors.AccentGlowStrong, Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            RailIcon(icon = Icons.Outlined.Redeem, tint = UnovColors.AccentLight, label = "Envoyer un cadeau")
        }
        AnimatedCount(count = giftsFmt, color = UnovColors.AccentLight, onClick = onCountClick)
    }
}

@Composable
private fun MoreButton(onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = "Plus d'options",
            tint = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(26.dp)
        )
    }
}

/* ---------- Bottom info ---------- */

@Composable
private fun BottomInfo(
    video: FeedVideoUi,
    followed: Boolean,
    isSelf: Boolean,
    captionExpanded: Boolean,
    onToggleCaption: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noRipple = remember { MutableInteractionSource() }
    var nowMs by remember(video.createdAt) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(video.createdAt) {
        while (true) {
            delay(if (video.createdAt.isBlank()) 60_000L else 15_000L)
            nowMs = System.currentTimeMillis()
        }
    }
    val ageText = remember(video.createdAt, nowMs) { relativePublicationTime(video.createdAt, nowMs) }
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(shadow = HudTextShadow),
                modifier = Modifier.clickable(
                    interactionSource = noRipple,
                    indication = null,
                    onClick = onOpenProfile
                )
            )
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Vérifié",
                tint = UnovColors.Accent,
                modifier = Modifier.size(16.dp)
            )
            if (!followed && !isSelf) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .clickable(
                            interactionSource = noRipple,
                            indication = null,
                            onClick = onToggleFollow
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Suivre",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }

        // Caption tronquée à 2 lignes par défaut → tap pour étendre. Hashtags/mentions en or.
        var captionOverflows by remember(video.id) { mutableStateOf(false) }
        Text(
            text = highlightCaption(video.description),
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = if (captionExpanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = HudTextShadow),
            onTextLayout = { captionOverflows = it.hasVisualOverflow },
            modifier = Modifier.clickable(
                interactionSource = noRipple,
                indication = null,
                onClick = onToggleCaption
            )
        )
        // Affordance d'extension explicite (le « … » seul est facile à rater).
        if (captionOverflows && !captionExpanded) {
            Text(
                text = "Voir plus",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(shadow = HudTextShadow),
                modifier = Modifier.clickable(
                    interactionSource = noRipple,
                    indication = null,
                    onClick = onToggleCaption
                )
            )
        }

        // Méta-ligne fusionnée : 👁 vues · ville · temps · son original
        MetaRow(creatorUsername = video.creatorUsername, ageText = ageText, viewsFmt = video.viewsFmt)
    }
}

/**
 * Méta en 2 lignes hiérarchisées :
 *  1. 👁 vues · temps — l'info factuelle, discrète.
 *  2. mini-disque rotatif + « Son original · @creator » en marquee — le signal audio,
 *     qui défile s'il est trop long (façon TikTok).
 * (Le « 📍 Cotonou » codé en dur a été retiré : pas de fausse donnée à l'écran.)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MetaRow(creatorUsername: String, ageText: String, viewsFmt: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Compteur de vues (façon TikTok) — masqué si 0/inconnu.
            if (viewsFmt.isNotBlank() && viewsFmt != "0") {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = "Vues",
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "$viewsFmt vues",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(shadow = HudTextShadow)
                )
                Dot()
            }
            Text(
                text = ageText,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                style = TextStyle(shadow = HudTextShadow)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            TinyMusicDisc()
            Text(
                text = "Son original · @$creatorUsername",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.sp,
                maxLines = 1,
                style = TextStyle(shadow = HudTextShadow),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )
        }
    }
}

/** Mini-disque qui tourne — signal audio compact de la ligne musique. */
@Composable
private fun TinyMusicDisc() {
    val rotation by rememberInfiniteTransition(label = "tinyDisc").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(3500, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "tinyDiscRot"
    )
    Box(
        modifier = Modifier
            .size(18.dp)
            .rotate(rotation)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(colors = listOf(Color(0xFF2A2A2A), Color(0xFF101010)))
            )
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = UnovColors.Accent,
            modifier = Modifier.size(10.dp)
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
            .size(46.dp)
            .rotate(rotation)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1F1F1F), Color(0xFF3A3A3A), Color(0xFF1F1F1F))
                )
            )
            .border(1.5.dp, UnovColors.Accent.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = UnovColors.Accent,
            modifier = Modifier.size(24.dp)
        )
    }
}

/* ---------- Progress bar ---------- */

/** m:ss — pour le chip temps affiché pendant le scrub. */
private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/**
 * Barre de progression **scrubbable** façon TikTok, en bas de l'item.
 *
 *  - Au repos : trait discret 2.5dp, or sur ardoise (suit ExoPlayer en temps réel).
 *  - Drag horizontal (zone de toucher élargie à 32dp) : la barre s'épaissit, un point or
 *    marque la position visée et un chip « 0:12 / 0:45 » s'affiche au-dessus.
 *  - Relâchement (ou tap direct) → [onSeek] avec la fraction 0..1 : la vidéo saute là où
 *    l'utilisateur veut. La lecture continue pendant le drag (comme TikTok) — le seek ne
 *    part qu'une fois, au relâchement (important sur réseau lent : pas de re-buffer en rafale).
 *
 * Le drag est purement HORIZONTAL → les swipes verticaux du pager passent au travers.
 */
@Composable
private fun VideoProgressBar(
    progress: Float,
    durationMs: Long = 0L,
    enabled: Boolean = false,
    onSeek: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Fraction visée pendant le scrub — null = pas de scrub en cours (on suit la lecture).
    var scrub by remember { mutableStateOf<Float?>(null) }
    val shown = (scrub ?: progress).coerceIn(0f, 1f)
    val scrubbing = scrub != null

    val barHeight by animateDpAsState(
        targetValue = if (scrubbing) 7.dp else 2.5.dp,
        animationSpec = tween(durationMillis = 160),
        label = "barHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Zone de toucher généreuse (32dp) autour d'un trait de 2.5dp, sinon impossible
            // à attraper au doigt. Le rendu visuel reste collé en bas.
            .height(32.dp)
            .then(
                if (enabled) Modifier
                    .pointerInput(durationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset -> scrub = (offset.x / size.width).coerceIn(0f, 1f) },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scrub = ((scrub ?: 0f) + dragAmount / size.width).coerceIn(0f, 1f)
                            },
                            onDragEnd = { scrub?.let(onSeek); scrub = null },
                            onDragCancel = { scrub = null }
                        )
                    }
                    .pointerInput(durationMs) {
                        detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
                    }
                else Modifier
            )
    ) {
        // Chip temps pendant le scrub : « position visée / durée totale ».
        if (scrubbing && durationMs > 0) {
            Text(
                text = "${formatClock((shown * durationMs).toLong())} / ${formatClock(durationMs)}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = if (scrubbing) 0.28f else 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown)
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovGradients.Gold)
            )
        }
        // Point de repère or au bout de la barre pendant le scrub.
        if (scrubbing) {
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(barHeight)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * shown - 6.dp)
                        .align(Alignment.CenterStart)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(UnovColors.Accent)
                )
            }
        }
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


private fun relativePublicationTime(createdAt: String, nowMs: Long): String {
    val publishedMs = parseIsoUtc(createdAt) ?: return "il y a 2 h"
    val diff = (nowMs - publishedMs).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> "à l'instant"
        diff < 3_600_000L -> "il y a ${diff / 60_000L} min"
        diff < 86_400_000L -> "il y a ${diff / 3_600_000L} h"
        diff < 7L * 86_400_000L -> "il y a ${diff / 86_400_000L} j"
        else -> {
            val sdf = SimpleDateFormat("d MMM", Locale.FRENCH)
            sdf.format(java.util.Date(publishedMs))
        }
    }
}

private fun parseIsoUtc(value: String): Long? {
    if (value.isBlank()) return null
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(value)?.time
        }.getOrNull()
    }
}
