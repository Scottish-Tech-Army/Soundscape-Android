package org.scottishtecharmy.soundscape.dto

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.isBetween

/**
 * Bounding box usually follow the standard format of:
 *
 * bbox = left,bottom,right,top
 * bbox = min Longitude , min Latitude , max Longitude , max Latitude
 *     https://wiki.openstreetmap.org/wiki/Bounding_box
 */
data class BoundingBox(
    var westLongitude: Double = 0.0,
    var southLatitude: Double = 0.0,
    var eastLongitude: Double = 0.0,
    var northLatitude: Double = 0.0
)
