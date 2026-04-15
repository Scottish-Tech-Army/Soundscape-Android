package org.scottishtecharmy.soundscape.geojsonparser.geojson

open class LineString() : MultiPoint() {
    init {
        type = "LineString"
    }

    constructor(vararg points: LngLatAlt) : this() {
        this.coordinates = arrayListOf(*points)
    }

    constructor(points: ArrayList<LngLatAlt>) : this() {
        this.coordinates = points
    }
}
