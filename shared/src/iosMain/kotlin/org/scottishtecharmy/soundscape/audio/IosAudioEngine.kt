package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import platform.AVFAudio.AVAudio3DAngularOrientation
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEnvironmentNode
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive

/**
 * iOS audio engine implementing the KMP AudioEngine interface.
 * Uses Apple's AVAudioEngine with AVAudioEnvironmentNode for HRTF spatial audio.
 * Ported from the original Soundscape iOS Swift implementation.
 */
@OptIn(ExperimentalForeignApi::class)
class IosAudioEngine : AudioEngine {

    private val engine = AVAudioEngine()
    private var engineStarted = false

    // Environment nodes for 3D audio (one per sample rate)
    private val environmentNodes = mutableListOf<AVAudioEnvironmentNode>()

    // Handle tracking
    private var nextHandle = 1L
    private val activePlayers = mutableMapOf<Long, PlayerEntry>()

    // Discrete sound queue
    private val discreteQueue = ArrayDeque<QueuedSound>()
    private var currentDiscreteHandle: Long? = null

    // Listener state
    private var listenerLatitude = 0.0
    private var listenerLongitude = 0.0
    private var listenerHeading: Double? = null

    // TTS
    private val ttsRenderer = TtsRenderer()

    // Beacon state
    private var currentBeaconType = "Current"
    private var beaconMuted = false

    private sealed class PlayerEntry {
        class Discrete(val player: DiscretePlayer) : PlayerEntry()
        // Beacon entries added in Phase 2
    }

    private data class QueuedSound(
        val handle: Long,
        val isTts: Boolean,
        val text: String = "",
        val assetName: String = "",
        val audioType: AudioType = AudioType.STANDARD,
        val latitude: Double = Double.NaN,
        val longitude: Double = Double.NaN,
        val heading: Double = Double.NaN,
    )

    // --- Engine Lifecycle ---

    private fun ensureEngineStarted() {
        if (engineStarted) return

        // Configure audio session
        val session = AVAudioSession.sharedInstance()
        try {
            session.setCategory(
                AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionMixWithOthers,
                error = null
            )
            session.setActive(true, error = null)
        } catch (e: Exception) {
            println("IosAudioEngine: Failed to configure audio session: $e")
        }

        // Start the engine
        try {
            engine.startAndReturnError(null)
            engineStarted = true
        } catch (e: Exception) {
            println("IosAudioEngine: Failed to start engine: $e")
        }
    }

    private fun getOrCreateEnvironmentNode(format: AVAudioFormat?): AVAudioEnvironmentNode {
        val sampleRate = format?.sampleRate ?: 48000.0

        // Look for existing node with matching sample rate
        for (node in environmentNodes) {
            val outputFormat = engine.outputNode.inputFormatForBus(0u)
            // Reuse any existing environment node for simplicity
            return node
        }

        // Create new environment node
        val envNode = AVAudioEnvironmentNode()
        engine.attachNode(envNode)
        engine.connect(envNode, to = engine.mainMixerNode, format = null)

        // Configure distance attenuation
        envNode.distanceAttenuationParameters.referenceDistance = DEFAULT_RENDERING_DISTANCE.toFloat()

        // Apply current listener heading
        listenerHeading?.let { heading ->
            envNode.listenerAngularOrientation = cValue<AVAudio3DAngularOrientation> {
                yaw = heading.toFloat()
                pitch = 0f
                roll = 0f
            }
        }

        environmentNodes.add(envNode)
        return envNode
    }

    private fun positionForType(
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double
    ): kotlinx.cinterop.CValue<platform.AVFAudio.AVAudio3DPoint>? {
        return when (type) {
            AudioType.STANDARD -> null // 2D, no positioning
            AudioType.LOCALIZED -> {
                if (latitude.isNaN() || longitude.isNaN()) return null
                val b = bearing(listenerLatitude, listenerLongitude, latitude, longitude)
                bearingToPoint(b)
            }
            AudioType.RELATIVE -> {
                if (heading.isNaN()) return null
                val absHeading = (listenerHeading ?: 0.0) + heading
                bearingToPoint(absHeading)
            }
            AudioType.COMPASS -> {
                if (heading.isNaN()) return null
                bearingToPoint(heading)
            }
        }
    }

    // --- AudioEngine Interface: TTS ---

    override fun createTextToSpeech(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double
    ): Long {
        val handle = nextHandle++
        val queuedSound = QueuedSound(
            handle = handle,
            isTts = true,
            text = text,
            audioType = type,
            latitude = latitude,
            longitude = longitude,
            heading = heading,
        )

        if (currentDiscreteHandle == null) {
            playQueued(queuedSound)
        } else {
            discreteQueue.addLast(queuedSound)
        }
        return handle
    }

    override fun createEarcon(
        asset: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double
    ): Long {
        val handle = nextHandle++

        // Extract asset name from Android-style path
        val assetName = asset
            .removePrefix("file:///android_asset/earcons/")
            .removeSuffix(".wav")

        val queuedSound = QueuedSound(
            handle = handle,
            isTts = false,
            assetName = assetName,
            audioType = type,
            latitude = latitude,
            longitude = longitude,
            heading = heading,
        )

        if (currentDiscreteHandle == null) {
            playQueued(queuedSound)
        } else {
            discreteQueue.addLast(queuedSound)
        }
        return handle
    }

    private fun playQueued(sound: QueuedSound) {
        ensureEngineStarted()
        currentDiscreteHandle = sound.handle

        val is3D = sound.audioType != AudioType.STANDARD
        val targetNode = if (is3D) {
            getOrCreateEnvironmentNode(null)
        } else {
            engine.mainMixerNode
        }

        val player = DiscretePlayer(onComplete = {
            onDiscreteComplete(sound.handle)
        })

        // Set 3D position if needed
        val position = positionForType(sound.audioType, sound.latitude, sound.longitude, sound.heading)
        if (position != null) {
            player.layer.position = position
        }

        activePlayers[sound.handle] = PlayerEntry.Discrete(player)

        if (sound.isTts) {
            // For TTS, use direct speak as fallback (buffer rendering can be added later
            // once we verify AVSpeechSynthesizer.write() works in Kotlin/Native)
            ttsRenderer.speakDirect(sound.text)
            // Mark as complete after a delay (direct speak doesn't give us buffer completion)
            // This is a temporary approach - Phase 1 gets TTS working, spatial TTS comes later
            activePlayers.remove(sound.handle)
            currentDiscreteHandle = null
            playNextQueued()
        } else {
            player.playEarcon(sound.assetName, engine, targetNode)
        }
    }

    private fun onDiscreteComplete(handle: Long) {
        val entry = activePlayers.remove(handle)
        if (entry is PlayerEntry.Discrete) {
            entry.player.layer.disconnect()
            entry.player.layer.detach()
        }
        if (currentDiscreteHandle == handle) {
            currentDiscreteHandle = null
            playNextQueued()
        }
    }

    private fun playNextQueued() {
        val next = discreteQueue.removeFirstOrNull() ?: return
        playQueued(next)
    }

    override fun clearTextToSpeechQueue() {
        // Remove TTS entries from queue
        discreteQueue.removeAll { it.isTts }

        // Stop current if it's TTS
        val currentHandle = currentDiscreteHandle
        if (currentHandle != null) {
            val entry = activePlayers[currentHandle]
            if (entry is PlayerEntry.Discrete) {
                entry.player.stop()
            }
        }
        ttsRenderer.stopDirect()
    }

    override fun getQueueDepth(): Long {
        val current = if (currentDiscreteHandle != null) 1L else 0L
        return current + discreteQueue.size.toLong()
    }

    override fun isHandleActive(handle: Long): Boolean {
        return activePlayers.containsKey(handle)
    }

    // --- AudioEngine Interface: Beacons (Phase 2 stubs) ---

    override fun createBeacon(location: LngLatAlt, headingOnly: Boolean): Long {
        // TODO: Phase 2 - implement beacon playback
        return nextHandle++
    }

    override fun destroyBeacon(beaconHandle: Long) {
        // TODO: Phase 2
    }

    override fun toggleBeaconMute(): Boolean {
        beaconMuted = !beaconMuted
        return beaconMuted
    }

    // --- AudioEngine Interface: Geometry ---

    override fun updateGeometry(
        listenerLatitude: Double,
        listenerLongitude: Double,
        listenerHeading: Double?,
        focusGained: Boolean,
        duckingAllowed: Boolean,
        proximityNear: Double
    ) {
        this.listenerLatitude = listenerLatitude
        this.listenerLongitude = listenerLongitude
        this.listenerHeading = listenerHeading

        // Update listener orientation on all environment nodes
        if (listenerHeading != null) {
            val orientation = cValue<AVAudio3DAngularOrientation> {
                yaw = listenerHeading.toFloat()
                pitch = 0f
                roll = 0f
            }
            for (envNode in environmentNodes) {
                envNode.listenerAngularOrientation = orientation
            }
        }
    }

    // --- AudioEngine Interface: Configuration ---

    override fun setBeaconType(beaconType: String) {
        currentBeaconType = beaconType
    }

    override fun getListOfBeaconTypes(): Array<String> {
        return arrayOf(
            "Original", "Current", "Tactile", "Flare", "Shimmer",
            "Ping", "Drop", "Signal", "Signal Slow", "Signal Very Slow",
            "Mallet", "Mallet Slow", "Mallet Very Slow"
        )
    }

    override fun setSpeechLanguage(language: String): Boolean {
        ttsRenderer.setLanguage(language)
        return true
    }

    override fun onAllBeaconsCleared() {
        // TODO: Phase 2
    }

    override fun setHrtfEnabled(enabled: Boolean) {
        // AVAudioEnvironmentNode uses HRTF by default
        // Could toggle rendering algorithm here if needed
    }
}
