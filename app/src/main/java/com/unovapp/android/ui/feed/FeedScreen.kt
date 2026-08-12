@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.unovapp.android.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import coil.imageLoader
import coil.request.ImageRequest
import com.unovapp.android.ui.components.EmptyState
import com.unovapp.android.ui.components.ErrorRetry
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unovapp.android.data.video.VideoApi
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
import androidx.compose.runtime.mutableIntStateOf
import com.unovapp.android.ui.theme.Ember
import com.unovapp.android.ui.theme.EmberMotion
import com.unovapp.android.ui.theme.Haptics
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
    active: Boolean = true,
    /** L'Orbe pilote la navigation de l'app (remplace la barre d'onglets sur le Flux). */
    onNavigate: (com.unovapp.android.ui.components.MainTab) -> Unit = {},
    onOpenCreate: () -> Unit = {}
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
    // Nouvelle maquette : mode de tri (Suivis/Braise) + ouverture de la navigation de l'Orbe.
    var feedMode by rememberSaveable { mutableStateOf(EmberFeedMode.Braise) }
    var orbeOpen by remember { mutableStateOf(false) }
    // Compteur de Lueurs — chaque incrément déclenche le halo sur la vidéo.
    var lueurPulse by remember { mutableIntStateOf(0) }
    // Chaque interaction dans l'arc relance le timer d'inactivité.
    var orbeTick by remember { mutableIntStateOf(0) }
    val view = androidx.compose.ui.platform.LocalView.current
    var commentsForVideoId by remember { mutableStateOf<String?>(null) }
    var giftSheetOpen by remember { mutableStateOf(false) }
    var giftBreakdownVideo by remember { mutableStateOf<FeedVideoUi?>(null) }
    // Confirmation explicite (–1 jeton) avant la Braise : jamais par accident (§La Braise).
    var braiseConfirm by remember { mutableStateOf(false) }

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
            when {
                feedState.isLoading -> FeedPlaceholder()
                // Vraie panne réseau/serveur : on propose de réessayer.
                feedState.loadFailed -> ErrorRetry(
                    message = "Impossible de charger le feed.\nVérifie ta connexion.",
                    onRetry = { feedVm.loadFeed() },
                    modifier = Modifier.align(Alignment.Center)
                )
                // Requête réussie mais 0 vidéo — ce n'est PAS une panne. « Suivis » sans
                // abonnement est le cas courant : accuser la connexion serait un mensonge.
                feedState.feedType == VideoApi.FEED_FOLLOWING -> EmptyState(
                    title = "Tu ne suis encore personne",
                    subtitle = "Suis des créateurs pour voir leurs vidéos ici, ou reviens sur Braise.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> EmptyState(
                    title = "Aucune vidéo pour l'instant",
                    subtitle = "Reviens dans un moment — le flux se remplit vite.",
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
            // Parallax inter-vidéos : la page qui sort rétrécit légèrement et s'assombrit,
            // celle qui entre émerge — profondeur au swipe, sans toucher au pool vidéo.
            // Lu dans graphicsLayer {} → recalcul au draw, ZÉRO recomposition par frame.
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
            EmberFeedItem(
                video = videos[page],
                isCurrentPage = page == pagerState.currentPage,
                // Pool : la page courante ET ses voisines reçoivent leur lecteur (voisins =
                // préparés/en pause). Seule la courante joue → jamais deux lectures en même temps.
                player = if (kotlin.math.abs(page - pagerState.currentPage) <= 1) pool.playerForPage(page) else null,
                progress = if (page == pagerState.currentPage) playbackProgress else 0f,
                durationMs = if (page == pagerState.currentPage) playbackDurationMs else 0L,
                isFollowing = following.contains(videos[page].creatorId),
                isSelf = currentUserId != null && videos[page].creatorId == currentUserId,
                reacted = videos[page].isLiked || ReactionMemory.map.containsKey(videos[page].id),
                showPauseIndicator = page == pagerState.currentPage && userPaused,
                onTogglePlay = { userPaused = !userPaused },
                // Souffler = la réaction (le like backend, en attendant l'économie de braises).
                onSouffler = { videoId -> feedVm.toggleLike(videoId) },
                // Étinceler = les commentaires (ancrés à un instant — feuille Étincelles).
                onEtinceler = { commentsForVideoId = videos[page].id },
                onSuivre = { creatorId -> feedVm.follow(creatorId) },
                onOpenProfile = { creatorId -> if (creatorId.isNotBlank()) onOpenProfile(creatorId) },
                onOrbe = { orbeOpen = true },
                bottomInset = navBarBottom + 12.dp,
                awakened = orbeOpen && page == pagerState.currentPage,
                spark = feedState.sparks[videos[page].id],
                lueurPulse = if (page == pagerState.currentPage) lueurPulse else 0
            )
            }
        }

        // Chips du haut (Éco · 240p + toggle Suivis/Braise) — remplace l'ancien FeedHeader.
        // La bascule pilote réellement le tri : « Suivis » demande ?type=following au
        // backend, « Braise » retombe sur le flux chronologique.
        EmberFeedTop(
            ecoActive = ecoActive,
            mode = feedMode,
            onToggleMode = { mode ->
                feedMode = mode
                feedVm.setFeedType(
                    if (mode == EmberFeedMode.Suivis) VideoApi.FEED_FOLLOWING else null
                )
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Panneau « CE MOMENT » (écran 02) — les contextes actionnables de la vidéo
        // (son / lieu / défi), révélé à l'ÉVEIL sous le pouce. Il ne montre que les
        // champs réels de la vidéo courante : un son absent n'affiche pas de ligne.
        val ceVideo = videos.getOrNull(pagerState.currentPage)
        AnimatedVisibility(
            visible = orbeOpen,
            enter = fadeIn(tween(EmberMotion.DurBase, easing = EmberMotion.EaseOut)) + slideInVertically(
                initialOffsetY = { -it / 3 },
                animationSpec = tween(EmberMotion.DurBase, easing = EmberMotion.EaseOut)
            ),
            exit = fadeOut(tween(EmberMotion.DurQuick, easing = EmberMotion.EaseIn)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, top = 64.dp)
        ) {
            CeMomentPanel(
                soundLabel = ceVideo?.soundLabel,
                locationLabel = ceVideo?.locationLabel,
                challengeTag = ceVideo?.challengeTag
            )
        }

        // État ÉVEILLÉ convoqué par l'Orbe (maquette écran 02) : l'arc vertical de
        // réactions à droite + la rangée de navigation. Rien de tout ça n'existe au repos.
        val orbeVideo = videos.getOrNull(pagerState.currentPage)
        // Timer d'inactivité (§10.5) : l'état éveillé se retire tout seul après
        // 3,2 s sans interaction. L'interface du Flux n'existe que le temps d'un geste.
        // `orbeTick` relance le compte à rebours à chaque interaction (§10.5) : le
        // timer n'est PAS censé courir pendant qu'on agit.
        LaunchedEffect(orbeOpen, orbeTick) {
            if (orbeOpen) {
                orbeVideo?.id?.let { feedVm.loadSpark(it) }
                delay(3_200); orbeOpen = false
            }
        }
        OrbeNavRow(
            visible = orbeOpen,
            onDismiss = { orbeOpen = false },
            onNavigate = { orbeOpen = false; onNavigate(it) },
            onCreate = { orbeOpen = false; onOpenCreate() },
            bottomInset = navBarBottom,
            // L'arc reflète l'état réel de la vidéo courante : la pastille dit si
            // c'est déjà fait, au lieu de laisser l'utilisateur deviner.
            isLiked = orbeVideo?.isLiked == true,
            isSaved = orbeVideo?.isSaved == true,
            // Compteurs réels : lueurs = réactions gratuites, braises = cadeaux reçus,
            // étincelles = commentaires ancrés. Zéro n'affiche rien.
            lueurCount = orbeVideo?.likesCount ?: 0,
            braiseCount = orbeVideo?.giftsCount ?: 0,
            etincelleCount = orbeVideo?.commentsCount ?: 0,
            // Braise = réaction payante : une confirmation explicite (–1 jeton) s'interpose
            // (§La Braise — jamais par accident). La Lueur reste gratuite et sans garde-fou.
            onBraise = { orbeOpen = false; giftSheetOpen = true },
            // ── Les deux BASCULES ne referment PAS l'arc ────────────────────────
            // La spec ne liste que trois déclencheurs de fermeture : tap sur l'Orbe,
            // tap ailleurs, ou 3,2 s d'inactivité (§6.7). En fermant sur l'action,
            // on empêchait l'utilisateur de voir sa propre bascule — le geste
            // semblait sans effet. On relance simplement le compte à rebours.
            onLueur = {
                orbeVideo?.let { feedVm.toggleLike(it.id) }
                lueurPulse++
                Haptics.light(view)
                orbeTick++
            },
            onGarder = {
                orbeVideo?.let { feedVm.toggleSave(it.id) }
                Haptics.light(view)
                orbeTick++
            },
            // ── Celles-ci ouvrent autre chose : là, fermer l'arc est justifié ───
            onEtinceler = { orbeOpen = false; commentsForVideoId = orbeVideo?.id },
            onEnvoyer = { orbeOpen = false; orbeVideo?.let { shareVideo(context, it) } }
        )
    }

    commentsForVideoId?.let { videoId ->
        val video = videos.first { it.id == videoId }
        CommentsSheet(
            videoId = videoId,
            commentCountFmt = video.commentsFmt,
            // Le créateur de la vidéo peut épingler des commentaires.
            isVideoOwner = currentUserId != null && video.creatorId == currentUserId,
            // L'instant où l'Étincelle se plantera : la position de lecture au moment
            // où l'on ouvre la feuille. C'est tout le propos — « Dis-le à 0:06 ».
            anchorMs = (playbackProgress * playbackDurationMs).toLong().takeIf { playbackDurationMs > 0L },
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

    // ── CONFIRMATION DE LA BRAISE (–1 jeton) ─────────────────────────────────────
    // Le point critique de la spec : la bascule Lueur (gratuite) → Braise (payante)
    // doit être impossible par accident. Une confirmation explicite s'interpose,
    // affichant le solde réel de la Réserve. Solde insuffisant → on propose la recharge.
    if (braiseConfirm) {
        val solde = jetonBalance
        // `orbeVideo` vit dans la portée du Box de l'arc ; ici on est au niveau du
        // composable, d'où une relecture de la vidéo courante pour nommer le créateur.
        val confirmVideo = videos.getOrNull(pagerState.currentPage)
        androidx.compose.ui.window.Dialog(onDismissRequest = { braiseConfirm = false }) {
            val shape = RoundedCornerShape(24.dp)
            Column(
                modifier = Modifier
                    .clip(shape)
                    .background(Ember.Surface)
                    .border(1.dp, Ember.Braise.copy(alpha = 0.45f), shape)
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Ember.BraiseClair, Ember.Braise),
                                center = androidx.compose.ui.geometry.Offset(0.5f, 0.35f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = Ember.Bg, modifier = Modifier.size(28.dp))
                }
                Text(
                    "Souffler sur la braise ?",
                    color = Ember.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "1 jeton prélevé sur ta Réserve. Le créateur qui la reçoit peut la reverser en FCFA — ton soutien devient du revenu.",
                    color = Ember.TextDim,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Ta Réserve : $solde jeton${if (solde > 1) "s" else ""}",
                    color = if (solde >= 1) Ember.BraiseClair else Ember.Gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp)
                )
                if (solde >= 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .border(1.dp, Ember.Line, RoundedCornerShape(999.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { braiseConfirm = false }
                                .padding(horizontal = 22.dp, vertical = 11.dp)
                        ) {
                            Text("Annuler", color = Ember.TextDim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Ember.Braise)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (walletViewModel.trySpend(1)) {
                                        Haptics.medium(view)
                                        braiseConfirm = false
                                        orbeOpen = false
                                        android.widget.Toast.makeText(
                                            context,
                                            "Braise soufflée — 1 jeton pour ${confirmVideo?.creatorUsername?.let { "@$it" } ?: "le créateur"}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        braiseConfirm = false
                                        onOpenWallet()
                                    }
                                }
                                .padding(horizontal = 22.dp, vertical = 11.dp)
                        ) {
                            Text("Souffler — 1 jeton", color = Ember.Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Ember.Gold)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                braiseConfirm = false
                                onOpenWallet()
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Recharger la Réserve", color = Ember.Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
