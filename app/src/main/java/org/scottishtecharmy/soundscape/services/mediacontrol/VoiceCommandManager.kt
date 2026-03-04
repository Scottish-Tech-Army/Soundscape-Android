package org.scottishtecharmy.soundscape.services.mediacontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine.Companion.EARCON_CALLOUTS_OFF
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine.Companion.EARCON_CALLOUTS_ON
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.utils.Analytics
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
            if (audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                // clearCommunicationDevice is async; wait for routing to return to normal.
                audioManager.addOnCommunicationDeviceChangedListener(
                    ContextCompat.getMainExecutor(context),
                    object : AudioManager.OnCommunicationDeviceChangedListener {
                        override fun onCommunicationDeviceChanged(device: AudioDeviceInfo?) {
                            println("CommunicationDevice cleared: ${device?.type}")
                            if (device?.type != AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                                audioManager.removeOnCommunicationDeviceChangedListener(this)
                                onStopped()
                            }
                        }
                    }
                )
                audioManager.clearCommunicationDevice()
            } else {
                onStopped()
            }
        } else {
            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothScoOn) {
                // stopBluetoothSco is async; wait for DISCONNECTED before calling onStopped.
                scoDisconnectedCallback = onStopped
                val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
                context.registerReceiver(scoReceiver, filter)
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
            } else {
                onStopped()
            }
        }
    }

    // ── Commands & matching ─────────────────────────────────────────────────────

    data class VoiceCommand(val stringId: Int, val action: (arg: ArrayList<String>) -> Unit)

    private val simpleCommands = arrayOf(
        VoiceCommand(R.string.directions_my_location)          { service.myLocation() },
        VoiceCommand(R.string.help_orient_page_title)          { service.whatsAroundMe() },
        VoiceCommand(R.string.help_explore_page_title)         { service.aheadOfMe() },
        VoiceCommand(R.string.callouts_nearby_markers)         { service.nearbyMarkers() },
        VoiceCommand(R.string.route_detail_action_next)        { service.routeSkipNext() },
        VoiceCommand(R.string.route_detail_action_previous)    { service.routeSkipPrevious() },
        VoiceCommand(R.string.beacon_action_mute_beacon)       { service.routeMute() },
        VoiceCommand(R.string.route_detail_action_stop_route)  { service.routeStop() },
        VoiceCommand(R.string.voice_cmd_list_routes)           { service.routeListRoutes() },
        VoiceCommand(R.string.voice_cmd_list_markers)          { service.routeListMarkers() },
        VoiceCommand(R.string.menu_help)                       { voiceHelp() },
    )

    private fun matchDynamicMarkers(speech: String): Boolean {
        val markers = listOfMarkers
        val routes = listOfRoutes
        var minMatch = Double.MAX_VALUE

        // Markers
        var bestMarker: MarkerEntity? = null
        for (marker in markers) {
            val commandString = context.getString(R.string.voice_cmd_start_beacon_at_marker_with_name)
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
            val commandString = context.getString(R.string.voice_cmd_start_route)
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
                val commandString = context.getString(command.stringId).lowercase()
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
                Analytics.getInstance().logEvent("voice_command_recognized", null)
                return
            }
        }

        if (bestMatch != null) {
            println("Found command: ${context.getString(bestMatch.stringId)}")
            // Pass in all the speech strings, it may be that the argument is clearer in ones
            // other than our best match.
            bestMatch.action(speech)
            Analytics.getInstance().logEvent("voice_command_recognized", null)
        } else {
            service.speak2dText(
                context.getString(R.string.voice_cmd_not_recognized).format(speech.first()),
                false,
                EARCON_CALLOUTS_OFF
            )
            Analytics.getInstance().logEvent("voice_command_not_recognized", null)
        }
    }

    private fun voiceHelp() {
        val firstRoute = listOfRoutes.firstOrNull()?.name
        val firstMarker = listOfMarkers.firstOrNull()?.name

        val commandNames = simpleCommands.map { context.getString(it.stringId) }
        val builder = StringBuilder()
        builder.append(context.getString(R.string.voice_cmd_help_response))
        commandNames.forEach { builder.append(it).append(". ") }

        if (firstRoute != null || firstMarker != null)
            builder.append(context.getString(R.string.voice_cmd_explain_dynamic_markers))
        if (firstRoute != null)
            builder.append(context.getString(R.string.voice_cmd_start_route).format(firstRoute)).append(". ")
        if (firstMarker != null)
            builder.append(context.getString(R.string.voice_cmd_start_beacon_at_marker_with_name).format(firstMarker)).append(". ")

        service.speak2dText(builder.toString())
    }

    // ── Speech recognition ──────────────────────────────────────────────────────

    private val errorMap = mapOf(
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT to R.string.voice_cmd_speech_recognition_error_network_timeout,
        SpeechRecognizer.ERROR_NETWORK to R.string.voice_cmd_speech_recognition_error_network,
        SpeechRecognizer.ERROR_AUDIO to R.string.voice_cmd_speech_recognition_error_audio,
        SpeechRecognizer.ERROR_SERVER to R.string.voice_cmd_speech_recognition_error_server,
        SpeechRecognizer.ERROR_CLIENT to R.string.voice_cmd_speech_recognition_error_client,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT to R.string.voice_cmd_speech_recognition_error_speech_timeout,
        SpeechRecognizer.ERROR_NO_MATCH to R.string.voice_cmd_speech_recognition_not_match,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY to R.string.voice_cmd_speech_recognition_busy,
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS to R.string.voice_cmd_speech_recognition_error_permissions,
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS to R.string.voice_cmd_speech_recognition_error_too_many_requests,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED to R.string.voice_cmd_speech_recognition_error_server_disconnected,
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED to R.string.voice_cmd_speech_recognition_error_language_not_supported,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE to R.string.voice_cmd_speech_recognition_error_language_unavailable,
        SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT to R.string.voice_cmd_speech_recognition_error_cannot_check_support,
        SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS to R.string.voice_cmd_speech_recognition_error_cannot_listen_to_download_events,
    )

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = VoiceCommandState.Listening
        }

        override fun onResults(results: Bundle?) {
            _state.value = VoiceCommandState.Idle
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.forEach { println("speech: $it") }
            stopBluetoothSco {
                if (matches != null) handleSpeech(matches)
            }
        }

        override fun onError(error: Int) {
            println("onError $error")
            val errorResource = errorMap[error]
            val errorText = if (errorResource != null)
                context.getString(errorResource)
            else
                context.getString(R.string.voice_cmd_speech_recognition_error_unknown)
            stopBluetoothSco {
                service.speak2dText(errorText, false, EARCON_CALLOUTS_OFF)
            }
            _state.value = VoiceCommandState.Error
        }

        override fun onEndOfSpeech() {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun buildRecognitionIntent(language: String?): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (language != null) putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val biasingStrings = ArrayList<String>()
                simpleCommands.forEach { biasingStrings.add(context.getString(it.stringId)) }
                val markers = listOfMarkers
                val routes = listOfRoutes
                for (marker in markers)
                    biasingStrings.add(context.getString(R.string.voice_cmd_start_beacon_at_marker_with_name).format(marker.name))
                for (route in routes)
                    biasingStrings.add(context.getString(R.string.voice_cmd_start_route).format(route.name))
                putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, biasingStrings)
            }
            Analytics.getInstance().logEvent("trigger_voice_command", null)
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
                context.getString(R.string.voice_cmd_speech_recognition_error_unsupported),
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
        stopBluetoothSco()
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: route to Bluetooth SCO mic via setCommunicationDevice if available.
            val bluetoothSco = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            if (bluetoothSco != null) {
                // setCommunicationDevice is async; wait for confirmation before starting.
                audioManager.addOnCommunicationDeviceChangedListener(
                    ContextCompat.getMainExecutor(context),
                    object : AudioManager.OnCommunicationDeviceChangedListener {
                        override fun onCommunicationDeviceChanged(device: AudioDeviceInfo?) {
                            println("CommunicationDevice changed: ${device?.type}")
                            if (device?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                                audioManager.removeOnCommunicationDeviceChangedListener(this)
                                startListeningInternal()
                            }
                        }
                    }
                )
                audioManager.setCommunicationDevice(bluetoothSco)
            } else {
                startListeningInternal()
            }
        } else {
            // API < 31: SCO connection is async; wait for the broadcast before starting.
            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothScoOn) {
                startListeningInternal()
                return
            }
            @Suppress("DEPRECATION")
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            context.registerReceiver(scoReceiver, filter)
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = true
        }
    }

    // Must be called on the main thread (satisfied: service is on main thread).
    private fun startListeningInternal() {
        if (_state.value is VoiceCommandState.Listening) return
        service.speak2dText(context.getString(R.string.voice_cmd_listening), false, EARCON_CALLOUTS_ON)
        val deadline = System.currentTimeMillis() + 1000L
        while (service.isAudioEngineBusy() && System.currentTimeMillis() < deadline) {
            sleep(20)
        }
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
