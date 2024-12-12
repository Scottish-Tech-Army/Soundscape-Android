package org.scottishtecharmy.soundscape.locationprovider

import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class StaticLocationProvider(private var latitude: Double, private var longitude: Double) :
    LocationProvider() {

    override fun destroy() {
    }

    override fun start(context : Context){
        // Simply set our flow source as the passed in location with 10m accuracy so that it's not ignored
        val location = Location(LocationManager.PASSIVE_PROVIDER)
        location.latitude = latitude
        location.longitude = longitude
        location.accuracy = 10.0F
        mutableLocationFlow.value = location
    }

    override fun updateLocation(newLocation: LngLatAlt, speed: Float) {
        val location = Location(LocationManager.PASSIVE_PROVIDER)
        location.latitude = newLocation.latitude
        location.longitude = newLocation.longitude
        location.speed = speed
        location.accuracy = 10.0F
        mutableLocationFlow.value = location
    }

    companion object {
        private const val TAG = "StaticLocationProvider"
    }
}