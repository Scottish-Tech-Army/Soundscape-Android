package org.scottishtecharmy.soundscape.screens.home.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

actual fun provideSleepScreenViewModel(): ISleepScreenViewModel {
    return SleepScreenViewModel()
}

class SleepScreenViewModel : ISleepScreenViewModel {
    private val _state: MutableStateFlow<SleepScreenState> = MutableStateFlow(SleepScreenState())
    override val state: StateFlow<SleepScreenState> = _state.asStateFlow()

    override fun onWakeOnLeaveClicked() {
        _state.update { it.copy(wakeOnLeaveEnabled = !_state.value.wakeOnLeaveEnabled) }
    }
}