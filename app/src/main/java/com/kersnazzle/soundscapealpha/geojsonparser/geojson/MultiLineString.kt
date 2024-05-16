package com.kersnazzle.soundscapealpha.geojsonparser.geojson

/**
 * Coordinates of a MultiLineString are an array of LineString
 *    coordinate arrays:
 *
 *      {
 *          "type": "MultiLineString",
 *          "coordinates": [
 *              [
 *                  [100.0, 0.0],
 *                  [101.0, 1.0]
 *              ],
 *              [
 *                  [102.0, 2.0],
 *                  [103.0, 3.0]
 *              ]
 *          ]
 *      }
 *  https://datatracker.ietf.org/doc/html/rfc7946#appendix-A.5
 */
open class MultiLineString() : Geometry<ArrayList<LngLatAlt>>() {
    init {
        type = "MultiLineString"
    }

    constructor(vararg line: ArrayList<LngLatAlt>) : this() {
        this.coordinates = arrayListOf(*line)
    }
}