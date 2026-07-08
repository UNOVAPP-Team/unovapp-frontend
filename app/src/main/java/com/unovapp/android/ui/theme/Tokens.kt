package com.unovapp.android.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

object UnovColors {
    // Palette officielle UNOVAPP (cf. charte) — noir + orange, contraste optimisé.
    val BgBase = Color(0xFF0D0D0D)          // Noir principal (fond)
    val BgRaised = Color(0xFF161616)        // Fond légèrement surélevé
    val Surface = Color(0xFF1A1A1A)         // Gris foncé (cartes)
    val SurfaceAlt = Color(0xFF242424)      // Carte légèrement plus claire

    val Line = Color(0xFF262626)
    val LineStrong = Color(0xFF333333)

    val Text = Color(0xFFFFFFFF)            // Blanc (texte principal)
    val TextDim = Color(0xFFE5E5E5)         // Gris clair (texte secondaire)
    val TextMute = Color(0xFF888888)        // Gris moyen (icônes / texte tertiaire)

    // L'orange est l'accent unique de toute l'app.
    val Accent = Color(0xFFFF6A00)          // Orange principal (primaire)
    val AccentLight = Color(0xFFFF944D)     // Orange clair (accent / highlight)
    val AccentDeep = Color(0xFFE55F00)      // Orange profond (base des dégradés)
    val AccentGlow = Color(0x40FF6A00)      // Halo orange diffus
    val AccentGlowStrong = Color(0x80FF6A00)

    val Mtn = Color(0xFFFFCC08)             // jaune marque MTN (paiement — conservé)
    val Moov = Color(0xFF0066B3)            // bleu marque Moov (paiement — conservé)

    val Success = Color(0xFF22C55E)         // Vert succès
    val Danger = Color(0xFFFF5252)
}

object UnovGradients {
    /** Dégradé orange signature (boutons primaires, anneaux, accents) : clair → primaire → profond. */
    val Gold = Brush.linearGradient(
        colors = listOf(UnovColors.AccentLight, UnovColors.Accent, UnovColors.AccentDeep)
    )

    /** Fonds de vignettes vidéo — variations noir → orange (placeholders cohérents). */
    fun videoBg(index: Int): Brush {
        val palettes = listOf(
            listOf(Color(0xFF2A1400), Color(0xFFFF6A00)),
            listOf(Color(0xFF1A0E00), Color(0xFFE55F00)),
            listOf(Color(0xFF2A1400), Color(0xFFFF944D)),
            listOf(Color(0xFF120A00), Color(0xFFFF6A00)),
            listOf(Color(0xFF3A2000), Color(0xFFFF944D)),
            listOf(Color(0xFF200E00), Color(0xFFFF6A00))
        )
        val p = palettes[index % palettes.size]
        return Brush.linearGradient(colors = p)
    }
}

object UnovType {
    val DisplayLetterSpacing = (-0.03).sp
    val EyebrowLetterSpacing = 0.24.sp
}
