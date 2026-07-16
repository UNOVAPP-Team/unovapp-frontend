@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.unovapp.android.ui.feed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.unovapp.android.ui.comments.CommentsSheet
import com.unovapp.android.ui.wallet.WalletViewModel
import kotlinx.coroutines.delay

/**
 * Lecture plein écran d'une liste vidéo isolée (tap sur une vignette du profil : mes vidéos,
 * aimées, sauvegardées). Même expérience que le feed — VerticalPager + lecteur UNIQUE partagé,
 * donc aucune lecture simultanée — mais amorcé avec la liste fournie et démarré sur [startIndex].
 */
@Composable
fun VideoDetailScreen(
    videos: List<FeedVideoUi>,
    startIndex: Int,
    onBack: () -> Unit,
    onOpenWallet: () -> Unit = {},
    onOpenProfile: (creatorId: String) -> Unit = {},
    vm: VideoPagerViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { vm.setVideos(videos) }
    val list by vm.videos.collectAsStateWithLifecycle()
    val display = list.ifEmpty { videos }
    val following by vm.following.collectAsStateWithLifecycle()
    val currentUserId by vm.currentUserId.collectAsStateWithLifecycle()

    val walletViewModel: WalletViewModel = hiltViewModel()
    val jetonBalance by walletViewModel.balance.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (display.size - 1).coerceAtLeast(0)),
        pageCount = { display.size }
    )
    var commentsForVideoId by remember { mutableStateOf<String?>(null) }
    var giftSheetOpen by remember { mutableStateOf(false) }
    var giftBreakdownVideo by remember { mutableStateOf<FeedVideoUi?>(null) }
    var muted by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = navBarBottom + 16.dp

    // Enregistre une vue à chaque page vue.
    LaunchedEffect(pagerState.currentPage, display.size) {
        display.getOrNull(pagerState.currentPage)?.id?.let { vm.recordView(it) }
    }

    // ═══════════════════ POOL DE LECTEURS + PRÉFETCH (façon TikTok) ═══════════════════
    // Même architecture que le feed : fenêtre glissante préfetchée + cache disque partagé →
    // swipe instantané dans la liste (mes vidéos / aimées / sauvegardées). Un seul lecteur joue.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pool = remember { FeedPlayerPool(context, vm.mediaSourceFactory, vm.bandwidthMeter) }

    var userPaused by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) { userPaused = false }

    val overlayOpen = giftSheetOpen || commentsForVideoId != null || giftBreakdownVideo != null
    val shouldPlay = !overlayOpen && !userPaused && display.isNotEmpty()

    KeepScreenOn(enabled = shouldPlay)

    val urls = remember(display) { display.map { it.hlsUrl } }
    LaunchedEffect(pagerState.currentPage, urls) { pool.setWindow(pagerState.currentPage, urls, shouldPlay) }
    LaunchedEffect(shouldPlay) { pool.setPlaying(shouldPlay) }
    LaunchedEffect(muted) { pool.setMuted(muted) }

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            beyondBoundsPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // Même parallax inter-vidéos que le feed principal (profondeur au swipe).
            Box(
                modifier = Modifier.graphicsLayer {
                    val dist = pagerState.currentPage - page + pagerState.currentPageOffsetFraction
                    val f = kotlin.math.abs(dist).coerceIn(0f, 1f)
                    val s = 1f - 0.045f * f
                    scaleX = s
                    scaleY = s
                    alpha = 1f - 0.22f * f
                }
            ) {
            FeedItem(
                video = display[page],
                isCurrentPage = page == pagerState.currentPage,
                muted = muted,
                bottomPadding = bottomPadding,
                player = if (kotlin.math.abs(page - pagerState.currentPage) <= 1) pool.playerForPage(page) else null,
                playbackProgress = if (page == pagerState.currentPage) playbackProgress else 0f,
                durationMs = if (page == pagerState.currentPage) playbackDurationMs else 0L,
                // Scrub de la barre : saute à la position visée (au relâchement du drag).
                onSeek = { fraction ->
                    pool.currentPlayer()?.let { p ->
                        val d = p.duration
                        if (d > 0) {
                            p.seekTo((fraction * d).toLong().coerceIn(0L, d))
                            playbackProgress = fraction
                        }
                    }
                },
                showPauseIndicator = page == pagerState.currentPage && userPaused,
                onTogglePlay = { userPaused = !userPaused },
                onCommentClick = { commentsForVideoId = display[page].id },
                onGiftClick = { giftSheetOpen = true },
                onGiftCountClick = { giftBreakdownVideo = display[page] },
                onChallengeClick = { },
                onLike = { videoId -> vm.toggleLike(videoId) },
                onSave = { vm.toggleSave(display[page].id) },
                onShare = { vm.share(display[page].id) },
                onReport = { reason -> vm.reportVideo(display[page].id, reason) },
                isFollowing = following.contains(display[page].creatorId),
                onFollow = { creatorId -> vm.follow(creatorId) },
                isSelf = currentUserId != null && display[page].creatorId == currentUserId,
                onOpenProfile = { creatorId -> if (creatorId.isNotBlank()) onOpenProfile(creatorId) }
            )
            }
        }

        // Retour (haut-gauche) — chaque bouton a SA propre source d'interaction (en partager
        // une seule entre deux clickables empêchait le clic de se déclencher).
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 8.dp, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Son (haut-droite)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(end = 8.dp, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { muted = !muted },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                if (muted) "Activer le son" else "Couper le son",
                tint = Color.White, modifier = Modifier.size(20.dp)
            )
        }
    }

    commentsForVideoId?.let { videoId ->
        val video = display.first { it.id == videoId }
        CommentsSheet(
            videoId = videoId,
            commentCountFmt = video.commentsFmt,
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
                display.getOrNull(pagerState.currentPage)?.id?.let { vm.addGift(it) }
                giftSheetOpen = false
            },
            onRecharge = {
                giftSheetOpen = false
                onOpenWallet()
            }
        )
    }

    giftBreakdownVideo?.let { video ->
        GiftBreakdownSheet(video = video, onDismiss = { giftBreakdownVideo = null })
    }
}
