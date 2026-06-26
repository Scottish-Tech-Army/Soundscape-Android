package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.content.Context
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

open class SoundscapeGeocoder {
    open suspend fun getAddressFromLocationName(locationName: String, nearbyLocation: LngLatAlt, localizedContext: Context?) : List<LocationDescription>? { return null }
    open suspend fun getAddressFromLngLat(userGeometry: UserGeometry, localizedContext: Context?, ignoreHouseNumbers: Boolean) : LocationDescription? { return null }
}
