package com.unovapp.android

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unovapp.android.ui.auth.AuthScreen
import com.unovapp.android.ui.interests.InterestsScreen
import com.unovapp.android.ui.main.MainScreen
import com.unovapp.android.ui.onboarding.OnboardingScreen
import com.unovapp.android.ui.theme.UnovMotion

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    /** Choix des centres d'intérêt — après l'inscription, avant le feed. */
    object Interests : Screen("interests")
    object Main : Screen("main")
}

/**
 * Transitions inter-écrans inspirées du **shared-axis Material 3 Expressive**.
 *
 *  - Onboarding → Auth : axe Y (l'utilisateur "monte" vers l'authentification)
 *  - Auth → Main : zoom-in + fade (révélation héroïque du contenu principal)
 *  - Back : transition inverse pour réinforcer la sensation spatiale
 */
@Composable
fun UnovAppNavigation(
    startDestination: String = Screen.Onboarding.route,
    verifyEmailToken: String? = null
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = Screen.Onboarding.route,
            // Pas d'enter (premier écran), mais exit en glissant vers le haut
            exitTransition = {
                slideOutOfContainer(
                    towards = SlideDirection.Up,
                    animationSpec = UnovMotion.accelerate(UnovMotion.DurationStandard)
                ) + fadeOut(UnovMotion.fast())
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = SlideDirection.Down,
                    animationSpec = UnovMotion.decelerate(UnovMotion.DurationStandard)
                ) + fadeIn(UnovMotion.standard())
            }
        ) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onLogin = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Auth.route,
            enterTransition = {
                slideIntoContainer(
                    towards = SlideDirection.Up,
                    animationSpec = UnovMotion.decelerate(UnovMotion.DurationStandard)
                ) + fadeIn(UnovMotion.standard())
            },
            exitTransition = {
                // Sortie vers Main : zoom out + fade (révèle le feed dans un effet "voile qui tombe")
                scaleOut(
                    targetScale = 1.08f,
                    animationSpec = UnovMotion.accelerate(UnovMotion.DurationSlow)
                ) + fadeOut(UnovMotion.fast())
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = SlideDirection.Down,
                    animationSpec = UnovMotion.accelerate(UnovMotion.DurationStandard)
                ) + fadeOut(UnovMotion.fast())
            }
        ) {
            AuthScreen(
                verifyEmailToken = verifyEmailToken,
                onAuthenticated = {
                    // Étape centres d'intérêt : elle se saute d'elle-même si l'utilisateur en
                    // a déjà (compte existant) → seuls les nouveaux inscrits la voient.
                    navController.navigate(Screen.Interests.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Onboarding.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Interests.route,
            enterTransition = { fadeIn(UnovMotion.standard()) },
            exitTransition = {
                scaleOut(
                    targetScale = 1.08f,
                    animationSpec = UnovMotion.accelerate(UnovMotion.DurationSlow)
                ) + fadeOut(UnovMotion.fast())
            }
        ) {
            InterestsScreen(
                onDone = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Interests.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Main.route,
            enterTransition = {
                // Apparition héroïque du contenu : zoom in léger + fade depuis le centre
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = UnovMotion.decelerate(UnovMotion.DurationSlow)
                ) + fadeIn(UnovMotion.standard())
            }
        ) {
            MainScreen(
                onLoggedOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
