package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesisVoiceQualityEnhanced
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

data class IosTtsVoiceInfo(
    val identifier: String,
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
 * Returns the AVSpeechSynthesisVoice list for the current app language with the
 * legacy app's filters applied: drop disallowed voices, and when both an
 * enhanced and a default-quality voice exist for the same speaker keep only the
 * enhanced one. The transformation that collides "premium"/"compact" identifier
 * tokens matches the legacy `TTSConfigHelper.loadVoices(...)` logic.
 */
@OptIn(ExperimentalForeignApi::class)
fun availableTtsVoicesForCurrentLanguage(): List<IosTtsVoiceInfo> {
    val currentLanguageCode = NSLocale.currentLocale.languageCode

    val rawVoices = AVSpeechSynthesisVoice.speechVoices()
        .filterIsInstance<AVSpeechSynthesisVoice>()
        .filter { it.identifier !in DISALLOWED_VOICES }

    val enhancedStubs = rawVoices
        .filter { it.quality == AVSpeechSynthesisVoiceQualityEnhanced }
        .map { it.identifier.replace("premium", "") }
        .toSet()

    val condensed = rawVoices.filter { voice ->
        if (voice.quality == AVSpeechSynthesisVoiceQualityEnhanced) {
            true
        } else {
            voice.identifier.replace("compact", "") !in enhancedStubs
        }
    }

    return condensed
        .filter { voice -> NSLocale(voice.language).languageCode == currentLanguageCode }
        .sortedWith(compareBy({ it.language }, { it.name }))
        .map { IosTtsVoiceInfo(it.identifier, it.name, it.language) }
}
