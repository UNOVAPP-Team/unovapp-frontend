package com.unovapp.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.Ember
import com.unovapp.android.ui.theme.EmberDim
import com.unovapp.android.ui.theme.EmberFont
import com.unovapp.android.ui.theme.EmberMotion
import com.unovapp.android.ui.theme.animatedProgress
import com.unovapp.android.ui.theme.emberEnter
import com.unovapp.android.ui.theme.pressable

/* ══════════════════════════════════════════════════════════════════════════
 *  KIT « BRAISE » — la grammaire visuelle commune aux écrans de la maquette.
 *
 *  Relevée sur screen-05 (Pulsations) et vérifiée sur les autres écrans, elle
 *  se répète partout :
 *      titre XXL à gauche  +  pilule d'action à droite
 *      carte héro (eyebrow · grand nombre · courbe · méta · CTA)
 *      eyebrows de section en capitales espacées
 *      cartes de liste à coins 18 dp, fond tiède, filet discret
 *      l'Orbe seul en bas, avec son glyphe propre à l'écran
 *
 *  Tout écran migré doit passer par ces briques : c'est ce qui garantit qu'ils
 *  se ressemblent sans qu'on ait à répéter les mêmes valeurs dans chaque fichier.
 * ══════════════════════════════════════════════════════════════════════════ */

/** Titre d'écran — le grand grotesque de la maquette. */
@Composable
fun EmberScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Ember.Text,
        fontSize = 34.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.8).sp,
        fontFamily = EmberFont,
        maxLines = 1,
        modifier = modifier
    )
}

/** Eyebrow — capitales espacées. Braise par défaut, muet pour les sections neutres. */
@Composable
fun EmberEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    lagune: Boolean = false
) {
    Text(
        text = text.uppercase(),
        color = when {
            lagune -> Ember.Lagune
            muted -> Ember.TextMute
            else -> Ember.BraiseClair
        },
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.2.sp,
        fontFamily = EmberFont,
        modifier = modifier
    )
}

/** Pilule d'action en haut à droite (« Apaiser », « Modifier »…). */
@Composable
fun EmberActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, Ember.Line, RoundedCornerShape(999.dp))
            .pressable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        leading?.invoke()
        Text(text, color = Ember.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = EmberFont, maxLines = 1)
    }
}

/**
 * Carte de la maquette. [accent] borde en braise (carte héro, élément qui compte) ;
 * sinon filet discret sur fond tiède, comme les cartes de liste.
 */
@Composable
fun EmberCard(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    lagune: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(EmberDim.CardRadius)
    val border = when {
        lagune -> Ember.Lagune.copy(alpha = 0.28f)
        accent -> Ember.Braise.copy(alpha = 0.42f)
        else -> Ember.Line
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (lagune) Ember.Lagune.copy(alpha = 0.05f) else Ember.Surface.copy(alpha = 0.55f))
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(18.dp),
        content = content
    )
}

/** Le grand nombre des écrans de bilan (37 braises, solde…). */
@Composable
fun EmberBigNumber(text: String, modifier: Modifier = Modifier, color: Color = Ember.Braise) {
    Text(
        text = text,
        color = color,
        fontSize = 58.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-2).sp,
        fontFamily = EmberFont,
        modifier = modifier
    )
}

/** Texte courant de la maquette. */
@Composable
fun EmberBody(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ember.TextDim,
    size: Int = 15,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        color = color,
        fontSize = size.sp,
        lineHeight = (size + 6).sp,
        fontWeight = weight,
        fontFamily = EmberFont,
        maxLines = maxLines,
        modifier = modifier
    )
}

/**
 * L'onde — la courbe de tendance des écrans de bilan. Elle se DESSINE de gauche
 * à droite à l'apparition (§3.10 waveDraw, 700 ms), puis reste immobile.
 */
@Composable
fun EmberWave(
    modifier: Modifier = Modifier,
    color: Color = Ember.Braise,
    points: List<Float> = listOf(0.35f, 0.5f, 0.3f, 0.62f, 0.45f, 0.78f, 0.6f, 0.7f)
) {
    val draw by animatedProgress(1f, durationMs = 700, delayMs = 120)
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val step = w / (points.size - 1)
        val path = Path().apply {
            moveTo(0f, h * (1f - points[0]))
            for (i in 1 until points.size) {
                val px = step * i
                val py = h * (1f - points[i])
                val prevX = step * (i - 1)
                val prevY = h * (1f - points[i - 1])
                cubicTo((prevX + px) / 2f, prevY, (prevX + px) / 2f, py, px, py)
            }
        }
        val measure = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
        val drawn = Path()
        measure.getSegment(0f, measure.length * draw, drawn, true)
        drawPath(drawn, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Avatar-pastille à initiale, façon maquette (pile de « qui a réagi »). */
@Composable
fun EmberInitial(
    letter: String,
    modifier: Modifier = Modifier,
    tint: Color = Ember.Braise,
    size: Dp = 44.dp
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(letter.take(1).uppercase(), color = tint, fontSize = (size.value / 2.6f).sp, fontWeight = FontWeight.Bold, fontFamily = EmberFont)
    }
}

/**
 * Ossature commune : insets, titre + action, contenu, et l'Orbe seul en bas.
 * [orbeGlyph] change d'un écran à l'autre (onde sur le Flux, pouls sur Pulsations…).
 */
@Composable
fun EmberScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ember.Bg)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EmberScreenTitle(title, modifier = Modifier.emberEnter(dyFrom = 8.dp))
            action?.invoke()
        }
        content()
    }
}

/** Le pouls — glyphe de l'Orbe sur Pulsations (tracé ECG). */
@Composable
fun PulseGlyph(modifier: Modifier = Modifier, color: Color = Ember.Text) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.5f)
            lineTo(w * 0.26f, h * 0.5f)
            lineTo(w * 0.36f, h * 0.16f)
            lineTo(w * 0.5f, h * 0.86f)
            lineTo(w * 0.62f, h * 0.42f)
            lineTo(w * 0.72f, h * 0.5f)
            lineTo(w, h * 0.5f)
        }
        drawPath(path, color, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** L'Orbe posé seul en bas d'un écran migré, avec son glyphe et sa pastille. */
@Composable
fun EmberOrbeBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false,
    glyph: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(EmberDim.Orbe + 6.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(EmberDim.Orbe)
                    .clip(CircleShape)
                    .background(Ember.Bg.copy(alpha = 0.55f))
                    .border(1.5.dp, Ember.Braise.copy(alpha = 0.85f), CircleShape)
                    .pressable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) { glyph() }
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(9.dp).clip(CircleShape).background(Ember.Braise)
                )
            }
        }
    }
}

/** Séparateur fin des cartes. */
@Composable
fun EmberDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(Ember.Line))
}
