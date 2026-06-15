package com.unovapp.android.data.network

/**
 * Modèle d'erreur conforme au contrat API UNOVAPP : `{ code, message, details }`.
 * Toutes les erreurs côté client (réseau, parsing, métier) sont normalisées dans cette sealed.
 */
sealed class ApiError(open val userMessage: String) {

    /**
     * Détail technique pour le diagnostic (type + statut HTTP + message brut backend).
     * Affiché tel quel pendant la phase d'intégration pour repérer vite la cause.
     */
    open val debugDetail: String get() = userMessage

    /** Pas de connectivité ou serveur injoignable. Affichable avec un bouton "Réessayer". */
    data class Network(override val userMessage: String = "Connexion impossible. Vérifie ton réseau.") : ApiError(userMessage) {
        override val debugDetail get() = "RÉSEAU · $userMessage"
    }

    /** Timeout (connexion ou lecture). */
    data class Timeout(override val userMessage: String = "La requête a pris trop de temps.") : ApiError(userMessage) {
        override val debugDetail get() = "TIMEOUT · $userMessage"
    }

    /** 4xx — règle métier rejetée. Le message vient du backend. */
    data class Business(
        val code: String,
        override val userMessage: String,
        val httpStatus: Int
    ) : ApiError(userMessage) {
        override val debugDetail get() = "HTTP $httpStatus · $code · $userMessage"
    }

    /** 401 — token expiré / invalide. Forcer une re-auth. */
    data class Unauthorized(override val userMessage: String = "Session expirée. Reconnecte-toi.") : ApiError(userMessage) {
        override val debugDetail get() = "HTTP 401 · $userMessage"
    }

    /** 5xx — backend en panne, après épuisement des retries. */
    data class Server(
        override val userMessage: String = "Service indisponible. Réessaie dans quelques instants.",
        val httpStatus: Int = 500
    ) : ApiError(userMessage) {
        override val debugDetail get() = "HTTP $httpStatus · $userMessage"
    }

    /** Erreur de parsing / contrat — bug côté client ou backend. */
    data class Unknown(
        override val userMessage: String = "Une erreur inattendue est survenue.",
        val cause: Throwable? = null
    ) : ApiError(userMessage) {
        override val debugDetail get() = "CLIENT · ${cause?.javaClass?.simpleName ?: ""} ${cause?.message ?: userMessage}".trim()
    }
}
