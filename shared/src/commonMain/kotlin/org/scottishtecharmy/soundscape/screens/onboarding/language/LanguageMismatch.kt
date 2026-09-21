package org.scottishtecharmy.soundscape.screens.onboarding.language

/** Snapshot of a locale: BCP-47-style language code plus optional region. */
data class LocaleSnapshot(val language: String, val region: String?)

/**
 * Returns the locale the user explicitly chose for the app, or `null` if the app
 * is following the system default. Android backs this with
 * [androidx.appcompat.app.AppCompatDelegate.getApplicationLocales]; iOS reads
 * the first entry of `NSUserDefaults["AppleLanguages"]`.
 */
expect fun getAppLocale(): LocaleSnapshot?

/** Returns the device's current system locale. */
expect fun getSystemLocale(): LocaleSnapshot

/**
 * Returns a supported [Language] when the system locale and the explicit app
 * locale disagree but the system locale is something Soundscape can offer, so
 * the UI can prompt the user to switch. `null` means there's nothing to nag
 * about — either there's no app override, the languages already match, or the
 * system locale isn't in the supported list.
 *
 * Prefers an exact language+region match, falling back to a language-only match.
 */
fun getLanguageMismatch(): Language? {
    val app = getAppLocale() ?: return null
    val system = getSystemLocale()
    if (app.language == system.language) return null

    var languageOnlyMatch: Language? = null
    for (language in supportedLanguages) {
        if (language.code == system.language && language.region == system.region) {
            return language
        }
        if (language.code == system.language && languageOnlyMatch == null) {
            languageOnlyMatch = language
        }
    }
    return languageOnlyMatch
}
