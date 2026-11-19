package org.scottishtecharmy.soundscape.viewmodels

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * The Android voice picker's list - see [voiceOptions]. Robolectric because android.speech.tts.Voice
 * is a framework class.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class VoiceOptionsTest {

    private fun voice(
        name: String,
        locale: Locale,
        networkRequired: Boolean = false,
        features: Set<String> = emptySet(),
    ) = Voice(
        name,
        locale,
        Voice.QUALITY_NORMAL,
        Voice.LATENCY_NORMAL,
        networkRequired,
        features,
    )

    @Test
    fun appLocaleThenAppLanguageThenOtherLanguagesByName() {
        // Finnish before German: other languages go by their name in the app's language.
        val voices = listOf(
            voice("fi-fi-x-a", Locale.of("fi", "FI")),
            voice("en-us-x-b", Locale.of("en", "US")),
            voice("de-de-x-c", Locale.of("de", "DE")),
            voice("en-gb-x-b", Locale.of("en", "GB")),
            voice("en-gb-x-a", Locale.of("en", "GB")),
        )

        val options = voiceOptions(voices, Locale.of("en", "GB"))

        assertEquals(
            listOf("en-gb-x-a", "en-gb-x-b", "en-us-x-b", "fi-fi-x-a", "de-de-x-c"),
            options.map { it.identifier },
        )
        assertEquals(
            listOf(true, true, true, false, false),
            options.map { it.inAppLanguage },
        )
        assertEquals("English (United Kingdom)", options.first().languageDisplayName)
    }

    @Test
    fun threeLetterLocalesMatchTheAppLanguage() {
        // Some engines report "eng_GBR" rather than "en_GB".
        val options = voiceOptions(
            listOf(
                voice("other", Locale.of("fr", "FR")),
                voice("three-letter", Locale.of("eng", "GBR")),
            ),
            Locale.of("en", "GB"),
        )

        assertEquals("three-letter", options.first().identifier)
        assertEquals(true, options.first().inAppLanguage)
    }

    @Test
    fun languageNamesAreCapitalisedInTheAppLanguage() {
        // French writes language names in lower case ("anglais"), which reads oddly as a heading.
        val options = voiceOptions(listOf(voice("v", Locale.of("en", "GB"))), Locale.of("fr", "FR"))

        assertEquals("Anglais (Royaume-Uni)", options.single().languageDisplayName)
    }

    @Test
    fun networkAndUndownloadedVoicesAreLeftOut() {
        val options = voiceOptions(
            listOf(
                voice("local", Locale.of("en", "GB")),
                voice("network", Locale.of("en", "GB"), networkRequired = true),
                voice(
                    "missing",
                    Locale.of("en", "GB"),
                    features = setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED),
                ),
            ),
            Locale.of("en", "GB"),
        )

        assertEquals(listOf("local"), options.map { it.identifier })
    }
}
