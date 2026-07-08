package com.unovapp.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.runtime.LaunchedEffect
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.components.enterFadeSlide
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

/**
 * Écran d'édition du profil. Les champs texte (display_name, username, bio) sont enregistrés
 * via PATCH /users/{id}. L'avatar est uploadé via le flux S3 presign/confirm
 * (POST /users/me/avatar/presign → PUT S3 → PUT /users/me/avatar).
 */
@Composable
fun EditProfileScreen(
    initialDisplayName: String,
    initialUsername: String,
    initialBio: String,
    avatarUrl: String?,
    saving: Boolean,
    error: String?,
    avatarUploading: Boolean = false,
    avatarError: String? = null,
    onAvatarSelected: (contentType: String, uri: Uri) -> Unit = { _, _ -> },
    onSave: (displayName: String, username: String, bio: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Sélecteur d'image restreint aux formats acceptés par le backend (JPEG/PNG/WebP).
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
            onAvatarSelected(contentType, uri)
        }
    }
    val openAvatarPicker = { pickAvatar.launch(arrayOf("image/jpeg", "image/png", "image/webp")) }
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var username by remember { mutableStateOf(initialUsername.filter { it != '@' }) }
    var bio by remember { mutableStateOf(initialBio) }

    val usernameValid = username.length in 3..20
    val canSave = !saving && usernameValid

    // Déclenche la cascade d'entrée (fade + slide) de chaque élément à l'ouverture.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    BackHandler { onDismiss() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleIcon(Icons.Filled.Close, "Fermer", onClick = onDismiss)
            Text(
                text = "Modifier le profil",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            // Bouton Enregistrer
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canSave) UnovGradients.Gold else androidx.compose.ui.graphics.Brush.linearGradient(listOf(UnovColors.Surface, UnovColors.Surface)))
                    .clickable(enabled = canSave) { onSave(displayName.trim(), username.trim(), bio.trim()) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        color = Color(0xFF0D0D0D),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "Enregistrer",
                        color = if (canSave) Color(0xFF0D0D0D) else UnovColors.TextMute,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Avatar (photo de profil) — tap pour choisir une image → upload S3.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .enterFadeSlide(appeared, delayMillis = 60)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Pas de clip circulaire ici : sinon le badge appareil-photo (en bas-droite,
                // sur le bord du cercle) est rogné. Le conteneur enveloppe l'avatar + le badge.
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !avatarUploading
                    ) { openAvatarPicker() }
                ) {
                    Box(
                        modifier = Modifier.size(92.dp).clip(CircleShape).background(Color(0xFF141414)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(92.dp).clip(CircleShape)
                            )
                        } else {
                            Avatar(idx = 0, name = displayName.ifBlank { username }, size = 92.dp)
                        }
                        // Overlay de progression pendant l'upload.
                        if (avatarUploading) {
                            Box(
                                modifier = Modifier.size(92.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = UnovColors.Accent,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    // Halo sombre (découpe nette) + pastille dorée + icône → badge net et visible.
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF141414)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(27.dp).clip(CircleShape).background(UnovGradients.Gold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = "Changer la photo de profil",
                                tint = Color(0xFF0D0D0D),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when {
                        avatarUploading -> "Envoi de la photo…"
                        avatarError != null -> avatarError
                        else -> "Touche la photo pour la changer"
                    },
                    color = if (avatarError != null && !avatarUploading) UnovColors.Danger else UnovColors.TextMute,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditField(
                label = "NOM AFFICHÉ",
                value = displayName,
                onValueChange = { displayName = it.take(40) },
                modifier = Modifier.fillMaxWidth().enterFadeSlide(appeared, delayMillis = 140)
            )
            EditField(
                label = "PSEUDO",
                value = username,
                onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(20) },
                helper = if (username.isNotEmpty() && !usernameValid) "Entre 3 et 20 caractères" else "@$username",
                modifier = Modifier.fillMaxWidth().enterFadeSlide(appeared, delayMillis = 210)
            )
            EditField(
                label = "BIO",
                value = bio,
                onValueChange = { bio = it.take(160) },
                singleLine = false,
                helper = "${bio.length}/160",
                modifier = Modifier.fillMaxWidth().enterFadeSlide(appeared, delayMillis = 280)
            )

            if (error != null) {
                Text(text = error, color = UnovColors.Danger, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    helper: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = UnovColors.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = SolidColor(UnovColors.Accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 0.dp else 80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(UnovColors.Surface)
                .border(1.dp, UnovColors.Line, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp)
        )
        if (helper != null) {
            Text(text = helper, color = UnovColors.TextMute, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cd: String,
    onClick: () -> Unit
) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(UnovColors.Surface)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = cd, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
