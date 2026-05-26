package com.unovapp.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

@Composable
fun GoldPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val effectiveEnabled = enabled && !isLoading

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (effectiveEnabled) 1f else 0.45f)
            .clip(RoundedCornerShape(14.dp))
            .background(UnovGradients.Gold)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = effectiveEnabled,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = text,
                color = Color(0xFF0D0D0D),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFF0D0D0D),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                trailingIcon?.invoke()
            }
        }
    }
}

@Composable
fun OutlineCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(PaddingValues(0.dp)),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun GhostPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = UnovColors.TextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.9.sp
        )
    }
}
