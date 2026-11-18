package org.scottishtecharmy.soundscape.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The voice picker's contents, checked without AVFoundation - [shapeVoiceList] takes the voices
 * iOS reports and does the filtering, ordering and labelling the picker shows.
 */
class IosTtsVoicesTest {

    private fun voice(
        name: String,
        language: String,
        languageDisplayName: String,
        enhanced: Boolean = false,
        identifier: String = "com.apple.voice.${if (enhanced) "premium" else "compact"}.$language.$name",
    ) = RawTtsVoice(
        identifier = identifier,
        name = name,
        language = language,
        languageCode = language.substringBefore('-'),
        languageDisplayName = languageDisplayName,
        enhanced = enhanced,
    )

    private val daniel = voice("Daniel", "en-GB", "English (United Kingdom)")
    private val karen = voice("Karen", "en-AU", "English (Australia)")
    private val satu = voice("Satu", "fi-FI", "Finnish (Finland)")
    private val anna = voice("Anna", "de-DE", "German (Germany)")

    @Test
    fun voicesInTheAppLanguageComeFirstUnderTheirOwnName() {
        val shaped = shapeVoiceList(listOf(satu, daniel, karen), "en")

        assertEquals(listOf("Karen", "Daniel"), shaped.take(2).map { it.displayName })
    }

    /**
     * The regression this test exists for: street names in Finland are Finnish, so a user running
     * the app in English still has to be able to reach a Finnish voice.
     */
    @Test
    fun voicesInOtherLanguagesAreOfferedWithTheirLanguageNamed() {
        val shaped = shapeVoiceList(listOf(daniel, satu, anna), "en")

        assertEquals(
            listOf("Daniel", "Satu, Finnish (Finland)", "Anna, German (Germany)"),
            shaped.map { it.displayName },
        )
    }

    @Test
    fun otherLanguagesRunInTheOrderTheUserReadsThem() {
        // Sorted by the language as the app names it, not by the raw tag: "Finnish" before
        // "German" where "de-DE" would have come before "fi-FI", so a user scanning for their
        // language finds it where they'd expect.
        val swedish = voice("Alva", "sv-SE", "Swedish (Sweden)")
        val shaped = shapeVoiceList(listOf(swedish, anna, satu), "en")

        assertEquals(
            listOf("Satu, Finnish (Finland)", "Anna, German (Germany)", "Alva, Swedish (Sweden)"),
            shaped.map { it.displayName },
        )
    }

    @Test
    fun roboticLegacyVoicesAreDropped() {
        val fred = voice("Fred", "en-US", "English (United States)", identifier = "com.apple.speech.synthesis.voice.Fred")
        val shaped = shapeVoiceList(listOf(fred, daniel), "en")

        assertEquals(listOf("Daniel"), shaped.map { it.displayName })
    }

    @Test
    fun enhancedVoiceWinsOverItsCompactTwin() {
        val compactSatu = voice("Satu", "fi-FI", "Finnish (Finland)", identifier = "com.apple.voice.compact.fi-FI.Satu")
        val enhancedSatu = voice(
            "Satu",
            "fi-FI",
            "Finnish (Finland)",
            enhanced = true,
            identifier = "com.apple.voice.premium.fi-FI.Satu",
        )
        val shaped = shapeVoiceList(listOf(compactSatu, enhancedSatu, daniel), "en")

        assertEquals(
            listOf("com.apple.voice.premium.fi-FI.Satu"),
            shaped.filter { it.language == "fi-FI" }.map { it.identifier },
        )
    }

    @Test
    fun aCompactVoiceWithNoEnhancedTwinSurvives() {
        val shaped = shapeVoiceList(listOf(satu, daniel), "en")

        assertTrue(shaped.any { it.identifier == satu.identifier })
    }
}
