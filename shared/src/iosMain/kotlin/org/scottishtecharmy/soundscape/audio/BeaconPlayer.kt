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
 * Pre-loads all WAV variants for the selected beacon type, then loops the
 * appropriate asset based on the user's angular relationship to the beacon.
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
     * Load all WAV assets using each file's processingFormat (mono Float32).
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

    /**
     * Start playing after the layer has been attached and connected externally.
     */
    fun startPlaying() {
        layer.play()
        isPlaying = true
        scheduleCurrentAsset()
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
            currentAssetName = newAssetName
            scheduleCurrentAsset()
        }

        layer.volume = if (muted) 0f else volume
    }

    private fun scheduleCurrentAsset() {
        layer.player.stop()

        val buffer = currentAssetName?.let { buffers[it] } ?: silentBuffer ?: return

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
