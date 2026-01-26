package org.scottishtecharmy.soundscape.screens.home.data

import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

enum class LocationType {
    StreetNumber,
    Street,
    City,
    Country
}

data class LocationDescription(
    var name: String = "",
    var location: LngLatAlt,
    var opposite: Boolean = false,
    var locationType : LocationType = LocationType.Country,
    var description: String? = null,
    var source: LocationSource = LocationSource.UnknownSource,
    var orderId: Long = 0L,
    var databaseId: Long = 0,

    // Deferred properties
    var feature: Feature? = null,
    var alternateLocation: LngLatAlt? = null,
    var featureName: String? = null
)
