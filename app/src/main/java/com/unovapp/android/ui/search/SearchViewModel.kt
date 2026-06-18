package com.unovapp.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.user.FollowManager
import com.unovapp.android.data.user.FollowStore
import com.unovapp.android.data.user.UserRepository
import com.unovapp.android.data.user.UserSummaryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<UserSummaryDto> = emptyList(),
    val followingIds: Set<String> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val followManager: FollowManager,
    followStore: FollowStore
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())

    // L'état des boutons « Suivre » vient du store partagé → cohérent avec profil & listes,
    // et reflété instantanément dès qu'on suit quelqu'un n'importe où dans l'app.
    val state: StateFlow<SearchUiState> =
        combine(_state, followStore.following) { s, following -> s.copy(followingIds = following) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SearchUiState())

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        if (q.isBlank()) {
            _state.update { it.copy(loading = false, results = emptyList(), error = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _state.update { it.copy(loading = true, error = null) }
            when (val r = userRepository.search(q.trim())) {
                is NetworkResult.Success ->
                    _state.update { it.copy(loading = false, results = r.data.data, error = null) }
                is NetworkResult.Failure ->
                    _state.update { it.copy(loading = false, error = r.error.debugDetail) }
            }
        }
    }

    /** Suivre / ne plus suivre — délégué au store partagé (optimiste + rollback centralisés). */
    fun toggleFollow(userId: String) = followManager.toggle(userId)
}
