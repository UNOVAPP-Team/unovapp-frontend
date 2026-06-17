# ============================================================================
# Règles R8/ProGuard — UNOVAPP
# La plupart des libs (Retrofit, OkHttp, Gson, Hilt, Coil, Media3) fournissent
# leurs propres "consumer rules". Ici on protège surtout NOS modèles sérialisés.
# ============================================================================

# --- Attributs nécessaires à la réflexion (Gson generics, annotations, etc.) ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# --- NOS DTO / data layer : sérialisés par Gson via réflexion sur les noms de
#     champs → interdiction de les renommer/supprimer (sinon le JSON ne parse plus). ---
-keep class com.unovapp.android.data.** { *; }

# --- Gson : conserver les champs annotés @SerializedName ---
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# --- Retrofit : conserver les méthodes annotées des interfaces API ---
-keepattributes Exceptions
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# --- BuildConfig (URLs / flags lus au runtime) ---
-keep class com.unovapp.android.BuildConfig { *; }

# --- Réduction des warnings (libs avec annotations optionnelles) ---
-dontwarn kotlinx.coroutines.**
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
