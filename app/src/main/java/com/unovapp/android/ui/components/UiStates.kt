package com.unovapp.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors

/**
 * Bloc "squelette" animé (shimmer) — placeholder de chargement réutilisable.
 * Utiliser avec une taille fixe (height/width/fillMaxWidth) pour dessiner la forme du contenu.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(UnovColors.Surface, UnovColors.SurfaceAlt, UnovColors.Surface),
        start = Offset(x - 300f, 0f),
        end = Offset(x, 0f)
    )
    Box(modifier = modifier.clip(shape).background(brush))
}

/** État d'erreur centré avec bouton "Réessayer". */
@Composable
fun ErrorRetry(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = UnovColors.TextMute,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = message,
            color = UnovColors.TextDim,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(UnovColors.Accent)
                .clickable(onClick = onRetry)
                .padding(horizontal = 26.dp, vertical = 11.dp)
        ) {
            Text(
                text = "Réessayer",
                color = Color(0xFF0D0D0D),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** État vide générique (listes sans contenu). */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = UnovColors.TextMute,
            modifier = Modifier.size(40.dp)
        )
        Text(title, color = UnovColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = UnovColors.TextMute, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
