package com.unovapp.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* choix utilisateur */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        deepLinkToken = extractVerificationToken(intent)
        requestNotificationPermissionIfNeeded()

        setContent {
            // Lecture synchrone depuis les EncryptedSharedPreferences (déjà chargées en mémoire
            // par Hilt avant setContent). Pas de LaunchedEffect asynchrone → pas de flash noir,
            // décision prise au premier frame, compatible avec le splash screen Android 12+.
            val startRoute = remember {
                if (tokenStore.readAccessTokenSync() != null) Screen.Main.route
                else Screen.Onboarding.route
            }

            val token = deepLinkToken
            if (token != null) {
                // Un deep link de vérification prime sur la destination calculée.
                UnovAppNavigation(startDestination = Screen.Auth.route, verifyEmailToken = token)
            } else {
                UnovAppNavigation(startDestination = startRoute)
            }
        }
    }

    /** Demande POST_NOTIFICATIONS sur Android 13+ (téléchargements + activité). */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractVerificationToken(intent)?.let { deepLinkToken = it }
    }

    /**
     * Extrait le `?token=` d'un lien de vérification email (https ou schéma `unovapp://`).
     * Valide le host avant extraction pour bloquer les Intents forgées par d'autres apps
     * (risque si assetlinks.json absent et autoVerify non confirmé par Android).
     */
    private fun extractVerificationToken(intent: Intent?): String? {
        val data = intent?.data ?: return null
        // Seul le schéma custom `unovapp://verify-email?token=...` est accepté (la vérif
        // email se fait par OTP in-app ; plus de lien web vers le backend).
        val validHosts = setOf("verify-email")
        if (data.host !in validHosts) return null
        return data.getQueryParameter("token")?.takeIf { it.isNotBlank() }
    }
}
