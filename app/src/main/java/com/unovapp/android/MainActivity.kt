package com.unovapp.android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Hérite d'[AppCompatActivity] (et non plus [androidx.activity.ComponentActivity]) pour
 * permettre à `AppCompatDelegate.setApplicationLocales` de propager les changements de
 * langue jusqu'aux ressources Compose — voir [LocaleManager].
 *
 * Aucun composant Material/AppCompat n'est utilisé visuellement : on est full Compose +
 * Material 3, AppCompat sert uniquement de relais d'API langue.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenStore: TokenDataStore

    // Token de vérification email reçu par deep link (lien cliqué dans l'email).
    private var deepLinkToken by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        deepLinkToken = extractVerificationToken(intent)

        setContent {
            // Décision de la destination de départ : si une session existe → Main directement
            // (identifiants pris en compte), sinon Onboarding. `null` = en cours de lecture.
            var startRoute by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                startRoute = if (tokenStore.readAccessToken() != null) {
                    Screen.Main.route
                } else {
                    Screen.Onboarding.route
                }
            }

            val token = deepLinkToken
            when {
                // Un deep link de vérification prime : on ouvre l'auth pour valider le token.
                token != null -> UnovAppNavigation(
                    startDestination = Screen.Auth.route,
                    verifyEmailToken = token
                )
                startRoute != null -> UnovAppNavigation(startDestination = startRoute!!)
                else -> Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF050505)))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractVerificationToken(intent)?.let { deepLinkToken = it }
    }

    /** Extrait le `?token=` d'un lien de vérification email (https ou schéma `unovapp://`). */
    private fun extractVerificationToken(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return data.getQueryParameter("token")
    }
}
