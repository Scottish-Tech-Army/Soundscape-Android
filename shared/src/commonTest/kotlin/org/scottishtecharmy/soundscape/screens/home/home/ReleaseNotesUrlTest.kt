package org.scottishtecharmy.soundscape.screens.home.home

import org.scottishtecharmy.soundscape.screens.onboarding.language.LocaleSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseNotesUrlTest {
    private val site = "https://scottish-tech-army.github.io/Soundscape-Android"

    private fun url(language: String, region: String?) =
        releaseNotesUrl(LocaleSnapshot(language, region))

    @Test
    fun englishIsAtTheRoot() {
        assertEquals("$site/release-notes.html", url("en", "US"))
        assertEquals("$site/release-notes.html", url("en", null))
    }

    @Test
    fun aSupportedLanguageIsUnderItsCode() {
        assertEquals("$site/de/release-notes.html", url("de", "DE"))
        assertEquals("$site/de/release-notes.html", url("de", "AT"))
        assertEquals("$site/ja/release-notes.html", url("ja", null))
    }

    @Test
    fun regionalVariantsKeepTheirRegion() {
        assertEquals("$site/en-GB/release-notes.html", url("en", "GB"))
        assertEquals("$site/fr-CA/release-notes.html", url("fr", "CA"))
        assertEquals("$site/fr/release-notes.html", url("fr", "FR"))
        assertEquals("$site/pt-BR/release-notes.html", url("pt", "BR"))
        assertEquals("$site/pt/release-notes.html", url("pt", "PT"))
    }

    @Test
    fun chineseIsOnlyPublishedAsZhCn() {
        assertEquals("$site/zh-CN/release-notes.html", url("zh", "CN"))
        assertEquals("$site/zh-CN/release-notes.html", url("zh", "TW"))
    }

    @Test
    fun anUnsupportedLanguageFallsBackToEnglish() {
        assertEquals("$site/release-notes.html", url("xx", null))
    }
}
