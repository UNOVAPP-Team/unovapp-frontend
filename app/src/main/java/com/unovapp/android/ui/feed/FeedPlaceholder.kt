package com.unovapp.android.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors

@Composable
fun FeedPlaceholder(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    UnovAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050505))
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "FEED",
                    color = UnovColors.Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.4.sp
                )
                Text(
                    text = "À venir au Mois 2.",
                    color = UnovColors.Text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.8).sp
                )
                Text(
                    text = "Auth réussie ✓ — pipeline prêt pour le service vidéo.",
                    color = UnovColors.TextDim,
                    fontSize = 13.sp
                )
            }
        }
    }
}
