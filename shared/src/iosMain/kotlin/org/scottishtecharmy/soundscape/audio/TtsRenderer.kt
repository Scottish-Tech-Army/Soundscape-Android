package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.AVFAudio.AVSpeechUtteranceMaximumSpeechRate
import platform.AVFAudio.AVSpeechUtteranceMinimumSpeechRate
import platform.darwin.NSObject

/**
 * Renders TTS text to AVAudioPCMBuffer chunks using AVSpeechSynthesizer.write().
 * Buffers are passed through in their native format (typically Int16 mono).
 * AVAudioPlayerNode handles format conversion when connected to the audio graph.
 */
@OptIn(ExperimentalForeignApi::class)
class TtsRenderer {

    private val synthesizer = AVSpeechSynthesizer()
    private var language: String? = null
    private var voiceId: String? = null

    // User-facing multiplier from PreferenceKeys.SPEECH_RATE: 1.0 = normal,
    // 2.0 = max, 0.5 = slowest. Translated to AVSpeechUtterance.rate at render time.
    private var rateMultiplier: Float = 1.0f
    private var currentDelegate: TtsDelegate? = null

    fun setLanguage(language: String) {
        this.language = language
    }

    /** Pass null/empty to fall back to the language-derived default voice. */
    fun setVoiceId(voiceId: String?) {
        this.voiceId = voiceId?.takeIf { it.isNotEmpty() }
    }

    fun setRateMultiplier(multiplier: Float) {
        this.rateMultiplier = multiplier
    }

    fun render(text: String, completion: (List<AVAudioPCMBuffer>) -> Unit) {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        val selectedVoice = voiceId?.let { AVSpeechSynthesisVoice.voiceWithIdentifier(it) }
            ?: language?.let { AVSpeechSynthesisVoice.voiceWithLanguage(it) }
        if (selectedVoice != null) {
            utterance.voice = selectedVoice
        }
        utterance.rate = perceivedMultiplierToAvRate(rateMultiplier)
            .coerceIn(AVSpeechUtteranceMinimumSpeechRate, AVSpeechUtteranceMaximumSpeechRate)

        val collectedBuffers = mutableListOf<AVAudioPCMBuffer>()

        val delegate = TtsDelegate {
            completion(collectedBuffers)
        }
        currentDelegate = delegate
        synthesizer.delegate = delegate

        synthesizer.writeUtterance(utterance) { buffer ->
            val pcmBuffer = buffer as? AVAudioPCMBuffer ?: return@writeUtterance
            if (pcmBuffer.frameLength > 0u) {
                collectedBuffers.add(pcmBuffer)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun cancel() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.byValue(0))
    }
}

/**
 * Maps the user-facing speed multiplier (1.0 = normal, 2.0 = "twice as fast",
 * 0.5 = "half speed") to AVSpeechUtterance.rate, which is highly non-linear
 * in perceived speed above the default. Empirically default rate (0.5) ≈ 175
 * wpm and max rate (1.0) ≈ 575 wpm, so a linear "multiplier × default" mapping
 * makes the slider's top end roughly 3× faster than its label claims. This
 * piecewise mapping compresses the upper half to track perceived speed:
 * multiplier 1.0 → 0.5, 1.5 → 0.6, 2.0 → 0.7. Below the default the slider
 * already reads about right, so we keep proportional scaling.
 */
private fun perceivedMultiplierToAvRate(multiplier: Float): Float {
    val default = AVSpeechUtteranceDefaultSpeechRate
    return if (multiplier >= 1.0f) {
        default + 0.2f * (multiplier - 1.0f)
    } else {
        default * multiplier
    }
}

private class TtsDelegate(
    private val onFinish: () -> Unit
) : NSObject(), AVSpeechSynthesizerDelegateProtocol {

    override fun speechSynthesizer(
        synthesizer: AVSpeechSynthesizer,
        didFinishSpeechUtterance: AVSpeechUtterance
    ) {
        onFinish()
    }
}
