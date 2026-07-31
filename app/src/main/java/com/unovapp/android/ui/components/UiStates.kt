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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.EmberCascade
import com.unovapp.android.ui.theme.EmberMotion
import com.unovapp.android.ui.theme.LocalReducedMotion
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.emberEnter
import com.unovapp.android.ui.theme.pressable

/**
 * Bloc "squelette" animé (shimmer) — placeholder de chargement réutilisable.
 * Utiliser avec une taille fixe (height/width/fillMaxWidth) pour dessiner la forme du contenu.
 * Le shimmer utilise une teinte dorée premium UNOVAPP (au lieu du gris standard).
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    gold: Boolean = false
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
    val colors = if (gold) {
        listOf(UnovColors.Surface, UnovColors.Accent.copy(alpha = 0.10f), UnovColors.Surface)
    } else {
        listOf(UnovColors.Surface, UnovColors.SurfaceAlt, UnovColors.Surface)
    }
    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(x - 300f, 0f),
        end = Offset(x, 0f)
    )
    Box(modifier = modifier.clip(shape).background(brush))
}

/**
 * **La braise qui couve** — l'indicateur de chargement d'UNOVAPP.
 *
 * Remplace tout `CircularProgressIndicator` : la grammaire de mouvement interdit
 * les spinners rotatifs (§3.11 « Aucun spinner rotatif n'existe dans UNOVAPP »).
 * Ici, un point de braise qui respire en opacité — il couve, il ne tourne pas.
 * Statique en reduced-motion.
 */
@Composable
fun BraiseLoader(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 22.dp,
    color: Color = UnovColors.Accent,
    /**
     * Progression réelle 0..1 (upload, traitement). Quand elle est fournie, on trace
     * un anneau qui se remplit — c'est une donnée, pas une attente indéterminée ; il
     * ne tourne jamais et ne revient jamais visuellement à zéro (promesse de l'upload
     * reprenable, §Écran 07). `null` = la braise couve.
     */
    progress: Float? = null
) {
    val reduced = LocalReducedMotion.current

    if (progress != null) {
        val p by androidx.compose.animation.core.animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(EmberMotion.DurSlow, easing = EmberMotion.EaseOut),
            label = "braiseProgress"
        )
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size).then(modifier)) {
            val stroke = this.size.minDimension * 0.11f
            drawCircle(
                color = color.copy(alpha = 0.18f),
                radius = (this.size.minDimension - stroke) / 2f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * p, useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round
                ),
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(this.size.width - stroke, this.size.height - stroke)
            )
        }
        return
    }

    val alpha = if (reduced) {
        0.45f
    } else {
        val t = rememberInfiniteTransition(label = "braiseLoader")
        val v by t.animateFloat(
            initialValue = 0.28f,
            targetValue = 0.62f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = EmberMotion.EaseBreath),
                repeatMode = RepeatMode.Reverse
            ),
            label = "braiseLoaderAlpha"
        )
        v
    }
    // La taille par défaut est posée d'abord : un `modifier` entrant qui porte
    // sa propre taille l'emporte, et les enfants s'y adaptent (fillMaxSize).
    Box(modifier = Modifier.size(size).then(modifier), contentAlignment = Alignment.Center) {
        // Halo diffus qui couve.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(999.dp))
                .background(color.copy(alpha = alpha * 0.5f))
        )
        // Cœur de braise.
        Box(
            modifier = Modifier
                .fillMaxSize(0.42f)
                .clip(RoundedCornerShape(999.dp))
                .background(color.copy(alpha = (alpha + 0.35f).coerceAtMost(1f)))
        )
    }
}

/** État d'erreur centré avec bouton "Réessayer". */
@Composable
fun ErrorRetry(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Cascade d'entrée : l'icône, puis le message, puis l'action (§2.4).
    EmberCascade(step = 60) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = UnovColors.TextMute,
                modifier = Modifier.size(40.dp).emberEnter(dyFrom = 8.dp, scaleFrom = 0.9f)
            )
            Text(
                text = message,
                color = UnovColors.TextDim,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.emberEnter(dyFrom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .emberEnter()
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovColors.Accent)
                    .pressable(onClick = onRetry)
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
}

/** État vide générique (listes sans contenu). */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox
) {
    EmberCascade(step = 60) {
        Column(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = UnovColors.TextMute,
                modifier = Modifier.size(40.dp).emberEnter(dyFrom = 8.dp, scaleFrom = 0.9f)
            )
            Text(title, color = UnovColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.emberEnter(dyFrom = 8.dp))
            Text(subtitle, color = UnovColors.TextMute, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.emberEnter(dyFrom = 8.dp))
        }
    }
}
