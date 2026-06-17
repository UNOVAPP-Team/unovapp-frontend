package com.unovapp.android.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unovapp.android.R
import com.unovapp.android.ui.components.CountdownRing
import com.unovapp.android.ui.components.GoldPrimaryButton
import com.unovapp.android.ui.components.RotatingGreeting
import com.unovapp.android.ui.components.pulseScale
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import kotlinx.coroutines.delay

/**
 * Pays + spec téléphone. Le nombre de chiffres, le découpage et le préfixe obligatoire
 * (Bénin : `01` depuis 2021) varient d'un pays à l'autre.
 */
data class Country(
    val flag: String,
    val name: String,
    val code: String,
    val phoneLength: Int,
    val phoneChunks: List<Int>,
    val phonePrefix: String? = null,
    val exampleDigits: String
) {
    fun format(rawDigits: String): String {
        val clean = rawDigits.filter(Char::isDigit).take(phoneLength)
        val out = StringBuilder()
        var pos = 0
        for ((i, n) in phoneChunks.withIndex()) {
            if (pos >= clean.length) break
            if (i > 0) out.append(' ')
            out.append(clean.substring(pos, minOf(pos + n, clean.length)))
            pos += n
        }
        return out.toString()
    }

    fun placeholder(): String = format(exampleDigits)

    fun isValid(rawDigits: String): Boolean {
        val clean = rawDigits.filter(Char::isDigit)
        if (clean.length != phoneLength) return false
        return phonePrefix == null || clean.startsWith(phonePrefix)
    }
}

internal val COUNTRIES = listOf(
    Country("🇧🇯", "Bénin",         "+229", 10, listOf(2, 2, 2, 2, 2), phonePrefix = "01", exampleDigits = "0196123456"),
    Country("🇨🇮", "Côte d'Ivoire", "+225", 10, listOf(2, 2, 2, 2, 2), exampleDigits = "0712345678"),
    Country("🇹🇬", "Togo",          "+228", 8,  listOf(2, 2, 2, 2),    exampleDigits = "90123456"),
    Country("🇸🇳", "Sénégal",       "+221", 9,  listOf(3, 3, 3),       exampleDigits = "771234567"),
    Country("🇳🇬", "Nigeria",       "+234", 10, listOf(3, 3, 4),       exampleDigits = "8031234567"),
    Country("🇬🇭", "Ghana",         "+233", 9,  listOf(2, 3, 4),       exampleDigits = "241234567"),
    Country("🇲🇱", "Mali",          "+223", 8,  listOf(2, 2, 2, 2),    exampleDigits = "70123456"),
    Country("🇧🇫", "Burkina Faso",  "+226", 8,  listOf(2, 2, 2, 2),    exampleDigits = "70123456"),
    Country("🇨🇲", "Cameroun",      "+237", 9,  listOf(3, 2, 2, 2),    exampleDigits = "651234567")
)

private class PhoneVisualTransformation(private val country: Country) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter(Char::isDigit).take(country.phoneLength)
        val formatted = country.format(raw)

        val rawToFormatted = IntArray(raw.length + 1)
        val formattedToRaw = IntArray(formatted.length + 1)
        var rawIdx = 0
        for ((i, c) in formatted.withIndex()) {
            formattedToRaw[i] = rawIdx
            if (c != ' ') {
                rawToFormatted[rawIdx] = i
                rawIdx++
            }
        }
        rawToFormatted[raw.length] = formatted.length
        formattedToRaw[formatted.length] = raw.length

        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) =
                    rawToFormatted.getOrElse(offset) { formatted.length }
                override fun transformedToOriginal(offset: Int) =
                    formattedToRaw.getOrElse(offset) { raw.length }
            }
        )
    }
}

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    onBack: () -> Unit,
    verifyEmailToken: String? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    UnovAppTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var pickerOpen by remember { mutableStateOf(false) }
        var forgotOpen by remember { mutableStateOf(false) }

        LaunchedEffect(state.step) {
            if (state.step == AuthStep.Success) {
                delay(1300)
                onAuthenticated()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050505))
        ) {
            AmbientGlow()

            if (state.step == AuthStep.Success) {
                SuccessState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .imePadding()
                ) {
                    TopBar(
                        step = state.step,
                        onBack = {
                            if (state.step == AuthStep.Welcome) onBack()
                            else viewModel.goBack()
                        }
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 28.dp, end = 28.dp, top = 24.dp)
                    ) {
                        AnimatedContent(
                            targetState = state.step,
                            transitionSpec = {
                                val forward = targetState.ordinal > initialState.ordinal
                                val dir = if (forward) 1 else -1
                                (slideInHorizontally(tween(300)) { dir * it / 4 } + fadeIn())
                                    .togetherWith(slideOutHorizontally(tween(220)) { -dir * it / 4 } + fadeOut())
                            },
                            label = "step"
                        ) { step ->
                            when (step) {
                                AuthStep.Welcome -> WelcomeStep(
                                    onChooseLogin = { viewModel.chooseMode(AuthMode.Login) },
                                    onChooseRegister = { viewModel.chooseMode(AuthMode.Register) }
                                )
                                AuthStep.Form -> FormStep(
                                    state = state,
                                    onEmailChange = viewModel::onEmailChange,
                                    onUsernameChange = viewModel::onUsernameChange,
                                    onPasswordChange = viewModel::onPasswordChange,
                                    onCountryClick = { pickerOpen = true },
                                    onPhoneChange = viewModel::onPhoneChange,
                                    onSwitchMode = {
                                        viewModel.chooseMode(
                                            if (state.mode == AuthMode.Login) AuthMode.Register
                                            else AuthMode.Login
                                        )
                                    },
                                    onForgotPassword = { forgotOpen = true },
                                    onGoogle = { viewModel.signInWithGoogle(context) }
                                )
                                AuthStep.Otp -> OtpStep(
                                    email = state.email,
                                    otp = state.otp,
                                    onOtpDigitChange = viewModel::onOtpDigitChange,
                                    otpError = state.otpError,
                                    countdown = state.countdown,
                                    onResend = viewModel::resendOtp,
                                    onSkip = viewModel::skipOtp
                                )
                                AuthStep.VerifyEmail -> Unit
                                AuthStep.Success -> Unit
                            }
                        }
                    }

                    AnimatedVisibility(visible = state.networkError != null) {
                        NetworkErrorBanner(
                            message = state.networkError.orEmpty(),
                            canRetry = state.retryAction != null,
                            onRetry = viewModel::retryLastAction,
                            onDismiss = viewModel::dismissError
                        )
                    }

                    AnimatedVisibility(visible = state.infoMessage != null) {
                        Text(
                            text = state.infoMessage.orEmpty(),
                            color = UnovColors.Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 8.dp)
                        )
                    }

                    Cta(
                        state = state,
                        onSubmit = {
                            when (state.step) {
                                AuthStep.Welcome -> viewModel.chooseMode(AuthMode.Register)
                                AuthStep.Form -> viewModel.submitForm()
                                AuthStep.Otp -> viewModel.verifyOtp()
                                AuthStep.VerifyEmail -> Unit
                                AuthStep.Success -> Unit
                            }
                        }
                    )
                }
            }
        }

        if (pickerOpen) {
            CountrySheet(
                value = state.country,
                onChange = {
                    viewModel.onCountryChange(it)
                    pickerOpen = false
                },
                onClose = { pickerOpen = false }
            )
        }

        if (forgotOpen) {
            ForgotPasswordScreen(
                initialEmail = state.email,
                onForgot = viewModel::forgotPassword,
                onReset = viewModel::resetPassword,
                onDismiss = { forgotOpen = false }
            )
        }
    }
}

/* ---------- Top bar (back + step counter) ---------- */

@Composable
private fun TopBar(step: AuthStep, onBack: () -> Unit) {
    val counter = when (step) {
        AuthStep.Welcome -> ""
        AuthStep.Form -> "01 / 02"
        AuthStep.Otp -> "02 / 02"
        AuthStep.VerifyEmail -> "02 / 02"
        AuthStep.Success -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BackButton(onClick = onBack)
        if (counter.isNotEmpty()) {
            Text(
                text = counter,
                color = UnovColors.TextMute,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.4.sp
            )
        } else {
            Spacer(modifier = Modifier.size(38.dp))
        }
        Spacer(modifier = Modifier.size(38.dp))
    }
}

/* ---------- CTA primaire en bas ---------- */

@Composable
private fun Cta(state: AuthUiState, onSubmit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
    ) {
        if (state.step == AuthStep.Welcome) {
            // Pas de CTA primaire en Welcome — les tuiles font office de CTA.
            // Petite mention de sécurité pour rassurer.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = UnovColors.TextMute,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.welcome_security),
                    color = UnovColors.TextMute,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.6.sp
                )
            }
            return@Column
        }

        val ctaLabel = when (state.step) {
            AuthStep.Form -> when (state.mode) {
                AuthMode.Login -> stringResource(R.string.auth_cta_login)
                AuthMode.Register -> stringResource(R.string.auth_cta_register)
            }
            AuthStep.Otp -> stringResource(R.string.otp_cta)
            AuthStep.VerifyEmail -> "J'ai vérifié — me connecter"
            else -> ""
        }
        val ctaEnabled = when (state.step) {
            AuthStep.Form -> state.formValid
            AuthStep.Otp -> state.otpValid
            AuthStep.VerifyEmail -> true
            else -> false
        }

        GoldPrimaryButton(
            text = ctaLabel,
            onClick = onSubmit,
            enabled = ctaEnabled,
            isLoading = state.isLoading,
            trailingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF0D0D0D),
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}

/* ---------- Step Welcome ---------- */

@Composable
private fun WelcomeStep(
    onChooseLogin: () -> Unit,
    onChooseRegister: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        RotatingGreeting()

        Text(
            text = stringResource(R.string.welcome_title),
            color = UnovColors.Text,
            fontSize = 34.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.9).sp,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            text = stringResource(R.string.welcome_subtitle),
            color = UnovColors.TextDim,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        ModeTile(
            title = stringResource(R.string.welcome_register_title),
            subtitle = stringResource(R.string.welcome_register_sub),
            highlighted = true,
            onClick = onChooseRegister
        )

        Spacer(modifier = Modifier.height(12.dp))

        ModeTile(
            title = stringResource(R.string.welcome_login_title),
            subtitle = stringResource(R.string.welcome_login_sub),
            highlighted = false,
            onClick = onChooseLogin
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(UnovColors.Line))
            Text(
                text = stringResource(R.string.welcome_soon),
                color = UnovColors.TextMute,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.8.sp
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(UnovColors.Line))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SsoTile(icon = Icons.Outlined.AlternateEmail, label = "Google", modifier = Modifier.weight(1f), enabled = false)
            SsoTile(icon = Icons.Outlined.MailOutline, label = "Lien email", modifier = Modifier.weight(1f), enabled = false)
            SsoTile(icon = Icons.Outlined.Sms, label = "OTP SMS", modifier = Modifier.weight(1f), enabled = false)
        }
    }
}

@Composable
private fun ModeTile(
    title: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (highlighted) UnovColors.Accent.copy(alpha = 0.6f) else UnovColors.Line
    val bg = if (highlighted) UnovColors.Accent.copy(alpha = 0.04f) else UnovColors.Surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = UnovColors.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
            Text(
                text = subtitle,
                color = UnovColors.TextMute,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = if (highlighted) UnovColors.Accent else UnovColors.TextDim,
            modifier = Modifier.size(18.dp)
        )
    }
}

/* ---------- Step Form (login / register) ---------- */

@Composable
private fun FormStep(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onCountryClick: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onSwitchMode: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogle: () -> Unit
) {
    val isRegister = state.mode == AuthMode.Register
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = stringResource(
                if (isRegister) R.string.auth_eyebrow_register else R.string.auth_eyebrow_login
            ),
            color = UnovColors.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp
        )
        Text(
            text = stringResource(
                if (isRegister) R.string.auth_title_register else R.string.auth_title_login
            ),
            color = UnovColors.Text,
            fontSize = 32.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.8).sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = stringResource(
                if (isRegister) R.string.auth_sub_register else R.string.auth_sub_login
            ),
            color = UnovColors.TextDim,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        LabeledField(label = stringResource(R.string.auth_label_email)) {
            TextFieldShell(
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = stringResource(R.string.auth_placeholder_email),
                error = state.emailError,
                valid = state.emailValid,
                keyboardType = KeyboardType.Email
            )
        }

        if (isRegister) {
            Spacer(modifier = Modifier.height(16.dp))
            LabeledField(label = stringResource(R.string.auth_label_username)) {
                TextFieldShell(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    placeholder = stringResource(R.string.auth_placeholder_username),
                    error = state.usernameError,
                    valid = state.usernameValid,
                    keyboardType = KeyboardType.Text
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LabeledField(label = stringResource(R.string.auth_label_password)) {
            PasswordField(
                value = state.password,
                onValueChange = onPasswordChange,
                error = state.passwordError,
                valid = state.passwordValid
            )
        }

        if (!isRegister) {
            Text(
                text = "Mot de passe oublié ?",
                color = UnovColors.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onForgotPassword)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }

        if (isRegister) {
            Spacer(modifier = Modifier.height(16.dp))
            LabeledField(label = stringResource(R.string.auth_label_phone)) {
                PhoneField(
                    country = state.country,
                    onCountryClick = onCountryClick,
                    phone = state.phone,
                    onPhoneChange = onPhoneChange,
                    error = state.phoneError,
                    valid = state.phoneValid
                )
            }
            Text(
                text = stringResource(R.string.auth_phone_hint),
                color = UnovColors.TextMute,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (isRegister) R.string.auth_already else R.string.auth_not_yet
                ),
                color = UnovColors.TextMute,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    if (isRegister) R.string.auth_cta_login else R.string.welcome_register_title
                ),
                color = UnovColors.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onSwitchMode)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Séparateur "ou"
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(UnovColors.Line))
            Text(
                text = "ou",
                color = UnovColors.TextMute,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Box(Modifier.weight(1f).height(1.dp).background(UnovColors.Line))
        }

        Spacer(modifier = Modifier.height(16.dp))
        GoogleButton(onClick = onGoogle)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun GoogleButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "G",
            color = UnovColors.Accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Continuer avec Google",
            color = UnovColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ---------- Step Vérification email ---------- */

@Composable
private fun VerifyEmailStep(
    email: String,
    isLoading: Boolean,
    onResend: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "VÉRIFICATION EMAIL",
            color = UnovColors.Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )
        Text(
            text = "Vérifie ton\nadresse email.",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp
        )
        Text(
            text = "On a envoyé un lien de vérification à $email. Ouvre ta boîte mail, clique sur le lien, puis reviens te connecter.",
            color = UnovColors.TextDim,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Bouton secondaire : ouvre l'app mail par défaut du téléphone.
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, UnovColors.Accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable {
                    runCatching {
                        val intent = Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_APP_EMAIL)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Ouvrir ma boîte mail",
                color = UnovColors.Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = if (isLoading) "Envoi en cours…" else "Renvoyer l'email",
            color = UnovColors.TextDim,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !isLoading, onClick = onResend)
                .padding(vertical = 8.dp)
        )
    }
}

/* ---------- Step OTP ---------- */

@Composable
private fun OtpStep(
    email: String,
    otp: List<String>,
    onOtpDigitChange: (Int, String) -> Unit,
    otpError: String?,
    countdown: Int,
    onResend: () -> Unit,
    onSkip: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
    }

    Column {
        Text(
            text = stringResource(R.string.otp_eyebrow),
            color = UnovColors.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp
        )
        Text(
            text = stringResource(R.string.otp_title),
            color = UnovColors.Text,
            fontSize = 32.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.8).sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = buildAnnotatedString {
                append("Envoyé à ")
                withStyle(SpanStyle(color = UnovColors.Text, fontWeight = FontWeight.Medium)) {
                    append(email)
                }
                append(".")
            },
            color = UnovColors.TextDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Box {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                otp.forEachIndexed { i, digit ->
                    val focused = digit.isEmpty() && otp.take(i).all { it.isNotEmpty() }
                    OtpBox(digit = digit, focused = focused, hasError = otpError != null)
                }
            }
            BasicTextField(
                value = otp.joinToString(""),
                onValueChange = { new ->
                    val digits = new.filter(Char::isDigit).take(otp.size)
                    for (i in otp.indices) {
                        onOtpDigitChange(i, digits.getOrNull(i)?.toString() ?: "")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                cursorBrush = SolidColor(Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .height(72.dp)
            ) {}
        }

        AnimatedVisibility(visible = otpError != null) {
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = UnovColors.Danger,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = otpError.orEmpty(),
                    color = UnovColors.Danger,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (countdown > 0) {
                val pct = (RESEND_COUNTDOWN_VISUAL - countdown) / RESEND_COUNTDOWN_VISUAL.toFloat()
                val animPct by animateFloatAsState(
                    targetValue = pct,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
                    label = "ringProgress"
                )
                CountdownRing(progress = animPct, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Renvoyer dans ")
                        withStyle(SpanStyle(color = UnovColors.Text, fontWeight = FontWeight.Medium)) {
                            append("00:${countdown.toString().padStart(2, '0')}")
                        }
                    },
                    color = UnovColors.TextMute,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = stringResource(R.string.otp_resend),
                    color = UnovColors.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onResend)
                        .padding(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lien "passer cette étape" — verify-otp est facultatif puisque le compte est déjà créé
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = UnovColors.TextMute,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.otp_skip),
                color = UnovColors.TextMute,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

private const val RESEND_COUNTDOWN_VISUAL = 60

/* ---------- Form building blocks ---------- */

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            color = UnovColors.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun TextFieldShell(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    valid: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            error != null -> UnovColors.Danger
            valid && value.isNotEmpty() -> UnovColors.Accent
            else -> UnovColors.Line
        },
        animationSpec = tween(280),
        label = "fieldBorder"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(UnovColors.Surface)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(UnovColors.Accent),
                visualTransformation = visualTransformation,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = UnovColors.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                decorationBox = { inner ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = UnovColors.TextMute,
                                    fontSize = 15.sp
                                )
                            }
                            inner()
                        }
                        if (trailing != null) {
                            trailing()
                        } else {
                            AnimatedVisibility(
                                visible = valid && value.isNotEmpty(),
                                enter = scaleIn(initialScale = 0.4f, animationSpec = tween(280)) + fadeIn(tween(280)),
                                exit = scaleOut(targetScale = 0.4f, animationSpec = tween(150)) + fadeOut(tween(150))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = UnovColors.Accent,
                                    modifier = Modifier.padding(start = 6.dp).size(18.dp)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        if (error != null) {
            FieldError(text = error)
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    valid: Boolean
) {
    var visible by remember { mutableStateOf(false) }
    val noRipple = remember { MutableInteractionSource() }
    TextFieldShell(
        value = value,
        onValueChange = onValueChange,
        placeholder = "8 caractères minimum",
        error = error,
        valid = valid,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            Icon(
                imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (visible) "Cacher" else "Afficher",
                tint = UnovColors.TextDim,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(20.dp)
                    .clickable(
                        interactionSource = noRipple,
                        indication = null,
                        onClick = { visible = !visible }
                    )
            )
        }
    )
}

@Composable
private fun PhoneField(
    country: Country,
    onCountryClick: () -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    error: String?,
    valid: Boolean
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            error != null -> UnovColors.Danger
            valid -> UnovColors.Accent
            else -> UnovColors.Line
        },
        animationSpec = tween(280),
        label = "phoneBorder"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(12.dp))
                .background(UnovColors.Surface)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onCountryClick)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedContent(
                    targetState = country.flag,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.6f, animationSpec = tween(300)) + fadeIn(tween(300)))
                            .togetherWith(scaleOut(targetScale = 1.3f, animationSpec = tween(180)) + fadeOut(tween(150)))
                    },
                    label = "flag"
                ) { f -> Text(text = f, fontSize = 20.sp) }

                AnimatedContent(
                    targetState = country.code,
                    transitionSpec = {
                        (slideInVertically(tween(280)) { it / 2 } + fadeIn(tween(280)))
                            .togetherWith(slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(150)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "code"
                ) { c ->
                    Text(text = c, color = UnovColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = UnovColors.TextMute,
                    modifier = Modifier.size(14.dp)
                )
            }

            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(UnovColors.Line))

            BasicTextField(
                value = phone,
                onValueChange = onPhoneChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                cursorBrush = SolidColor(UnovColors.Accent),
                visualTransformation = remember(country) { PhoneVisualTransformation(country) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = UnovColors.Text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp
                ),
                decorationBox = { inner ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (phone.isEmpty()) {
                                Text(text = country.placeholder(), color = UnovColors.TextMute, fontSize = 16.sp, letterSpacing = 0.6.sp)
                            }
                            inner()
                        }
                        AnimatedVisibility(
                            visible = valid,
                            enter = scaleIn(initialScale = 0.4f, animationSpec = tween(280)) + fadeIn(tween(280)),
                            exit = scaleOut(targetScale = 0.4f, animationSpec = tween(150)) + fadeOut(tween(150))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = UnovColors.Accent,
                                modifier = Modifier.padding(start = 6.dp).size(18.dp)
                            )
                        }
                    }
                }
            )
        }
        if (error != null) FieldError(text = error)
    }
}

@Composable
private fun FieldError(text: String) {
    Row(
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = UnovColors.Danger,
            modifier = Modifier.size(13.dp)
        )
        Text(text = text, color = UnovColors.Danger, fontSize = 12.sp)
    }
}

@Composable
private fun OtpBox(digit: String, focused: Boolean, hasError: Boolean = false) {
    val borderColor = when {
        hasError -> UnovColors.Danger
        digit.isNotEmpty() -> UnovColors.Accent
        focused -> UnovColors.Accent.copy(alpha = 0.45f)
        else -> UnovColors.Line
    }
    val bg = if (digit.isNotEmpty()) UnovColors.Accent.copy(alpha = 0.06f) else UnovColors.Surface
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (digit.isNotEmpty()) {
            Text(
                text = digit,
                color = UnovColors.Accent,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold
            )
        } else if (focused) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(26.dp)
                    .background(UnovColors.Accent)
                    .pulseScale(0.6f, 1f, 1100)
            )
        }
    }
}

/* ---------- Success ---------- */

@Composable
private fun SuccessState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(560.dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(UnovColors.Accent.copy(alpha = 0.20f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(UnovGradients.Gold),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF0D0D0D),
                    modifier = Modifier.size(56.dp)
                )
            }
            Text(
                text = stringResource(R.string.auth_success_title),
                color = UnovColors.Text,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp)
            )
            Text(
                text = stringResource(R.string.auth_success_sub),
                color = UnovColors.TextDim,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 40.dp, end = 40.dp)
            )
        }
    }
}

/* ---------- Sub-components communs ---------- */

@Composable
private fun NetworkErrorBanner(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(UnovColors.Danger.copy(alpha = 0.08f))
            .border(1.dp, UnovColors.Danger.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = UnovColors.Danger,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = message,
            color = UnovColors.Text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (canRetry) "RÉESSAYER" else "FERMER",
            color = if (canRetry) UnovColors.Accent else UnovColors.TextMute,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = if (canRetry) onRetry else onDismiss)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(1.dp, UnovColors.Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Retour",
            tint = UnovColors.Text,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AmbientGlow() {
    Box(
        modifier = Modifier
            .size(460.dp)
            .offset(x = (-140).dp, y = (-170).dp)
            .blur(60.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(UnovColors.Accent.copy(alpha = 0.10f), Color.Transparent)
                ),
                shape = CircleShape
            )
    )
}

@Composable
private fun SsoTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val noRipple = remember { MutableInteractionSource() }
    val alpha = if (enabled) 1f else 0.38f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(UnovColors.Surface.copy(alpha = alpha))
            .border(1.dp, UnovColors.Line.copy(alpha = alpha), RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                interactionSource = noRipple,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = UnovColors.Text.copy(alpha = alpha),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = UnovColors.TextDim.copy(alpha = alpha),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountrySheet(
    value: Country,
    onChange: (Country) -> Unit,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF0A0A0A),
        contentColor = UnovColors.Text,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovColors.LineStrong)
            )
        }
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp)) {
                Text(
                    text = "PAYS",
                    color = UnovColors.TextMute,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.6.sp
                )
                Text(
                    text = "Sélectionne ton pays",
                    color = UnovColors.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            COUNTRIES.forEach { c ->
                val selected = c.code == value.code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) UnovColors.Accent.copy(alpha = 0.06f) else Color.Transparent
                        )
                        .clickable { onChange(c) }
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = c.flag, fontSize = 22.sp)
                    Text(
                        text = c.name,
                        color = UnovColors.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = c.code,
                        color = if (selected) UnovColors.Accent else UnovColors.TextMute,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    AnimatedVisibility(
                        visible = selected,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = UnovColors.Accent,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
