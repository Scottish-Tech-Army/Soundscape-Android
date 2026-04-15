package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class PointAndDistanceAndHeading(var point: LngLatAlt = LngLatAlt(),
                                      var distance: Double = Double.MAX_VALUE,
                                      var heading: Double = 0.0,
                                      var index: Int = -1,
                                      var positionAlongLine: Double = Double.NaN)


fun PointAndDistanceAndHeading.clone(): PointAndDistanceAndHeading {
    return PointAndDistanceAndHeading(point.clone(), distance, heading, index, positionAlongLine)
}
