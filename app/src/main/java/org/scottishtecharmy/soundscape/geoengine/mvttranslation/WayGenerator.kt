package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.Direction
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.getCombinedDirectionSegments
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
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

    // We don't allow comparison of Intersections by data because we can have two TILE_EDGE
    // intersections at exactly the same point which are joined by a JOINER way and we can't have
    // them be declared to be the same as then we can't tell the direction of the JOINER.

    fun toFeature() {
        geometry = Point(location.longitude, location.latitude)
        properties = hashMapOf<String, Any?>()
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

    fun getName(direction: Boolean? = null) : String {

        var destinationModifier: Any? = null
        var name = properties?.get("name")

        if(name == null) {
            // Un-named way, so use "class" property
            name = properties?.get("class").toString()

            if (direction != null) {
                // Describe as 'towards'
                destinationModifier = if (direction)
                    properties?.get("destination:forward")
                else
                    properties?.get("destination:backward")

                if (destinationModifier == null) {
                    destinationModifier = if (direction)
                        properties?.get("dead-end:forward")
                    else
                        properties?.get("dead-end:backward")
                }

                if (destinationModifier != null) {
                    return "$name to $destinationModifier"
                }
            } else {
                val start = properties?.get("destination:backward")
                val end = properties?.get("destination:forward")

                if ((end != null) and (start != null)) {
                    return "$name that joins $start and $end"
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
            return "$name to $destinationModifier"
        }
            return name.toString()
    }

    fun doesIntersect(other: Way) : Intersection? {
        for(ours in intersections) {
            for(theirs in other.intersections) {
                if(ours == theirs)
                    return ours
            }
        }
        return null
    }

    fun followWays(fromIntersection: Intersection,
                   ways: MutableList<Pair<Boolean, Way>>,
                   depth: Int = 0,
                   optionalEarlyPredicate: ((Way, Way?) -> Boolean)? = null) {

        if(optionalEarlyPredicate != null) {
            if(optionalEarlyPredicate(this, ways.lastOrNull()?.second))
                return
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

}

fun convertBackToTileCoordinates(location: LngLatAlt,
                                 tileZoom : Int) : Pair<Int, Int> {


    var x = ((location.longitude + 180.0) / 360.0) * (1 shl tileZoom)
    var y = (1 shl tileZoom) * (1.0 - asinh(tan(toRadians(location.latitude))) / PI) / 2

    var xInt = (abs(x - truncate(x)) * 4096).toInt()
    var yInt = (abs(y - truncate(y)) * 4096).toInt()

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
            newFeature.properties = hashMapOf<String, Any?>()
            for((key, prop) in properties) {
                newFeature.properties!![key] = prop
            }
            newFeature.properties?.set("segmentIndex", segmentIndex.toString())
        }
        feature.foreign?.let { foreign ->

            newFeature.foreign = hashMapOf<String, Any?>()
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
                        currentSegmentLength += currentSegment.coordinates.last().distance(coordinate)
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
            intersection.value.properties = hashMapOf<String, Any?>()
            intersection.value.properties?.set("name", intersection.value.name)
            intersection.value.foreign = hashMapOf<String, Any?>()
            intersection.value.foreign?.set("feature_type", "highway")
            intersection.value.foreign?.set("feature_value", "gd_intersection")
            intersection.value.foreign?.set("osm_ids", osmIds)
            intersectionCollection.addFeature(intersection.value)

            intersectionMap[intersection.key] = intersection.value
        }
    }
}