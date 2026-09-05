package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.utils.Direction
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.confectNamesForRoad
import org.scottishtecharmy.soundscape.geoengine.utils.distanceAlongLineString
import org.scottishtecharmy.soundscape.geoengine.utils.getCombinedDirectionSegments
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.toRadians
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.tan
import kotlin.math.truncate

enum class IntersectionType(
    val id: Int,
) {
    REGULAR(0),
    TILE_EDGE(1)
}

class Intersection : MvtFeature() {
    var members = mutableListOf<Way>()
    var location =
        LngLatAlt()                                          // Location of the intersection
    var intersectionType = IntersectionType.REGULAR

    // Dijkstra variables
    var dijkstraRunCount = 0
    var dijkstraDistance = Double.MAX_VALUE
    var dijkstraPrevious: Intersection? = null

    // We don't allow comparison of Intersections by data because we can have two TILE_EDGE
    // intersections at exactly the same point which are joined by a JOINER way and we can't have
    // them be declared to be the same as then we can't tell the direction of the JOINER.

    fun toFeature() {
        geometry = Point(location)
        properties = HashMap<String, Any?>().apply {
            set("name", name)
            set("members", members.size)
            set(
                "type",
                if (intersectionType == IntersectionType.TILE_EDGE) "tile_edge" else "intersection"
            )
        }
    }

    fun updateName(
        gridState: GridState? = null,
        strings: LocalizedStrings?
    ) {
        val updatedName = StringBuilder()
        val namesUsed = mutableSetOf<String>()
        for (way in members) {
            val segmentName = way.getName(
                way.intersections[WayEnd.START.id] == this,
                gridState,
                strings,
                nonGenericOnly = false
            )
            if (!namesUsed.contains(segmentName)) {
                if (updatedName.isNotEmpty()) {
                    updatedName.append("/")
                }
                updatedName.append(segmentName)
                namesUsed.add(segmentName)
            }
        }
        name = updatedName.toString()
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

class Way : MvtFeature() {
    var length = 0.0                            // We could easily calculate this from the segments.

    var intersections = arrayOf<Intersection?>(null, null)  // Intersections at either end

    var wayType = WayType.REGULAR

    /**
     * Features positioned along this Way - crossings today, transit stops and junctions in future
     * - kept sorted ascending by [AlongWayFeature.distanceFromStart] so that "what's next along
     * this road?" is a lookup rather than a geographic search. Add via [addAlongWayFeature] to
     * maintain that ordering.
     *
     * A sorted list rather than a map keyed by distance: commonMain has no sorted-map type, and
     * two features can legitimately sit at the same distance. Ways are the pieces of road between
     * intersections, so these lists are short and a linear scan costs nothing.
     */
    val alongWayFeatures = mutableListOf<AlongWayFeature>()

    /** Inserts [feature] keeping [alongWayFeatures] sorted by distance from the START end. */
    fun addAlongWayFeature(feature: AlongWayFeature) {
        val index = alongWayFeatures.indexOfFirst {
            it.distanceFromStart > feature.distanceFromStart
        }
        if (index < 0)
            alongWayFeatures.add(feature)
        else
            alongWayFeatures.add(index, feature)
    }

    fun alongWayFeatures(kind: AlongWayKind): List<AlongWayFeature> =
        alongWayFeatures.filter { it.kind == kind }

    fun firstAlongWayFeature(kind: AlongWayKind): AlongWayFeature? =
        alongWayFeatures.firstOrNull { it.kind == kind }

    /**
     * The distance in metres from this Way's START intersection to the nearest point on this Way
     * to [point].
     *
     * Ruler.distanceToLineString clamps to the line's extent, so a [point] that isn't on this Way
     * comes back as 0.0 or [length] rather than as a position beyond either end. Everything that
     * records an [AlongWayFeature] therefore picks the Way the point genuinely lies on first - an
     * entry sitting at a clamped end would be read by the along-way queries as a real position.
     */
    fun distanceAlongWay(point: LngLatAlt, ruler: Ruler): Double {
        val line = geometry as? LineString ?: return 0.0
        if (line.coordinates.size < 2) return 0.0
        return distanceAlongLineString(line, ruler.distanceToLineString(point, line), ruler)
    }

    fun getName(
        direction: Boolean? = null,
        gridState: GridState? = null,
        strings: LocalizedStrings?,
        nonGenericOnly: Boolean = false,
        noGenericDeadEnds: Boolean = false
    ): String {

        var destinationModifier: Any? = null
        var passesModifier: Any?
        var result = name
        var genericName = (result == null)
        var passesString = ""
        val isRailway = (featureType == "rail") || (featureType == "transit")

        if ((result == null) && isRailway) {
            // Rail ways rarely carry a "ref", and the destination/dead-end confection below is a
            // road/pedestrian-path concept that reads oddly for a railway line ("Train that joins
            // Lennox Park and Milngavie Station"). OSM route-relation line names (e.g. "Argyle
            // Line") aren't in this tile schema yet, so fall back to a plain "train" rather than
            // confecting a name - use the real name above once the tile data has one.
            //
            // A tramway gets its own word: the Edinburgh line carries no name in the tile data, so
            // without this a tram rider is told "On train". Only tram, deliberately - light_rail,
            // monorail and funicular have no one word that reads right across the systems tagged
            // with them, and "train" is at least not wrong for those.
            return if (featureValue == "tram") {
                strings?.getOrNull(StringKey.DirectionsGenericTram) ?: "tram"
            } else {
                strings?.getOrNull(StringKey.DirectionsGenericTrain) ?: "train"
            }
        }

        if (result == null) {
            // Un-named way, so use "ref" (a route number e.g. "A81"/"M8") if we have one, since
            // that's how many trunk/motorway ways are actually signposted, otherwise fall back to
            // the "class" property. A ref identifies the road just as well as a name, so it isn't
            // a generic description.
            val wayRef = ref
            if (wayRef != null) {
                result = wayRef
                genericName = false
            } else {
                // Un-named way, so use "class" property
                result = genericClassName(strings)
            }

            result = result.replaceFirstChar {
                if (it.isLowerCase())
                    it.titlecase()
                else
                    it.toString()
            }

            // A confected name (a pavement's parent road, or the water the way follows) describes
            // the way in full, so it replaces the generic class noun and skips the destination
            // handling below - "Path next to Allander Water to dead end" reads no better than
            // either half alone. Applying it here rather than leaving it for the next getName()
            // call to pick up off this.name means the very first callout for the way already says
            // it, instead of announcing a bare "Path" once and the real description afterwards.
            val confectedName = if (gridState != null) {
                confectNamesForRoad(this, gridState, strings)
            } else {
                null
            }
            if (confectedName != null) {
                result = confectedName
                genericName = false
            } else {
                if (direction != null) {
                    // Describe as 'towards'
                    destinationModifier = if (direction)
                        properties?.get("destination:forward")
                    else
                        properties?.get("destination:backward")

                    passesModifier = if (direction)
                        properties?.get("passes:forward")
                    else
                        properties?.get("passes:backward")
                    passesString = passesModifier?.toString()?.let {
                        strings?.resolveFeatureClass(it) ?: it
                    } ?: ""

                    if (destinationModifier == null) {
                        destinationModifier = if (direction)
                            properties?.get("dead-end:forward")
                        else
                            properties?.get("dead-end:backward")
                    }

                    if (destinationModifier != null) {
                        if (destinationModifier == "dead-end") {
                            if (noGenericDeadEnds)
                                return ""
                            destinationModifier = strings?.getOrNull(StringKey.ConfectNameDeadEnd) ?: "dead end"
                        }

                        return if (passesString.isNotEmpty()) {
                            strings?.getOrNull(
                                StringKey.ConfectNameToVia,
                                result,
                                destinationModifier,
                                passesString
                            )
                                ?: "$result to $destinationModifier via $passesString"
                        } else {
                            strings?.getOrNull(StringKey.ConfectNameTo, result, destinationModifier)
                                ?: "$result to $destinationModifier"
                        }
                    }
                } else {
                    val start = properties?.get("destination:backward")
                    val end = properties?.get("destination:forward")

                    if ((end != null) and (start != null)) {
                        return strings?.getOrNull(StringKey.ConfectNameJoins, result, start, end)
                            ?: "$result that joins $start and $end"
                    }
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
            if (destinationModifier == "dead-end") {
                destinationModifier = strings?.getOrNull(StringKey.ConfectNameDeadEnd) ?: "dead end"
            }
            return if (passesString.isNotEmpty()) {
                strings?.getOrNull(
                    StringKey.ConfectNameToVia,
                    result,
                    destinationModifier,
                    passesString
                )
                    ?: "$result to $destinationModifier via $passesString"
            } else {
                strings?.getOrNull(StringKey.ConfectNameTo, result, destinationModifier)
                    ?: "$result to $destinationModifier"
            }
        } else {
            return if (passesString.isNotEmpty()) {
                strings?.getOrNull(StringKey.ConfectNameVia, result, passesString)
                    ?: "$result via $passesString"
            } else {
                // This is a path/service/track with no other qualifiers, so just return the name
                // unless we're looking for a non-generic name.
                if (nonGenericOnly && genericName) {
                    ""
                } else {
                    result
                }
            }
        }
    }

    fun doesIntersect(other: Way): Pair<Intersection?, Int> {
        for ((ourIndex, ours) in intersections.withIndex()) {
            if (ours == null) continue
            for (theirs in other.intersections) {
                if (theirs == null) continue
                // Check for direct intersection first
                if (ours == theirs)
                    return Pair(ours, ourIndex)
            }
        }

        for ((ourIndex, ours) in intersections.withIndex()) {
            if (ours == null) continue
            for (theirs in other.intersections) {
                if (theirs == null) continue
                // Check for tile-edge joiner
                if ((ours.intersectionType == IntersectionType.TILE_EDGE) &&
                    (theirs.intersectionType == IntersectionType.TILE_EDGE)
                ) {
                    for (member in ours.members) {
                        if (theirs.members.contains(member)) {
                            return Pair(theirs, ourIndex)
                        }
                    }
                }
            }
        }

        return Pair(null, 0)
    }

    /**
     * The generic, class-derived noun for an un-named way - "Path", "Track", "Service Road" -
     * localized and title-cased ready to start a description. Shared with addWaterAdjacency (see
     * WayNaming.kt), which builds "&lt;noun&gt; next to &lt;water&gt;" out of it.
     */
    fun genericClassName(strings: LocalizedStrings?): String {
        val noun = featureClass?.let { strings?.resolveFeatureClass(it) } ?: featureClass ?: ""
        return noun.replaceFirstChar {
            if (it.isLowerCase())
                it.titlecase()
            else
                it.toString()
        }
    }

    fun isSidewalkOrCrossing(): Boolean {
        val footway = properties?.get("footway")
        val bicycle = properties?.get("bicycle")
        return ((footway == "sidewalk") ||
                (footway == "crossing") ||
                (bicycle == "designated") ||
                ((featureType == "highway") && (featureValue == "cycleway")))
    }

    /**
     * True for footways/paths/bridleways/cycleways - the same classification used to keep these
     * out of TreeId.ROADS (see generateWays' roadsOnlyWaysCollection split below). A car/bus can
     * never legitimately be on one of these, so map-matching should never select one as the
     * matched way while in vehicle mode, even if it's still being tracked as a follower (e.g.
     * because it was extended into during a momentary low-speed reading).
     */
    fun isPath(): Boolean {
        return (featureType == "highway") &&
                (featureValue in setOf("footway", "path", "bridleway", "cycleway"))
    }

    fun endsAtTileEdge(): Boolean {
        return (intersections[WayEnd.START.id]?.intersectionType == IntersectionType.TILE_EDGE) ||
                (intersections[WayEnd.END.id]?.intersectionType == IntersectionType.TILE_EDGE)
    }

    /**
     * isSidewalkConnector returns true if this way is joining mainWay from intersection to its
     * own sidewalk e.g. https://www.openstreetmap.org/way/958596881. If we are map matched to the
     * sidewalk, but calling out from the perspective of mainWay, these connectors are not useful.
     */
    fun isSidewalkConnector(
        intersection: Intersection,
        mainWay: Way?,
        gridState: GridState,
        strings: LocalizedStrings?,
    ): Boolean {

        // It's not a connector if the mainWay isn't named
        if (mainWay == null)
            return false

        // It's not a connector if it's named
        if (name != null)
            return false

        // It's not a connector if it's more than 20m long, or it ends in a TILE_EDGE
        if ((length > 20.0) || endsAtTileEdge())
            return false

        // Look at the other end and check if it connects to a sidewalk associated with the mainWay
        getOtherIntersection(intersection)?.let { otherIntersection ->
            for (way in otherIntersection.members) {
                if (way == this) continue
                if (isSidewalkOrCrossing()) {
                    // This does connect to something that isn't a sidewalk, so it's not a simple
                    // connector i.e. it may connect to a sidewalk, but it goes further.
                    return false
                } else if (way.properties?.get("pavement") == null) {
                    confectNamesForRoad(way, gridState, strings)
                }
                // And then return true if it's the pavement for this Way
                val pavement = way.properties?.get("pavement")
                return ((pavement != null) && (pavement == mainWay.name))
            }
        }
        return false
    }

    fun followWays(
        fromIntersection: Intersection,
        ways: MutableList<Pair<Boolean, Way>>,
        depth: Int = 0,
        optionalEarlyPredicate: ((Way, Way?) -> Boolean)? = null
    ) {

        if (depth > 15) {
            // Break out at arbitrarily deep following.
            return
        }

        if (optionalEarlyPredicate != null) {
            if (wayType != WayType.JOINER) {
                if (optionalEarlyPredicate(this, ways.lastOrNull()?.second))
                    return
            }
        }

        for (existingWay in ways) {
            if (this == existingWay.second) {
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

        if (nextIntersection?.members?.size == 2) {
            // We have a next intersection and it's only got 2 ways, so follow it onwards
            for (way in nextIntersection.members) {
                if (way != this) {
                    way.followWays(nextIntersection, ways, depth + 1, optionalEarlyPredicate)
                }
            }
        }
    }

    /** isLoopedBack is used to determine if a Way starts and ends at the same intersection.
     * @return true if the Way starts and ends at the same intersection, false otherwise
     */
    fun isLoopedBack(): Boolean {
        return (intersections[WayEnd.START.id] == intersections[WayEnd.END.id])
    }

    /** direction returns the integer direction (0-7) indicating which direction the way is relative
     * to the device heading.
     * @param fromIntersection is the intersection which the way is part of and from which we want
     * the direction to be calculated
     * @param deviceHeading is the heading relative to which the direction is calculated
     * @return the Direction indicating which direction the way is relative to the device heading
     */
    fun direction(fromIntersection: Intersection, deviceHeading: Double): Direction {
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
    fun heading(fromIntersection: Intersection): Double {
        val nextLocation = if (fromIntersection == intersections[WayEnd.START.id])
            (geometry as LineString).coordinates.drop(1).first()
        else
            (geometry as LineString).coordinates.dropLast(1).last()

        return bearingFromTwoPoints(fromIntersection.location, nextLocation)
    }

    fun containsIntersection(intersection: Intersection): Boolean {
        return intersections.contains(intersection)
    }

    fun getOtherIntersection(fromIntersection: Intersection): Intersection? {
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
    fun createTemporaryIntersectionAndWays(location: LngLatAlt, ruler: Ruler): Intersection {
        val newIntersection = Intersection()
        newIntersection.location = location

        val point = ruler.distanceToLineString(location, geometry as LineString)

        // Create two line strings out of the original line, adding in the location in the middle
        val line1 = LineString()
        val line2 = LineString()
        line2.coordinates.add(location)
        var length1 = 0.0
        var length2 = 0.0
        for (coordinate in (geometry as LineString).coordinates.withIndex()) {
            if (coordinate.index <= point.index) {
                if (coordinate.index > 0) {
                    length1 += ruler.distance(line1.coordinates.last(), coordinate.value)
                }
                line1.coordinates.add(coordinate.value)
            } else {
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

        // Divide any along-way features between the two halves, re-basing the second half's
        // distances onto its own new START. Routing (the only current user of these temporary
        // Ways) doesn't read them, but leaving them behind would silently mislead any future
        // caller which does.
        for (feature in alongWayFeatures) {
            if (feature.distanceFromStart <= length1) {
                newWay1.addAlongWayFeature(feature)
            } else {
                newWay2.addAlongWayFeature(
                    feature.copy(distanceFromStart = feature.distanceFromStart - length1)
                )
            }
        }

        if (length1 > length2) {
            newIntersection.members.add(newWay2)
            newIntersection.members.add(newWay1)        // Sort these based on length
        } else {
            newIntersection.members.add(newWay1)
            newIntersection.members.add(newWay2)        // Sort these based on length
        }

        val startIntersection = intersections[WayEnd.START.id]
        if (startIntersection != null) {
            startIntersection.members.add(newWay1)
            startIntersection.members =
                startIntersection.members.sortedBy { way ->
                    way.length
                }.toMutableList()
        }

        val endIntersection = intersections[WayEnd.END.id]
        if (endIntersection != null) {
            endIntersection.members.add(newWay2)
            endIntersection.members =
                endIntersection.members.sortedBy { way ->
                    way.length
                }.toMutableList()
        }

        return newIntersection
    }

    fun removeIntersection(intersection: Intersection) {
        // The passed in intersection has two member ways - one in each direction. Remove them from
        // the intersection at the other end.
        val startIntersection = intersections[WayEnd.START.id]
        if (startIntersection != null) {
            startIntersection.members.remove(intersection.members[0])
            startIntersection.members.remove(intersection.members[1])
        }

        val endIntersection = intersections[WayEnd.END.id]
        if (endIntersection != null) {
            endIntersection.members.remove(intersection.members[0])
            endIntersection.members.remove(intersection.members[1])
        }

        intersection.members.clear()
    }
}

fun convertBackToTileCoordinates(
    location: LngLatAlt,
    tileZoom: Int
): Pair<Int, Int> {


    val x = ((location.longitude + 180.0) / 360.0) * (1 shl tileZoom)
    val y = (1 shl tileZoom) * (1.0 - asinh(tan(toRadians(location.latitude))) / PI) / 2

    val xInt = (abs(x - truncate(x)) * 4096).toInt()
    val yInt = (abs(y - truncate(y)) * 4096).toInt()

    return Pair(xInt, yInt)
}

class WayGenerator(val transit: Boolean = false) {

    /**
     * highwayPoints is a sparse map which maps from a location within the tile to a list of
     * lines which have nodes at that point. Every node on any `transportation` line will appear in the
     * map and if after processing all of the lines there's an intersection at that point, the map
     * entry will have information for more than one line.
     */
    private val highwayNodes: HashMap<Int, Int> = hashMapOf()
    private val wayFeatures = mutableListOf<MvtFeature>()

    private val ways = mutableListOf<Way>()

    private val intersections: HashMap<LngLatAlt, Intersection> = hashMapOf()

    /**
     * Turns the crossings found by MvtToGeoJson.extractCrossings into AlongWayFeatures on the Ways
     * they belong to. Must be called after [generateWays], since the distance along a Way is only
     * meaningful once the parent feature has been split into Ways with their own geometry.
     *
     * The crossings arrive keyed by the road's osmId, which several Ways share once that road has
     * been split at its intersections - so each one goes onto the single piece it actually lies
     * on, found by projecting the crossing point onto each candidate. Marking them all would put
     * the same crossing at a distance clamped to the end of every other piece, which the along-way
     * queries (see nextAlongWayFeature) would read as a real position. Reaching a crossing that is
     * on the Way ahead is the graph walk's job, not this step's.
     */
    internal fun attachCrossings(
        crossingsByOsmId: Map<Long, MutableList<CrossingInfo>>,
        ruler: Ruler
    ) {
        if (crossingsByOsmId.isEmpty()) return
        val waysByOsmId = ways.groupBy { it.osmId }
        for ((osmId, crossings) in crossingsByOsmId) {
            val candidates = waysByOsmId[osmId] ?: continue
            for (crossing in crossings) {
                val way = candidates.minByOrNull { candidate ->
                    val line = candidate.geometry as? LineString
                    if ((line == null) || (line.coordinates.size < 2)) {
                        Double.MAX_VALUE
                    } else {
                        ruler.distanceToLineString(crossing.point, line).distance
                    }
                } ?: continue
                way.addAlongWayFeature(
                    AlongWayFeature(
                        distanceFromStart = way.distanceAlongWay(crossing.point, ruler),
                        point = crossing.point,
                        kind = crossing.kind,
                        name = crossing.name,
                        position = crossing.position
                    )
                )
            }
        }
    }

    /**
     * addLine is called for any line feature that is being added to the FeatureCollection.
     * @param line is a new `transportation` layer line to add to the map
     *
     */
    fun addLine(line: ArrayList<Pair<Int, Int>>) {
        for (point in line) {
            if ((point.first < 0) || (point.first > 4095) ||
                (point.second < 0) || (point.second > 4095)
            ) {
                continue
            }

            // Rather than have a 2D sparse array, turn the coordinates into a single int so that we
            // can have a 1D sparse array instead.
            val coordinateKey = point.first.shl(12) + point.second
            val currentCount = highwayNodes[coordinateKey]
            if (currentCount == null) {
                highwayNodes[coordinateKey] = 1
            } else {
                highwayNodes[coordinateKey] = currentCount + 1
            }
        }
    }

    fun addFeature(feature: MvtFeature) {
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
    fun addSegmentFeatureToWay(
        feature: MvtFeature,
        currentSegment: LineString,
        currentSegmentLength: Double,
        segmentIndex: Int,
        way: Way
    ) {
        // Add feature with the segment up until this point
        val newProperties = hashMapOf<String, Any?>()
        feature.properties?.let { properties ->
            for ((key, prop) in properties) {
                newProperties[key] = prop
            }
            newProperties["segmentIndex"] = segmentIndex.toString()
        }
        way.copyProperties(feature)
        way.properties = newProperties
        way.geometry = currentSegment
        way.length = currentSegmentLength
    }

    fun generateWays(
        intersectionCollection: FeatureCollection?,
        mainWaysCollection: FeatureCollection,
        roadsOnlyWaysCollection: FeatureCollection?,
        leftOverCollection: FeatureCollection,
        intersectionMap: HashMap<LngLatAlt, Intersection>?,
        xTile: Int,
        yTile: Int,
        tileZoom: Int
    ) {

        // Calculated tile limits
        val topLeft = getLatLonTileWithOffset(xTile, yTile, tileZoom, 0.0, 0.0)
        val bottomRight = getLatLonTileWithOffset(xTile, yTile, tileZoom, 1.0, 1.0)

        val ruler = topLeft.createCheapRuler()

        for (feature in wayFeatures) {
            if (feature.geometry.type == "LineString") {
                val line = feature.geometry as LineString
                var currentWay = Way()
                var currentSegment = LineString()
                var currentSegmentLength = 0.0
                var segmentIndex = 0
                var coordinateKey: Int
                var tileEdge = false
                for (coordinate in line.coordinates) {

                    tileEdge =
                        (coordinate.latitude == topLeft.latitude) or
                                (coordinate.longitude == topLeft.longitude) or
                                (coordinate.latitude == bottomRight.latitude) or
                                (coordinate.longitude == bottomRight.longitude)

                    if (tileEdge and (currentSegment.coordinates.isEmpty())) {
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

                    if (currentSegment.coordinates.isNotEmpty()) {
                        // Add the length of the new segment
                        currentSegmentLength += ruler.distance(
                            currentSegment.coordinates.last(),
                            coordinate
                        )
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
                            if (intersection == null) {
                                intersection = Intersection()
                                intersection.name = ""
                                intersection.location = coordinate
                                intersection.intersectionType = IntersectionType.REGULAR
                                intersections[coordinate] = intersection
                            }

                            if (currentSegment.coordinates.size > 1) {
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

                if (currentSegment.coordinates.size > 1) {
                    addSegmentFeatureToWay(
                        feature,
                        currentSegment,
                        currentSegmentLength,
                        segmentIndex,
                        currentWay
                    )
                    ways.add(currentWay)
                    // Add completed way to intersection at start if there is one
                    currentWay.intersections[WayEnd.START.id]?.members?.add(currentWay)
                    if (tileEdge) {
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
        for (way in ways) {
            when (way.geometry.type) {
                "LineString", "MultiLineString" -> {
                    if (roadsOnlyWaysCollection != null) {
                        if (way.featureType == "highway") {
                            when (way.featureValue) {
                                "bus_stop", "crossing" -> {} // Don't add
                                "footway", "path", "bridleway", "cycleway" -> {
                                    // These are paths
                                    mainWaysCollection.addFeature(way)
                                }

                                else -> {
                                    // These are roads
                                    mainWaysCollection.addFeature(way)
                                    roadsOnlyWaysCollection.addFeature(way)
                                }
                            }
                        } else {
                            leftOverCollection.addFeature(way)
                        }
                    } else {
                        mainWaysCollection.addFeature(way)
                    }
                }

                else -> leftOverCollection.addFeature(way)
            }
        }
        for (intersection in intersections) {

            // Sort the members by length of the Way, shortest first. This is important for when we
            // traverse the graph using the Dijkstra algorithm.
            intersection.value.members = intersection.value.members.sortedBy { way ->
                way.length
            }.toMutableList()

            // Naming the intersection is now done as a separate pass after the name confection has
            // taken place
            //intersection.value.updateName()
            intersection.value.geometry = Point(intersection.value.location)
            intersection.value.properties = hashMapOf()
            if (transit) {
                intersection.value.featureType = "transit"
                intersection.value.featureValue = "transit_intersection"
            } else {
                intersection.value.featureType = "highway"
            }
            if (!transit) {
                if (intersectionCollection != null) {
                    if (intersection.value.intersectionType != IntersectionType.TILE_EDGE)
                        intersectionCollection.addFeature(intersection.value)
                }
            }
            // The map is needed regardless of transit/road, so that GridState can stitch Ways
            // across tile boundaries for both networks.
            if (intersectionMap != null)
                intersectionMap[intersection.key] = intersection.value
        }
    }
}
