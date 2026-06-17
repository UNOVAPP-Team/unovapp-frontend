package com.unovapp.android.data.auth

import android.content.Context
import com.unovapp.android.TokenDataStore
import com.unovapp.android.data.network.ApiError
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AuthRepository {
    /** Démarre l'inscription : le backend envoie un code OTP par email. Pas de tokens encore. */
    suspend fun register(
        email: String,
        username: String,
        password: String,
        phoneNumber: String?
    ): NetworkResult<Unit>

    /** Vérifie le code OTP reçu par email → finalise le compte et ouvre la session. */
    suspend fun verifyEmail(email: String, otpCode: String): NetworkResult<AuthSession>

    suspend fun login(email: String, password: String): NetworkResult<AuthSession>

    /** Connexion Google (Credential Manager → /auth/google). */
    suspend fun loginWithGoogle(context: Context): NetworkResult<AuthSession>

    /** Demande un email de réinitialisation de mot de passe. */
    suspend fun forgotPassword(email: String): NetworkResult<Unit>

    /** Réinitialise le mot de passe avec le token reçu par email. */
    suspend fun resetPassword(token: String, newPassword: String): NetworkResult<Unit>

    suspend fun sendOtp(phoneNumber: String): NetworkResult<Unit>

    suspend fun verifyOtp(phoneNumber: String, code: String): NetworkResult<Unit>

    suspend fun fetchMe(): NetworkResult<MeResponse>

    /** Efface la session locale sans appeler le backend (utilisé après register → login). */
    suspend fun clearSession()

    suspend fun logout()

    fun isAuthenticated(): Flow<Boolean>
}

/**
 * Implémentation réelle — câblée sur `unovapp-auth.onrender.com`.
 *
 * Flow attendu par le backend NestJS :
 *  1. `register(email, username, password, phone?)` → tokens.
 *  2. `sendOtp(phone)` (public) puis `verifyOtp(phone, code)` (bearer) pour valider le numéro.
 *  3. `login(email, password)` pour les sessions ultérieures.
 *  4. `fetchMe()` après chaque login/register pour récupérer `userId` (à stocker pour le service User).
 *
 * Le username / displayName / avatar viendront du service User — pas câblé ici.
 */
class AuthRepositoryImpl(
    private val api: AuthApi,
    private val tokenStore: TokenDataStore,
    private val googleSignInHelper: GoogleSignInHelper
) : AuthRepository {

    override suspend fun register(
        email: String,
        username: String,
        password: String,
        phoneNumber: String?
    ): NetworkResult<Unit> = safeCall {
        // Le backend envoie un code OTP par email. Aucun token à ce stade.
        api.register(
            RegisterRequest(
                email = email,
                username = username,
                password = password,
                phoneNumber = phoneNumber
            )
        )
        Unit
    }

    override suspend fun verifyEmail(email: String, otpCode: String): NetworkResult<AuthSession> =
        safeCall {
            val tokens = api.verifyEmail(VerifyEmailRequest(email = email, otpCode = otpCode))
            tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken)
            val me = api.me()
            tokenStore.saveSession(tokens.accessToken, tokens.refreshToken, me.userId)
            tokens.toSessionWithMe(me)
        }

    override suspend fun login(email: String, password: String): NetworkResult<AuthSession> =
        safeCall {
            val tokens = api.login(LoginRequest(email = email, password = password))
            tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken)
            val me = api.me()
            tokenStore.saveSession(tokens.accessToken, tokens.refreshToken, me.userId)
            tokens.toSessionWithMe(me)
        }

    override suspend fun loginWithGoogle(context: Context): NetworkResult<AuthSession> {
        return when (val idr = googleSignInHelper.requestIdToken(context)) {
            is NetworkResult.Failure -> NetworkResult.Failure(idr.error)
            is NetworkResult.Success -> safeCall {
                val tokens = api.google(GoogleSignInRequest(idToken = idr.data))
                tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken)
                val me = api.me()
                tokenStore.saveSession(tokens.accessToken, tokens.refreshToken, me.userId)
                tokens.toSessionWithMe(me)
            }
        }
    }

    override suspend fun forgotPassword(email: String): NetworkResult<Unit> = safeCall {
        api.forgotPassword(ForgotPasswordRequest(email = email))
        Unit
    }

    override suspend fun resetPassword(token: String, newPassword: String): NetworkResult<Unit> = safeCall {
        api.resetPassword(ResetPasswordRequest(token = token, newPassword = newPassword))
        Unit
    }

    override suspend fun sendOtp(phoneNumber: String): NetworkResult<Unit> = safeCall {
        api.sendOtp(SendOtpRequest(phoneNumber = phoneNumber))
        Unit
    }

    override suspend fun verifyOtp(phoneNumber: String, code: String): NetworkResult<Unit> =
        safeCall {
            api.verifyOtp(VerifyOtpRequest(phoneNumber = phoneNumber, code = code))
            Unit
        }

    override suspend fun fetchMe(): NetworkResult<MeResponse> = safeCall { api.me() }

    override suspend fun clearSession() {
        tokenStore.clear()
    }

    override suspend fun logout() {
        // Best-effort : on tente l'appel mais on nettoie le store quoi qu'il arrive.
        runCatching { api.logout() }
        tokenStore.clear()
    }

    override fun isAuthenticated(): Flow<Boolean> =
        tokenStore.getAccessToken().map { it != null }
}

/**
 * Stub local — actif uniquement si `BuildConfig.USE_STUB_AUTH = true` (off par défaut depuis
 * que le backend NestJS est en ligne). Conservé pour le dev offline ou les tests UI.
 *
 * `code` accepté en démo : `123456` (6 chiffres, comme le backend réel).
 */
class AuthRepositoryStub(
    private val tokenStore: TokenDataStore
) : AuthRepository {

    override suspend fun register(
        email: String,
        username: String,
        password: String,
        phoneNumber: String?
    ): NetworkResult<Unit> {
        delay(800)
        return NetworkResult.Success(Unit)
    }

    override suspend fun verifyEmail(email: String, otpCode: String): NetworkResult<AuthSession> {
        delay(700)
        return if (otpCode == "123456") {
            val session = fakeSession(userIdFrom = email)
            tokenStore.saveSession(session.accessToken, session.refreshToken, session.userId)
            NetworkResult.Success(session)
        } else {
            NetworkResult.Failure(
                ApiError.Business(
                    code = "OTP_INVALID",
                    userMessage = "Code incorrect ou expiré.",
                    httpStatus = 400
                )
            )
        }
    }

    override suspend fun login(email: String, password: String): NetworkResult<AuthSession> {
        delay(700)
        if (password.length < 8) {
            return NetworkResult.Failure(
                ApiError.Business(
                    code = "LOGIN_INVALID",
                    userMessage = "Identifiants incorrects.",
                    httpStatus = 401
                )
            )
        }
        val session = fakeSession(userIdFrom = email)
        tokenStore.saveSession(session.accessToken, session.refreshToken, session.userId)
        return NetworkResult.Success(session)
    }

    override suspend fun loginWithGoogle(context: Context): NetworkResult<AuthSession> {
        delay(400)
        return NetworkResult.Failure(
            ApiError.Business(code = "GOOGLE_STUB", userMessage = "Google indisponible en mode démo.", httpStatus = 0)
        )
    }

    override suspend fun forgotPassword(email: String): NetworkResult<Unit> {
        delay(500)
        return NetworkResult.Success(Unit)
    }

    override suspend fun resetPassword(token: String, newPassword: String): NetworkResult<Unit> {
        delay(500)
        return NetworkResult.Success(Unit)
    }

    override suspend fun sendOtp(phoneNumber: String): NetworkResult<Unit> {
        delay(600)
        return NetworkResult.Success(Unit)
    }

    override suspend fun verifyOtp(phoneNumber: String, code: String): NetworkResult<Unit> {
        delay(700)
        return if (code == "123456") {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Failure(
                ApiError.Business(
                    code = "OTP_INVALID",
                    userMessage = "Code incorrect. Vérifie et réessaie.",
                    httpStatus = 401
                )
            )
        }
    }

    override suspend fun fetchMe(): NetworkResult<MeResponse> = NetworkResult.Success(
        MeResponse(userId = "stub-user", email = "stub@example.com")
    )

    override suspend fun clearSession() {
        tokenStore.clear()
    }

    override suspend fun logout() {
        tokenStore.clear()
    }

    override fun isAuthenticated(): Flow<Boolean> =
        tokenStore.getAccessToken().map { it != null }

    private fun fakeSession(userIdFrom: String) = AuthSession(
        accessToken = "stub-jwt-${System.currentTimeMillis()}",
        refreshToken = "stub-refresh-${System.currentTimeMillis()}",
        userId = "stub-${userIdFrom.hashCode()}",
        email = userIdFrom
    )
}
