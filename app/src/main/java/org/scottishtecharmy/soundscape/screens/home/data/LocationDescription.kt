package org.scottishtecharmy.soundscape.screens.home.data

import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class LocationDescription(
    var name: String = "",
    var location: LngLatAlt,
    var opposite: Boolean = false,
    var description: String? = null,
    var source: LocationSource = LocationSource.UnknownSource,
    var orderId: Long = 0L,
    var databaseId: Long = 0
)
