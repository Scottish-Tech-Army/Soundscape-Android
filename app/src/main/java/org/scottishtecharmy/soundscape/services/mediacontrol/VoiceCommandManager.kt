@file:Suppress("DEPRECATION")

package org.scottishtecharmy.soundscape.services.mediacontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.RecognizerIntent.EXTRA_AUDIO_SOURCE
import android.speech.RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT
import android.speech.RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE
import android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine.Companion.EARCON_CALLOUTS_OFF
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.services.SoundscapeService
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine.Companion.EARCON_CALLOUTS_ON
import org.scottishtecharmy.soundscape.utils.AnalyticsProvider
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import java.lang.Thread.sleep
import java.util.Locale

sealed class VoiceCommandState {
    object Idle : VoiceCommandState()
    object Listening : VoiceCommandState()
    object Error : VoiceCommandState()
}

class VoiceCommandManager(
    private val service: SoundscapeService
) {

    // ── Properties ──────────────────────────────────────────────────────────────

    private var context: Context = service
    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow<VoiceCommandState>(VoiceCommandState.Idle)
    val state: StateFlow<VoiceCommandState> = _state.asStateFlow()
    @Volatile private var listOfRoutes: List<RouteEntity> = emptyList()
    @Volatile private var listOfMarkers: List<MarkerEntity> = emptyList()
    // Language tag validated against the recognizer's supported list; set by initialize().
    private var cachedLanguage: String? = null
    private val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // ── Bluetooth audio capture (pipes mic audio to SpeechRecognizer) ──────────

    private var btAudioRecord: AudioRecord? = null
    private var btPipeReadFd: ParcelFileDescriptor? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val btCaptureTimeout = Runnable { stopBluetoothAudioCapture(closeReadEnd = true) }

    @Suppress("MissingPermission") // RECORD_AUDIO already required for SpeechRecognizer
    private fun startBluetoothAudioCapture(btDevice: AudioDeviceInfo) {
        // Find the Bluetooth input device from the actual input devices list —
        // availableCommunicationDevices may not be usable as an AudioRecord source.
        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        inputs.forEach { println("Input device: type=${it.type} addr=${it.address} name=${it.productName}") }
        val btInput = inputs.firstOrNull {
            it.type == btDevice.type && it.address == btDevice.address
        } ?: inputs.firstOrNull {
            it.type == btDevice.type
        }
        println("BT input device: type=${btInput?.type} addr=${btInput?.address}")

        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize <= 0) {
            println("BT audio: getMinBufferSize failed ($bufferSize)")
            return
        }
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            println("BT audio: AudioRecord failed to initialize (state=${record.state})")
            record.release()
            return
        }

        record.preferredDevice = btInput
        btAudioRecord = record

        val pipe = ParcelFileDescriptor.createPipe()
        btPipeReadFd = pipe[0]
        val pipeWriteFd = pipe[1]

        record.startRecording()
        println("BT AudioRecord state: ${record.recordingState}, device: ${record.routedDevice?.type}, bufferSize: $bufferSize")

        Thread({
            val buf = ByteArray(bufferSize)

            val debugStream : java.io.FileOutputStream? = null
//            val debugFile = java.io.File(context.getExternalFilesDir(null), "bt_audio_debug.pcm")
//            val debugStream = java.io.FileOutputStream(debugFile)
//            println("BT audio: writing debug PCM to ${debugFile.absolutePath}")

            var discard = true
            ParcelFileDescriptor.AutoCloseOutputStream(pipeWriteFd).use { os ->
                while (!Thread.currentThread().isInterrupted) {
                    val read = record.read(buf, 0, buf.size)
                    if (read > 0) {
                        if(discard) {
                            discard = false
                            continue
                        }

                        debugStream?.write(buf, 0, read)
                        os.write(buf, 0, read)
                    } else {
                        println("BT audio: read() returned $read, stopping")
                        break
                    }
                }
            }
            debugStream?.close()
            btAudioRecord?.release()
            btAudioRecord = null

            println("BT audio pipe closed")
        }, "BT-audio-capture").start()

        // Safety timeout: close the pipe if the recognizer hasn't finished
        // (e.g. no speech detected). Without this the recognizer hangs
        // waiting for EOF on the EXTRA_AUDIO_SOURCE pipe.
        mainHandler.postDelayed(btCaptureTimeout, 10_000)
    }

    /**
     * Stop Bluetooth audio capture.  When [closeReadEnd] is false (the default for
     * onEndOfSpeech), only the write side of the pipe is closed so the
     * recognizer sees a clean EOF and can finish processing.  The read end
     * is left open for the recognizer to drain; it will be closed later in
     * [cleanUpBtPipe].  When [closeReadEnd] is true (timeout / destroy),
     * both ends are closed immediately.
     */
    private fun stopBluetoothAudioCapture(closeReadEnd: Boolean = false) {
        mainHandler.removeCallbacks(btCaptureTimeout)
        btAudioRecord?.stop()
    }

    /** Close the read end of the BT pipe after recognition completes. */
    private fun cleanUpBtPipe() {
        btPipeReadFd?.close()
        btPipeReadFd = null
    }

    // ── Bluetooth SCO helpers ───────────────────────────────────────────────────

    // Legacy SCO receiver used only on API < 31.
    // Handles both start (wait for CONNECTED) and stop (wait for DISCONNECTED) paths.
    private var scoDisconnectedCallback: (() -> Unit)? = null
    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED) return
            val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            when (state) {
                AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> println("SCO Audio state DISCONNECTED")
                AudioManager.SCO_AUDIO_STATE_CONNECTING   -> println("SCO Audio state CONNECTING")
                AudioManager.SCO_AUDIO_STATE_CONNECTED    -> println("SCO Audio state CONNECTED")
                else -> println("SCO Audio state $state")
            }
            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED && scoDisconnectedCallback == null) {
                // Start path: SCO is now active, begin recognition.
                context.unregisterReceiver(this)
                startListeningInternal()
            } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED && scoDisconnectedCallback != null) {
                // Stop path: SCO has disconnected, safe to play audio output.
                val callback = scoDisconnectedCallback
                scoDisconnectedCallback = null
                context.unregisterReceiver(this)
                callback!!()
            }
        }
    }

    private fun stopBluetoothSco(onStopped: () -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isBluetooth(audioManager.communicationDevice)) {
                // clearCommunicationDevice is async; wait for routing to return to normal.
                audioManager.addOnCommunicationDeviceChangedListener(
                    ContextCompat.getMainExecutor(context),
                    object : AudioManager.OnCommunicationDeviceChangedListener {
                        override fun onCommunicationDeviceChanged(device: AudioDeviceInfo?) {
                            println("CommunicationDevice cleared: ${device?.type}")
                            if (!isBluetooth(device)) {
                                audioManager.removeOnCommunicationDeviceChangedListener(this)
                                // Allow audio stream to restart on A2DP
                                service.audioEngine.setSuppressRestart(false)
                                stopBluetoothAudioCapture(closeReadEnd = true)
                                onStopped()
                            }
                        }
                    }
                )
                audioManager.clearCommunicationDevice()
                audioManager.mode = AudioManager.MODE_NORMAL
            } else {
                onStopped()
            }
        } else {
            if (audioManager.isBluetoothScoOn) {
                // stopBluetoothSco is async; wait for DISCONNECTED before calling onStopped.
                scoDisconnectedCallback = {
                    // Allow audio stream to restart on A2DP
                    service.audioEngine.setSuppressRestart(false)
                    onStopped()
                }
                val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
                context.registerReceiver(scoReceiver, filter)
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
            } else {
                onStopped()
            }
        }
    }

    // ── Commands & matching ─────────────────────────────────────────────────────

    data class VoiceCommand(val stringId: StringResource, val action: (arg: ArrayList<String>) -> Unit)

    private val simpleCommands = arrayOf(
        VoiceCommand(Res.string.directions_my_location)          { service.myLocation() },
        VoiceCommand(Res.string.help_orient_page_title)          { service.whatsAroundMe() },
        VoiceCommand(Res.string.help_explore_page_title)         { service.aheadOfMe() },
        VoiceCommand(Res.string.callouts_nearby_markers)         { service.nearbyMarkers() },
        VoiceCommand(Res.string.route_detail_action_next)        { service.routeSkipNext() },
        VoiceCommand(Res.string.route_detail_action_previous)    { service.routeSkipPrevious() },
        VoiceCommand(Res.string.beacon_action_mute_beacon)       { service.routeMute() },
        VoiceCommand(Res.string.route_detail_action_stop_route)  { service.routeStop() },
        VoiceCommand(Res.string.voice_cmd_list_routes)           { service.routeListRoutes() },
        VoiceCommand(Res.string.voice_cmd_list_markers)          { service.routeListMarkers() },
        VoiceCommand(Res.string.menu_help)                       { voiceHelp() },
    )

    private fun matchDynamicMarkers(speech: String): Boolean {
        val markers = listOfMarkers
        val routes = listOfRoutes
        var minMatch = Double.MAX_VALUE

        // Markers
        var bestMarker: MarkerEntity? = null
        for (marker in markers) {
            val commandString = kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_start_beacon_at_marker_with_name) }
                .format(marker.name).lowercase()
            println("Marker compare \"$commandString\" with \"$speech\"")
            val match = commandString.fuzzyCompare(speech, false)
            if (match < 0.2 && match < minMatch) {
                minMatch = match
                bestMarker = marker
            }
        }
        if (bestMarker != null) {
            service.markerStart(bestMarker)
            return true
        }

        // Routes
        var bestRoute: RouteEntity? = null
        for (route in routes) {
            val commandString = kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_start_route) }
                .format(route.name).lowercase()
            println("Route compare \"$commandString\" with \"$speech\"")
            val match = commandString.fuzzyCompare(speech, false)
            if (match < 0.2 && match < minMatch) {
                minMatch = match
                bestRoute = route
            }
        }
        if (bestRoute != null) {
            service.routeStart(bestRoute)
            return true
        }

        return false
    }

    private fun handleSpeech(speech: ArrayList<String>) {
        var bestMatch: VoiceCommand? = null
        for (text in speech) {
            val t = text.lowercase()
            var minMatch = Double.MAX_VALUE

            // Start with simpleCommands which don't contain any dynamic arguments
            for (command in simpleCommands) {
                val commandString = kotlinx.coroutines.runBlocking { getString(command.stringId) }.lowercase()
                val match = commandString.fuzzyCompare(t, false)
                if (match < 0.3 && match < minMatch) {
                    minMatch = match
                    bestMatch = command
                }
            }
            if (bestMatch != null) break
            println("No simple command match found")

            // Check if we match any of our dynamic markers
            if (matchDynamicMarkers(t)) {
                AnalyticsProvider.getInstance().logEvent("voice_command_recognized", null)
                return
            }
        }

        if (bestMatch != null) {
            println("Found command: ${kotlinx.coroutines.runBlocking { getString(bestMatch.stringId) }}")
            // Pass in all the speech strings, it may be that the argument is clearer in ones
            // other than our best match.
            bestMatch.action(speech)
            AnalyticsProvider.getInstance().logEvent("voice_command_recognized", null)
        } else {
            service.speak2dText(
                kotlinx.coroutines.runBlocking { getString(
                    Res.string.voice_cmd_not_recognized) }.format(
                    speech.firstOrNull() ?: "",
                    kotlinx.coroutines.runBlocking { getString(Res.string.menu_help) }),
                false,
                EARCON_CALLOUTS_OFF
            )
            AnalyticsProvider.getInstance().logEvent("voice_command_not_recognized", null)
        }
    }

    private fun voiceHelp() {
        val firstRoute = listOfRoutes.firstOrNull()?.name
        val firstMarker = listOfMarkers.firstOrNull()?.name

        val commandNames = simpleCommands.map { kotlinx.coroutines.runBlocking { getString(it.stringId) } }
        val builder = StringBuilder()
        builder.append(kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_help_response) })
        commandNames.forEach { builder.append(it).append(". ") }

        if (firstRoute != null || firstMarker != null)
            builder.append(kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_explain_dynamic_markers) })
        if (firstRoute != null)
            builder.append(kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_start_route) }.format(firstRoute)).append(". ")
        if (firstMarker != null)
            builder.append(kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_start_beacon_at_marker_with_name) }.format(firstMarker)).append(". ")

        service.speak2dText(builder.toString())
    }

    // ── Speech recognition ──────────────────────────────────────────────────────

    @Suppress("NewApi") // Inlined int constants, safe on all API levels
    private val errorMap = mapOf(
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT to Res.string.voice_cmd_speech_recognition_error_network_timeout,
        SpeechRecognizer.ERROR_NETWORK to Res.string.voice_cmd_speech_recognition_error_network,
        SpeechRecognizer.ERROR_AUDIO to Res.string.voice_cmd_speech_recognition_error_audio,
        SpeechRecognizer.ERROR_SERVER to Res.string.voice_cmd_speech_recognition_error_server,
        SpeechRecognizer.ERROR_CLIENT to Res.string.voice_cmd_speech_recognition_error_client,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT to Res.string.voice_cmd_speech_recognition_error_speech_timeout,
        SpeechRecognizer.ERROR_NO_MATCH to Res.string.voice_cmd_speech_recognition_not_match,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY to Res.string.voice_cmd_speech_recognition_busy,
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS to Res.string.voice_cmd_speech_recognition_error_permissions,
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS to Res.string.voice_cmd_speech_recognition_error_too_many_requests,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED to Res.string.voice_cmd_speech_recognition_error_server_disconnected,
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED to Res.string.voice_cmd_speech_recognition_error_language_not_supported,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE to Res.string.voice_cmd_speech_recognition_error_language_unavailable,
        SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT to Res.string.voice_cmd_speech_recognition_error_cannot_check_support,
        SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS to Res.string.voice_cmd_speech_recognition_error_cannot_listen_to_download_events,
    )

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = VoiceCommandState.Listening
        }

        override fun onResults(results: Bundle?) {
            _state.value = VoiceCommandState.Idle
            cleanUpBtPipe()
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.forEach { println("speech: $it") }
            stopBluetoothSco {
                if (matches != null) handleSpeech(matches)
            }
        }

        override fun onError(error: Int) {
            println("onError $error")
            cleanUpBtPipe()
            val errorResource = errorMap[error]
            val errorText = if (errorResource != null)
                kotlinx.coroutines.runBlocking { getString(errorResource) }
            else
                kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_speech_recognition_error_unknown) }
            stopBluetoothSco {
                service.speak2dText(errorText, false, EARCON_CALLOUTS_OFF)
            }
            _state.value = VoiceCommandState.Error
        }

        override fun onEndOfSpeech() {
            // Close the BT audio pipe so the recognizer sees EOF and processes results.
            println("onEndOfSpeech")
            stopBluetoothAudioCapture()
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onPartialResults(partialResults: Bundle?) {
            println("onPartialResults")
            onResults(partialResults)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun buildRecognitionIntent(language: String?): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (language != null) putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val biasingStrings = ArrayList<String>()
                simpleCommands.forEach { biasingStrings.add(kotlinx.coroutines.runBlocking { getString(it.stringId) }) }
                val markers = listOfMarkers
                val routes = listOfRoutes
                for (marker in markers)
                    biasingStrings.add(kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_start_beacon_at_marker_with_name) }.format(marker.name))
                for (route in routes)
                    biasingStrings.add(kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_start_route) }.format(route.name))
                putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, biasingStrings)
            }
            // If BT audio capture is active, pipe our AudioRecord to the recognizer
            // instead of letting it open its own (which wouldn't see the BT mic).
            btPipeReadFd?.let {
                putExtra(EXTRA_AUDIO_SOURCE, it)
                putExtra(EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                //putExtra(EXTRA_AUDIO_SOURCE_ENCODING, )
                putExtra(EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16000)
            } ?: putExtra(EXTRA_PARTIAL_RESULTS, false)
            AnalyticsProvider.getInstance().logEvent("trigger_voice_command", null)
        }

    /**
     * Choose the best BCP-47 language tag from [supportedTags] for the given [locale].
     * Prefers an exact match (language + country); falls back to any tag with the same language
     * code; returns null if nothing matches.
     */
    private fun pickBestLanguage(locale: Locale, supportedTags: List<String>): String? {
        val tag = locale.toLanguageTag()
        println("pickBestLanguage for $tag")
        if (tag in supportedTags) return tag
        val langCode = locale.language
        return supportedTags.firstOrNull { Locale.forLanguageTag(it).language == langCode }
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    /** Must be called on the main thread when switching to VoiceControl mode. */
    fun initialize() {
        destroyRecognizer()
        cachedLanguage = null

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            service.speak2dText(
                kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_speech_recognition_error_unsupported) },
                false,
                EARCON_CALLOUTS_OFF
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: create the recognizer once; foreground service credentials are stable.
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(listener)
        }
        // API < 31: recognizer is created fresh in startListeningInternal() each time
        // to avoid ERROR_INSUFFICIENT_PERMISSIONS when the screen is locked.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val probeIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            speechRecognizer?.checkRecognitionSupport(
                probeIntent,
                ContextCompat.getMainExecutor(context),
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                        val supported = recognitionSupport.installedOnDeviceLanguages +
                                recognitionSupport.onlineLanguages
                        supported.forEach { println("Supported language: $it") }
                        cachedLanguage = pickBestLanguage(getCurrentLocale(), supported)
                        println("cachedLanguage $cachedLanguage")
                    }

                    override fun onError(error: Int) {
                        // Support query failed — cachedLanguage stays null (no EXTRA_LANGUAGE).
                    }
                }
            )
        }
    }

    fun destroy() {
        stopBluetoothAudioCapture(closeReadEnd = true)
        stopBluetoothSco()
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    /**
     * Parse the stored microphone preference value ("Auto", or "type|identifier") and find
     * the matching device.  Returns null for "Auto" or if the device is no longer available.
     */
    private fun findPreferredDevice(micPref: String): AudioDeviceInfo? {
        if (micPref == MainActivity.VOICE_COMMAND_MICROPHONE_DEFAULT) return null
        val parts = micPref.split("|", limit = 2)
        if (parts.size != 2) return null
        val type = parts[0].toIntOrNull() ?: return null
        val identifier = parts[1]
        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        return inputs.firstOrNull {
            it.type == type && (
                (it.address.isNotBlank() && it.address == identifier) ||
                (it.address.isBlank() && it.productName.toString() == identifier)
            )
        }
    }

    @Suppress("NewApi") // Inlined int constants, safe on all API levels
    private fun isBluetooth(device: AudioDeviceInfo?): Boolean =
        device?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        device?.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
        device?.type == AudioDeviceInfo.TYPE_HEARING_AID

    fun startListening() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val micPref = prefs.getString(
            MainActivity.VOICE_COMMAND_MICROPHONE_KEY,
            MainActivity.VOICE_COMMAND_MICROPHONE_DEFAULT
        ) ?: MainActivity.VOICE_COMMAND_MICROPHONE_DEFAULT

        val preferred = findPreferredDevice(micPref)

        if (prefs.getBoolean(
                MainActivity.VOICE_COMMAND_LISTENING_PROMPT_KEY,
                MainActivity.VOICE_COMMAND_LISTENING_PROMPT_DEFAULT
            )) {
            service.speak2dText(kotlinx.coroutines.runBlocking { getString(Res.string.voice_cmd_listening) }, false, EARCON_CALLOUTS_ON)
            val deadline = System.currentTimeMillis() + 1000L
            while (service.isAudioEngineBusy() && System.currentTimeMillis() < deadline) {
                sleep(20)
            }
            sleep(200)
        }

        // If the user picked a non-Bluetooth device (e.g. built-in mic), skip BT routing.
        if (preferred != null && !isBluetooth(preferred)) {
            startListeningInternal()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: route to Bluetooth SCO mic via setCommunicationDevice.
            // If a specific BT device was chosen, use it; otherwise pick the first available.
            val bluetoothHeadset = if (preferred != null) {
                audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == preferred.type && it.address == preferred.address
                }
            } else {
                audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_HEARING_AID
                }
            }
            if (bluetoothHeadset != null) {
                // Suppress audio stream restart while SCO/BLE is active
                service.audioEngine.setSuppressRestart(true)
                val useAudioCapture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                // setCommunicationDevice is async; wait for confirmation before starting.
                audioManager.addOnCommunicationDeviceChangedListener(
                    ContextCompat.getMainExecutor(context),
                    object : AudioManager.OnCommunicationDeviceChangedListener {
                        override fun onCommunicationDeviceChanged(device: AudioDeviceInfo?) {
                            println("CommunicationDevice changed: ${device?.type}")
                            if (isBluetooth(device)) {
                                audioManager.removeOnCommunicationDeviceChangedListener(this)
                                if (useAudioCapture) {
                                    // The SpeechRecognizer runs in Google's process
                                    // which doesn't see our per-process communication
                                    // device routing.  Now that BT routing is active,
                                    // capture audio ourselves and pipe it to the recognizer.
                                    startBluetoothAudioCapture(bluetoothHeadset)
                                }
                                startListeningInternal()
                            }
                        }
                    }
                )
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.setCommunicationDevice(bluetoothHeadset)
            } else {
                startListeningInternal()
            }
        } else {
            // API < 31: SCO connection is async; wait for the broadcast before starting.
            if (audioManager.isBluetoothScoOn) {
                startListeningInternal()
                return
            }
            if(audioManager.isBluetoothScoAvailableOffCall) {
                // Suppress audio stream restart while SCO is active
                service.audioEngine.setSuppressRestart(true)
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
                val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
                context.registerReceiver(scoReceiver, filter)
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            } else {
                startListeningInternal()
            }
        }
    }

    // Must be called on the main thread (satisfied: service is on main thread).
    private fun startListeningInternal() {
        if (_state.value is VoiceCommandState.Listening) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // API < 31: recreate the recognizer each time so the binding has current
            // foreground credentials (avoids ERROR_INSUFFICIENT_PERMISSIONS when screen locked).
            destroyRecognizer()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(listener)
        }
        speechRecognizer?.startListening(buildRecognitionIntent(cachedLanguage))
    }

    /** Call this whenever SoundscapeService updates its localizedContext. */
    fun updateContext(newContext: Context) {
        context = newContext
    }

    fun updateRoutes(routes: List<RouteEntity>) {
        listOfRoutes = routes
    }

    fun updateMarkers(markers: List<MarkerEntity>) {
        listOfMarkers = markers
    }
}
