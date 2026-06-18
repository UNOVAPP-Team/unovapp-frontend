package com.unovapp.android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.auth.AuthRepository
import com.unovapp.android.data.network.ApiError
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.user.FollowStore
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
    val sessionExpired: Boolean = false,
    // Édition du profil
    val saving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val followStore: FollowStore
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileNetworkState())
    val state: StateFlow<ProfileNetworkState> = _state.asStateFlow()

    /**
     * Variation du nombre d'abonnements depuis le dernier `/users/me` — permet d'incrémenter
     * « Suivis » instantanément quand on suit quelqu'un (recherche, autre profil…) sans recharger.
     */
    val followingDelta: StateFlow<Int> = followStore.followingDelta

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val r = userRepository.fetchMe()) {
                is NetworkResult.Success -> {
                    // Le compteur backend intègre déjà les follows de la session → on repart de 0.
                    followStore.resetDelta()
                    _state.update { it.copy(isLoading = false, profile = r.data, error = null) }
                }
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

    /** Met à jour le profil (display_name, bio, username) via PATCH /users/{id}. */
    fun updateProfile(displayName: String, bio: String, username: String, onSaved: () -> Unit) {
        val id = _state.value.profile?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, saveError = null) }
            when (val r = userRepository.updateProfile(id, displayName, bio, username)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(saving = false, profile = r.data, saveError = null) }
                    onSaved()
                }
                is NetworkResult.Failure ->
                    _state.update { it.copy(saving = false, saveError = r.error.debugDetail) }
            }
        }
    }

    fun clearSaveError() = _state.update { it.copy(saveError = null) }

    /** Déconnexion : efface la session (best-effort backend) puis notifie l'UI. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
