package org.scottishtecharmy.soundscape.locationprovider

import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class StaticLocationProvider(private var location: LngLatAlt) :
    LocationProvider() {

    override fun destroy() {
    }

    override fun start(context : Context){
        // Simply set our flow source as the passed in location with 0.0m accuracy so that it's not ignored
        val passiveLocation = Location(LocationManager.PASSIVE_PROVIDER)
        passiveLocation.latitude = location.latitude
        passiveLocation.longitude = location.longitude
        passiveLocation.accuracy = 0.0F
        mutableLocationFlow.value = passiveLocation
    }

    override fun updateLocation(newLocation: LngLatAlt, heading: Float, speed: Float) {
        val passiveLocation = Location(LocationManager.PASSIVE_PROVIDER)
        passiveLocation.latitude = newLocation.latitude
        passiveLocation.longitude = newLocation.longitude
        passiveLocation.accuracy = 0.0F
        passiveLocation.bearing = heading
        passiveLocation.bearingAccuracyDegrees = 10.0F
        passiveLocation.speed = speed
        mutableLocationFlow.value = passiveLocation
    }
}