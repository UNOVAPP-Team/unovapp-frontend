package com.unovapp.android.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.unovapp.android.ui.components.enterFadeSlide
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import com.unovapp.android.ui.challenge.ChallengeSection
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.components.EmptyState
import com.unovapp.android.ui.components.ParticleBurst
import com.unovapp.android.ui.components.ErrorRetry
import com.unovapp.android.ui.components.ShimmerBox
import com.unovapp.android.ui.components.Filigree
import com.unovapp.android.ui.components.LanguageChip
import com.unovapp.android.ui.components.LanguagePickerSheet
import com.unovapp.android.ui.components.Sparkline
import com.unovapp.android.ui.components.StaggerReveal
import com.unovapp.android.ui.components.rememberCountUp
import com.unovapp.android.ui.components.unovTap
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import com.unovapp.android.ui.theme.UnovMotion
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

/* ---------- State ---------- */

data class ProfileUiState(
    val displayName: String = "",
    val username: String = "",
    val isVerified: Boolean = false,
    val tier: String = "Tier Free",
    val city: String = "",
    val bio: String = "",
    val website: String = "",
    val avatarUrl: String? = null,
    /** Photo de couverture (bannière) — null tant que le créateur n'en a pas choisi. */
    val coverUrl: String? = null,
    val followersFmt: String = "0",
    val likesFmt: String = "0",
    val videosFmt: String = "0",
    /** Valeurs brutes — pour les compteurs animés (count-up) des stats. */
    val followersCount: Int = 0,
    val likesCount: Int = 0,
    val videosCount: Int = 0,
    /** Abonnements (following) — 1ʳᵉ stat de la maquette. */
    val followingCount: Int = 0,
    val followingFmt: String = "0",
    val followersTrend: String = "",
    val videosTrend: String = "",
    val followersSpark: List<Float> = emptyList(),
    val likesSpark: List<Float> = emptyList(),
    val liveViewers: Int = 0,
    val revenueFcfaFmt: String = "0",
    val revenueTrend: String = "",
    val revenueSpark: List<Float> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
    val topFans: List<TopFan> = emptyList(),
    val recentBattles: List<BattleEntry> = emptyList(),
    val gridVideos: List<VideoTile> = emptyList()
)

data class Achievement(val icon: ImageVector, val label: String, val sub: String?, val accent: Boolean)
data class Highlight(val label: String, val count: String, val gradientIndex: Int, val live: Boolean = false)
data class TopFan(val avatarIdx: Int, val username: String, val gifts: String, val rank: Int)
data class BattleEntry(val opponent: String, val avatarIdx: Int, val won: Boolean, val votes: String, val date: String)
data class VideoTile(val gradientIndex: Int, val views: String, val durationSec: Int)

/* ---------- Demo data ---------- */

private val DEFAULT_ACHIEVEMENTS = listOf(
    Achievement(Icons.Outlined.WorkspacePremium, "Top 1% Bénin", "2026", accent = true),
    Achievement(Icons.Outlined.LocalFireDepartment, "1M+ vues", "14 vidéos", accent = false),
    Achievement(Icons.Outlined.Bolt, "Champion Battle", "37 victoires", accent = true),
    Achievement(Icons.Filled.Verified, "MoMo certifié", "Paiements garantis", accent = false),
    Achievement(Icons.Outlined.AutoAwesome, "Partenaire UNOVAPP", null, accent = false)
)

private val DEFAULT_HIGHLIGHTS = listOf(
    Highlight("Mamans 🇧🇯", "24", gradientIndex = 0, live = true),
    Highlight("Marché", "12", gradientIndex = 2),
    Highlight("Sketches", "48", gradientIndex = 4),
    Highlight("Battles", "9", gradientIndex = 3),
    Highlight("Voyage", "6", gradientIndex = 5),
    Highlight("BTS", "15", gradientIndex = 1)
)

private val DEFAULT_TOP_FANS = listOf(
    TopFan(1, "kossi.dance", "2 480", rank = 1),
    TopFan(3, "mariama_dak", "1 320", rank = 2),
    TopFan(5, "samuel228", "980", rank = 3),
    TopFan(2, "fatou_ben", "640", rank = 4),
    TopFan(4, "ange_ptn", "412", rank = 5)
)

private val DEFAULT_BATTLES = listOf(
    BattleEntry("le_chef_moise", 2, won = true, "2 840 vs 2 210", "hier"),
    BattleEntry("reezy_naija", 4, won = true, "4 120 vs 3 890", "mar."),
    BattleEntry("kossi.dance", 1, won = false, "1 980 vs 2 410", "lun.")
)

private val DEFAULT_VIDEOS = listOf(
    VideoTile(2, "1,2 M", 28),
    VideoTile(4, "412 K", 35),
    VideoTile(0, "89 K", 42),
    VideoTile(3, "67 K", 49),
    VideoTile(5, "54 K", 56),
    VideoTile(1, "31 K", 63)
)

/* ---------- Screen ---------- */

@Composable
fun ProfileScreen(
    baseState: ProfileUiState = ProfileUiState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onOpenBattle: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    onOpenConnections: (userId: String, username: String, showFollowers: Boolean) -> Unit = { _, _, _ -> },
    viewModel: ProfileViewModel = hiltViewModel()
) {
    UnovAppTheme {
        val net by viewModel.state.collectAsStateWithLifecycle()
        val followingDelta by viewModel.followingDelta.collectAsStateWithLifecycle()
        val likesReceivedDelta by viewModel.likesReceivedDelta.collectAsStateWithLifecycle()
        val avatarState by viewModel.avatarState.collectAsStateWithLifecycle()
        val coverState by viewModel.coverState.collectAsStateWithLifecycle()
        val videosState by viewModel.videos.collectAsStateWithLifecycle()

        // Session expirée/invalide (401) → on renvoie vers la connexion.
        androidx.compose.runtime.LaunchedEffect(net.sessionExpired) {
            if (net.sessionExpired) onLoggedOut()
        }

        // Données réelles (/users/me) fusionnées dans l'état d'affichage.
        // Les sections riches (battles, top fans, grille…) restent mockées tant que
        // le backend ne les expose pas.
        val state = mergeRealProfile(baseState, net.profile, followingDelta, likesReceivedDelta)
        val myId = net.profile?.id

        var subscribed by remember { mutableStateOf(false) }
        var tab by remember { mutableStateOf(ProfileVideoTab.Videos) }
        // Vidéo(s) en lecture plein écran (tap sur une vignette de la grille).
        var detailPlaying by remember { mutableStateOf<Pair<List<com.unovapp.android.ui.feed.FeedVideoUi>, Int>?>(null) }
        var filter by remember { mutableStateOf("Récents") }
        var langPickerOpen by remember { mutableStateOf(false) }
        var editOpen by remember { mutableStateOf(false) }
        // Challenges : écran de création + liste locale (pas encore d'API — cf.
        // docs/BACKEND_CHALLENGES.md). Les challenges créés restent visibles dans la session.
        var createChallengeOpen by remember { mutableStateOf(false) }
        var myChallenges by remember { mutableStateOf(listOf<com.unovapp.android.ui.challenge.ChallengeCard>()) }
        var settingsOpen by remember { mutableStateOf(false) }
        var photoSheetOpen by remember { mutableStateOf(false) }
        var photoViewerOpen by remember { mutableStateOf(false) }
        val context = LocalContext.current
        // Sélecteur d'image restreint aux formats acceptés par le backend (JPEG/PNG/WebP).
        val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val ct = context.contentResolver.getType(uri) ?: "image/jpeg"
                viewModel.uploadAvatar(ct, uri)
            }
        }
        // Sélecteur de photo de couverture (mêmes formats acceptés par le backend).
        val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val ct = context.contentResolver.getType(uri) ?: "image/jpeg"
                viewModel.uploadCover(ct, uri)
            }
        }
        // Erreur d'upload de couverture → message clair, puis on efface l'erreur.
        LaunchedEffect(coverState.error) {
            coverState.error?.let { msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                viewModel.clearCoverError()
            }
        }
        val scrollState = rememberScrollState()

        when {
            // Premier chargement échoué (aucune donnée) → erreur plein écran + retry.
            net.profile == null && net.error != null -> ProfileErrorState(
                message = "Impossible de charger ton profil.\n${net.error}",
                onRetry = { viewModel.load() }
            )
            // Premier chargement en cours → squelette (pas de flash de données mock).
            net.profile == null && net.isLoading -> ProfileLoadingState()
            else -> {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
                .verticalScroll(scrollState)
                .padding(contentPadding)
        ) {
            CoverHeader(
                state = state,
                scrollPx = scrollState.value,
                onOpenLangPicker = { langPickerOpen = true },
                onOpenSettings = { settingsOpen = true },
                coverUploading = coverState.isUploading,
                onChangeCover = {
                    coverPicker.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                }
            )
            IdentityBlock(
                state = state,
                subscribed = subscribed,
                onToggleSubscribe = { subscribed = !subscribed },
                onOpenBattle = onOpenBattle,
                onOpenWallet = onOpenWallet,
                onEditProfile = { editOpen = true },
                onOpenFollowers = { myId?.let { onOpenConnections(it, state.username, true) } },
                onOpenFollowing = { myId?.let { onOpenConnections(it, state.username, false) } },
                avatarUploading = avatarState.isUploading,
                onAvatarClick = { photoSheetOpen = true }
            )

            // Sections content-aware : on n'affiche que ce qui a réellement des données
            // (backend). Le reste reste masqué tant que les endpoints n'existent pas.
            if (state.achievements.isNotEmpty()) {
                StaggerReveal(index = 0) {
                    Column {
                        SectionHeader(eyebrow = "Distinctions")
                        HorizontalScrollRow(start = 16.dp, end = 16.dp) {
                            state.achievements.forEach { AchievementPill(it) }
                        }
                    }
                }
            }

            if (state.highlights.isNotEmpty()) {
                StaggerReveal(index = 1) {
                    Column {
                        SectionHeader(eyebrow = "Séries", action = "Tout voir")
                        HorizontalScrollRow(start = 16.dp, end = 16.dp, itemSpacing = 14.dp) {
                            state.highlights.forEach { HighlightCircle(it) }
                        }
                    }
                }
            }

            if (state.topFans.isNotEmpty()) {
                StaggerReveal(index = 2) {
                    Column {
                        SectionHeader(eyebrow = "Top fans")
                        HorizontalScrollRow(start = 16.dp, end = 16.dp, itemSpacing = 6.dp) {
                            state.topFans.forEach { TopFanChip(it) }
                        }
                    }
                }
            }

            if (state.recentBattles.isNotEmpty()) {
                StaggerReveal(index = 4) {
                    Column {
                        SectionHeader(eyebrow = "Derniers Battles", action = "Historique")
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.recentBattles.forEach { BattleRow(it) }
                        }
                    }
                }
            }

            StaggerReveal(index = 5) {
                Column {
                    ContentTabs(
                        active = tab,
                        videosCount = state.videosFmt,
                        onTabChange = { tab = it; viewModel.selectTab(it) }
                    )
                    when (tab) {
                        // Onglet Challenges : carte CTA + carrousel « Mes challenges ».
                        ProfileVideoTab.Challenges -> ChallengeSection(
                            challenges = myChallenges,
                            onCreate = { createChallengeOpen = true }
                        )
                        // Playlists : pas encore d'API backend → état vide honnête.
                        ProfileVideoTab.Playlists -> EmptyState(
                            title = "Aucune playlist",
                            subtitle = "Regroupe tes vidéos en playlists — bientôt disponible.",
                            modifier = Modifier.padding(top = 28.dp, bottom = 16.dp)
                        )
                        // Vidéos / Favoris : grille 3 colonnes.
                        else -> {
                            val gridVideos = videosState.current
                            when {
                                videosState.loading && gridVideos.isEmpty() ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 28.dp),
                                        contentAlignment = Alignment.Center
                                    ) { androidx.compose.material3.CircularProgressIndicator(color = UnovColors.Accent, strokeWidth = 2.dp) }
                                gridVideos.isEmpty() -> {
                                    val (t, s) = when (tab) {
                                        ProfileVideoTab.Videos -> "Aucune vidéo pour l'instant" to "Tes vidéos publiées apparaîtront ici."
                                        else -> "Aucun favori" to "Enregistre des vidéos pour les retrouver ici."
                                    }
                                    EmptyState(title = t, subtitle = s, modifier = Modifier.padding(top = 28.dp, bottom = 16.dp))
                                }
                                else -> RealVideoGrid(videos = gridVideos, onTap = { index -> detailPlaying = gridVideos to index })
                            }
                        }
                    }
                }
            }

            StaggerReveal(index = 6) {
                LogoutButton(onClick = { viewModel.logout(onLoggedOut) })
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (langPickerOpen) {
            LanguagePickerSheet(onDismiss = { langPickerOpen = false })
        }
            }
        }

        if (editOpen) {
            val p = net.profile
            EditProfileScreen(
                initialDisplayName = p?.displayName?.takeIf { it.isNotBlank() } ?: p?.username ?: "",
                initialUsername = p?.username ?: "",
                initialBio = p?.bio ?: "",
                avatarUrl = p?.avatarUrl,
                saving = net.saving,
                error = net.saveError,
                avatarUploading = avatarState.isUploading,
                avatarError = avatarState.error,
                onAvatarSelected = { contentType, uri -> viewModel.uploadAvatar(contentType, uri) },
                onSave = { dn, un, b -> viewModel.updateProfile(dn, b, un) { editOpen = false } },
                onDismiss = { viewModel.clearSaveError(); viewModel.clearAvatarError(); editOpen = false }
            )
        }

        // Tap sur l'avatar → options claires (voir / changer la photo).
        ProfilePhotoOptionsSheet(
            visible = photoSheetOpen,
            hasPhoto = net.profile?.avatarUrl?.isNotBlank() == true,
            onView = { photoViewerOpen = true },
            onChooseFromGallery = { avatarPicker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) },
            onDismiss = { photoSheetOpen = false }
        )
        // Visionneuse plein écran de la photo de profil.
        FullScreenPhotoViewer(
            visible = photoViewerOpen,
            avatarUrl = net.profile?.avatarUrl,
            fallbackName = state.displayName,
            onDismiss = { photoViewerOpen = false }
        )

        // « Paramètres et confidentialité » — overlay plein écran ouvert depuis le ⋮ du cover.
        // On ferme settings avant d'ouvrir une sous-destination (édition/portefeuille) pour
        // éviter l'empilement d'overlays et garder un retour propre.
        if (settingsOpen) {
            SettingsScreen(
                tier = state.tier,
                onClose = { settingsOpen = false },
                onEditProfile = { settingsOpen = false; editOpen = true },
                onOpenWallet = { settingsOpen = false; onOpenWallet() },
                onLogout = { viewModel.logout(onLoggedOut) }
            )
        }

        // Lecture plein écran d'une vidéo de la grille (mes vidéos / favoris).
        detailPlaying?.let { (list, index) ->
            com.unovapp.android.ui.feed.VideoDetailScreen(
                videos = list,
                startIndex = index,
                onBack = { detailPlaying = null },
                onOpenWallet = onOpenWallet
            )
        }

        // Écran « Créer un challenge ». Rendu dans un Dialog plein écran : la BottomNav de
        // MainScreen flotte au-dessus du contenu (architecture Box), elle masquerait sinon
        // le bouton « Lancer le challenge ». Le Dialog crée sa propre fenêtre, au-dessus de tout.
        if (createChallengeOpen) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { createChallengeOpen = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true
                )
            ) {
            com.unovapp.android.ui.challenge.CreateChallengeScreen(
                onBack = { createChallengeOpen = false },
                onLaunch = { form ->
                    // Pas d'endpoint backend : le challenge est ajouté localement pour la
                    // session et l'utilisateur est prévenu honnêtement.
                    myChallenges = myChallenges + com.unovapp.android.ui.challenge.ChallengeCard(
                        id = System.currentTimeMillis().toString(),
                        hashtag = "#" + form.name.trim().replace(" ", ""),
                        participantsFmt = "0",
                        coverUrl = form.coverUri,
                        isActive = true
                    )
                    createChallengeOpen = false
                    android.widget.Toast.makeText(
                        ctx,
                        "Challenge créé (local) — la publication arrivera avec l'API backend.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            )
            }
        }
    }
}

/* ---------- États chargement / erreur ---------- */

@Composable
private fun ProfileErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        ErrorRetry(message = message, onRetry = onRetry)
    }
}

@Composable
private fun ProfileLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(20.dp))
        ShimmerBox(modifier = Modifier.size(84.dp), shape = CircleShape)
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(22.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(90.dp), shape = RoundedCornerShape(16.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(16.dp))
    }
}

/* ---------- Intégration données réelles (/users/me) ---------- */

/** Fusionne le profil réel dans l'état mock (ne touche qu'aux champs d'identité connus). */
private fun mergeRealProfile(
    base: ProfileUiState,
    p: com.unovapp.android.data.user.UserProfileDto?,
    followingDelta: Int = 0,
    likesReceivedDelta: Int = 0
): ProfileUiState {
    if (p == null) return base
    return base.copy(
        displayName = p.displayName?.takeIf { it.isNotBlank() } ?: p.username,
        username = p.username,
        isVerified = p.isVerified,
        tier = if (p.subscriptionTier.equals("free", ignoreCase = true)) "Tier Free"
        else "Tier " + p.subscriptionTier.replaceFirstChar { it.uppercase() },
        bio = p.bio?.takeIf { it.isNotBlank() } ?: base.bio,
        website = p.websiteUrl ?: "",
        avatarUrl = p.avatarUrl,
        coverUrl = p.coverUrl,
        followersFmt = formatCompact(p.followersCount),
        // Stats réelles (Sprint 3) : vraies vidéos publiées + total de likes reçus (+ delta local
        // si je viens de liker une de mes vidéos → mise à jour immédiate).
        videosFmt = formatCompact(p.videosCount),
        likesFmt = formatCompact((p.totalLikesReceived + likesReceivedDelta).coerceAtLeast(0)),
        followersCount = p.followersCount,
        likesCount = (p.totalLikesReceived + likesReceivedDelta).coerceAtLeast(0),
        videosCount = p.videosCount,
        // Abonnements = following du backend + delta local (suivis/désuivis dans la session).
        followingCount = (p.followingCount + followingDelta).coerceAtLeast(0),
        followingFmt = formatCompact((p.followingCount + followingDelta).coerceAtLeast(0)),
        revenueFcfaFmt = formatThousands(p.walletBalance)
    )
}

private fun formatCompact(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1f M", n / 1_000_000.0).replace(".0 ", " ")
    n >= 1_000 -> String.format("%.1f K", n / 1_000.0).replace(".0 ", " ")
    else -> n.toString()
}

private fun formatThousands(amount: Double): String =
    amount.toLong().toString().reversed().chunked(3).joinToString(" ").reversed()

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, UnovColors.Line, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Se déconnecter",
                color = UnovColors.Danger,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/* ---------- Cover ---------- */

@Composable
private fun CoverHeader(
    state: ProfileUiState,
    scrollPx: Int = 0,
    onOpenLangPicker: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    coverUploading: Boolean = false,
    onChangeCover: () -> Unit = {}
) {
    // Révélation de la photo de couverture : quand l'URL arrive (ou change après un upload),
    // l'image se dévoile en fondu + léger dézoom — elle « se pose » au lieu d'apparaître sec.
    val reveal = remember(state.coverUrl) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(state.coverUrl) {
        if (!state.coverUrl.isNullOrBlank()) reveal.animateTo(1f, tween(700, easing = UnovMotion.Standard))
    }
    // Salve d'étincelles à la confirmation d'une nouvelle couverture.
    var coverBurst by remember { mutableIntStateOf(0) }
    var firstCover by remember { mutableStateOf(true) }
    LaunchedEffect(state.coverUrl) {
        if (firstCover) { firstCover = false; return@LaunchedEffect }
        if (!state.coverUrl.isNullOrBlank()) coverBurst++
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .graphicsLayer {
                // Parallax : le cover suit le scroll à demi-vitesse + léger zoom + fondu élégant.
                translationY = scrollPx * 0.5f
                val z = 1f + (scrollPx / 1600f).coerceIn(0f, 0.25f)
                scaleX = z
                scaleY = z
                alpha = (1f - scrollPx / 520f).coerceIn(0.25f, 1f)
            }
    ) {
        // Base gradient cinema — visible tant qu'aucune couverture n'est définie, et derrière
        // l'image pendant son chargement.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFF1A0E00),
                            0.35f to Color(0xFF3A1F08),
                            1f to UnovColors.AccentDeep
                        )
                    )
                )
        )

        // Photo de couverture réelle (R2) — révélation en fondu + dézoom (effet « Ken Burns »
        // inversé : l'image se pose), puis parallax de zoom au scroll par-dessus.
        if (!state.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = state.coverUrl,
                contentDescription = "Photo de couverture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = reveal.value
                        val s = 1.12f - 0.12f * reveal.value
                        scaleX = s
                        scaleY = s
                    }
            )
            // Voile sombre : garantit la lisibilité des contrôles et du bloc identité par-dessus,
            // quelle que soit la photo choisie (claire, chargée, contrastée…).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.45f),
                            0.45f to Color.Black.copy(alpha = 0.15f),
                            1f to Color.Black.copy(alpha = 0.60f)
                        )
                    )
            )
        }
        // Décor animé — UNIQUEMENT quand il n'y a pas de photo : sinon il salirait la couverture
        // du créateur. Sans photo, le header reste vivant (filigrane + aurora qui dérive).
        val hasCover = !state.coverUrl.isNullOrBlank()
        if (!hasCover) {
        Filigree(modifier = Modifier.fillMaxSize())
        // Aurora vivante : deux nappes lumineuses qui dérivent lentement en boucle ambient —
        // le cover « respire » au lieu d'être une image figée. Canvas pur, coût négligeable.
        val auroraT by rememberInfiniteTransition(label = "aurora").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(11_000, easing = LinearEasing),
                RepeatMode.Reverse
            ),
            label = "auroraT"
        )
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Nappe principale — glisse de gauche à droite.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(UnovColors.Accent.copy(alpha = 0.34f), Color.Transparent),
                    center = Offset(w * (0.18f + 0.5f * auroraT), h * 0.30f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * (0.18f + 0.5f * auroraT), h * 0.30f)
            )
            // Contre-nappe plus claire — dérive en sens inverse, croise la première.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(UnovColors.AccentLight.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(w * (0.85f - 0.55f * auroraT), h * (0.75f - 0.25f * auroraT)),
                    radius = w * 0.42f
                ),
                radius = w * 0.42f,
                center = Offset(w * (0.85f - 0.55f * auroraT), h * (0.75f - 0.25f * auroraT))
            )
        }
        }
        // Bottom gold rule (light divider)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to UnovColors.Accent.copy(alpha = 0.6f),
                        1f to Color.Transparent
                    )
                )
        )

        // ── Bouton « changer la couverture » (bas-droite) + états d'upload ──────────
        // Pastille sombre discrète : elle n'écrase pas la photo mais reste trouvable.
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                .unovTap(onClick = onChangeCover, pressedScale = 0.92f)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (coverUploading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = UnovColors.Accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(13.dp)
                )
                Text("Envoi…", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Changer la photo de couverture",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (hasCover) "Modifier" else "Ajouter une couverture",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Balayage lumineux pendant l'envoi : une bande claire traverse le header en boucle —
        // l'utilisateur voit que « ça travaille » sans bloquer l'écran.
        if (coverUploading) {
            val sweep by rememberInfiniteTransition(label = "coverSweep").animateFloat(
                initialValue = -0.4f,
                targetValue = 1.4f,
                animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
                label = "coverSweepV"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val bandWidth = size.width * 0.35f
                val x = size.width * sweep
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        startX = x - bandWidth / 2f,
                        endX = x + bandWidth / 2f
                    ),
                    topLeft = Offset(x - bandWidth / 2f, 0f),
                    size = androidx.compose.ui.geometry.Size(bandWidth, size.height)
                )
            }
        }

        // Explosion d'étincelles à la confirmation d'une nouvelle couverture (récompense).
        ParticleBurst(
            trigger = coverBurst,
            particleCount = 34,
            maxRadius = 150.dp,
            durationMs = 900,
            modifier = Modifier.fillMaxSize()
        )

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.42f))
                    .border(1.dp, UnovColors.Accent.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                    .padding(start = 8.dp, top = 6.dp, end = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(UnovGradients.Gold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFF0D0D0D),
                        modifier = Modifier.size(10.dp)
                    )
                }
                Text(
                    text = "@${state.username}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = UnovColors.TextMute,
                    modifier = Modifier.size(11.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageChip(onClick = onOpenLangPicker)
                CoverIconButton(Icons.Outlined.QrCode, "QR")
                CoverIconButton(
                    Icons.Filled.MoreVert,
                    "Paramètres et confidentialité",
                    onClick = onOpenSettings
                )
            }
        }

        // (Badge LIVE + spectateurs retirés : pas de backend live, et chevauchait l'avatar.)
    }
}

@Composable
private fun CoverIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.18f), CircleShape)
            .unovTap(onClick = onClick, pressedScale = 0.88f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

/* ---------- Identity ---------- */

@Composable
private fun IdentityBlock(
    state: ProfileUiState,
    subscribed: Boolean,
    onToggleSubscribe: () -> Unit,
    onOpenBattle: () -> Unit,
    onOpenWallet: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenFollowers: () -> Unit = {},
    onOpenFollowing: () -> Unit = {},
    avatarUploading: Boolean = false,
    onAvatarClick: () -> Unit = {}
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // ── Avatar à gauche + identité à droite (disposition maquette) ──────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-38).dp)
                .enterFadeSlide(appeared, delayMillis = 40),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileAvatar(
                name = state.displayName,
                avatarUrl = state.avatarUrl,
                uploading = avatarUploading,
                onClick = onAvatarClick
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "@${state.username}",
                        color = UnovColors.Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        maxLines = 1
                    )
                    if (state.isVerified) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Compte vérifié",
                            tint = UnovColors.Accent,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Text(
                    text = state.displayName,
                    color = UnovColors.TextMute,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // ── Stats : Abonnements · Abonnés · J'aime ─────────────────────────────
        Column(modifier = Modifier.offset(y = (-24).dp).enterFadeSlide(appeared, delayMillis = 130)) {
            StatsTrio(
                state = state,
                onOpenFollowers = onOpenFollowers,
                onOpenFollowing = onOpenFollowing
            )

            if (state.bio.isNotBlank()) {
                Text(
                    text = state.bio,
                    color = UnovColors.TextDim,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }

            // Site web cliquable (ouvre le navigateur) — affiché seulement si renseigné.
            if (state.website.isNotBlank()) {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            runCatching {
                                ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(state.website)))
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinkPill(
                        icon = Icons.Outlined.Language,
                        label = state.website.removePrefix("https://").removePrefix("http://"),
                        accent = true
                    )
                }
            }

            // Action bar (maquette) : « Modifier le profil » large + bouton icône compact.
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(12.dp))
                        .clickable(onClick = onEditProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Modifier le profil",
                        color = UnovColors.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenWallet),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAddAlt,
                        contentDescription = "Trouver des amis",
                        tint = UnovColors.Text,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    name: String,
    avatarUrl: String?,
    uploading: Boolean = false,
    onClick: () -> Unit = {}
) {
    // Anneau conique doré qui tourne lentement (signature premium).
    val ringRotation by rememberInfiniteTransition(label = "avatarRing").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringRot"
    )
    // Halo respirant derrière l'anneau — pulse lent, très diffus (présence, pas clignotement).
    val haloPulse by rememberInfiniteTransition(label = "avatarHalo").animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            tween(UnovMotion.DurationAmbient * 2, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "haloPulse"
    )
    // Entrée héroïque : l'avatar surgit avec sur-rebond, puis une salve d'étincelles éclate.
    val entrance = remember { androidx.compose.animation.core.Animatable(0.55f) }
    var welcomeBurst by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, UnovMotion.wobbly())
        welcomeBurst++
    }
    // Conteneur NON rogné (104dp) : le badge en bas-droite déborde légèrement de l'anneau et
    // reste donc entièrement visible (avant, le clip circulaire du parent le coupait).
    Box(
        modifier = Modifier
            .size(104.dp)
            .graphicsLayer {
                scaleX = entrance.value
                scaleY = entrance.value
                alpha = ((entrance.value - 0.55f) / 0.45f).coerceIn(0f, 1f)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Halo diffus animé (sous l'anneau).
        Box(
            modifier = Modifier
                .size(104.dp)
                .graphicsLayer {
                    scaleX = haloPulse
                    scaleY = haloPulse
                    alpha = 0.9f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(UnovColors.AccentGlow, Color.Transparent)
                    ),
                    CircleShape
                )
        )
        // Anneau conique doré animé (signature premium), légèrement plus fin et élégant.
        Box(
            modifier = Modifier
                .size(96.dp)
                .rotate(ringRotation)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            UnovColors.Accent,
                            Color(0xFFFF944D),
                            UnovColors.AccentDeep,
                            UnovColors.Accent
                        )
                    )
                )
        )
        // Liseré sombre (gap) + photo réelle (Coil) ou initiales.
        Box(
            modifier = Modifier
                .size(89.dp)
                .clip(CircleShape)
                .background(Color(0xFF0D0D0D)),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(83.dp).clip(CircleShape)
                )
            } else {
                Avatar(idx = 0, name = name, size = 83.dp)
            }
        }
        // Overlay pendant l'upload de la nouvelle photo.
        if (uploading) {
            Box(
                modifier = Modifier
                    .size(89.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = UnovColors.Accent,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        // Badge appareil photo (bas-droite) — affordance moderne « changer la photo ».
        // Halo sombre = découpe nette dans l'avatar, pastille dorée + icône = repère clair.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF0D0D0D)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(27.dp)
                    .clip(CircleShape)
                    .background(UnovGradients.Gold),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Changer la photo de profil",
                    tint = Color(0xFF0D0D0D),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        // Salve d'étincelles à l'arrivée de l'avatar (fin de l'entrée héroïque).
        ParticleBurst(
            trigger = welcomeBurst,
            maxRadius = 76.dp,
            particleCount = 24,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun StatsTrio(
    state: ProfileUiState,
    onOpenFollowers: () -> Unit = {},
    onOpenFollowing: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ordre de la maquette : Abonnements · Abonnés · J'aime
        StatTile(
            value = state.followingFmt,
            rawValue = state.followingCount,
            label = "Abonnements",
            trend = null,
            sparkValues = null,
            popIndex = 0,
            onClick = onOpenFollowing,
            modifier = Modifier.weight(1f)
        )
        StatDivider()
        StatTile(
            value = state.followersFmt,
            rawValue = state.followersCount,
            label = "Abonnés",
            trend = null,
            sparkValues = null,
            popIndex = 1,
            onClick = onOpenFollowers,
            modifier = Modifier.weight(1f)
        )
        StatDivider()
        StatTile(
            value = state.likesFmt,
            rawValue = state.likesCount,
            label = "J'aime",
            trend = null,
            sparkValues = null,
            popIndex = 2,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(UnovColors.Line)
    )
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    trend: String?,
    sparkValues: List<Float>?,
    modifier: Modifier = Modifier,
    /** Valeur brute — active le compteur animé (count-up) quand > 0. */
    rawValue: Int = 0,
    /** Position dans le trio — décale le pop d'entrée en cascade. */
    popIndex: Int = 0,
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    // Micro-burst au tap (stat cliquable) — feedback festif, discret.
    var tapBurst by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val clickMod = if (onClick != null) {
        Modifier.clickable(interactionSource = interaction, indication = null) {
            tapBurst++
            onClick()
        }
    } else Modifier
    // Pop d'entrée en cascade : chaque tuile surgit avec un léger rebond, décalée de 70 ms.
    val pop = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120L + popIndex * 70L)
        pop.animateTo(1f, UnovMotion.bouncy())
    }
    Column(
        modifier = modifier
            .then(clickMod)
            .graphicsLayer {
                val s = 0.8f + 0.2f * pop.value
                scaleX = s
                scaleY = s
                alpha = pop.value.coerceIn(0f, 1f)
            }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            // Compteur animé : défile de l'ancienne valeur vers la nouvelle (count-up).
            val animated = if (rawValue > 0) formatCompact(rememberCountUp(rawValue)) else value
            Text(
                text = animated,
                color = UnovColors.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                lineHeight = 22.sp
            )
            ParticleBurst(
                trigger = tapBurst,
                particleCount = 14,
                maxRadius = 40.dp,
                durationMs = 550,
                modifier = Modifier.matchParentSize()
            )
        }
        // Libellé sous la valeur, centré (maquette).
        Text(
            text = label,
            color = UnovColors.TextMute,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (trend != null) {
            Text(
                text = "↗ $trend",
                color = UnovColors.Accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        if (sparkValues != null) {
            Sparkline(values = sparkValues, width = 44.dp, height = 16.dp)
        }
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(UnovColors.TextMute)
    )
}

@Composable
private fun LinkPill(icon: ImageVector, label: String, accent: Boolean = false) {
    val noRipple = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (accent) UnovColors.Accent.copy(alpha = 0.06f) else Color.Transparent
            )
            .border(
                1.dp,
                if (accent) UnovColors.Accent.copy(alpha = 0.22f) else UnovColors.Line,
                RoundedCornerShape(999.dp)
            )
            .clickable(interactionSource = noRipple, indication = null) {}
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (accent) UnovColors.Accent else UnovColors.TextDim,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            color = if (accent) UnovColors.Accent else UnovColors.TextDim,
            fontSize = 11.sp,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun SubscribeButton(
    subscribed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Passage à « Abonné » : pop physique du bouton + explosion d'étincelles or.
    var celebrateBurst by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val popScale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(subscribed) {
        if (subscribed) {
            celebrateBurst++
            popScale.snapTo(0.86f)
            popScale.animateTo(1f, UnovMotion.wobbly())
        }
    }
    Box(
        modifier = modifier
            .height(46.dp)
            .graphicsLayer { scaleX = popScale.value; scaleY = popScale.value }
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (subscribed) {
                    Modifier
                        .background(UnovColors.SurfaceAlt)
                        .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(14.dp))
                } else Modifier.background(UnovGradients.Gold)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        ParticleBurst(
            trigger = celebrateBurst,
            particleCount = 30,
            maxRadius = 90.dp,
            modifier = Modifier.matchParentSize()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (subscribed) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = UnovColors.Text,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Abonné",
                    color = UnovColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "S'abonner",
                    color = Color(0xFF0D0D0D),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GhostActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = UnovColors.Text,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = UnovColors.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/* ---------- Section headers + scroll rows ---------- */

@Composable
private fun SectionHeader(eyebrow: String, action: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = eyebrow.uppercase(),
            color = UnovColors.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.8.sp
        )
        if (action != null) {
            Text(
                text = "${action.uppercase()} →",
                color = UnovColors.TextMute,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                modifier = Modifier.clickable {}
            )
        }
    }
}

/**
 * Liste horizontale scrollable avec padding latéraux + spacing constant entre items.
 * Pas de LazyRow ici : on a au plus 9 items à afficher, Row + horizontalScroll suffit
 * et évite la friction des LazyItemScope.
 */
@Composable
private fun HorizontalScrollRow(
    start: Dp = 16.dp,
    end: Dp = 16.dp,
    itemSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = start, end = end),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        content()
    }
}

/* ---------- Achievement pill ---------- */

@Composable
private fun AchievementPill(a: Achievement) {
    val bgBrush = if (a.accent) {
        Brush.linearGradient(
            colors = listOf(
                UnovColors.Accent.copy(alpha = 0.10f),
                UnovColors.AccentDeep.copy(alpha = 0.04f)
            )
        )
    } else null
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .then(if (bgBrush != null) Modifier.background(bgBrush) else Modifier.background(UnovColors.Surface))
            .border(
                1.dp,
                if (a.accent) UnovColors.Accent.copy(alpha = 0.25f) else UnovColors.Line,
                RoundedCornerShape(999.dp)
            )
            .padding(start = 8.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (a.accent) UnovGradients.Gold
                    else Brush.linearGradient(listOf(UnovColors.SurfaceAlt, UnovColors.SurfaceAlt))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = a.icon,
                contentDescription = null,
                tint = if (a.accent) Color(0xFF0D0D0D) else UnovColors.TextMute,
                modifier = Modifier.size(14.dp)
            )
        }
        Column {
            Text(
                text = a.label,
                color = if (a.accent) UnovColors.Accent else UnovColors.Text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (a.sub != null) {
                Text(
                    text = a.sub,
                    color = UnovColors.TextMute,
                    fontSize = 9.sp,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

/* ---------- Highlight circle ---------- */

@Composable
private fun HighlightCircle(h: Highlight) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (h.live) Brush.sweepGradient(
                        listOf(
                            UnovColors.Accent,
                            Color(0xFFFF944D),
                            UnovColors.AccentDeep,
                            UnovColors.Accent
                        )
                    ) else Brush.linearGradient(
                        listOf(
                            UnovColors.Accent.copy(alpha = 0.4f),
                            UnovColors.AccentDeep.copy(alpha = 0.2f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner black gap
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                // Video preview
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(UnovGradients.videoBg(h.gradientIndex))
                )
            }
            // Count badge bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF0A0A0A))
                    .border(1.5.dp, UnovColors.Accent, RoundedCornerShape(999.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = h.count,
                    color = UnovColors.Accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = h.label,
            color = UnovColors.Text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(70.dp)
        )
    }
}

/* ---------- Top fan chip ---------- */

@Composable
private fun TopFanChip(fan: TopFan) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(UnovColors.Surface)
            .border(
                1.dp,
                if (fan.rank == 1) UnovColors.Accent.copy(alpha = 0.35f) else UnovColors.Line,
                RoundedCornerShape(999.dp)
            )
            .padding(start = 4.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            Avatar(idx = fan.avatarIdx, name = fan.username, size = 28.dp)
            if (fan.rank == 1) {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = "Rang 1",
                    tint = UnovColors.Accent,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-6).dp)
                        .size(12.dp)
                )
            }
        }
        Column {
            Text(
                text = "@${fan.username}",
                color = UnovColors.Text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MonetizationOn,
                    contentDescription = null,
                    tint = UnovColors.TextMute,
                    modifier = Modifier.size(9.dp)
                )
                Text(
                    text = "${fan.gifts} jetons",
                    color = UnovColors.TextMute,
                    fontSize = 9.sp
                )
            }
        }
    }
}

/* ---------- Dashboard card ---------- */

@Composable
private fun DashboardCard(state: ProfileUiState) {
    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        UnovColors.Accent.copy(alpha = 0.06f),
                        UnovColors.AccentDeep.copy(alpha = 0.02f)
                    )
                )
            )
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
    ) {
        // Corner radial decoration
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            UnovColors.Accent.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "MES REVENUS · SEPTEMBRE 2026",
                        color = UnovColors.TextMute,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.6.sp
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = state.revenueFcfaFmt,
                            color = UnovColors.Accent,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.6).sp
                        )
                        Text(
                            text = " FCFA",
                            color = UnovColors.TextDim,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "↗ ${state.revenueTrend}",
                            color = UnovColors.Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "vs mois dernier",
                            color = UnovColors.TextMute,
                            fontSize = 11.sp
                        )
                    }
                }
                Sparkline(
                    values = state.revenueSpark,
                    width = 88.dp,
                    height = 32.dp
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(UnovColors.Accent.copy(alpha = 0.10f))
                    .border(1.dp, UnovColors.Accent, RoundedCornerShape(12.dp))
                    .clickable {}
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OUVRIR LE STUDIO CRÉATEUR →",
                    color = UnovColors.Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}

/* ---------- Battle row ---------- */

@Composable
private fun BattleRow(b: BattleEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Avatar(idx = b.avatarIdx, name = b.opponent, size = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "vs @${b.opponent}",
                color = UnovColors.Text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${b.votes} · ${b.date}",
                color = UnovColors.TextMute,
                fontSize = 10.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (b.won) UnovColors.Accent.copy(alpha = 0.12f)
                    else UnovColors.SurfaceAlt
                )
                .border(
                    1.dp,
                    if (b.won) UnovColors.Accent.copy(alpha = 0.3f) else UnovColors.Line,
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (b.won) "GAGNÉ" else "PERDU",
                color = if (b.won) UnovColors.Accent else UnovColors.TextMute,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

/* ---------- Tabs + filters + grid ---------- */

/** Métadonnées d'affichage d'un onglet (l'enum vit dans le ViewModel). */
private fun profileTabLabel(tab: ProfileVideoTab): String = when (tab) {
    ProfileVideoTab.Videos -> "Vidéos"
    ProfileVideoTab.Favoris -> "Favoris"
    ProfileVideoTab.Playlists -> "Playlists"
    ProfileVideoTab.Challenges -> "Challenges"
}

private fun profileTabIcon(tab: ProfileVideoTab): ImageVector = when (tab) {
    ProfileVideoTab.Videos -> Icons.Outlined.PlayCircleOutline
    ProfileVideoTab.Favoris -> Icons.Outlined.BookmarkBorder
    ProfileVideoTab.Playlists -> Icons.Outlined.PlaylistPlay
    ProfileVideoTab.Challenges -> Icons.Outlined.EmojiEvents
}

/**
 * Onglets du profil (maquette) : 4 pastilles — l'onglet actif est **encadré en doré** avec un
 * fond ambré léger, les autres restent sobres. Transition de couleur/bordure animée, icône qui
 * pop au ressort : la sélection se sent, sans barre indicatrice.
 */
@Composable
private fun ContentTabs(active: ProfileVideoTab, videosCount: String, onTabChange: (ProfileVideoTab) -> Unit) {
    val tabs = ProfileVideoTab.values()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val isActive = tab == active
            val iconScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isActive) 1.15f else 1f,
                animationSpec = UnovMotion.bouncy(),
                label = "tabIcon"
            )
            val border by androidx.compose.animation.animateColorAsState(
                targetValue = if (isActive) UnovColors.Accent.copy(alpha = 0.75f) else Color.Transparent,
                animationSpec = tween(220),
                label = "tabBorder"
            )
            val bg by androidx.compose.animation.animateColorAsState(
                targetValue = if (isActive) UnovColors.Accent.copy(alpha = 0.10f) else Color.Transparent,
                animationSpec = tween(220),
                label = "tabBg"
            )
            val content = if (isActive) UnovColors.Accent else UnovColors.TextMute

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabChange(tab) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = profileTabIcon(tab),
                    contentDescription = profileTabLabel(tab),
                    tint = content,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                )
                Text(
                    text = profileTabLabel(tab),
                    color = content,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Grille 3 colonnes des vraies vidéos (miniatures backend). Tap → lecture plein écran.
 * Chaque vignette ENTRE EN CASCADE (scale + fade décalés) et répond au doigt (press scale) ;
 * scrim dégradé en bas pour la lisibilité du compteur de vues + badge durée en haut.
 */
@Composable
private fun RealVideoGrid(videos: List<com.unovapp.android.ui.feed.FeedVideoUi>, onTap: (Int) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        videos.chunked(3).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { colIndex, v ->
                    val index = rowIndex * 3 + colIndex
                    // Entrée en cascade : décalage par vignette, plafonné (au-delà : immédiat).
                    val reveal = remember(v.id) { androidx.compose.animation.core.Animatable(0f) }
                    LaunchedEffect(v.id) {
                        kotlinx.coroutines.delay(
                            (index.coerceAtMost(UnovMotion.StaggerMaxItems) * UnovMotion.StaggerDelayMs).toLong()
                        )
                        reveal.animateTo(1f, UnovMotion.smooth())
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(9f / 14f)
                            .graphicsLayer {
                                val s = 0.88f + 0.12f * reveal.value
                                scaleX = s
                                scaleY = s
                                alpha = reveal.value.coerceIn(0f, 1f)
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(UnovGradients.videoBg(kotlin.math.abs(v.id.hashCode()) % 6))
                            .unovTap(onClick = { onTap(index) }, pressedScale = 0.94f)
                    ) {
                        if (!v.thumbnailUrl.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = v.thumbnailUrl,
                                contentDescription = v.description.take(40),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Scrim bas : dégradé noir → transparent, lisibilité du compteur garantie.
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.55f)
                                    )
                                )
                        )
                        // Badge durée (haut-droite) — repère de longueur avant d'ouvrir.
                        if (v.durationSec > 0) {
                            Text(
                                text = "%d:%02d".format(v.durationSec / 60, v.durationSec % 60),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        // Compteur de VUES en bas (façon TikTok : nombre de lectures générées).
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Vues",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(text = v.viewsFmt, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private val FILTERS = listOf("Récents", "Populaires", "Épinglés", "Mensuel", "Lives")

@Composable
private fun FilterPills(selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(FILTERS.size) { i ->
            val f = FILTERS[i]
            val isActive = f == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isActive) UnovColors.Accent.copy(alpha = 0.10f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isActive) UnovColors.Accent.copy(alpha = 0.35f) else UnovColors.Line,
                        RoundedCornerShape(999.dp)
                    )
                    .clickable { onSelect(f) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = f,
                    color = if (isActive) UnovColors.Accent else UnovColors.TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun VideoGrid(videos: List<VideoTile>) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        // Featured 2×2 + right column 2 stacked
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Featured big tile (2x2)
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(UnovGradients.videoBg(4))
            ) {
                // Pinned badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(UnovColors.Accent.copy(alpha = 0.95f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = Color(0xFF0D0D0D),
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        text = "ÉPINGLÉE",
                        color = Color(0xFF0D0D0D),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp
                    )
                }
                // Bottom title + views + duration
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Quand maman entend…",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "1,2 M",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "0:28",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            // Right column 2 stacked
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "412 K" to 2,
                    "89 K" to 4
                ).forEach { (views, grad) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(UnovGradients.videoBg(grad))
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = views,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3-col remaining
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            videos.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEachIndexed { idx, v ->
                        VideoGridTile(
                            video = v,
                            isTopOne = idx == 0 && row == videos.chunked(3).first(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Pad incomplete row
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoGridTile(
    video: VideoTile,
    isTopOne: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(9f / 14f)
            .clip(RoundedCornerShape(6.dp))
            .background(UnovGradients.videoBg(video.gradientIndex))
    ) {
        if (isTopOne) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(UnovColors.Accent.copy(alpha = 0.95f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "TOP 1%",
                    color = Color(0xFF0D0D0D),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Visibility,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(9.dp)
            )
            Text(
                text = video.views,
                color = Color.White,
                fontSize = 9.sp
            )
        }
        val minutes = video.durationSec / 60
        val seconds = video.durationSec % 60
        Text(
            text = "$minutes:${seconds.toString().padStart(2, '0')}",
            color = Color.White,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
        )
    }
}
