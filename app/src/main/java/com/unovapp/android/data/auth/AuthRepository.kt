package com.unovapp.android.data.auth

import com.unovapp.android.TokenDataStore
import com.unovapp.android.data.network.ApiError
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AuthRepository {
    suspend fun register(
        email: String,
        username: String,
        password: String,
        phoneNumber: String?
    ): NetworkResult<AuthSession>

    suspend fun login(email: String, password: String): NetworkResult<AuthSession>

    suspend fun sendOtp(phoneNumber: String): NetworkResult<Unit>

    suspend fun verifyOtp(phoneNumber: String, code: String): NetworkResult<Unit>

    /** (Re)envoie l'email de vérification au compte courant (Bearer requis). */
    suspend fun sendEmailVerification(): NetworkResult<Unit>

    /** Valide l'email via le token reçu (lien email / deep link). */
    suspend fun verifyEmail(token: String): NetworkResult<Unit>

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
    private val tokenStore: TokenDataStore
) : AuthRepository {

    override suspend fun register(
        email: String,
        username: String,
        password: String,
        phoneNumber: String?
    ): NetworkResult<AuthSession> = safeCall {
        val tokens = api.register(
            RegisterRequest(
                email = email,
                username = username,
                password = password,
                phoneNumber = phoneNumber
            )
        )
        // Sauvegarder le token AVANT le /me — l'interceptor en a besoin pour l'appel suivant.
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

    override suspend fun sendOtp(phoneNumber: String): NetworkResult<Unit> = safeCall {
        api.sendOtp(SendOtpRequest(phoneNumber = phoneNumber))
        Unit
    }

    override suspend fun verifyOtp(phoneNumber: String, code: String): NetworkResult<Unit> =
        safeCall {
            api.verifyOtp(VerifyOtpRequest(phoneNumber = phoneNumber, code = code))
            Unit
        }

    override suspend fun sendEmailVerification(): NetworkResult<Unit> = safeCall {
        api.sendEmailVerification().close()
        Unit
    }

    override suspend fun verifyEmail(token: String): NetworkResult<Unit> = safeCall {
        api.verifyEmail(token).close()
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
    ): NetworkResult<AuthSession> {
        delay(800)
        val session = fakeSession(userIdFrom = email)
        tokenStore.saveSession(session.accessToken, session.refreshToken, session.userId)
        return NetworkResult.Success(session)
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

    override suspend fun sendEmailVerification(): NetworkResult<Unit> {
        delay(400)
        return NetworkResult.Success(Unit)
    }

    override suspend fun verifyEmail(token: String): NetworkResult<Unit> {
        delay(400)
        return NetworkResult.Success(Unit)
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
