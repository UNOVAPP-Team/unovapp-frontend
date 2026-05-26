package com.unovapp.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Deprecated("Use UnovColors.Accent", ReplaceWith("UnovColors.Accent"))
val OrangeUnovApp = UnovColors.Accent

private val Display = FontFamily.SansSerif

private val DarkColorScheme = darkColorScheme(
    primary = UnovColors.Accent,
    onPrimary = Color(0xFF0D0D0D),
    background = UnovColors.BgBase,
    onBackground = UnovColors.Text,
    surface = UnovColors.Surface,
    onSurface = UnovColors.Text,
    surfaceVariant = UnovColors.SurfaceAlt,
    onSurfaceVariant = UnovColors.TextDim,
    outline = UnovColors.Line,
    error = UnovColors.Danger
)

private val UnovTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.2).sp,
        color = UnovColors.Text
    ),
    displayMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.9).sp,
        color = UnovColors.Text
    ),
    displaySmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.6).sp,
        color = UnovColors.Text
    ),
    titleMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
        color = UnovColors.Text
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = UnovColors.TextDim
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = UnovColors.TextDim
    ),
    labelSmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.6.sp,
        color = UnovColors.TextMute
    )
)

@Composable
fun UnovAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = UnovTypography,
        content = content
    )
}
