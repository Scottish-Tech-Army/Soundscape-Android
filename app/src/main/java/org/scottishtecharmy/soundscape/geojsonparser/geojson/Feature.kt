package org.scottishtecharmy.soundscape.geojsonparser.geojson

import com.squareup.moshi.Json

/**
 * A Feature object represents a spatially bounded thing.  Every Feature
 * object is a GeoJSON object no matter where it occurs in a GeoJSON
 * text.
 *
 *    o  A Feature object has a "type" member with the value "Feature".
 *    o  A Feature object has a member with the name "geometry".  The value
 *       of the geometry member SHALL be either a Geometry object as
 *       defined above or, in the case that the Feature is unlocated, a
 *       JSON null value.
 *    o  A Feature object has a member with the name "properties".  The
 *       value of the properties member is an object (any JSON object or a
 *       JSON null value).
 *    o  If a Feature has a commonly used identifier, that identifier
 *       SHOULD be included as a member of the Feature object with the name
 *       "id", and the value of this member is either a JSON string or
 *       number.
 *     https://datatracker.ietf.org/doc/html/rfc7946#section-3.2
 */
open class Feature() : GeoJsonObject() {
    @field:Json(name = "geometry")
    var geometry: GeoJsonObject = GeoJsonObject()

    @field:Json(name = "id")
    var id: String? = null

    init {
        type = "Feature"
    }
}