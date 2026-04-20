package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import platform.AVFAudio.AVAudio3DAngularOrientation
import platform.AVFAudio.AVAudioChannelLayout
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEnvironmentNode
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioChannelLayoutTag_Stereo

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
        class Beacon(val player: BeaconPlayer) : PlayerEntry()
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

        // Access mainMixerNode to ensure the output chain is connected
        // (AVAudioEngine requires at least one node before starting)
        @Suppress("UNUSED_VARIABLE")
        val mixer = engine.mainMixerNode

        // Start the engine
        try {
            engine.startAndReturnError(null)
            engineStarted = true
        } catch (e: Exception) {
            println("IosAudioEngine: Failed to start engine: $e")
        }
    }

    /**
     * Matches AudioEngine.outputFormat() from the original iOS app:
     * Creates a stereo format with a channel layout at the given sample rate.
     */
    private fun outputFormat(sampleRate: Double): AVAudioFormat? {
        val layout = AVAudioChannelLayout(layoutTag = kAudioChannelLayoutTag_Stereo) ?: return null
        return AVAudioFormat(standardFormatWithSampleRate = sampleRate, channelLayout = layout)
    }

    /**
     * Connect a layer to the 3D audio environment, matching the original iOS app's
     * connectLayer(_:for:) method. Handles:
     * - Finding/creating the environment node
     * - Disconnecting the layer first
     * - Connecting layer → environment (with source format)
     * - Connecting environment → mainMixer (with stereo output format)
     */
    fun connectLayerToEnvironment(layer: AudioLayer) {
        val format = layer.format
        val envNode = getOrCreateEnvironmentNode(format)

        // Disconnect existing connections (matching original)
        layer.disconnect()

        // Connect layer → environment (uses the file's processingFormat)
        layer.connect(envNode, format)

        // Connect environment → mainMixer with stereo layout format at source sample rate
        val sampleRate = format?.sampleRate ?: engine.outputNode.outputFormatForBus(0u).sampleRate
        val envOutputFmt = outputFormat(sampleRate)
        engine.connect(envNode, to = engine.mainMixerNode, format = envOutputFmt)
    }

    private fun getOrCreateEnvironmentNode(format: AVAudioFormat?): AVAudioEnvironmentNode {
        // Reuse existing environment node
        for (node in environmentNodes) {
            return node
        }

        // Create new environment node
        val envNode = AVAudioEnvironmentNode()
        engine.attachNode(envNode)
        envNode.distanceAttenuationParameters.referenceDistance = DEFAULT_RENDERING_DISTANCE.toFloat()
        envNode.outputVolume = 1.0f

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

        val player = DiscretePlayer(onComplete = {
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                onDiscreteComplete(sound.handle)
            }
        })

        activePlayers[sound.handle] = PlayerEntry.Discrete(player)

        if (sound.isTts) {
            // Render TTS to PCM buffers, then connect and play through the audio graph
            ttsRenderer.render(sound.text) { buffers ->
                platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                    if (buffers.isNotEmpty()) {
                        // Attach the layer
                        player.layer.format = buffers.first().format
                        player.layer.attach(engine)

                        if (is3D) {
                            connectLayerToEnvironment(player.layer)
                        } else {
                            player.layer.connect(engine.mainMixerNode)
                        }

                        val position = positionForType(sound.audioType, sound.latitude, sound.longitude, sound.heading)
                        if (position != null) player.layer.position = position

                        player.layer.play()
                        player.scheduleTtsBuffers(buffers)
                    } else {
                        onDiscreteComplete(sound.handle)
                    }
                }
            }
        } else {
            // For earcons: load, attach, connect, play
            val targetNode = if (is3D) getOrCreateEnvironmentNode(null) else engine.mainMixerNode
            player.playEarcon(sound.assetName, engine, targetNode)

            val position = positionForType(sound.audioType, sound.latitude, sound.longitude, sound.heading)
            if (position != null) player.layer.position = position
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
        // Cancel any in-progress TTS rendering
        ttsRenderer.cancel()

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
    }

    override fun getQueueDepth(): Long {
        val current = if (currentDiscreteHandle != null) 1L else 0L
        return current + discreteQueue.size.toLong()
    }

    override fun isHandleActive(handle: Long): Boolean {
        return activePlayers.containsKey(handle)
    }

    // --- AudioEngine Interface: Beacons ---

    override fun createBeacon(location: LngLatAlt, headingOnly: Boolean): Long {
        ensureEngineStarted()
        val handle = nextHandle++

        val type = BEACON_TYPES[currentBeaconType] ?: BEACON_TYPES["Current"]!!
        val player = BeaconPlayer(type, location.latitude, location.longitude)

        if (!player.loadAssets()) {
            println("IosAudioEngine: Failed to load beacon assets for $currentBeaconType")
            return handle
        }

        player.layer.attach(engine)
        connectLayerToEnvironment(player.layer)
        player.startPlaying()
        player.setMuted(beaconMuted)

        // Apply initial geometry
        player.updateForGeometry(listenerLatitude, listenerLongitude, listenerHeading)

        activePlayers[handle] = PlayerEntry.Beacon(player)
        return handle
    }

    override fun destroyBeacon(beaconHandle: Long) {
        val entry = activePlayers.remove(beaconHandle)
        if (entry is PlayerEntry.Beacon) {
            entry.player.stop()
        }
    }

    override fun toggleBeaconMute(): Boolean {
        beaconMuted = !beaconMuted
        for ((_, entry) in activePlayers) {
            if (entry is PlayerEntry.Beacon) {
                entry.player.setMuted(beaconMuted)
            }
        }
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

        // Update all active beacon players with new geometry
        for ((_, entry) in activePlayers) {
            if (entry is PlayerEntry.Beacon) {
                entry.player.updateForGeometry(
                    listenerLatitude, listenerLongitude, this.listenerHeading
                )
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
        val beaconHandles = activePlayers.entries
            .filter { it.value is PlayerEntry.Beacon }
            .map { it.key }
        for (handle in beaconHandles) {
            destroyBeacon(handle)
        }
    }

    override fun setHrtfEnabled(enabled: Boolean) {
        // AVAudioEnvironmentNode uses HRTF by default
        // Could toggle rendering algorithm here if needed
    }
}
