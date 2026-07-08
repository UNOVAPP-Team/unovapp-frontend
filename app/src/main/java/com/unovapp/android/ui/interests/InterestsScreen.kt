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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

@Composable
fun InterestsScreen(onDone: () -> Unit, vm: InterestsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler { onDone() }

    Column(
        modifier = Modifier.fillMaxSize().background(UnovColors.BgBase).windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(UnovColors.Surface).clickable(onClick = onDone),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White, modifier = Modifier.size(18.dp)) }
            Text("Centres d'intérêt", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            Text(
                "Choisis ce que tu aimes — on personnalisera ton feed. (jusqu'à 10)",
                color = UnovColors.TextDim, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 16.dp)
            )
            if (state.loading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = UnovColors.Accent) }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    INTEREST_CATEGORIES.forEach { (key, label) ->
                        val sel = key in state.selected
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (sel) UnovGradients.Gold else androidx.compose.ui.graphics.Brush.linearGradient(listOf(UnovColors.Surface, UnovColors.Surface)))
                                .border(1.dp, if (sel) Color.Transparent else UnovColors.Line, RoundedCornerShape(999.dp))
                                .clickable { vm.toggle(key) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (sel) Icon(Icons.Filled.Check, null, tint = Color(0xFF0D0D0D), modifier = Modifier.size(15.dp))
                            Text(label, color = if (sel) Color(0xFF0D0D0D) else UnovColors.Text, fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Bouton enregistrer
        Box(Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).background(UnovGradients.Gold)
                    .clickable(enabled = !state.saving) { vm.save(onDone) },
                contentAlignment = Alignment.Center
            ) {
                if (state.saving) CircularProgressIndicator(color = Color(0xFF0D0D0D), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("Enregistrer (${state.selected.size})", color = Color(0xFF0D0D0D), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
