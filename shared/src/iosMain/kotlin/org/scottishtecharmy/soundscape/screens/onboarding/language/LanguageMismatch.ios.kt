package org.scottishtecharmy.soundscape.screens.onboarding.language

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

actual fun getAppLocale(): LocaleSnapshot? {
    // iOS records an explicit per-app locale by writing into the
    // AppleLanguages array on standardUserDefaults. If we haven't set anything,
    // there's no override and we should report null.
    val tags = NSUserDefaults.standardUserDefaults.arrayForKey(APPLE_LANGUAGES_KEY)
        ?: return null
    val tag = tags.firstOrNull() as? String ?: return null
    return parseTag(tag)
}

actual fun getSystemLocale(): LocaleSnapshot {
    // NSLocale.preferredLanguages returns the user's system-level language
    // preferences regardless of any per-app override; the first entry is the
    // primary language.
    val systemTag = (NSLocale.preferredLanguages.firstOrNull() as? String)
        ?: NSLocale.currentLocale.languageCode
    val parsed = parseTag(systemTag)
    if (parsed.region != null) return parsed
    // Fall back to the current locale's country code if the system tag doesn't
    // include a region (common for "en", "fr" etc).
    val region = NSLocale.currentLocale.countryCode
    return LocaleSnapshot(parsed.language, region)
}

/** Splits a BCP-47 tag like "en-GB" or "zh_Hans-CN" into language/region. */
private fun parseTag(tag: String): LocaleSnapshot {
    val normalized = tag.replace('_', '-')
    val parts = normalized.split('-')
    val language = parts.firstOrNull().orEmpty()
    // The region is typically a 2-letter uppercase code at the end. Skip
    // script subtags like "Hans" / "Latn" by picking the last part if it's
    // 2 chars or 3 digits.
    val region = parts.drop(1).lastOrNull { part ->
        (part.length == 2 && part.all { it.isLetter() }) ||
                (part.length == 3 && part.all { it.isDigit() })
    }?.uppercase()
    return LocaleSnapshot(language.lowercase(), region)
}
