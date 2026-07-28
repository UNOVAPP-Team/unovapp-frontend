package com.unovapp.android.ui.onboarding.ember

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.theme.Ember
import com.unovapp.android.ui.theme.EmberDim
import com.unovapp.android.ui.theme.EmberMotion
import com.unovapp.android.ui.theme.GhostFinger
import com.unovapp.android.ui.theme.GlowPulse
import com.unovapp.android.ui.theme.Haptics
import com.unovapp.android.ui.theme.LocalReducedMotion
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.animatedProgress
import com.unovapp.android.ui.theme.appear
import com.unovapp.android.ui.theme.arcOpen
import com.unovapp.android.ui.theme.breathe
import com.unovapp.android.ui.theme.countUp
import com.unovapp.android.ui.theme.emberBurst
import com.unovapp.android.ui.theme.rememberReducedMotion
import com.unovapp.android.ui.theme.revealAI
import com.unovapp.android.ui.theme.riseIn
import com.unovapp.android.ui.theme.riseInSection
import com.unovapp.android.ui.theme.riseInSmall
import com.unovapp.android.ui.theme.sheetUp
import com.unovapp.android.ui.theme.staggerDelay

/* ══════════════════════════════════════════════════════════════════════════
 *  Onboarding « La braise, pas le bruit » — les 13 écrans, en fidélité maquette.
 * ══════════════════════════════════════════════════════════════════════════ */

private const val ONB_COUNT = 13

@Composable
fun EmberOnboarding(onFinished: () -> Unit) {
    UnovAppTheme {
        CompositionLocalProvider(LocalReducedMotion provides rememberReducedMotion()) {
            var step by remember { mutableIntStateOf(0) }
            val next: () -> Unit = { if (step >= ONB_COUNT - 1) onFinished() else step++ }
            val skip: () -> Unit = onFinished
            BackHandler(enabled = step > 0) { step-- }

            Box(modifier = Modifier.fillMaxSize().background(Ember.Bg)) {
                AnimatedContent(
                    targetState = step,
                    // §6 — aucune translation latérale (l'onboarding n'est pas une pile
                    // gauche/droite) : fondu + léger zoom, entrée EaseOut / sortie EaseIn.
                    transitionSpec = {
                        (fadeIn(tween(EmberMotion.DurBase, easing = EmberMotion.EaseOut)) +
                            scaleIn(initialScale = 1.02f, animationSpec = tween(EmberMotion.DurBase, easing = EmberMotion.EaseOut)))
                            .togetherWith(
                                fadeOut(tween(EmberMotion.DurQuick, easing = EmberMotion.EaseIn)) +
                                    scaleOut(targetScale = 0.96f, animationSpec = tween(EmberMotion.DurQuick, easing = EmberMotion.EaseIn))
                            )
                    },
                    label = "onbStep"
                ) { s ->
                    when (s) {
                        0 -> ScreenAllumage(next)
                        1 -> ScreenLueur(next, skip)
                        2 -> ScreenBraise(next, skip)
                        3 -> ScreenNumero(next)
                        4 -> ScreenSignature(next)
                        5 -> ScreenCalibration(next, skip)
                        6 -> ScreenPremierCercle(next)
                        7 -> ScreenRythme(next)
                        8 -> ScreenReseau(next)
                        9 -> ScreenBraises(next)
                        10 -> ScreenOrbe(next)
                        11 -> ScreenPremiereEtincelle(next, skip)
                        else -> ScreenRallumage(onFinished)
                    }
                }
            }
        }
    }
}

/* ---------- Scaffold commun (insets + zone haute + bas) ---------- */

@Composable
private fun OnbScaffold(
    topRight: (@Composable () -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp)
    ) {
        if (topRight != null) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.CenterEnd) { topRight() }
        } else {
            Spacer(Modifier.height(20.dp))
        }
        content()
        Spacer(Modifier.weight(1f))
        if (bottom != null) {
            bottom()
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(18.dp))
    }
}

/* ═══════════════ 00 — Allumage ═══════════════ */

@Composable
private fun ScreenAllumage(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // §5 Onb 00 — séquence d'allumage chronométrée (logo → geste → texte → CTA).
        Spacer(Modifier.weight(0.9f))
        Text(
            "U N O V A P P",
            color = Ember.TextMute, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp, fontFamily = OnbFont,
            modifier = Modifier.appear(dyFrom = 0.dp, scaleFrom = 0.92f, durationMs = EmberMotion.DurStory, delayMs = 500)
        )
        Spacer(Modifier.height(20.dp))
        HoldToIgnite(onComplete = onNext)
        Spacer(Modifier.height(24.dp))
        OnbTitle("Maintiens pour allumer", align = TextAlign.Center, modifier = Modifier.riseInSmall(delayMs = 1100))
        Spacer(Modifier.height(12.dp))
        OnbSubtitle("C'est le seul geste à retenir. Tu viens déjà de l'apprendre.", align = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp).riseInSmall(delayMs = 1250))
        Spacer(Modifier.weight(1f))
        PageDots(index = 0, count = 3, modifier = Modifier.riseIn(delayMs = 1700))
        Spacer(Modifier.height(20.dp))
        Text("Aucun compte n'est demandé pour l'instant", color = Ember.TextMute, fontSize = 14.sp, fontFamily = OnbFont, modifier = Modifier.riseIn(delayMs = 1700))
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(18.dp))
    }
}

/* ═══════════════ 01 — Lueur ═══════════════ */

@Composable
private fun ScreenLueur(onNext: () -> Unit, onSkip: () -> Unit) {
    var lueurs by remember { mutableIntStateOf(3) }
    var userTapped by remember { mutableStateOf(false) }
    var glowTrigger by remember { mutableIntStateOf(0) }
    val view = LocalView.current
    OnbScaffold(topRight = { SkipChip(onClick = onSkip) }) {
        Spacer(Modifier.height(120.dp))
        OnbTitle("Effleure\nl'écran", align = TextAlign.Center, modifier = Modifier.fillMaxWidth().riseInSmall(delayMs = 60))
        Spacer(Modifier.height(14.dp))
        OnbSubtitleRich("Tu laisses une *Lueur*. C'est gratuit, autant de fois que tu veux.", align = TextAlign.Center, modifier = Modifier.fillMaxWidth().riseInSmall(delayMs = 140))
        Spacer(Modifier.height(40.dp))
        // Cercle Lueur (à effleurer). Un doigt fantôme enseigne le geste jusqu'au 1ᵉʳ vrai tap.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    GhostFinger(active = !userTapped, modifier = Modifier.matchParentSize(), ringRadius = 75.dp)
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .background(Ember.radialGlow(0.14f), CircleShape)
                            .border(1.dp, Ember.Line, CircleShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                userTapped = true; lueurs++; glowTrigger++; Haptics.light(view)
                            },
                        contentAlignment = Alignment.Center
                    ) { LueurGlyph(modifier = Modifier.size(34.dp)) }
                    GlowPulse(trigger = glowTrigger, modifier = Modifier.matchParentSize(), diameter = 150.dp)
                }
                Spacer(Modifier.height(14.dp))
                Text("Lueur ×${countUp(lueurs)}", color = Ember.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
            }
        }
        Spacer(Modifier.weight(1f))
        MiniCreatorBlock()
        Spacer(Modifier.height(24.dp))
        PageDots(index = 1, count = 3, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
        // Avance après quelques lueurs — un tap sur le cercle suffit à 6.
        if (lueurs >= 6) onNext()
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(10.dp))
    }
}

/* ═══════════════ 02 — Braise ═══════════════ */

@Composable
private fun ScreenBraise(onNext: () -> Unit, onSkip: () -> Unit) {
    var intensity by remember { mutableIntStateOf(2) }
    var userHeld by remember { mutableStateOf(false) }
    OnbScaffold(topRight = { SkipChip(onClick = onSkip) }) {
        Spacer(Modifier.height(60.dp))
        OnbTitle("Maintenant,\nmaintiens", align = TextAlign.Center, modifier = Modifier.fillMaxWidth().riseInSmall(delayMs = 60))
        Spacer(Modifier.height(14.dp))
        OnbSubtitleRich("Tu souffles une *Braise*. Plus tu tiens, plus elle est forte.", align = TextAlign.Center, modifier = Modifier.fillMaxWidth().riseInSmall(delayMs = 140), emphColor = Ember.BraiseClair)
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(end = 40.dp), horizontalArrangement = Arrangement.End) {
            listOf(1, 2, 3).forEach { i ->
                Text(
                    "×$i",
                    color = if (i == intensity) Ember.Braise else Ember.TextMute,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
        // Doigt fantôme « maintenu » (ringFill 900 ms + emberBurst) jusqu'au 1ᵉʳ vrai maintien.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                GhostFinger(active = !userHeld, hold = true, modifier = Modifier.matchParentSize(), ringRadius = 75.dp)
                HoldToIgnite(onComplete = onNext, onIntensity = { if (it > 0) { intensity = it; userHeld = true } })
            }
        }
        Spacer(Modifier.weight(1f))
        // Carte « CE QUE ÇA COÛTE ».
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius))
                .background(Ember.SurfaceWarm.copy(alpha = 0.5f)).border(1.dp, Ember.Braise.copy(alpha = 0.35f), RoundedCornerShape(EmberDim.CardRadius))
                .padding(18.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Ce que ça coûte")
                Text("1 braise = 10 F", color = Ember.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
            }
            Spacer(Modifier.height(10.dp))
            OnbSubtitleRich("La braise part de ta Réserve et *arrive chez le créateur en FCFA*. C'est pour ça qu'elle n'est pas gratuite.", modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius))
                .border(1.dp, Ember.Positif.copy(alpha = 0.4f), RoundedCornerShape(EmberDim.CardRadius)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Check, null, tint = Ember.Positif, modifier = Modifier.size(18.dp))
            Text(buildOffer(), color = Ember.Text, fontSize = 15.sp, fontFamily = OnbFont)
        }
        Spacer(Modifier.height(20.dp))
        PageDots(index = 2, count = 3, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

private fun buildOffer() = "On t'en offre 10 pour commencer"

/* ═══════════════ 03 — Numéro ═══════════════ */

@Composable
private fun ScreenNumero(onNext: () -> Unit) {
    var number by remember { mutableStateOf("") }
    OnbScaffold(bottom = { EmberPrimaryButton("Recevoir le code", onNext, Modifier.fillMaxWidth()) }) {
        Spacer(Modifier.height(10.dp))
        OnbTitle("Ton numéro,\nc'est ton\ncompte", modifier = Modifier.riseInSmall(delayMs = 60))
        Spacer(Modifier.height(12.dp))
        OnbSubtitle("Le même que ton Mobile Money — c'est là que tes braises reviendront en FCFA.", modifier = Modifier.riseInSmall(delayMs = 140))
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius))
                .border(1.dp, Ember.Braise.copy(alpha = 0.5f), RoundedCornerShape(EmberDim.CardRadius))
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("+229", color = Ember.TextDim, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
            Box(modifier = Modifier.padding(horizontal = 14.dp).width(1.dp).height(24.dp).background(Ember.Line))
            Text(formatPhone(number), color = Ember.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(2.dp).height(26.dp).background(Ember.Braise))
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Lock, null, tint = Ember.TextMute, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            OnbSubtitle("Un code arrive par SMS. Pas de mot de passe à inventer, ni à oublier.", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Keypad(
            onDigit = { if (number.length < 8) number += it },
            onDelete = { number = number.dropLast(1) },
            modifier = Modifier.sheetUp(delayMs = 120)
        )
    }
}

private fun formatPhone(d: String): String = d.chunked(2).joinToString(" ")

@Composable
private fun Keypad(onDigit: (String) -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier.weight(1f).height(58.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() }, indication = null,
                                enabled = key.isNotEmpty()
                            ) { if (key == "⌫") onDelete() else if (key.isNotEmpty()) onDigit(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key.isNotEmpty()) Text(key, color = Ember.Text, fontSize = 26.sp, fontWeight = FontWeight.Medium, fontFamily = OnbFont)
                    }
                }
            }
        }
    }
}

/* ═══════════════ 04 — Signature ═══════════════ */

private val SignatureColors = listOf(Ember.Braise, Ember.Gold, Ember.Positif, Color(0xFFC86BD9))

@Composable
private fun ScreenSignature(onNext: () -> Unit) {
    var color by remember { mutableIntStateOf(0) }
    // §5 Onb 04 — la couleur choisie se propage à toute l'identité en 520 ms (EaseSoft).
    val propagated by androidx.compose.animation.animateColorAsState(
        targetValue = SignatureColors[color],
        animationSpec = tween(EmberMotion.DurSlow, easing = EmberMotion.EaseSoft),
        label = "signatureColor"
    )
    OnbScaffold(bottom = { EmberPrimaryButton("C'est moi", onNext, Modifier.fillMaxWidth()) }) {
        Spacer(Modifier.height(10.dp))
        OnbTitle("Ta signature", modifier = Modifier.riseInSmall(delayMs = 60))
        Spacer(Modifier.height(12.dp))
        OnbSubtitle("Elle te suit partout : ton monogramme, tes anneaux, tes braises.", modifier = Modifier.riseInSmall(delayMs = 140))
        Spacer(Modifier.height(24.dp))
        MonogramHeatRing("B", dotColor = propagated, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius))
                .border(1.dp, Ember.Line, RoundedCornerShape(EmberDim.CardRadius)).padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("@", color = Ember.TextMute, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
            Spacer(Modifier.width(12.dp))
            Text("beingar", color = Ember.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Check, null, tint = Ember.Positif, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("libre", color = Ember.Positif, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = OnbFont)
        }
        Spacer(Modifier.height(20.dp))
        Eyebrow("Choisis ta couleur", muted = true)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SignatureColors.forEachIndexed { i, c ->
                val selected = i == color
                val pop by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = tween(EmberMotion.DurBase, easing = EmberMotion.EaseOut),
                    label = "pastillePop"
                )
                Box(
                    modifier = Modifier.size(56.dp)
                        .graphicsLayer { scaleX = pop; scaleY = pop }
                        .clip(CircleShape).background(c)
                        .then(if (selected) Modifier.border(2.dp, Ember.Text, CircleShape) else Modifier)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { color = i }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Tu pourras ajouter une photo plus tard, si tu veux.", color = Ember.TextMute, fontSize = 14.sp, fontFamily = OnbFont, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

/* ═══════════════ 05 — Calibration ═══════════════ */

@Composable
private fun ScreenCalibration(onNext: () -> Unit, onSkip: () -> Unit) {
    var index by remember { mutableIntStateOf(1) }
    val advance: () -> Unit = { if (index >= 3) onNext() else index++ }
    OnbScaffold(topRight = { Text("$index / 3", color = Ember.TextDim, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = OnbFont) }) {
        Spacer(Modifier.height(8.dp))
        OnbTitle("Ce qui te réchauffe", modifier = Modifier.riseInSmall(delayMs = 60))
        Spacer(Modifier.height(10.dp))
        OnbSubtitle("Regarde trois vidéos. Souffle sur celles que tu aimes.", modifier = Modifier.riseInSmall(delayMs = 140))
        Spacer(Modifier.height(20.dp))
        // Pile de cartes vidéo.
        Box(modifier = Modifier.fillMaxWidth().height(320.dp).riseIn(delayMs = 220), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxWidth(0.86f).height(300.dp).graphicsLayer { rotationZ = 3f }.clip(RoundedCornerShape(20.dp)).background(Ember.SurfaceWarm2))
            Box(
                modifier = Modifier.fillMaxWidth(0.9f).height(310.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(Ember.SurfaceWarm3, Ember.SurfaceWarm))),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Danse au Fidjrossè", color = Ember.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
                    Text("@ciscomignon · 0:14", color = Ember.TextDim, fontSize = 14.sp, fontFamily = OnbFont)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            EmberGhostButton("›  Passer", advance, Modifier.weight(1f))
            EmberPrimaryButton("Souffler", advance, Modifier.weight(1f), icon = true)
        }
        Spacer(Modifier.weight(1f))
        LagouneCard("Ce que l'IA retient", "Elle apprend de *tes gestes*, pas de tes déclarations. Trois vidéos suffisent pour partir.", modifier = Modifier.revealAI(delayMs = 300))
    }
}

/* ═══════════════ 06 — Premier cercle ═══════════════ */

@Composable
private fun ScreenPremierCercle(onNext: () -> Unit) {
    var c1 by remember { mutableStateOf(true) }
    var c2 by remember { mutableStateOf(true) }
    var c3 by remember { mutableStateOf(false) }
    OnbScaffold(bottom = {
        Column {
            Text("Tu pourras te délier d'un geste, à tout moment.", color = Ember.TextMute, fontSize = 14.sp, fontFamily = OnbFont, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            EmberPrimaryButton("Garder ces deux-là", onNext, Modifier.fillMaxWidth())
        }
    }) {
        Spacer(Modifier.height(8.dp))
        OnbTitle("Ton premier\ncercle", modifier = Modifier.riseInSmall(delayMs = 60))
        Spacer(Modifier.height(10.dp))
        OnbSubtitle("Tu viens de souffler sur eux. On te les garde ?", modifier = Modifier.riseInSmall(delayMs = 140))
        Spacer(Modifier.height(22.dp))
        CercleRow("C", "@ciscomignon", "tu as soufflé ×2", Ember.Braise, c1, { c1 = it }, modifier = Modifier.riseIn(delayMs = staggerDelay(0, 40) + 180))
        Spacer(Modifier.height(12.dp))
        CercleRow("E", "Eloge", "tu as soufflé ×1", Color(0xFF7C6BD9), c2, { c2 = it }, modifier = Modifier.riseIn(delayMs = staggerDelay(1, 40) + 180))
        Spacer(Modifier.height(12.dp))
        CercleRow("R", "Ramsover", "tu as passé sa vidéo", Ember.Positif, c3, { c3 = it }, muted = true, modifier = Modifier.riseIn(delayMs = staggerDelay(2, 40) + 180))
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius)).border(1.dp, Ember.Line, RoundedCornerShape(EmberDim.CardRadius)).padding(16.dp),
            verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Lock, null, tint = Ember.Positif, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            OnbSubtitleRich("On ne te demandera *jamais ton répertoire*. Ton cercle se construit tout seul, avec ceux que tu réchauffes.", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CercleRow(letter: String, name: String, sub: String, ring: Color, checked: Boolean, onChange: (Boolean) -> Unit, muted: Boolean = false, modifier: Modifier = Modifier) {
    val ringP by animatedProgress(1f, durationMs = EmberMotion.DurSlow, delayMs = 240)
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius))
            .border(1.dp, if (checked) Ember.Braise.copy(alpha = 0.4f) else Ember.Line, RoundedCornerShape(EmberDim.CardRadius))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            if (!muted) Canvas(modifier = Modifier.fillMaxSize()) {
                val st = 2.5.dp.toPx()
                drawArc(ring, -215f, 250f * ringP, false, style = Stroke(st, cap = StrokeCap.Round),
                    topLeft = Offset(st / 2, st / 2), size = androidx.compose.ui.geometry.Size(size.width - st, size.height - st))
            }
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(ring.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text(letter, color = ring, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(name, color = Ember.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (!muted) Icon(Icons.Filled.LocalFireDepartment, null, tint = Ember.Braise, modifier = Modifier.size(13.dp))
                Text(sub, color = if (muted) Ember.TextMute else Ember.BraiseClair, fontSize = 14.sp, fontFamily = OnbFont)
            }
        }
        EmberToggle(checked, onChange)
    }
}

/* ═══════════════ 07 — Rythme (IA) ═══════════════ */

@Composable
private fun ScreenRythme(onNext: () -> Unit) {
    var t1 by remember { mutableStateOf(true) }
    var t2 by remember { mutableStateOf(true) }
    var t3 by remember { mutableStateOf(true) }
    OnbScaffold(bottom = { EmberLaguneButton("D'accord, on continue", onNext, Modifier.fillMaxWidth()) }) {
        // §5 Onb 07 — tout est lagune : revealAI (opacité seule, aucune translation, R5).
        Spacer(Modifier.height(14.dp))
        Eyebrow("Ton rythme", lagune = true, modifier = Modifier.revealAI())
        Spacer(Modifier.height(10.dp))
        OnbTitle("Ce que l'IA a le\ndroit de faire", modifier = Modifier.revealAI(delayMs = 60))
        Spacer(Modifier.height(12.dp))
        OnbSubtitle("On te le demande maintenant, pas dans six mois au fond des réglages.", modifier = Modifier.revealAI(delayMs = 120))
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius))
                .background(Ember.Lagune.copy(alpha = 0.05f)).border(1.dp, Ember.Lagune.copy(alpha = 0.25f), RoundedCornerShape(EmberDim.CardRadius))
                .padding(18.dp)
                .revealAI(delayMs = 180)
        ) {
            RythmeRow("Composer mon flux", "Elle regarde les vidéos pour comprendre ce que tu aimes", t1, { t1 = it })
            RythmeDivider()
            RythmeRow("Suggérer où aller", "L'Orbe devine ton prochain écran", t2, { t2 = it })
            RythmeDivider()
            RythmeRow("Me proposer une pause", "Après 45 minutes de flux d'affilée", t3, { t3 = it }, badge = "CONSEILLÉ", color = Ember.Positif)
        }
        Spacer(Modifier.height(14.dp))
        OnbSubtitleRich("Tout se coupe d'un geste, à tout moment, dans *Réglages › Rythme*. Rien n'est définitif.", modifier = Modifier.fillMaxWidth().revealAI(delayMs = 500))
    }
}

@Composable
private fun RythmeRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit, badge: String? = null, color: Color = Ember.Lagune) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = Ember.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
                if (badge != null) Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, Ember.Positif.copy(alpha = 0.5f), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                ) { Text(badge, color = Ember.Positif, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, fontFamily = OnbFont) }
            }
            Text(sub, color = Ember.TextDim, fontSize = 14.sp, lineHeight = 19.sp, fontFamily = OnbFont, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.width(12.dp))
        EmberToggle(checked, onChange, activeColor = color)
    }
}

@Composable
private fun RythmeDivider() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(1.dp).background(Ember.Line))
}

/* ═══════════════ 08 — Réseau ═══════════════ */

@Composable
private fun ScreenReseau(onNext: () -> Unit) {
    var eco by remember { mutableStateOf(true) }
    var night by remember { mutableStateOf(false) }
    OnbScaffold(bottom = { EmberPrimaryButton("Garder ces réglages", onNext, Modifier.fillMaxWidth()) }) {
        Spacer(Modifier.height(10.dp))
        OnbTitle("On a regardé\nton réseau", modifier = Modifier.riseInSmall(delayMs = 60))
        Spacer(Modifier.height(12.dp))
        OnbSubtitle("Autant se le dire tout de suite, plutôt que te laisser croire que l'app est cassée.", modifier = Modifier.riseInSmall(delayMs = 140))
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius))
                .border(1.dp, Ember.Braise.copy(alpha = 0.35f), RoundedCornerShape(EmberDim.CardRadius)).padding(18.dp)
                .riseIn(delayMs = 200)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("3G", color = Ember.BraiseClair, fontSize = 44.sp, fontWeight = FontWeight.Black, fontFamily = OnbFont)
                    Text("≈ 240 kb/s · MTN Cotonou", color = Ember.TextDim, fontSize = 14.sp, fontFamily = OnbFont)
                }
                OndeCurve(modifier = Modifier.size(width = 120.dp, height = 44.dp))
            }
            RythmeDivider()
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mode ÉCO — 240p", color = Ember.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
                    Text("Quatre fois moins de données, démarrage en moins de 2 s", color = Ember.TextDim, fontSize = 14.sp, lineHeight = 19.sp, fontFamily = OnbFont, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.width(12.dp))
                EmberToggle(eco, { eco = it })
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius)).border(1.dp, Ember.Line, RoundedCornerShape(EmberDim.CardRadius)).padding(16.dp)
                .riseIn(delayMs = 260),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Télécharger la nuit en Wi-Fi", color = Ember.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
                Text("Les vidéos de tes fidèles t'attendent le matin", color = Ember.TextDim, fontSize = 14.sp, fontFamily = OnbFont, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(12.dp))
            EmberToggle(night, { night = it })
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, Ember.Braise.copy(alpha = 0.5f), RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 7.dp)
            ) { Text("Éco · 240p", color = Ember.BraiseClair, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = OnbFont) }
            Text("Ce bandeau apparaîtra quand la qualité baisse.", color = Ember.TextDim, fontSize = 14.sp, fontFamily = OnbFont)
        }
    }
}

@Composable
private fun OndeCurve(modifier: Modifier = Modifier) {
    // §3.10 waveDraw — l'onde se dessine de gauche à droite (700 ms).
    val draw by animatedProgress(1f, durationMs = 700, delayMs = 200)
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.6f)
            cubicTo(w * 0.15f, h * 0.1f, w * 0.25f, h * 0.9f, w * 0.4f, h * 0.5f)
            cubicTo(w * 0.55f, h * 0.1f, w * 0.7f, h * 0.9f, w * 0.85f, h * 0.35f)
            lineTo(w, h * 0.45f)
        }
        val measure = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
        val drawn = Path()
        measure.getSegment(0f, measure.length * draw, drawn, true)
        drawPath(drawn, Ember.Braise, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

/* ═══════════════ 09 — Braises offertes ═══════════════ */

@Composable
private fun ScreenBraises(onNext: () -> Unit) {
    // §5 Onb 09 — le solde offert se savoure : countUp 900 ms puis emberBurst + haptique.
    var burstTrigger by remember { mutableIntStateOf(0) }
    val view = LocalView.current
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(EmberMotion.DurStory.toLong())
        burstTrigger++
        Haptics.medium(view)
    }
    OnbScaffold(bottom = { EmberPrimaryButton("Merci, on y va", onNext, Modifier.fillMaxWidth()) }) {
        Spacer(Modifier.height(10.dp))
        OnbTitle("On t'offre\ntes premières\nbraises")
        Spacer(Modifier.height(12.dp))
        OnbSubtitle("Dépense-les. C'est en soufflant qu'on comprend ce que ça vaut.")
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius))
                .border(1.dp, Ember.Braise.copy(alpha = 0.4f), RoundedCornerShape(EmberDim.CardRadius)).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Ta réserve")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier.size(60.dp)
                            .emberBurst(burstTrigger)
                            .breathe(amplitude = 1.03f, minAlpha = 0.9f)
                            .clip(CircleShape).background(Ember.FireGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = Ember.BraisePale2, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text("${countUp(10, durationMs = EmberMotion.DurStory)}", color = Ember.BraisePale, fontSize = 44.sp, fontWeight = FontWeight.Black, fontFamily = OnbFont)
                        Text("≈ 100 FCFA · offertes", color = Ember.TextDim, fontSize = 14.sp, fontFamily = OnbFont, modifier = Modifier.riseInSmall(delayMs = 600))
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Eyebrow("Quand tu voudras en reprendre", muted = true, modifier = Modifier.padding(start = 2.dp))
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PayCard("MTN MoMo", "50 braises = 500 F", Ember.Gold, Modifier.weight(1f))
            PayCard("Moov Money", "Flooz", Color(0xFF3B9AE1), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        LagouneCard(null, "Et quand *on soufflera sur les tiennes*, elles reviendront en FCFA sur ton Mobile Money.", dotOnly = true)
    }
}

@Composable
private fun PayCard(title: String, sub: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(EmberDim.CardRadius))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(EmberDim.CardRadius)).padding(16.dp)
    ) {
        Text(title, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
        Text(sub, color = Ember.TextDim, fontSize = 13.sp, fontFamily = OnbFont, modifier = Modifier.padding(top = 4.dp))
    }
}

/* ═══════════════ 10 — Orbe ═══════════════ */

@Composable
private fun ScreenOrbe(onEnter: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.8f))
        OnbTitle("Tout part\nd'ici", align = TextAlign.Center, modifier = Modifier.fillMaxWidth().riseInSmall(delayMs = 60))
        Spacer(Modifier.height(14.dp))
        OnbSubtitle("Un seul point, sous ton pouce. Pas de barre d'onglets, pas de menu à apprendre.", align = TextAlign.Center, modifier = Modifier.fillMaxWidth().riseInSmall(delayMs = 140))
        Spacer(Modifier.weight(1f))
        // Rangée de destinations — se déplie en arcOpen (stagger 28 ms), part du centre.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            OrbeDest("Flux", Modifier.arcOpen(delayMs = staggerDelay(2, 28))) { WaveGlyph(Modifier.size(22.dp)) }
            OrbeDest("Explorer", Modifier.arcOpen(delayMs = staggerDelay(1, 28))) { Icon(Icons.Outlined.Search, null, tint = Ember.TextDim, modifier = Modifier.size(20.dp)) }
            OrbeDest("Studio", Modifier.arcOpen(delayMs = staggerDelay(0, 28))) { Icon(Icons.Filled.Add, null, tint = Ember.TextDim, modifier = Modifier.size(22.dp)) }
            OrbeDest("Pulsations", Modifier.arcOpen(delayMs = staggerDelay(1, 28))) { Icon(Icons.Outlined.Notifications, null, tint = Ember.TextDim, modifier = Modifier.size(20.dp)) }
            OrbeDest("Univers", Modifier.arcOpen(delayMs = staggerDelay(2, 28))) { Icon(Icons.Outlined.Person, null, tint = Ember.TextDim, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.height(20.dp))
        // L'Orbe pulsant.
        val pulse by rememberInfiniteTransition(label = "orbe").animateFloat(
            initialValue = 1f, targetValue = 1.35f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "orbeP"
        )
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                for (i in 0..2) {
                    val p = ((pulse - 1f) + i * 0.12f) % 0.5f
                    drawCircle(Ember.Braise.copy(alpha = (0.4f - p).coerceAtLeast(0f)), radius = size.minDimension / 2f * (0.6f + p), style = Stroke(1.5.dp.toPx()))
                }
            }
            // Doigt fantôme qui tape l'Orbe — 3 démonstrations puis il s'efface (§5 Onb 10).
            GhostFinger(active = true, maxLoops = 3, modifier = Modifier.matchParentSize(), ringRadius = 42.dp)
            Box(
                modifier = Modifier.size(84.dp).clip(CircleShape).background(Ember.Bg).border(2.dp, Ember.Braise, CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onEnter),
                contentAlignment = Alignment.Center
            ) { WaveGlyph(Modifier.size(30.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Touche l'Orbe pour entrer", color = Ember.BraiseClair, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(24.dp))
    }
}

@Composable
private fun OrbeDest(label: String, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(EmberDim.Touch).clip(CircleShape).border(1.dp, Ember.Line, CircleShape), contentAlignment = Alignment.Center) { icon() }
        Text(label, color = Ember.TextMute, fontSize = 12.sp, fontFamily = OnbFont)
    }
}

/* ═══════════════ 11 — Première étincelle ═══════════════ */

@Composable
private fun ScreenPremiereEtincelle(onNext: () -> Unit, onSkip: () -> Unit) {
    OnbScaffold(topRight = { SkipChip("Plus tard", onSkip) }, bottom = {
        Text("Une vidéo, ce sera pour quand tu voudras.", color = Ember.TextMute, fontSize = 14.sp, fontFamily = OnbFont, modifier = Modifier.align(Alignment.CenterHorizontally))
    }) {
        Spacer(Modifier.height(14.dp))
        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, Ember.Braise.copy(alpha = 0.5f), RoundedCornerShape(999.dp)).padding(horizontal = 14.dp, vertical = 8.dp).riseInSmall(delayMs = 60)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SparkGlyph(Modifier.size(15.dp), Ember.BraiseClair)
                Text("TON TOUR", color = Ember.BraiseClair, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp, fontFamily = OnbFont)
            }
        }
        Spacer(Modifier.height(14.dp))
        OnbTitle("Dis-le à 0:06", modifier = Modifier.riseInSmall(delayMs = 140))
        Spacer(Modifier.height(12.dp))
        OnbSubtitle("Quatre mots suffisent. Ton étincelle se plantera exactement là, sur le moment qui t'a fait réagir.", modifier = Modifier.riseInSmall(delayMs = 200))
        Spacer(Modifier.weight(1f))
        // Ligne de temps avec poignée à 0:06 + étincelle.
        Box(modifier = Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.CenterStart) {
            Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                val y = size.height / 2f
                drawLine(Ember.Line, Offset(0f, y), Offset(size.width, y), 3.dp.toPx(), StrokeCap.Round)
                drawLine(Ember.Braise, Offset(0f, y), Offset(size.width * 0.35f, y), 3.dp.toPx(), StrokeCap.Round)
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                val y = size.height / 2f
                drawCircle(Ember.TextMute, 4.dp.toPx(), Offset(size.width * 0.66f, y))
            }
            // Poignée de scrub à ~35 % (0:06) — positionnée par poids, sans offset fragile.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(0.35f))
                Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(Ember.BraisePale2))
                Spacer(Modifier.weight(0.65f))
            }
        }
        Text("0:06", color = Ember.BraiseClair, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
        Spacer(Modifier.height(14.dp))
        val view = LocalView.current
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius)).border(1.dp, Ember.Braise.copy(alpha = 0.4f), RoundedCornerShape(EmberDim.CardRadius)).padding(horizontal = 16.dp, vertical = 12.dp)
                .sheetUp(delayMs = 240),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SparkGlyph(Modifier.size(18.dp), Ember.BraiseClair)
            Text("Ce pas est propre", color = Ember.Text, fontSize = 17.sp, fontFamily = OnbFont, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Ember.FireGradient).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Haptics.light(view); onNext() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Ember.Text, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/* ═══════════════ 12 — Rallumage (jour 2) ═══════════════ */

@Composable
private fun ScreenRallumage(onEnter: () -> Unit) {
    // §5 Onb 12 — écran de sortie : séquence monogramme → anneau (ringFill) → texte.
    val ringP by animatedProgress(1f, durationMs = EmberMotion.DurSlow, delayMs = 400)
    OnbScaffold(bottom = { EmberPrimaryButton("Rallumer", onEnter, Modifier.fillMaxWidth(), icon = true) }) {
        Spacer(Modifier.height(14.dp))
        Eyebrow("Samedi matin · Ton 2ᵉ jour", modifier = Modifier.riseInSmall(delayMs = 60))
        Spacer(Modifier.height(10.dp))
        OnbTitle("Ta braise\nest restée\nchaude", modifier = Modifier.riseInSmall(delayMs = 140))
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.appear(dyFrom = 0.dp, scaleFrom = 0.92f, durationMs = EmberMotion.DurStory)) {
                Box(modifier = Modifier.size(190.dp).breathe(amplitude = 1.03f, minAlpha = 0.7f).background(Ember.radialGlow(0.16f), CircleShape))
                Canvas(modifier = Modifier.size(150.dp)) {
                    val st = 4.dp.toPx()
                    drawCircle(Ember.Braise.copy(alpha = 0.18f), radius = (size.minDimension - st) / 2f, style = Stroke(st))
                    drawArc(Ember.Braise, -90f, 300f * ringP, false, style = Stroke(st, cap = StrokeCap.Round), topLeft = Offset(st / 2, st / 2), size = androidx.compose.ui.geometry.Size(size.width - st, size.height - st))
                }
                Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Ember.FireGradient), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${countUp(4, durationMs = EmberMotion.DurSlow)}", color = Ember.Text, fontSize = 46.sp, fontWeight = FontWeight.Black, fontFamily = OnbFont)
                        Text("réactions", color = Ember.BraisePale2, fontSize = 13.sp, fontFamily = OnbFont)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius)).border(1.dp, Ember.Line, RoundedCornerShape(EmberDim.CardRadius)).padding(18.dp)
                .riseIn(delayMs = 700)
        ) {
            Eyebrow("Pendant ton absence", muted = true, modifier = Modifier.padding(bottom = 14.dp))
            AbsenceLine({ SparkGlyph(Modifier.size(16.dp), Ember.Text) }, "3 lueurs", " sur ton étincelle de hier")
            Spacer(Modifier.height(12.dp))
            AbsenceLine({ Icon(Icons.Filled.LocalFireDepartment, null, tint = Ember.Braise, modifier = Modifier.size(16.dp)) }, "@ciscomignon", " t'a répondu")
            Spacer(Modifier.height(12.dp))
            AbsenceLine({ Icon(Icons.Outlined.Person, null, tint = Ember.Positif, modifier = Modifier.size(16.dp)) }, "Eloge", " a publié un nouvel épisode")
        }
        Spacer(Modifier.height(14.dp))
        LagouneCard(null, "Ton flux du matin est prêt — *téléchargé cette nuit*, il démarre sans réseau.", dotOnly = true, modifier = Modifier.revealAI(delayMs = 900))
    }
}

@Composable
private fun AbsenceLine(icon: @Composable () -> Unit, bold: String, rest: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) { icon() }
        Row {
            Text(bold, color = Ember.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
            Text(rest, color = Ember.TextDim, fontSize = 16.sp, fontFamily = OnbFont)
        }
    }
}

/* ═══════════════ Éléments partagés ═══════════════ */

@Composable
private fun MiniCreatorBlock() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Ember.SurfaceWarm), contentAlignment = Alignment.Center) {
            Avatar(idx = 0, name = "ciscomignon", size = 44.dp)
        }
        Column {
            Text("@ciscomignon", color = Ember.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = OnbFont)
            Text("son original", color = Ember.TextDim, fontSize = 13.sp, fontFamily = OnbFont)
        }
    }
}

@Composable
private fun LagouneCard(eyebrow: String?, body: String, dotOnly: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(EmberDim.CardRadius))
            .background(Ember.Lagune.copy(alpha = 0.05f)).border(1.dp, Ember.Lagune.copy(alpha = 0.25f), RoundedCornerShape(EmberDim.CardRadius)).padding(16.dp)
    ) {
        if (eyebrow != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Ember.Lagune))
                Eyebrow(eyebrow, lagune = true)
            }
            Spacer(Modifier.height(8.dp))
            OnbSubtitleRich(body, modifier = Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(Ember.Lagune))
                OnbSubtitleRich(body, modifier = Modifier.weight(1f))
            }
        }
    }
}

/* ---------- Glyphes ---------- */

@Composable
private fun WaveGlyph(modifier: Modifier = Modifier, color: Color = Ember.BraiseClair) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.62f)
            cubicTo(w * 0.30f, h * 0.30f, w * 0.42f, h * 0.30f, w * 0.52f, h * 0.50f)
            cubicTo(w * 0.62f, h * 0.70f, w * 0.74f, h * 0.70f, w * 0.88f, h * 0.40f)
        }
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun SparkGlyph(modifier: Modifier = Modifier, color: Color = Ember.Text) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val rOut = size.minDimension / 2f; val rIn = rOut * 0.42f
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            val dx = kotlin.math.cos(a).toFloat(); val dy = kotlin.math.sin(a).toFloat()
            drawLine(color, Offset(c.x + dx * rIn, c.y + dy * rIn), Offset(c.x + dx * rOut, c.y + dy * rOut), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun LueurGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Ember.BraisePale2, radius = size.minDimension * 0.16f, center = c)
        val rOut = size.minDimension / 2f; val rIn = rOut * 0.55f
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            val dx = kotlin.math.cos(a).toFloat(); val dy = kotlin.math.sin(a).toFloat()
            drawLine(Ember.BraisePale2, Offset(c.x + dx * rIn, c.y + dy * rIn), Offset(c.x + dx * rOut, c.y + dy * rOut), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}
