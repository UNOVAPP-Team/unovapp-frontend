package com.unovapp.android.ui.feed

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.unovapp.android.download.DownloadCenter
import com.unovapp.android.download.DownloadService
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
import com.unovapp.android.ui.theme.UnovGradients

/**
 * Feuille de partage (slide-up premium). Actions 100% fonctionnelles :
 *  - **Télécharger dans la galerie** (HLS → mp4 réel via [VideoDownloader], barre de progression),
 *  - Partager via… (feuille système Android),
 *  - Copier le lien.
 */
@Composable
fun ShareSheet(
    video: FeedVideoUi,
    onShareTracked: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scrim by animateFloatAsState(if (shown) 0.6f else 0f, tween(240), label = "shareScrim")
    val offset by animateDpAsState(
        targetValue = if (shown) 0.dp else 460.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "sharePanel"
    )
    val noRipple = remember { MutableInteractionSource() }

    // Le téléchargement vit dans un SERVICE → on observe seulement sa progression. Fermer la
    // feuille n'interrompt donc PAS le téléchargement : il continue en arrière-plan (+ notification).
    val dl by DownloadCenter.state.collectAsState()
    val downloading = dl.running && dl.videoId == video.id
    val progress = dl.progress

    fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    fun startDownload() {
        if (dl.running) { toast("Un téléchargement est déjà en cours"); return }
        if (video.shareableUrl.isBlank()) { toast("Vidéo indisponible"); return }
        DownloadService.start(context, video.id, video.shareableUrl)
        toast("Téléchargement lancé — continue en arrière-plan")
    }

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
                    .width(40.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(UnovColors.LineStrong)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Partager",
                color = UnovColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )

            // Télécharger — met en avant, avec progression réelle.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !downloading, onClick = { startDownload() })
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(UnovGradients.Gold),
                    contentAlignment = Alignment.Center
                ) {
                    if (downloading) {
                        CircularProgressIndicator(
                            color = Color(0xFF0D0D0D), strokeWidth = 2.dp, modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(Icons.Outlined.Download, "Télécharger", tint = Color(0xFF0D0D0D), modifier = Modifier.size(20.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (downloading) "Téléchargement… $progress%" else "Télécharger dans la galerie",
                        color = UnovColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                    if (downloading) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            color = UnovColors.Accent,
                            trackColor = UnovColors.Line,
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape)
                        )
                    } else {
                        Text("Enregistre la vidéo sur ton téléphone", color = UnovColors.TextMute, fontSize = 12.sp)
                    }
                }
            }

            ShareRow(Icons.AutoMirrored.Outlined.Send, "Partager via…") {
                onShareTracked()
                shareVideo(context, video)
                onDismiss()
            }
            ShareRow(Icons.Outlined.Link, "Copier le lien") {
                clipboard.setText(AnnotatedString(video.shareableUrl))
                toast("Lien copié")
                onDismiss()
            }
        }
    }
}

@Composable
private fun ShareRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = UnovColors.Text, modifier = Modifier.size(22.dp))
        Text(text = label, color = UnovColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
