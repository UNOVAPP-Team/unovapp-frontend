package com.unovapp.android.ui.challenge

/**
 * Modèles Challenge — **UI uniquement pour l'instant** : le backend n'expose aucun endpoint
 * `challenges` (cf. docs/BACKEND_CHALLENGES.md). Dès que l'API existera, ces modèles seront
 * alimentés par un ChallengeRepository sans changer les écrans.
 */

/** Formulaire de création d'un challenge (écran « Créer un challenge »). */
data class ChallengeForm(
    val name: String = "",
    val description: String = "",
    val coverUri: String? = null,
    val startDateMs: Long? = null,
    val endDateMs: Long? = null,
    val rules: String = "",
    val audience: Audience = Audience.Everyone,
    val minAge: MinAge = MinAge.None,
    val rewardType: RewardType = RewardType.Money,
    val rewardValue: String = "",
    val feesEnabled: Boolean = false,
    val feeAmount: String = ""
) {
    /** Le minimum vital pour lancer : un nom, une description et une période cohérente. */
    val isValid: Boolean
        get() = name.isNotBlank() &&
            description.isNotBlank() &&
            startDateMs != null &&
            endDateMs != null &&
            endDateMs > startDateMs &&
            rules.isNotBlank()
}

enum class Audience(val label: String) {
    Everyone("Tout le monde"),
    Followers("Mes abonnés"),
    Invited("Sur invitation")
}

enum class MinAge(val label: String) {
    None("Aucun"),
    Age13("13 ans"),
    Age16("16 ans"),
    Age18("18 ans")
}

enum class RewardType(val label: String) {
    Money("Argent"),
    Jetons("Jetons UNOVAPP"),
    Gift("Cadeau / lot"),
    Visibility("Visibilité")
}

/** Un challenge affiché dans le carrousel « Mes challenges » du profil. */
data class ChallengeCard(
    val id: String,
    val hashtag: String,
    val participantsFmt: String,
    val coverUrl: String? = null,
    val isActive: Boolean = true
)

/** Limites de saisie — reprises telles quelles de la maquette. */
const val CHALLENGE_DESC_MAX = 200
const val CHALLENGE_RULES_MAX = 300
