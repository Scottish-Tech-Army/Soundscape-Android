package org.scottishtecharmy.soundscape.geojsonparser.geojson

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
open class GeoJsonObject {
    var type: String = "GeoJson"
    var bbox: List<Double>? = null
    var properties: HashMap<String, Any?>? = null
}
