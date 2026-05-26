package com.unovapp.android.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.auth.AuthRepository
import com.unovapp.android.data.auth.GoogleSignInHelper
import com.unovapp.android.data.network.ApiError
import com.unovapp.android.data.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Étape courante du flow Auth. */
enum class AuthStep { Phone, Otp, Success }

/** État UI complet de l'écran Auth — recomposition + survit à la rotation. */
data class AuthUiState(
    val step: AuthStep = AuthStep.Phone,
    val country: Country = COUNTRIES.first(),
    val phone: String = "",
    val phoneError: String? = null,
    val otp: List<String> = List(4) { "" },
    val otpError: String? = null,
    val countdown: Int = 60,
    val isLoading: Boolean = false,
    /** Message d'erreur réseau / serveur — distinct des erreurs de validation. */
    val networkError: String? = null,
    /** Action à rejouer si l'utilisateur tape "Réessayer". null = non retryable. */
    val retryAction: RetryAction? = null
) {
    val phoneValid: Boolean get() = country.isValid(phone)
    val otpValid: Boolean get() = otp.all { it.length == 1 && it[0].isDigit() }
}

enum class RetryAction { SendOtp, VerifyOtp, GoogleSignIn }

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    /* ---------- Input mutations ---------- */

    fun onCountryChange(country: Country) {
        // Le format / la longueur diffèrent : repartir d'un champ vide évite les
        // numéros half-bake quand on passe de Bénin (10) à Togo (8) par ex.
        _state.update {
            it.copy(country = country, phone = "", phoneError = null)
        }
    }

    fun onPhoneChange(raw: String) {
        val maxLen = _state.value.country.phoneLength
        val digits = raw.filter(Char::isDigit).take(maxLen)
        _state.update { it.copy(phone = digits, phoneError = null) }
    }

    fun onOtpDigitChange(index: Int, value: String) {
        val digit = value.filter(Char::isDigit).take(1)
        val nextOtp = _state.value.otp.toMutableList().also { it[index] = digit }
        _state.update { it.copy(otp = nextOtp, otpError = null) }

        // Auto-verify quand les 4 chiffres sont remplis
        if (nextOtp.all { it.isNotEmpty() }) {
            verifyOtp()
        }
    }

    /* ---------- Validation + actions ---------- */

    fun sendOtp() {
        val s = _state.value
        if (!s.phoneValid) {
            val msg = when {
                s.country.phonePrefix != null && !s.phone.startsWith(s.country.phonePrefix) ->
                    "Au ${s.country.name}, le numéro doit commencer par ${s.country.phonePrefix}."
                else -> "Numéro invalide — ${s.country.phoneLength} chiffres requis."
            }
            _state.update { it.copy(phoneError = msg) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, networkError = null) }
            when (val r = repository.sendOtp(phone = s.phone, countryCode = s.country.code)) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            step = AuthStep.Otp,
                            otp = List(4) { "" },
                            otpError = null,
                            isLoading = false,
                            countdown = 60,
                            retryAction = null
                        )
                    }
                    startCountdown()
                }
                is NetworkResult.Failure -> _state.update {
                    it.copy(
                        isLoading = false,
                        networkError = r.error.userMessage,
                        retryAction = if (r.error.isRetryable()) RetryAction.SendOtp else null
                    )
                }
            }
        }
    }

    fun verifyOtp() {
        val s = _state.value
        if (!s.otpValid) {
            _state.update { it.copy(otpError = "Code incomplet.") }
            return
        }
        val code = s.otp.joinToString("")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, networkError = null) }
            when (val r = repository.verifyOtp(phone = s.phone, code = code)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(step = AuthStep.Success, isLoading = false, retryAction = null)
                }
                is NetworkResult.Failure -> {
                    val err = r.error
                    if (err is ApiError.Business && err.code == "OTP_INVALID") {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                otpError = err.userMessage,
                                otp = List(4) { "" },
                                retryAction = null
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                networkError = err.userMessage,
                                retryAction = if (err.isRetryable()) RetryAction.VerifyOtp else null
                            )
                        }
                    }
                }
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, networkError = null) }
            when (val tokenResult = googleSignInHelper.requestIdToken(context)) {
                is NetworkResult.Failure -> _state.update {
                    it.copy(
                        isLoading = false,
                        networkError = tokenResult.error.userMessage,
                        retryAction = null
                    )
                }
                is NetworkResult.Success -> {
                    when (val r = repository.signInWithGoogle(tokenResult.data)) {
                        is NetworkResult.Success -> _state.update {
                            it.copy(step = AuthStep.Success, isLoading = false, retryAction = null)
                        }
                        is NetworkResult.Failure -> _state.update {
                            it.copy(
                                isLoading = false,
                                networkError = r.error.userMessage,
                                retryAction = if (r.error.isRetryable()) RetryAction.GoogleSignIn else null
                            )
                        }
                    }
                }
            }
        }
    }

    /* ---------- Navigation & error UX ---------- */

    fun goBackFromOtp() {
        countdownJob?.cancel()
        _state.update { it.copy(step = AuthStep.Phone, otp = List(4) { "" }, otpError = null) }
    }

    fun dismissError() = _state.update { it.copy(networkError = null) }

    fun retryLastAction(context: Context) {
        when (_state.value.retryAction) {
            RetryAction.SendOtp -> { dismissError(); sendOtp() }
            RetryAction.VerifyOtp -> { dismissError(); verifyOtp() }
            RetryAction.GoogleSignIn -> { dismissError(); signInWithGoogle(context) }
            null -> dismissError()
        }
    }

    fun resendOtp() {
        _state.update { it.copy(otp = List(4) { "" }, otpError = null, countdown = 60) }
        sendOtp()
    }

    /* ---------- Helpers ---------- */

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_state.value.countdown > 0 && _state.value.step == AuthStep.Otp) {
                delay(1000)
                _state.update { it.copy(countdown = (it.countdown - 1).coerceAtLeast(0)) }
            }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}

private fun ApiError.isRetryable(): Boolean = this is ApiError.Network ||
    this is ApiError.Timeout ||
    this is ApiError.Server
