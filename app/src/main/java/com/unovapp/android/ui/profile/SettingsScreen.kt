package com.unovapp.android.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WorkspacePremium
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.BuildConfig
import com.unovapp.android.ui.components.LanguagePickerSheet
import com.unovapp.android.ui.theme.UnovColors

// TODO: remplacer par les URLs réelles où sont hébergées les pages de docs/ (lien Play Store).
private const val URL_PRIVACY = "https://unovapp.com/privacy.html"
private const val URL_TERMS = "https://unovapp.com/terms.html"
private const val URL_CHILD_SAFETY = "https://unovapp.com/child-safety.html"
private const val URL_ACCOUNT_DELETION = "https://unovapp.com/account-deletion.html"

/**
 * « Paramètres et confidentialité » — hub de réglages du profil personnel (équivalent TikTok).
 * Overlay plein écran ouvert depuis le ⋮ du profil.
 *
 * v1 content-aware : on ne câble que ce qui existe réellement (édition profil, portefeuille,
 * langue, liens légaux, suppression de compte, déconnexion). Les sections sans backend sont
 * affichées en lecture seule avec un badge « Bientôt » plutôt que masquées — l'utilisateur
 * voit la trajectoire produit sans tomber sur un écran mort.
 */
@Composable
fun SettingsScreen(
    tier: String,
    onClose: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenWallet: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var langPickerOpen by remember { mutableStateOf(false) }

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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onClick = onClose)
            Text(
                text = "Paramètres et confidentialité",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(4.dp))

        // ----- Compte -----
        SettingsGroup("Compte") {
            SettingRow(Icons.Outlined.Edit, "Modifier le profil", onClick = onEditProfile)
            SettingRow(
                Icons.Outlined.AccountBalanceWallet,
                "Portefeuille & Mobile Money",
                subtitle = "Jetons, recharge, retrait",
                onClick = onOpenWallet
            )
            SettingRow(
                Icons.Outlined.WorkspacePremium,
                "Abonnement",
                subtitle = tier,
                soon = true
            )
        }

        // ----- Créateur -----
        SettingsGroup("Créateur") {
            SettingRow(Icons.Outlined.BarChart, "Studio créateur", subtitle = "Statistiques & revenus", soon = true)
            SettingRow(Icons.Outlined.LiveTv, "Lives & Battles", soon = true)
        }

        // ----- Confidentialité & sécurité -----
        SettingsGroup("Confidentialité & sécurité") {
            SettingRow(Icons.Outlined.Lock, "Compte privé", soon = true)
            SettingRow(Icons.Outlined.Forum, "Commentaires, cadeaux, duos", soon = true)
            SettingRow(Icons.Outlined.Block, "Comptes bloqués", soon = true)
            SettingRow(Icons.Outlined.Devices, "Appareils connectés", soon = true)
        }

        // ----- Préférences -----
        SettingsGroup("Préférences") {
            SettingRow(Icons.Outlined.Language, "Langue de l'app", onClick = { langPickerOpen = true })
            SettingRow(Icons.Outlined.DataUsage, "Économiseur de données", subtitle = "Optimisé 2G/3G", soon = true)
            SettingRow(Icons.Outlined.Notifications, "Notifications", soon = true)
            SettingRow(Icons.Outlined.DarkMode, "Thème", subtitle = "Sombre", soon = true)
        }

        // ----- Support & légal -----
        SettingsGroup("Support & légal") {
            SettingRow(Icons.Outlined.HelpOutline, "Aide & assistance", soon = true)
            SettingRow(Icons.Outlined.PrivacyTip, "Confidentialité", onClick = { openUrl(context, URL_PRIVACY) })
            SettingRow(Icons.Outlined.Description, "Conditions d'utilisation", onClick = { openUrl(context, URL_TERMS) })
            SettingRow(Icons.Outlined.Shield, "Sécurité des enfants", onClick = { openUrl(context, URL_CHILD_SAFETY) })
            SettingRow(
                Icons.Outlined.DeleteOutline,
                "Supprimer mon compte",
                danger = true,
                onClick = { openUrl(context, URL_ACCOUNT_DELETION) }
            )
        }

        Spacer(Modifier.height(20.dp))

        // ----- Déconnexion -----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, UnovColors.Line, RoundedCornerShape(14.dp))
                .clickable(onClick = onLogout)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = UnovColors.Danger,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Se déconnecter",
                color = UnovColors.Danger,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "UNOVAPP · v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            color = UnovColors.TextMute,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            fontWeight = FontWeight.Medium
        )
    }

    if (langPickerOpen) {
        LanguagePickerSheet(onDismiss = { langPickerOpen = false })
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        color = UnovColors.TextMute,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.6.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.Line, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    danger: Boolean = false,
    soon: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val noRipple = remember { MutableInteractionSource() }
    val enabled = onClick != null && !soon
    val tint = when {
        danger -> UnovColors.Danger
        else -> UnovColors.Text
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable(interactionSource = noRipple, indication = null) { onClick!!() }
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (soon) UnovColors.TextMute else tint,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (soon) UnovColors.TextDim else tint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = UnovColors.TextMute,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (soon) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovColors.SurfaceAlt)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Bientôt",
                    color = UnovColors.TextMute,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )
            }
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = UnovColors.TextMute,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, cd: String, onClick: () -> Unit) {
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

/** Ouvre une URL externe (navigateur / Custom Tab). No-op silencieux si aucune app ne peut l'ouvrir. */
private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
