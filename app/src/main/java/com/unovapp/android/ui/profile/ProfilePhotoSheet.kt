package com.unovapp.android.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.theme.UnovColors

/**
 * Feuille d'options « Photo de profil » (style clair façon Facebook).
 * Apparaît au tap sur l'avatar. Options : voir la photo, choisir une nouvelle photo.
 */
@Composable
fun ProfilePhotoOptionsSheet(
    visible: Boolean,
    hasPhoto: Boolean,
    onView: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    // Scrim cliquable.
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
    // Carte d'options qui glisse du bas.
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(UnovColors.BgRaised)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Poignée
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(UnovColors.LineStrong)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    "Photo de profil",
                    color = UnovColors.Text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp)
                )
                if (hasPhoto) {
                    PhotoOptionRow(Icons.Outlined.Visibility, "Voir la photo de profil") { onDismiss(); onView() }
                }
                PhotoOptionRow(Icons.Outlined.PhotoLibrary, "Choisir une nouvelle photo") { onDismiss(); onChooseFromGallery() }
                Spacer(Modifier.height(4.dp))
                // Annuler
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(UnovColors.Surface)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Annuler", color = UnovColors.TextDim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PhotoOptionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(UnovColors.Accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = UnovColors.Accent, modifier = Modifier.size(20.dp))
        }
        Text(label, color = UnovColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Visionneuse plein écran de la photo de profil. Fond noir, photo centrée, fermeture
 * par le bouton ✕ ou en touchant l'arrière-plan.
 */
@Composable
fun FullScreenPhotoViewer(
    visible: Boolean,
    avatarUrl: String?,
    fallbackName: String,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Photo de profil",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Pas de photo réelle : on agrandit l'avatar à initiales.
                Avatar(idx = 0, name = fallbackName, size = 220.dp)
            }
            // Bouton fermer (haut-gauche).
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, "Fermer", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}
