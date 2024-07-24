package org.scottishtecharmy.soundscape.geojsonparser.geojson

/**
 * Coordinates of a Polygon are an array of linear ring coordinate arrays.
 * The first element in the array represents the exterior ring.
 * Any subsequent elements represent interior rings (or holes).
 *
 *    No holes:
 *
 *      {
 *          "type": "Polygon",
 *          "coordinates": [
 *              [
 *                  [100.0, 0.0],
 *                  [101.0, 0.0],
 *                  [101.0, 1.0],
 *                  [100.0, 1.0],
 *                  [100.0, 0.0]
 *              ]
 *          ]
 *      }
 *
 *    With holes:
 *
 *      {
 *          "type": "Polygon",
 *          "coordinates": [
 *              [
 *                  [100.0, 0.0],
 *                  [101.0, 0.0],
 *                  [101.0, 1.0],
 *                  [100.0, 1.0],
 *                  [100.0, 0.0]
 *              ],
 *              [
 *                  [100.8, 0.8],
 *                  [100.8, 0.2],
 *                  [100.2, 0.2],
 *                  [100.2, 0.8],
 *                  [100.8, 0.8]
 *              ]
 *          ]
 *      }
 *
 *     https://datatracker.ietf.org/doc/html/rfc7946#appendix-A.3
 */
open class Polygon() : Geometry<ArrayList<LngLatAlt>>() {
    init {
        type = "Polygon"
    }

    constructor(polygon: ArrayList<LngLatAlt>) : this() {
        this.coordinates.add(polygon)
    }

    fun getExteriorRing(): ArrayList<LngLatAlt> {
        assertExteriorRing()
        return coordinates[0]
    }

    fun getInteriorRings(): ArrayList<ArrayList<LngLatAlt>> {
        assertExteriorRing()
        return ArrayList(coordinates.subList(1, coordinates.size))
    }

    fun getInteriorRing(index: Int): ArrayList<LngLatAlt> {
        assertExteriorRing()
        return coordinates[1 + index]
    }

    fun addInteriorRing(points: ArrayList<LngLatAlt>) {
        assertExteriorRing()
        coordinates.add(points)
    }

    fun addInteriorRing(vararg points: LngLatAlt) {
        assertExteriorRing()
        coordinates.add(arrayListOf(*points))
    }

    private fun assertExteriorRing() {
        if (coordinates.isEmpty()) {
            throw RuntimeException("No exterior ring defined.")
        }
    }
}