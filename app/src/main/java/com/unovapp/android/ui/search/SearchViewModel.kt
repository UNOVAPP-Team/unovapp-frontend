package com.unovapp.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.user.UserRepository
import com.unovapp.android.data.user.UserSummaryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

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

    /** Suivre / ne plus suivre (optimiste + rollback si l'API échoue). */
    fun toggleFollow(userId: String) {
        val wasFollowing = _state.value.followingIds.contains(userId)
        _state.update {
            it.copy(
                followingIds = if (wasFollowing) it.followingIds - userId
                else it.followingIds + userId
            )
        }
        viewModelScope.launch {
            val r = if (wasFollowing) userRepository.unfollow(userId)
            else userRepository.follow(userId)
            if (r is NetworkResult.Failure) {
                _state.update {
                    it.copy(
                        followingIds = if (wasFollowing) it.followingIds + userId
                        else it.followingIds - userId
                    )
                }
            }
        }
    }
}
