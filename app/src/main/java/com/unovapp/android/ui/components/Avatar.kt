package com.unovapp.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AvatarPalette = listOf(
    listOf(Color(0xFF7B2D5C), Color(0xFFC9527A)),
    listOf(Color(0xFFE5722D), Color(0xFFFFB46B)),
    listOf(Color(0xFF1A4D8C), Color(0xFF4FA2D4)),
    listOf(Color(0xFFC9A227), Color(0xFFEFD86E)),
    listOf(Color(0xFF3D2D5C), Color(0xFF7A5DB0)),
    listOf(Color(0xFF8C2D4D), Color(0xFFD45A8B)),
    listOf(Color(0xFF2D5C3D), Color(0xFF5DAE7A))
)

/**
 * Avatar dégradé + initiales. `idx` choisit le palette deterministe pour qu'un
 * même utilisateur ait toujours la même couleur. Plus tard, on swap pour Coil
 * avec l'`avatar_url` depuis l'API.
 */
@Composable
fun Avatar(
    idx: Int,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val palette = AvatarPalette[idx.absoluteMod(AvatarPalette.size)]
    val initials = name
        .split(' ', '.', '_', '-')
        .filter { it.isNotEmpty() }
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifEmpty { "?" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors = palette)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun Int.absoluteMod(n: Int): Int = ((this % n) + n) % n
