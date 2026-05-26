package com.unovapp.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboarding_affiche_le_titre_unovapp() {
        composeTestRule
            .onNodeWithText("UNOVAPP")
            .assertIsDisplayed()
    }

    @Test
    fun onboarding_bouton_suivant_existe() {
        composeTestRule
            .onNodeWithText("Suivant →")
            .assertIsDisplayed()
    }

    @Test
    fun onboarding_navigation_slide2_au_clic() {
        composeTestRule
            .onNodeWithText("Suivant →")
            .performClick()

        composeTestRule
            .onNodeWithText("Lance des Battles")
            .assertIsDisplayed()
    }

    @Test
    fun onboarding_passer_va_a_inscription() {
        composeTestRule
            .onNodeWithText("Passer")
            .performClick()

        composeTestRule
            .onNodeWithText("Créer un compte")
            .assertIsDisplayed()
    }
}
