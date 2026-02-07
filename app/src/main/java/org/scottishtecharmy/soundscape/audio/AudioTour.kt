package org.scottishtecharmy.soundscape.audio

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import javax.inject.Inject

data class AudioTourInstruction(
    val text: String
)

enum class AudioTourStep {
    NOT_STARTED,
    WELCOME,
    SELECT_PLACE,
    CREATE_MARKER_STARTED,
    CREATE_MARKER_DONE,
    MARKERS_AND_ROUTES,
    MARKERS,
    START_BEACON,
    BEACON_DEMO,
    BEACON_DEMO_LOCKED,
    STOP_BEACON,
    MY_LOCATION_PROMPT,
    MY_LOCATION_WAIT,
    AROUND_ME_PROMPT,
    AROUND_ME_WAIT,
    AHEAD_PROMPT,
    AHEAD_WAIT,
    NEARBY_MARKERS_PROMPT,
    NEARBY_MARKERS_WAIT,
    FINISH,
    CANCEL
}

enum class TourButton {
    MY_LOCATION,
    AROUND_ME,
    AHEAD_OF_ME,
    NEARBY_MARKERS
}

@ActivityRetainedScoped
class AudioTour @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val serviceConnection: SoundscapeServiceConnection
) {
    private val _currentStep = MutableStateFlow(AudioTourStep.NOT_STARTED)

    private val _currentInstruction = MutableStateFlow<AudioTourInstruction?>(null)
    val currentInstruction: StateFlow<AudioTourInstruction?> = _currentInstruction.asStateFlow()

    // Track the pending step to advance to after dialog acknowledgment
    private var pendingStepAfterAcknowledgment: AudioTourStep? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    fun isRunning() : Boolean {
        return (_currentStep.value != AudioTourStep.NOT_STARTED)
    }

    fun toggleState() {
        if(_currentStep.value == AudioTourStep.NOT_STARTED)
            start()
        else
            stop()
    }

    fun start() {
        Log.d(TAG, "Starting audio tour")
        _currentStep.value = AudioTourStep.WELCOME
        showTourInstruction(context.getString(R.string.tour_welcome))
    }

    fun onInstructionAcknowledged() {
        Log.d(TAG, "Instruction acknowledged, current step: ${_currentStep.value}")
        _currentInstruction.value = null

        when (_currentStep.value) {
            AudioTourStep.BEACON_DEMO -> {
                // Start delay timer after dialog dismissal
                coroutineScope.launch {
                    delay(5000)
                    advanceToStep(AudioTourStep.BEACON_DEMO_LOCKED)
                }
            }
            AudioTourStep.BEACON_DEMO_LOCKED -> {
                // Start delay timer after dialog dismissal
                coroutineScope.launch {
                    delay(5000)
                    advanceToStep(AudioTourStep.STOP_BEACON)
                }
            }
            AudioTourStep.FINISH, AudioTourStep.CANCEL -> {
                // Reset to NOT_STARTED after dialog dismissal
                _currentStep.value = AudioTourStep.NOT_STARTED
            }
            else -> {
                // For other steps, advance to pending step if set
                pendingStepAfterAcknowledgment?.let { step ->
                    pendingStepAfterAcknowledgment = null
                    _currentStep.value = step
                }
            }
        }
    }

    private fun showTourInstruction(text: String) {
        Log.d(TAG, "Showing instruction: $text")
        _currentInstruction.value = AudioTourInstruction(text)
    }

    fun stop() {
        Log.d(TAG, "Stopping audio tour")
        advanceToStep(AudioTourStep.CANCEL)
    }

    fun onButtonPressed(button: TourButton) {
        Log.d(TAG, "Button pressed: $button, current step: ${_currentStep.value}")

        when (_currentStep.value) {
            AudioTourStep.MY_LOCATION_WAIT -> {
                coroutineScope.launch {
                    waitForAudioComplete()
                    advanceToStep(AudioTourStep.AROUND_ME_PROMPT)
                }
            }
            AudioTourStep.AROUND_ME_WAIT -> {
                if (button == TourButton.AROUND_ME) {
                    coroutineScope.launch {
                        waitForAudioComplete()
                        advanceToStep(AudioTourStep.AHEAD_PROMPT)
                    }
                }
            }
            AudioTourStep.AHEAD_WAIT -> {
                if (button == TourButton.AHEAD_OF_ME) {
                    coroutineScope.launch {
                        waitForAudioComplete()
                        advanceToStep(AudioTourStep.NEARBY_MARKERS_PROMPT)
                    }
                }
            }
            AudioTourStep.NEARBY_MARKERS_WAIT -> {
                if (button == TourButton.NEARBY_MARKERS) {
                    coroutineScope.launch {
                        waitForAudioComplete()
                        advanceToStep(AudioTourStep.FINISH)
                    }
                }
            }
            else -> { /* Ignore button presses in other states */ }
        }
    }

    fun onNavigatedToPlacesNearby() {
        Log.d(TAG, "Navigated to Places Nearby, current step: ${_currentStep.value}")
        if (_currentStep.value == AudioTourStep.WELCOME) {
            advanceToStep(AudioTourStep.SELECT_PLACE)
        }
    }

    fun onPlaceSelected() {
        Log.d(TAG, "Place selected, current step: ${_currentStep.value}")
        if (_currentStep.value == AudioTourStep.SELECT_PLACE) {
            advanceToStep(AudioTourStep.CREATE_MARKER_STARTED)
        }
    }

    fun onMarkerCreateStarted() {
        Log.d(TAG, "Marker create started, current step: ${_currentStep.value}")
        if (_currentStep.value == AudioTourStep.CREATE_MARKER_STARTED) {
            advanceToStep(AudioTourStep.CREATE_MARKER_DONE)
        }
    }

    fun onMarkerCreateDone() {
        Log.d(TAG, "Marker create done, current step: ${_currentStep.value}")
        if (_currentStep.value == AudioTourStep.CREATE_MARKER_DONE) {
            advanceToStep(AudioTourStep.MARKERS_AND_ROUTES)
        }
    }

    fun onMarkerAndRoutes() {
        Log.d(TAG, "Marker and routes, current step: ${_currentStep.value}")
        if (_currentStep.value == AudioTourStep.MARKERS_AND_ROUTES) {
            advanceToStep(AudioTourStep.MARKERS)
        }
    }

    fun onMarkers() {
        Log.d(TAG, "Marker and routes, current step: ${_currentStep.value}")
        if (_currentStep.value == AudioTourStep.MARKERS) {
            advanceToStep(AudioTourStep.START_BEACON)
        }
    }

    fun onBeaconStarted() {
        Log.d(TAG, "Beacon started, current step: ${_currentStep.value}")
        if (_currentStep.value == AudioTourStep.START_BEACON) {
            advanceToStep(AudioTourStep.BEACON_DEMO)
        }
    }

    fun onBeaconStopped() {
        Log.d(TAG, "Beacon stopped, current step: ${_currentStep.value}")
        if (_currentStep.value == AudioTourStep.STOP_BEACON) {
            advanceToStep(AudioTourStep.MY_LOCATION_PROMPT)
        }
    }

    private fun advanceToStep(step: AudioTourStep) {
        Log.d(TAG, "Advancing to step: $step")
        if(_currentStep.value == AudioTourStep.NOT_STARTED)
            return

        _currentStep.value = step
        when (step) {
            AudioTourStep.MY_LOCATION_PROMPT -> {
                showTourInstruction(context.getString(R.string.tour_my_location))
                pendingStepAfterAcknowledgment = AudioTourStep.MY_LOCATION_WAIT
            }
            AudioTourStep.AROUND_ME_PROMPT -> {
                showTourInstruction(context.getString(R.string.tour_around_me))
                pendingStepAfterAcknowledgment = AudioTourStep.AROUND_ME_WAIT
            }
            AudioTourStep.AHEAD_PROMPT -> {
                showTourInstruction(context.getString(R.string.tour_ahead))
                pendingStepAfterAcknowledgment = AudioTourStep.AHEAD_WAIT
            }
            AudioTourStep.NEARBY_MARKERS_PROMPT -> {
                showTourInstruction(context.getString(R.string.tour_nearby_markers))
                pendingStepAfterAcknowledgment = AudioTourStep.NEARBY_MARKERS_WAIT
            }
            AudioTourStep.SELECT_PLACE -> {
                showTourInstruction(context.getString(R.string.tour_select_place))
            }
            AudioTourStep.CREATE_MARKER_STARTED -> {
                showTourInstruction(context.getString(R.string.tour_create_marker_started))
            }
            AudioTourStep.CREATE_MARKER_DONE -> {
                showTourInstruction(context.getString(R.string.tour_create_marker_done))
            }
            AudioTourStep.MARKERS_AND_ROUTES -> {
                showTourInstruction(context.getString(R.string.tour_markers_and_routes))
            }
            AudioTourStep.MARKERS -> {
                showTourInstruction(context.getString(R.string.tour_markers))
            }
            AudioTourStep.START_BEACON -> {
                showTourInstruction(context.getString(R.string.tour_start_beacon))
            }
            AudioTourStep.BEACON_DEMO -> {
                // Show dialog, delay timer starts in onInstructionAcknowledged()
                showTourInstruction(context.getString(R.string.tour_beacon_demo))
            }
            AudioTourStep.BEACON_DEMO_LOCKED -> {
                // Show dialog, delay timer starts in onInstructionAcknowledged()
                showTourInstruction(context.getString(R.string.tour_beacon_demo_locked))
            }
            AudioTourStep.STOP_BEACON -> {
                showTourInstruction(context.getString(R.string.tour_stop_beacon))
            }
            AudioTourStep.FINISH -> {
                // Show dialog, reset to NOT_STARTED in onInstructionAcknowledged()
                showTourInstruction(context.getString(R.string.tour_finish))
            }
            AudioTourStep.CANCEL -> {
                serviceConnection.soundscapeService?.audioEngine?.clearTextToSpeechQueue()
                // Show dialog, reset to NOT_STARTED in onInstructionAcknowledged()
                showTourInstruction(context.getString(R.string.tour_cancel))
            }
            else -> { /* No action needed */ }
        }
    }

    private suspend fun waitForAudioComplete() {
        while(true) {
            // Wait for the audio queue to empty, debounce and then check it's still not busy
            while ((serviceConnection.soundscapeService?.isAudioEngineBusy() == true)) {
                delay(100)
            }
            delay(1000)
            if (serviceConnection.soundscapeService?.isAudioEngineBusy() == false) break
        }
    }

    companion object {
        private const val TAG = "AudioTour"
    }
}
