package org.scottishtecharmy.soundscape.screens.onboarding.language

data class Language(
    val name: String,
    val code: String,
    val region: String,
)

/**
 * All languages supported by the app. Single source of truth used by the onboarding
 * language picker and the language mismatch detection.
 */
val supportedLanguages: List<Language> = listOf(
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
    Language("Türkçe", "tr", "TR"),
    Language("Українська", "uk", "UA"),
)
