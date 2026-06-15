package com.unovapp.android.ui.feed

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors

/**
 * Qualité réseau perçue par le client. À brancher sur `ConnectivityManager` +
 * `NetworkCapabilities.LINK_DOWNSTREAM_BANDWIDTH_KBPS` quand le module monitoring sera prêt.
 */
enum class NetworkQuality(val short: String, val color: Color) {
    G2("2G", Color(0xFFFF5252)),
    G3("3G", Color(0xFFFFA726)),
    G4("4G", Color(0xFF66BB6A)),
    Wifi("WIFI", UnovColors.Accent)
}

/**
 * Pill réseau intelligent : indique la qualité de la connexion + l'état du mode économie data.
 * Cliquable → ouvre la feuille de réglages data (à brancher).
 *
 * En contexte africain, c'est l'information la plus utile à afficher en permanence :
 *  - Sur 2G, l'utilisateur s'attend à une qualité dégradée → on l'annonce franchement.
 *  - Le mode ECO est l'engagement implicite de l'app à protéger les Mo facturés.
 */
@Composable
fun NetworkQualityChip(
    quality: NetworkQuality,
    ecoActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noRipple = remember { MutableInteractionSource() }
    val transition = rememberInfiniteTransition(label = "netPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (quality == NetworkQuality.G2) 700 else 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "netAlpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF0A0A0A).copy(alpha = 0.78f))
            .border(1.dp, quality.color.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick)
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(quality.color.copy(alpha = pulseAlpha))
        )
        Text(
            text = quality.short,
            color = quality.color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        if (ecoActive) {
            Box(
                modifier = Modifier
                    .size(2.dp)
                    .clip(CircleShape)
                    .background(UnovColors.TextMute)
            )
            Text(
                text = "ÉCO",
                color = UnovColors.Accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
        }
    }
}
