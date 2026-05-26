package com.unovapp.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

@Composable
private fun VideoBg(gradientIndex: Int, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    Box(
        modifier = modifier
            .background(UnovGradients.videoBg(gradientIndex))
    ) {
        content()
    }
}

@Composable
fun MiniFeedCard(rotationDeg: Float = -6f, gradientIndex: Int = 0) {
    Box(
        modifier = Modifier
            .size(width = 168.dp, height = 232.dp)
            .rotate(rotationDeg)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0A0A0A))
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
    ) {
        VideoBg(gradientIndex, modifier = Modifier.fillMaxSize()) {
            // bottom gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )

            // "POUR TOI" pill (top-left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovColors.Accent.copy(alpha = 0.92f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "POUR TOI",
                    color = Color(0xFF0D0D0D),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                )
            }

            // Right-side action buttons (3 circles)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp, bottom = 50.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262626).copy(alpha = 0.7f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    )
                }
            }

            // Bottom username + lines
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = 50.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "@aminata.cot",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }
        }
    }
}

@Composable
fun MiniBattleCard(rotationDeg: Float = 4f) {
    Box(
        modifier = Modifier
            .size(width = 158.dp, height = 218.dp)
            .rotate(rotationDeg)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A))
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
    ) {
        // top half / bottom half videos
        Column(modifier = Modifier.fillMaxSize()) {
            VideoBg(3, modifier = Modifier.fillMaxWidth().height(109.dp))
            VideoBg(5, modifier = Modifier.fillMaxWidth().height(109.dp))
        }

        // mid-line accent
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(1.5.dp)
                .background(UnovColors.Accent.copy(alpha = 0.6f))
        )

        // VS badge
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A0A0A))
                .border(1.5.dp, UnovColors.Accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "VS",
                color = UnovColors.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }

        // vote bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .fillMaxSize()
                    .background(UnovGradients.Gold)
            )
        }
    }
}

@Composable
fun MiniWalletCard(rotationDeg: Float = -3f) {
    Box(
        modifier = Modifier
            .size(width = 200.dp, height = 122.dp)
            .rotate(rotationDeg)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(UnovColors.Accent, UnovColors.AccentDeep)
                )
            )
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
    ) {
        // decorative dark circle that pokes out top-right (clipped by parent)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Text(
                text = "SOLDE",
                color = Color(0xFF0D0D0D).copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp
            )
            Text(
                text = "1 240",
                color = Color(0xFF0D0D0D),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "jetons · ≈ 6 200 FCFA",
                color = Color(0xFF0D0D0D).copy(alpha = 0.85f),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // MoMo pill bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.18f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "MoMo",
                color = Color(0xFF0D0D0D),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OperatorPill(letter: String, circleColor: Color, letterColor: Color, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF141414).copy(alpha = 0.95f))
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
            .padding(start = 8.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                color = letterColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
