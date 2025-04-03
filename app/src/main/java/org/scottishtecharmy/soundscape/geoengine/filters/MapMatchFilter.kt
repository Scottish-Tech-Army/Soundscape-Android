package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.circleToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.clone
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

private const val FRECHET_QUEUE_SIZE = 4 // The size of this queue doesn't make a lot of difference!

class RoadFollower(val parent: MapMatchFilter,
                   var currentNearestRoad: Way,
                   var lastGpsLocation: LngLatAlt?,
                   val colorIndex: Int) {

    var radius = 2.0
    var lastChordPoints: Array<LngLatAlt> = arrayOf(LngLatAlt(), LngLatAlt())
    var nearestPoint: PointAndDistanceAndHeading? = null
    var lastMatchedLocation: LngLatAlt? = null
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


    fun update(gpsLocation: LngLatAlt, overallLastMatchedLocation: LngLatAlt?, collection: FeatureCollection) : Double {

        // Update radius
        var gpsHeading = Double.NaN
        var pointGap = 0.0
        var dMin = 0.0
        lastGpsLocation?.let { lastLocation ->
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
                    return Double.MAX_VALUE
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
                        frechetQueue.addLast(50.0)
                        if(frechetQueue.size > FRECHET_QUEUE_SIZE)
                            frechetQueue.removeFirst()
                        return (frechetQueue.average())
                    }
                }

                // Last point
                var center = gpsLocation
                var matchedPoint = nearestPoint
                if ((lastGpsLocation != null) and (lastMatchedLocation != null)) {
                    val c1 = bearingFromTwoPoints(lastGpsLocation!!, lastMatchedLocation!!)
                    val d1 = lastGpsLocation!!.distance(lastMatchedLocation!!)
                    var ar = k.pow(pointGap / averagePointGap)
                    if (ar.isNaN()) ar = 1.0
                    center = getDestinationCoordinate(gpsLocation, c1, d1 * ar)
                    matchedPoint =
                        center.distanceToLineString(currentNearestRoad.geometry as LineString)
                    radius = max(dMin, gpsLocation.distance(lastGpsLocation!!) * ar)
                }
                // Check if we can actually get to this new point from the last point
                if (overallLastMatchedLocation != null) {
                }

                if (matchedPoint.distance > radius)
                    radius = matchedPoint.distance * 1.2

                if(radius > 30.0) {
                    return Double.MAX_VALUE
                }

                var directionToNearestPoint = bearingFromTwoPoints(center, matchedPoint.point)
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
                    center,
                    directionToNearestPoint + fromRadians(chordAngle),
                    radius
                )
                lastChordPoints[1] = getDestinationCoordinate(
                    center,
                    directionToNearestPoint - fromRadians(chordAngle),
                    radius
                )

                lastMatchedLocation = matchedPoint.point

                val circle = Feature()
                circle.geometry = circleToPolygon(32, center.latitude, center.longitude, radius)
                circle.properties = hashMapOf()
                circle.properties?.set("fill-opacity", 0.0)
                circle.properties?.set("stroke", color)
                circle.properties?.set("radius", radius)
                collection.addFeature(circle)
            }
            lastGpsLocation = gpsLocation
        }
        if (frechetQueue.isNotEmpty())
            return frechetQueue.average()

        return Double.MAX_VALUE
    }

    fun chosen(collection: FeatureCollection): LngLatAlt? {
        nearestPoint?.let { nearestPoint ->
//            val mapMatched = Feature()
//            mapMatched.geometry = Point(nearestPoint.point.longitude, nearestPoint.point.latitude)
//            mapMatched.properties = hashMapOf()
//            mapMatched.properties?.set("name", currentNearestRoad.properties?.get("name"))
//            mapMatched.properties?.set("marker-size", "small")
//            mapMatched.properties?.set("marker-color", "#f00000")
//            collection.addFeature(mapMatched)

            return nearestPoint.point
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
    var matchedLocation: LngLatAlt? = null
    var matchedWay: Way? = null
    var matchedFollower: RoadFollower? = null
    var lastLocation: LngLatAlt? = null

    fun nearestIntersection(way: Way, location: LngLatAlt) : Double {
        var distanceToNearestIntersection = Double.MAX_VALUE
        for(wayEnd in way.intersections) {
            if (wayEnd != null) {
                distanceToNearestIntersection = min(
                    distanceToNearestIntersection,
                    location.distance(wayEnd.location)
                )
            }
        }
        return distanceToNearestIntersection
    }

    var colorIndex = 0
    fun filter(location: LngLatAlt, gridState: GridState, collection: FeatureCollection): Triple<LngLatAlt?, Feature?, String> {

        var roadTree = gridState.featureTrees[TreeId.ROADS_AND_PATHS.id]

        var roads = roadTree.getNearestCollection(location, 20.0, 4)
        var found = false
        for (road in roads) {
            try {
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
            } catch(e:Exception) {
                println("!")
            }
        }
        var lowestFrechet = Double.MAX_VALUE
        var lowestFollower: RoadFollower? = null
        var freshetList = emptyList<Pair<Double, String>>().toMutableList()
        val followerIterator = followerList.listIterator()
        while(followerIterator.hasNext()) {
            val follower = followerIterator.next()
            val frechetAverage = follower.update(location, matchedLocation, collection)
            freshetList.add(Pair(frechetAverage, follower.color))
            if(frechetAverage > 20.0) {
                followerIterator.remove()
                continue
            }
            val way = follower.currentNearestRoad
            if(frechetAverage < lowestFrechet) {
                var skip = false
                if(matchedWay != null) {
                    if (matchedWay != way) {
                        val currentLayer = matchedWay?.properties?.get("layer")
                        val newRoadLayer = way.properties?.get("layer")
                        if((currentLayer != null) or (newRoadLayer != null)) {
                            var currentLayerInt = 0
                            var newRoadLayerInt = 0
                            if(currentLayer != null) currentLayerInt = currentLayer.toString().toInt()
                            if(newRoadLayer != null) newRoadLayerInt = newRoadLayer.toString().toInt()
                            if(currentLayerInt != newRoadLayerInt) {
                                // The current tracked way is on a different layer. Do they have a
                                // direct intersection that allows moving between layers? If they
                                // do not, then we skip this way.
                                skip = true
                                matchedWay?.let {
                                    if (way.doesIntersect(it) != null) {
                                        skip = false
                                    }
                                }
                            }
                        }
                        if(!skip) {
                            // The matched road is on the same Layer as the new road, but could they
                            // be connected?
                            matchedLocation?.let { location ->

                                val nearestToMatched =
                                    nearestIntersection(matchedWay!!, location)

                                val newMatchedLocation = follower.chosen(collection)
                                if (newMatchedLocation != null) {
                                    val nearestToNew =
                                        nearestIntersection(way, newMatchedLocation)

                                    val nearest = nearestToNew + nearestToMatched
                                    if (nearest > (follower.averagePointGap * 4))
                                        skip = true
                                }
                            }
                        }
                    }
                }
                if(!skip) {
                    lowestFrechet = frechetAverage
                    lowestFollower = follower
                }
            }
        }

        lastLocation = location
        if(lowestFollower != null) {
            matchedLocation = lowestFollower.chosen(collection)
            matchedFollower = lowestFollower
            if (matchedWay != matchedFollower!!.currentNearestRoad) {
                val choiceFeature = Feature()
                choiceFeature.geometry = Point(matchedLocation!!.longitude, matchedLocation!!.latitude)
                choiceFeature.properties = hashMapOf()
                choiceFeature.properties?.set("marker-size", "large")
                choiceFeature.properties?.set("marker-color", "#000000")
                for (choices in freshetList) {
                    choiceFeature.properties?.set(choices.second, choices.first.toString())
                }
                collection.addFeature(choiceFeature)
            }
            matchedWay = matchedFollower!!.currentNearestRoad
            val color = matchedFollower!!.color
            matchedLocation?.let { matchedLocation ->
                return Triple(matchedLocation, matchedWay, color)
            }
        } else {
            matchedLocation = null
            matchedFollower = null
            matchedWay = null
        }

        return Triple(null, null, "")
    }
}