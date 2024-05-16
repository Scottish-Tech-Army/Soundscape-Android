package com.kersnazzle.soundscapealpha.geojsonparser.geojson

import com.squareup.moshi.Json
import java.util.*

/**
 * A Geometry object represents points, curves, and surfaces in
 *    coordinate space.  Every Geometry object is a GeoJSON object no
 *    matter where it occurs in a GeoJSON text.
 *
 *    o  The value of a Geometry object's "type" member MUST be one of the
 *       seven geometry types
 *
 *    o  A GeoJSON Geometry object of any type other than
 *       "GeometryCollection" has a member with the name "coordinates".
 *       The value of the "coordinates" member is an array.  The structure
 *       of the elements in this array is determined by the type of
 *       geometry.  GeoJSON processors MAY interpret Geometry objects with
 *       empty "coordinates" arrays as null objects.
 *     https://datatracker.ietf.org/doc/html/rfc7946#section-3.1
 */
abstract class Geometry<T>() : GeoJsonObject() {
    @field:Json(name = "coordinates")
    public var coordinates: ArrayList<T> = arrayListOf<T>()

    init {
        type = "Geometry"
    }

    constructor(vararg elements: T) : this() {
        elements.forEach { coordinates.add(it) }
    }
}