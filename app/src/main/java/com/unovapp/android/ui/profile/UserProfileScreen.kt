@file:OptIn(ExperimentalFoundationApi::class)

package com.unovapp.android.ui.profile

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.user.FollowManager
import com.unovapp.android.data.user.FollowStore
import com.unovapp.android.data.user.UserProfileDto
import com.unovapp.android.data.user.UserRepository
import com.unovapp.android.data.user.UserProfileStore
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.components.ErrorRetry
import com.unovapp.android.ui.components.ShimmerBox
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ---------- ViewModel ---------- */

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val profileStore: UserProfileStore,
    private val followManager: FollowManager,
    private val followStore: FollowStore
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val profile: UserProfileDto? = null,
        val error: String? = null,
        val isFollowing: Boolean = false,
        val followersDelta: Int = 0
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var userId: String? = null
    private var observing = false

    fun load(id: String) {
        userId = id
        if (!observing) {
            observing = true
            viewModelScope.launch {
                followStore.following.collect { set ->
                    val uid = userId
                    _state.update { it.copy(isFollowing = uid != null && set.contains(uid)) }
                }
            }
            viewModelScope.launch {
                profileStore.profiles.collect { profiles ->
                    val uid = userId
                    val fresh = if (uid != null) profiles[uid] else null
                    if (fresh != null) _state.update { it.copy(profile = fresh) }
                }
            }
        }
        if (_state.value.profile?.id == id) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = userRepository.getUser(id)) {
                is NetworkResult.Success -> {
                    // Synchronise le suivi depuis le backend (is_following) → le bouton reflète
                    // la réalité même après redémarrage (le FollowStore en mémoire était vide).
                    if (r.data.isFollowing) followStore.merge(listOf(id))
                    _state.update { it.copy(loading = false, profile = r.data, error = null) }
                }
                is NetworkResult.Failure -> _state.update { it.copy(loading = false, error = r.error.debugDetail) }
            }
        }
    }

    fun toggleFollow() {
        val id = userId ?: return
        val willFollow = !followStore.isFollowing(id)
        _state.update { it.copy(followersDelta = it.followersDelta + if (willFollow) 1 else -1) }
        followManager.toggle(id)
    }

    /** Bloquer / débloquer (optimiste). Bloquer rompt le suivi côté serveur. */
    fun toggleBlock() {
        val id = userId ?: return
        val willBlock = !(_state.value.profile?.isBlocked ?: false)
        _state.update { it.copy(profile = it.profile?.copy(isBlocked = willBlock)) }
        viewModelScope.launch {
            if (willBlock) userRepository.blockUser(id) else userRepository.unblockUser(id)
        }
    }

    fun retry() {
        val id = userId ?: return
        _state.update { it.copy(profile = null) }
        load(id)
    }
}

/* ---------- Tabs ---------- */

private enum class UserProfileTab(val label: String) {
    Videos("Vidéos"),
    Liked("Appréciées"),
    Battles("Battles")
}

/* ---------- Screen ---------- */

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenConnections: (userId: String, username: String, showFollowers: Boolean) -> Unit
) {
    UnovAppTheme {
        val vm: UserProfileViewModel = hiltViewModel(key = userId)
        LaunchedEffect(userId) { vm.load(userId) }
        val s by vm.state.collectAsStateWithLifecycle()
        var activeTab by remember { mutableStateOf(UserProfileTab.Videos) }
        var moreMenuOpen by remember { mutableStateOf(false) }

        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UnovColors.BgBase)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 78.dp + navBarBottom)
            ) {
                // Hero : couverture + avatar + infos + actions
                item(key = "hero") {
                    when {
                        s.loading && s.profile == null -> ProfileHeroLoading()
                        s.error != null && s.profile == null -> Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ErrorRetry(message = "Profil indisponible", onRetry = vm::retry)
                        }
                        s.profile != null -> {
                            val p = s.profile!!
                            val followers = (p.followersCount + s.followersDelta).coerceAtLeast(0)
                            ProfileHero(
                                profile = p,
                                followersCount = followers,
                                isFollowing = s.isFollowing,
                                onToggleFollow = vm::toggleFollow,
                                onOpenFollowers = { onOpenConnections(p.id, p.username, true) },
                                onOpenFollowing = { onOpenConnections(p.id, p.username, false) }
                            )
                        }
                    }
                }

                // Barre d'onglets — reste collée en haut au scroll
                stickyHeader(key = "tabs") {
                    UserProfileTabRow(activeTab = activeTab, onSelect = { activeTab = it })
                }

                // Contenu de l'onglet actif
                item(key = "content_$activeTab") {
                    when (activeTab) {
                        UserProfileTab.Videos  -> VideoGridPlaceholder("🎬", "Aucune vidéo pour l'instant")
                        UserProfileTab.Liked   -> VideoGridPlaceholder("❤️", "Aucune vidéo appréciée")
                        UserProfileTab.Battles -> VideoGridPlaceholder("⚔️", "Aucun battle pour l'instant")
                    }
                }
            }

            // Contrôles flottants (retour, partager, more) — au-dessus de la couverture edge-to-edge
            ProfileTopBar(onBack = onBack, onMore = { moreMenuOpen = true })

            // Menu ⋮ : bloquer / débloquer (câblé backend).
            if (moreMenuOpen && s.profile != null) {
                UserMoreMenu(
                    isBlocked = s.profile!!.isBlocked,
                    onToggleBlock = { vm.toggleBlock(); moreMenuOpen = false },
                    onDismiss = { moreMenuOpen = false }
                )
            }
        }
    }
}

/** Menu ⋮ d'un profil visité : bloquer / débloquer l'utilisateur. */
@Composable
private fun UserMoreMenu(isBlocked: Boolean, onToggleBlock: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onDismiss)
        )
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(UnovColors.BgRaised)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(12.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onToggleBlock).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Outlined.Block, null, tint = UnovColors.Danger, modifier = Modifier.size(22.dp))
                Text(if (isBlocked) "Débloquer" else "Bloquer", color = UnovColors.Danger, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).background(UnovColors.Surface).clickable(onClick = onDismiss).padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) { Text("Annuler", color = UnovColors.TextDim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/* ---------- Hero ---------- */

@Composable
private fun ProfileHero(
    profile: UserProfileDto,
    followersCount: Int,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val coverHeight = 200.dp + statusTop
    val avatarSize = 94.dp
    val avatarOverlap = avatarSize / 2f   // 47dp → le disque chevauche la cover de la moitié

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Couverture + avatar ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverHeight + avatarOverlap)   // espace pour l'overlap de l'avatar
        ) {
            // Gradient de couverture (tier-aware, edge-to-edge derrière la status bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverHeight)
                    .align(Alignment.TopCenter)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(coverBrush(profile.subscriptionTier)))

                // Glow d'accent en coin supérieur-droit (profondeur visuelle)
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            Brush.radialGradient(
                                listOf(tierGlowColor(profile.subscriptionTier).copy(alpha = 0.28f), Color.Transparent)
                            )
                        )
                )

                // Scrim bas → fond noir (lisibilité de l'avatar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, UnovColors.BgBase))
                        )
                )
            }

            // Avatar positionné au bas du Box, le bord inférieur = bas du Box
            ProfileAvatarRing(
                profile = profile,
                size = avatarSize,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // ── Nom · badge tier · @username ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = profile.displayName?.takeIf { it.isNotBlank() } ?: profile.username,
                    color = UnovColors.Text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (profile.isVerified) {
                    Icon(
                        Icons.Filled.Verified,
                        contentDescription = "Vérifié",
                        tint = UnovColors.Accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "@${profile.username}",
                color = UnovColors.TextMute,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 3.dp)
            )

            Spacer(Modifier.height(8.dp))
            TierBadge(tier = profile.subscriptionTier)

            if (!profile.bio.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = profile.bio,
                    color = UnovColors.TextDim,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            // ── Stats ────────────────────────────────────────────────────────────
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatTile(
                    value = compact(profile.followingCount),
                    label = "Abonnements",
                    onClick = onOpenFollowing,
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                StatTile(
                    value = compact(followersCount),
                    label = "Abonnés",
                    onClick = onOpenFollowers,
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                StatTile(value = "—", label = "Vidéos", onClick = null, modifier = Modifier.weight(1f))
                StatDivider()
                StatTile(value = "—", label = "Battles", onClick = null, modifier = Modifier.weight(1f))
            }

            // ── Boutons d'action ─────────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Suivre / Suivi — bouton principal plein gradient
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (isFollowing)
                                Modifier.border(1.dp, UnovColors.LineStrong, RoundedCornerShape(14.dp))
                            else
                                Modifier.background(UnovGradients.Gold)
                        )
                        .clickable(onClick = onToggleFollow),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isFollowing) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = UnovColors.Text,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = if (isFollowing) "Suivi" else "Suivre",
                            color = if (isFollowing) UnovColors.Text else Color(0xFF0D0D0D),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Message
                SmallIconButton(
                    icon = Icons.AutoMirrored.Outlined.Message,
                    contentDescription = "Message",
                    tint = UnovColors.TextDim
                )

                // Envoyer un Jeton — différenciateur Mobile Money UNOVAPP
                SmallIconButton(
                    icon = Icons.Outlined.Redeem,
                    contentDescription = "Envoyer un Jeton",
                    tint = UnovColors.Accent,
                    glowColor = UnovColors.AccentGlow
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/* ---------- Cover helpers ---------- */

private fun coverBrush(tier: String): Brush = when {
    tier.equals("star", ignoreCase = true) -> Brush.linearGradient(
        listOf(Color(0xFF1A0A00), Color(0xFF5A3000), Color(0xFF9A5A00), Color(0xFF2A1400))
    )
    tier.equals("pro", ignoreCase = true) -> Brush.linearGradient(
        listOf(Color(0xFF1A0A00), Color(0xFF2A1400), Color(0xFF5A3000), Color(0xFF1A0A00))
    )
    else -> Brush.linearGradient(
        listOf(Color(0xFF0A0500), Color(0xFF1A0E00), Color(0xFF2A1606), Color(0xFF0A0500))
    )
}

private fun tierGlowColor(tier: String): Color = when {
    tier.equals("star", ignoreCase = true) -> UnovColors.Accent
    tier.equals("pro", ignoreCase = true) -> Color(0xFFFF944D)
    else -> Color(0xFFE55F00)
}

/* ---------- Avatar kente ring ---------- */

@Composable
private fun ProfileAvatarRing(
    profile: UserProfileDto,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val avatarIdx = kotlin.math.abs(profile.id.hashCode()) % 6
    val ringSize = size + 6.dp   // 3dp de bord de chaque côté

    Box(modifier = modifier.size(ringSize)) {
        // Anneau sweep kente (or → ocre → terre cuite → brun → or)
        Box(
            modifier = Modifier
                .size(ringSize)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            UnovColors.Accent,
                            Color(0xFFE55F00),
                            Color(0xFF8B3A14),
                            Color(0xFF3E1A08),
                            UnovColors.AccentDeep,
                            UnovColors.Accent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(UnovColors.BgBase),
                contentAlignment = Alignment.Center
            ) {
                Avatar(idx = avatarIdx, name = profile.username, size = size - 8.dp)
            }
        }

        // Badge vérifié (coin inférieur-droit)
        if (profile.isVerified) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(UnovColors.BgBase)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(UnovColors.Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Verified,
                    contentDescription = null,
                    tint = Color(0xFF0D0D0D),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/* ---------- Tier badge ---------- */

@Composable
private fun TierBadge(tier: String) {
    val (label, brush, textColor) = when {
        tier.equals("star", ignoreCase = true) ->
            Triple("⭐  STAR", UnovGradients.Gold as Brush, Color(0xFF0D0D0D))
        tier.equals("pro", ignoreCase = true) ->
            Triple("PRO", Brush.linearGradient(listOf(Color(0xFF1A4A7C), Color(0xFF4A9ABD))), UnovColors.Text)
        else ->
            Triple("FREE", Brush.linearGradient(listOf(UnovColors.Surface, UnovColors.SurfaceAlt)), UnovColors.TextMute)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(brush)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )
    }
}

/* ---------- Stats row ---------- */

@Composable
private fun StatTile(
    value: String,
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val noRipple = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .then(
                if (onClick != null)
                    Modifier.clickable(interactionSource = noRipple, indication = null, onClick = onClick)
                else Modifier
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = UnovColors.Text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label.uppercase(),
            color = UnovColors.TextMute,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun StatDivider() {
    Box(modifier = Modifier.width(1.dp).height(32.dp).background(UnovColors.Line))
}

/* ---------- Action button helpers ---------- */

@Composable
private fun SmallIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    glowColor: Color = Color.Transparent
) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(14.dp))
            .then(
                if (glowColor != Color.Transparent)
                    Modifier.background(glowColor.copy(alpha = 0.12f))
                else Modifier
            )
            .clickable(interactionSource = noRipple, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/* ---------- Sticky tab bar ---------- */

@Composable
private fun UserProfileTabRow(activeTab: UserProfileTab, onSelect: (UserProfileTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(UnovColors.BgBase)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            UserProfileTab.values().forEach { tab ->
                val isActive = tab == activeTab
                val noRipple = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = noRipple,
                            indication = null,
                            onClick = { onSelect(tab) }
                        )
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = tab.label,
                        color = if (isActive) UnovColors.Text else UnovColors.TextMute,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                    // Indicateur or sous l'onglet actif
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .then(if (isActive) Modifier.background(UnovGradients.Gold) else Modifier)
                    )
                }
            }
        }
        // Séparateur bas
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(UnovColors.Line)
        )
    }
}

/* ---------- Grille placeholder ---------- */

@Composable
private fun VideoGridPlaceholder(tabIcon: String, emptyMessage: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Grille 3 colonnes de thumbnails grisés (aperçu du futur contenu)
        repeat(3) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(9f / 16f)
                            .padding(1.dp)
                            .background(UnovColors.SurfaceAlt, RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Message d'état vide
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = tabIcon, fontSize = 36.sp)
            Text(
                text = emptyMessage,
                color = UnovColors.TextMute,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ---------- Top bar flottant ---------- */

@Composable
private fun ProfileTopBar(onBack: () -> Unit, onMore: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, cd = "Retour", onClick = onBack)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopBarIconButton(icon = Icons.Outlined.Share, cd = "Partager", onClick = {})
            TopBarIconButton(icon = Icons.Outlined.MoreHoriz, cd = "Options", onClick = onMore)
        }
    }
}

@Composable
private fun TopBarIconButton(icon: ImageVector, cd: String, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = cd, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/* ---------- Loading skeleton ---------- */

@Composable
private fun ProfileHeroLoading() {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp + statusTop))
        ShimmerBox(modifier = Modifier.size(94.dp), shape = CircleShape)
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(22.dp), shape = RoundedCornerShape(6.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.3f).height(14.dp), shape = RoundedCornerShape(6.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp))
    }
}

/* ---------- Helpers ---------- */

private fun compact(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1f M", n / 1_000_000.0).replace(".0 ", " ")
    n >= 1_000     -> String.format("%.1f K", n / 1_000.0).replace(".0 ", " ")
    else           -> n.toString()
}
