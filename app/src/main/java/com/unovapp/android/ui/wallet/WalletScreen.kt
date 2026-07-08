package com.unovapp.android.ui.wallet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.components.rememberCountUp
import com.unovapp.android.ui.components.unovTap
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import com.unovapp.android.ui.theme.UnovMotion
import kotlinx.coroutines.delay
import kotlin.random.Random

data class JetonPack(
    val coins: Int,
    val priceFcfa: String,
    val bonus: String? = null,
    val popular: Boolean = false
)

private enum class Provider(val label: String, val letter: String, val color: Color) {
    Mtn("MTN MoMo", "M", Color(0xFFFFCC08)),
    Moov("Moov Money", "m", Color(0xFF0066B3))
}

private enum class WalletStep { Packs, Confirm, Success }

private val PACKS = listOf(
    JetonPack(100, "500 FCFA"),
    JetonPack(500, "2 500 FCFA", bonus = "+10%", popular = true),
    JetonPack(1500, "7 000 FCFA", bonus = "+15%"),
    JetonPack(5000, "22 000 FCFA", bonus = "+20%")
)

private val SuccessGreen = Color(0xFF22C55E)

@Composable
fun WalletScreen(
    onClose: () -> Unit,
    walletViewModel: WalletViewModel = hiltViewModel()
) {
    UnovAppTheme {
        val balance by walletViewModel.balance.collectAsStateWithLifecycle()
        var step by remember { mutableStateOf(WalletStep.Packs) }
        var selectedPackIdx by remember { mutableIntStateOf(1) } // 500 jetons par défaut (popular)
        var provider by remember { mutableStateOf(Provider.Mtn) }
        var processing by remember { mutableStateOf(false) }
        val refCode = remember { "MM-2026-${Random.nextInt(100000, 999999)}" }

        // Auto-confirm après 1.6s en simulation backend
        LaunchedEffect(step, processing) {
            if (step == WalletStep.Confirm && processing) {
                delay(1600)
                // Recharge réussie → on crédite les jetons sur le solde partagé.
                walletViewModel.credit(PACKS[selectedPackIdx].coins.toLong())
                step = WalletStep.Success
                processing = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally { it * dir } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it * dir } + fadeOut())
                },
                label = "walletStep"
            ) { current ->
                when (current) {
                    WalletStep.Packs -> PacksStep(
                        balance = balance,
                        selectedIdx = selectedPackIdx,
                        onSelect = { selectedPackIdx = it },
                        provider = provider,
                        onProviderChange = { provider = it },
                        onClose = onClose,
                        onContinue = { step = WalletStep.Confirm }
                    )
                    WalletStep.Confirm -> ConfirmStep(
                        pack = PACKS[selectedPackIdx],
                        provider = provider,
                        processing = processing,
                        onBack = { step = WalletStep.Packs },
                        onConfirm = { processing = true }
                    )
                    WalletStep.Success -> SuccessStep(
                        coins = PACKS[selectedPackIdx].coins,
                        refCode = refCode,
                        onDone = onClose
                    )
                }
            }
        }
    }
}

/* ---------- Step 1 : Packs ---------- */

@Composable
private fun PacksStep(
    balance: Long,
    selectedIdx: Int,
    onSelect: (Int) -> Unit,
    provider: Provider,
    onProviderChange: (Provider) -> Unit,
    onClose: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleIconButton(icon = Icons.Filled.Close, onClick = onClose, description = "Fermer")
            Text(
                text = "Recharger",
                color = UnovColors.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(38.dp))
        }

        // Solde courant pill
        BalanceCard(balance = balance, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

        Text(
            text = "CHOISIS UN PACK",
            color = UnovColors.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
            modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 10.dp)
        )

        // Packs 2x2
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PACKS.chunked(2).forEachIndexed { rowIdx, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { colIdx, pack ->
                        val packIdx = rowIdx * 2 + colIdx
                        PackTile(
                            pack = pack,
                            isSelected = packIdx == selectedIdx,
                            onClick = { onSelect(packIdx) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Text(
            text = "MOYEN DE PAIEMENT",
            color = UnovColors.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
            modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp)
        )

        // Provider toggle
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Provider.entries.forEach { p ->
                ProviderTile(
                    provider = p,
                    isSelected = p == provider,
                    onClick = { onProviderChange(p) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)
        ) {
            PrimaryGoldButton(
                text = "Continuer",
                onClick = onContinue
            )
            Text(
                text = "Paiement sécurisé · Aucun compte bancaire requis",
                color = UnovColors.TextMute,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun BalanceCard(balance: Long, modifier: Modifier = Modifier) {
    // Count-up sur le solde réel (jetons). Le FCFA est estimé (~5 FCFA / jeton).
    val animatedCoins = rememberCountUp(targetValue = balance.toInt(), durationMs = 1400)
    val animatedFcfa = rememberCountUp(targetValue = (balance * 5).toInt(), durationMs = 1400)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(UnovGradients.Gold)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "SOLDE",
                color = Color(0xFF0D0D0D).copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp
            )
            Text(
                text = "%,d".format(animatedCoins).replace(',', ' '),
                color = Color(0xFF0D0D0D),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp
            )
            Text(
                text = "jetons · ≈ ${"%,d".format(animatedFcfa).replace(',', ' ')} FCFA",
                color = Color(0xFF0D0D0D).copy(alpha = 0.85f),
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Outlined.MonetizationOn,
            contentDescription = null,
            tint = Color(0xFF0D0D0D),
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun PackTile(
    pack: JetonPack,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Scale subtle + animation des couleurs : la tuile sélectionnée "respire" légèrement
    // pour confirmer le choix sans être agressive
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = UnovMotion.bouncy(),
        label = "packScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) UnovColors.Accent else UnovColors.Line,
        animationSpec = UnovMotion.standard(),
        label = "packBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) UnovColors.Accent.copy(alpha = 0.10f) else UnovColors.Surface,
        animationSpec = UnovMotion.standard(),
        label = "packBg"
    )
    Box(modifier = modifier.scale(scale)) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .unovTap(onClick = onClick, pressedScale = 0.96f)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MonetizationOn,
                    contentDescription = null,
                    tint = if (isSelected) UnovColors.Accent else UnovColors.TextDim,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${formatNum(pack.coins)} jetons",
                    color = if (isSelected) UnovColors.Accent else UnovColors.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = pack.priceFcfa,
                color = UnovColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp
            )
            if (pack.bonus != null) {
                Text(
                    text = "Bonus ${pack.bonus}",
                    color = UnovColors.Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (pack.popular) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(UnovGradients.Gold)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "POPULAIRE",
                    color = Color(0xFF0D0D0D),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun ProviderTile(
    provider: Provider,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) UnovColors.Accent else UnovColors.Line,
        animationSpec = UnovMotion.standard(),
        label = "provBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) UnovColors.Accent.copy(alpha = 0.08f) else UnovColors.Surface,
        animationSpec = UnovMotion.standard(),
        label = "provBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = UnovMotion.bouncy(),
        label = "provScale"
    )
    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .unovTap(onClick = onClick, pressedScale = 0.96f)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(provider.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = provider.letter,
                color = if (provider == Provider.Mtn) Color(0xFF0D0D0D) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = provider.label,
            color = UnovColors.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = UnovColors.Accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/* ---------- Step 2 : Confirm ---------- */

@Composable
private fun ConfirmStep(
    pack: JetonPack,
    provider: Provider,
    processing: Boolean,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack, description = "Retour")
            Text(
                text = "Confirmation",
                color = UnovColors.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(38.dp))
        }

        // Summary card
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(UnovColors.Surface)
                .border(1.dp, UnovColors.Line, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SummaryRow(label = "Pack", value = "${formatNum(pack.coins)} jetons")
            SummaryRow(label = "Montant", value = pack.priceFcfa)
            if (pack.bonus != null) {
                SummaryRow(label = "Bonus", value = pack.bonus, accent = true)
            }
            HSeparator()
            SummaryRow(
                label = "Paiement via",
                value = provider.label
            )
        }

        Text(
            text = "Tu vas recevoir un SMS de confirmation. Saisis ton code PIN ${provider.label} pour valider.",
            color = UnovColors.TextDim,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)
        ) {
            PrimaryGoldButton(
                text = if (processing) "Traitement en cours…" else "Confirmer le paiement",
                onClick = onConfirm,
                isLoading = processing
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, accent: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = UnovColors.TextMute,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = if (accent) UnovColors.Accent else UnovColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(UnovColors.Line)
    )
}

/* ---------- Step 3 : Success ---------- */

@Composable
private fun SuccessStep(
    coins: Int,
    refCode: String,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .size(500.dp)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SuccessGreen.copy(alpha = 0.20f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .align(Alignment.Center)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF0D0D0D),
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Paiement réussi",
                color = UnovColors.Text,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatNum(coins)} jetons ajoutés à ton portefeuille.",
                color = UnovColors.TextDim,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Réf : $refCode",
                color = UnovColors.TextMute,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Done CTA at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
        ) {
            PrimaryGoldButton(text = "Retour au feed", onClick = onDone)
        }
    }
}

/* ---------- Shared mini-components ---------- */

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    description: String
) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.Line, CircleShape)
            .clickable(
                interactionSource = noRipple,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = UnovColors.Text,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PrimaryGoldButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(UnovGradients.Gold)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = text,
                color = Color(0xFF0D0D0D),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF0D0D0D),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatNum(n: Int): String =
    "%,d".format(n).replace(',', ' ')
