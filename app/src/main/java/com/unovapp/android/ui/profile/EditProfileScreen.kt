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
import coil.compose.AsyncImage
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

/**
 * Écran d'édition du profil. Les champs texte (display_name, username, bio) sont enregistrés
 * via PATCH /users/{id}. L'upload de photo (avatar/couverture) est désactivé tant que le
 * stockage S3 n'est pas configuré côté backend.
 */
@Composable
fun EditProfileScreen(
    initialDisplayName: String,
    initialUsername: String,
    initialBio: String,
    avatarUrl: String?,
    saving: Boolean,
    error: String?,
    onSave: (displayName: String, username: String, bio: String) -> Unit,
    onDismiss: () -> Unit
) {
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var username by remember { mutableStateOf(initialUsername.filter { it != '@' }) }
    var bio by remember { mutableStateOf(initialBio) }

    val usernameValid = username.length in 3..20
    val canSave = !saving && usernameValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
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

        // Avatar (photo) — upload bientôt disponible
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
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
                    }
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(UnovGradients.Gold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = Color(0xFF0D0D0D),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Photo de profil et couverture : bientôt",
                    color = UnovColors.TextMute,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditField(label = "NOM AFFICHÉ", value = displayName, onValueChange = { displayName = it.take(40) })
            EditField(
                label = "PSEUDO",
                value = username,
                onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(20) },
                helper = if (username.isNotEmpty() && !usernameValid) "Entre 3 et 20 caractères" else "@$username"
            )
            EditField(
                label = "BIO",
                value = bio,
                onValueChange = { bio = it.take(160) },
                singleLine = false,
                helper = "${bio.length}/160"
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
    helper: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
