package com.unovapp.android

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unovapp.android.ui.auth.AuthScreen
import com.unovapp.android.ui.main.MainScreen
import com.unovapp.android.ui.onboarding.OnboardingScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Main : Screen("main")
}

@Composable
fun UnovAppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
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
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Screen.Main.route) {
                        // Vide la backstack auth — pas de retour possible sans logout
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
        composable(Screen.Main.route) {
            MainScreen()
        }
    }
}
