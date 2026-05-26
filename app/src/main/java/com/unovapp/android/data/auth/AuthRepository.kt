package com.unovapp.android.data.auth

import com.unovapp.android.TokenDataStore
import com.unovapp.android.data.network.ApiError
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AuthRepository {
    suspend fun sendOtp(phone: String, countryCode: String): NetworkResult<Unit>
    suspend fun verifyOtp(phone: String, code: String): NetworkResult<AuthSession>
    suspend fun signInWithGoogle(idToken: String): NetworkResult<AuthSession>
    suspend fun logout()
    fun isAuthenticated(): Flow<Boolean>
}

/**
 * Implémentation réelle — utilisée dès que le backend NestJS Auth est en ligne
 * (Jour 3, Semaine 1). Activable via `BuildConfig.USE_STUB_AUTH = false`.
 */
class AuthRepositoryImpl(
    private val api: AuthApi,
    private val tokenStore: TokenDataStore
) : AuthRepository {

    override suspend fun sendOtp(phone: String, countryCode: String) = safeCall {
        api.sendOtp(SendOtpRequest(phone = phone, countryCode = countryCode))
        Unit
    }

    override suspend fun verifyOtp(phone: String, code: String) = safeCall {
        val dto = api.verifyOtp(VerifyOtpRequest(phone = phone, code = code))
        tokenStore.saveToken(dto.accessToken)
        dto.toDomain()
    }

    override suspend fun signInWithGoogle(idToken: String) = safeCall {
        val dto = api.signInWithGoogle(GoogleSignInRequest(idToken = idToken))
        tokenStore.saveToken(dto.accessToken)
        dto.toDomain()
    }

    override suspend fun logout() {
        runCatching { api.logout(idempotencyKey = java.util.UUID.randomUUID().toString()) }
        tokenStore.clearToken()
    }

    override fun isAuthenticated(): Flow<Boolean> =
        tokenStore.getToken().map { it != null }
}

/**
 * Stub local — actif tant que le backend n'est pas prêt (`BuildConfig.USE_STUB_AUTH = true`).
 *
 * - `sendOtp` : succès après 800ms.
 * - `verifyOtp` : accepte le code "1234" (matchant le démo auto-fill), sinon 401.
 * - `signInWithGoogle` : succès après 600ms.
 *
 * Permet de tester tout le flow UX (loading, succès, erreurs, retry) sans dépendre du backend.
 */
class AuthRepositoryStub(
    private val tokenStore: TokenDataStore
) : AuthRepository {

    override suspend fun sendOtp(phone: String, countryCode: String): NetworkResult<Unit> {
        delay(800)
        return NetworkResult.Success(Unit)
    }

    override suspend fun verifyOtp(phone: String, code: String): NetworkResult<AuthSession> {
        delay(800)
        return if (code == "1234") {
            val session = stubSession(phone)
            tokenStore.saveToken(session.accessToken)
            NetworkResult.Success(session)
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

    override suspend fun signInWithGoogle(idToken: String): NetworkResult<AuthSession> {
        delay(600)
        val session = stubSession(phone = "google-${idToken.take(8)}")
        tokenStore.saveToken(session.accessToken)
        return NetworkResult.Success(session)
    }

    override suspend fun logout() {
        tokenStore.clearToken()
    }

    override fun isAuthenticated(): Flow<Boolean> =
        tokenStore.getToken().map { it != null }

    private fun stubSession(phone: String) = AuthSession(
        accessToken = "stub-jwt-${System.currentTimeMillis()}",
        refreshToken = "stub-refresh-${System.currentTimeMillis()}",
        expiresInSeconds = 3600,
        userId = "stub-user-${phone.hashCode()}",
        username = "akossi_creator",
        displayName = "Akossi Koffi",
        avatarUrl = null,
        walletBalance = 250
    )
}
