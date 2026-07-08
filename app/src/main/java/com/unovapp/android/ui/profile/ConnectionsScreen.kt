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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
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
import com.unovapp.android.data.user.UserRepository
import com.unovapp.android.data.user.UserSummaryDto
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.components.EmptyState
import com.unovapp.android.ui.components.ErrorRetry
import com.unovapp.android.ui.components.ShimmerBox
import com.unovapp.android.ui.components.unovTap
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ---------- ViewModel ---------- */

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val followManager: FollowManager,
    private val followStore: FollowStore
) : ViewModel() {

    data class State(
        val userId: String = "",
        val isSelf: Boolean = false,
        val showFollowers: Boolean = true,
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val users: List<UserSummaryDto> = emptyList(),
        val error: String? = null,
        val page: Int = 1,
        val total: Int = 0,
        val filter: String = "",
        val followingIds: Set<String> = emptySet()
    )

    private val _state = MutableStateFlow(State())
    val state = combine(_state, followStore.following) { s, f -> s.copy(followingIds = f) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State())

    private var initialized = false

    fun start(userId: String, isSelf: Boolean, showFollowers: Boolean) {
        if (initialized) return
        initialized = true
        _state.update { it.copy(userId = userId, isSelf = isSelf, showFollowers = showFollowers) }
        fetch(reset = true)
    }

    fun setTab(showFollowers: Boolean) {
        if (_state.value.showFollowers == showFollowers) return
        _state.update { it.copy(showFollowers = showFollowers, users = emptyList(), page = 1, total = 0, error = null) }
        fetch(reset = true)
    }

    fun setFilter(q: String) = _state.update { it.copy(filter = q) }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || s.users.size >= s.total) return
        fetch(reset = false)
    }

    fun retry() = fetch(reset = true)

    fun toggleFollow(id: String) = followManager.toggle(id)

    private fun fetch(reset: Boolean) {
        val s = _state.value
        val nextPage = if (reset) 1 else s.page + 1
        viewModelScope.launch {
            _state.update { it.copy(loading = reset, loadingMore = !reset, error = null) }
            val r = if (s.showFollowers) userRepository.followers(s.userId, nextPage)
            else userRepository.following(s.userId, nextPage)
            when (r) {
                is NetworkResult.Success -> {
                    val pageData = r.data
                    _state.update {
                        it.copy(
                            loading = false,
                            loadingMore = false,
                            users = if (reset) pageData.data else it.users + pageData.data,
                            page = pageData.page,
                            total = pageData.total,
                            error = null
                        )
                    }
                    // Ma propre liste d'abonnements = exactement les gens que je suis → on amorce le store.
                    if (s.isSelf && !s.showFollowers) {
                        followStore.merge(pageData.data.map { u -> u.id })
                    }
                }
                is NetworkResult.Failure ->
                    _state.update { it.copy(loading = false, loadingMore = false, error = r.error.debugDetail) }
            }
        }
    }
}

/* ---------- Screen ---------- */

@Composable
fun ConnectionsScreen(
    userId: String,
    username: String,
    isSelf: Boolean,
    showFollowers: Boolean,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit
) {
    UnovAppTheme {
        val vm: ConnectionsViewModel = hiltViewModel(key = "conn-$userId-$showFollowers")
        LaunchedEffect(Unit) { vm.start(userId, isSelf, showFollowers) }
        val s by vm.state.collectAsStateWithLifecycle()

        val filtered = remember(s.users, s.filter) {
            val q = s.filter.trim().lowercase()
            if (q.isEmpty()) s.users
            else s.users.filter { u ->
                u.username.lowercase().contains(q) || (u.displayName?.lowercase()?.contains(q) == true)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircleIconBtn(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onClick = onBack)
                Text(text = "@$username", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Tabs
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Tab(label = "Abonnés", active = s.showFollowers, onClick = { vm.setTab(true) }, modifier = Modifier.weight(1f))
                Tab(label = "Abonnements", active = !s.showFollowers, onClick = { vm.setTab(false) }, modifier = Modifier.weight(1f))
            }

            // Recherche dans la liste (mieux que TikTok : filtre instantané)
            SearchField(value = s.filter, onValueChange = vm::setFilter)

            when {
                s.loading && s.users.isEmpty() -> Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) { repeat(7) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp)) } }

                s.error != null && s.users.isEmpty() ->
                    ErrorRetry(message = "Chargement impossible.\n${s.error}", onRetry = vm::retry)

                filtered.isEmpty() -> EmptyState(
                    title = if (s.showFollowers) "Aucun abonné" else "Aucun abonnement",
                    subtitle = if (s.filter.isNotBlank()) "Aucun résultat pour « ${s.filter} »"
                    else if (s.showFollowers) "Personne ne suit ce compte pour l'instant."
                    else "Ce compte ne suit personne pour l'instant."
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { user ->
                        ConnectionRow(
                            user = user,
                            following = s.followingIds.contains(user.id),
                            onToggleFollow = { vm.toggleFollow(user.id) },
                            onOpen = { onOpenUser(user.id) }
                        )
                    }
                    if (s.users.size < s.total && s.filter.isBlank()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (s.loadingMore) {
                                    CircularProgressIndicator(color = UnovColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                } else {
                                    Text(
                                        text = "Charger plus",
                                        color = UnovColors.Accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.unovTap(onClick = vm::loadMore, pressedScale = 0.94f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tab(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.unovTap(onClick = onClick, pressedScale = 0.96f).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = if (active) UnovColors.Text else UnovColors.TextMute,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = 2.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (active) UnovColors.Accent else Color.Transparent)
        )
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.Line, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = UnovColors.TextMute, modifier = Modifier.size(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Filtrer cette liste…", color = UnovColors.TextMute, fontSize = 13.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = UnovColors.Text, fontSize = 13.sp),
                cursorBrush = SolidColor(UnovColors.Accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConnectionRow(
    user: UserSummaryDto,
    following: Boolean,
    onToggleFollow: () -> Unit,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(UnovColors.Surface)
            .unovTap(onClick = onOpen, pressedScale = 0.98f)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Avatar(idx = kotlin.math.abs(user.id.hashCode()) % 6, name = user.username, size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("@${user.username}", color = UnovColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (user.isVerified) {
                    Icon(Icons.Filled.Verified, contentDescription = "Vérifié", tint = UnovColors.Accent, modifier = Modifier.size(13.dp))
                }
            }
            if (!user.displayName.isNullOrBlank()) {
                Text(user.displayName, color = UnovColors.TextMute, fontSize = 12.sp)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .then(
                    if (following) Modifier.border(1.dp, UnovColors.LineStrong, RoundedCornerShape(999.dp))
                    else Modifier.background(UnovGradients.Gold)
                )
                .unovTap(onClick = onToggleFollow, pressedScale = 0.94f)
                .padding(horizontal = 16.dp, vertical = 7.dp)
        ) {
            Text(
                text = if (following) "Suivi" else "Suivre",
                color = if (following) UnovColors.TextDim else Color(0xFF0D0D0D),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CircleIconBtn(icon: ImageVector, cd: String, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(UnovColors.Surface)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = cd, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
