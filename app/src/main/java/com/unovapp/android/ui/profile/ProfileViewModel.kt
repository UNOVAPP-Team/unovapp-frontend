package com.unovapp.android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.auth.AuthRepository
import com.unovapp.android.data.network.ApiError
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.user.UserProfileDto
import com.unovapp.android.data.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** État réseau du profil connecté (issu de `GET /users/me`). */
data class ProfileNetworkState(
    val isLoading: Boolean = true,
    val profile: UserProfileDto? = null,
    val error: String? = null,
    /** Session invalide/expirée (401) → l'UI doit rediriger vers la connexion. */
    val sessionExpired: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileNetworkState())
    val state: StateFlow<ProfileNetworkState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val r = userRepository.fetchMe()) {
                is NetworkResult.Success ->
                    _state.update { it.copy(isLoading = false, profile = r.data, error = null) }
                is NetworkResult.Failure -> {
                    if (r.error is ApiError.Unauthorized) {
                        // Token absent/expiré et refresh impossible → on repart proprement au login.
                        authRepository.clearSession()
                        _state.update { it.copy(isLoading = false, sessionExpired = true) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = r.error.debugDetail) }
                    }
                }
            }
        }
    }

    /** Déconnexion : efface la session (best-effort backend) puis notifie l'UI. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
