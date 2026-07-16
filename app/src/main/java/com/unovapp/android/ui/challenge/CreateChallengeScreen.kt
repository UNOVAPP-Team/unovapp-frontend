package com.unovapp.android.ui.challenge

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * « Créer un challenge » — reprise fidèle de la maquette produit.
 *
 * Quatre sections empilées, chacune dans une carte sombre :
 *   1. Informations générales — nom, description (0/200), image de couverture
 *   2. Conditions du challenge — dates début/fin, règles (0/300), audience, âge minimum
 *   3. Récompenses (optionnel) — type + montant
 *   4. Frais de participation (optionnel) — interrupteur + montant
 * puis le bouton « Lancer le challenge » et la mention des CGU.
 *
 * ⚠️ Le backend n'expose encore AUCUN endpoint challenge (cf. docs/BACKEND_CHALLENGES.md) :
 * [onLaunch] remonte le formulaire validé — le POST sera branché dès que l'API existera.
 */
@Composable
fun CreateChallengeScreen(
    onBack: () -> Unit,
    onLaunch: (ChallengeForm) -> Unit = {}
) {
    var form by remember { mutableStateOf(ChallengeForm()) }
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) form = form.copy(coverUri = uri.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UnovColors.BgBase)
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
    ) {
        // ── Barre du haut : retour + titre centré ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Créer un challenge",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            /* ─────────────── 1. Informations générales ─────────────── */
            SectionTitle("Informations générales")
            SectionCard {
                FieldLabel("Nom du challenge")
                UnovTextField(
                    value = form.name,
                    onValueChange = { form = form.copy(name = it) },
                    placeholder = "Ex: Danse Afro 2026"
                )

                Spacer(Modifier.height(16.dp))
                FieldLabel("Description")
                UnovTextField(
                    value = form.description,
                    onValueChange = {
                        if (it.length <= CHALLENGE_DESC_MAX) form = form.copy(description = it)
                    },
                    placeholder = "Présentez votre challenge et ce que les participants doivent faire...",
                    minHeight = 88.dp,
                    singleLine = false
                )
                CharCounter(form.description.length, CHALLENGE_DESC_MAX)

                Spacer(Modifier.height(16.dp))
                FieldLabel("Image de couverture")
                CoverPicker(
                    coverUri = form.coverUri,
                    onPick = { coverPicker.launch("image/*") }
                )
            }

            /* ─────────────── 2. Conditions du challenge ─────────────── */
            SectionTitle("Conditions du challenge")
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Date de début")
                        DateField(
                            millis = form.startDateMs,
                            placeholder = "Choisir",
                            onPick = { form = form.copy(startDateMs = it) },
                            context = context
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Date de fin")
                        DateField(
                            millis = form.endDateMs,
                            placeholder = "Choisir",
                            minMillis = form.startDateMs,
                            onPick = { form = form.copy(endDateMs = it) },
                            context = context
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                FieldLabel("Règles de participation")
                UnovTextField(
                    value = form.rules,
                    onValueChange = {
                        if (it.length <= CHALLENGE_RULES_MAX) form = form.copy(rules = it)
                    },
                    placeholder = "Définissez clairement les règles que les participants doivent respecter...",
                    minHeight = 80.dp,
                    singleLine = false
                )
                CharCounter(form.rules.length, CHALLENGE_RULES_MAX)

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Qui peut participer ?")
                        UnovDropdown(
                            selected = form.audience.label,
                            options = Audience.values().map { it.label },
                            onSelect = { i -> form = form.copy(audience = Audience.values()[i]) }
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Âge minimum", optional = true)
                        UnovDropdown(
                            selected = form.minAge.label,
                            options = MinAge.values().map { it.label },
                            onSelect = { i -> form = form.copy(minAge = MinAge.values()[i]) }
                        )
                    }
                }
            }

            /* ─────────────── 3. Récompenses ─────────────── */
            SectionTitle("Récompenses", optional = true)
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Type de récompense")
                        UnovDropdown(
                            selected = form.rewardType.label,
                            options = RewardType.values().map { it.label },
                            onSelect = { i -> form = form.copy(rewardType = RewardType.values()[i]) }
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Montant / Valeur")
                        UnovTextField(
                            value = form.rewardValue,
                            onValueChange = { form = form.copy(rewardValue = it) },
                            placeholder = "100 000 FCFA"
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                HelperText("Cette récompense sera visible par les participants.")
            }

            /* ─────────────── 4. Frais de participation ─────────────── */
            SectionTitle("Frais de participation", optional = true)
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Activer des frais de participation",
                        color = UnovColors.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = form.feesEnabled,
                        onCheckedChange = { form = form.copy(feesEnabled = it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0D0D0D),
                            checkedTrackColor = UnovColors.Accent,
                            checkedBorderColor = UnovColors.Accent,
                            uncheckedThumbColor = UnovColors.TextMute,
                            uncheckedTrackColor = UnovColors.SurfaceAlt,
                            uncheckedBorderColor = UnovColors.LineStrong
                        )
                    )
                }

                if (form.feesEnabled) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(UnovColors.BgRaised)
                            .border(1.dp, UnovColors.Line, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Montant des frais", color = UnovColors.TextDim, fontSize = 14.sp)
                        BasicTextField(
                            value = form.feeAmount,
                            onValueChange = { form = form.copy(feeAmount = it) },
                            textStyle = TextStyle(
                                color = UnovColors.Text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            ),
                            cursorBrush = SolidColor(UnovColors.Accent),
                            singleLine = true,
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterEnd) {
                                    if (form.feeAmount.isEmpty()) {
                                        Text("500 FCFA", color = UnovColors.TextMute, fontSize = 14.sp)
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier.width(120.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    HelperText("UNOVAPP prélève des frais de service sur chaque participation.")
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        /* ─────────────── Lancer + CGU ─────────────── */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(UnovColors.BgBase)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            val enabled = form.isValid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (enabled) Modifier.background(UnovGradients.Gold)
                        else Modifier.background(UnovColors.SurfaceAlt)
                    )
                    .clickable(enabled = enabled) { onLaunch(form) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lancer le challenge",
                    color = if (enabled) Color(0xFF0D0D0D) else UnovColors.TextMute,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "En lançant, vous acceptez les ",
                    color = UnovColors.TextMute,
                    fontSize = 11.sp
                )
                Text(
                    "Conditions d'utilisation.",
                    color = UnovColors.Accent,
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

/* ---------- Briques de formulaire ---------- */

@Composable
private fun SectionTitle(text: String, optional: Boolean = false) {
    Row(
        modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (optional) {
            Text("(optionnel)", color = UnovColors.TextMute, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.Line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun FieldLabel(text: String, optional: Boolean = false) {
    Row(
        modifier = Modifier.padding(bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(text, color = UnovColors.TextDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (optional) Text("(optionnel)", color = UnovColors.TextMute, fontSize = 11.sp)
    }
}

@Composable
private fun HelperText(text: String) {
    Text(text, color = UnovColors.TextMute, fontSize = 11.5.sp, lineHeight = 16.sp)
}

@Composable
private fun CharCounter(current: Int, max: Int) {
    Text(
        text = "$current/$max",
        color = if (current >= max) UnovColors.Accent else UnovColors.TextMute,
        fontSize = 11.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.End
    )
}

@Composable
private fun UnovTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: androidx.compose.ui.unit.Dp = 46.dp,
    singleLine: Boolean = true
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = UnovColors.Text, fontSize = 14.sp, lineHeight = 20.sp),
        cursorBrush = SolidColor(UnovColors.Accent),
        singleLine = singleLine,
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(minHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(UnovColors.BgRaised)
                    .border(1.dp, UnovColors.Line, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = UnovColors.TextMute,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
                inner()
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun UnovDropdown(selected: String, options: List<String>, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(UnovColors.BgRaised)
                .border(1.dp, UnovColors.Line, RoundedCornerShape(12.dp))
                .clickable { open = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                selected,
                color = UnovColors.Text,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = UnovColors.TextMute,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(UnovColors.SurfaceAlt)
        ) {
            options.forEachIndexed { i, label ->
                DropdownMenuItem(
                    text = { Text(label, color = UnovColors.Text, fontSize = 14.sp) },
                    onClick = { onSelect(i); open = false }
                )
            }
        }
    }
}

@Composable
private fun DateField(
    millis: Long?,
    placeholder: String,
    onPick: (Long) -> Unit,
    context: android.content.Context,
    minMillis: Long? = null
) {
    val fmt = remember { SimpleDateFormat("d MMMM yyyy", Locale.FRENCH) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(UnovColors.BgRaised)
            .border(1.dp, UnovColors.Line, RoundedCornerShape(12.dp))
            .clickable {
                val cal = Calendar.getInstance().apply { millis?.let { timeInMillis = it } }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val picked = Calendar.getInstance().apply {
                            set(y, m, d, 12, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onPick(picked.timeInMillis)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    // Une date de fin ne peut pas précéder le début ; rien dans le passé.
                    datePicker.minDate = minMillis ?: System.currentTimeMillis()
                }.show()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = millis?.let { fmt.format(it) } ?: placeholder,
            color = if (millis != null) UnovColors.Text else UnovColors.TextMute,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            Icons.Outlined.CalendarMonth,
            contentDescription = "Choisir une date",
            tint = UnovColors.TextMute,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun CoverPicker(coverUri: String?, onPick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(UnovColors.BgRaised)
            .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(12.dp))
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        if (coverUri != null) {
            AsyncImage(
                model = coverUri,
                contentDescription = "Image de couverture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = UnovColors.TextMute,
                    modifier = Modifier.size(20.dp)
                )
                Text("Ajouter une image", color = UnovColors.TextMute, fontSize = 14.sp)
            }
        }
    }
}
