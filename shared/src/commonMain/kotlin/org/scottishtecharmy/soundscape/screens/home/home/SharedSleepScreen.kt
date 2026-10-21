package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.asLngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.Accuracy
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.sleep_sleeping
import org.scottishtecharmy.soundscape.resources.sleep_sleeping_message
import org.scottishtecharmy.soundscape.resources.sleep_sleeping_wake_on_leave_message
import org.scottishtecharmy.soundscape.resources.sleep_snoozing
import org.scottishtecharmy.soundscape.resources.sleep_wake_on_leave
import org.scottishtecharmy.soundscape.resources.sleep_wake_up_now
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.largePadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

sealed class SleepScreenState {
    object Sleeping : SleepScreenState()
    data class Snoozing(
        val userLocation: LngLatAlt,
        val startLocation: LngLatAlt? = null,
        val geofenceDistance: Double = 0.0,
    ) : SleepScreenState()
}

sealed class SleepScreenEvent {
    object WakeUpNowClick : SleepScreenEvent()
    object WakeOnLeaveClick : SleepScreenEvent()
}

sealed class SleepScreenEffect {
    object WakeUpNow : SleepScreenEffect()
}

/**
 * How far the user has to move from where they snoozed before wake on leave wakes the app. 40m,
 * as the legacy iOS app used: far enough that GPS drift while sitting indoors doesn't wake it.
 */
private const val WAKE_ON_LEAVE_DISTANCE_M = 40.0

class SleepScreenViewModel(
    private val locationProvider: LocationProvider,
    /**
     * Called with true when wake on leave starts and false when the user turns it off again, so
     * the platform can keep location updates coming with the phone locked (Android runs its
     * service while snoozing). Not called on waking: [onWakeUp] covers that.
     */
    private var onSnooze: (Boolean) -> Unit = {},
    /**
     * True when the platform is still snoozing from before this screen was recreated (Android
     * keeps its service snoozing across activity re-creation), so wake on leave carries on.
     */
    alreadySnoozing: Boolean = false,
    private var onWakeUp: () -> Unit,
) : ViewModel() {

    /**
     * Replaces the platform callbacks with the current screen's. The view model outlives the
     * activity when it is recreated (dark mode, language, font size...), and the callbacks it was
     * created with call into the old, destroyed activity, which ignores them - so the user could
     * never wake from a sleep that a re-creation had happened during.
     */
    fun updateCallbacks(onSnooze: (Boolean) -> Unit, onWakeUp: () -> Unit) {
        this.onSnooze = onSnooze
        this.onWakeUp = onWakeUp
    }

    private val _state: MutableStateFlow<SleepScreenState> =
        MutableStateFlow(SleepScreenState.Sleeping)
    val state: StateFlow<SleepScreenState> = _state.asStateFlow()

    // Null until the provider has a fix, so that no made-up position becomes the start location
    private val _location: MutableStateFlow<LngLatAlt?> = MutableStateFlow(null)
    private val _events: MutableSharedFlow<SleepScreenEvent> = MutableSharedFlow()

    // Rendezvous Channel to handle navigation
    val effects: Channel<SleepScreenEffect> = Channel(
        capacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    private var _locationJob: Job? = null
    private val _eventsJob: Job
    private var _geofenceJob: Job? = null
    private var _isDestroyed = false

    init {
        _eventsJob = viewModelScope.launch {
            _events.collect {
                when (it) {
                    SleepScreenEvent.WakeUpNowClick -> {
                        onWakeUpNowClicked()
                    }

                    SleepScreenEvent.WakeOnLeaveClick -> {
                        onWakeOnLeaveClicked()
                    }
                }
            }
        }
        if (alreadySnoozing) {
            // The start location is wherever the user is now, which can only be closer to
            // where they snoozed than where they are about to walk to
            _state.value = SleepScreenState.Snoozing(userLocation = LngLatAlt())
            subscribeToLocation()
        }
    }

    fun onWakeUpNowClicked() {
        destroy()
    }

    fun onWakeOnLeaveClicked() {
        when (_state.value) {
            is SleepScreenState.Sleeping -> {
                onSnooze(true)
                // Snoozing before subscribing: on the main dispatcher the jobs run straight away,
                // and the geofence ignores fixes until it is, so the provider's current fix -
                // where the user actually is - would be dropped and the next one taken as start.
                _state.update {
                    SleepScreenState.Snoozing(
                        userLocation = _location.value ?: LngLatAlt(),
                    )
                }
                subscribeToLocation()
            }

            is SleepScreenState.Snoozing -> {
                unsubscribeFromLocation()
                _state.update { SleepScreenState.Sleeping }
                onSnooze(false)
            }
        }
    }

    private fun destroy() {
        if (_isDestroyed) return
        _isDestroyed = true

        if (_eventsJob.isActive) {
            _eventsJob.cancel()
        }
        unsubscribeFromLocation()
        onWakeUp()
        viewModelScope.launch {
            effects.send(SleepScreenEffect.WakeUpNow)
        }
    }

    override fun onCleared() {
        destroy()
    }

    private fun unsubscribeFromLocation() {
        _locationJob?.cancel()
        _locationJob = null
        _geofenceJob?.cancel()
        _geofenceJob = null
        // Forget the last fix, so that snoozing again starts from wherever the user is then
        _location.value = null
        locationProvider.destroy()
    }

    private fun subscribeToLocation() {
        // Start location job to update internal location sharedflow
        if (_locationJob?.isActive != true) {
            _locationJob = viewModelScope.launch {
                locationProvider.start(Accuracy.Balanced)
                locationProvider.locationFlow.filterNotNull().collect { loc ->
                    _location.value = loc.asLngLatAlt()
                }
            }
        }

        // The first fix becomes the start location, and the session ends once a later fix is
        // further than WAKE_ON_LEAVE_DISTANCE_M from it.
        if (_geofenceJob?.isActive != true) {
            _geofenceJob = viewModelScope.launch {
                var ruler: CheapRuler? = null
                _location.filterNotNull().collect { value ->
                    val state = _state.value as? SleepScreenState.Snoozing ?: return@collect
                    val start = state.startLocation ?: value
                    // Built at the start's latitude: at 0 it overstates east-west distances
                    // everywhere else (by 1.8x in Glasgow), waking the user early.
                    val startRuler = ruler ?: CheapRuler(start.latitude).also { ruler = it }
                    val updated = state.copy(
                        userLocation = value,
                        startLocation = start,
                        geofenceDistance = startRuler.distance(value, start),
                    )
                    _state.value = updated
                    if (updated.geofenceDistance > WAKE_ON_LEAVE_DISTANCE_M) {
                        destroy()
                    }
                }
            }
        }
    }
}

@Composable
fun SharedSleepScreen(
    onWakeUpNowClicked: () -> Unit,
    onWakeOnLeaveClicked: () -> Unit,
    state: SleepScreenState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            Text(
                text = stringResource(
                    when (state) {
                        is SleepScreenState.Sleeping -> Res.string.sleep_sleeping
                        is SleepScreenState.Snoozing -> Res.string.sleep_snoozing
                    }
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.largePadding(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row {
            Text(
                text = stringResource(
                    when (state) {
                        SleepScreenState.Sleeping -> Res.string.sleep_sleeping_message
                        is SleepScreenState.Snoozing -> Res.string.sleep_sleeping_wake_on_leave_message
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.largePadding(),
            )
        }
        WakeButtons(
            wakeUpNowOnClick = {
                onWakeUpNowClicked()
            },
            wakeOnLeaveOnClick = onWakeOnLeaveClicked,
            sleepScreenState = state,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WakeButtons(
    wakeUpNowOnClick: () -> Unit,
    wakeOnLeaveOnClick: () -> Unit,
    sleepScreenState: SleepScreenState,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        WakeButton(
            text = stringResource(Res.string.sleep_wake_up_now),
            onClick = wakeUpNowOnClick,
            modifier = Modifier.fillMaxWidth(
                when (sleepScreenState) {
                    SleepScreenState.Sleeping -> 0.5f
                    is SleepScreenState.Snoozing -> 1.0f
                }
            )
        )
        if (sleepScreenState is SleepScreenState.Sleeping) {
            WakeButton(
                text = stringResource(Res.string.sleep_wake_on_leave),
                onClick = wakeOnLeaveOnClick,
                modifier = Modifier,
            )
        }
    }
}

@Composable
fun WakeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(spacing.targetSize * 4)
            .testTag("sleepWakeUpNow"),
        shape = RoundedCornerShape(spacing.tiny),
        colors = if (!LocalInspectionMode.current) currentAppButtonColors else ButtonDefaults.buttonColors(),
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
