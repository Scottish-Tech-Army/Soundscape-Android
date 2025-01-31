package org.scottishtecharmy.soundscape.screens.home.data

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class LocationDescription(
    var addressName: String? = null,
    val fullAddress: String? = null,
    val country: String? = null,
    var distance: String? = null,
    var location: LngLatAlt = LngLatAlt(),
    val marker: Boolean = false
)
