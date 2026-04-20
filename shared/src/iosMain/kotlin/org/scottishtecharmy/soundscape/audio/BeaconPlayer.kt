package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNodeBufferInterrupts
import platform.AVFAudio.AVAudioPlayerNodeBufferLoops
import platform.AVFAudio.AVAudioTime
import platform.Foundation.NSBundle
import platform.AVFAudio.AVAudioFile
import platform.Foundation.NSURL

/**
 * Continuous looped audio player for beacons with beat-aligned asset switching.
 * Port of DynamicAudioPlayer.swift from the original iOS Soundscape app.
 */
@OptIn(ExperimentalForeignApi::class)
class BeaconPlayer(
    private val beaconType: BeaconType,
    val beaconLatitude: Double,
    val beaconLongitude: Double,
) {
    val layer = AudioLayer()
    private val buffers = mutableMapOf<String, AVAudioPCMBuffer>()
    private var silentBuffer: AVAudioPCMBuffer? = null
    private var currentAssetName: String? = null
    private var isPlaying = false
    private var muted = false

    fun loadAssets(): Boolean {
        for (assetName in beaconType.assets) {
            val path = NSBundle.mainBundle.pathForResource(assetName, "wav")
                ?: run {
                    println("BeaconPlayer: WAV not found: $assetName")
                    return false
                }

            val url = NSURL.fileURLWithPath(path)
            val audioFile = AVAudioFile(forReading = url, error = null) ?: return false

            val format = audioFile.processingFormat
            val frameCount = audioFile.length().toUInt()
            val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = frameCount)
                ?: return false
            audioFile.readIntoBuffer(buffer, error = null)
            buffer.frameLength = frameCount
            buffers[assetName] = buffer
        }

        val firstBuffer = buffers.values.firstOrNull() ?: return false
        silentBuffer = AVAudioPCMBuffer(
            pCMFormat = firstBuffer.format,
            frameCapacity = firstBuffer.frameLength
        )?.also {
            it.frameLength = firstBuffer.frameLength
        }

        layer.format = firstBuffer.format
        return true
    }

    fun startPlaying() {
        layer.play()
        isPlaying = true
        scheduleAsset(currentAssetName)
    }

    fun updateForGeometry(
        listenerLatitude: Double,
        listenerLongitude: Double,
        listenerHeading: Double?
    ) {
        if (!isPlaying) return

        val poiBearing = bearing(
            listenerLatitude, listenerLongitude,
            beaconLatitude, beaconLongitude
        )

        val selection = beaconType.selector(listenerHeading, poiBearing)
        val newAssetName = if (selection != null) {
            beaconType.assets.getOrNull(selection.assetIndex)
        } else {
            null
        }

        val volume = selection?.volume ?: 0f

        if (newAssetName != currentAssetName) {
            scheduleAsset(newAssetName)
        }

        layer.volume = if (muted) 0f else volume
    }

    /**
     * Schedule a new asset buffer aligned to the current beat position.
     * Matches DynamicAudioPlayer.scheduleAsset() from the original iOS app.
     */
    private fun scheduleAsset(newAsset: String?) {
        val previousAsset = currentAssetName
        currentAssetName = newAsset

        val buffer = newAsset?.let { buffers[it] } ?: silentBuffer ?: return

        // Try to get the current playback position for beat alignment
        val lastRendered = layer.player.lastRenderTime
        val playerTime = if (lastRendered != null &&
            (lastRendered.isSampleTimeValid() || lastRendered.isHostTimeValid())
        ) {
            layer.player.playerTimeForNodeTime(lastRendered)
        } else null

        if (playerTime == null || beaconType.beatsInPhrase <= 0) {
            // No timing info — just schedule with interrupts + loops
            layer.player.scheduleBuffer(
                buffer,
                atTime = null,
                options = AVAudioPlayerNodeBufferInterrupts or AVAudioPlayerNodeBufferLoops,
                completionHandler = null
            )
            if (!layer.isPlaying) layer.play()
            return
        }

        // Calculate beat-aligned start time
        val currentBuffer = previousAsset?.let { buffers[it] } ?: silentBuffer ?: buffer
        val samplesPerBeat = currentBuffer.frameLength.toLong() / beaconType.beatsInPhrase.toLong()
        if (samplesPerBeat <= 0) {
            layer.player.scheduleBuffer(
                buffer,
                atTime = null,
                options = AVAudioPlayerNodeBufferInterrupts or AVAudioPlayerNodeBufferLoops,
                completionHandler = null
            )
            return
        }

        val sampleTime = playerTime.sampleTime
        val beatsPlayed = sampleTime / samplesPerBeat
        val startTime = (beatsPlayed + 1) * samplesPerBeat

        // Calculate which beat we'll be at in the new asset's phrase
        val beatInPhrase = beatsPlayed % beaconType.beatsInPhrase.toLong()
        val suffixStartFrame = ((beatInPhrase + 1) * samplesPerBeat).toInt()

        // Try to create a partial buffer starting at the right beat position
        val partialBuffer = bufferSuffix(buffer, fromFrame = suffixStartFrame)

        val startAudioTime = AVAudioTime(
            sampleTime = startTime,
            atRate = playerTime.sampleRate
        )

        if (partialBuffer != null) {
            // Schedule: partial (to sync the beat) → full loop
            layer.player.scheduleBuffer(
                partialBuffer,
                atTime = startAudioTime,
                options = AVAudioPlayerNodeBufferInterrupts,
                completionHandler = null
            )
            val fullStartTime = AVAudioTime(
                sampleTime = startTime + partialBuffer.frameLength.toLong(),
                atRate = playerTime.sampleRate
            )
            layer.player.scheduleBuffer(
                buffer,
                atTime = fullStartTime,
                options = AVAudioPlayerNodeBufferLoops,
                completionHandler = null
            )
        } else {
            // Can't create partial — just schedule at next beat with loop
            layer.player.scheduleBuffer(
                buffer,
                atTime = startAudioTime,
                options = AVAudioPlayerNodeBufferInterrupts or AVAudioPlayerNodeBufferLoops,
                completionHandler = null
            )
        }
    }

    /**
     * Create a new buffer containing frames from [fromFrame] to the end of the source buffer.
     * Port of AVAudioPCMBuffer.suffix(from:) from the original iOS app.
     */
    private fun bufferSuffix(source: AVAudioPCMBuffer, fromFrame: Int): AVAudioPCMBuffer? {
        val totalFrames = source.frameLength.toInt()
        if (fromFrame >= totalFrames || fromFrame < 0) return null

        val suffixLength = totalFrames - fromFrame
        val result = AVAudioPCMBuffer(
            pCMFormat = source.format,
            frameCapacity = suffixLength.toUInt()
        ) ?: return null

        // Copy float channel data (mono)
        val srcChannels = source.floatChannelData ?: return null
        val dstChannels = result.floatChannelData ?: return null

        val channelCount = source.format.channelCount.toInt()
        for (ch in 0 until channelCount) {
            val srcPtr = srcChannels[ch] ?: return null
            val dstPtr = dstChannels[ch] ?: return null
            for (i in 0 until suffixLength) {
                dstPtr[i] = srcPtr[fromFrame + i]
            }
        }

        result.frameLength = suffixLength.toUInt()
        return result
    }

    fun setMuted(muted: Boolean) {
        this.muted = muted
        layer.volume = if (muted) 0f else 1f
    }

    fun stop() {
        isPlaying = false
        layer.stop()
        layer.disconnect()
        layer.detach()
    }
}
