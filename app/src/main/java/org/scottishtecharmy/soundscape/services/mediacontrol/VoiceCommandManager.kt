package org.scottishtecharmy.soundscape.services.mediacontrol

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine.Companion.EARCON_CALLOUTS_OFF
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
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
    @Volatile private var listOfRoutes: List<RouteEntity> = emptyList()
    @Volatile private var listOfMarkers: List<MarkerEntity> = emptyList()

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val biasingStrings = ArrayList<String>()
                simpleCommands.forEach { biasingStrings.add(context.getString(it.stringId)) }
                val markers = listOfMarkers
                val routes = listOfRoutes
                for(marker in markers)
                    biasingStrings.add(context.getString(R.string.voice_cmd_start_beacon_at_marker_with_name).format(marker.name))
                for(route in routes)
                    biasingStrings.add(context.getString(R.string.voice_cmd_start_route).format(route.name))

                putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, biasingStrings)
            }
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

    data class VoiceCommand(val stringId: Int, val action: (arg: ArrayList<String>) -> Unit)

    private fun getArgument(speech: ArrayList<String>, commandString: String) : String {
        return ""
    }

    val simpleCommands = arrayOf(
        VoiceCommand(R.string.directions_my_location) { service.myLocation() },
        VoiceCommand(R.string.help_orient_page_title)               { service.whatsAroundMe() },
        VoiceCommand(R.string.help_explore_page_title)             { service.aheadOfMe() },
        VoiceCommand(R.string.callouts_nearby_markers)          { service.nearbyMarkers() },
        VoiceCommand(R.string.route_detail_action_next)           { service.routeSkipNext() },
        VoiceCommand(R.string.route_detail_action_previous)       { service.routeSkipPrevious() },
        VoiceCommand(R.string.beacon_action_mute_beacon)             { service.routeMute() },
        VoiceCommand(R.string.route_detail_action_stop_route)              { service.routeStop() },
        VoiceCommand(R.string.voice_cmd_list_routes)             { service.routeListRoutes() },
        VoiceCommand(R.string.voice_cmd_list_markers)            { service.routeListMarkers() },
        VoiceCommand(R.string.menu_help)                    { voiceHelp() },
    )

    fun matchDynamicMarkers(speech: String): Boolean{
        val markers = listOfMarkers
        val routes = listOfRoutes

        var minMatch = Double.MAX_VALUE

        // Markers
        var bestMarker : MarkerEntity? = null
        for(marker in markers) {
            val commandString = context.getString(R.string.voice_cmd_start_beacon_at_marker_with_name).format(marker.name).lowercase()
            println("Marker compare \"$commandString\" with \"$speech\"")
            val match = commandString.fuzzyCompare(speech, false)
            if ((match < 0.2) && (match < minMatch)) {
                minMatch = match
                bestMarker = marker
            }
        }
        if(bestMarker != null) {
            service.markerStart(bestMarker)
            return true
        }

        // Routes
        var bestRoute : RouteEntity? = null
        for(route in routes) {
            val commandString = context.getString(R.string.voice_cmd_start_route).format(route.name).lowercase()
            println("Route compare \"$commandString\" with \"$speech\"")
            val match = commandString.fuzzyCompare(speech, false)
            if ((match < 0.2) && (match < minMatch)) {
                minMatch = match
                bestRoute = route
            }
        }
        if(bestRoute != null) {
            service.routeStart(bestRoute)
            return true
        }

        return false
    }

    private fun handleSpeech(speech: ArrayList<String>) {
        var bestMatch: VoiceCommand? = null
        for(text in speech) {

            // Find the best match to the speech.
            val t = text.lowercase()

            var minMatch = Double.MAX_VALUE
            // Start with simpleCommands which don't contain any dynamic arguments
            for (command in simpleCommands) {
                val commandString = context.getString(command.stringId).lowercase()
                val match = commandString.fuzzyCompare(t, false)
                if ((match < 0.3) && (match < minMatch)) {
                    minMatch = match
                    bestMatch = command
                }
            }
            if(bestMatch != null)
                break
            println("No simple command match found")

            // Check if we match any of our dynamic markers
            if(matchDynamicMarkers(t))
                return
        }
        if (bestMatch != null) {
            println("Found command: ${context.getString(bestMatch.stringId)}")

            // Pass in all the speech strings, it may be that the argument is clearer in ones other
            // than our best match.
            bestMatch.action(speech)
        } else {
            service.speak2dText(
                context.getString(R.string.voice_cmd_not_recognized).format(speech.first()),
                false,
                EARCON_CALLOUTS_OFF
            )
        }
    }

    private fun voiceHelp() {

        val firstRoute = listOfRoutes.firstOrNull()?.name
        val firstMarker = listOfMarkers.firstOrNull()?.name

        val commandNames = simpleCommands.map { context.getString(it.stringId) }
        val builder = StringBuilder()
        builder.append(context.getString(R.string.voice_cmd_help_response))
        commandNames.forEach { builder.append(it).append(". ") }

        if((firstRoute != null) || (firstMarker != null))
            builder.append(context.getString(R.string.voice_cmd_explain_dynamic_markers))
        if(firstRoute != null)
            builder.append(context.getString(R.string.voice_cmd_start_route).format(firstRoute)).append(". ")
        if(firstMarker != null)
            builder.append(context.getString(R.string.voice_cmd_start_beacon_at_marker_with_name).format(firstMarker)).append(". ")

        service.speak2dText(builder.toString())
    }
}
