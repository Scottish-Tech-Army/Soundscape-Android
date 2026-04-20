package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioNode
import platform.AVFAudio.AVAudioPCMBuffer
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

/**
 * Continuous looped audio player for beacons.
 * Port of DynamicAudioPlayer.swift (simplified — no beat-aligned switching initially).
 *
 * Pre-loads all WAV variants for the selected beacon type, then loops the
 * appropriate asset based on the user's angular relationship to the beacon.
 * The asset is re-selected on each geometry update.
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

    /**
     * Load all WAV assets for this beacon type from the app bundle.
     * Returns false if any required asset is missing.
     */
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

        // Create a silent buffer matching the first asset's format and length
        val firstBuffer = buffers.values.firstOrNull() ?: return false
        silentBuffer = AVAudioPCMBuffer(
            pCMFormat = firstBuffer.format,
            frameCapacity = firstBuffer.frameLength
        )?.also {
            it.frameLength = firstBuffer.frameLength
            // Buffer is zero-initialized = silence
        }

        layer.format = firstBuffer.format
        return true
    }

    /**
     * Start playing the beacon through the audio engine.
     */
    fun start(engine: AVAudioEngine, targetNode: AVAudioNode) {
        layer.attach(engine)
        layer.connect(targetNode, layer.format)
        layer.play()
        isPlaying = true

        // Schedule initial buffer (silence until first geometry update picks the right asset)
        scheduleCurrentAsset()
    }

    /**
     * Update the beacon based on the listener's current position and heading.
     * Re-selects the appropriate asset based on angular relationship.
     */
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
            null // silence
        }

        val volume = selection?.volume ?: 0f

        if (newAssetName != currentAssetName) {
            currentAssetName = newAssetName
            scheduleCurrentAsset()
        }

        // Apply volume (respecting mute state)
        layer.volume = if (muted) 0f else volume
    }

    /**
     * Schedule the current asset buffer as a loop.
     */
    private fun scheduleCurrentAsset() {
        // Stop current playback to switch buffers
        layer.player.stop()

        val buffer = currentAssetName?.let { buffers[it] } ?: silentBuffer ?: return

        // Schedule as looping
        layer.player.scheduleBuffer(
            buffer,
            atTime = null,
            options = platform.AVFAudio.AVAudioPlayerNodeBufferLoops,
            completionHandler = null
        )
        layer.play()
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
