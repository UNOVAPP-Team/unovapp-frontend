@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.unovapp.android.ui.feed

import androidx.compose.animation.AnimatedVisibility
import coil.imageLoader
import coil.request.ImageRequest
import com.unovapp.android.ui.components.ErrorRetry
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.unovapp.android.ui.theme.UnovGradients
import kotlin.math.roundToInt
import com.unovapp.android.ui.comments.CommentsSheet
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.wallet.WalletViewModel

private enum class FeedTab(val label: String) {
    ForYou("Pour Toi"),
    Following("Abonnements")
}

@Composable
fun FeedScreen(
    onOpenWallet: () -> Unit = {},
    /** Ouvre le profil/stats d'un créateur (tap avatar/pseudo dans le feed). */
    onOpenProfile: (creatorId: String) -> Unit = {},
    /** false quand un écran/overlay recouvre le feed (création, wallet, profil visité…) :
     *  on met alors la vidéo en pause pour couper l'image ET le son. */
    active: Boolean = true
) {
    val feedVm: FeedViewModel = hiltViewModel()
    val feedState by feedVm.state.collectAsStateWithLifecycle()
    val following by feedVm.following.collectAsStateWithLifecycle()
    val currentUserId by feedVm.currentUserId.collectAsStateWithLifecycle()
    // Jamais de vidéos mockées : pendant le chargement initial → FeedPlaceholder (squelette),
    // en échec réseau sans cache → état erreur avec « Réessayer ». Les mocks bufferisaient en
    // plus des flux de test étrangers au démarrage (bande passante gaspillée).
    val videos = feedState.videos
    val walletViewModel: WalletViewModel = hiltViewModel()
    val jetonBalance by walletViewModel.balance.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(
        initialPage = feedState.currentPage.coerceIn(0, (videos.size - 1).coerceAtLeast(0)),
        pageCount = { videos.size }
    )
    var tab by rememberSaveable { mutableStateOf(FeedTab.ForYou) }
    var commentsForVideoId by remember { mutableStateOf<String?>(null) }
    var giftSheetOpen by remember { mutableStateOf(false) }
    var giftBreakdownVideo by remember { mutableStateOf<FeedVideoUi?>(null) }

    // État de session — partagé entre toutes les pages du feed (comportement attendu sur TikTok).
    var muted by rememberSaveable { mutableStateOf(false) }
    var ecoActive by rememberSaveable { mutableStateOf(true) }
    // TODO: brancher sur ConnectivityManager dès que le module monitoring sera prêt.
    val networkQuality = remember { NetworkQuality.G3 }

    // Padding pour positionner les overlays (ActionRail, BottomInfo) au-dessus de la BottomNav.
    // 78.dp = hauteur visuelle de la pilule BottomNav (6dp top + 60dp Row + 12dp bottom).
    // Calculé depuis WindowInsets directement — indépendant du Scaffold.
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = navBarBottom + 78.dp

    // Enregistre une vue + mémorise la page/vidéo courante (pour retrouver la même vidéo
    // en revenant sur le feed, au lieu de repartir de la première).
    LaunchedEffect(pagerState.currentPage, videos.size) {
        val page = pagerState.currentPage
        val vid = videos.getOrNull(page)?.id
        vid?.let { feedVm.recordView(it) }
        feedVm.rememberCurrentPage(page, vid)
    }
    // Charge la page suivante quand on approche de la fin (3 items avant la fin).
    LaunchedEffect(pagerState.currentPage, videos.size) {
        if (pagerState.currentPage >= videos.size - 3) feedVm.loadMore()
    }

    // ═══════════════════ POOL DE LECTEURS + PRÉFETCH (façon TikTok) ═══════════════════
    // Plusieurs ExoPlayer préparés à l'avance (fenêtre {courante-1, courante, courante+1}) +
    // cache disque partagé → la vidéo suivante est DÉJÀ bufferisée = swipe instantané, sans
    // re-chargement. Le pool garantit toujours qu'UN SEUL lecteur joue à la fois.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pool = remember { FeedPlayerPool(context, feedVm.mediaSourceFactory, feedVm.bandwidthMeter) }

    // Pause manuelle (tap) — réinitialisée à chaque changement de page.
    var userPaused by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) { userPaused = false }

    val overlayOpen = giftSheetOpen || commentsForVideoId != null || giftBreakdownVideo != null
    // On ne joue QUE si le feed est au premier plan, sans overlay, et pas en pause manuelle.
    val shouldPlay = active && !overlayOpen && !userPaused && videos.isNotEmpty()

    // Empêche la mise en veille de l'écran pendant la lecture.
    KeepScreenOn(enabled = shouldPlay)

    // Fenêtre glissante : (re)prépare courante ± 1 dès que la page ou la liste change.
    val urls = remember(videos) { videos.map { it.hlsUrl } }
    LaunchedEffect(pagerState.currentPage, urls) { pool.setWindow(pagerState.currentPage, urls, shouldPlay) }
    LaunchedEffect(shouldPlay) { pool.setPlaying(shouldPlay) }
    LaunchedEffect(muted) { pool.setMuted(muted) }

    // Préchauffe les miniatures des 2 pages suivantes dans le cache Coil → le fond flouté de
    // la prochaine vidéo est déjà en mémoire au moment du swipe (zéro flash noir/réseau).
    LaunchedEffect(pagerState.currentPage, videos) {
        for (p in intArrayOf(pagerState.currentPage + 1, pagerState.currentPage + 2)) {
            val thumb = videos.getOrNull(p)?.thumbnailUrl
            if (!thumb.isNullOrBlank()) {
                context.imageLoader.enqueue(ImageRequest.Builder(context).data(thumb).build())
            }
        }
    }

    // Progression + durée de la vidéo active (barre de lecture scrubbable).
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var playbackDurationMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(pagerState.currentPage) {
        playbackProgress = 0f
        playbackDurationMs = 0L
        while (true) {
            val p = pool.currentPlayer()
            val d = p?.duration?.takeIf { it > 0 } ?: 0L
            val pos = p?.currentPosition?.coerceAtLeast(0L) ?: 0L
            playbackDurationMs = d
            playbackProgress = if (d > 0) (pos.toFloat() / d).coerceIn(0f, 1f) else 0f
            delay(200)
        }
    }

    // Cycle de vie : pause dès que l'app passe en arrière-plan, release de tous à la sortie du feed.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> pool.pauseAll()
                Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START -> pool.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pool.pauseAll()
            pool.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (videos.isEmpty()) {
            if (feedState.isLoading) {
                FeedPlaceholder()
            } else {
                ErrorRetry(
                    message = "Impossible de charger le feed.\nVérifie ta connexion.",
                    onRetry = { feedVm.loadFeed() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        VerticalPager(
            state = pagerState,
            // Compose la page voisine → sa 1ʳᵉ image est déjà rendue (avec le lecteur préfetché)
            // = aucun flash noir au swipe.
            beyondBoundsPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            FeedItem(
                video = videos[page],
                isCurrentPage = page == pagerState.currentPage,
                muted = muted,
                bottomPadding = bottomPadding,
                // Pool : la page courante ET ses voisines reçoivent leur lecteur (voisins =
                // préparés/en pause). Seule la courante joue → jamais deux lectures en même temps.
                player = if (kotlin.math.abs(page - pagerState.currentPage) <= 1) pool.playerForPage(page) else null,
                playbackProgress = if (page == pagerState.currentPage) playbackProgress else 0f,
                durationMs = if (page == pagerState.currentPage) playbackDurationMs else 0L,
                // Scrub de la barre : saute à la position visée (au relâchement du drag).
                onSeek = { fraction ->
                    pool.currentPlayer()?.let { p ->
                        val d = p.duration
                        if (d > 0) {
                            p.seekTo((fraction * d).toLong().coerceIn(0L, d))
                            playbackProgress = fraction // reflet immédiat, sans attendre le tick
                        }
                    }
                },
                showPauseIndicator = page == pagerState.currentPage && userPaused,
                onTogglePlay = { userPaused = !userPaused },
                onCommentClick = { commentsForVideoId = videos[page].id },
                onGiftClick = { giftSheetOpen = true },
                onGiftCountClick = { giftBreakdownVideo = videos[page] },
                onChallengeClick = { /* TODO: ouvrir création Battle */ },
                onLike = { videoId -> feedVm.toggleLike(videoId) },
                onSave = { feedVm.toggleSave(videos[page].id) },
                onShare = { feedVm.share(videos[page].id) },
                onReport = { reason -> feedVm.reportVideo(videos[page].id, reason) },
                // Le suivi reflète le store partagé (persiste au scroll, met à jour le profil).
                isFollowing = following.contains(videos[page].creatorId),
                onFollow = { creatorId -> feedVm.follow(creatorId) },
                // Masque « Suivre » sur mes propres vidéos + tap avatar/pseudo → profil créateur.
                isSelf = currentUserId != null && videos[page].creatorId == currentUserId,
                onOpenProfile = { creatorId -> if (creatorId.isNotBlank()) onOpenProfile(creatorId) }
            )
        }

        FeedHeader(
            tab = tab,
            onTabChange = { tab = it },
            networkQuality = networkQuality,
            ecoActive = ecoActive,
            onToggleEco = { ecoActive = !ecoActive },
            muted = muted,
            onToggleMute = { muted = !muted },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    commentsForVideoId?.let { videoId ->
        val video = videos.first { it.id == videoId }
        CommentsSheet(
            videoId = videoId,
            commentCountFmt = video.commentsFmt,
            // Le créateur de la vidéo peut épingler des commentaires.
            isVideoOwner = currentUserId != null && video.creatorId == currentUserId,
            onDismiss = { commentsForVideoId = null }
        )
    }

    if (giftSheetOpen) {
        GiftSheet(
            balance = jetonBalance,
            onDismiss = { giftSheetOpen = false },
            onSend = { gift ->
                walletViewModel.trySpend(gift.price.toLong())
                // Reflète le cadeau sur le compteur de la vidéo regardée (optimiste, local).
                videos.getOrNull(pagerState.currentPage)?.id?.let { feedVm.addGift(it) }
                giftSheetOpen = false
            },
            onRecharge = {
                giftSheetOpen = false
                onOpenWallet()
            }
        )
    }

    giftBreakdownVideo?.let { video ->
        GiftBreakdownSheet(
            video = video,
            onDismiss = { giftBreakdownVideo = null }
        )
    }
}

/* ---------- Header ---------- */

@Composable
private fun FeedHeader(
    tab: FeedTab,
    onTabChange: (FeedTab) -> Unit,
    networkQuality: NetworkQuality,
    ecoActive: Boolean,
    onToggleEco: () -> Unit,
    muted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Gradient noir → transparent — pas de carré opaque, juste un fade pour la lisibilité.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    1f to Color.Transparent
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)
        ) {
            // Puce réseau/éco compacte à gauche
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                NetworkQualityChip(
                    quality = networkQuality,
                    ecoActive = ecoActive,
                    onClick = onToggleEco
                )
            }
            // Onglets vraiment centrés
            Box(modifier = Modifier.align(Alignment.Center)) {
                TabsRow(tab = tab, onTabChange = onTabChange)
            }
            // Un seul bouton à droite : le son (recherche/notifs retirés → déjà dans la nav)
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                HeaderIconButton(
                    icon = if (muted) Icons.AutoMirrored.Outlined.VolumeOff
                    else Icons.AutoMirrored.Outlined.VolumeUp,
                    contentDescription = if (muted) "Activer le son" else "Couper le son",
                    onClick = onToggleMute
                )
            }
        }
    }
}

@Composable
private fun TabsRow(tab: FeedTab, onTabChange: (FeedTab) -> Unit) {
    val tabs = remember { FeedTab.values().reversed() }
    val density = LocalDensity.current
    // Position (x) et largeur de chaque onglet, mesurées au layout → indicateur glissant.
    val lefts = remember { mutableStateListOf(*Array(tabs.size) { 0f }) }
    val widths = remember { mutableStateListOf(*Array(tabs.size) { 0f }) }
    val activeIndex = tabs.indexOf(tab).coerceAtLeast(0)

    val animLeft by animateFloatAsState(
        targetValue = lefts.getOrElse(activeIndex) { 0f },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "indicatorLeft"
    )
    val animWidth by animateFloatAsState(
        targetValue = widths.getOrElse(activeIndex) { 0f },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "indicatorWidth"
    )

    Box {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            tabs.forEachIndexed { i, t ->
                TabItem(
                    label = t.label,
                    isActive = t == tab,
                    onClick = { onTabChange(t) },
                    onMeasured = { x, w -> lefts[i] = x; widths[i] = w }
                )
            }
        }
        // Indicateur doré court qui glisse sous l'onglet actif (centré + spring).
        val indWidthDp = 26.dp
        val indWidthPx = with(density) { indWidthDp.toPx() }
        val centeredLeft = animLeft + (animWidth - indWidthPx) / 2f
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset(centeredLeft.roundToInt(), 0) }
                .width(indWidthDp)
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(UnovGradients.Gold)
        )
    }
}

@Composable
private fun TabItem(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onMeasured: (x: Float, width: Float) -> Unit
) {
    val noRipple = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "tabScale"
    )
    val color by animateColorAsState(
        targetValue = if (isActive) Color.White else UnovColors.TextDim.copy(alpha = 0.55f),
        animationSpec = tween(260),
        label = "tabColor"
    )
    Text(
        text = label,
        color = color,
        fontSize = 16.sp,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .onGloballyPositioned { c -> onMeasured(c.positionInParent().x, c.size.width.toFloat()) }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
    )
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    badge: String? = null
) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE54646))
                    .border(2.dp, Color(0xFF0D0D0D), CircleShape),
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
