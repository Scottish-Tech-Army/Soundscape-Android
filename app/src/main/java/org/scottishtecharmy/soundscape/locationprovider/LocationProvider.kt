package org.scottishtecharmy.soundscape.locationprovider

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

abstract class LocationProvider {
    abstract fun start(context : Context)
    abstract fun destroy()
    open fun updateLocation(newLocation: LngLatAlt, speed: Float) { }

    fun getCurrentLatitude() : Double? {
        return mutableLocationFlow.value?.latitude
    }
    fun getCurrentLongitude() : Double? {
        return mutableLocationFlow.value?.longitude
    }
    fun get() : LngLatAlt {
        mutableLocationFlow.value?.let { location ->
            return LngLatAlt(location.longitude, location.latitude)
        }
        return LngLatAlt(0.0,0.0)
    }

    // Flow to return Location objects
    val mutableLocationFlow = MutableStateFlow<Location?>(null)
    var locationFlow: StateFlow<Location?> = mutableLocationFlow
}

