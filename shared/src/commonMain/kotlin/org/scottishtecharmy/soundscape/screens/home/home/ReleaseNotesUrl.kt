package org.scottishtecharmy.soundscape.screens.home.home

import org.scottishtecharmy.soundscape.screens.onboarding.language.LocaleSnapshot
import org.scottishtecharmy.soundscape.screens.onboarding.language.getAppLocale
import org.scottishtecharmy.soundscape.screens.onboarding.language.getSystemLocale
import org.scottishtecharmy.soundscape.screens.onboarding.language.supportedLanguages

private const val WEBSITE = "https://scottish-tech-army.github.io/Soundscape-Android"

/**
 * The release notes page on the website, in the language the app is showing.
 *
 * The site is built by jekyll-polyglot: English is at the root and every other language under
 * /<lang>/, using the codes in docs/_config.yml. A page nobody has translated is still published
 * there, in English, so any language the app supports has a page to land on. Only four of those
 * codes carry a region - the ones where the app offers two variants of a language, plus Chinese,
 * which the site only has as zh-CN.
 */
fun releaseNotesUrl(locale: LocaleSnapshot = getAppLocale() ?: getSystemLocale()): String {
    val language = locale.language
    val path = when {
        language == "en" && locale.region == "GB" -> "en-GB/"
        language == "fr" && locale.region == "CA" -> "fr-CA/"
        language == "pt" && locale.region == "BR" -> "pt-BR/"
        language == "zh" -> "zh-CN/"
        language == "en" -> ""
        supportedLanguages.any { it.code == language } -> "$language/"
        else -> ""
    }
    return "$WEBSITE/${path}release-notes.html"
}
