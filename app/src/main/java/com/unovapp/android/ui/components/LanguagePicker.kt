package com.unovapp.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Language
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.AppLanguage
import com.unovapp.android.LocaleManager
import com.unovapp.android.R
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovMotion

/**
 * Bottom sheet de sélection de langue. Liste les 3 langues supportées avec leur libellé
 * natif (Français / Fɔngbè / Yorùbá) et un check or sur la langue actuellement active.
 *
 * Tap sur une langue → applique via [LocaleManager.apply] et ferme. AppCompat se charge
 * de la persistance et de la recréation des activités pour propager le changement.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val current = remember { LocaleManager.current() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
        Column(modifier = Modifier.padding(bottom = 18.dp)) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = UnovColors.Accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.lang_title).uppercase(),
                        color = UnovColors.TextMute,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    )
                }
                Text(
                    text = stringResource(R.string.lang_sub),
                    color = UnovColors.TextDim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            LanguageRow(
                language = AppLanguage.French,
                label = stringResource(R.string.lang_fr),
                hint = "Français",
                selected = current == AppLanguage.French,
                onClick = {
                    LocaleManager.apply(AppLanguage.French)
                    onDismiss()
                }
            )
            LanguageRow(
                language = AppLanguage.Fon,
                label = stringResource(R.string.lang_fon),
                hint = "Bénin · sud",
                selected = current == AppLanguage.Fon,
                onClick = {
                    LocaleManager.apply(AppLanguage.Fon)
                    onDismiss()
                }
            )
            LanguageRow(
                language = AppLanguage.Yoruba,
                label = stringResource(R.string.lang_yo),
                hint = "Bénin · Nigeria · Togo",
                selected = current == AppLanguage.Yoruba,
                onClick = {
                    LocaleManager.apply(AppLanguage.Yoruba)
                    onDismiss()
                }
            )

            Text(
                text = "Traductions en cours de validation par des locuteurs natifs.",
                color = UnovColors.TextMute,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)
            )
        }
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    label: String,
    hint: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) UnovColors.Accent.copy(alpha = 0.06f) else Color.Transparent,
        animationSpec = UnovMotion.standard(),
        label = "langBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .unovTap(onClick = onClick, pressedScale = 0.97f)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pastille avec le code court de la langue (FR / FON / YO)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (selected) UnovColors.Accent.copy(alpha = 0.14f) else UnovColors.Surface
                )
                .border(
                    1.dp,
                    if (selected) UnovColors.Accent else UnovColors.Line,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = language.tag.uppercase().take(3),
                color = if (selected) UnovColors.Accent else UnovColors.TextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = UnovColors.Text,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
            Text(
                text = hint,
                color = UnovColors.TextMute,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(initialScale = 0.4f, animationSpec = UnovMotion.bouncy()) + fadeIn(UnovMotion.fast()),
            exit = scaleOut(targetScale = 0.4f, animationSpec = UnovMotion.snappy()) + fadeOut(UnovMotion.fast())
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = UnovColors.Accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Chip compacte (FR / FON / YO) à placer en header pour ouvrir le picker. Lit la langue
 * courante via [LocaleManager.current] et se met à jour si elle change (`LaunchedEffect`).
 */
@Composable
fun LanguageChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf(LocaleManager.current()) }
    LaunchedEffect(Unit) { current = LocaleManager.current() }
    val label = when (current) {
        AppLanguage.French -> stringResource(R.string.lang_short_fr)
        AppLanguage.Fon -> stringResource(R.string.lang_short_fon)
        AppLanguage.Yoruba -> stringResource(R.string.lang_short_yo)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
            .unovTap(onClick = onClick, pressedScale = 0.92f)
            .padding(start = 8.dp, top = 5.dp, end = 10.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = null,
            tint = UnovColors.Accent,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}
