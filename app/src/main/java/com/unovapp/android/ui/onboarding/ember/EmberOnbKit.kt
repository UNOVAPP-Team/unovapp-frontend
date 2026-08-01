package com.unovapp.android.ui.onboarding.ember

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.Ember
import com.unovapp.android.ui.theme.EmberFont
import kotlinx.coroutines.launch

/* ══════════════════════════════════════════════════════════════════════════
 *  Kit de composants de l'onboarding « La braise, pas le bruit ».
 *  Typo : Hanken Grotesk (grotesque géométrique, comme la maquette). On bundle
 *  la fonte variable pour ne PAS dépendre de la police système de l'appareil
 *  (certains Android — Xiaomi/HyperOS — rendent SansSerif en arrondi/manuscrit).
 * ══════════════════════════════════════════════════════════════════════════ */

/** Alias historique — la fonte vit désormais dans le thème ([EmberFont]). */
val OnbFont = EmberFont

@Composable
fun OnbTitle(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        color = Ember.Text,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1).sp,
        fontFamily = OnbFont,
        textAlign = align,
        modifier = modifier
    )
}

@Composable
fun OnbSubtitle(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        color = Ember.TextDim,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = OnbFont,
        textAlign = align,
        modifier = modifier
    )
}

/**
 * Sous-titre avec segments en gras — fidèle aux emphases de la maquette
 * (ex. « Tu laisses une *Lueur*. »). Syntaxe : entoure d'astérisques le
 * segment à mettre en avant ; [emphColor] colore ces segments.
 */
@Composable
fun OnbSubtitleRich(
    text: String,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
    emphColor: Color = Ember.Text
) {
    val annotated = buildAnnotatedString {
        text.split("*").forEachIndexed { i, seg ->
            if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = emphColor)) { append(seg) }
            else append(seg)
        }
    }
    Text(
        text = annotated,
        color = Ember.TextDim,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontFamily = OnbFont,
        textAlign = align,
        modifier = modifier
    )
}

/** Eyebrow — micro-label espacé en capitales (braise, lagune pour l'IA, ou muet). */
@Composable
fun Eyebrow(text: String, lagune: Boolean = false, muted: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = when {
            lagune -> Ember.Lagune
            muted -> Ember.TextMute
            else -> Ember.BraiseClair
        },
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.4.sp,
        fontFamily = OnbFont,
        modifier = modifier
    )
}

/** Bouton primaire — pilule pleine dégradé de feu, texte sombre. */
@Composable
fun EmberPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Boolean = false) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Ember.FireGradient)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = Ember.Text, modifier = Modifier.size(20.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
        }
        Text(text, color = Ember.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
    }
}

/** Bouton fantôme — contour discret. */
@Composable
fun EmberGhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Ember.Line, RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Ember.TextDim, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = OnbFont)
    }
}

/** Bouton lagune (validation d'un réglage IA). */
@Composable
fun EmberLaguneButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Ember.Lagune.copy(alpha = 0.08f))
            .border(1.dp, Ember.Lagune.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Ember.Lagune, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
    }
}

/**
 * Braise incandescente à maintenir : anneau de progression qui se remplit tant qu'on presse ;
 * au relâchement avant la fin, il reflue ; atteint 1 → [onComplete]. Cœur = flamme sur disque
 * dégradé de feu, halo radial diffus. [intensity] pilote l'index ×1/×2/×3 (écran Braise).
 */
@Composable
fun HoldToIgnite(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onIntensity: (Int) -> Unit = {},
    holdMs: Int = 1200
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // Reporte l'intensité ×1/×2/×3 selon l'avancement du remplissage.
    LaunchedEffect(Unit) {
        snapshotFlow { progress.value }.collect { onIntensity((it * 3).toInt().coerceIn(0, 3)) }
    }
    val breathe by rememberInfiniteTransition(label = "ignite").animateFloat(
        initialValue = 0.96f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "igniteB"
    )
    Box(
        modifier = modifier
            .size(200.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // Remplit l'anneau tant qu'on presse ; au relâchement, complet → onComplete.
                        val fill = scope.launch { progress.animateTo(1f, tween(holdMs, easing = LinearEasing)) }
                        tryAwaitRelease()
                        fill.cancel()
                        if (progress.value >= 0.98f) onComplete()
                        scope.launch { progress.animateTo(0f, tween(280)) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Halo radial diffus.
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer { scaleX = breathe; scaleY = breathe }
                .background(Ember.radialGlow(0.35f), CircleShape)
        )
        // Anneau de progression.
        Canvas(modifier = Modifier.size(150.dp)) {
            val stroke = 4.dp.toPx()
            drawCircle(Ember.Braise.copy(alpha = 0.18f), radius = (size.minDimension - stroke) / 2f, style = Stroke(stroke))
            drawArc(
                color = Ember.Braise,
                startAngle = -90f, sweepAngle = 360f * progress.value, useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke)
            )
        }
        // Disque de braise + flamme.
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { val s = 1f + 0.05f * progress.value; scaleX = s; scaleY = s }
                .clip(CircleShape)
                .background(Ember.FireGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = Ember.BraisePale2, modifier = Modifier.size(44.dp))
        }
    }
}

/** Monogramme dans un anneau de chaleur (écran Signature). */
@Composable
fun MonogramHeatRing(letter: String, dotColor: Color = Ember.Braise, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(180.dp).background(Ember.radialGlow(0.18f), CircleShape))
        Canvas(modifier = Modifier.size(160.dp)) {
            val stroke = 3.dp.toPx()
            drawCircle(Ember.Line, radius = (size.minDimension - stroke) / 2f, style = Stroke(stroke))
            drawArc(
                brush = Ember.HeatRing,
                startAngle = -120f, sweepAngle = 300f, useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke)
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(letter, color = Ember.Text, fontSize = 72.sp, fontWeight = FontWeight.Black, fontFamily = OnbFont)
            Box(modifier = Modifier.padding(bottom = 8.dp).size(16.dp).clip(CircleShape).background(dotColor))
        }
    }
}

/** Interrupteur façon maquette — piste colorée quand actif, pastille sombre. */
@Composable
fun EmberToggle(checked: Boolean, onChange: (Boolean) -> Unit, activeColor: Color = Ember.Braise, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 54.dp, height = 30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) activeColor else Ember.SurfaceWarm3)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(if (checked) Ember.Bg else Ember.TextMute))
    }
}

/** Indicateur de pages (barres). */
@Composable
fun PageDots(index: Int, count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (i == index) 26.dp else 12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (i == index) Ember.Braise else Ember.Line)
            )
        }
    }
}

/** Chip « Passer » / « Plus tard » discret. */
@Composable
fun SkipChip(text: String = "Passer", onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Ember.TextDim,
        fontSize = 16.sp,
        fontFamily = OnbFont,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick
        )
    )
}

