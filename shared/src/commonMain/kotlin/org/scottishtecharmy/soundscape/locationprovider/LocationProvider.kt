package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed class Accuracy {
    abstract val updateInterval: Duration
    abstract val minimumDistanceM: Float

    object High : Accuracy() {
        override val updateInterval: Duration = 1.seconds
        override val minimumDistanceM: Float = 1.0f

    }

    object Balanced : Accuracy() {
        override val updateInterval: Duration = 30.seconds
        override val minimumDistanceM: Float = 5.0f
    }
}

abstract class LocationProvider {

    abstract fun start()
    abstract fun start(accuracy: Accuracy)
    abstract fun destroy()
    open fun updateLocation(newLocation: SoundscapeLocation) {}

    fun hasValidLocation(): Boolean {
        return mutableLocationFlow.value != null
    }

    val mutableLocationFlow = MutableStateFlow<SoundscapeLocation?>(null)
    var locationFlow: StateFlow<SoundscapeLocation?> = mutableLocationFlow

    val mutableFilteredLocationFlow = MutableStateFlow<SoundscapeLocation?>(null)
    var filteredLocationFlow: StateFlow<SoundscapeLocation?> = mutableFilteredLocationFlow
}
