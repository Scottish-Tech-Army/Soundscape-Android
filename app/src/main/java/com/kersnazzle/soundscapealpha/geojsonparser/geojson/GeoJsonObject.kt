package com.kersnazzle.soundscapealpha.geojsonparser.geojson

import com.squareup.moshi.Json
import java.io.Serializable
import java.util.HashMap

/**
 * A GeoJSON object represents a Geometry, Feature, or collection of
 *    Features.
 *
 *    o  A GeoJSON object is a JSON object.
 *
 *    o  A GeoJSON object has a member with the name "type".  The value of
 *       the member MUST be one of the GeoJSON types.
 *
 *    o  A GeoJSON object MAY have a "bbox" member, the value of which MUST
 *       be a bounding box array
 *     https://datatracker.ietf.org/doc/html/rfc7946#section-3
 */
open class GeoJsonObject : Serializable {
    @field:Json(name = "type")
    var type: String = "GeoJson"

    @field:Json(name = "bbox")
    var bbox: List<Double>? = null

    @field:Json(name = "properties")
    var properties: HashMap<String, Any?>? = null

    /**
     * In the (abridged) Feature object shown below
     *
     *    {
     *        "type": "Feature",
     *        "id": "f1",
     *        "geometry": {...},
     *        "properties": {...},
     *        "title": "Example Feature"
     *    }
     *
     * the name/value pair of "title": "Example Feature" is a foreign
     * member.
     *  https://datatracker.ietf.org/doc/html/rfc7946#section-6
     */
    var foreign: HashMap<String, Any?>? = null
}