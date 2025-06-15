package org.scottishtecharmy.soundscape.screens.home.data

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class LocationDescription(
    var name: String = "",
    var location: LngLatAlt,
    var description: String? = null,
    var databaseId: Long = 0
)
