package org.scottishtecharmy.soundscape.geojsonparser.geojson

/**
 * Coordinates of a MultiPoint are an array of positions:
 *
 *      {
 *          "type": "MultiPoint",
 *          "coordinates": [
 *              [100.0, 0.0],
 *              [101.0, 1.0]
 *          ]
 *      }
 *     https://datatracker.ietf.org/doc/html/rfc7946#appendix-A.4
 */
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