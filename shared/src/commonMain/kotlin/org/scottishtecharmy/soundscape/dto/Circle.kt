package org.scottishtecharmy.soundscape.dto

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class Circle(
    var center: LngLatAlt = LngLatAlt(0.0, 0.0),
    var radius: Double = 0.0
)
