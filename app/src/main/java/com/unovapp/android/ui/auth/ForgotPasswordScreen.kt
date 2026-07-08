package com.unovapp.android.ui.auth

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

/**
 * Flux mot de passe oublié (OTP email, en 1 étape pour la réinit) :
 *  1. Email → forgot-password (le backend envoie un code OTP 6 chiffres par email).
 *  2. Email + code OTP + nouveau mot de passe → reset-password → retour à la connexion.
 */
@Composable
fun ForgotPasswordScreen(
    initialEmail: String,
    onForgot: (email: String, onDone: (String?) -> Unit) -> Unit,
    onReset: (email: String, otpCode: String, newPassword: String, onDone: (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    val emailValid = email.contains("@") && email.contains(".")
    val resetValid = otpCode.length == 6 && newPassword.length >= 8

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(UnovColors.Surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Mot de passe\noublié ?",
            color = UnovColors.Text,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp
        )
        Text(
            text = if (!sent)
                "Entre ton email : on t'enverra un code/lien pour réinitialiser ton mot de passe."
            else
                "Entre le code reçu par email et choisis un nouveau mot de passe.",
            color = UnovColors.TextDim,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(Modifier.height(28.dp))

        if (!sent) {
            Field("EMAIL", email, { email = it.trim(); error = null }, KeyboardType.Email)
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "Envoyer le lien",
                enabled = emailValid && !loading,
                loading = loading
            ) {
                loading = true; error = null
                onForgot(email) { err ->
                    loading = false
                    if (err == null) { sent = true; info = "Code/lien envoyé à $email." }
                    else error = err
                }
            }
        } else {
            info?.let {
                Text(it, color = UnovColors.Accent, fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
            }
            Field("CODE À 6 CHIFFRES REÇU PAR EMAIL", otpCode, { otpCode = it.filter(Char::isDigit).take(6); error = null }, KeyboardType.Number, helper = "Code de réinitialisation (valable 15 min)")
            Spacer(Modifier.height(16.dp))
            Field("NOUVEAU MOT DE PASSE", newPassword, { newPassword = it; error = null }, KeyboardType.Password, isPassword = true, helper = "8 caractères minimum")
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "Réinitialiser",
                enabled = resetValid && !loading,
                loading = loading
            ) {
                loading = true; error = null
                onReset(email, otpCode, newPassword) { err ->
                    loading = false
                    if (err == null) onDismiss() else error = err
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Renvoyer un code",
                color = UnovColors.TextDim,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !loading) {
                        loading = true; error = null
                        onForgot(email) { err -> loading = false; if (err == null) info = "Nouveau code envoyé à $email." else error = err }
                    }
                    .padding(8.dp)
            )
        }

        if (error != null) {
            Spacer(Modifier.height(14.dp))
            Text(error!!, color = UnovColors.Danger, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    helper: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = UnovColors.TextMute, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = SolidColor(UnovColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(UnovColors.Surface)
                .border(1.dp, UnovColors.Line, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        )
        if (helper != null) Text(helper, color = UnovColors.TextMute, fontSize = 11.sp)
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
        if (loading) {
            CircularProgressIndicator(color = Color(0xFF0D0D0D), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text(text, color = if (enabled) Color(0xFF0D0D0D) else UnovColors.TextMute, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
