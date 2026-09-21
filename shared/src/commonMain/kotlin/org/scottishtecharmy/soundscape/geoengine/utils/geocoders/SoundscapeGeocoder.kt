package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

open class SoundscapeGeocoder {
    /**
     * True for a geocoder whose [getAddressFromLocationName] is a full place search - one that
     * finds businesses and landmarks by name, not just addresses. [FusedGeocoder] gives such a
     * geocoder the search on its own instead of merging its results with Photon's.
     */
    open val providesPlaceSearch: Boolean = false

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
