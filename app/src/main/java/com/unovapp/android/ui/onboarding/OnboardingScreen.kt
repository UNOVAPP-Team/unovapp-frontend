package com.unovapp.android.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.components.GhostPillButton
import com.unovapp.android.ui.components.GoldPrimaryButton
import com.unovapp.android.ui.components.MiniBattleCard
import com.unovapp.android.ui.components.MiniFeedCard
import com.unovapp.android.ui.components.MiniWalletCard
import com.unovapp.android.ui.components.OperatorPill
import com.unovapp.android.ui.components.RotatingGreeting
import com.unovapp.android.ui.components.floating
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import kotlinx.coroutines.delay

private enum class Visual { Hero, Battle, Wallet }

private data class Slide(
    val eyebrow: String,
    val title: String,
    val sub: String,
    val visual: Visual
)

private val SLIDES = listOf(
    Slide(
        eyebrow = "01 — Le terrain",
        title = "L'Afrique\nen vidéo.",
        sub = "Cotonou, Lagos, Dakar, Abidjan. Une scène, un feed. Pensé pour la 3G, optimisé pour ton forfait.",
        visual = Visual.Hero
    ),
    Slide(
        eyebrow = "02 — Le moteur",
        title = "Le Battle\nen direct.",
        sub = "Défie un créateur. Vote en direct. Les jetons coulent, les cadeaux flottent, le gagnant s'envole.",
        visual = Visual.Battle
    ),
    Slide(
        eyebrow = "03 — La valeur",
        title = "Mobile Money,\nintégré.",
        sub = "MTN MoMo et Moov Money en natif. Recharge en deux clics, retire ce que tu gagnes. Pas de carte requise.",
        visual = Visual.Wallet
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onLogin: () -> Unit = onFinished
) {
    UnovAppTheme {
        var slide by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
        ) {
            AmbientOrbs()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // Progress segments
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SLIDES.forEachIndexed { i, _ ->
                        val target = when {
                            i < slide -> 1f
                            i == slide -> 1f
                            else -> 0f
                        }
                        val fill by animateFloatAsState(
                            targetValue = target,
                            animationSpec = tween(
                                durationMillis = if (i == slide) 4000 else 300,
                                easing = LinearEasing
                            ),
                            label = "progress-$i"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fill)
                                    .background(UnovColors.Accent)
                            )
                        }
                    }
                }

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RotatingGreeting()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .border(
                                1.dp,
                                UnovColors.Accent.copy(alpha = 0.22f),
                                RoundedCornerShape(999.dp)
                            )
                    ) {
                        GhostPillButton(text = "Passer", onClick = onFinished)
                    }
                }

                // Visual stage
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = SLIDES[slide].visual,
                        transitionSpec = {
                            (fadeIn(tween(400)) + slideInVertically { it / 4 })
                                .togetherWith(fadeOut(tween(200)) + slideOutVertically { -it / 4 })
                        },
                        label = "visual"
                    ) { visual ->
                        when (visual) {
                            Visual.Hero -> HeroVisual()
                            Visual.Battle -> BattleVisual()
                            Visual.Wallet -> WalletVisual()
                        }
                    }
                }

                // Text content (eyebrow + title + sub)
                Column(modifier = Modifier.padding(start = 28.dp, end = 28.dp, bottom = 12.dp)) {
                    AnimatedContent(
                        targetState = slide,
                        transitionSpec = {
                            (fadeIn(tween(350)) + slideInVertically { it / 6 })
                                .togetherWith(fadeOut(tween(150)))
                        },
                        label = "copy"
                    ) { idx ->
                        val cur = SLIDES[idx]
                        Column {
                            Text(
                                text = cur.eyebrow.uppercase(),
                                color = UnovColors.TextMute,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 2.4.sp
                            )
                            Text(
                                text = cur.title,
                                color = UnovColors.Text,
                                fontSize = 42.sp,
                                lineHeight = 42.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-1.3).sp,
                                modifier = Modifier.padding(top = 14.dp)
                            )
                            Text(
                                text = cur.sub,
                                color = UnovColors.TextDim,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }

                // Social proof — slide 0 only
                AnimatedVisibility(visible = slide == 0) {
                    Row(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AvatarsStack()
                        Text(
                            text = buildAnnotatedSocialProof(),
                            color = UnovColors.TextMute,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // CTA + login link
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoldPrimaryButton(
                        text = if (slide < SLIDES.lastIndex) "Continuer" else "Créer mon compte",
                        onClick = {
                            if (slide < SLIDES.lastIndex) slide++ else onFinished()
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF0D0D0D),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    val noRipple = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = noRipple,
                                indication = null,
                                onClick = onLogin
                            )
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "J'ai déjà un compte",
                            color = UnovColors.TextMute,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "· Connexion",
                            color = UnovColors.Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Auto-advance synced with progress bar
        LaunchedEffect(slide) {
            delay(4200)
            if (slide < SLIDES.lastIndex) slide++
        }
    }
}

@Composable
private fun AmbientOrbs() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(480.dp)
                .offset(x = (-96).dp, y = (-168).dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            UnovColors.Accent.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(380.dp)
                .offset(x = 80.dp, y = 110.dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            UnovColors.AccentDeep.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun HeroVisual() {
    Box(
        modifier = Modifier.size(width = 320.dp, height = 290.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 30.dp)
                .floating(periodMs = 3500, amplitudeDp = 8f)
        ) { MiniFeedCard(rotationDeg = -8f, gradientIndex = 0) }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 26.dp, top = 12.dp)
                .floating(periodMs = 4000, amplitudeDp = 8f, reverse = true)
        ) { MiniFeedCard(rotationDeg = 7f, gradientIndex = 4) }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .floating(periodMs = 3000, amplitudeDp = 10f)
        ) { MiniFeedCard(rotationDeg = 0f, gradientIndex = 2) }
    }
}

@Composable
private fun BattleVisual() {
    Box(
        modifier = Modifier.size(width = 320.dp, height = 290.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 28.dp, top = 30.dp)
                .floating(periodMs = 4000, amplitudeDp = 8f)
        ) { MiniBattleCard(rotationDeg = -9f) }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 22.dp, top = 14.dp)
                .floating(periodMs = 3000, amplitudeDp = 8f, reverse = true)
        ) { MiniBattleCard(rotationDeg = 6f) }
        Text(
            text = "🎁",
            fontSize = 28.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 60.dp, bottom = 40.dp)
                .floating(periodMs = 2500, amplitudeDp = 10f)
        )
        Text(
            text = "💎",
            fontSize = 22.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 56.dp, bottom = 70.dp)
                .floating(periodMs = 3200, amplitudeDp = 10f, reverse = true)
        )
    }
}

@Composable
private fun WalletVisual() {
    Box(
        modifier = Modifier.size(width = 320.dp, height = 290.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .floating(periodMs = 3500, amplitudeDp = 8f)
        ) { MiniWalletCard(rotationDeg = -3f) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 30.dp)
                .floating(periodMs = 4000, amplitudeDp = 8f, reverse = true)
        ) {
            OperatorPill(
                letter = "M",
                circleColor = UnovColors.Mtn,
                letterColor = Color(0xFF0D0D0D),
                label = "MTN MoMo · prêt"
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp, top = 30.dp)
                .floating(periodMs = 3000, amplitudeDp = 8f)
        ) {
            OperatorPill(
                letter = "m",
                circleColor = UnovColors.Moov,
                letterColor = Color.White,
                label = "Moov Money"
            )
        }
    }
}

@Composable
private fun AvatarsStack() {
    val palette = listOf(
        Color(0xFFFF6A00), Color(0xFFE5722D), Color(0xFFE55F00), Color(0xFFFF944D)
    )
    Box(modifier = Modifier.size(width = 58.dp, height = 22.dp)) {
        palette.forEachIndexed { i, c ->
            Box(
                modifier = Modifier
                    .offset(x = (i * 12).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D0D0D))
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(c)
            )
        }
    }
}

private fun buildAnnotatedSocialProof(): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = UnovColors.Text, fontWeight = FontWeight.SemiBold)) {
        append("47 233 créateurs")
    }
    append(" ont déjà rejoint au Bénin")
}
