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

// Famille orange — nuances distinctes par utilisateur (déterministe via idx),
// toutes dans l'identité noir + orange de l'app.
private val AvatarPalette = listOf(
    listOf(Color(0xFFFF6A00), Color(0xFFFF944D)),  // orange vif
    listOf(Color(0xFFE5722D), Color(0xFFFFC08A)),  // orange chaud
    listOf(Color(0xFFE55F00), Color(0xFFFF944D)),  // brûlé
    listOf(Color(0xFFFF944D), Color(0xFFFFD2A6)),  // ambre clair
    listOf(Color(0xFFE55F00), Color(0xFFE55F00)),  // profond
    listOf(Color(0xFFFF6A00), Color(0xFFFF944D)),  // vif
    listOf(Color(0xFFB85C2D), Color(0xFFFFC08A))   // terracotta
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
