package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

open class SoundscapeGeocoder {
    open suspend fun getAddressFromLocationName(
        locationName: String,
        nearbyLocation: LngLatAlt,
        localizedStrings: LocalizedStrings?
    ): List<LocationDescription>? = null

    open suspend fun getAddressFromLngLat(
        userGeometry: UserGeometry,
        localizedStrings: LocalizedStrings?,
        ignoreHouseNumbers: Boolean
    ): LocationDescription? = null
}
