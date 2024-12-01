package org.scottishtecharmy.soundscape.dto

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
    var northLatitude: Double = 0.0) {
    constructor(geometry: List<Double>) :
            this(geometry[0], geometry[1], geometry[2], geometry[3])
}
