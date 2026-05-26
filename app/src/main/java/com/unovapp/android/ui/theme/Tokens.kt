package com.unovapp.android.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

object UnovColors {
    val BgBase = Color(0xFF050505)
    val BgRaised = Color(0xFF0A0A0A)
    val Surface = Color(0xFF141414)
    val SurfaceAlt = Color(0xFF1A1A1A)

    val Line = Color(0xFF1F1F1F)
    val LineStrong = Color(0xFF2A2A2A)

    val Text = Color(0xFFFFFFFF)
    val TextDim = Color(0xFFB8B8B8)
    val TextMute = Color(0xFF6E6E6E)

    val Accent = Color(0xFFFFD700)
    val AccentDeep = Color(0xFFB8860B)
    val AccentGlow = Color(0x40FFD700)
    val AccentGlowStrong = Color(0x80FFD700)

    val Mtn = Color(0xFFFFCC08)
    val Moov = Color(0xFF0066B3)

    val Danger = Color(0xFFFF5252)
}

object UnovGradients {
    val Gold = Brush.linearGradient(
        colors = listOf(UnovColors.Accent, UnovColors.AccentDeep)
    )

    fun videoBg(index: Int): Brush {
        val palettes = listOf(
            listOf(Color(0xFF7B2D5C), Color(0xFFE5722D)),
            listOf(Color(0xFF1A4D8C), Color(0xFF6B2D8C)),
            listOf(Color(0xFF8C3D2D), Color(0xFFC9A227)),
            listOf(Color(0xFF2D5C3D), Color(0xFF1A2D4D)),
            listOf(Color(0xFFB85C2D), Color(0xFF4D1A2D)),
            listOf(Color(0xFF3D2D5C), Color(0xFF8C2D4D))
        )
        val p = palettes[index % palettes.size]
        return Brush.linearGradient(colors = p)
    }
}

object UnovType {
    val DisplayLetterSpacing = (-0.03).sp
    val EyebrowLetterSpacing = 0.24.sp
}
