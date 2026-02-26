package org.scottishtecharmy.soundscape.services

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
import org.scottishtecharmy.soundscape.utils.getCurrentLocale

sealed class VoiceCommandState {
    object Idle : VoiceCommandState()
    object Listening : VoiceCommandState()
    object Error : VoiceCommandState()
}

enum class VoiceCommand {
    MY_LOCATION, AROUND_ME, AHEAD_OF_ME, NEARBY_MARKERS,
    SKIP_NEXT, SKIP_PREVIOUS, MUTE, STOP_ROUTE, UNKNOWN
}

class VoiceCommandManager(
    // Mutable so SoundscapeService can push localizedContext once it's created.
    // The service context is used for SpeechRecognizer binding;
    // the localized context is used for string-resource keyword lookups.
    private var context: Context,
    private val onCommand: (VoiceCommand) -> Unit,
    private val onError: () -> Unit
) {

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
        if (!SpeechRecognizer.isRecognitionAvailable(context)) { onError(); return }
        destroyRecognizer()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
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
            onCommand(parseCommand(matches?.firstOrNull() ?: ""))
        }

        override fun onError(error: Int) {
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

    private fun parseCommand(text: String): VoiceCommand {
        val t = text.lowercase()

        // Match against localized keyword lists stored in string resources.
        // Each resource is a comma-separated list of phrases a user might say.
        fun matches(resId: Int) =
            context.getString(resId).split(",").any { t.contains(it.trim()) }

        return when {
            matches(R.string.voice_cmd_my_location)              -> VoiceCommand.MY_LOCATION
            matches(R.string.voice_cmd_around_me)                   -> VoiceCommand.AROUND_ME
            matches(R.string.voice_cmd_ahead_of_me)                   -> VoiceCommand.AHEAD_OF_ME
            matches(R.string.voice_cmd_nearby_markers)                    -> VoiceCommand.NEARBY_MARKERS
            matches(R.string.voice_cmd_skip_previous) -> VoiceCommand.SKIP_PREVIOUS
            matches(R.string.voice_cmd_skip_next)                -> VoiceCommand.SKIP_NEXT
            matches(R.string.voice_cmd_mute)                                     -> VoiceCommand.MUTE
            matches(R.string.voice_cmd_stop_route)                                  -> VoiceCommand.STOP_ROUTE
            else                                                        -> VoiceCommand.UNKNOWN
        }
    }
}
