package com.kersnazzle.soundscapealpha.dto

import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Feature

data class IntersectionRelativeDirections(
    val direction: Int,
    val feature: Feature
)
