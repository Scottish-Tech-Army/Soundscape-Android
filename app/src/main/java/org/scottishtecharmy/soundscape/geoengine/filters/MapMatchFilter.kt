package org.scottishtecharmy.soundscape.geoengine.filters

import android.os.Build
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.addSidewalk
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.circleToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.clone
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance
import org.scottishtecharmy.soundscape.geoengine.utils.fromRadians
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.toRadians
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.measureTime

private const val FRECHET_QUEUE_SIZE = 4 // The size of this queue doesn't make a lot of difference!

data class RoadFollowerStatus(val frechetAverage: Double, val confidence: Int)

class RoadFollower(val parent: MapMatchFilter,
                   var currentNearestRoad: Way,
                   var lastGpsLocation: LngLatAlt?,
                   val colorIndex: Int) {

    var radius = 2.0
    var lastChordPoints: Array<LngLatAlt> = arrayOf(LngLatAlt(), LngLatAlt())
    var nearestPoint: PointAndDistanceAndHeading? = null
    var lastMatchedLocation: PointAndDistanceAndHeading? = null
    var lastCenter = LngLatAlt()
    val frechetQueue = ArrayDeque<Double>(FRECHET_QUEUE_SIZE)
    var nextRoad: Way? = null

    val k = 0.2
    var averagePointGap: Double = 0.0

    val color: String
    init {
        val colorArray = arrayOf(
            "#ff0000",
            "#00ff00",
            "#0000ff",
            "#ffff00",
            "#00ffff",
            "#ff00ff",
            "#800000",
            "#008000",
            "#000080",
            "#808000",
            "#008080",
            "#800080"
        )
        color = colorArray[colorIndex % colorArray.size]
    }

    fun extendToNewWay(newWay: Way, colorIndexOffset: Int) : RoadFollower {
        // Create a new follower which is a copy of this one, but with a new nextRoad set
        val newFollower = RoadFollower(parent, currentNearestRoad, lastGpsLocation?.clone(), colorIndex + colorIndexOffset)

        // Clone all the data that we need
        newFollower.averagePointGap = averagePointGap
        newFollower.nearestPoint = nearestPoint?.clone()
        newFollower.radius = radius
        frechetQueue.forEach { newFollower.frechetQueue.add(it) }
        lastChordPoints.forEachIndexed { index,point -> newFollower.lastChordPoints[index] = point.clone() }
        newFollower.lastMatchedLocation = lastMatchedLocation?.clone()

        // Don't clone the way as it's shared data
        newFollower.nextRoad = newWay

        return newFollower
    }


    fun update(gpsLocation: LngLatAlt, overallLastMatchedLocation: LngLatAlt?, collection: FeatureCollection) : RoadFollowerStatus {

        // Update radius
        var gpsHeading = Double.NaN
        var pointGap = 0.0
        var dMin = 0.0
        lastGpsLocation?.let { lastLocation ->

            if(lastLocation == gpsLocation) {
                if (frechetQueue.size == FRECHET_QUEUE_SIZE) {
                    return RoadFollowerStatus(frechetQueue.average(), 1)
                }
                return RoadFollowerStatus(Double.MAX_VALUE, 0)
            }

            // Get the shortest D min value (distance from new location to previous chord ends)
            dMin = min(
                gpsLocation.distance(lastChordPoints[0]),
                gpsLocation.distance(lastChordPoints[1])
            )

            // Calculate the average point gap
            pointGap = gpsLocation.distance(lastLocation)
            if (averagePointGap == 0.0)
                averagePointGap = pointGap
            else {
                averagePointGap *= 0.9
                averagePointGap += (0.1 * pointGap)
            }
            gpsHeading = bearingFromTwoPoints(lastLocation, gpsLocation)
        }

        if (currentNearestRoad.geometry.type == "LineString") {

            nearestPoint =
                gpsLocation.distanceToLineString(currentNearestRoad.geometry as LineString)
            if (nextRoad != null) {
                val nearestPointOnNextRoad =
                    gpsLocation.distanceToLineString(nextRoad!!.geometry as LineString)
                if (nearestPointOnNextRoad.distance < nearestPoint!!.distance) {

                    currentNearestRoad = nextRoad!!.also {
                        nextRoad = currentNearestRoad
                        nearestPoint = nearestPointOnNextRoad
                    }
                }
            }
            nearestPoint?.let { nearestPoint ->

                // Dispose of this if we're a long way away
                if(nearestPoint.distance > 30.0) {
                    return RoadFollowerStatus(Double.MAX_VALUE, -1)
                }

                val roadHeading = nearestPoint.heading
                if (!gpsHeading.isNaN()) {
                    var headingDifference = calculateHeadingOffset(gpsHeading, roadHeading)
                    if (headingDifference > 90.0)
                        headingDifference = 180.0 - headingDifference
                    val cosHeadingDifference = cos(toRadians(headingDifference))
                    if (cosHeadingDifference < 0.730) {
                        // Unreliable GPS heading - the GPS is moving in a different direction to
                        // the road
                        if(Build.VERSION.SDK_INT == 0) {
                            val circle = Feature()
                            circle.geometry = circleToPolygon(
                                32,
                                gpsLocation.latitude,
                                gpsLocation.longitude,
                                radius
                            )
                            circle.properties = hashMapOf()
                            circle.properties?.set("fill-opacity", 0.0)
                            circle.properties?.set("stroke", color)
                            circle.properties?.set("radius", radius)
                            collection.addFeature(circle)
                        }

                        if (frechetQueue.isNotEmpty()) {
                            frechetQueue.removeFirst()
                            if(frechetQueue.size > (FRECHET_QUEUE_SIZE / 2))
                                return RoadFollowerStatus(frechetQueue.average(), 0)
                        }
                        return RoadFollowerStatus(Double.MAX_VALUE, -1)
                    }
                }

                // Last point
                lastCenter = gpsLocation
                var matchedPoint = nearestPoint
                if ((lastGpsLocation != null) and (lastMatchedLocation != null)) {
                    val c1 = bearingFromTwoPoints(lastGpsLocation!!, lastMatchedLocation!!.point)
                    val d1 = lastGpsLocation!!.distance(lastMatchedLocation!!.point)
                    var ar = k.pow(pointGap / averagePointGap)
                    if (ar.isNaN()) ar = 1.0
                    lastCenter = getDestinationCoordinate(gpsLocation, c1, d1 * ar)
                    matchedPoint =
                        lastCenter.distanceToLineString(currentNearestRoad.geometry as LineString)
                    radius = max(dMin, gpsLocation.distance(lastGpsLocation!!) * ar)
                }
                // Check if we can actually get to this new point from the last point
                if (overallLastMatchedLocation != null) {
                }

                if (matchedPoint.distance > radius)
                    radius = matchedPoint.distance * 1.2

                if(radius > 30.0) {
                    return RoadFollowerStatus(Double.MAX_VALUE, -1)
                }

                if(Build.VERSION.SDK_INT == 0) {
                    val circle = Feature()
                    circle.geometry =
                        circleToPolygon(32, lastCenter.latitude, lastCenter.longitude, radius)
                    circle.properties = hashMapOf()
                    circle.properties?.set("fill-opacity", 0.0)
                    circle.properties?.set("stroke", color)
                    circle.properties?.set("radius", radius)
                    collection.addFeature(circle)
                }

                var directionToNearestPoint = bearingFromTwoPoints(lastCenter, matchedPoint.point)
                val chordLength = sqrt(
                    (radius * radius) - (matchedPoint.distance * matchedPoint.distance)
                ) * 2

                //
                // TODO: There's a problem with the chordLength on far away roads, where it's
                //  sometimes zero which messes the area calculation up. If it's zero, then there's
                //  no free space, and so it's not a valid path at all.
                if (chordLength == 0.0)
                    frechetQueue.clear()
                else
                    frechetQueue.addLast(chordLength)

                if (frechetQueue.size > FRECHET_QUEUE_SIZE)
                    frechetQueue.removeFirst()

                val chordAngle = asin(chordLength / (2 * radius))
                lastChordPoints[0] = getDestinationCoordinate(
                    lastCenter,
                    directionToNearestPoint + fromRadians(chordAngle),
                    radius
                )
                lastChordPoints[1] = getDestinationCoordinate(
                    lastCenter,
                    directionToNearestPoint - fromRadians(chordAngle),
                    radius
                )

                lastMatchedLocation = matchedPoint
            }
            lastGpsLocation = gpsLocation
        }
        if (frechetQueue.size == FRECHET_QUEUE_SIZE) {
            return RoadFollowerStatus(frechetQueue.average(), 1)
        }

        return RoadFollowerStatus(Double.MAX_VALUE, 0)
    }

    fun chosen(): PointAndDistanceAndHeading? {
        nearestPoint?.let { nearestPoint ->
            return nearestPoint
        }
        return null
    }
}

class MapMatchFilter {

    //
    // This filter is partially based on the following paper:
    //
    // "An Improved Map-Matching Technique Based on the Fréchet Distance Approach for Pedestrian
    // Navigation Services" by Yoonsik Bang, Jiyoung Kim and Kiyun Yu.
    //
    // https://pmc.ncbi.nlm.nih.gov/articles/PMC5087552/
    //
    // The search radius value is calculated to try and keep an open free space path along the
    // current nearest road.
    //


    val followerList: MutableList<RoadFollower> = emptyList<RoadFollower>().toMutableList()
    var matchedLocation: PointAndDistanceAndHeading? = null
    var matchedWay: Way? = null
    var matchedFollower: RoadFollower? = null
    var lastLocation: LngLatAlt? = null

    fun nearestIntersection(way: Way, location: LngLatAlt) : Intersection? {
        var distanceToNearestIntersection = Double.MAX_VALUE
        var nearestIntersection : Intersection? = null

        for(wayEnd in way.intersections) {
            if (wayEnd != null) {
                // Follow way from intersection to avoid TILE_EDGE intersections
                val ways = emptyList<Pair<Boolean, Way>>().toMutableList()
                way.followWays(wayEnd, ways)

                val intersection = if (ways.last().first)
                    ways.last().second.intersections[WayEnd.START.id]
                else
                    ways.last().second.intersections[WayEnd.END.id]

                if (intersection != null) {
                    val distance = location.distance(intersection.location)
                    if (distance < distanceToNearestIntersection) {
                        distanceToNearestIntersection = distance
                        nearestIntersection = intersection
                    }
                }
            }
        }
        return nearestIntersection
    }

    var colorIndex = 0
    fun filter(location: LngLatAlt, gridState: GridState, collection: FeatureCollection): Triple<LngLatAlt?, Feature?, String> {

        var roadTree = gridState.featureTrees[TreeId.ROADS_AND_PATHS.id]

        var roads = roadTree.getNearestCollection(location, 20.0, 4)
        var found = false
        for (road in roads) {
            val way = road as Way

            for (follower in followerList) {
                if ((follower.currentNearestRoad == way) or
                    (follower.nextRoad == way)
                ) {
                    // Don't re-add the current follower
                    found = true
                }
            }

            if (!found) {
                val intersection = matchedWay?.doesIntersect(way)
                if (intersection != null) {
                    // This road meets our currently matched way, add all of the roads from the same
                    // intersection
                    for (member in intersection.members.withIndex()) {
                        var intersectionFound = false
                        for (follower in followerList) {
                            if ((follower.currentNearestRoad == member.value) or
                                (follower.nextRoad == member.value)
                            ) {
                                // Don't re-add the current follower
                                intersectionFound = true
                            }
                        }
                        if (!intersectionFound)
                            followerList.add(
                                matchedFollower!!.extendToNewWay(
                                    member.value,
                                    member.index + 1
                                )
                            )
                    }
                } else {
                    followerList.add(RoadFollower(this, way, lastLocation, colorIndex))
                    ++colorIndex
                }
            }
        }
        var lowestFrechet = Double.MAX_VALUE
        var lowestFollower: RoadFollower? = null
        var freshetList = emptyList<Pair<RoadFollowerStatus, String>>().toMutableList()
        val followerIterator = followerList.listIterator()
        while(followerIterator.hasNext()) {
            val follower = followerIterator.next()
            val frechetStatus = follower.update(location, matchedLocation?.point, collection)
            freshetList.add(Pair(frechetStatus, follower.color))
            if(frechetStatus.confidence < 0) {
                followerIterator.remove()
                continue
            }
            val way = follower.currentNearestRoad
            if(frechetStatus.frechetAverage < lowestFrechet) {
                var skip = false
                matchedWay?.let { matched ->
                    if (matched != way) {
                        // Can we get to this followers matched location from the last matched
                        // location via the road/path network?

                        // First check that we aren't just hopping between footway=sidewalk for the
                        // same Way. These aren't usually well inter-connected and so running
                        // Dijkstra on them will result in a longer distance than it really is. For
                        // example, crossing the road here:
                        // https://www.openstreetmap.org/query?lat=55.941074&lon=-4.320473
                        // is really moving between two sidewalks and is easily done regardless of
                        // the Dijkstra distance.

                        var useDijkstra = true
                        if(matched.isSidewalkOrCrossing() || way.isSidewalkOrCrossing()) {
                            // We're matching on a sidewalk, see if the other way is either the
                            // associated way or another sidewalk for the associated way
                            addSidewalk(matched, roadTree)
                            addSidewalk(way, roadTree)

                            val matchedPavement = matched.properties?.get("pavement")
                            val matchedName = matched.properties?.get("name")
                            val wayPavement = way.properties?.get("pavement")
                            val wayName = way.properties?.get("name")

                            if((matchedPavement != null) &&
                               ((matchedPavement == wayName) || (matchedPavement == wayPavement))) {
                                // The matched way is a sidewalk, and the other way is either the
                                // associated way or another sidewalk for the associated way
                                useDijkstra = false
                            }
                            else if((wayPavement != null) && (wayPavement == matchedName)) {
                                // The other way is a sidewalk, and the matched way is its
                                // associated way
                                useDijkstra = false
                            }
                        }
                        if(useDijkstra) {
                            val testDistance = (follower.averagePointGap * 3) + 10.0
                            val timeDijkstra = measureTime {
                                val shortestDistance = findShortestDistance(
                                    matchedLocation!!.point,
                                    follower.chosen()!!.point,
                                    matched,
                                    way,
                                    null,
                                    testDistance
                                )
                                if (shortestDistance.distance >= testDistance)
                                    skip = true
                                shortestDistance.tidy()
                            }
                            println("Time Dijkstra: $timeDijkstra")
                        }
                    }
                }
                if(!skip) {
                    lowestFrechet = frechetStatus.frechetAverage
                    lowestFollower = follower
                }
            }
        }

        lastLocation = location
        if(lowestFollower != null) {
            matchedLocation = lowestFollower.chosen()
            matchedFollower = lowestFollower
            if(Build.VERSION.SDK_INT == 0) {
                if (matchedWay != matchedFollower!!.currentNearestRoad) {
                    val choiceFeature = Feature()
                    choiceFeature.geometry =
                        Point(matchedLocation!!.point.longitude, matchedLocation!!.point.latitude)
                    choiceFeature.properties = hashMapOf()
                    choiceFeature.properties?.set("marker-size", "large")
                    choiceFeature.properties?.set("marker-color", "#000000")
                    for (choices in freshetList) {
                        choiceFeature.properties?.set(choices.second, choices.first.toString())
                    }
                    collection.addFeature(choiceFeature)
                }
            }

            matchedWay = matchedFollower!!.currentNearestRoad
            val color = matchedFollower!!.color
            matchedLocation?.let { matchedLocation ->
                return Triple(matchedLocation.point, matchedWay, color)
            }
        } else {
            matchedLocation = null
            matchedFollower = null
            matchedWay = null
        }

        return Triple(null, null, "")
    }
}