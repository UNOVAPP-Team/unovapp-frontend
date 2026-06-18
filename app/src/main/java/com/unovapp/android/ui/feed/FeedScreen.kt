@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.unovapp.android.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onOpenWallet: () -> Unit = {}
) {
    val videos = remember { MockFeedVideos }
    val walletViewModel: WalletViewModel = hiltViewModel()
    val jetonBalance by walletViewModel.balance.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { videos.size })
    var tab by rememberSaveable { mutableStateOf(FeedTab.ForYou) }
    var commentsForVideoId by remember { mutableStateOf<String?>(null) }
    var giftSheetOpen by remember { mutableStateOf(false) }

    // État de session — partagé entre toutes les pages du feed (comportement attendu sur TikTok).
    var muted by rememberSaveable { mutableStateOf(true) }
    var ecoActive by rememberSaveable { mutableStateOf(true) }
    // TODO: brancher sur ConnectivityManager dès que le module monitoring sera prêt.
    val networkQuality = remember { NetworkQuality.G3 }

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
                muted = muted,
                // Libère le pipeline vidéo (décodage HLS + upscale) tant qu'une feuille
                // recouvre le feed : inutile de décoder sous un scrim, et ça évite que
                // la vidéo concurrence les animations du panneau sur les appareils faibles.
                paused = giftSheetOpen || commentsForVideoId != null,
                onCommentClick = { commentsForVideoId = videos[page].id },
                onGiftClick = { giftSheetOpen = true },
                onChallengeClick = { /* TODO: ouvrir création Battle */ }
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
            commentCountFmt = video.commentsFmt,
            onDismiss = { commentsForVideoId = null }
        )
    }

    if (giftSheetOpen) {
        GiftSheet(
            balance = jetonBalance,
            onDismiss = { giftSheetOpen = false },
            onSend = { gift ->
                // Solde suffisant : on débite les jetons (paiement) et on ferme.
                walletViewModel.trySpend(gift.price.toLong())
                giftSheetOpen = false
            },
            onRecharge = {
                // Solde insuffisant : redirection vers l'écran de recharge.
                giftSheetOpen = false
                onOpenWallet()
            }
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
                    .border(2.dp, Color(0xFF050505), CircleShape),
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
