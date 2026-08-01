@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.unovapp.android.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.unovapp.android.R

/**
 * La typographie « braise » — Hanken Grotesk, embarquée.
 *
 * On bundle la fonte variable pour ne PAS dépendre de la police système : certains
 * Android (Xiaomi/HyperOS) rendent `FontFamily.SansSerif` en arrondi manuscrit, ce
 * qui écrasait complètement le caractère des titres de la maquette.
 *
 * Définie ici — et non dans le kit d'onboarding où elle vivait — parce que tous les
 * écrans migrés vers la maquette l'utilisent, pas seulement l'onboarding.
 */
private fun hk(w: Int) = Font(
    R.font.hanken_grotesk,
    weight = FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w))
)

val EmberFont = FontFamily(
    hk(400), hk(500), hk(600), hk(700), hk(800), hk(900)
)
