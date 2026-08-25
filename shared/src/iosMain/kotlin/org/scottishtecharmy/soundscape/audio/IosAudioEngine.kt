package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.coroutines.CoroutineDispatcher
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControlTarget
import platform.AVFAudio.AVAudio3DAngularOrientation
import platform.AVFAudio.AVAudioChannelLayout
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEnvironmentNode
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioChannelLayoutTag_Stereo
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.Foundation.NSLock
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyMediaType
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

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

    // activePlayers is touched from multiple threads: from coroutine dispatchers
    // (updateGeometry, createBeacon, speakCallout, ...) and from [audioQueue]
    // completion callbacks. mutableMapOf is not thread-safe, so guard every
    // access with this lock.
    private val activePlayersLock = NSLock()

    // Serial queue that owns the discrete-sound pipeline (TTS render callback,
    // DiscretePlayer completion, playQueued → attach/connect/play). Kept off
    // the main queue so first-tap keyboard init (or any other main-thread stall)
    // can't gap out callout audio between utterances.
    private val audioQueue: dispatch_queue_t =
        dispatch_queue_create("org.scottishtecharmy.soundscape.audio", null)!!

    /**
     * Serial coroutine dispatcher backed by [audioQueue]. Publish this so
     * [org.scottishtecharmy.soundscape.IosSoundscapeService.startCallout] can
     * launch on the same queue: its `clearTextToSpeechQueue` + `createTextToSpeech`
     * calls then serialize naturally with the render/completion callbacks in
     * [playQueued] / [onDiscreteComplete], without ever hopping to main.
     */
    val audioDispatcher: CoroutineDispatcher = DispatchQueueDispatcher(audioQueue)

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

    // Media control target for remote commands
    var mediaControlTarget: MediaControlTarget? = null

    // Audio session configuration
    var mixWithOthers: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                if (engineStarted) {
                    reconfigureAudioSession()
                }
            }
        }

    // Audio session state
    private var needsReactivation = false
    private var interruptionObserver: Any? = null
    private var mediaResetObserver: Any? = null

    private sealed class PlayerEntry {
        class Discrete(val player: DiscretePlayer, val isTts: Boolean) : PlayerEntry()
        class Beacon(val player: BeaconPlayer) : PlayerEntry()
    }

    private inline fun <T> withActivePlayersLock(block: () -> T): T {
        activePlayersLock.lock()
        try {
            return block()
        } finally {
            activePlayersLock.unlock()
        }
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

        // Configure and activate audio session
        configureAudioSession()

        // Register for audio session notifications
        registerAudioSessionObservers()

        // Set up lock screen remote commands (always registered, but only
        // effective when not mixing — iOS ignores them otherwise)
        registerRemoteCommands()

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
     * Idempotent AVAudioSession setup. Called internally when the audio engine
     * first needs to start playing, and from Swift (SplashCoordinator) so the
     * splash's AVAudioPlayer shares the same session category/options rather
     * than racing us for `setCategory`/`setActive` on the shared session.
     */
    fun configureAudioSession() {
        val session = AVAudioSession.sharedInstance()
        val options = if (mixWithOthers) AVAudioSessionCategoryOptionMixWithOthers else 0u
        try {
            session.setCategory(
                AVAudioSessionCategoryPlayback,
                withOptions = options,
                error = null
            )
            session.setActive(true, error = null)
        } catch (e: Exception) {
            println("IosAudioEngine: Failed to configure audio session: $e")
        }

        if (!mixWithOthers) {
            setNowPlayingInfo("Soundscape")
        } else {
            MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
        }
    }

    /**
     * Reconfigure the audio session when the mixWithOthers setting changes at runtime.
     */
    private fun reconfigureAudioSession() {
        configureAudioSession()
        println("IosAudioEngine: Audio session reconfigured (mixWithOthers=$mixWithOthers)")
    }

    /**
     * Matches AudioEngine.outputFormat() from the original iOS app:
     * Creates a stereo format with a channel layout at the given sample rate.
     */
    private fun outputFormat(sampleRate: Double): AVAudioFormat? {
        // AVAudioChannelLayout's failable ObjC init is bridged to a Kotlin constructor, which
        // throws on failure rather than returning null like the ObjC init can. kAudioChannelLayoutTag_Stereo
        // is a well-known built-in tag so this shouldn't realistically fail, but catch it anyway
        // to match this function's nullable-on-failure contract instead of crashing.
        val layout = try {
            AVAudioChannelLayout(layoutTag = kAudioChannelLayoutTag_Stereo)
        } catch (e: Exception) {
            println("IosAudioEngine: Failed to create stereo channel layout: ${e.message}")
            return null
        }
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
        envNode.distanceAttenuationParameters.referenceDistance =
            DEFAULT_RENDERING_DISTANCE.toFloat()
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
            .removePrefix("file:///android_asset/Sounds/")
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
            dispatch_async(audioQueue) {
                onDiscreteComplete(sound.handle)
            }
        })

        withActivePlayersLock {
            activePlayers[sound.handle] = PlayerEntry.Discrete(player, sound.isTts)
        }

        if (sound.isTts) {
            // Render TTS to PCM buffers, then connect and play through the audio graph
            ttsRenderer.render(sound.text) { buffers ->
                dispatch_async(audioQueue) {
                    // If the sound was cancelled (e.g. clearTextToSpeechQueue) while we were
                    // rendering, it is no longer in activePlayers. Bail without touching the
                    // engine — otherwise we would attach a node that no one will disconnect
                    // and the next teardown trips AVAudioEngine's `_nodes containsObject`
                    // assertion.
                    val stillActive =
                        withActivePlayersLock { activePlayers.containsKey(sound.handle) }
                    if (!stillActive) return@dispatch_async

                    if (buffers.isNotEmpty()) {
                        // Attach the layer
                        player.layer.format = buffers.first().format
                        player.layer.attach(engine)

                        if (is3D) {
                            connectLayerToEnvironment(player.layer)
                        } else {
                            player.layer.connect(engine.mainMixerNode)
                        }

                        val position = positionForType(
                            sound.audioType,
                            sound.latitude,
                            sound.longitude,
                            sound.heading
                        )
                        if (position != null) player.layer.position = position

                        player.layer.play()
                        player.scheduleTtsBuffers(buffers)
                    } else {
                        onDiscreteComplete(sound.handle)
                    }
                }
            }
        } else {
            // For earcons: load the WAV, then attach/connect/play
            if (!player.loadEarcon(sound.assetName)) {
                onDiscreteComplete(sound.handle)
                return
            }

            player.layer.attach(engine)
            if (is3D) {
                connectLayerToEnvironment(player.layer)
            } else {
                player.layer.connect(engine.mainMixerNode)
            }

            val position =
                positionForType(sound.audioType, sound.latitude, sound.longitude, sound.heading)
            if (position != null) player.layer.position = position

            player.layer.play()
            player.scheduleEarconBuffer()
        }
    }

    private fun onDiscreteComplete(handle: Long) {
        val entry = withActivePlayersLock { activePlayers.remove(handle) }
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

        // Match Android's ClearQueue: nuke *everything* queued behind the current
        // sound (TTS and earcons alike). Filtering just isTts here left earcons
        // piled up so callouts kept playing beeps long after the user cancelled.
        discreteQueue.clear()

        // Stop the current sound only if it's TTS — matches Android where
        // ttsEngine.stop() cancels in-flight TTS but leaves currently-playing
        // earcons/beacons alone. Removing from activePlayers *before* stop() so
        // that a TTS render callback already dispatched to the main queue for
        // this handle sees the sound as cancelled and bails on its own
        // containsKey check; otherwise it would attach a zombie node behind our
        // back. Clearing currentDiscreteHandle here also lets the next enqueued
        // sound play immediately instead of stacking behind the async
        // onDiscreteComplete.
        val currentHandle = currentDiscreteHandle ?: return
        val stopped = withActivePlayersLock {
            val entry = activePlayers[currentHandle]
            if (entry is PlayerEntry.Discrete && entry.isTts) {
                activePlayers.remove(currentHandle)
                entry
            } else null
        }
        if (stopped != null) {
            currentDiscreteHandle = null
            stopped.player.stop()
        }
    }

    override fun getQueueDepth(): Long {
        val current = if (currentDiscreteHandle != null) 1L else 0L
        return current + discreteQueue.size.toLong()
    }

    override fun isHandleActive(handle: Long): Boolean {
        if (withActivePlayersLock { activePlayers.containsKey(handle) }) return true
        // A handle enqueued behind the currently-playing sound isn't in activePlayers
        // yet — treat it as active so awaitHandle waits for its whole turn.
        return discreteQueue.any { it.handle == handle }
    }

    // --- AudioEngine Interface: Beacons ---

    override fun createBeacon(location: LngLatAlt, headingOnly: Boolean): Long {
        ensureEngineStarted()
        val handle = nextHandle++

        val type = BEACON_TYPES[currentBeaconType]
            ?: BEACON_TYPES["Current"]
            ?: error("BEACON_TYPES is missing its \"Current\" entry")
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

        withActivePlayersLock { activePlayers[handle] = PlayerEntry.Beacon(player) }
        return handle
    }

    override fun destroyBeacon(beaconHandle: Long) {
        val entry = withActivePlayersLock { activePlayers.remove(beaconHandle) }
        if (entry is PlayerEntry.Beacon) {
            entry.player.stop()
        }
    }

    override fun toggleBeaconMute(): Boolean {
        beaconMuted = !beaconMuted
        val beacons = withActivePlayersLock {
            activePlayers.values.filterIsInstance<PlayerEntry.Beacon>()
        }
        for (entry in beacons) {
            entry.player.setMuted(beaconMuted)
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

        // Update all active beacon players with new geometry. Snapshot under
        // the lock so we don't iterate the live map (which can be mutated on
        // the main queue by onDiscreteComplete or by other callers).
        val beacons = withActivePlayersLock {
            activePlayers.values.filterIsInstance<PlayerEntry.Beacon>()
        }
        for (entry in beacons) {
            entry.player.updateForGeometry(
                listenerLatitude, listenerLongitude, this.listenerHeading
            )
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

    fun setSpeechVoice(voiceId: String?) {
        ttsRenderer.setVoiceId(voiceId)
    }

    fun setSpeechRate(multiplier: Float) {
        ttsRenderer.setRateMultiplier(multiplier)
    }

    override fun onAllBeaconsCleared() {
        val beaconHandles = withActivePlayersLock {
            activePlayers.entries
                .filter { it.value is PlayerEntry.Beacon }
                .map { it.key }
        }
        for (handle in beaconHandles) {
            destroyBeacon(handle)
        }
    }

    override fun setHrtfEnabled(enabled: Boolean) {
        // AVAudioEnvironmentNode uses HRTF by default
        // Could toggle rendering algorithm here if needed
    }

    // --- Audio Session Interruption Handling ---

    private fun registerAudioSessionObservers() {
        val center = NSNotificationCenter.defaultCenter

        interruptionObserver = center.addObserverForName(
            name = platform.AVFAudio.AVAudioSessionInterruptionNotification,
            `object` = AVAudioSession.sharedInstance(),
            queue = NSOperationQueue.mainQueue
        ) { notification ->
            handleInterruption(notification)
        }

        mediaResetObserver = center.addObserverForName(
            name = platform.AVFAudio.AVAudioSessionMediaServicesWereResetNotification,
            `object` = AVAudioSession.sharedInstance(),
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            handleMediaServicesReset()
        }
    }

    private fun handleInterruption(notification: NSNotification?) {
        val userInfo = notification?.userInfo ?: return
        val typeValue = userInfo[AVAudioSessionInterruptionTypeKey] as? Long ?: return

        if (typeValue == AVAudioSessionInterruptionTypeBegan.toLong()) {
            println("IosAudioEngine: Audio session interruption began")
            needsReactivation = true
        } else if (typeValue == AVAudioSessionInterruptionTypeEnded.toLong()) {
            println("IosAudioEngine: Audio session interruption ended")
            // Reactivate audio session
            val session = AVAudioSession.sharedInstance()
            try {
                session.setActive(true, error = null)
                needsReactivation = false
                println("IosAudioEngine: Audio session reactivated after interruption")
            } catch (e: Exception) {
                println("IosAudioEngine: Failed to reactivate audio session: $e")
            }

            // Restart engine if needed
            if (!engine.isRunning()) {
                try {
                    engine.startAndReturnError(null)
                    println("IosAudioEngine: Engine restarted after interruption")
                } catch (e: Exception) {
                    println("IosAudioEngine: Failed to restart engine: $e")
                }
            }
        }
    }

    private fun handleMediaServicesReset() {
        println("IosAudioEngine: Media services were reset — reconfiguring")
        engineStarted = false
        environmentNodes.clear()
        ensureEngineStarted()
    }

    // --- Now Playing Info ---

    fun setNowPlayingInfo(title: String, subtitle: String? = null) {
        val info = mutableMapOf<Any?, Any?>(
            MPNowPlayingInfoPropertyMediaType to 1L, // MPNowPlayingInfoMediaType.audio
            MPMediaItemPropertyTitle to title,
        )
        if (subtitle != null) {
            info[MPMediaItemPropertyArtist] = subtitle
        }
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
    }

    // --- Remote Command Center ---

    private fun registerRemoteCommands() {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()

        commandCenter.togglePlayPauseCommand.setEnabled(true)
        commandCenter.togglePlayPauseCommand.addTargetWithHandler { _ ->
            mediaControlTarget?.onPlayPause()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.playCommand.setEnabled(true)
        commandCenter.playCommand.addTargetWithHandler { _ ->
            mediaControlTarget?.onPlayPause()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.pauseCommand.setEnabled(true)
        commandCenter.pauseCommand.addTargetWithHandler { _ ->
            mediaControlTarget?.onPlayPause()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.nextTrackCommand.setEnabled(true)
        commandCenter.nextTrackCommand.addTargetWithHandler { _ ->
            mediaControlTarget?.onNext()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.previousTrackCommand.setEnabled(true)
        commandCenter.previousTrackCommand.addTargetWithHandler { _ ->
            mediaControlTarget?.onPrevious()
            MPRemoteCommandHandlerStatusSuccess
        }
    }
}
