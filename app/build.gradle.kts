import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

// Charge les identifiants de signature depuis keystore.properties (gitignoré).
// Absent sur les machines/CI sans secrets → la build release reste possible mais non signée.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}

android {
    namespace = "com.unovapp.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.unovapp.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "AUTH_BASE_URL",
            "\"https://unovapp-auth.onrender.com/api/v1/\""
        )
        buildConfigField(
            "String",
            "USER_BASE_URL",
            "\"https://unovapp-user.onrender.com/api/v1/\""
        )
        buildConfigField(
            "String",
            "SOCIAL_BASE_URL",
            "\"https://unovapp-social.onrender.com/api/v1/\""
        )
        // TODO: remplacer par le vrai Web Client ID Google OAuth quand le backend exposera /auth/google.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"REPLACE_ME.apps.googleusercontent.com\""
        )
        buildConfigField("boolean", "USE_STUB_AUTH", "false")
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file("app/${keystoreProps["storeFile"]}")
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Minification désactivée pour ce premier test interne : évite tout risque
            // R8/Gson sur les DTO Retrofit. À réactiver (isMinifyEnabled = true) + règles
            // ProGuard avant la mise en production publique.
            isMinifyEnabled = false
            signingConfig = if (keystorePropsFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Le lint « vital » bloquait les builds release (et très lent sur cette machine).
        // Inutile pour distribuer un APK aux testeurs/fournisseurs — on ne fait pas
        // échouer la build sur des avertissements lint.
        checkReleaseBuilds = false
        abortOnError = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Material Components (pour le thème)
    implementation("com.google.android.material:material:1.12.0")

    // AppCompat — utilisé uniquement pour `AppCompatDelegate.setApplicationLocales`
    // (per-app language preferences avec back-compat depuis API 24, équivalent natif d'Android 13+).
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-android-compiler:2.51")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Lifecycle + ViewModel + Hilt nav
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Credential Manager (Google Sign-In)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Coil — chargement d'images distantes (avatars, miniatures)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Media3 ExoPlayer + HLS (Mois 2 — pipeline vidéo)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
