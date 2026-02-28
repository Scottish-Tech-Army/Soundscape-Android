package org.scottishtecharmy.soundscape.services.mediacontrol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine.Companion.EARCON_CALLOUTS_OFF
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.getCurrentLocale

sealed class VoiceCommandState {
    object Idle : VoiceCommandState()
    object Listening : VoiceCommandState()
    object Error : VoiceCommandState()
}

class VoiceCommandManager(
    private val service: SoundscapeService,
    private val onError: () -> Unit
) {

    private var context: Context = service
    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow<VoiceCommandState>(VoiceCommandState.Idle)
    val state: StateFlow<VoiceCommandState> = _state.asStateFlow()

    /** Call this whenever SoundscapeService updates its localizedContext. */
    fun updateContext(newContext: Context) {
        context = newContext
    }

    // Must be called on the main thread (satisfied: service is on main thread)
    fun startListening() {
        if (_state.value is VoiceCommandState.Listening) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            println("Recognition is unavailable")
            onError()
            return
        }
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(listener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // TODO: We need to query the API to find out which Locales are supported and use one of
            //  those. For example, we might want to use es_ES even if we're in another country.
            //  https://medium.com/@andraz.pajtler/android-speech-to-text-the-missing-guide-part-1-824e2636c45a
            // Match recognizer language to the app's configured locale
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, getCurrentLocale().toLanguageTag())
        }
        speechRecognizer?.startListening(intent)
    }

    fun destroy() {
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = VoiceCommandState.Listening
        }

        override fun onResults(results: Bundle?) {
            _state.value = VoiceCommandState.Idle
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.forEach { println("speech: $it") }
            if (matches != null)
                handleSpeech(matches)
        }

        override fun onError(error: Int) {
            println("onError $error")
            destroyRecognizer()
            _state.value = VoiceCommandState.Error
            onError()
        }

        override fun onEndOfSpeech() {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    data class VoiceCommand(val stringId: Int, val action: (arg: String) -> Unit)

    val commands = arrayOf(
        VoiceCommand(R.string.directions_my_location) { service.myLocation() },
        VoiceCommand(R.string.help_orient_page_title)             { service.whatsAroundMe() },
        VoiceCommand(R.string.help_explore_page_title)            { service.aheadOfMe() },
        VoiceCommand(R.string.callouts_nearby_markers)            { service.nearbyMarkers() },
        VoiceCommand(R.string.route_detail_action_next)           { service.routeSkipNext() },
        VoiceCommand(R.string.route_detail_action_previous)       { service.routeSkipPrevious() },
        VoiceCommand(R.string.beacon_action_mute_beacon)          { service.routeMute() },
        VoiceCommand(R.string.route_detail_action_stop_route)     { service.routeStop() },
        VoiceCommand(R.string.voice_cmd_list_routes)              { service.routeListRoutes() },
        VoiceCommand(R.string.route_detail_action_start_route)    {
            // TODO: We need a "fuzzy remove" here
            val routeName = it.removePrefix(context.getString(R.string.route_detail_action_start_route).lowercase()).trim()
            service.routeStartByName(routeName)
        },
        VoiceCommand(R.string.voice_cmd_list_markers)              { service.routeListMarkers() },
        VoiceCommand(R.string.voice_cmd_start_beacon_at_marker)    {
            // TODO: We need a "fuzzy remove" here
            val markerName = it.removePrefix(context.getString(R.string.location_detail_action_beacon).lowercase()).trim()
            service.markerStartByName(markerName)
        },
        VoiceCommand(R.string.menu_help)                          { voiceHelp() },
    )

    private fun handleSpeech(speech: ArrayList<String>) {

        // TODO: Start by only looking at the very first string for a match. We should check the
        //  other strings too.

        // Find the best match to the speech.
        val t = speech.first().lowercase()

        var minMatch = Double.MAX_VALUE
        var bestMatch: VoiceCommand? = null
        for (command in commands) {
            val commandString = context.getString(command.stringId).lowercase()
            val match = commandString.fuzzyCompare(t, true)
            if ((match < 0.3) && (match < minMatch)) {
                minMatch = match
                bestMatch = command
            }
        }
        if (bestMatch != null) {
            println("Found command: ${context.getString(bestMatch.stringId)}")
            bestMatch.action(t)
        } else {
            service.speak2dText(
                context.getString(R.string.voice_cmd_not_recognized).format(t),
                false,
                EARCON_CALLOUTS_OFF
            )
        }
    }

    private fun voiceHelp() {
        val commandNames = commands.map { context.getString(it.stringId) }
        val text =
            context.getString(R.string.voice_cmd_help_response) + commandNames.joinToString(", ")
        service.speak2dText(text)
    }
}
