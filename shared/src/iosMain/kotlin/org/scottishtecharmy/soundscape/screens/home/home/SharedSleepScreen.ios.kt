package org.scottishtecharmy.soundscape.screens.home.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreenState.Sleeping
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreenState.Snoozing

actual fun provideSleepScreenViewModel(): ISleepScreenViewModel {
    return SleepScreenViewModel()
}

class SleepScreenViewModel : ISleepScreenViewModel {
    private val _state: MutableStateFlow<SleepScreenState> =
        MutableStateFlow(Sleeping)
    override val state: StateFlow<SleepScreenState> = _state.asStateFlow()

    override fun onWakeOnLeaveClicked() {
        _state.update {
            when (it) {
                is Sleeping -> Snoozing(
                    userLocation = LngLatAlt()
                )

                is Snoozing -> Sleeping
            }
        }
    }
}