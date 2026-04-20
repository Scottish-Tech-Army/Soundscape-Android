package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject

/**
 * Renders TTS text to AVAudioPCMBuffer chunks using AVSpeechSynthesizer.write().
 * This routes audio through the AVAudioEngine graph (with spatial positioning)
 * instead of playing directly through the speaker.
 */
@OptIn(ExperimentalForeignApi::class)
class TtsRenderer {

    private val synthesizer = AVSpeechSynthesizer()
    private var language: String? = null

    fun setLanguage(language: String) {
        this.language = language
    }

    /**
     * Renders the given text to PCM buffers and calls completion with the result.
     * The completion is called on an arbitrary thread.
     */
    fun render(text: String, completion: (List<AVAudioPCMBuffer>) -> Unit) {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)

        // Set voice based on language
        language?.let { lang ->
            AVSpeechSynthesisVoice.voiceWithLanguage(lang)?.let { voice ->
                utterance.voice = voice
            }
        }

        val buffers = mutableListOf<AVAudioPCMBuffer>()
        val delegate = TtsDelegate {
            completion(buffers)
        }
        synthesizer.delegate = delegate

        synthesizer.writeUtterance(utterance) { buffer ->
            val pcmBuffer = buffer as? AVAudioPCMBuffer
            if (pcmBuffer != null && pcmBuffer.frameLength > 0u) {
                buffers.add(pcmBuffer)
            }
        }
    }

    fun cancel() {
        @Suppress("CAST_NEVER_SUCCEEDS")
        synthesizer.stopSpeakingAtBoundary(0L as platform.AVFAudio.AVSpeechBoundary) // AVSpeechBoundaryImmediate = 0
    }

    /**
     * Simple convenience: render TTS and play directly through the synthesizer
     * (bypasses spatial audio graph — used as fallback).
     */
    fun speakDirect(text: String) {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        language?.let { lang ->
            AVSpeechSynthesisVoice.voiceWithLanguage(lang)?.let { voice ->
                utterance.voice = voice
            }
        }
        synthesizer.speakUtterance(utterance)
    }

    fun stopDirect() {
        @Suppress("CAST_NEVER_SUCCEEDS")
        synthesizer.stopSpeakingAtBoundary(0L as platform.AVFAudio.AVSpeechBoundary)
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
