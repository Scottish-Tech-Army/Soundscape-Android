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
 * Buffers are passed through in their native format (typically Int16 mono).
 * AVAudioPlayerNode handles format conversion when connected to the audio graph.
 */
@OptIn(ExperimentalForeignApi::class)
class TtsRenderer {

    private val synthesizer = AVSpeechSynthesizer()
    private var language: String? = null
    private var currentDelegate: TtsDelegate? = null

    fun setLanguage(language: String) {
        this.language = language
    }

    fun render(text: String, completion: (List<AVAudioPCMBuffer>) -> Unit) {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        language?.let { lang ->
            AVSpeechSynthesisVoice.voiceWithLanguage(lang)?.let { voice ->
                utterance.voice = voice
            }
        }

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

    fun cancel() {
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
