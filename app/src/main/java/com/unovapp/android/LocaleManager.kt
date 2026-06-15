package com.unovapp.android

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Gestion centralisée de la langue de l'app.
 *
 * Sous le capot, on s'appuie sur **`AppCompatDelegate.setApplicationLocales`** qui :
 *  - sur Android 13+, délègue au système (per-app language preferences natif)
 *  - sur Android 5.0–12, gère la persistance et la propagation manuelles
 *
 * La langue choisie persiste automatiquement entre les lancements (AppCompat le fait
 * pour nous). On expose une enum [AppLanguage] pour que l'UI travaille avec un type
 * plus clair que des codes BCP 47 bruts.
 */
enum class AppLanguage(val tag: String) {
    French("fr"),
    Fon("fon"),
    Yoruba("yo");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { tag?.startsWith(it.tag) == true } ?: French
    }
}

object LocaleManager {

    /**
     * Renvoie la langue actuellement appliquée. Si l'utilisateur n'a jamais choisi,
     * on tombe par défaut sur le français (langue par défaut des `res/values/`).
     */
    fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (locales.isEmpty) null else locales[0]?.language
        return AppLanguage.fromTag(tag)
    }

    /**
     * Applique une nouvelle langue. AppCompat la persiste et déclenche une recréation
     * des activités visibles pour propager le changement aux composables (qui re-résoudront
     * leurs `stringResource()`).
     */
    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.tag)
        )
    }
}
