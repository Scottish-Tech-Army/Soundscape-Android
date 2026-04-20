package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudio3DPoint
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioNode
import platform.AVFAudio.AVAudioPCMBuffer
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

/**
 * One-shot audio player for TTS utterances and earcon sound effects.
 * Port of DiscreteAudioPlayer.swift (simplified).
 */
@OptIn(ExperimentalForeignApi::class)
class DiscretePlayer(
    private val onComplete: () -> Unit
) {
    val layer = AudioLayer()
    private var completed = false

    /**
     * Load and play a WAV file from the app bundle.
     */
    fun playEarcon(
        assetName: String,
        engine: AVAudioEngine,
        targetNode: AVAudioNode
    ): Boolean {
        val path = NSBundle.mainBundle.pathForResource(assetName, "wav")
            ?: run {
                println("DiscretePlayer: WAV not found: $assetName")
                notifyComplete()
                return false
            }

        val url = NSURL.fileURLWithPath(path)
        val audioFile = AVAudioFile(forReading = url, error = null)
            ?: run {
                println("DiscretePlayer: Failed to load audio file: $assetName")
                notifyComplete()
                return false
            }

        val processingFormat = audioFile.processingFormat
        val frameCount = audioFile.length().toUInt()
        val buffer = AVAudioPCMBuffer(pCMFormat = processingFormat, frameCapacity = frameCount)
            ?: run {
                notifyComplete()
                return false
            }

        audioFile.readIntoBuffer(buffer, error = null)

        layer.format = processingFormat
        layer.attach(engine)
        layer.connect(targetNode, processingFormat)
        layer.play()

        layer.player.scheduleBuffer(buffer, completionHandler = {
            notifyComplete()
        })

        return true
    }

    /**
     * Schedule pre-rendered TTS buffers on an already-connected layer.
     * The layer must be attached, connected, and playing before calling this.
     */
    fun scheduleTtsBuffers(buffers: List<AVAudioPCMBuffer>) {
        if (buffers.isEmpty()) {
            notifyComplete()
            return
        }
        for (i in buffers.indices) {
            val isLast = (i == buffers.lastIndex)
            if (isLast) {
                layer.player.scheduleBuffer(buffers[i], completionHandler = {
                    notifyComplete()
                })
            } else {
                layer.player.scheduleBuffer(buffers[i], completionHandler = null)
            }
        }
    }

    fun stop() {
        layer.stop()
        layer.disconnect()
        layer.detach()
        notifyComplete()
    }

    private fun notifyComplete() {
        if (!completed) {
            completed = true
            onComplete()
        }
    }
}
