package com.unovapp.android.ui.auth

import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    /** Format "01 96 12 34 56" depuis "0196123456". Tronque proprement si incomplet. */
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

    /** Texte gris qui apparaît dans le champ vide. */
    fun placeholder(): String = format(exampleDigits)

    /** Validation finale : longueur + préfixe (Bénin uniquement pour l'instant). */
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

/**
 * Formate le contenu du champ téléphone selon le pays sélectionné, sans muter le state
 * sous-jacent. `state.phone` reste un flux de chiffres bruts ; le mapping cursor/spaces
 * est géré ici pour que l'utilisateur tape naturellement sans que le caret saute.
 */
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
    viewModel: AuthViewModel = hiltViewModel()
) {
    UnovAppTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()
        var pickerOpen by remember { mutableStateOf(false) }
        val activity = LocalActivity.current
        val context = LocalContext.current

        // Navigation après succès
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
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BackButton(onClick = {
                            if (state.step == AuthStep.Otp) viewModel.goBackFromOtp() else onBack()
                        })
                        Text(
                            text = if (state.step == AuthStep.Phone) "01 / 02" else "02 / 02",
                            color = UnovColors.TextMute,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.4.sp
                        )
                        Spacer(modifier = Modifier.size(38.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 28.dp, end = 28.dp, top = 32.dp)
                    ) {
                        AnimatedContent(
                            targetState = state.step,
                            transitionSpec = {
                                if (targetState == AuthStep.Otp) {
                                    (slideInHorizontally { it / 4 } + fadeIn())
                                        .togetherWith(slideOutHorizontally { -it / 4 } + fadeOut())
                                } else {
                                    (slideInHorizontally { -it / 4 } + fadeIn())
                                        .togetherWith(slideOutHorizontally { it / 4 } + fadeOut())
                                }
                            },
                            label = "step"
                        ) { step ->
                            when (step) {
                                AuthStep.Phone -> PhoneStep(
                                    country = state.country,
                                    onCountryClick = { pickerOpen = true },
                                    phone = state.phone,
                                    onPhoneChange = viewModel::onPhoneChange,
                                    phoneError = state.phoneError,
                                    phoneValid = state.phoneValid,
                                    onGoogleClick = {
                                        activity?.let { viewModel.signInWithGoogle(it) }
                                    }
                                )

                                AuthStep.Otp -> OtpStep(
                                    country = state.country,
                                    phone = state.phone,
                                    otp = state.otp,
                                    onOtpDigitChange = viewModel::onOtpDigitChange,
                                    otpError = state.otpError,
                                    countdown = state.countdown,
                                    onResend = viewModel::resendOtp
                                )

                                AuthStep.Success -> Unit
                            }
                        }
                    }

                    // Network error banner
                    AnimatedVisibility(visible = state.networkError != null) {
                        NetworkErrorBanner(
                            message = state.networkError.orEmpty(),
                            canRetry = state.retryAction != null,
                            onRetry = { viewModel.retryLastAction(activity ?: context) },
                            onDismiss = viewModel::dismissError
                        )
                    }

                    // CTA
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                    ) {
                        GoldPrimaryButton(
                            text = if (state.step == AuthStep.Phone) "Recevoir le code" else "Vérifier",
                            onClick = {
                                if (state.step == AuthStep.Phone) viewModel.sendOtp()
                                else viewModel.verifyOtp()
                            },
                            enabled = if (state.step == AuthStep.Phone) state.phoneValid
                                      else state.otpValid,
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

                        if (state.step == AuthStep.Phone) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
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
                                    text = "CHIFFRÉ · MOMO & MOOV PRÊTS",
                                    color = UnovColors.TextMute,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.6.sp
                                )
                            }
                        }
                    }
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
    }
}

/* ---------- Phone step ---------- */

@Composable
private fun PhoneStep(
    country: Country,
    onCountryClick: () -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    phoneError: String?,
    phoneValid: Boolean,
    onGoogleClick: () -> Unit
) {
    Column {
        RotatingGreeting()

        Text(
            text = "Ton numéro,\npour commencer.",
            color = UnovColors.Text,
            fontSize = 34.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.9).sp,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            text = "On t'envoie un code par SMS — la connexion est instantanée et ton numéro reste privé.",
            color = UnovColors.TextDim,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "NUMÉRO DE TÉLÉPHONE",
            color = UnovColors.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        val borderColor by animateColorAsState(
            targetValue = when {
                phoneError != null -> UnovColors.Danger
                phoneValid -> UnovColors.Accent
                else -> UnovColors.Line
            },
            animationSpec = tween(durationMillis = 280),
            label = "fieldBorder"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(12.dp))
                .background(UnovColors.Surface)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Country selector — flag + code animés (effet "slot-machine")
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
                        (scaleIn(initialScale = 0.6f, animationSpec = tween(300)) +
                                fadeIn(tween(300))) togetherWith
                                (scaleOut(targetScale = 1.3f, animationSpec = tween(180)) +
                                        fadeOut(tween(150)))
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
                    Text(
                        text = c,
                        color = UnovColors.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = UnovColors.TextMute,
                    modifier = Modifier.size(14.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(UnovColors.Line)
            )

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
                                AnimatedContent(
                                    targetState = country.placeholder(),
                                    transitionSpec = {
                                        (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 3 })
                                            .togetherWith(fadeOut(tween(150)))
                                    },
                                    label = "placeholder"
                                ) { ph ->
                                    Text(
                                        text = ph,
                                        color = UnovColors.TextMute,
                                        fontSize = 16.sp,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                            }
                            inner()
                        }
                        AnimatedVisibility(
                            visible = phoneValid,
                            enter = scaleIn(initialScale = 0.4f, animationSpec = tween(280)) +
                                    fadeIn(tween(280)),
                            exit = scaleOut(targetScale = 0.4f, animationSpec = tween(150)) +
                                    fadeOut(tween(150))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Numéro valide",
                                tint = UnovColors.Accent,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                }
            )
        }

        AnimatedVisibility(visible = phoneError != null) {
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
                Text(
                    text = phoneError.orEmpty(),
                    color = UnovColors.Danger,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(UnovColors.Line))
            Text(
                text = "OU",
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
            SsoTile(
                icon = Icons.Outlined.AlternateEmail,
                label = "Google",
                modifier = Modifier.weight(1f),
                onClick = onGoogleClick
            )
            SsoTile(
                icon = Icons.Outlined.AlternateEmail,
                label = "Email",
                modifier = Modifier.weight(1f),
                onClick = { /* TODO: flow email mot de passe */ }
            )
            SsoTile(
                icon = Icons.Outlined.Person,
                label = "Apple",
                modifier = Modifier.weight(1f),
                onClick = { /* TODO: Apple SSO */ }
            )
        }
    }
}

/* ---------- OTP step ---------- */

@Composable
private fun OtpStep(
    country: Country,
    phone: String,
    otp: List<String>,
    onOtpDigitChange: (Int, String) -> Unit,
    otpError: String?,
    countdown: Int,
    onResend: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Column {
        Text(
            text = "VÉRIFICATION",
            color = UnovColors.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp
        )
        Text(
            text = "On t'a envoyé\nun code à 4 chiffres.",
            color = UnovColors.Text,
            fontSize = 32.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.8).sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = buildAnnotatedString {
                append("Au ")
                withStyle(SpanStyle(color = UnovColors.Text, fontWeight = FontWeight.Medium)) {
                    append("${country.code} ${country.format(phone)}")
                }
                append(".")
            },
            color = UnovColors.TextDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Stack : 4 visual boxes + hidden BasicTextField pour le keyboard
        Box {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                otp.forEachIndexed { i, digit ->
                    val focused = digit.isEmpty() && otp.take(i).all { it.isNotEmpty() }
                    OtpBox(digit = digit, focused = focused, hasError = otpError != null)
                }
            }
            BasicTextField(
                value = otp.joinToString(""),
                onValueChange = { new ->
                    val digits = new.filter(Char::isDigit).take(4)
                    for (i in 0..3) {
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

        // En mode stub : afficher l'astuce
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = UnovColors.Accent,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (com.unovapp.android.BuildConfig.USE_STUB_AUTH) "Code démo : 1234"
                       else "Auto-remplissage activé",
                color = UnovColors.TextMute,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (countdown > 0) {
                val pct = (60 - countdown) / 60f
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
                    text = "Renvoyer le code",
                    color = UnovColors.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onResend)
                        .padding(4.dp)
                )
            }
        }
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
            .size(width = 60.dp, height = 72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (digit.isNotEmpty()) {
            Text(
                text = digit,
                color = UnovColors.Accent,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
        } else if (focused) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(28.dp)
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
                text = "Bienvenue.",
                color = UnovColors.Text,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp)
            )
            Text(
                text = "Ton compte est prêt. On t'emmène sur le feed dans un instant.",
                color = UnovColors.TextDim,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 40.dp, end = 40.dp)
            )
        }
    }
}

/* ---------- Sub-components ---------- */

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
        if (canRetry) {
            Text(
                text = "RÉESSAYER",
                color = UnovColors.Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            Text(
                text = "FERMER",
                color = UnovColors.TextMute,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
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
    onClick: () -> Unit
) {
    val noRipple = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.Line, RoundedCornerShape(12.dp))
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = UnovColors.Text,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = UnovColors.TextDim,
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

