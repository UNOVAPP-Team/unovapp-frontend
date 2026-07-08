package com.unovapp.android.data.auth

import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall

/**
 * Gestion du compte (Sprint 2) : changement d'email en 2 temps + sessions multi-appareils.
 * Distinct d'[AuthRepository] (flux de connexion) pour ne pas alourdir le stub d'auth.
 */
interface AccountRepository {
    suspend fun changeEmail(newEmail: String): NetworkResult<Unit>
    suspend fun verifyEmailChange(otpCode: String): NetworkResult<String>   // nouvel email
    suspend fun sessions(): NetworkResult<List<SessionDto>>
    suspend fun revokeSession(id: String): NetworkResult<Unit>
}

class AccountRepositoryImpl(private val api: AuthApi) : AccountRepository {

    override suspend fun changeEmail(newEmail: String): NetworkResult<Unit> =
        safeCall { api.changeEmail(ChangeEmailRequest(newEmail)); Unit }

    override suspend fun verifyEmailChange(otpCode: String): NetworkResult<String> =
        safeCall { api.verifyEmailChange(OtpCodeRequest(otpCode)).email }

    override suspend fun sessions(): NetworkResult<List<SessionDto>> =
        safeCall { api.sessions() }

    override suspend fun revokeSession(id: String): NetworkResult<Unit> =
        safeCall { api.revokeSession(id); Unit }
}
