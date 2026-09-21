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
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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

class SleepScreenViewModel(
    private val locationProvider: LocationProvider,
    private val onWakeUp: () -> Unit,
) : ViewModel() {
    private val _state: MutableStateFlow<SleepScreenState> =
        MutableStateFlow(SleepScreenState.Sleeping)
    val state: StateFlow<SleepScreenState> = _state.asStateFlow()

    private val _location: MutableStateFlow<LngLatAlt> = MutableStateFlow(LngLatAlt())
    private val _events: MutableSharedFlow<SleepScreenEvent> = MutableSharedFlow()

    // Rendezvous Channel to handle navigation
    val effects: Channel<SleepScreenEffect> = Channel(
        capacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    private lateinit var _locationJob: Job
    private val _eventsJob: Job
    private lateinit var _geofenceJob: Job
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
    }

    fun onWakeUpNowClicked() {
        destroy()
    }

    fun onWakeOnLeaveClicked() {
        when (_state.value) {
            is SleepScreenState.Sleeping -> {
                subscribeToLocation()
                _state.update {
                    SleepScreenState.Snoozing(
                        userLocation = _location.value,
                    )
                }
            }

            is SleepScreenState.Snoozing -> {
                unsubscribeFromLocation()
                _state.update { SleepScreenState.Sleeping }
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
        if (this::_locationJob.isInitialized && _locationJob.isActive) {
            _locationJob.cancel()
        }
        if (this::_geofenceJob.isInitialized && _geofenceJob.isActive) {
            _geofenceJob.cancel()
        }
        locationProvider.destroy()
    }

    private fun subscribeToLocation() {
        // Start location job to update internal location sharedflow
        if (!this::_locationJob.isInitialized) {
            _locationJob = viewModelScope.launch {
                locationProvider.start(Accuracy.Balanced)
                locationProvider.locationFlow.collect { loc ->
                    if (isActive) {
                        val lngLat = loc?.asLngLatAlt() ?: LngLatAlt()
                        _location.update { lngLat }
                    }
                }
            }
        }

        // Start geofence job to collect from location job. On collection of value, update the state with:
        // if first emission: the start location
        // subsequent emissions update the distance from the start location
        if (!this::_geofenceJob.isInitialized) {
            _geofenceJob = viewModelScope.launch {
                println("Geofencing job started: $state")
                val ruler = CheapRuler(0.0)
                _location.collectIndexed { index, value ->
                    var state = _state.value

                    println("Geofencing job new location. Current State: $state")

                    when (state) {
                        is SleepScreenState.Snoozing -> {
                            state = if (index == 0) {
                                state.copy(
                                    userLocation = value,
                                    startLocation = value,
                                    geofenceDistance = ruler.distance(value, value)
                                )
                            } else {
                                state.copy(
                                    userLocation = value,
                                    geofenceDistance = ruler.distance(
                                        state.userLocation,
                                        state.startLocation ?: value
                                    )
                                )
                            }

                            // Check if the geofence distance is greater than 12 metres and if so, end the
                            // session
                            println("Geofencing job. Distance: ${state.geofenceDistance}")
                            if (state.geofenceDistance > 12.0) {
                                destroy()
                            }

                            _state.value = state

                            println("Geofencing job new location. Updated State: ${_state.value}")
                        }

                        SleepScreenState.Sleeping -> {
                            // no-op
                        }
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
            style = MaterialTheme.typography.displaySmall,
        )
    }
}
