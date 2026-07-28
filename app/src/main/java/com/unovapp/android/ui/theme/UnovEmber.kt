package com.unovapp.android.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Système de design « La braise, pas le bruit » — la nouvelle signature UNOVAPP.
 *
 * Palette du feu (source : documentation produit §3). Introduite en parallèle de [UnovColors]
 * (l'ancienne charte orange) le temps de migrer les écrans un à un. Tout ce qui est migré vers
 * la nouvelle maquette utilise EMBER ; le reste garde l'ancienne charte.
 *
 * Règle absolue : la **lagune** [Lagune] est réservée à l'IA. Aucun élément non-IA ne l'emploie.
 */
object Ember {
    // Fonds
    val Bg = Color(0xFF0B0704)              // fond de toutes les surfaces
    val Surface = Color(0xFF100A05)         // cartes, panneaux
    val SurfaceAlt = Color(0xFF140C06)
    val SurfaceWarm = Color(0xFF160D06)     // cartes accentuées
    val SurfaceWarm2 = Color(0xFF1B1008)
    val SurfaceWarm3 = Color(0xFF33200E)

    // Braise (action principale, valeur, chaleur)
    val Braise = Color(0xFFFF4D00)
    val BraiseClair = Color(0xFFFF9D66)     // labels, accents secondaires
    val BraisePale = Color(0xFFFFD9B0)      // texte sur braise
    val BraisePale2 = Color(0xFFFFE9CF)     // la Lueur (réaction gratuite)

    val Gold = Color(0xFFE8B84B)            // premium, Brasier camp B, Forge
    val Lagune = Color(0xFF6FD9C4)          // RÉSERVÉ à l'IA
    val Positif = Color(0xFF8FCE84)         // gains, validations

    // Texte
    val Text = Color(0xFFF2E9DC)            // texte principal
    val TextDim = Color(0x8CF2E9DC)         // .55 — hiérarchie secondaire
    val TextMute = Color(0x59F2E9DC)        // .35 — tertiaire

    val Line = Color(0x1FF2E9DC)            // filets discrets

    /** Dégradé de feu — bouton Braise, anneaux de chaleur. */
    val FireGradient = Brush.verticalGradient(listOf(BraiseClair, Braise, Color(0xFFCC3D00)))

    /** Anneau de chaleur (avatar, contact) — conique braise. */
    val HeatRing = Brush.sweepGradient(listOf(Braise, BraiseClair, Color(0xFF8B2E00), Braise))

    /** Lueur radiale posée derrière les éléments chauds (la braise « éclaire » l'écran). */
    fun radialGlow(alpha: Float = 0.22f) = Brush.radialGradient(
        listOf(Braise.copy(alpha = alpha), Color.Transparent)
    )
}

/** Géométrie de la maquette (source : documentation produit §3). */
object EmberDim {
    val ScreenRadius = 34.dp
    val CardRadius = 18.dp
    val Touch = 44.dp        // cible tactile minimale
    val Orbe = 58.dp
    val RecordButton = 84.dp
    val ReactionArc = 110.dp
}
