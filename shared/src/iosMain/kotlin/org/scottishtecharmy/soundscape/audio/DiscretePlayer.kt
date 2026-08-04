package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioFile
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
    private var earconBuffer: AVAudioPCMBuffer? = null

    /**
     * Load a WAV file from the app bundle into a buffer.
     * Sets layer.format but does NOT attach/connect/play.
     * Call scheduleEarconBuffer() after connecting the layer.
     */
    fun loadEarcon(assetName: String): Boolean {
        val path = NSBundle.mainBundle.pathForResource(assetName, "wav", "Sounds")
            ?: run {
                println("DiscretePlayer: WAV not found: $assetName")
                return false
            }

        // AVAudioFile/AVAudioPCMBuffer's failable ObjC initializers are bridged to Kotlin
        // constructors, which can't return null on failure like the ObjC init can — Kotlin/Native
        // throws instead. Catch that so a corrupt/unreadable bundled WAV degrades gracefully
        // (matching the false-return contract below) instead of crashing the app.
        try {
            val url = NSURL.fileURLWithPath(path)
            val audioFile = AVAudioFile(forReading = url, error = null)

            val processingFormat = audioFile.processingFormat
            val frameCount = audioFile.length().toUInt()
            val buffer = AVAudioPCMBuffer(pCMFormat = processingFormat, frameCapacity = frameCount)

            audioFile.readIntoBuffer(buffer, error = null)

            layer.format = processingFormat
            earconBuffer = buffer
            return true
        } catch (e: Exception) {
            println("DiscretePlayer: Failed to load audio file: $assetName (${e.message})")
            return false
        }
    }

    /**
     * Schedule the loaded earcon buffer for playback.
     * The layer must be attached, connected, and playing before calling this.
     */
    fun scheduleEarconBuffer() {
        val buffer = earconBuffer ?: run {
            notifyComplete()
            return
        }
        layer.player.scheduleBuffer(buffer, completionHandler = {
            notifyComplete()
        })
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
