package com.unovapp.android.ui.profile

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
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
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.components.EmptyState
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
    val followersFmt: String = "0",
    val likesFmt: String = "0",
    val videosFmt: String = "0",
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

        // Session expirée/invalide (401) → on renvoie vers la connexion.
        androidx.compose.runtime.LaunchedEffect(net.sessionExpired) {
            if (net.sessionExpired) onLoggedOut()
        }

        // Données réelles (/users/me) fusionnées dans l'état d'affichage.
        // Les sections riches (battles, top fans, grille…) restent mockées tant que
        // le backend ne les expose pas.
        val state = mergeRealProfile(baseState, net.profile, followingDelta)
        val myId = net.profile?.id

        var subscribed by remember { mutableStateOf(false) }
        var tab by remember { mutableStateOf(ProfileTab.Videos) }
        var filter by remember { mutableStateOf("Récents") }
        var langPickerOpen by remember { mutableStateOf(false) }
        var editOpen by remember { mutableStateOf(false) }
        var settingsOpen by remember { mutableStateOf(false) }
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
                .background(Color(0xFF050505))
                .verticalScroll(scrollState)
                .padding(contentPadding)
        ) {
            CoverHeader(
                state = state,
                scrollPx = scrollState.value,
                onOpenLangPicker = { langPickerOpen = true },
                onOpenSettings = { settingsOpen = true }
            )
            IdentityBlock(
                state = state,
                subscribed = subscribed,
                onToggleSubscribe = { subscribed = !subscribed },
                onOpenBattle = onOpenBattle,
                onOpenWallet = onOpenWallet,
                onEditProfile = { editOpen = true },
                onOpenFollowers = { myId?.let { onOpenConnections(it, state.username, true) } },
                onOpenFollowing = { myId?.let { onOpenConnections(it, state.username, false) } }
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
                    ContentTabs(active = tab, onTabChange = { tab = it })
                    FilterPills(selected = filter, onSelect = { filter = it })
                    if (state.gridVideos.isEmpty()) {
                        EmptyState(
                            title = "Aucune vidéo pour l'instant",
                            subtitle = "Tes vidéos publiées apparaîtront ici.",
                            modifier = Modifier.padding(top = 28.dp, bottom = 16.dp)
                        )
                    } else {
                        VideoGrid(videos = state.gridVideos)
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
                onSave = { dn, un, b -> viewModel.updateProfile(dn, b, un) { editOpen = false } },
                onDismiss = { viewModel.clearSaveError(); editOpen = false }
            )
        }

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
    }
}

/* ---------- États chargement / erreur ---------- */

@Composable
private fun ProfileErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505)),
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
            .background(Color(0xFF050505))
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
    followingDelta: Int = 0
): ProfileUiState {
    if (p == null) return base
    return base.copy(
        displayName = p.displayName?.takeIf { it.isNotBlank() } ?: p.username,
        username = p.username,
        isVerified = p.isVerified,
        tier = if (p.subscriptionTier.equals("free", ignoreCase = true)) "Tier Free"
        else "Tier " + p.subscriptionTier.replaceFirstChar { it.uppercase() },
        bio = p.bio?.takeIf { it.isNotBlank() } ?: base.bio,
        avatarUrl = p.avatarUrl,
        followersFmt = formatCompact(p.followersCount),
        // pas de "vidéos" backend → on affiche les abonnements réels, ajustés du delta optimiste.
        videosFmt = formatCompact((p.followingCount + followingDelta).coerceAtLeast(0)),
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
    onOpenSettings: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .graphicsLayer {
                // Parallax : le cover suit le scroll à demi-vitesse + léger zoom + fondu élégant.
                translationY = scrollPx * 0.5f
                val z = 1f + (scrollPx / 1600f).coerceIn(0f, 0.25f)
                scaleX = z
                scaleY = z
                alpha = (1f - scrollPx / 520f).coerceIn(0.25f, 1f)
            }
    ) {
        // Base gradient cinema
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFF1A1408),
                            0.35f to Color(0xFF3A2D10),
                            1f to UnovColors.AccentDeep
                        )
                    )
                )
        )
        // Decorative filigree
        Filigree(modifier = Modifier.fillMaxSize())
        // Radial highlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            UnovColors.Accent.copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        radius = 600f
                    )
                )
        )
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
    onOpenFollowing: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Avatar (overlaps cover) + stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-38).dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileAvatar(name = state.displayName, avatarUrl = state.avatarUrl)
            StatsTrio(state = state, onOpenFollowers = onOpenFollowers, onOpenFollowing = onOpenFollowing)
        }

        // Name + verified + tier
        Column(modifier = Modifier.offset(y = (-24).dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = state.displayName,
                    color = UnovColors.Text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
                if (state.isVerified) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Compte vérifié",
                        tint = UnovColors.Accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "@${state.username}",
                    color = UnovColors.TextMute,
                    fontSize = 12.sp
                )
                if (state.city.isNotBlank()) {
                    Dot()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = UnovColors.TextMute,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = state.city,
                            color = UnovColors.TextMute,
                            fontSize = 12.sp
                        )
                    }
                }
                Dot()
                Text(
                    text = state.tier,
                    color = UnovColors.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.bio.isNotBlank()) {
                Text(
                    text = state.bio,
                    color = UnovColors.TextDim,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // Link pills — affichés seulement si le site est renseigné (le reste viendra du backend).
            if (state.website.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinkPill(
                        icon = Icons.Outlined.Language,
                        label = state.website,
                        accent = true
                    )
                    LinkPill(icon = Icons.Outlined.Mail, label = "contact")
                    LinkPill(icon = Icons.Outlined.Phone, label = "WhatsApp")
                }
            }

            // Action bar — profil personnel : Modifier le profil + Partager.
            Row(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(UnovGradients.Gold)
                        .clickable(onClick = onEditProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Modifier le profil",
                        color = Color(0xFF0D0D0D),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenWallet),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Partager",
                        color = UnovColors.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, avatarUrl: String?) {
    // Anneau conique doré qui tourne lentement (signature premium).
    val ringRotation by rememberInfiniteTransition(label = "avatarRing").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringRot"
    )
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        // Conic ring background (animé)
        Box(
            modifier = Modifier
                .size(96.dp)
                .rotate(ringRotation)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            UnovColors.Accent,
                            Color(0xFFF4C430),
                            UnovColors.AccentDeep,
                            UnovColors.Accent
                        )
                    )
                )
        )
        // Inner black ring + photo réelle (Coil) ou initiales
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0xFF050505)),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(84.dp).clip(CircleShape)
                )
            } else {
                Avatar(idx = 0, name = name, size = 84.dp)
            }
        }
        // Crown badge bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF050505)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(UnovGradients.Gold),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFF0D0D0D),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
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
        StatTile(
            value = state.followersFmt,
            label = "Abonnés",
            trend = null,
            sparkValues = null,
            onClick = onOpenFollowers,
            modifier = Modifier.weight(1f)
        )
        StatDivider()
        StatTile(
            value = state.likesFmt,
            label = "J'aime",
            trend = null,
            sparkValues = null,
            modifier = Modifier.weight(1f)
        )
        StatDivider()
        StatTile(
            value = state.videosFmt,
            label = "Suivis",
            trend = null,
            sparkValues = null,
            onClick = onOpenFollowing,
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
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val clickMod = if (onClick != null) {
        Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else Modifier
    Column(modifier = modifier.then(clickMod).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(
            text = value,
            color = UnovColors.Text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            lineHeight = 22.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label.uppercase(),
                    color = UnovColors.TextMute,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp,
                    maxLines = 1
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
            }
            if (sparkValues != null) {
                Sparkline(values = sparkValues, width = 44.dp, height = 16.dp)
            }
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
    Box(
        modifier = modifier
            .height(46.dp)
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
                            Color(0xFFF4C430),
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

private enum class ProfileTab(val label: String, val count: String, val icon: ImageVector) {
    Videos("Vidéos", "128", Icons.Outlined.GridOn),
    Lives("Lives", "12", Icons.Outlined.LiveTv),
    Liked("Aimées", "—", Icons.Outlined.Favorite),
    Tags("Tags", "24", Icons.Outlined.Tag),
    Private("Privées", "—", Icons.Outlined.Lock)
}

@Composable
private fun ContentTabs(active: ProfileTab, onTabChange: (ProfileTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .border(width = 1.dp, color = UnovColors.Line, shape = RoundedCornerShape(0.dp))
            .padding(start = 16.dp, end = 16.dp, top = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ProfileTab.values().forEach { tab ->
            val isActive = tab == active
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabChange(tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (isActive) UnovColors.Accent else UnovColors.TextMute,
                    modifier = Modifier.size(16.dp)
                )
                Row {
                    Text(
                        text = tab.label,
                        color = if (isActive) UnovColors.Accent else UnovColors.TextMute,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                    )
                    Text(
                        text = " ${tab.count}",
                        color = UnovColors.TextMute,
                        fontSize = 10.sp
                    )
                }
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(width = 22.dp, height = 2.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(UnovColors.Accent)
                    )
                }
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
