package org.scottishtecharmy.soundscape.geojsonparser.geojson

open class MultiPoint() : Geometry<LngLatAlt>() {
    init {
        type = "MultiPoint"
    }

    constructor(vararg points: LngLatAlt) : this() {
        this.coordinates = arrayListOf(*points)
    }

    constructor(points: ArrayList<LngLatAlt>) : this() {
        this.coordinates = points
    }
}
