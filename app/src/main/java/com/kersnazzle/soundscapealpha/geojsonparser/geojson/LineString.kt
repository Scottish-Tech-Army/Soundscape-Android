package com.kersnazzle.soundscapealpha.geojsonparser.geojson

/**
 * Coordinates of a LineString are an array of positions
 *
 *     {
 *          "type": "LineString",
 *          "coordinates": [
 *              [100.0, 0.0],
 *              [101.0, 1.0]
 *          ]
 *      }
 *     https://datatracker.ietf.org/doc/html/rfc7946#appendix-A.2
 */
open class LineString() : MultiPoint() {
    init {
        type = "LineString"
    }

    constructor(vararg points: LngLatAlt) : this() {
        this.coordinates = arrayListOf(*points)
    }
}