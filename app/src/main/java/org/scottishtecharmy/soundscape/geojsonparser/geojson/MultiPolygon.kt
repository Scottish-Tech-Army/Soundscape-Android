package org.scottishtecharmy.soundscape.geojsonparser.geojson

/**
 * Coordinates of a MultiPolygon are an array of Polygon coordinate
 *    arrays:
 *
 *      {
 *          "type": "MultiPolygon",
 *          "coordinates": [
 *              [
 *                  [
 *                      [102.0, 2.0],
 *                      [103.0, 2.0],
 *                      [103.0, 3.0],
 *                      [102.0, 3.0],
 *                      [102.0, 2.0]
 *                  ]
 *              ],
 *              [
 *                  [
 *                      [100.0, 0.0],
 *                      [101.0, 0.0],
 *                      [101.0, 1.0],
 *                      [100.0, 1.0],
 *                      [100.0, 0.0]
 *                  ],
 *                  [
 *                      [100.2, 0.2],
 *                      [100.2, 0.8],
 *                      [100.8, 0.8],
 *                      [100.8, 0.2],
 *                      [100.2, 0.2]
 *                  ]
 *              ]
 *          ]
 *      }
 *     https://datatracker.ietf.org/doc/html/rfc7946#appendix-A.5
 */
open class MultiPolygon() : Geometry<ArrayList<ArrayList<LngLatAlt>>>() {
    init {
        type = "MultiPolygon"
    }

    operator fun contains(point: Point): Boolean =
        coordinates.any { it.contains(arrayListOf(point.coordinates)) }
}