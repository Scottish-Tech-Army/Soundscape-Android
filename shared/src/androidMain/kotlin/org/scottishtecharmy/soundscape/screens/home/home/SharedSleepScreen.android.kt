package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.AndroidLocationProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreenState.Sleeping
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreenState.Snoozing

actual fun provideSleepScreenViewModel(
    locationProvider: LocationProvider,
    coroutineScope: CoroutineScope,
): ISleepScreenViewModel {
    return SleepScreenViewModel(locationProvider, coroutineScope)
}

fun SoundscapeLocation.asLngLatAlt(): LngLatAlt {
    return LngLatAlt(
        longitude = longitude,
        latitude = latitude,
    )
}

class SleepScreenViewModel(
    private val locationProvider: LocationProvider,
    private val coroutineScope: CoroutineScope,
) : ViewModel(),
    ISleepScreenViewModel {
    private val _state: MutableStateFlow<SleepScreenState> =
        MutableStateFlow(Sleeping)
    override val state: StateFlow<SleepScreenState> = _state.asStateFlow()

    private val _location: MutableStateFlow<LngLatAlt> = MutableStateFlow(LngLatAlt())

    private lateinit var _locationJob: Job

    override fun onWakeOnLeaveClicked() {
        _state.update {
            when (it) {
                is Sleeping -> {
                    subscribeToLocation()
                    Snoozing(_location.value)
                }

                is Snoozing -> {
                    unsubscribeFromLocation()
                    Sleeping
                }
            }
        }
    }

    private fun unsubscribeFromLocation() {
        if (this::_locationJob.isInitialized) {
            _locationJob.cancel()
        }
        _state.update { Sleeping }
    }

    private fun subscribeToLocation() {
        _locationJob = coroutineScope.launch {
            locationProvider.locationFlow.collect { loc ->
                if (isActive) {
                    val lngLat = loc?.asLngLatAlt() ?: LngLatAlt()
                    _location.update { lngLat }
                    _state.update {
                        if (it is Snoozing) it.copy(userLocation = lngLat) else it
                    }
                }
            }
        }
    }
}

@Composable
actual fun provideLocationProvider(): LocationProvider {
    val context = LocalContext.current.applicationContext
    return remember {
        AndroidLocationProvider(context).apply {
            start(context)
        }
    }
}
