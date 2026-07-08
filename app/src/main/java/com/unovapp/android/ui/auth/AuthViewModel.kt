package com.unovapp.android.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.BuildConfig
import com.unovapp.android.data.auth.AuthRepository
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

/** Étape courante du flow Auth. (Otp conservé mais désormais hors flux — vérif tél. désactivée.) */
enum class AuthStep { Welcome, Form, Otp, VerifyEmail, Success }

/** Mode choisi par l'utilisateur depuis l'écran Welcome. */
enum class AuthMode { Login, Register }

/** État UI complet de l'écran Auth — survit à la rotation, dirige toute la composition. */
data class AuthUiState(
    val step: AuthStep = AuthStep.Welcome,
    val mode: AuthMode = AuthMode.Register,

    // Formulaire
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val country: Country = COUNTRIES.first(),
    val phone: String = "",

    val emailError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val phoneError: String? = null,

    // OTP (6 chiffres côté backend)
    val otp: List<String> = List(OTP_LENGTH) { "" },
    val otpError: String? = null,
    val countdown: Int = 0,

    // Réseau / loading
    val isLoading: Boolean = false,
    val networkError: String? = null,
    val retryAction: RetryAction? = null,

    // Vérification email
    val infoMessage: String? = null,
    val emailVerified: Boolean = false
) {
    val emailValid: Boolean get() = EMAIL_REGEX.matches(email)
    val usernameValid: Boolean get() = username.length in 3..20
    val passwordValid: Boolean get() = password.length >= 8
    val phoneValid: Boolean get() = country.isValid(phone)
    val otpValid: Boolean get() = otp.all { it.length == 1 && it[0].isDigit() }

    /** Le téléphone est facultatif (cf. hint "vérification facultative"). */
    val phoneProvided: Boolean get() = phone.isNotBlank()
    /** OK si laissé vide, ou valide s'il est renseigné. */
    val phoneOk: Boolean get() = !phoneProvided || phoneValid

    val phoneE164: String get() = "${country.code}${phone}"

    /** Validité du formulaire courant selon le mode. */
    val formValid: Boolean
        get() = when (mode) {
            AuthMode.Login -> emailValid && passwordValid
            AuthMode.Register -> emailValid && usernameValid && passwordValid && phoneOk
        }
}

enum class RetryAction { SubmitForm, SendOtp, VerifyOtp }

private const val OTP_LENGTH = 6
private const val RESEND_COUNTDOWN_SEC = 60
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    /* ---------- Navigation ---------- */

    fun chooseMode(mode: AuthMode) {
        _state.update { it.copy(mode = mode, step = AuthStep.Form, networkError = null, infoMessage = null) }
    }

    fun goBack() {
        countdownJob?.cancel()
        _state.update { current ->
            when (current.step) {
                AuthStep.Form -> current.copy(step = AuthStep.Welcome, networkError = null)
                AuthStep.Otp -> current.copy(
                    step = AuthStep.Form,
                    otp = List(OTP_LENGTH) { "" },
                    otpError = null,
                    networkError = null
                )
                // Le compte est déjà créé : revenir en arrière depuis la vérif email/OTP
                // ramène à l'accueil (l'utilisateur pourra se connecter).
                AuthStep.VerifyEmail -> current.copy(
                    step = AuthStep.Welcome,
                    networkError = null,
                    infoMessage = null
                )
                else -> current
            }
        }
    }

    /* ---------- Input mutations ---------- */

    fun onEmailChange(v: String) = _state.update {
        it.copy(email = v.trim(), emailError = null)
    }

    fun onUsernameChange(v: String) = _state.update {
        it.copy(username = v.filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(20), usernameError = null)
    }

    fun onPasswordChange(v: String) = _state.update {
        it.copy(password = v, passwordError = null)
    }

    fun onCountryChange(country: Country) = _state.update {
        it.copy(country = country, phone = "", phoneError = null)
    }

    fun onPhoneChange(raw: String) {
        val maxLen = _state.value.country.phoneLength
        val digits = raw.filter(Char::isDigit).take(maxLen)
        _state.update { it.copy(phone = digits, phoneError = null) }
    }

    fun onOtpDigitChange(index: Int, value: String) {
        val digit = value.filter(Char::isDigit).take(1)
        val next = _state.value.otp.toMutableList().also { it[index] = digit }
        _state.update { it.copy(otp = next, otpError = null) }
        // Auto-submit dès que les 6 chiffres sont remplis
        if (next.all { it.isNotEmpty() }) verifyOtp()
    }

    /* ---------- Submit / OTP ---------- */

    /**
     * Soumet le formulaire (login ou register selon `mode`).
     * Après register : on bascule sur l'étape OTP et on envoie le code SMS.
     * Après login : on va directement au Success.
     */
    fun submitForm() {
        val s = _state.value
        // Validation locale
        val emailErr = if (s.emailValid) null else "Adresse email invalide."
        val pwdErr = if (s.passwordValid) null else "Mot de passe trop court (8+ caractères)."
        val userErr = if (s.mode == AuthMode.Register && !s.usernameValid)
            "Pseudo entre 3 et 20 caractères." else null
        val phoneErr = if (s.mode == AuthMode.Register && s.phoneProvided && !s.phoneValid)
            "Numéro ${s.country.name} invalide." else null
        if (listOfNotNull(emailErr, pwdErr, userErr, phoneErr).isNotEmpty()) {
            _state.update {
                it.copy(
                    emailError = emailErr,
                    passwordError = pwdErr,
                    usernameError = userErr,
                    phoneError = phoneErr
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, networkError = null) }
            if (s.mode == AuthMode.Login) {
                when (val r = repository.login(s.email, s.password)) {
                    is NetworkResult.Success ->
                        _state.update { it.copy(step = AuthStep.Success, isLoading = false) }
                    is NetworkResult.Failure ->
                        handleAuthError(r.error, RetryAction.SubmitForm)
                }
            } else {
                doRegister(s, attempt = 0)
            }
        }
    }

    private suspend fun doRegister(s: AuthUiState, attempt: Int) {
        val r = repository.register(
            email = s.email,
            username = s.username,
            password = s.password,
            phoneNumber = if (s.phoneProvided) s.phoneE164 else null
        )
        when (r) {
            is NetworkResult.Success -> {
                _state.update {
                    it.copy(
                        step = AuthStep.Otp,
                        isLoading = false,
                        otp = List(OTP_LENGTH) { "" },
                        otpError = null,
                        countdown = RESEND_COUNTDOWN_SEC,
                        networkError = null,
                        infoMessage = "Code à 6 chiffres envoyé à ${s.email}"
                    )
                }
                startCountdown()
            }
            is NetworkResult.Failure -> when {
                r.error.isLikelyDuplicate() -> _state.update {
                    it.copy(
                        isLoading = false,
                        networkError = "${r.error.displayMessage()}\n→ email, pseudo ou numéro peut-être déjà utilisé ?",
                        retryAction = null
                    )
                }
                r.error is ApiError.Server && attempt < MAX_REGISTER_RETRY -> {
                    // Cold-start Render.com (503) ou erreur temporaire : on réessaie.
                    delay(registerRetryDelay(attempt))
                    doRegister(s, attempt + 1)
                }
                else -> handleAuthError(r.error, RetryAction.SubmitForm)
            }
        }
    }

    private fun registerRetryDelay(attempt: Int): Long = when (attempt) {
        0 -> 8_000L
        else -> 20_000L
    }

    /* ---------- Google + mot de passe oublié ---------- */

    /** Connexion via Google (Credential Manager → /auth/google). */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, networkError = null) }
            when (val r = repository.loginWithGoogle(context)) {
                is NetworkResult.Success ->
                    _state.update { it.copy(step = AuthStep.Success, isLoading = false) }
                is NetworkResult.Failure ->
                    _state.update { it.copy(isLoading = false, networkError = r.error.userMessage) }
            }
        }
    }

    /** Demande l'email de réinitialisation du mot de passe. */
    fun forgotPassword(email: String, onDone: (error: String?) -> Unit) {
        viewModelScope.launch {
            when (val r = repository.forgotPassword(email)) {
                is NetworkResult.Success -> onDone(null)
                is NetworkResult.Failure -> onDone(r.error.displayMessage())
            }
        }
    }

    /** Réinitialise le mot de passe avec le code OTP reçu par email. */
    fun resetPassword(email: String, otpCode: String, newPassword: String, onDone: (error: String?) -> Unit) {
        viewModelScope.launch {
            when (val r = repository.resetPassword(email, otpCode, newPassword)) {
                is NetworkResult.Success -> onDone(null)
                is NetworkResult.Failure -> onDone(r.error.displayMessage())
            }
        }
    }

    /** Renvoie un nouveau code OTP par email (relance l'inscription). */
    fun resendOtp() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(otp = List(OTP_LENGTH) { "" }, otpError = null, isLoading = true) }
            when (val r = repository.register(
                s.email, s.username, s.password,
                if (s.phoneProvided) s.phoneE164 else null
            )) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            countdown = RESEND_COUNTDOWN_SEC,
                            networkError = null,
                            infoMessage = "Nouveau code envoyé à ${s.email}"
                        )
                    }
                    startCountdown()
                }
                is NetworkResult.Failure ->
                    _state.update { it.copy(isLoading = false, networkError = r.error.displayMessage()) }
            }
        }
    }

    /**
     * Vérifie le code OTP reçu par email → finalise le compte et ouvre la session (tokens).
     */
    fun verifyOtp() {
        val s = _state.value
        if (!s.otpValid) {
            _state.update { it.copy(otpError = "Code incomplet.") }
            return
        }
        val code = s.otp.joinToString("")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, networkError = null) }
            when (val r = repository.verifyEmail(s.email, code)) {
                is NetworkResult.Success -> {
                    countdownJob?.cancel()
                    _state.update {
                        it.copy(step = AuthStep.Success, isLoading = false, otpError = null, retryAction = null)
                    }
                }
                is NetworkResult.Failure -> {
                    val err = r.error
                    if (err is ApiError.Business && err.httpStatus in 400..499) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                otpError = err.userMessage,
                                otp = List(OTP_LENGTH) { "" }
                            )
                        }
                    } else {
                        handleAuthError(err, RetryAction.VerifyOtp)
                    }
                }
            }
        }
    }

    /** Annule la vérification — le compte n'est créé qu'une fois le code validé. */
    fun skipOtp() {
        countdownJob?.cancel()
        _state.update {
            it.copy(
                step = AuthStep.Welcome,
                otp = List(OTP_LENGTH) { "" },
                otpError = null,
                networkError = null,
                infoMessage = null
            )
        }
    }

    /* ---------- Error UX ---------- */

    private fun handleAuthError(err: ApiError, retry: RetryAction) {
        _state.update {
            it.copy(
                isLoading = false,
                networkError = err.displayMessage(),
                retryAction = if (err.isRetryable()) retry else null
            )
        }
    }

    fun dismissError() = _state.update { it.copy(networkError = null) }

    fun retryLastAction() {
        when (_state.value.retryAction) {
            RetryAction.SubmitForm -> { dismissError(); submitForm() }
            RetryAction.SendOtp -> { dismissError(); resendOtp() }
            RetryAction.VerifyOtp -> { dismissError(); verifyOtp() }
            null -> dismissError()
        }
    }

    /* ---------- Countdown ---------- */

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

    companion object {
        private const val MAX_REGISTER_RETRY = 2
    }
}

private fun ApiError.isRetryable(): Boolean =
    this is ApiError.Network || this is ApiError.Timeout || this is ApiError.Server

/** En debug : détail technique complet. En release : message utilisateur uniquement. */
private fun ApiError.displayMessage(): String =
    if (BuildConfig.DEBUG) debugDetail else userMessage

/**
 * À l'inscription, indique qu'un identifiant est probablement déjà pris :
 *  - 409 Conflict (cas propre, email/pseudo déjà utilisé) ;
 *  - 500 : bug backend connu — un doublon de `phone_number` renvoie un 500 au lieu d'un 409.
 * NB : 503 (cold-start Render) est exclu — c'est une erreur temporaire, pas un conflit.
 */
private fun ApiError.isLikelyDuplicate(): Boolean =
    (this is ApiError.Business && httpStatus == 409) ||
    (this is ApiError.Server && httpStatus == 500)
