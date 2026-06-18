package com.unovapp.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    private val followManager: FollowManager,
    private val followStore: FollowStore
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val profile: UserProfileDto? = null,
        val error: String? = null,
        val isFollowing: Boolean = false,
        /** Ajustement optimiste du nombre d'abonnés du profil visité quand je le suis/désabonne. */
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
        }
        if (_state.value.profile?.id == id) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = userRepository.getUser(id)) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, profile = r.data, error = null) }
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

    fun retry() {
        val id = userId ?: return
        _state.update { it.copy(profile = null) }
        load(id)
    }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050505))
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircleIcon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onClick = onBack)
                Text(
                    text = s.profile?.username?.let { "@$it" } ?: "Profil",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            when {
                s.loading && s.profile == null -> LoadingHeader()
                s.error != null && s.profile == null ->
                    ErrorRetry(message = "Profil indisponible.\n${s.error}", onRetry = vm::retry)
                s.profile != null -> {
                    val p = s.profile!!
                    val followers = (p.followersCount + s.followersDelta).coerceAtLeast(0)
                    ProfileHeader(
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
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfileDto,
    followersCount: Int,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).background(UnovGradients.Gold),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(90.dp).clip(CircleShape).background(Color(0xFF050505)),
                    contentAlignment = Alignment.Center
                ) {
                    Avatar(idx = kotlin.math.abs(profile.id.hashCode()) % 6, name = profile.username, size = 84.dp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = profile.displayName?.takeIf { it.isNotBlank() } ?: profile.username,
                color = UnovColors.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (profile.isVerified) {
                Icon(Icons.Filled.Verified, contentDescription = "Vérifié", tint = UnovColors.Accent, modifier = Modifier.size(16.dp))
            }
        }
        Text(text = "@${profile.username}", color = UnovColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))

        // Stats (Abonnés / Abonnements cliquables)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatTile(value = compact(followersCount), label = "Abonnés", onClick = onOpenFollowers, modifier = Modifier.weight(1f))
            Divider()
            StatTile(value = compact(profile.followingCount), label = "Abonnements", onClick = onOpenFollowing, modifier = Modifier.weight(1f))
            Divider()
            StatTile(value = tierLabel(profile.subscriptionTier), label = "Tier", onClick = null, modifier = Modifier.weight(1f))
        }

        if (!profile.bio.isNullOrBlank()) {
            Text(
                text = profile.bio,
                color = UnovColors.TextDim,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 14.dp)
            )
        }

        // Actions : Suivre/Suivi + Message (bientôt)
        Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isFollowing) Modifier.border(1.dp, UnovColors.LineStrong, RoundedCornerShape(14.dp))
                        else Modifier.background(UnovGradients.Gold)
                    )
                    .clickable(onClick = onToggleFollow),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isFollowing) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = UnovColors.Text, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = if (isFollowing) "Suivi" else "Suivre",
                        color = if (isFollowing) UnovColors.Text else Color(0xFF0D0D0D),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, UnovColors.Line, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.Message, contentDescription = "Message (bientôt)", tint = UnovColors.TextDim, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "Vidéos bientôt disponibles",
            color = UnovColors.TextMute,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val noRipple = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(interactionSource = noRipple, indication = null, onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, color = UnovColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            text = label.uppercase(),
            color = UnovColors.TextMute,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.width(1.dp).height(34.dp).background(UnovColors.Line))
}

@Composable
private fun LoadingHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(modifier = Modifier.size(96.dp), shape = CircleShape)
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(20.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp))
    }
}

@Composable
private fun CircleIcon(icon: ImageVector, cd: String, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(UnovColors.Surface)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = cd, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

private fun compact(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1f M", n / 1_000_000.0).replace(".0 ", " ")
    n >= 1_000 -> String.format("%.1f K", n / 1_000.0).replace(".0 ", " ")
    else -> n.toString()
}

private fun tierLabel(tier: String): String =
    if (tier.equals("free", ignoreCase = true)) "Free" else tier.replaceFirstChar { it.uppercase() }
