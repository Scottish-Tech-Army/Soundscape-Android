package org.scottishtecharmy.soundscape.geojsonparser.geojson

open class MultiPolygon() : Geometry<ArrayList<ArrayList<LngLatAlt>>>() {
    init {
        type = "MultiPolygon"
    }

    operator fun contains(point: Point): Boolean =
        coordinates.any { it.contains(arrayListOf(point.coordinates)) }
}
