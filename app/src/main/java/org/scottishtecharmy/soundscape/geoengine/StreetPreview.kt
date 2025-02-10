package org.scottishtecharmy.soundscape.geoengine

import android.util.Log
import org.scottishtecharmy.soundscape.geoengine.utils.RoadDirectionAtIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.getDirectionAtIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.geoengine.utils.splitRoadAtNode
import org.scottishtecharmy.soundscape.geoengine.utils.splitRoadByIntersection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class StreetPreviewChoice(
    val heading: Double,
    val name: String,
    val route: List<LngLatAlt>
)

enum class StreetPreviewEnabled {
    OFF, INITIALIZING, ON
}
data class StreetPreviewState(
    val enabled: StreetPreviewEnabled = StreetPreviewEnabled.OFF,
    val choices: List<StreetPreviewChoice> = emptyList()
)

class StreetPreview {

    enum class PreviewState(val id: Int) {
        INITIAL(0),
        AT_NODE(1)
    }

    private var previewState = PreviewState.INITIAL
    private var previewRoad: StreetPreviewChoice? = null

    private var lastHeading = Double.NaN

    fun start() {
        previewState = PreviewState.INITIAL
    }

    fun go(userGeometry: UserGeometry, engine: GeoEngine) : LngLatAlt? {
        when (previewState) {

            PreviewState.INITIAL -> {
                // Jump to a node on the nearest road or path
                val road = engine.gridState.getNearestFeature(TreeId.ROADS_AND_PATHS, userGeometry.location, Double.POSITIVE_INFINITY)
                    ?: return null

                var nearestDistance = Double.POSITIVE_INFINITY
                var nearestPoint = LngLatAlt()
                val nearestPointOnRoad = LngLatAlt()
                val distance = userGeometry.location.distanceToLineString(
                    road.geometry as LineString,
                    nearestPointOnRoad
                )
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearestPoint = nearestPointOnRoad
                }
                if (nearestDistance != Double.POSITIVE_INFINITY) {
                    // We've got a location, so jump to it
                    engine.locationProvider.updateLocation(nearestPoint, 0.0F)
                    previewState = PreviewState.AT_NODE
                    return nearestPoint
                }
            }

            PreviewState.AT_NODE -> {
                // Find which road that we're choosing based on our current heading
                val choices = getDirectionChoices(engine, userGeometry.location)
                var bestIndex = -1
                var bestHeadingDiff = Double.POSITIVE_INFINITY

                // Find the choice with the closest heading to our own
                for ((index, choice) in choices.withIndex()) {
                    val diff = calculateHeadingOffset(choice.heading, userGeometry.heading())
                    if (diff < bestHeadingDiff) {
                        bestHeadingDiff = diff
                        bestIndex = index
                    }
                    Log.d(TAG, "Choice: ${choice.name} heading: ${choice.heading}")
                }

                // We've got a road - let's head down it
                previewRoad = extendChoice(engine, userGeometry.location, choices[bestIndex])
                previewRoad?.let { road ->
                    engine.locationProvider.updateLocation(road.route.last(), 1.0F)
                    lastHeading = bearingOfLineFromEnd(road.route.last(), road.route)
                    return road.route.last()
                }
                previewState = PreviewState.AT_NODE
            }
        }
        return null
    }

    private fun bearingOfLineFromEnd(location: LngLatAlt, line: List<LngLatAlt>): Double {
        var heading = Double.NaN
        var nextPoint: LngLatAlt? = null
        if (location == line.first()) {
            if (line.size <= 1) {
                Log.e(TAG, "bearingOfLineFromEnd: line too short")
            } else {
                nextPoint = line[1]
            }
        } else if (location == line.last()) {
            nextPoint = line.dropLast(1).last()
        }
        if (nextPoint != null) {
            heading = bearingFromTwoPoints(location, nextPoint)
        }
        return heading
    }

    /**
     * extendChoice extends the road line in the choice in the direction of travel. It  also orders
     * the points in the line so that first() is the current location and last() is one of either:
     *  1. The next intersection (most common)
     *  2. The end of the road
     *  3. The edge of the tile grid
     */
    private fun extendUntilIntersection(
        engine: GeoEngine,
        newLine: List<LngLatAlt>,
        extendedLine: MutableList<LngLatAlt>
    ): Boolean {
        var foundIntersection = false
        for ((index, point) in newLine.withIndex()) {
            extendedLine.add(point)

            if (index != 0) {
                // Check for an intersection at this point by searching in the intersection tree
                val intersection = engine.gridState.getNearestFeature(TreeId.INTERSECTIONS, point, 0.5)
                if (intersection != null) {
                    Log.e(
                        TAG,
                        "Stop at intersection ${intersection.properties?.get("name")}"
                    )
                    foundIntersection = true
                    break
                }
            }
        }
        return foundIntersection
    }

    private fun extendChoice(
        engine: GeoEngine,
        location: LngLatAlt,
        choice: StreetPreviewChoice
    ): StreetPreviewChoice {
        var line = choice.route
        if (location == line.last()) {
            line = line.reversed()
        }

        val extendedLine = mutableListOf<LngLatAlt>()

        while (true) {
            // Find any intersections in the current line segment
            val foundIntersection = extendUntilIntersection(engine, line, extendedLine)

            if (foundIntersection) {
                return StreetPreviewChoice(choice.heading, choice.name, extendedLine)
            }

            // We've not found an intersection yet, so we've reached the end of the line
            var foundNewRoad = false
            if (line.size < 2) {
                return StreetPreviewChoice(choice.heading, choice.name, extendedLine)
            }
            val currentPoint = line.last()
            val previousPoint = line.dropLast(1).last()
            val roads = engine.gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS, currentPoint, 0.5, 3)
            for (road in roads) {
                // Find which roads the currentPoint is in
                val roadPoints = road.geometry as LineString
                val containsCurrentPoint = roadPoints.coordinates.any { it == currentPoint }
                val containsPreviousPoint = roadPoints.coordinates.any { it == previousPoint }
                if (containsCurrentPoint && containsPreviousPoint) {
                    // This is our line, so ignore it
                } else if (containsCurrentPoint) {
                    // This is the next line
                    line = roadPoints.coordinates
                    if (currentPoint == line.last()) {
                        line = line.reversed()
                    }
                    foundNewRoad = true
                    break
                }
            }
            if (!foundNewRoad) {
                // Return what we have
                return StreetPreviewChoice(choice.heading, choice.name, extendedLine)
            }
        }
    }

    /**
     * getDirectionChoices returns a List of possible choices at an intersection
     * Each entry contains a heading, the street name and a list of points that make up the road.
     * The app can choose the road based on the heading and then move the user along it.
     */
    fun getDirectionChoices(engine: GeoEngine, location: LngLatAlt): List<StreetPreviewChoice> {
        val choices = mutableListOf<StreetPreviewChoice>()

        val start = System.currentTimeMillis()

        val nearestIntersection = engine.gridState.getNearestFeature(TreeId.INTERSECTIONS, location, 1.0)
        val nearestRoads = engine.gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS, location, 1.0)

        if (nearestIntersection != null) {
            // We're at an intersection
            val intersectionRoadNames = getIntersectionRoadNames(nearestIntersection, nearestRoads)
            for (road in intersectionRoadNames) {
                val testRoadDirectionAtIntersection =
                    getDirectionAtIntersection(nearestIntersection, road)

                if (testRoadDirectionAtIntersection == RoadDirectionAtIntersection.LEADING_AND_TRAILING) {
                    // split the road into two
                    val roadCoordinatesSplitIntoTwo = splitRoadByIntersection(
                        nearestIntersection,
                        road
                    )

                    for (splitRoad in roadCoordinatesSplitIntoTwo) {
                        road.properties?.get("name")?.let { choice ->
                            val line = splitRoad.geometry as LineString
                            val heading = bearingOfLineFromEnd(location, line.coordinates)
                            choices.add(
                                StreetPreviewChoice(
                                    heading,
                                    choice.toString(),
                                    line.coordinates
                                )
                            )
                        }
                    }
                } else if (testRoadDirectionAtIntersection != RoadDirectionAtIntersection.NONE) {
                    road.properties?.get("name")?.let { choice ->
                        val line = road.geometry as LineString
                        val heading = bearingOfLineFromEnd(location, line.coordinates)
                        choices.add(
                            StreetPreviewChoice(
                                heading,
                                choice.toString(),
                                line.coordinates
                            )
                        )
                    }
                }
            }
        } else {
            // There are a couple of reasons that we could be here:
            //
            //  1. We're in the middle of a road straight after starting StreetPreview
            //  2. We reached the edge of the tile grid on the last jump - the road had no
            //     intersections between the central bounding box and the grid edge, or it was slow
            //     in loading up the tile grid and we jumped along the road very fast.
            //
            // In the first case we likely have a single road within 1m that we need to split into
            // two, but in the second case we likely have 3 nearby roads, one of which is a short
            // joining road between two tiles and it's also pointing in the wrong direction! We want
            // to follow along the joining road and then use the road that it joins on to for the
            // heading.
            var currentRoad: Feature? = null
            var joiningRoadOtherEnd: LngLatAlt? = null
            var adjoiningRoads = FeatureCollection()

            // Add the current road and find out if we have a short joining road
            for (road in nearestRoads.features) {
                val lineString = road.geometry as LineString
                val containsCurrentPoint = lineString.coordinates.any { it == location }
                if (containsCurrentPoint) {
                    if (road.foreign?.containsKey("tileJoiner") != true) {
                        adjoiningRoads.addFeature(road)
                        currentRoad = road
                    } else {
                        var index = 0
                        if (lineString.coordinates[0] == location) {
                            index = 1
                        } else {
                            assert(lineString.coordinates[1] == location)
                        }
                        joiningRoadOtherEnd = lineString.coordinates[index]
                    }
                }
            }
            if (joiningRoadOtherEnd != null) {
                // Add the road at the other end of the joining road
                for (road in nearestRoads.features) {
                    val lineString = road.geometry as LineString
                    val containsOtherEnd = lineString.coordinates.any { it == joiningRoadOtherEnd }
                    if (containsOtherEnd) {
                        if (road.foreign?.containsKey("tileJoiner") != true) {
                            // This is the road at the other end of the joining road
                            adjoiningRoads.addFeature(road)
                        }
                    }
                }
            }

            if ((adjoiningRoads.features.size == 1) && (currentRoad != null)) {
                // We've only got one road, so split it
                adjoiningRoads = splitRoadAtNode(location, currentRoad)
            }

            // Create the choices from the roads that we've selected
            for (road in adjoiningRoads) {
                val line = road.geometry as LineString
                var lineList: List<LngLatAlt> = line.coordinates
                if (joiningRoadOtherEnd != null) {
                    val containsOtherEnd = line.coordinates.any { it == joiningRoadOtherEnd }
                    if (containsOtherEnd) {
                        // We've got the line after the joining road. Copy it and replace the end of
                        // it with our current location
                        val newLine: MutableList<LngLatAlt> = mutableListOf()
                        for (point in lineList) {
                            if (point == joiningRoadOtherEnd) {
                                // This moves the end of the line to our current location, thus
                                // skipping the joining line.
                                newLine.add(location)
                            } else
                                newLine.add(point)
                        }
                        lineList = newLine
                    }
                }
                val heading = bearingOfLineFromEnd(location, lineList)
                choices.add(
                    StreetPreviewChoice(
                        heading,
                        name = road.properties?.get("name").toString(),
                        lineList
                    )
                )
            }
        }
        val end = System.currentTimeMillis()
        Log.d(TAG, "getDirectionChoices: ${end - start}ms")

        return choices
    }

    fun getLastHeading() : Double {
        return lastHeading
    }

    companion object {
        private const val TAG = "StreetPreview"
    }
}
