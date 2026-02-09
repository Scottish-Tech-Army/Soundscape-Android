package org.scottishtecharmy.soundscape.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import org.scottishtecharmy.soundscape.screens.onboarding.language.Language
import java.util.Locale

/**
 * Return the application locale if there is one, otherwise return the default system one
 */
fun getCurrentLocale() : Locale {
    val appLocale = AppCompatDelegate.getApplicationLocales()[0]
    return appLocale ?: Locale.getDefault()
}

/**
 * All languages supported by the app. This is the single source of truth used by both the
 * onboarding language picker and the language mismatch detection.
 */
val supportedLanguages = listOf(
    Language("العربية المصرية", "arz", "EG"),
    Language("中国人", "zh", "CN"),
    Language("Dansk", "da", "DK"),
    Language("Deutsch", "de", "DE"),
    Language("Ελληνικά", "el", "GR"),
    Language("English", "en", "US"),
    Language("English (UK)", "en", "GB"),
    Language("Español", "es", "ES"),
    Language("فارسی", "fa", "IR"),
    Language("Suomi", "fi", "FI"),
    Language("Français (France)", "fr", "FR"),
    Language("Français (Canada)", "fr", "CA"),
    Language("हिंदी", "hi", "IN"),
    Language("Íslenska", "is", "IS"),
    Language("Italiano", "it", "IT"),
    Language("日本語", "ja", "JP"),
    Language("Norsk", "nb", "NO"),
    Language("Nederlands", "nl", "NL"),
    Language("Polski", "pl", "PL"),
    Language("Português (Portugal)", "pt", "PT"),
    Language("Português (Brasil)", "pt", "BR"),
    Language("Русский", "ru", "RU"),
    Language("Română", "ro", "RO"),
    Language("Svenska", "sv", "SE"),
    Language("українська", "uk", "UK"),
)

/**
 * Check if the phone's language differs from the app's configured language and is supported.
 * Returns the matching [Language] if the phone language is supported but different from the
 * app language, or null if they match or the phone language is not supported.
 */
fun getLanguageMismatch(context: Context): Language? {
    val appLocale = AppCompatDelegate.getApplicationLocales()[0]
        ?: return null // No explicit app locale set — using system default, so no mismatch

    // Get the actual device locale. On API 33+, LocaleManager.systemLocales gives the real
    // system locales unaffected by per-app locale settings. On older APIs, fall back to
    // Resources.getSystem() (which may be affected by AppCompatDelegate on some devices).
    val phoneLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager.systemLocales[0]
    } else {
        Resources.getSystem().configuration.locales[0]
    }

    println("phoneLocale $phoneLocale vs appLocale $appLocale")

    if (appLocale.language == phoneLocale.language) return null

    // Find the best matching supported language for the phone locale
    var bestMatch: Language? = null
    for (language in supportedLanguages) {
        if (language.code == phoneLocale.language && language.region == phoneLocale.country) {
            return language // Exact match on language + region
        }
        if (language.code == phoneLocale.language && bestMatch == null) {
            bestMatch = language // Language-only match as fallback
        }
    }
    return bestMatch
}
