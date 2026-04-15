package org.scottishtecharmy.soundscape.geojsonparser.geojson

/**
 * A Geometry object represents points, curves, and surfaces in
 *    coordinate space.
 *     https://datatracker.ietf.org/doc/html/rfc7946#section-3.1
 */
abstract class Geometry<T>() : GeoJsonObject() {
    var coordinates: ArrayList<T> = arrayListOf()

    init {
        type = "Geometry"
    }

    constructor(vararg elements: T) : this() {
        elements.forEach { coordinates.add(it) }
    }
}
