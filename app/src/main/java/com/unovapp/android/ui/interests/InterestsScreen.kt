@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.unovapp.android.ui.interests

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import com.unovapp.android.ui.theme.UnovMotion

/**
 * Choix des centres d'intérêt — **étape d'onboarding affichée juste après l'inscription**.
 *
 * Comportement :
 *  - Si l'utilisateur a DÉJÀ des intérêts enregistrés (reconnexion, compte existant), l'écran
 *    s'efface tout seul → [onDone] immédiat, aucun clignotement gênant.
 *  - Pas de retour arrière (on ne revient pas à l'écran de connexion), mais un « Passer »
 *    explicite : on ne bloque jamais l'entrée dans l'app.
 *  - Minimum recommandé de 3 catégories : en dessous, le bouton propose « Passer ».
 *
 * Les catégories partent au backend (`POST /users/me/interests`) et serviront à personnaliser
 * le feed (cf. docs/BACKEND_FEED_ALGO.md, étape 3 — cold start utilisateur).
 */
@Composable
fun InterestsScreen(onDone: () -> Unit, vm: InterestsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    // Onboarding : on ne remonte pas vers l'écran d'auth.
    BackHandler { /* bloqué volontairement */ }

    // Compte existant qui a déjà choisi → on file directement au feed.
    LaunchedEffect(state.alreadySet) {
        if (state.alreadySet == true) onDone()
    }

    if (state.loading || state.alreadySet == true) {
        Box(
            modifier = Modifier.fillMaxSize().background(UnovColors.BgBase),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = UnovColors.Accent) }
        return
    }

    val enough = state.selected.size >= MIN_INTERESTS

    Column(
        modifier = Modifier.fillMaxSize().background(UnovColors.BgBase).windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(28.dp))
            Text(
                "Qu'est-ce qui te plaît ?",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp
            )
            Text(
                "Choisis au moins $MIN_INTERESTS thèmes — ton feed s'adaptera à tes goûts.",
                color = UnovColors.TextDim,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 22.dp)
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                INTEREST_CATEGORIES.forEachIndexed { index, (key, label) ->
                    val sel = key in state.selected
                    // Pop bouncy à la sélection (récompense tactile immédiate).
                    val scale by animateFloatAsState(
                        targetValue = if (sel) 1.06f else 1f,
                        animationSpec = UnovMotion.bouncy(),
                        label = "interestPop$index"
                    )
                    Row(
                        modifier = Modifier
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(RoundedCornerShape(999.dp))
                            .then(
                                if (sel) Modifier.background(UnovGradients.Gold)
                                else Modifier.background(UnovColors.Surface)
                            )
                            .border(1.dp, if (sel) Color.Transparent else UnovColors.Line, RoundedCornerShape(999.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { vm.toggle(key) }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (sel) Icon(Icons.Filled.Check, null, tint = Color(0xFF0D0D0D), modifier = Modifier.size(15.dp))
                        Text(
                            label,
                            color = if (sel) Color(0xFF0D0D0D) else UnovColors.Text,
                            fontSize = 14.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                    .then(
                        if (enough) Modifier.background(UnovGradients.Gold)
                        else Modifier.background(UnovColors.SurfaceAlt)
                    )
                    .clickable(enabled = enough && !state.saving) { vm.save(onDone) },
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.saving -> CircularProgressIndicator(
                        color = Color(0xFF0D0D0D), strokeWidth = 2.dp, modifier = Modifier.size(18.dp)
                    )
                    enough -> Text(
                        "Continuer (${state.selected.size})",
                        color = Color(0xFF0D0D0D), fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    else -> Text(
                        "Choisis ${MIN_INTERESTS - state.selected.size} thème(s) de plus",
                        color = UnovColors.TextMute, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
            // On ne bloque jamais l'entrée dans l'app.
            Text(
                "Passer",
                color = UnovColors.TextMute,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 14.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDone
                    )
            )
        }
    }
}

/** En dessous, le feed n'a pas assez de signal pour se personnaliser utilement. */
private const val MIN_INTERESTS = 3
