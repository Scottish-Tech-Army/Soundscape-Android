package org.scottishtecharmy.soundscape.geoengine.filters

import android.os.Build
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.addSidewalk
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.calculateSmallestAngleBetweenLines
import org.scottishtecharmy.soundscape.geoengine.utils.circleToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.clone
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance
import org.scottishtecharmy.soundscape.geoengine.utils.fromRadians
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geoengine.utils.toRadians
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

private const val FRECHET_QUEUE_SIZE = 12

enum class RoadFollowerState {
    LOCKED,
    UNLOCKED,
    ANGLED_AWAY,
    DIRECTION_CHANGED,
    DISTANT
}
data class RoadFollowerStatus(val frechetAverage: Double, val state: RoadFollowerState)

/**
 * Create a single LineString from our route which is a list of Ways. Two Arrays are created when
 * the line is created:
 *   * indices - this allows any point in the line to be referenced back to the route that created
 *     it. When a point is found on the line it can be immediately matched to the Way that it
 *     belongs to.
 *   * direction - the line segments all have to be in the same direction, and this involves
 *     reversing the lines from the Way to keep the line contiguous. We store this so that we can
 *     adjust the heading for a point on the line to match that of the Way rather than of the line.
 *
 *  We also create a hashCode as the line is created. This is so that RoadFollowers with identical
 *  lines can be de-duplicated.
 */
class IndexedLineString {

    var line : LineString? = null
    var indices: Array<Int>? = null
    var direction: Array<Boolean>? = null
    var hashCode: Int = 0

    fun getWayIndex(pointIndex: Int) : Int? {

        indices?.let { indices ->
            for ((index, offset) in indices.withIndex()) {
                if (pointIndex < offset) {
                    return index
                }
            }
            return indices.size - 1
        }
        return null
    }

    fun updateFromRoute(route: List<Way>) {

        if (route.isEmpty()) {
            line = null
            return
        }

        indices = Array(route.size) { 0 }
        direction = Array(route.size) { true }
        if (route.size == 1) {
            line = route[0].geometry as LineString
            indices?.set(0, line!!.coordinates.size)
            hashCode = line?.coordinates.hashCode()
            return
        }

        line = LineString()
        for ((index, way) in route.withIndex()) {

            var forwards: Boolean
            if(index < route.size - 1) {
                // Most of the Ways in the route
                val (nextIntersection, ourIndex) = route[index].doesIntersect(route[index + 1])
                if(nextIntersection == null) {
                    // We can get here if the tile grid is recalculated, and tiles rejoined and the route is
                    // left with out of date and disconnected ways.
                    line = null
                    return
                }
                forwards = (ourIndex == WayEnd.END.id)
            } else {
                // The last Way in the route
                val (firstIntersection, ourIndex) = route[index].doesIntersect(route[index - 1])
                if(firstIntersection == null) {
                    // We can get here if the tile grid is recalculated, and tiles rejoined and the route is
                    // left with out of date and disconnected ways.
                    line = null
                    return
                }
                forwards = (ourIndex == WayEnd.START.id)
            }

            // And add its coordinates to the LineString along with whether or not we reversed the
            // order of the coordinates. This results in the same coordinate being duplicated at
            // each intersection, but is simple.
            if (forwards) {
                line!!.coordinates.addAll((way.geometry as LineString).coordinates)
            }
            else {
                line!!.coordinates.addAll((way.geometry as LineString).coordinates.reversed())
            }
            direction?.set(index, forwards)

            // Note the index at which this Way ends
            indices?.set(index, line!!.coordinates.size)
        }
        hashCode = line?.coordinates.hashCode()
    }
}

class RoadFollower(val parent: MapMatchFilter,
                   var route: MutableList<Way>,
                   var lastGpsLocation: LngLatAlt?,
                   val colorIndex: Int) {

    var radius = 2.0
    var lastChordPoints: Array<LngLatAlt> = arrayOf(LngLatAlt(), LngLatAlt())
    var nearestPoint: PointAndDistanceAndHeading? = null
    var lastMatchedLocation: PointAndDistanceAndHeading? = null
    var lastCenter = LngLatAlt()
    val frechetQueue = ArrayDeque<Double>(FRECHET_QUEUE_SIZE)

    val k = 0.2
    var averagePointGap: Double = 0.0

    var ils = IndexedLineString()
    var currentNearestRoad: Way = route[0]

    // Instead of acting on the LineStrings of individual Ways, we want to create our own LineString
    // by concatenating those within the Ways to make a single line. Each point should link to
    // the Way that it is a member of so that at any point it's know that that's what's being
    // followed. This should solve the current issue where the following is poor near intersections,
    // and followers with the closest matching LineString should maintain a good lock.
    // This is particularly important where there are many short segments e.g. around
    //          https://www.openstreetmap.org/node/12580941684
    //
    // The LineString can be trimmed once it's members no longer form part of the Frechet queue.
    //
    // OR perhaps we can just have an ordered list of Ways which are treated as a single LineString
    // by the algorithm. How about:
    //
    // When a follower nears the end of it's current Way (measurable via the points of the way),
    // another Way can be queued up behind it. In the case of a single Way with multiple 'joins'
    // there would exist a single RoadFollower containing all of the segments of the Way.
    // If a Way splits at an Intersection, then there would need to exist a RoadFollower for each
    // member of the intersection. This sort of happens right now - but they are for the segment
    // and not a continuation. We can perhaps tighten up on ending RoadFollowers when their radius
    // is far greater than the chosen RoadFollower.
    //
    // We also still need to create RoadFollowers for Way that appear nearby in case a user
    // jumps to an un-connected Way e.g. uses an unmarked Way or goes across some grass/pedestrian
    // area where there is no Way.
    //

    val color: String
    var directionOnLine = 0.0
    var directionHysteresis = 5
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

        validateRoute()
        ils.updateFromRoute(route)
    }

    fun dump() {
        val adapter = GeoJsonObjectMoshiAdapter()
        val collection = FeatureCollection()
        for (way in route) {
            collection.addFeature(way)
        }
        println(adapter.toJson(collection))
    }

    fun validateRoute() {
        val hashMap = HashMap<Intersection, Int>()
        for(way in route) {
            for(intersection in way.intersections) {
                if(intersection != null) {
                    if (hashMap.containsKey(intersection)) {
                        hashMap[intersection] = hashMap[intersection]!! + 1
                    } else {
                        hashMap[intersection] = 1
                    }
                }
            }
        }
        for(count in hashMap) {
            if(count.value > 2) {
                println("Too many intersections")
                dump()
                // We can get here 'legally' if we add a way which loops back and joins the current
                // way e.g. https://www.openstreetmap.org/way/945577262
//                assert(false)
            }
        }
    }

    /**
     * If a Way in a route is more than 60m away, then trim it off. If we get close to it again, we
     * can add it back in.
     */
    fun trimRoute(location: LngLatAlt, ruler: CheapRuler) : Boolean {
        var trimmed = false

        // Trim start
        val iterator = route.listIterator()
        while (iterator.hasNext()) {
            val way = iterator.next()
            if (ruler.distanceToLineString(location, way.geometry as LineString).distance > 60.0) {
                iterator.remove()
                trimmed = true
            } else {
                break
            }
        }

        // Go to end of list
        while (iterator.hasNext()) {
            iterator.next()
        }

        // Trim end
        while(iterator.hasPrevious()) {
            val way = iterator.previous()
            if(ruler.distanceToLineString(location, way.geometry as LineString).distance > 60.0) {
                iterator.remove()
                trimmed = true
            } else {
                break
            }
        }
        if(trimmed) {
            validateRoute()
            ils.updateFromRoute(route)
        }
        return trimmed
    }

    fun getRouteIntersectionIndices(newWay: Way) : Pair<Int, Int> {
        var minIndex = Int.MAX_VALUE
        var maxIndex = -1
        for((index, way) in route.withIndex()) {
            if(way.doesIntersect(newWay).first != null) {
                if(index < minIndex) minIndex = index
                if(index > maxIndex) maxIndex = index
            }
        }
        return Pair(minIndex, maxIndex)
    }
    fun createExtendedFollower(extensionAddedAtStart: Boolean,
                               newWay: Way,
                               colorIndexOffset: Int) : RoadFollower {
        // Create a new follower which is a copy of this one, but with an extended route
        val newRoute = mutableListOf<Way>()

        val (minIndex, maxIndex) = getRouteIntersectionIndices(newWay)
        if(minIndex != maxIndex) {
            // newWay intersects with more than one of our ways, so we need to drop one.
            if (extensionAddedAtStart) {
                // Replace the first way
                newRoute.addAll(route)
                newRoute[0] = newWay
            } else {
                // Replace the last way
                newRoute.addAll(route)
                newRoute[maxIndex] = newWay
            }
        } else {
            if (extensionAddedAtStart) {
                // Insert as first way
                newRoute.add(newWay)
                newRoute.addAll(route)
            } else {
                // Append as last way
                newRoute.addAll(route)
                newRoute.add(newWay)
            }
        }

        val newFollower = RoadFollower(parent, newRoute, lastGpsLocation?.clone(), colorIndex + colorIndexOffset)

        // Clone all the data that we need
        newFollower.averagePointGap = averagePointGap
        newFollower.nearestPoint = nearestPoint?.clone()
        newFollower.radius = radius
        newFollower.lastCenter = lastCenter.clone()
        newFollower.currentNearestRoad = currentNearestRoad

        frechetQueue.forEach { newFollower.frechetQueue.add(it) }
        lastChordPoints.forEachIndexed { index,point -> newFollower.lastChordPoints[index] = point.clone() }
        newFollower.lastMatchedLocation = lastMatchedLocation?.clone()

        return newFollower
    }

    fun extendToNewWay(newWay: Way, ruler: Ruler) : Boolean {
        val (minIndex, maxIndex) = getRouteIntersectionIndices(newWay)
        if(minIndex != maxIndex) {
            // We can't extend the route as the newWay intersects with more than one of the ways in
            // the route already.
            return false
        }

        val newRoute = mutableListOf<Way>()
        if(route.first().doesIntersect(newWay).first != null) {
            if(!route.first().intersections.contains(null)) {
                newRoute.add(newWay)
                newRoute.addAll(route)
            }
        } else if(route.last().doesIntersect(newWay).first != null) {
            if(!route.last().intersections.contains(null)) {
                newRoute.addAll(route)
                newRoute.add(newWay)
            }
        } else {
            assert(false)
        }
        if(newRoute.isNotEmpty()) {
            if (newRoute[0].doesIntersect(newRoute[1]).first == null) {
                assert(false)
            }

            route = newRoute
            validateRoute()
            ils.updateFromRoute(route)
            if(ils.line != null) {
                nearestPoint?.let { point ->
                    nearestPoint = ruler.distanceToLineString(point.point, ils.line as LineString)
                }
            }
            directionOnLine = 0.0
            directionHysteresis = 5
            return true
        }
        return false
    }

    fun update(
        gpsLocation: LngLatAlt,
        collection: FeatureCollection,
        ruler: CheapRuler
    ) : RoadFollowerStatus {

        if(ils.line == null)
            return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.DISTANT)
        if(trimRoute(gpsLocation, ruler)) {
            // The route was trimmed
            if (route.isEmpty() || ils.line == null) {
                return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.DISTANT)
            }
            // Reset nearestPoint so that the pointAlongLine is based on our newly trimmed line
            nearestPoint?.let { point ->
                nearestPoint = ruler.distanceToLineString(point.point, ils.line as LineString)
            }
            directionOnLine = 0.0
            directionHysteresis = 5
        }

        // Update radius
        var gpsHeading = Double.NaN
        var pointGap = 0.0
        var dMin = 0.0
        var directionChange = false
        lastGpsLocation?.let { lastLocation ->

            if(lastLocation == gpsLocation) {
                if (frechetQueue.size > FRECHET_QUEUE_SIZE/2) {
                    return RoadFollowerStatus(frechetQueue.average(), RoadFollowerState.LOCKED)
                }
                return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.UNLOCKED)
            }

            // Get the shortest D min value (distance from new location to previous chord ends)
            dMin = min(
                ruler.distance(gpsLocation, lastChordPoints[0]),
                ruler.distance(gpsLocation, lastChordPoints[1])
            )

            // Calculate the average point gap
            pointGap = ruler.distance(gpsLocation, lastLocation)
            if (averagePointGap == 0.0)
                averagePointGap = pointGap
            else {
                averagePointGap *= 0.9
                averagePointGap += (0.1 * pointGap)
            }
            gpsHeading = bearingFromTwoPoints(lastLocation, gpsLocation)
        }

        ils.line?.let { line ->

            val lastNearestPoint = nearestPoint
            // Get the nearest point on our accumulated LineString to the GPS location. Everything
            // returned is valid except for the heading. The heading is relative to the direction of
            // the accumulated LineString which may be the opposite direction to the line within the
            // Way.
            nearestPoint =
                ruler.distanceToLineString(gpsLocation, ils.line as LineString)
            nearestPoint?.let { nearestPoint ->

                if((lastNearestPoint != null) &&
                    !lastNearestPoint.positionAlongLine.isNaN() &&
                    !nearestPoint.positionAlongLine.isNaN())
                {
                    val delta = nearestPoint.positionAlongLine - lastNearestPoint.positionAlongLine
                    if((directionOnLine != 0.0) && ((sign(delta) != sign(directionOnLine)) || (delta == 0.0))) {
                        // Change of direction?
                        --directionHysteresis
                        if(directionHysteresis == 0) {
                            directionHysteresis = 5
                            directionOnLine = delta
                        }
                        directionChange = true
                    } else {
                        directionHysteresis = 5
                        directionOnLine = delta
                    }
                } else {
                    directionHysteresis = 5
                }

                val routeIndex = ils.getWayIndex(nearestPoint.index)
                if(routeIndex != null) {
                    currentNearestRoad = route[routeIndex]

                    if(ils.direction?.get(routeIndex) == false) {
                        // The coordinates were reversed, so we need to reverse the heading
                        nearestPoint.heading = (nearestPoint.heading + 180.0) % 360.0
                    }
                }

                // Dispose of this if we're a long way away
                if(nearestPoint.distance > 30.0) {
                    return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.DISTANT)
                }

                // Last point
                lastCenter = gpsLocation
                var matchedPoint = nearestPoint
                if ((lastGpsLocation != null) and (lastMatchedLocation != null)) {
                    val c1 = bearingFromTwoPoints(lastGpsLocation!!, lastMatchedLocation!!.point)
                    val d1 = ruler.distance(lastGpsLocation!!, lastMatchedLocation!!.point)
                    var ar = k.pow(pointGap / averagePointGap)
                    if (ar.isNaN()) ar = 1.0
                    lastCenter = getDestinationCoordinate(gpsLocation, c1, d1 * ar)
                    matchedPoint =
                        ruler.distanceToLineString(lastCenter, currentNearestRoad.geometry as LineString)
                    radius = max(dMin, ruler.distance(gpsLocation, lastGpsLocation!!) * ar)
                }

                if (matchedPoint.distance > radius)
                    radius = matchedPoint.distance * 1.2

                if(radius > 30.0) {
                    return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.DISTANT)
                }

                if(Build.VERSION.SDK_INT == 10000) {
                    val circle = Feature()
                    circle.geometry =
                        circleToPolygon(32, lastCenter.latitude, lastCenter.longitude, radius)
                    circle.properties = hashMapOf()
                    circle.properties?.set("fill-opacity", 0.0)
                    circle.properties?.set("stroke", color)
                    circle.properties?.set("radius", radius)
                    collection.addFeature(circle)
                }

                val directionToNearestPoint = bearingFromTwoPoints(lastCenter, matchedPoint.point)
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

        val roadHeading = nearestPoint?.heading
        if (!gpsHeading.isNaN() && roadHeading != null) {
            val headingDifference = calculateSmallestAngleBetweenLines(gpsHeading, roadHeading)
            val cosHeadingDifference = cos(toRadians(headingDifference))
            if (cosHeadingDifference < 0.703) {
                // Unreliable GPS heading - the GPS is moving in a very different direction
                // to the road.
                if(frechetQueue.size > (FRECHET_QUEUE_SIZE / 2))
                    return RoadFollowerStatus(frechetQueue.average(), RoadFollowerState.ANGLED_AWAY)

                return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.ANGLED_AWAY)
            }
        }
        if(frechetQueue.size > (FRECHET_QUEUE_SIZE / 2)) {
            return RoadFollowerStatus(frechetQueue.average(), if(directionChange) RoadFollowerState.DIRECTION_CHANGED else RoadFollowerState.LOCKED)
        }

        return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.UNLOCKED)
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


    val followerList: MutableList<RoadFollower> = mutableListOf<RoadFollower>()
    var matchedLocation: PointAndDistanceAndHeading? = null
    var matchedWay: Way? = null
    var matchedFollower: RoadFollower? = null
    var lastLocation: LngLatAlt? = null

    fun addWaysToFollowers(intersection: Intersection,
                           follower: RoadFollower,
                           iterator: MutableListIterator<RoadFollower>,
                           ruler: Ruler) {
        var extended = false
        var extensionAddedAtStart = false
        for (member in intersection.members.withIndex()) {

            // Don't add a Way more than once to the route
            if (follower.route.contains(member.value)) continue

            // Skip JOINERS
            if (member.value.wayType == WayType.JOINER) continue
            if (!extended) {
                // Try and extend the follower to this way
                if(follower.extendToNewWay(member.value, ruler)) {
                    extended = true
                    extensionAddedAtStart = follower.route.first() == member.value
                    continue
                }
            }
            // Create a new follower which adds this way to the route
            iterator.add(
                follower.createExtendedFollower(
                    extensionAddedAtStart,
                    member.value,
                    member.index + 1
                )
            )
        }
    }

    /**
     * extendFollowerList looks for nearby roads and ensures that they are included in the list
     * of RoadFollowers that we have. There should be a RoadFollower for every possible road segment
     * combination.
     */
    fun extendFollowerList(location: LngLatAlt, gridState: GridState) {
        val roadTree = gridState.featureTrees[TreeId.ROADS_AND_PATHS.id]

        val roads = roadTree.getNearestCollection(location, 20.0, 8, gridState.ruler)

        if (followerList.isEmpty()) {
            if(roads.features.isNotEmpty()) {
                // Start off with a follower for the nearest road
                followerList.add(RoadFollower(this, MutableList(1) { roads.first() as Way }, lastLocation, colorIndex))
                ++colorIndex
            }
        }

        for (road in roads) {
            val way = road as Way
            var added = false

            for (follower in followerList) {
                if (follower.route.contains(way)) {
                    // We already have some followers that are following this Way, so don't add more
                    added = true
                    break
                }
                if((way.properties?.containsKey("dead-end:forward") == true) ||
                   (way.properties?.containsKey("dead-end:backward") == true)) {
                    // The way is a dead end, don't follow it if it's also short
                    if(way.length < 20.0) {
                        added = true
                        break
                    }
                }
            }
            if (!added) {

                // For each follower, see if we can append this way
                val iterator = followerList.listIterator()
                while(iterator.hasNext()) {
                    val follower = iterator.next()
                    val lastWayInRoute = follower.route.last()
                    val (intersection, _) = lastWayInRoute.doesIntersect(way)
                    if (intersection != null) {
                        // This road intersects with the last way, so it can either replace it, or
                        // be added on to it.
                        addWaysToFollowers(intersection, follower, iterator, gridState.ruler)
                        added = true
                    } else {
                        val firstWayInRoute = follower.route.first()
                        val (firstIntersection, _) = firstWayInRoute.doesIntersect(way)
                        if (firstIntersection != null) {
                            // This road intersects with the first way, so it can either replace it,
                            // or be added on to it.
                            addWaysToFollowers(firstIntersection, follower, iterator, gridState.ruler)
                            added = true
                        }
                    }
                }
                // If no follower added this Way, then create a new follower for it
                if (!added) {
                    followerList.add(
                        RoadFollower(
                            this,
                            MutableList(1) { way },
                            lastLocation,
                            colorIndex
                        )
                    )
                    ++colorIndex
                }
            }
        }

        // De-duplicate list of followers
        val followerIterator = followerList.listIterator()
        val followerHashes = HashSet<Int>()
        while(followerIterator.hasNext()) {
            val follower = followerIterator.next()
            val hash = follower.ils.hashCode
            if(followerHashes.contains(hash))
                followerIterator.remove()
            else
                followerHashes.add(hash)
        }
    }

    var colorIndex = 0
    fun filter(location: LngLatAlt, gridState: GridState, collection: FeatureCollection, dump: Boolean): Triple<LngLatAlt?, Feature?, String> {

        extendFollowerList(location, gridState)

        var lowestFrechet = Double.MAX_VALUE
        var lowestFollower: RoadFollower? = null
        val freshetList = mutableListOf<Pair<RoadFollowerStatus, String>>()
        val followerIterator = followerList.listIterator()
        while(followerIterator.hasNext()) {
            val follower = followerIterator.next()
            val frechetStatus = follower.update(location, collection, gridState.ruler)

            freshetList.add(Pair(frechetStatus, follower.color))
            if(frechetStatus.state == RoadFollowerState.DISTANT) {
                followerIterator.remove()
                continue
            }
            if((frechetStatus.state == RoadFollowerState.ANGLED_AWAY) ||
               (frechetStatus.state == RoadFollowerState.DIRECTION_CHANGED)){
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
                            val roadTree = gridState.featureTrees[TreeId.ROADS_AND_PATHS.id]
                            addSidewalk(matched, roadTree, gridState.ruler)
                            addSidewalk(way, roadTree, gridState.ruler)

                            val matchedPavement = matched.properties?.get("pavement")
                            val matchedName = matched.name
                            val wayPavement = way.properties?.get("pavement")
                            val wayName = way.name

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
                            val testDistance = (follower.averagePointGap * 8) + 15.0
                            val shortestDistance = findShortestDistance(
                                matchedLocation!!.point,
                                matched,
                                follower.chosen()!!.point,
                                way,
                                null,
                                null,
                                testDistance
                            )
                            if (shortestDistance.distance >= testDistance)
                                skip = true
                            shortestDistance.tidy()
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
        if (lowestFollower != null) {
            matchedLocation = lowestFollower.chosen()
            matchedFollower = lowestFollower
            if (Build.VERSION.SDK_INT == 10000) {
                if (matchedWay != matchedFollower!!.currentNearestRoad) {
                    val choiceFeature = Feature()
                    choiceFeature.geometry =
                        Point(
                            matchedLocation!!.point.longitude,
                            matchedLocation!!.point.latitude
                        )
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
        }
        matchedLocation = null
        matchedFollower = null
        matchedWay = null

        return Triple(null, null, "")
    }

    fun dump() {
        println("Dump followers")
        for(follower in followerList) {
            follower.dump()
        }
    }
}