package com.unovapp.android.data.mock

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  CONTENU FICTIF — À SUPPRIMER QUAND LE BACKEND EXPOSE CES DONNÉES        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * Tout ce que la maquette montre mais que l'API ne fournit pas encore est
 * rassemblé ICI, et nulle part ailleurs. Objectif : le jour où les endpoints
 * arrivent, on supprime ce fichier et le compilateur pointe lui-même chaque
 * écran à rebrancher — au lieu de laisser des données inventées se disperser
 * dans l'UI où plus personne ne les retrouve.
 *
 * Règle de sécurité tenue partout ailleurs dans l'app : **on n'invente jamais
 * de donnée**. L'exception est assumée et cadrée ici pour valider le design et
 * donner une cible concrète à l'équipe backend (cf. A_ENVOYER_PAR_LE_BACKEND.md).
 *
 * ⚠️ Aucun de ces montants n'est un vrai solde et aucun de ces messages n'a été
 * envoyé par un vrai utilisateur. Les écrans qui les consomment affichent un
 * bandeau « Démo » pour que ce soit visible à l'écran, pas seulement dans le code.
 */

/* ═══════════════ Cercles (écran 08) — messagerie entre fidèles ═══════════════ */

data class MockCercle(
    val initial: String,
    val name: String,
    /** Braises échangées — la « chaleur » du lien. */
    val braises: Int,
    val lastMessage: String,
    val time: String,
    val unread: Boolean,
    /** Teinte de l'anneau, propre au contact. */
    val ringHex: Long
)

val MOCK_CERCLE_PROCHE = listOf(
    MockCercle(
        initial = "E", name = "Eloge", braises = 12,
        lastMessage = "t'a envoyé une vidéo · « regarde ça 😄 »",
        time = "", unread = true, ringHex = 0xFF7C6BD9
    ),
    MockCercle(
        initial = "C", name = "@ciscomignon", braises = 4,
        lastMessage = "Merci pour l'étincelle 🙏",
        time = "hier", unread = false, ringHex = 0xFFFF4D00
    )
)

/** Le « sas » : des gens hors du cercle qui demandent à parler. */
data class MockNouvellesVoix(val initials: List<String>, val count: Int)

val MOCK_NOUVELLES_VOIX = MockNouvellesVoix(initials = listOf("H", "R"), count = 2)

/* ═══════════════ La Forge (écran 13) — marques ↔ créateurs ═══════════════ */

data class MockCommande(
    val initial: String,
    val brand: String,
    val sector: String,
    val brief: String,
    /** Rémunération par créateur, en FCFA. */
    val budgetFcfa: Int,
    val placesTaken: Int,
    val placesTotal: Int,
    val daysLeft: Int,
    /** Score de correspondance calculé par l'IA (0–100). */
    val matchPercent: Int,
    val matchReason: String,
    val brandHex: Long,
    val open: Boolean
)

val MOCK_COMMANDE_PHARE = MockCommande(
    initial = "M", brand = "Maison Kossi", sector = "Cosmétiques · Cotonou",
    brief = "3 vidéos unboxing de nos soins · 60 s",
    budgetFcfa = 15_000, placesTaken = 2, placesTotal = 3, daysLeft = 5,
    matchPercent = 92, matchReason = "audience beauté & lifestyle",
    brandHex = 0xFFE0457B, open = true
)

data class MockCommandeCompacte(
    val initial: String,
    val brand: String,
    val detail: String,
    val budgetFcfa: Int,
    val placesTaken: Int,
    val placesTotal: Int,
    val brandHex: Long
)

val MOCK_COMMANDES_COMPACTES = listOf(
    MockCommandeCompacte("B", "Brasserie La Béninoise", "5 créateurs · skit 15 s", 8_000, 1, 5, 0xFFD98324),
    MockCommandeCompacte("T", "Tech Hub Bénin", "3 créateurs · démo 90 s", 25_000, 0, 3, 0xFF3B6BE1)
)

/* ═══════════════ Univers (écran 06) — Empreinte et Séries ═══════════════ */

/** Un thème récurrent des vidéos du créateur. */
data class MockTheme(val label: String, val videos: Int, val note: String?, val tintHex: Long)

val MOCK_EMPREINTE = listOf(
    MockTheme("Danse", 14, "la plus chaude", 0xFFFF4D00),
    MockTheme("Cuisine", 6, null, 0xFFE8B84B),
    MockTheme("Foi", 4, null, 0xFF8FCE84)
)

/** L'observation que l'IA tire de l'Empreinte. */
const val MOCK_EMPREINTE_INSIGHT =
    "Ton humour fait rester les gens 2× plus longtemps — et si tu en faisais une série ?"

data class MockSerie(val title: String, val badge: String?, val note: String, val draft: Boolean)

val MOCK_SERIES = listOf(
    MockSerie("Danse Afro", "S1 · 6 ÉP.", "suivie par 3 fidèles", draft = false),
    MockSerie("Rires de rue", null, "brouillon · 2 épisodes", draft = true)
)

/* ═══════════════ Studio (écran 07) — le plan proposé par l'IA ═══════════════ */

const val MOCK_REALISATEUR_IDEA = "« Un challenge de danse au coucher du soleil »"
const val MOCK_REALISATEUR_PLAN =
    "Plan proposé : accroche 3 s → démo 8 s → appel au défi 4 s. " +
    "La lumière est idéale dans 40 min."

data class MockScene(val index: Int, val label: String, val seconds: Int, val shot: String)

val MOCK_SCENES = listOf(
    MockScene(1, "ACCROCHE", 3, "plan serré"),
    MockScene(2, "DÉMO", 8, "pied"),
    MockScene(3, "APPEL", 4, "face")
)

/* ═══════════════ Flux (écran 02) — le panneau « CE MOMENT » ═══════════════ */

data class MockMoment(val label: String, val sub: String, val actionable: Boolean)

val MOCK_CE_MOMENT = listOf(
    MockMoment("DJ Bossou — Nuit d'Or", "Garder ce son", actionable = true),
    MockMoment("Cotonou, Fidjrossè", "12 vidéos près de toi", actionable = false),
    MockMoment("#ZangbetoChallenge", "Rejoindre", actionable = true)
)

/* ═══════════════ Réglages (écran 09) — les permissions du Rythme ═══════════════ */

data class MockRythme(val title: String, val sub: String, val defaultOn: Boolean, val advised: Boolean)

val MOCK_RYTHME = listOf(
    MockRythme("Adapter le flux à mes heures", "Elle regarde ce que tu regardes, pas ce que tu déclares", true, false),
    MockRythme("Suggérer dans la boussole", "L'Orbe devine ton prochain écran", true, false),
    MockRythme("Pause après 45 min de flux", "Elle te propose de souffler", false, true)
)
