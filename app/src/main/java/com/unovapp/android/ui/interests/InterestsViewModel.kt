package com.unovapp.android.ui.interests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Catégories autorisées par le backend (Sprint 2). */
val INTEREST_CATEGORIES = listOf(
    "musique" to "Musique", "sport" to "Sport", "cuisine" to "Cuisine", "culture" to "Culture",
    "mode" to "Mode", "humour" to "Humour", "education" to "Éducation", "religion" to "Religion",
    "politique" to "Politique", "tech" to "Tech"
)

data class InterestsState(
    val selected: Set<String> = emptySet(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class InterestsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InterestsState())
    val state: StateFlow<InterestsState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            when (val r = userRepository.getInterests()) {
                is NetworkResult.Success -> _state.update { it.copy(loading = false, selected = r.data.categories.toSet()) }
                is NetworkResult.Failure -> _state.update { it.copy(loading = false) }
            }
        }
    }

    fun toggle(cat: String) {
        _state.update { s ->
            val next = if (cat in s.selected) s.selected - cat else if (s.selected.size < 10) s.selected + cat else s.selected
            s.copy(selected = next)
        }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (userRepository.setInterests(_state.value.selected.toList())) {
                is NetworkResult.Success -> { _state.update { it.copy(saving = false, saved = true) }; onDone() }
                is NetworkResult.Failure -> _state.update { it.copy(saving = false) }
            }
        }
    }
}
