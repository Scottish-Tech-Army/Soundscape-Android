package org.scottishtecharmy.soundscape.geojsonparser.geojson

import com.squareup.moshi.Json

/**
 * Each element in the "geometries" array of a GeometryCollection is one
 * of the Geometry objects LineString, MultiLineString, Polygon, etc:
 *
 *      {
 *          "type": "GeometryCollection",
 *          "geometries": [{
 *              "type": "Point",
 *              "coordinates": [100.0, 0.0]
 *          }, {
 *              "type": "LineString",
 *              "coordinates": [
 *                  [101.0, 0.0],
 *                  [102.0, 1.0]
 *              ]
 *          }]
 *      }
 *     https://datatracker.ietf.org/doc/html/rfc7946#section-3.1
 */
open class GeometryCollection : GeoJsonObject(), Iterable<GeoJsonObject> {
    @field:Json(name = "geometries")
    var geometries: ArrayList<GeoJsonObject> = arrayListOf()

    init {
        type = "GeometryCollection"
    }

    override fun iterator(): Iterator<GeoJsonObject> = geometries.iterator()

    operator fun plusAssign(rhs: GeoJsonObject) {
        geometries.add(rhs)
    }
}