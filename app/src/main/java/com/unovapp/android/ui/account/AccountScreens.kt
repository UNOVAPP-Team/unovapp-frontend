package com.unovapp.android.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

/* ============================ Éléments communs ============================ */

@Composable
private fun AccountScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    BackHandler { onBack() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UnovColors.BgBase)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(UnovColors.Surface).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White, modifier = Modifier.size(18.dp)) }
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) UnovGradients.Gold else androidx.compose.ui.graphics.Brush.linearGradient(listOf(UnovColors.Surface, UnovColors.Surface)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) CircularProgressIndicator(color = Color(0xFF0D0D0D), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        else Text(text, color = if (enabled) Color(0xFF0D0D0D) else UnovColors.TextMute, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AccountField(label: String, value: String, onChange: (String) -> Unit, keyboard: KeyboardType) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = UnovColors.TextMute, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = SolidColor(UnovColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(UnovColors.Surface)
                .border(1.dp, UnovColors.Line, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        )
    }
}

/* ============================ Sessions / appareils ============================ */

@Composable
fun SessionsScreen(onBack: () -> Unit, vm: AccountViewModel = hiltViewModel()) {
    val state by vm.sessions.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadSessions() }

    AccountScaffold("Appareils connectés", onBack) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = UnovColors.Accent) }
            state.sessions.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(state.error ?: "Aucune session active", color = UnovColors.TextDim, fontSize = 14.sp)
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    Text(
                        "Ces appareils sont connectés à ton compte. Révoque ceux que tu ne reconnais pas.",
                        color = UnovColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                items(state.sessions, key = { it.id }) { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth().padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp)).background(UnovColors.Surface)
                            .border(1.dp, if (s.isCurrent) UnovColors.Accent.copy(alpha = 0.4f) else UnovColors.Line, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Smartphone, null, tint = UnovColors.Accent, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.deviceInfo ?: "Appareil inconnu", color = UnovColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                if (s.isCurrent) "Cet appareil · actif maintenant" else "Dernière activité : ${(s.lastUsedAt ?: "").take(10)}",
                                color = UnovColors.TextMute, fontSize = 11.sp
                            )
                        }
                        if (s.isCurrent) {
                            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(UnovColors.Accent.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("Actuel", color = UnovColors.Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                "Révoquer", color = UnovColors.Danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.revokeSession(s.id) }.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

/* ============================ Changement d'email ============================ */

@Composable
fun ChangeEmailScreen(onBack: () -> Unit, vm: AccountViewModel = hiltViewModel()) {
    val state by vm.changeEmail.collectAsStateWithLifecycle()
    var newEmail by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.resetChangeEmail() }
    LaunchedEffect(state.done) { if (state.done) onBack() }

    AccountScaffold("Changer d'email", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.step == 1) {
                Text("On enverra un code de vérification à ta nouvelle adresse pour confirmer que c'est bien la tienne.", color = UnovColors.TextDim, fontSize = 13.sp, lineHeight = 19.sp)
                AccountField("NOUVEL EMAIL", newEmail, { newEmail = it.trim() }, KeyboardType.Email)
                PrimaryButton("Envoyer le code", enabled = newEmail.contains("@") && !state.loading, loading = state.loading) {
                    vm.requestEmailChange(newEmail)
                }
            } else {
                Text("Entre le code à 6 chiffres envoyé à ${state.newEmail}.", color = UnovColors.TextDim, fontSize = 13.sp)
                AccountField("CODE À 6 CHIFFRES", otp, { otp = it.filter(Char::isDigit).take(6) }, KeyboardType.Number)
                PrimaryButton("Confirmer", enabled = otp.length == 6 && !state.loading, loading = state.loading) {
                    vm.confirmEmailChange(otp)
                }
            }
            if (state.error != null) Text(state.error!!, color = UnovColors.Danger, fontSize = 12.sp)
        }
    }
}

/* ============================ Suppression de compte (RGPD) ============================ */

@Composable
fun DeleteAccountScreen(onBack: () -> Unit, onDeleted: () -> Unit, vm: AccountViewModel = hiltViewModel()) {
    val deleting by vm.deleting.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf("") }
    val canDelete = confirm.trim().equals("SUPPRIMER", ignoreCase = true) && !deleting

    AccountScaffold("Supprimer mon compte", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(UnovColors.Danger.copy(alpha = 0.10f)).border(1.dp, UnovColors.Danger.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).padding(14.dp)) {
                Text(
                    "⚠️ Action irréversible. Ton compte, tes vidéos, tes abonnements et tes données seront supprimés/anonymisés. Tu ne pourras plus te reconnecter avec ce compte.",
                    color = UnovColors.Text, fontSize = 13.sp, lineHeight = 19.sp
                )
            }
            Text("Pour confirmer, tape SUPPRIMER ci-dessous.", color = UnovColors.TextDim, fontSize = 13.sp)
            AccountField("CONFIRMATION", confirm, { confirm = it }, KeyboardType.Text)
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (canDelete) UnovColors.Danger else UnovColors.Surface)
                    .clickable(enabled = canDelete) { vm.deleteAccount(onDeleted) },
                contentAlignment = Alignment.Center
            ) {
                if (deleting) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("Supprimer définitivement mon compte", color = if (canDelete) Color.White else UnovColors.TextMute, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

