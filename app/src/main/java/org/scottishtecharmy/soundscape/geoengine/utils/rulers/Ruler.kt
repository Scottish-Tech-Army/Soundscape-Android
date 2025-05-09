package org.scottishtecharmy.soundscape.geoengine.utils.rulers

import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

abstract class Ruler() {

    abstract fun distance(a: LngLatAlt, b: LngLatAlt) : Double
    abstract fun bearing(a: LngLatAlt, b: LngLatAlt) : Double
    abstract fun destination(p: LngLatAlt, dist: Double, bearing: Double) : LngLatAlt
    abstract fun along(line: LineString, dist: Double) : LngLatAlt
    abstract fun pointToSegmentDistance(p: LngLatAlt, a: LngLatAlt, b: LngLatAlt) : Double
    abstract fun distanceToLineString(p: LngLatAlt, line: LineString) : PointAndDistanceAndHeading
}
