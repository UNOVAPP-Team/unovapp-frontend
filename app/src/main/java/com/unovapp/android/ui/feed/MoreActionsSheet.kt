package com.unovapp.android.ui.feed

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors
import android.widget.Toast

/**
 * Feuille d'options "..." du feed (slide-up premium). Chaque action est fonctionnelle :
 * copier le lien, partager, enregistrer, pas intéressé, signaler.
 */
@Composable
fun MoreActionsSheet(
    video: FeedVideoUi,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scrim by animateFloatAsState(if (shown) 0.6f else 0f, tween(240), label = "moreScrim")
    val offset by animateDpAsState(
        targetValue = if (shown) 0.dp else 520.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "morePanel"
    )
    val noRipple = remember { MutableInteractionSource() }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrim))
                .clickable(interactionSource = noRipple, indication = null, onClick = onDismiss)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = offset)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF161616), Color(0xFF0A0A0A))))
                .clickable(interactionSource = noRipple, indication = null, onClick = {})
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 26.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(UnovColors.LineStrong)
            )
            Spacer(Modifier.height(14.dp))

            MoreRow(Icons.Outlined.Link, "Copier le lien") {
                clipboard.setText(AnnotatedString(video.hlsUrl))
                toast("Lien copié")
                onDismiss()
            }
            MoreRow(Icons.AutoMirrored.Outlined.Send, "Partager") {
                onDismiss()
                shareVideo(context, video)
            }
            MoreRow(Icons.Outlined.BookmarkBorder, "Enregistrer") {
                toast("Vidéo enregistrée")
                onDismiss()
            }
            MoreRow(Icons.Outlined.VisibilityOff, "Pas intéressé") {
                toast("On t'en montrera moins")
                onDismiss()
            }
            MoreRow(Icons.Outlined.Flag, "Signaler", danger = true) {
                toast("Signalement envoyé")
                onDismiss()
            }
        }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (danger) UnovColors.Danger else UnovColors.Text,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (danger) UnovColors.Danger else UnovColors.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
