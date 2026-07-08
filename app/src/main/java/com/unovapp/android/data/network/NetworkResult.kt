package com.unovapp.android.data.network

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Wrapper neutre pour le résultat d'un appel réseau.
 * Permet aux ViewModels de pattern-matcher Success / Failure sans gérer try/catch.
 */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Failure(val error: ApiError) : NetworkResult<Nothing>
}

/**
 * Couvre les deux formes connues :
 *  - Contrat UNOVAPP : `{ code, message, details }`.
 *  - NestJS par défaut : `{ statusCode, error, message }` où `message` peut être un string
 *    ou un array de strings (cas validation `class-validator`).
 */
private data class ErrorPayload(
    val code: String?,
    val error: String?,
    val message: JsonElement?
)

private fun JsonElement?.flattenMessage(): String? = when {
    this == null || isJsonNull -> null
    isJsonPrimitive -> asString
    isJsonArray -> asJsonArray.joinToString(" · ") { it.asString }
    else -> toString()
}

/**
 * Exécute un bloc Retrofit suspendu et normalise les exceptions en [ApiError].
 * Le retry réseau est géré au niveau OkHttp ([RetryInterceptor]), pas ici.
 */
suspend inline fun <T> safeCall(crossinline block: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (e: SocketTimeoutException) {
        NetworkResult.Failure(ApiError.Timeout())
    } catch (e: IOException) {
        NetworkResult.Failure(ApiError.Network())
    } catch (e: HttpException) {
        NetworkResult.Failure(parseHttpError(e))
    } catch (e: JsonSyntaxException) {
        NetworkResult.Failure(ApiError.Unknown(cause = e))
    } catch (e: Exception) {
        NetworkResult.Failure(ApiError.Unknown(cause = e))
    }
}

@PublishedApi
internal fun parseHttpError(e: HttpException): ApiError {
    val code = e.code()
    val rawBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val payload = rawBody?.let {
        runCatching { Gson().fromJson(it, ErrorPayload::class.java) }.getOrNull()
    }
    val msg = payload?.message.flattenMessage()
    val errCode = payload?.code ?: payload?.error ?: "HTTP_$code"

    return when (code) {
        401 -> ApiError.Unauthorized(msg ?: "Session expirée. Reconnecte-toi.")
        // 429 = rate-limit serveur : condition temporaire, à retraiter comme une erreur
        // serveur pour déclencher le retry avec backoff dans les ViewModels.
        429 -> ApiError.Server(
            userMessage = "Trop de requêtes. Réessaie dans quelques instants.",
            httpStatus = 429
        )
        in 400..499 -> ApiError.Business(
            code = errCode,
            userMessage = msg ?: "Requête invalide.",
            httpStatus = code
        )
        in 500..599 -> ApiError.Server(msg ?: "Service indisponible.", httpStatus = code)
        else -> ApiError.Unknown(msg ?: "Erreur inconnue (HTTP $code).")
    }
}
