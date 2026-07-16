package com.unovapp.android.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.unovapp.android.ui.components.ShimmerBox
import com.unovapp.android.ui.theme.UnovAppTheme

/**
 * Squelette de chargement du feed — affiché uniquement pendant le tout premier chargement
 * (pas de cache disque, réseau en cours). Mime la structure d'une page vidéo : rail d'actions
 * à droite, bloc infos en bas — l'utilisateur comprend instantanément ce qui arrive.
 */
@Composable
fun FeedPlaceholder(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    UnovAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
                .padding(contentPadding)
        ) {
            // Rail d'actions fantôme (droite)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp, top = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                ShimmerBox(modifier = Modifier.size(52.dp), shape = CircleShape)
                repeat(4) {
                    ShimmerBox(modifier = Modifier.size(34.dp), shape = CircleShape)
                }
            }
            // Bloc infos fantôme (bas-gauche)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, end = 96.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier.width(140.dp).height(18.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth().height(14.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth(0.6f).height(14.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                ShimmerBox(
                    modifier = Modifier.width(180.dp).height(12.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }
    }
}
