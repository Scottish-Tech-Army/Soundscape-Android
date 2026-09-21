package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class DirectionProvider {
    open fun destroy() {}

    val mutableOrientationFlow = MutableStateFlow<DeviceDirection?>(null)
    var orientationFlow: StateFlow<DeviceDirection?> = mutableOrientationFlow
}
