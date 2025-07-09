package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import android.content.Context
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.utils.Direction
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.confectNamesForRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getCombinedDirectionSegments
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geoengine.utils.toRadians
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import kotlin.collections.set
import kotlin.collections.toTypedArray
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.tan
import kotlin.math.truncate
import kotlin.text.replaceFirstChar

enum class IntersectionType(
    val id: Int,
) {
    REGULAR(0),
    TILE_EDGE(1)
}

class Intersection : Feature() {
    var members: MutableList<Way> =
        emptyList<Way>().toMutableList()    // Ways that make up this intersection
    var name = ""                                                       // Name of the intersection
    var location =
        LngLatAlt()                                          // Location of the intersection
    var intersectionType = IntersectionType.REGULAR

    // Dijkstra variables
    var dijkstraRunCount = 0
    var dijkstraDistance = Double.MAX_VALUE
    var dijkstraPrevious : Intersection? = null

    // We don't allow comparison of Intersections by data because we can have two TILE_EDGE
    // intersections at exactly the same point which are joined by a JOINER way and we can't have
    // them be declared to be the same as then we can't tell the direction of the JOINER.

    fun toFeature() {
        geometry = Point(location.longitude, location.latitude)
        properties = hashMapOf()
        properties?.set("name", name)
        properties?.set("members", members.size)
        properties?.set("type", if(intersectionType == IntersectionType.TILE_EDGE) "tile_edge" else "intersection")
    }
}

enum class WayType(
    val id: Int,
) {
    REGULAR(0),
    JOINER(1)
}

enum class WayEnd(
    val id: Int,
) {
    START(0),
    END(1)
}

private val DirectionLookup = Direction.entries.toTypedArray()
class Way : Feature() {
    var length = 0.0                            // We could easily calculate this from the segments.

    var intersections = arrayOf<Intersection?>(null, null)  // Intersections at either end

    var wayType = WayType.REGULAR

    fun getName(direction: Boolean? = null,
                gridState: GridState? = null,
                localizedContext: Context? = null,
                nonGenericOnly: Boolean = false) : String {

        var destinationModifier: Any? = null
        var passesModifier: Any?
        var name = properties?.get("name")
        val genericName = (name == null)
        var passesString = ""

        if(name == null) {
            // Un-named way, so use "class" property
            name = properties?.get("class").toString()

            if(gridState != null) {
                confectNamesForRoad(this, gridState)
            }

            if (direction != null) {
                // Describe as 'towards'
                destinationModifier = if (direction)
                    properties?.get("destination:forward")
                else
                    properties?.get("destination:backward")

                passesModifier = if(direction)
                    properties?.get("passes:forward")
                else
                    properties?.get("passes:backward")
                passesString = passesModifier?.toString() ?: ""

                if (destinationModifier == null) {
                    destinationModifier = if (direction)
                        properties?.get("dead-end:forward")
                    else
                        properties?.get("dead-end:backward")
                }

                if (destinationModifier != null) {
                    return if(passesString.isNotEmpty()) {
                        localizedContext?.getString(R.string.confect_name_to_via)
                            ?.format(name,destinationModifier, passesString) ?: "$name to $destinationModifier via $passesString"
                    } else {
                        localizedContext?.getString(R.string.confect_name_to)
                            ?.format(name,destinationModifier) ?: "$name to $destinationModifier"
                    }
                }
            } else {
                val start = properties?.get("destination:backward")
                val end = properties?.get("destination:forward")

                if ((end != null) and (start != null)) {
                    return localizedContext?.getString(R.string.confect_name_joins)
                        ?.format(name, start, end) ?: "$name that joins $start and $end"
                }
            }
        }
        if (direction != null) {
            destinationModifier = if (direction)
                properties?.get("dead-end:forward")
            else
                properties?.get("dead-end:backward")
        }
        if (destinationModifier != null) {
            if(destinationModifier == "dead-end") {
                destinationModifier = localizedContext?.getString(R.string.confect_name_dead_end) ?: "dead end"
            }
            return if(passesString.isNotEmpty()) {
                localizedContext?.getString(R.string.confect_name_to_via)
                    ?.format(name,destinationModifier, passesString) ?: "$name to $destinationModifier via $passesString"
            } else {
                localizedContext?.getString(R.string.confect_name_to)
                    ?.format(name,destinationModifier) ?: "$name to $destinationModifier"
            }
        }
        else {
            return if (passesString.isNotEmpty()) {
                localizedContext?.getString(R.string.confect_name_via)
                    ?.format(name, passesString) ?: "$name via $passesString"
            } else {
                // This is a path/service/track with no other qualifiers, so just return the name
                // unless we're looking for a non-generic name.
                if(nonGenericOnly && genericName) {
                    return ""
                }
                return name.toString()
            }
        }
    }

    fun doesIntersect(other: Way) : Pair<Intersection?, Int> {
        for((ourIndex ,ours) in intersections.withIndex()) {
            if(ours == null) continue
            for(theirs in other.intersections) {
                if(theirs == null) continue
                // Check for direct intersection first
                if(ours == theirs)
                    return Pair(ours, ourIndex)
            }
        }

        for((ourIndex ,ours) in intersections.withIndex()) {
            if(ours == null) continue
            for(theirs in other.intersections) {
                if(theirs == null) continue
                // Check for tile-edge joiner
                if((ours.intersectionType == IntersectionType.TILE_EDGE) &&
                    (theirs.intersectionType == IntersectionType.TILE_EDGE)) {
                    for(member in ours.members) {
                        if(theirs.members.contains(member)) {
                            return Pair(theirs, ourIndex)
                        }
                    }
                }
            }
        }

        return Pair(null, 0)
    }

    fun isSidewalkOrCrossing() : Boolean {
        val footway = properties?.get("footway")
        return ((footway == "sidewalk") || (footway == "crossing"))
    }

    fun endsAtTileEdge() : Boolean {
        return (intersections[WayEnd.START.id]?.intersectionType == IntersectionType.TILE_EDGE) ||
                (intersections[WayEnd.END.id]?.intersectionType == IntersectionType.TILE_EDGE)
    }

    /**
     * isSidewalkConnector returns true if this way is joining mainWay from intersection to its
     * own sidewalk e.g. https://www.openstreetmap.org/way/958596881. If we are map matched to the
     * sidewalk, but calling out from the perspective of mainWay, these connectors are not useful.
     */
    fun isSidewalkConnector(intersection: Intersection,
                            mainWay: Way?,
                            gridState: GridState,) : Boolean {

        // It's not a connector if the mainWay isn't named
        if(mainWay == null)
            return false

        // It's not a connector if it's named
        val name = properties?.get("name")
        if(name != null)
            return false

        // It's not a connector if it's more than 20m long, or it ends in a TILE_EDGE
        if((length > 20.0) || endsAtTileEdge())
            return false

        // Look at the other end and check if it connects to a sidewalk associated with the mainWay
        getOtherIntersection(intersection)?.let { otherIntersection ->
            for(way in otherIntersection.members) {
                if(way == this) continue
                if(isSidewalkOrCrossing()){
                    // This does connect to something that isn't a sidewalk, so it's not a simple
                    // connector i.e. it may connect to a sidewalk, but it goes further.
                    return false
                }
                else if(way.properties?.get("pavement") == null) {
                    confectNamesForRoad(way, gridState)
                }
                // And then return true if it's the pavement for this Way
                val pavement = way.properties?.get("pavement")
                return ((pavement != null) && (pavement == mainWay.properties?.get("name")))
            }
        }
        return false
    }

    fun followWays(fromIntersection: Intersection,
                   ways: MutableList<Pair<Boolean, Way>>,
                   depth: Int = 0,
                   optionalEarlyPredicate: ((Way, Way?) -> Boolean)? = null) {

        if(depth > 15)
        {
            // Break out at arbitrarily deep following.
            return
        }

        if(optionalEarlyPredicate != null) {
            if(wayType != WayType.JOINER) {
                if (optionalEarlyPredicate(this, ways.lastOrNull()?.second))
                    return
            }
        }

        for(existingWay in ways) {
            if(this == existingWay.second) {
                // This way has already been added to the list so we must have looped around, that's
                // the end of our following, otherwise we'll recurse forever deeper.
                return
            }
        }

        // Add this way
        val forwards = (fromIntersection == intersections[WayEnd.START.id])
        ways += Pair(forwards, this)

        // See if we can go further along the way. We can only go further if we have a series of
        // Intersections with only 2 Ways each and we haven't hit a named one yet.
        val nextIntersection = if (forwards)
            intersections[WayEnd.END.id]
        else
            intersections[WayEnd.START.id]

        if(nextIntersection?.members?.size == 2) {
            // We have a next intersection and it's only got 2 ways, so follow it onwards
            for(way in nextIntersection.members) {
                if(way != this) {
                    way.followWays(nextIntersection, ways, depth + 1, optionalEarlyPredicate)
                }
            }
        }
    }

    /** isLoopedBack is used to determine if a Way starts and ends at the same intersection.
     * @return true if the Way starts and ends at the same intersection, false otherwise
     */
    fun isLoopedBack() : Boolean {
        return (intersections[WayEnd.START.id] == intersections[WayEnd.END.id])
    }

    /** direction returns the integer direction (0-7) indicating which direction the way is relative
     * to the device heading.
     * @param fromIntersection is the intersection which the way is part of and from which we want
     * the direction to be calculated
     * @param deviceHeading is the heading relative to which the direction is calculated
     * @return the Direction indicating which direction the way is relative to the device heading
     */
    fun direction(fromIntersection: Intersection, deviceHeading: Double) : Direction {
        val directions = getCombinedDirectionSegments(deviceHeading)
        val heading = heading(fromIntersection)
        val index = directions.indexOfFirst { directionSegment ->
            directionSegment.contains(heading)
        }
        return DirectionLookup[index]
    }

    /**
     * heading returns the heading of the way as it leaves the intersection
     * @param fromIntersection is the intersection which the way is part of and from which we want
     * the heading to be calculated
     * @return the absolute heading of the way as it leaves the intersection
     */
    fun heading(fromIntersection: Intersection) : Double
    {
        val nextLocation = if (fromIntersection == intersections[WayEnd.START.id])
            (geometry as LineString).coordinates.drop(1).first()
        else
            (geometry as LineString).coordinates.dropLast(1).last()

        return bearingFromTwoPoints(fromIntersection.location, nextLocation)
    }

    fun containsIntersection(intersection: Intersection) : Boolean {
        return intersections.contains(intersection)
    }

    fun getOtherIntersection(fromIntersection: Intersection) : Intersection? {
        return if (fromIntersection == intersections[WayEnd.START.id])
            intersections[WayEnd.END.id]
        else
            intersections[WayEnd.START.id]
    }

    /**
     * @param location is where the distance is calculated from.
     * @return the distance along the Way from location to the START intersection. It's measured
     * from the nearest point on the Way.
     */
    fun createTemporaryIntersectionAndWays(location: LngLatAlt, ruler: Ruler) : Intersection {
        val newIntersection = Intersection()
        newIntersection.location = location

        val point = ruler.distanceToLineString(location, geometry as LineString)

        // Create two line strings out of the original line, adding in the location in the middle
        val line1 = LineString()
        val line2 = LineString()
        line2.coordinates.add(location)
        var length1 = 0.0
        var length2 = 0.0
        for(coordinate in (geometry as LineString).coordinates.withIndex()) {
            if(coordinate.index <= point.index) {
                if(coordinate.index > 0) {
                    length1 += ruler.distance(line1.coordinates.last(), coordinate.value)
                }
                line1.coordinates.add(coordinate.value)
            }
            else {
                length2 += ruler.distance(line2.coordinates.last(), coordinate.value)
                line2.coordinates.add(coordinate.value)
            }
        }
        length1 += ruler.distance(line1.coordinates.last(), location)
        line1.coordinates.add(location)

        val newWay1 = Way()
        newWay1.intersections[0] = intersections[0]
        newWay1.intersections[1] = newIntersection
        newWay1.geometry = line1
        newWay1.length = length1

        val newWay2 = Way()
        newWay2.intersections[0] = newIntersection
        newWay2.intersections[1] = intersections[1]
        newWay2.geometry = line2
        newWay2.length = length2

        if(length1 > length2) {
            newIntersection.members.add(newWay2)
            newIntersection.members.add(newWay1)        // Sort these based on length
        } else {
            newIntersection.members.add(newWay1)
            newIntersection.members.add(newWay2)        // Sort these based on length
        }

        if(intersections[WayEnd.START.id] != null) {
            intersections[WayEnd.START.id]!!.members.add(newWay1)
            intersections[WayEnd.START.id]!!.members =
                intersections[WayEnd.START.id]!!.members.sortedBy { way ->
                    way.length
                }.toMutableList()
        }

        if(intersections[WayEnd.END.id] != null) {
            intersections[WayEnd.END.id]!!.members.add(newWay2)
            intersections[WayEnd.END.id]!!.members =
                intersections[WayEnd.END.id]!!.members.sortedBy { way ->
                    way.length
                }.toMutableList()
        }

        return newIntersection
    }

    fun removeIntersection(intersection: Intersection) {
        // The passed in intersection has two member ways - one in each direction. Remove them from
        // the intersection at the other end.
        if(intersections[WayEnd.START.id] != null) {
            intersections[WayEnd.START.id]!!.members.remove(intersection.members[0])
            intersections[WayEnd.START.id]!!.members.remove(intersection.members[1])
        }

        if(intersections[WayEnd.END.id] != null) {
            intersections[WayEnd.END.id]!!.members.remove(intersection.members[0])
            intersections[WayEnd.END.id]!!.members.remove(intersection.members[1])
        }

        intersection.members.clear()
    }
}

fun convertBackToTileCoordinates(location: LngLatAlt,
                                 tileZoom : Int) : Pair<Int, Int> {


    val x = ((location.longitude + 180.0) / 360.0) * (1 shl tileZoom)
    val y = (1 shl tileZoom) * (1.0 - asinh(tan(toRadians(location.latitude))) / PI) / 2

    val xInt = (abs(x - truncate(x)) * 4096).toInt()
    val yInt = (abs(y - truncate(y)) * 4096).toInt()

    return Pair(xInt, yInt)
}

class WayGenerator {

    /**
    * highwayPoints is a sparse map which maps from a location within the tile to a list of
    * lines which have nodes at that point. Every node on any `transportation` line will appear in the
    * map and if after processing all of the lines there's an intersection at that point, the map
     * entry will have information for more than one line.
    */
    private val highwayNodes : HashMap< Int, Int> = hashMapOf()
    private val wayFeatures : MutableList<Feature> = emptyList<Feature>().toMutableList()

    private val ways : MutableList<Way> = emptyList<Way>().toMutableList()

    private val intersections : HashMap<LngLatAlt, Intersection> = hashMapOf()

    /**
     * addLine is called for any line feature that is being added to the FeatureCollection.
     * @param line is a new `transportation` layer line to add to the map
     *
     */
    fun addLine(line : ArrayList<Pair<Int, Int>>) {
        for (point in line) {
            if((point.first < 0) || (point.first > 4095) ||
                (point.second < 0) || (point.second > 4095)) {
                continue
            }

            // Rather than have a 2D sparse array, turn the coordinates into a single int so that we
            // can have a 1D sparse array instead.
            val coordinateKey = point.first.shl(12) + point.second
            val currentCount = highwayNodes[coordinateKey]
            if (currentCount == null) {
                highwayNodes[coordinateKey] = 1
            }
            else {
                highwayNodes[coordinateKey] = currentCount + 1
            }
        }
    }

    fun addFeature(feature: Feature) {
        wayFeatures.add(feature)
    }
    /**
    *  Inside generateIntersections, first traverse every line that was added and generate a new
    *  segment Feature at every intersection that we hit. Add these to Ways as we go. Intersections are spotted using the
    *  coordinate key (x + shr(y)). Put those features in two HashMaps a 'start' an 'end' one, again
    *  keyed by the coordinate key. Once we've traversed all of the lines we should have a Way for
    *  every segment between intersections. Now we generate the intersections and add the Ways directly
    *  to them. Let's do this in a separate class for now so that we can test it.
    */
    fun addSegmentFeatureToWay(feature: Feature,
                               currentSegment: LineString,
                               currentSegmentLength: Double,
                               segmentIndex: Int,
                               way: Way) {
        // Add feature with the segment up until this point
        val newFeature = Feature()
        feature.properties?.let { properties ->
            newFeature.properties = hashMapOf()
            for((key, prop) in properties) {
                newFeature.properties!![key] = prop
            }
            newFeature.properties?.set("segmentIndex", segmentIndex.toString())
        }
        feature.foreign?.let { foreign ->

            newFeature.foreign = hashMapOf()
            for((key, prop) in foreign) {
                newFeature.foreign!![key] = prop
            }
        }
        newFeature.geometry = currentSegment
        way.properties = newFeature.properties
        way.foreign = newFeature.foreign
        way.type = newFeature.type
        way.geometry = newFeature.geometry
        way.length = currentSegmentLength
    }

    fun generateWays(intersectionCollection: FeatureCollection,
                     waysCollection: FeatureCollection,
                     intersectionMap:  HashMap<LngLatAlt, Intersection>,
                     xTile: Int,
                     yTile: Int,
                     tileZoom : Int) {

        // Calculated tile limits
        val topLeft = getLatLonTileWithOffset(xTile, yTile, tileZoom, 0.0, 0.0)
        val bottomRight = getLatLonTileWithOffset(xTile, yTile, tileZoom, 1.0, 1.0)

        val ruler = topLeft.createCheapRuler()

        for(feature in wayFeatures) {
            if(feature.geometry.type == "LineString") {
                val line = feature.geometry as LineString
                var currentWay = Way()
                var currentSegment = LineString()
                var currentSegmentLength = 0.0
                var segmentIndex = 0
                var coordinateKey : Int
                var tileEdge = false
                for (coordinate in line.coordinates) {

                    tileEdge =
                        (coordinate.latitude == topLeft.latitude) or
                        (coordinate.longitude == topLeft.longitude) or
                        (coordinate.latitude == bottomRight.latitude) or
                        (coordinate.longitude == bottomRight.longitude)

                    if(tileEdge and (currentSegment.coordinates.isEmpty())) {
                        // We're starting at a tile edge, so create an intersection that we can
                        // join to other tiles later
                        val intersection = Intersection()
                        intersection.name = ""
                        intersection.location = coordinate
                        intersection.intersectionType = IntersectionType.TILE_EDGE

                        // The current way starts here
                        currentWay.intersections[WayEnd.START.id] = intersection
                        intersections[intersection.location] = intersection
                    }

                    if(currentSegment.coordinates.isNotEmpty()) {
                        // Add the length of the new segment
                        currentSegmentLength += ruler.distance(currentSegment.coordinates.last(), coordinate)
                    }
                    currentSegment.coordinates.add(coordinate)

                    // Is this coordinate at an intersection?
                    val tileCoordinates =
                        convertBackToTileCoordinates(coordinate, tileZoom)
                    coordinateKey = tileCoordinates.first.shl(12) + tileCoordinates.second
                    highwayNodes[coordinateKey]?.let {
                        if (it > 1) {
                            // Create an intersection if we don't have one already
                            var intersection = intersections.get(coordinate)
                            if(intersection == null) {
                                intersection = Intersection()
                                intersection.name = ""
                                intersection.location = coordinate
                                intersection.intersectionType = IntersectionType.REGULAR
                                intersections[coordinate] = intersection
                            }

                            if(currentSegment.coordinates.size > 1) {
                                addSegmentFeatureToWay(
                                    feature,
                                    currentSegment,
                                    currentSegmentLength,
                                    segmentIndex,
                                    currentWay
                                )
                                ++segmentIndex
                                currentWay.intersections[WayEnd.END.id] = intersection
                                ways.add(currentWay)

                                // Add completed way to intersection at end and at start if there is one
                                intersection.members.add(currentWay)
                                currentWay.intersections[WayEnd.START.id]?.members?.add(currentWay)

                                // Reset the segment accumulator
                                currentSegment = LineString()
                                currentSegmentLength = 0.0
                                currentSegment.coordinates.add(coordinate)
                            }

                            // Create a new Way feature for the upcoming segment
                            currentWay = Way().also { way ->
                                way.intersections[WayEnd.START.id] = intersection
                            }
                        }
                    }
                }

                if(currentSegment.coordinates.size > 1) {
                    addSegmentFeatureToWay(
                        feature,
                        currentSegment,
                        currentSegmentLength,
                        segmentIndex,
                        currentWay
                    )
                    ways.add(currentWay)
                    // Add completed way to intersection at start if there is one
                    if(currentWay.intersections[WayEnd.START.id] != null) {
                        currentWay.intersections[WayEnd.START.id]!!.members.add(currentWay)
                    }
                    if(tileEdge) {
                        // We're ending at a tile edge, so create an intersection that we can
                        // join to other tiles later
                        val intersection = Intersection()
                        intersection.name = ""
                        intersection.location = currentSegment.coordinates.last()
                        intersection.intersectionType = IntersectionType.TILE_EDGE

                        // The current way ends here
                        currentWay.intersections[WayEnd.END.id] = intersection
                        intersection.members.add(currentWay)
                        intersections[intersection.location] = intersection
                    }
                }
            }
        }
        for(way in ways) {
            waysCollection.addFeature(way)
        }

        for(intersection in intersections) {

            // Sort the members by length of the Way, shortest first. This is important for when we
            // traverse the graph using the Dijkstra algorithm.
            intersection.value.members = intersection.value.members.sortedBy { way ->
                way.length
            }.toMutableList()

            // Name the intersection
            val name = StringBuilder()
            val osmIds = arrayListOf<Double>()
            val namesUsed = emptySet<String>().toMutableSet()
            for(way in intersection.value.members) {
                var segmentName = way.properties?.get("name")
                if(segmentName == null) {
                    segmentName = way.properties?.get("class")
                    if(segmentName != null) {
                        val str = segmentName.toString()
                        if(str.isNotEmpty()) {
                            str.replaceFirstChar { it.uppercaseChar() }
                        }
                        if(name.isNotEmpty()) {
                            name.append("/")
                        }
                        name.append(str)
                    }
                } else {
                    if(!namesUsed.contains(segmentName.toString())) {
                        if(name.isNotEmpty()) {
                            name.append("/")
                        }
                        name.append("$segmentName")
                        namesUsed.add(segmentName.toString())
                    }
                }

                val id = way.properties?.get("osm_ids").toString().toDouble()
                osmIds.add(id)
            }
            intersection.value.name = name.toString()

            intersection.value.geometry = Point(intersection.value.location.longitude, intersection.value.location.latitude)
            intersection.value.properties = hashMapOf()
            intersection.value.properties?.set("name", intersection.value.name)
            intersection.value.foreign = hashMapOf()
            intersection.value.foreign?.set("feature_type", "highway")
            intersection.value.foreign?.set("feature_value", "gd_intersection")
            intersection.value.foreign?.set("osm_ids", osmIds)
            intersectionCollection.addFeature(intersection.value)

            intersectionMap[intersection.key] = intersection.value
        }
    }
}