package com.unovapp.android.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.auth.AccountRepository
import com.unovapp.android.data.auth.AuthRepository
import com.unovapp.android.data.auth.SessionDto
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionsState(
    val sessions: List<SessionDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

data class ChangeEmailState(
    val step: Int = 1,            // 1 = saisir nouvel email, 2 = saisir code
    val loading: Boolean = false,
    val error: String? = null,
    val newEmail: String = "",
    val done: Boolean = false
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _sessions = MutableStateFlow(SessionsState())
    val sessions: StateFlow<SessionsState> = _sessions.asStateFlow()

    private val _changeEmail = MutableStateFlow(ChangeEmailState())
    val changeEmail: StateFlow<ChangeEmailState> = _changeEmail.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    /* ---------- Sessions ---------- */

    fun loadSessions() {
        viewModelScope.launch {
            _sessions.update { it.copy(loading = true, error = null) }
            when (val r = accountRepository.sessions()) {
                is NetworkResult.Success -> _sessions.update { it.copy(loading = false, sessions = r.data) }
                is NetworkResult.Failure -> _sessions.update { it.copy(loading = false, error = r.error.userMessage) }
            }
        }
    }

    fun revokeSession(id: String) {
        _sessions.update { s -> s.copy(sessions = s.sessions.filterNot { it.id == id }) }
        viewModelScope.launch { accountRepository.revokeSession(id) }
    }

    /* ---------- Changement d'email ---------- */

    fun requestEmailChange(newEmail: String) {
        viewModelScope.launch {
            _changeEmail.update { it.copy(loading = true, error = null) }
            when (val r = accountRepository.changeEmail(newEmail)) {
                is NetworkResult.Success -> _changeEmail.update { it.copy(loading = false, step = 2, newEmail = newEmail) }
                is NetworkResult.Failure -> _changeEmail.update { it.copy(loading = false, error = r.error.userMessage) }
            }
        }
    }

    fun confirmEmailChange(otpCode: String) {
        viewModelScope.launch {
            _changeEmail.update { it.copy(loading = true, error = null) }
            when (val r = accountRepository.verifyEmailChange(otpCode)) {
                is NetworkResult.Success -> _changeEmail.update { it.copy(loading = false, done = true, newEmail = r.data) }
                is NetworkResult.Failure -> _changeEmail.update { it.copy(loading = false, error = r.error.userMessage) }
            }
        }
    }

    fun resetChangeEmail() = _changeEmail.update { ChangeEmailState() }

    /* ---------- Suppression de compte (RGPD) ---------- */

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _deleting.update { true }
            when (userRepository.deleteAccount()) {
                is NetworkResult.Success -> {
                    authRepository.clearSession()   // purge la session locale
                    _deleting.update { false }
                    onDeleted()
                }
                is NetworkResult.Failure -> _deleting.update { false }
            }
        }
    }
}
