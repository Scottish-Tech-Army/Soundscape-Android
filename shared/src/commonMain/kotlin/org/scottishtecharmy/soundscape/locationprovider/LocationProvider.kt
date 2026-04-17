package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class LocationProvider {
    abstract fun destroy()
    open fun updateLocation(newLocation: SoundscapeLocation) { }

    fun hasValidLocation(): Boolean {
        return mutableLocationFlow.value != null
    }

    val mutableLocationFlow = MutableStateFlow<SoundscapeLocation?>(null)
    var locationFlow: StateFlow<SoundscapeLocation?> = mutableLocationFlow

    val mutableFilteredLocationFlow = MutableStateFlow<SoundscapeLocation?>(null)
    var filteredLocationFlow: StateFlow<SoundscapeLocation?> = mutableFilteredLocationFlow
}
