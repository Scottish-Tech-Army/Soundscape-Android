package org.scottishtecharmy.soundscape.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The voice picker's contents, checked without AVFoundation - [shapeVoiceList] takes the voices
 * iOS reports and does the filtering, ordering and grouping the picker shows.
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

    /**
     * The app running in English (United Kingdom), which is what [shapeVoiceList] leads the list
     * with - see [theAppsOwnLocaleLeadsItsLanguage].
     */
    private fun shape(
        voices: List<RawTtsVoice>,
        languageCode: String = "en",
        localeTag: String = "en-GB",
    ) = shapeVoiceList(voices, languageCode, localeTag)

    private val daniel = voice("Daniel", "en-GB", "English (United Kingdom)")
    private val karen = voice("Karen", "en-AU", "English (Australia)")
    private val satu = voice("Satu", "fi-FI", "Finnish (Finland)")
    private val anna = voice("Anna", "de-DE", "German (Germany)")

    @Test
    fun voicesInTheAppLanguageComeFirst() {
        val shaped = shape(listOf(satu, daniel, karen), "en")

        assertEquals(listOf("Daniel", "Karen", "Satu"), shaped.map { it.name })
        assertEquals(listOf(true, true, false), shaped.map { it.inAppLanguage })
    }

    /**
     * An app running in English (United Kingdom) leads with exactly those voices. Sorting the
     * app's own language by name alone would put English (Australia) first, leaving a user to
     * scroll past accents they didn't ask for to reach their own.
     */
    @Test
    fun theAppsOwnLocaleLeadsItsLanguage() {
        val serenaIe = voice("Serena", "en-IE", "English (Ireland)")
        val samantha = voice("Samantha", "en-US", "English (United States)")
        val shaped = shape(listOf(karen, samantha, serenaIe, daniel, satu))

        assertEquals(
            listOf(
                "English (United Kingdom)",
                "English (Australia)",
                "English (Ireland)",
                "English (United States)",
                "Finnish (Finland)",
            ),
            shaped.map { it.languageDisplayName },
        )
    }

    /** The app's locale is "en_GB" to NSLocale and "en-GB" to a voice. */
    @Test
    fun theAppsOwnLocaleIsMatchedWhateverItsCase() {
        val shaped = shape(listOf(karen, daniel), localeTag = "EN-gb")

        assertEquals(listOf("Daniel", "Karen"), shaped.map { it.name })
    }

    /**
     * The regression this test exists for: street names in Finland are Finnish, so a user running
     * the app in English still has to be able to reach a Finnish voice.
     */
    @Test
    fun voicesInOtherLanguagesAreOffered() {
        val shaped = shape(listOf(daniel, satu, anna), "en")

        assertTrue(shaped.any { it.name == "Satu" && it.languageDisplayName == "Finnish (Finland)" })
    }

    @Test
    fun eachLanguagesVoicesAreTogether() {
        // The picker groups on languageDisplayName, so a language's voices have to arrive in one
        // run rather than interleaved with another's.
        val eddyFi = voice("Eddy", "fi-FI", "Finnish (Finland)")
        val eddyDe = voice("Eddy", "de-DE", "German (Germany)")
        val shaped = shape(listOf(eddyDe, satu, anna, eddyFi, daniel), "en")

        assertEquals(
            listOf(
                "English (United Kingdom)",
                "Finnish (Finland)",
                "Finnish (Finland)",
                "German (Germany)",
                "German (Germany)",
            ),
            shaped.map { it.languageDisplayName },
        )
    }

    @Test
    fun languagesRunInTheOrderTheUserReadsThem() {
        // Sorted by the language as the app names it, not by the raw tag: "Finnish" before
        // "German" where "de-DE" would have come before "fi-FI", so a user scanning for their
        // language finds it where they'd expect.
        val alva = voice("Alva", "sv-SE", "Swedish (Sweden)")
        val shaped = shape(listOf(alva, anna, satu), "en")

        assertEquals(
            listOf("Finnish (Finland)", "German (Germany)", "Swedish (Sweden)"),
            shaped.map { it.languageDisplayName },
        )
    }

    /**
     * iOS ships the Eloquence voices in every language they cover - there is a Flo and an Eddy in
     * each - and reports the bare name for all of them, so a name only identifies a voice within
     * its own language. Each of those is a distinct voice and all of them are kept.
     */
    @Test
    fun theSameNameInSeveralLanguagesIsSeveralVoices() {
        val floGb = voice("Flo", "en-GB", "English (United Kingdom)", identifier = "com.apple.eloquence.en-GB.Flo")
        val floUs = voice("Flo", "en-US", "English (United States)", identifier = "com.apple.eloquence.en-US.Flo")
        val floFi = voice("Flo", "fi-FI", "Finnish (Finland)", identifier = "com.apple.eloquence.fi-FI.Flo")
        val shaped = shape(listOf(floGb, floUs, floFi), "en")

        assertEquals(3, shaped.size)
        assertEquals(
            listOf(
                "English (United Kingdom)",
                "English (United States)",
                "Finnish (Finland)",
            ),
            shaped.map { it.languageDisplayName },
        )
    }

    /**
     * Two providers can ship a voice of the same name in the same language, and they are
     * different voices: on a phone with eSpeak installed, eSpeak and Eloquence both have a
     * Grandma, a Grandpa and a Reed in English. Deduping on the name alone would throw one of
     * each away.
     */
    @Test
    fun oneNameFromTwoProvidersIsTwoVoices() {
        val eloquenceGrandma = voice(
            "Grandma",
            "en-GB",
            "English (United Kingdom)",
            identifier = "com.apple.eloquence.en-GB.Grandma",
        )
        val espeakGrandma = voice(
            "Grandma",
            "en-GB",
            "English (United Kingdom)",
            identifier = "dj.phoenix.espeak-ng.synth-ext.auto.en-gb.Grandma",
        )
        val shaped = shape(listOf(eloquenceGrandma, espeakGrandma), "en")

        assertEquals(
            listOf(IosVoiceFamily.ELOQUENCE, IosVoiceFamily.ESPEAK),
            shaped.map { it.family },
        )
    }

    /** Within one language a name appears once, which is what lets the picker show it alone. */
    @Test
    fun theSameNameTwiceInOneLanguageIsOneVoice() {
        val oldScheme = voice("Daniel", "en-GB", "English (United Kingdom)", identifier = "com.apple.ttsbundle.Daniel-compact")
        val shaped = shape(listOf(oldScheme, daniel), "en")

        assertEquals(1, shaped.size)
    }

    @Test
    fun theBetterSoundingVoiceWinsADuplicateName() {
        val enhancedDaniel = voice(
            "Daniel",
            "en-GB",
            "English (United Kingdom)",
            enhanced = true,
            identifier = "com.apple.ttsbundle.Daniel-enhanced",
        )
        val shaped = shape(listOf(daniel, enhancedDaniel), "en")

        assertEquals(listOf("com.apple.ttsbundle.Daniel-enhanced"), shaped.map { it.identifier })
    }

    /** Alex is an ordinary voice, so only the legacy app's own suppression list catches it. */
    @Test
    fun roboticLegacyVoicesAreDropped() {
        val alex = voice("Alex", "en-US", "English (United States)", identifier = "com.apple.speech.voice.Alex")
        val shaped = shape(listOf(alex, daniel), "en")

        assertEquals(listOf("Daniel"), shaped.map { it.name })
    }

    /**
     * Bubbles and Zarvox are jokes, and this is an app a blind user navigates by - they have no
     * business in a list of voices to pick from, so they never reach it.
     */
    @Test
    fun noveltyVoicesAreNotOffered() {
        val zarvox = voice("Zarvox", "en-US", "English (United States)", identifier = "com.apple.speech.synthesis.voice.Zarvox")
        val badNews = voice("Bad News", "en-US", "English (United States)", identifier = "com.apple.speech.synthesis.voice.BadNews")
        val shaped = shape(listOf(zarvox, daniel, badNews), "en")

        assertEquals(listOf("Daniel"), shaped.map { it.name })
    }

    @Test
    fun applesOwnFamiliesAreToldApartByIdentifier() {
        assertEquals(IosVoiceFamily.ELOQUENCE, voiceFamilyOf("com.apple.eloquence.en-GB.Flo"))
        assertEquals(IosVoiceFamily.NOVELTY, voiceFamilyOf("com.apple.speech.synthesis.voice.Zarvox"))
        assertEquals(IosVoiceFamily.STANDARD, voiceFamilyOf("com.apple.voice.compact.en-GB.Daniel"))
        assertEquals(IosVoiceFamily.STANDARD, voiceFamilyOf("com.apple.voice.super-compact.fi-FI.Satu"))
        assertEquals(IosVoiceFamily.STANDARD, voiceFamilyOf("com.apple.ttsbundle.Daniel-compact"))
    }

    /**
     * Users install whatever speech provider they like, so a voice from outside Apple is either
     * one we can name or one we can only say is somebody else's.
     */
    @Test
    fun installedProvidersAreToldApartByIdentifier() {
        assertEquals(IosVoiceFamily.ESPEAK, voiceFamilyOf("dj.phoenix.espeak-ng.synth-ext.auto.en-gb.Alex"))
        assertEquals(IosVoiceFamily.ESPEAK, voiceFamilyOf("com.reecedunn.espeak.voice.en"))
        // A provider chooses its own identifiers and nothing promises their case.
        assertEquals(IosVoiceFamily.ESPEAK, voiceFamilyOf("com.example.eSpeakNG.en-GB"))
        assertEquals(IosVoiceFamily.OTHER, voiceFamilyOf("com.acapela.voices.en-GB.Rachel"))
        assertEquals(IosVoiceFamily.OTHER, voiceFamilyOf("io.vocalizer.en-GB.Serena"))
    }

    /**
     * The picker shows each language's voices as Apple's own, then a sub-list per provider, and
     * takes that order from here - so a language's voices have to arrive grouped by family, with
     * the ones somebody would navigate by first.
     */
    @Test
    fun aLanguagesVoicesAreGroupedByFamilyOrdinaryFirst() {
        val flo = voice("Flo", "en-GB", "English (United Kingdom)", identifier = "com.apple.eloquence.en-GB.Flo")
        val eddy = voice("Eddy", "en-GB", "English (United Kingdom)", identifier = "com.apple.eloquence.en-GB.Eddy")
        val espeakGb = voice("English", "en-GB", "English (United Kingdom)", identifier = "dj.phoenix.espeak-ng.synth-ext.auto.en-gb.Gene")
        val other = voice("Rachel", "en-GB", "English (United Kingdom)", identifier = "com.acapela.voices.en-GB.Rachel")
        val serena = voice("Serena", "en-GB", "English (United Kingdom)")
        val shaped = shape(listOf(flo, espeakGb, other, serena, eddy, daniel), "en")

        assertEquals(
            listOf("Daniel", "Serena", "Eddy", "Flo", "English", "Rachel"),
            shaped.map { it.name },
        )
        assertEquals(
            listOf(
                IosVoiceFamily.STANDARD,
                IosVoiceFamily.STANDARD,
                IosVoiceFamily.ELOQUENCE,
                IosVoiceFamily.ELOQUENCE,
                IosVoiceFamily.ESPEAK,
                IosVoiceFamily.OTHER,
            ),
            shaped.map { it.family },
        )
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
        val shaped = shape(listOf(compactSatu, enhancedSatu, daniel), "en")

        assertEquals(
            listOf("com.apple.voice.premium.fi-FI.Satu"),
            shaped.filter { it.language == "fi-FI" }.map { it.identifier },
        )
    }

    @Test
    fun aCompactVoiceWithNoEnhancedTwinSurvives() {
        val shaped = shape(listOf(satu, daniel), "en")

        assertTrue(shaped.any { it.identifier == satu.identifier })
    }
}
