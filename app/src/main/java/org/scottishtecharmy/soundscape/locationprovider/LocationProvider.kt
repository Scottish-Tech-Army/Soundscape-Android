package org.scottishtecharmy.soundscape.locationprovider

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

abstract class LocationProvider {
    abstract fun start(context : Context)
    abstract fun destroy()
    open fun updateLocation(newLocation: LngLatAlt, heading: Float, speed: Float) { }

    fun hasValidLocation(): Boolean{
        return mutableLocationFlow.value != null
    }

    // Flow to return raw Location objects
    val mutableLocationFlow = MutableStateFlow<Location?>(null)
    var locationFlow: StateFlow<Location?> = mutableLocationFlow

    val mutableFilteredLocationFlow = MutableStateFlow<Location?>(null)
    var filteredLocationFlow: StateFlow<Location?> = mutableFilteredLocationFlow
}

