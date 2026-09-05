package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString

/**
 * Converts the result of [Ruler.distanceToLineString] into a distance in metres from the start of
 * that line.
 *
 * [PointAndDistanceAndHeading.positionAlongLine] is a *fractional vertex index* (segment index +
 * how far along that segment the point falls), not a distance, so turning it into metres means
 * walking the vertices up to that segment and adding the fraction of the final one. Shared by
 * StreetDescription (distance along a chain of Ways making up a street) and Way.distanceAlongWay
 * (position of a feature along a single Way).
 */
fun distanceAlongLineString(
    line: LineString,
    pdh: PointAndDistanceAndHeading,
    ruler: Ruler
): Double {
    if (pdh.index < 0 || pdh.positionAlongLine.isNaN()) return 0.0

    var distance = 0.0
    for (i in 0 until pdh.index) {
        distance += ruler.distance(line.coordinates[i], line.coordinates[i + 1])
    }
    distance += (pdh.positionAlongLine - pdh.index) * ruler.distance(
        line.coordinates[pdh.index],
        line.coordinates[pdh.index + 1]
    )
    return distance
}
