package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesisVoiceQualityEnhanced
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localizedStringForLocaleIdentifier

data class IosTtsVoiceInfo(
    val identifier: String,
    /** What the voice picker shows, e.g. "Daniel", or "Satu, Finnish (Finland)". */
    val displayName: String,
    val language: String,
)

/**
 * Voices the legacy Soundscape iOS app suppressed because they sound robotic
 * compared to the modern voices and can't be removed by the user.
 */
private val DISALLOWED_VOICES = setOf(
    "com.apple.speech.synthesis.voice.Fred",
    "com.apple.speech.synthesis.voice.Victoria",
    "com.apple.speech.voice.Alex",
)

/**
 * A voice as iOS describes it, with the locale lookups already done so that
 * [shapeVoiceList] can be tested without AVFoundation.
 */
internal data class RawTtsVoice(
    val identifier: String,
    val name: String,
    /** The voice's own BCP-47 tag, e.g. "fi-FI". */
    val language: String,
    /** [language]'s language code, e.g. "fi". */
    val languageCode: String,
    /** [language] named in the app's own language, e.g. "Finnish (Finland)". */
    val languageDisplayName: String,
    val enhanced: Boolean,
)

/**
 * Returns every installed voice, with the legacy app's filters applied: drop disallowed voices,
 * and when both an enhanced and a default-quality voice exist for the same speaker keep only the
 * enhanced one. The transformation that collides "premium"/"compact" identifier tokens matches the
 * legacy `TTSConfigHelper.loadVoices(...)` logic.
 *
 * Voices in the app's own language come first, then the rest - the two sections the legacy app's
 * voice settings screen had, flattened into one picker. Voices in another language are worth
 * offering because the app's language and the language of the street names around the user are
 * two different things: a Finnish user running the app in English needs a Finnish voice to make
 * sense of Finnish street names. The language is part of their labels, since a name alone doesn't
 * say what a voice speaks.
 */
@OptIn(ExperimentalForeignApi::class)
fun availableTtsVoices(): List<IosTtsVoiceInfo> {
    val appLocale = NSLocale.currentLocale
    val rawVoices = AVSpeechSynthesisVoice.speechVoices()
        .filterIsInstance<AVSpeechSynthesisVoice>()
        .map { voice ->
            RawTtsVoice(
                identifier = voice.identifier,
                name = voice.name,
                language = voice.language,
                languageCode = NSLocale(voice.language).languageCode,
                // Named in the app's language, so an English-speaking user looking for a Finnish
                // voice reads "Finnish (Finland)" rather than "suomi" or "fi-FI". iOS hands back
                // the identifier itself for a language it can't name.
                languageDisplayName = appLocale.localizedStringForLocaleIdentifier(voice.language),
                enhanced = voice.quality == AVSpeechSynthesisVoiceQualityEnhanced,
            )
        }

    return shapeVoiceList(rawVoices, appLocale.languageCode)
}

internal fun shapeVoiceList(
    rawVoices: List<RawTtsVoice>,
    currentLanguageCode: String,
): List<IosTtsVoiceInfo> {
    val allowed = rawVoices.filter { it.identifier !in DISALLOWED_VOICES }

    val enhancedStubs = allowed
        .filter { it.enhanced }
        .map { it.identifier.replace("premium", "") }
        .toSet()

    val condensed = allowed.filter { voice ->
        voice.enhanced || voice.identifier.replace("compact", "") !in enhancedStubs
    }

    val (currentLanguage, otherLanguages) = condensed.partition {
        it.languageCode == currentLanguageCode
    }

    // Within the app's own language the name is enough, and is what the picker has always shown.
    // Everything else is sorted by the language as the user reads it, so the list runs in the
    // alphabetical order they'd expect while hunting for their language.
    return currentLanguage
        .sortedWith(compareBy({ it.language }, { it.name }))
        .map { IosTtsVoiceInfo(it.identifier, it.name, it.language) } +
        otherLanguages
            .sortedWith(compareBy({ it.languageDisplayName }, { it.name }))
            .map {
                IosTtsVoiceInfo(
                    identifier = it.identifier,
                    displayName = "${it.name}, ${it.languageDisplayName}",
                    language = it.language,
                )
            }
}
