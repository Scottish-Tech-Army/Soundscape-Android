package org.scottishtecharmy.soundscape.dto

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature

data class IntersectionRelativeDirections(
    val direction: Int,
    val feature: Feature
)
