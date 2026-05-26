package com.unovapp.android.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.unovapp.android.BuildConfig
import com.unovapp.android.data.network.ApiError
import com.unovapp.android.data.network.NetworkResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Récupère un Google ID Token via l'API Credential Manager (le nouveau remplaçant
 * de GoogleSignInClient, déprécié depuis 2024).
 *
 * Flow :
 *   UI tape "Google" → [requestIdToken] → Bottom sheet système → utilisateur choisit
 *   un compte → on récupère un ID Token JWT → on l'envoie au backend `/auth/google`.
 *
 * Prérequis prod :
 *   - Web Client ID Google OAuth configuré dans `BuildConfig.GOOGLE_WEB_CLIENT_ID`
 *     (depuis console.cloud.google.com → APIs & Services → Credentials → OAuth 2.0
 *     → Web application).
 *   - Empreinte SHA-1 de l'app debug ET release enregistrée sur le projet GCP.
 */
@Singleton
class GoogleSignInHelper @Inject constructor() {

    /**
     * Demande un ID Token Google à l'utilisateur. Doit être appelé depuis un Activity
     * context (Credential Manager affiche un bottom sheet système).
     *
     * @param filterByAuthorizedAccounts false → propose aussi les comptes non encore
     *                                   autorisés (premier login). true → "auto sign-in"
     *                                   silencieux pour les utilisateurs déjà revenus.
     */
    suspend fun requestIdToken(
        context: Context,
        filterByAuthorizedAccounts: Boolean = false
    ): NetworkResult<String> {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.startsWith("REPLACE_ME")) {
            return NetworkResult.Failure(
                ApiError.Business(
                    code = "GOOGLE_NOT_CONFIGURED",
                    userMessage = "Connexion Google bientôt disponible.",
                    httpStatus = 0
                )
            )
        }

        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(filterByAuthorizedAccounts)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context = context, request = request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                NetworkResult.Success(googleCred.idToken)
            } else {
                NetworkResult.Failure(
                    ApiError.Unknown(userMessage = "Type d'identifiant Google inattendu.")
                )
            }
        } catch (e: GetCredentialCancellationException) {
            NetworkResult.Failure(
                ApiError.Business(
                    code = "GOOGLE_CANCELLED",
                    userMessage = "Connexion Google annulée.",
                    httpStatus = 0
                )
            )
        } catch (e: NoCredentialException) {
            NetworkResult.Failure(
                ApiError.Business(
                    code = "GOOGLE_NO_ACCOUNT",
                    userMessage = "Aucun compte Google disponible sur cet appareil.",
                    httpStatus = 0
                )
            )
        } catch (e: GoogleIdTokenParsingException) {
            NetworkResult.Failure(ApiError.Unknown(cause = e))
        } catch (e: GetCredentialException) {
            NetworkResult.Failure(ApiError.Unknown(userMessage = e.message ?: "Échec Google.", cause = e))
        }
    }
}
