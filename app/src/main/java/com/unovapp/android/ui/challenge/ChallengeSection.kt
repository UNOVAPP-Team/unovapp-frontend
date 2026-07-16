package com.unovapp.android.ui.challenge

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unovapp.android.ui.components.unovTap
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

/**
 * Section « Challenges » du profil (onglet dédié) — reprise de la maquette :
 *  - une carte CTA dorée « Créez votre propre challenge » avec médaille animée,
 *  - le carrousel horizontal « Mes challenges » (badge Actif + participants).
 */
@Composable
fun ChallengeSection(
    challenges: List<ChallengeCard>,
    onCreate: () -> Unit,
    onOpenChallenge: (ChallengeCard) -> Unit = {},
    onSeeAll: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CreateChallengeCta(onCreate = onCreate)

        if (challenges.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Mes challenges",
                    color = UnovColors.Text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.unovTap(onClick = onSeeAll, pressedScale = 0.94f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Voir tout", color = UnovColors.TextMute, fontSize = 13.sp)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = UnovColors.TextMute,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(challenges, key = { it.id }) { c ->
                    ChallengeCardView(card = c, onClick = { onOpenChallenge(c) })
                }
            }
        } else {
            // Aucun challenge encore créé — message honnête, pas de fausses cartes.
            Text(
                text = "Tu n'as pas encore créé de challenge.\nLance le premier et défie la communauté !",
                color = UnovColors.TextMute,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Carte d'appel à l'action — bordure dorée, médaille qui pulse doucement. */
@Composable
private fun CreateChallengeCta(onCreate: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "ctaGlow").animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "ctaGlowV"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        UnovColors.Accent.copy(alpha = 0.14f),
                        UnovColors.Accent.copy(alpha = 0.04f)
                    )
                )
            )
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Médaille : halo doré qui respire (signature de la section).
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = glow; scaleY = glow; alpha = 1.15f - glow }
                    .background(
                        Brush.radialGradient(
                            listOf(UnovColors.AccentGlowStrong, Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Icon(
                Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = UnovColors.Accent,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Créez votre propre challenge",
                color = UnovColors.Accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Défiez la communauté avec vos idées",
                color = UnovColors.TextDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovGradients.Gold)
                    .unovTap(onClick = onCreate, pressedScale = 0.94f)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    "Créer un challenge",
                    color = Color(0xFF0D0D0D),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Vignette d'un challenge : couverture, badge « Actif », hashtag, participants. */
@Composable
private fun ChallengeCardView(card: ChallengeCard, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(132.dp)
            .height(176.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(UnovGradients.videoBg(kotlin.math.abs(card.id.hashCode()) % 6))
            .unovTap(onClick = onClick, pressedScale = 0.95f)
    ) {
        if (!card.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = card.coverUrl,
                contentDescription = card.hashtag,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Scrim bas — lisibilité du hashtag et du compteur.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(78.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.82f)
                    )
                )
        )
        if (card.isActive) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(UnovColors.Success)
                )
                Text("Actif", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(9.dp)
        ) {
            Text(
                card.hashtag,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                "${card.participantsFmt} participants",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
