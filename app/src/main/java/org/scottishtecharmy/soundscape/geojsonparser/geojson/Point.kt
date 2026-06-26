package org.scottishtecharmy.soundscape.geojsonparser.geojson

import com.squareup.moshi.Json

/**
 * Point coordinates are in x, y order (easting, northing for projected
 *   coordinates, longitude, and latitude for geographic coordinates):
 *
 *     {
 *         "type": "Point",
 *         "coordinates": [100.0, 0.0]
 *     }
 *     https://datatracker.ietf.org/doc/html/rfc7946#appendix-A.1
 */

open class Point() : GeoJsonObject() {
    @field:Json(name = "coordinates")
    var coordinates: LngLatAlt = LngLatAlt()

    init {
        type = "Point"
    }

    constructor(lng: Double, lat: Double, alt: Double? = null) : this() {
        coordinates = LngLatAlt(lng, lat, alt)
    }

    constructor(location: LngLatAlt) : this() {
        coordinates = location
    }
}