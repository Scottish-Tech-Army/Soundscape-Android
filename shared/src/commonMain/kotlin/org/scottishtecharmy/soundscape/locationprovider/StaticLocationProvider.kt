package org.scottishtecharmy.soundscape.locationprovider

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class StaticLocationProvider(private var location: LngLatAlt) : LocationProvider() {

    override fun destroy() {}

    fun start() {
        val loc = SoundscapeLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = 0.0f,
            hasAccuracy = true,
        )
        mutableLocationFlow.value = loc
        mutableFilteredLocationFlow.value = loc
    }

    override fun updateLocation(newLocation: SoundscapeLocation) {
        mutableLocationFlow.value = newLocation
        mutableFilteredLocationFlow.value = newLocation
    }
}
