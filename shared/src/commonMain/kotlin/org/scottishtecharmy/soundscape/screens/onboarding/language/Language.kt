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
    Language("العربية", "ar", "SA"),
    Language("العربية المصرية", "arz", "EG"),
    Language("বাংলা", "bn", "BD"),
    Language("中文 (简体)", "zh", "CN"),
    Language("Čeština", "cs", "CZ"),
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
    Language("Hausa", "ha", "NG"),
    Language("हिंदी", "hi", "IN"),
    Language("Hrvatski", "hr", "HR"),
    Language("Magyar", "hu", "HU"),
    Language("Bahasa Indonesia", "id", "ID"),
    Language("Íslenska", "is", "IS"),
    Language("Italiano", "it", "IT"),
    Language("日本語", "ja", "JP"),
    Language("한국어", "ko", "KR"),
    Language("मराठी", "mr", "IN"),
    Language("Norsk", "nb", "NO"),
    Language("Nederlands", "nl", "NL"),
    Language("Polski", "pl", "PL"),
    Language("Português (Portugal)", "pt", "PT"),
    Language("Português (Brasil)", "pt", "BR"),
    Language("Русский", "ru", "RU"),
    Language("Română", "ro", "RO"),
    Language("Slovenčina", "sk", "SK"),
    Language("Српски", "sr", "RS"),
    Language("Svenska", "sv", "SE"),
    Language("Kiswahili", "sw", "TZ"),
    Language("தமிழ்", "ta", "IN"),
    Language("తెలుగు", "te", "IN"),
    Language("ไทย", "th", "TH"),
    Language("Türkçe", "tr", "TR"),
    Language("Українська", "uk", "UA"),
    Language("اردو", "ur", "PK"),
    Language("Tiếng Việt", "vi", "VN"),
)
