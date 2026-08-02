package com.unovapp.android.ui.forge

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.data.mock.MOCK_COMMANDES_COMPACTES
import com.unovapp.android.data.mock.MOCK_COMMANDE_PHARE
import com.unovapp.android.data.mock.MockCommande
import com.unovapp.android.data.mock.MockCommandeCompacte
import com.unovapp.android.ui.components.DemoBanner
import com.unovapp.android.ui.components.EmberBody
import com.unovapp.android.ui.components.EmberScreenScaffold
import com.unovapp.android.ui.theme.Ember
import com.unovapp.android.ui.theme.EmberDim
import com.unovapp.android.ui.theme.EmberFont
import com.unovapp.android.ui.theme.EmberMotion
import com.unovapp.android.ui.theme.animatedProgress
import com.unovapp.android.ui.theme.countUp
import com.unovapp.android.ui.theme.emberEnter
import com.unovapp.android.ui.theme.pressable
import com.unovapp.android.ui.theme.riseIn
import com.unovapp.android.ui.theme.staggerDelay

/**
 * « La Forge » — écran 13 : la place de marché entre marques et créateurs.
 *
 * C'est le seul écran OR de l'app : la Forge est premium, et l'or la distingue
 * de la braise (qui reste la réaction) et de la lagune (réservée à l'IA).
 *
 * ⚠️ Écran de démonstration : aucun backend de marketplace n'existe. Les
 * commandes viennent de `data/mock/MockContent.kt`. Les montants affichés ne
 * correspondent à aucun budget réel — le bandeau le signale à l'écran, parce
 * qu'on ne laisse pas quelqu'un croire qu'il peut gagner 15 000 F ce soir.
 */
@Composable
fun ForgeScreen(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    var tab by remember { mutableIntStateOf(0) }

    EmberScreenScaffold(
        title = "La Forge",
        action = {
            // Badge PREMIUM — or, glyphe de couronne.
            Row(
                modifier = Modifier
                    .emberEnter(dyFrom = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, Ember.Gold.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(Icons.Filled.WorkspacePremium, null, tint = Ember.Gold, modifier = Modifier.size(15.dp))
                Text(
                    "PREMIUM", color = Ember.Gold, fontFamily = EmberFont,
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp
                )
            }
        }
    ) {
        EmberBody(
            "Créateurs & marques",
            color = Ember.TextMute, size = 15,
            modifier = Modifier.padding(start = 20.dp, bottom = 14.dp).emberEnter(dyFrom = 8.dp)
        )

        LazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DemoBanner(
                    "Écran de démonstration — les commandes et les montants sont fictifs.",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                ForgeTabs(active = tab, onSelect = { tab = it }, modifier = Modifier.padding(horizontal = 20.dp))
            }
            item {
                // Le bandeau IA : lagune, et il se révèle en opacité seule (R5).
                LaguneMatchBanner(
                    count = 3,
                    modifier = Modifier.padding(horizontal = 20.dp).emberEnter(dyFrom = 0.dp, extraDelayMs = 120)
                )
            }
            item {
                CommandePhareCard(
                    c = MOCK_COMMANDE_PHARE,
                    modifier = Modifier.padding(horizontal = 20.dp).riseIn(delayMs = 100)
                )
            }
            itemsIndexed(MOCK_COMMANDES_COMPACTES) { index, c ->
                CommandeCompacteCard(
                    c = c,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .riseIn(delayMs = staggerDelay(index, 50) + 160, key = c.brand)
                )
            }
            item {
                PublierButton(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).riseIn(delayMs = 500))
                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

/** Onglets Commandes / Mes annonces — le curseur glisse, il ne saute pas. */
@Composable
private fun ForgeTabs(active: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ember.Surface.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("Commandes", "Mes annonces").forEachIndexed { i, label ->
            val selected = i == active
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .then(
                        if (selected) Modifier
                            .background(Ember.Braise.copy(alpha = 0.14f))
                            .border(1.dp, Ember.Braise.copy(alpha = 0.55f), RoundedCornerShape(11.dp))
                        else Modifier
                    )
                    .pressable(onClick = { onSelect(i) })
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Ember.Braise else Ember.TextDim,
                    fontFamily = EmberFont, fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/** « L'IA a trouvé N commandes faites pour toi » — lagune, donc fondu sans translation. */
@Composable
private fun LaguneMatchBanner(count: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ember.Lagune.copy(alpha = 0.05f))
            .border(1.dp, Ember.Lagune.copy(alpha = 0.3f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Ember.Lagune))
        Row {
            EmberBody("L'IA a trouvé ", color = Ember.TextDim, size = 14)
            Text(
                "$count commandes", color = Ember.Lagune, fontFamily = EmberFont,
                fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            EmberBody(" faites pour toi", color = Ember.TextDim, size = 14)
        }
    }
}

/** La commande mise en avant : brief, budget, places, match IA, garantie, action. */
@Composable
private fun CommandePhareCard(c: MockCommande, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(EmberDim.CardRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ember.Surface.copy(alpha = 0.55f))
            .border(1.dp, Ember.Gold.copy(alpha = 0.4f), shape)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BrandTile(c.initial, Color(c.brandHex))
            Column(Modifier.weight(1f)) {
                Text(
                    c.brand, color = Ember.Text, fontFamily = EmberFont,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1
                )
                EmberBody(c.sector, color = Ember.TextMute, size = 14, maxLines = 1)
            }
            if (c.open) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, Ember.Positif.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text("Ouvert", color = Ember.Positif, fontFamily = EmberFont, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        EmberBody(c.brief, color = Ember.Text, size = 17, weight = FontWeight.Medium)

        // Budget · places · délai — les trois chiffres qui décident.
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${countUp(c.budgetFcfa)} F".replace(Regex("(?<=\\d)(?=(\\d{3})+ )"), " "),
                    color = Ember.Gold, fontFamily = EmberFont,
                    fontSize = 24.sp, fontWeight = FontWeight.Black
                )
                EmberBody("/ créateur", color = Ember.TextMute, size = 13)
            }
            StatDividerV()
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${c.placesTaken} / ${c.placesTotal}", color = Ember.Text,
                    fontFamily = EmberFont, fontSize = 19.sp, fontWeight = FontWeight.Bold
                )
                EmberBody("places prises", color = Ember.TextMute, size = 13)
            }
            StatDividerV()
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${c.daysLeft} j", color = Ember.Text,
                    fontFamily = EmberFont, fontSize = 19.sp, fontWeight = FontWeight.Bold
                )
                EmberBody("restants", color = Ember.TextMute, size = 13)
            }
        }

        // Barre de places — animée comme un ringFill (§Écran 13).
        Spacer(Modifier.height(12.dp))
        val fill by animatedProgress(
            c.placesTaken.toFloat() / c.placesTotal.coerceAtLeast(1),
            durationMs = EmberMotion.DurSlow, delayMs = 200
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp)
                .clip(RoundedCornerShape(999.dp)).background(Ember.Line)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(fill).height(4.dp)
                    .clip(RoundedCornerShape(999.dp)).background(Ember.Gold)
            )
        }

        // Le score de correspondance — c'est de l'IA, donc lagune.
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Ember.Lagune.copy(alpha = 0.05f))
                .border(1.dp, Ember.Lagune.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Check, null, tint = Ember.Lagune, modifier = Modifier.size(16.dp))
            Row {
                EmberBody("Tu matches à ", color = Ember.TextDim, size = 14)
                Text(
                    "${countUp(c.matchPercent)} %", color = Ember.Lagune,
                    fontFamily = EmberFont, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
                EmberBody(" · ${c.matchReason}", color = Ember.TextDim, size = 14, maxLines = 1)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // La garantie arrive après l'offre, pas avant (§Écran 13).
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(Icons.Filled.Lock, null, tint = Ember.Gold, modifier = Modifier.size(14.dp))
                EmberBody("Budget séquestré", color = Ember.TextMute, size = 14, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ember.Gold)
                    .pressable(onClick = {})
                    .padding(horizontal = 26.dp, vertical = 14.dp)
            ) {
                Text(
                    "Se proposer", color = Color(0xFF231703), fontFamily = EmberFont,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Commande secondaire — une ligne compacte : marque, détail, budget, places. */
@Composable
private fun CommandeCompacteCard(c: MockCommandeCompacte, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(EmberDim.CardRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ember.Surface.copy(alpha = 0.5f))
            .border(1.dp, Ember.Line, shape)
            .pressable(onClick = {})
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BrandTile(c.initial, Color(c.brandHex), size = 46.dp)
        Column(Modifier.weight(1f)) {
            Text(
                c.brand, color = Ember.Text, fontFamily = EmberFont,
                fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1
            )
            EmberBody(c.detail, color = Ember.TextMute, size = 14, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${c.budgetFcfa / 1000} 000 F", color = Ember.Gold, fontFamily = EmberFont,
                fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1
            )
            EmberBody("${c.placesTaken} / ${c.placesTotal} pris", color = Ember.TextMute, size = 13, maxLines = 1)
        }
    }
}

/** Pastille de marque — carré arrondi à l'initiale, dans la couleur de la marque. */
@Composable
private fun BrandTile(initial: String, tint: Color, size: androidx.compose.ui.unit.Dp = 54.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(size / 3.6f)).background(tint),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial, color = Color.White, fontFamily = EmberFont,
            fontSize = (size.value / 2.4f).sp, fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun StatDividerV() {
    Box(Modifier.width(1.dp).height(34.dp).background(Ember.Line))
}

/** Action flottante — publier une commande (côté marque). */
@Composable
private fun PublierButton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Ember.Braise.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .pressable(onClick = {})
            .padding(vertical = 17.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, null, tint = Ember.Braise, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Publier une commande", color = Ember.Braise, fontFamily = EmberFont,
            fontSize = 16.sp, fontWeight = FontWeight.Bold
        )
    }
}
