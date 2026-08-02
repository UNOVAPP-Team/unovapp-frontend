package com.unovapp.android.ui.cercles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.data.mock.MOCK_CERCLE_PROCHE
import com.unovapp.android.data.mock.MOCK_NOUVELLES_VOIX
import com.unovapp.android.data.mock.MockCercle
import com.unovapp.android.ui.components.DemoBanner
import com.unovapp.android.ui.components.EmberBody
import com.unovapp.android.ui.components.EmberEyebrow
import com.unovapp.android.ui.components.EmberScreenScaffold
import com.unovapp.android.ui.theme.Ember
import com.unovapp.android.ui.theme.EmberDim
import com.unovapp.android.ui.theme.EmberFont
import com.unovapp.android.ui.theme.breathe
import com.unovapp.android.ui.theme.emberEnter
import com.unovapp.android.ui.theme.pressable
import com.unovapp.android.ui.theme.riseIn
import com.unovapp.android.ui.theme.staggerDelay

/**
 * « Cercles » — écran 08 de la maquette : la messagerie entre fidèles.
 *
 * Deux sections, et l'ordre compte : d'abord le **Cercle proche** (ceux avec qui
 * la chaleur circule déjà), puis le **sas des Nouvelles voix** — les inconnus
 * arrivent en dernier, par construction, jamais en tête de liste.
 *
 * ⚠️ Écran de démonstration : aucun backend de messagerie n'existe. Les
 * conversations viennent de `data/mock/MockContent.kt`, et le bandeau le dit à
 * l'utilisateur plutôt que de le laisser croire à de vrais messages.
 */
@Composable
fun CerclesScreen(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    EmberScreenScaffold(title = "Cercles") {
        LazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                DemoBanner(
                    "Écran de démonstration — la messagerie n'est pas encore branchée.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            item {
                EmberEyebrow(
                    "Cercle proche", muted = true,
                    modifier = Modifier.padding(start = 22.dp, top = 8.dp, bottom = 2.dp).emberEnter(dyFrom = 8.dp)
                )
            }
            itemsIndexed(MOCK_CERCLE_PROCHE) { index, c ->
                CercleRow(
                    cercle = c,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .riseIn(delayMs = staggerDelay(index), key = c.name)
                )
            }
            item {
                EmberEyebrow(
                    "Nouvelles voix", muted = true,
                    modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 2.dp).emberEnter(dyFrom = 8.dp)
                )
            }
            item {
                NouvellesVoixSas(
                    modifier = Modifier.padding(horizontal = 20.dp).riseIn(delayMs = 300)
                )
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

/** Une conversation du cercle proche : anneau de chaleur, nom, braises, dernier message. */
@Composable
private fun CercleRow(cercle: MockCercle, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(EmberDim.CardRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ember.Surface.copy(alpha = 0.55f))
            .border(1.dp, Ember.Line, shape)
            .pressable(onClick = {})
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HeatRingInitial(initial = cercle.initial, ring = Color(cercle.ringHex))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    cercle.name, color = Ember.Text, fontFamily = EmberFont,
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1
                )
                // La chaleur du lien, en braises échangées.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(
                        Icons.Filled.LocalFireDepartment, null,
                        tint = Ember.Braise, modifier = Modifier.size(13.dp)
                    )
                    Text(
                        "${cercle.braises} j", color = Ember.Braise, fontFamily = EmberFont,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            EmberBody(cercle.lastMessage, color = Ember.TextDim, size = 14, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
        }
        if (cercle.unread) {
            // Le seul élément qui réclame de l'attention (§Écran 08).
            Box(
                Modifier.size(10.dp).breathe(amplitude = 1.08f, minAlpha = 0.7f)
                    .clip(CircleShape).background(Ember.Braise)
            )
        } else if (cercle.time.isNotBlank()) {
            EmberBody(cercle.time, color = Ember.TextMute, size = 13)
        }
    }
}

/** Avatar-initiale cerclé d'un arc de chaleur, propre au contact. */
@Composable
private fun HeatRingInitial(initial: String, ring: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(52.dp)) {
            val st = 2.5.dp.toPx()
            drawArc(
                color = ring, startAngle = -215f, sweepAngle = 250f, useCenter = false,
                style = Stroke(st, cap = StrokeCap.Round),
                topLeft = Offset(st / 2, st / 2),
                size = Size(size.width - st, size.height - st)
            )
        }
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(ring.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = ring, fontFamily = EmberFont, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Le sas des « Nouvelles voix ». Bordure pointillée : ce n'est pas encore ton
 * cercle. Pas de marching ants — « ça agite sans informer » (§Écran 08).
 */
@Composable
private fun NouvellesVoixSas(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(EmberDim.CardRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Ember.Line, shape)
            .pressable(onClick = {})
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pile d'initiales, légèrement chevauchées.
        Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
            MOCK_NOUVELLES_VOIX.initials.forEachIndexed { i, letter ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (i == 0) Color(0xFF33200E) else Color(0xFF17301B))
                        .border(2.dp, Ember.Bg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letter,
                        color = if (i == 0) Ember.BraiseClair else Ember.Positif,
                        fontFamily = EmberFont, fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        EmberBody(
            "${MOCK_NOUVELLES_VOIX.count} personnes hors de ton cercle veulent te parler",
            color = Ember.TextDim, size = 15, modifier = Modifier.weight(1f)
        )
        Text(
            "Écouter", color = Ember.Braise, fontFamily = EmberFont,
            fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1
        )
    }
}
