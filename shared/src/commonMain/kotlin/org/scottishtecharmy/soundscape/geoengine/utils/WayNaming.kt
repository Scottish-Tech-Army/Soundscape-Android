package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.math.abs

fun addSidewalk(
    currentRoad: Way,
    roadTree: FeatureTree,
    ruler: Ruler,
    strings: LocalizedStrings?,
): Boolean {

    var found = false
    if (currentRoad.isSidewalkOrCrossing()) {
        if (currentRoad.properties?.containsKey("pavement") == true)
            return true

        val line = currentRoad.geometry as LineString
        val start = line.coordinates.first()
        val end = line.coordinates.last()

        val startRoads = roadTree.getNearestCollection(
            location = start,
            distance = 20.0,
            maxCount = 25,
            ruler = ruler
        )
        val endRoads = roadTree.getNearestCollection(
            location = end,
            distance = 20.0,
            maxCount = 25,
            ruler = ruler
        )
        // Find common road that's near the start and the end of our road - ignoring any sidewalks
        var name: Any? = null
        for (road in startRoads) {
            if ((road as Way).isSidewalkOrCrossing()) continue
            name = road.name
            if (name != null) {
                for (road2 in endRoads) {
                    if ((road2 as Way).isSidewalkOrCrossing()) continue
                    if (road2.name == name) {
                        // The distance between the pavement and the road should be similar at both ends.
                        val delta = abs(
                            ruler.distanceToLineString(
                                start,
                                road.geometry as LineString
                            ).distance -
                                    ruler.distanceToLineString(
                                        end,
                                        road2.geometry as LineString
                                    ).distance
                        )
                        if ((delta < 5.0) && (delta < currentRoad.length / 2)) {
                            found = true
                            break
                        }
                    }
                }
                if (found)
                    break
            }
        }

        if (found) {
            if (name != null) {
                val text = strings?.get(StringKey.ConfectNamePavementNextTo, name)
                    ?: "Pavement next to $name"
                currentRoad.name = text
            } else {
                val text = strings?.get(StringKey.ConfectNamePavement)
                    ?: "Pavement"
                currentRoad.name = text
            }
        }
        (currentRoad.properties ?: HashMap()).also { properties ->
            // Set the property on the map (either the existing one or the new one)
            if (found)
                properties["pavement"] = name.toString()
            else
                properties["pavement"] = ""

            // Assign the map back to poi.properties, which is crucial if it was initially null
            currentRoad.properties = properties
        }
    }
    return found
}

fun checkNearbyPoi(
    tree: FeatureTree,
    location: LngLatAlt,
    polygonPoiToCompare: Feature?,
    ruler: Ruler
): Feature? {

    // Get the nearest 2 features so that we can exclude polygonPoiToCompare.
    // Otherwise we never find features within other Polygons like parks.
    val nearbyPois = tree.getNearestCollection(
        location = location,
        distance = 20.0,
        2,
        ruler = ruler
    )
    for (poi in nearbyPois) {
        // Return the startPoi so long as we haven't matched against the polygonEndPoi
        if (poi != polygonPoiToCompare) {
            return poi
        }
    }
    return null
}

fun addPoiDestinations(
    way: Way,
    gridState: GridState,
    strings: LocalizedStrings?,
): Boolean {

    // We want to use the locations at the furthest extent of the way as the start and end points.
    val line = way.geometry as LineString
    var startLocation = line.coordinates.first()
    var endLocation = line.coordinates.last()

    val startIntersection = way.intersections[WayEnd.START.id]
    val endIntersection = way.intersections[WayEnd.END.id]
    if (startIntersection != null) {
        val waysFromStart = mutableListOf<Pair<Boolean, Way>>()
        way.followWays(startIntersection, waysFromStart)
        // When followWays from the start intersection will head towards the end of the line
        endLocation = if (waysFromStart.last().first)
            (waysFromStart.last().second.geometry as LineString).coordinates.last()
        else
            (waysFromStart.last().second.geometry as LineString).coordinates.first()
    }
    if (endIntersection != null) {
        val waysFromEnd = mutableListOf<Pair<Boolean, Way>>()
        way.followWays(endIntersection, waysFromEnd)
        // When followWays from the end intersection will head towards the start of the line
        startLocation = if (waysFromEnd.last().first)
            (waysFromEnd.last().second.geometry as LineString).coordinates.last()
        else
            (waysFromEnd.last().second.geometry as LineString).coordinates.first()
    }

    // Only add in destinations tag if they don't already exist
    val startDestinationAdded = way.properties?.get("destination:backward") != null
    val endDestinationAdded = way.properties?.get("destination:forward") != null

    if (startDestinationAdded && endDestinationAdded) return false

    // Does the unnamed way start or end near a Marker?
    val markerTree = gridState.markerTree
    var startPoi = markerTree?.getNearestFeature(
        location = startLocation,
        distance = 20.0,
        ruler = gridState.ruler
    )
    var endPoi = markerTree?.getNearestFeature(
        location = endLocation,
        distance = 20.0,
        ruler = gridState.ruler
    )

    // Does the unnamed way start or end near inside a POI? If we don't do this check, we can end
    // up with confusing confections inside parks where a path is described "to Park" when the
    // whole path is within the park, but one end is nearer the edge of it.
    val poiTree = gridState.getFeatureTree(TreeId.POIS)
    val polygonStartPoi = poiTree.getContainingPolygons(startLocation).features.firstOrNull()
    val polygonEndPoi = poiTree.getContainingPolygons(endLocation).features.firstOrNull()
    if ((polygonEndPoi != null) || (polygonStartPoi != null)) {
        if (polygonEndPoi != polygonStartPoi) {
            // The way crosses across a polygon boundary
            if (startPoi == null) startPoi = polygonStartPoi
            if (endPoi == null) endPoi = polygonEndPoi
        }
    }

    // Does the unnamed way start or end near an entrance? These should take priority over other
    // types of POI as they are likely the most useful
    val entrancesTree = gridState.getFeatureTree(TreeId.ENTRANCES)
    if (startPoi == null)
        startPoi = checkNearbyPoi(entrancesTree, startLocation, polygonEndPoi, gridState.ruler)
    if (endPoi == null)
        endPoi = checkNearbyPoi(entrancesTree, endLocation, polygonStartPoi, gridState.ruler)

    // Does the unnamed way start or end near a Landmark or a place?
    val placesAndLandmarkTree = gridState.getFeatureTree(TreeId.PLACES_AND_LANDMARKS)
    if (startPoi == null)
        startPoi =
            checkNearbyPoi(placesAndLandmarkTree, startLocation, polygonEndPoi, gridState.ruler)
    if (endPoi == null)
        endPoi =
            checkNearbyPoi(placesAndLandmarkTree, endLocation, polygonStartPoi, gridState.ruler)

    val safetyTree = gridState.getFeatureTree(TreeId.SAFETY_POIS)
    if (startPoi == null) {
        startPoi = safetyTree.getContainingPolygons(startLocation).features.firstOrNull()
    }
    if (endPoi == null) {
        endPoi = safetyTree.getContainingPolygons(endLocation).features.firstOrNull()
    }

    var addedDestinations = false

    if (startPoi != endPoi) {
        if (!startDestinationAdded) {
            val startName = (startPoi as MvtFeature?)?.getText(strings)?.text
            if (!startName.isNullOrEmpty()) {
                way.setProperty("destination:backward", startName)
                addedDestinations = true
            }
        }
        if (!endDestinationAdded) {
            val endName = (endPoi as MvtFeature?)?.getText(strings)?.text
            if (!endName.isNullOrEmpty()) {
                way.setProperty("destination:forward", endName)
                addedDestinations = true
            }
        }
    }
    return addedDestinations
}


// How close to the water the way has to be to count as running beside it. Measured against the real
// geometry around Milngavie: the path round Craigmaddie Reservoir sits 10-24m from the shore and the
// Allander Water path 11-38m from the river, while widening this starts pulling in ways that merely
// pass near a burn rather than follow it.
private const val WATER_ADJACENCY_DISTANCE_METRES = 25.0

// How much of the way has to be within that distance. This is an absolute length rather than a
// proportion because a path that follows a river for a couple of hundred metres and then turns
// inland is still "the path next to the river" - it's the length spent beside the water that makes
// the water worth naming, not how much of the path that happens to be. Calibrated against the
// riverside path at 55.931961,-4.305300, which runs 99m of its 124m within 25m of Allander Water.
private const val WATER_ADJACENCY_MIN_ALONGSIDE_METRES = 80.0

// A way shorter than the threshold above can still qualify, but then all of it has to be beside the
// water - there's no partial credit for a short way. Below this length it's not described this way
// at all: ways are split at every intersection, so a genuine riverside path arrives as several Ways,
// and for the shortest of those "next to the river" says less about where the way goes than the
// junction it joins.
private const val WATER_ADJACENCY_MIN_LENGTH_METRES = 30.0

// Sampling interval along the way. Fine enough to measure the length spent beside the water to
// within a few metres, coarse enough that even a kilometre of riverside path is ~100 distance
// calculations against a handful of candidates.
private const val WATER_ADJACENCY_SAMPLE_METRES = 10.0

/**
 * Distance from a point to the water's edge. A waterway is a LineString and measures directly; a
 * lake or reservoir is a Polygon, and it's the shore that matters, not the middle, so this measures
 * to the exterior ring. A path round a reservoir is 15m from the water but can easily be 300m from
 * its centroid, which no sane threshold would accept.
 */
private fun distanceToWaterEdge(point: LngLatAlt, water: Feature, ruler: Ruler): Double? {
    return when (val geometry = water.geometry) {
        is LineString -> ruler.distanceToLineString(point, geometry).distance
        is Polygon ->
            geometry.coordinates.minOfOrNull {
                ruler.distanceToLineString(point, LineString(it)).distance
            }
        is MultiPolygon ->
            geometry.coordinates.flatten().minOfOrNull {
                ruler.distanceToLineString(point, LineString(it)).distance
            }
        else -> null
    }
}

/**
 * Points spaced at most [WATER_ADJACENCY_SAMPLE_METRES] apart along the way, including both ends.
 * Sampling rather than using the way's own vertices matters because MVT geometry is simplified: a
 * long straight stretch beside a river can be a single segment with nothing in between to test.
 */
private fun sampleAlongLine(line: LineString, ruler: Ruler): List<LngLatAlt> {
    val samples = mutableListOf(line.coordinates.first())
    for (i in 0 until line.coordinates.size - 1) {
        val start = line.coordinates[i]
        val end = line.coordinates[i + 1]
        val segmentLength = ruler.distance(start, end)
        if (segmentLength <= 0.0) continue
        val steps = maxOf(1, (segmentLength / WATER_ADJACENCY_SAMPLE_METRES).toInt())
        val bearing = ruler.bearing(start, end)
        for (step in 1..steps) {
            samples.add(ruler.destination(start, segmentLength * step / steps, bearing))
        }
    }
    return samples
}

/**
 * "Path next to Allander Water" - the way's generic class noun joined to the water it follows. Also
 * assigns it, so a way that has already been matched comes back named however it's reached.
 */
private fun nameForWaterside(way: Way, waterName: String, strings: LocalizedStrings?): String {
    val noun = way.genericClassName(strings)
    val text = strings?.get(StringKey.ConfectNameNextTo, noun, waterName)
        ?: "$noun next to $waterName"
    way.name = text
    return text
}

/**
 * Names an un-named way after the river, burn or loch it follows, e.g. "Path next to Allander
 * Water" for the path at 55.931961,-4.305300, or "Path next to Craigmaddie Reservoir" for the one
 * round the reservoir at 55.948076,-4.300997. OSM leaves these paths un-named, but they're known
 * locally by the water they follow, and "Path" on its own tells a user nothing.
 *
 * Being *near* water isn't enough - a road crossing a river is near it too. The way has to run
 * alongside it for a meaningful distance: at least [WATER_ADJACENCY_MIN_ALONGSIDE_METRES] of it (or
 * all of it, if it's shorter than that) must lie within [WATER_ADJACENCY_DISTANCE_METRES] of the
 * same water. A road crossing a river is only beside it for the few metres either side of the
 * bridge, and fails. A path that follows the bank for a couple of hundred metres and then turns
 * inland still passes, because what earns the water its mention is the distance spent beside it,
 * not the proportion of the path that represents.
 *
 * Candidates are grouped by name before testing, because one body of water is many features: a
 * river is split into a chain of LineStrings, and a loch spanning the grid arrives as several
 * tile-clipped polygons. Testing each fragment separately would let a path fail against every
 * fragment of the very river it runs along. Where more than one water qualifies, the one the way
 * follows furthest wins.
 *
 * Returns the confected name, or null if the way doesn't follow any water.
 */
fun addWaterAdjacency(
    way: Way,
    gridState: GridState,
    strings: LocalizedStrings?,
): String? {

    // Memoised like addSidewalk's "pavement" property, so the rtree searches below happen at most
    // once per way per grid however often the way is described. The water's name is memoised rather
    // than the finished text, because the text depends on the LocalizedStrings passed in and the
    // same way can be described by callers holding different ones.
    val memo = way.properties?.get("waterside") as? String
    if (memo != null) {
        if (memo.isEmpty()) return null
        return nameForWaterside(way, memo, strings)
    }

    val line = way.geometry as? LineString
    if ((line == null) || (line.coordinates.size < 2)) return null

    val ruler = gridState.ruler

    // A way carrying a route number is already identified by it, so "A81 next to River Kelvin"
    // would add noise rather than context - the same reasoning getName() uses to treat a ref as a
    // non-generic name.
    val wayLength = ruler.lineLength(line)
    if ((way.ref != null) || (wayLength < WATER_ADJACENCY_MIN_LENGTH_METRES)) {
        way.setProperty("waterside", "")
        return null
    }

    val samples = sampleAlongLine(line, ruler)

    // Gather the water to test against with a single search from the way's midpoint, wide enough to
    // reach anything the exact test below could accept: no point of the way is more than half the
    // way's length from its midpoint, so nothing within the adjacency distance of the way can fall
    // outside this circle. Searching once from the middle rather than per sample point keeps this
    // to one pruned rtree search per tree without having to argue about probe spacing, and the
    // candidates it over-collects are discarded by the measurement that follows. FeatureTree's own
    // getNearbyLine would be the natural fit but can't be used - entryNearLine doesn't handle
    // LineString entries, which is exactly what a waterway is.
    val midpoint = ruler.along(line, wayLength / 2)
    val searchRadius = (wayLength / 2) + WATER_ADJACENCY_DISTANCE_METRES
    val candidatesByName = mutableMapOf<String, MutableList<Feature>>()
    for (treeId in listOf(TreeId.NAMED_WATERWAYS, TreeId.NAMED_WATER_POLYGONS)) {
        val nearby = gridState.getFeatureTree(treeId).getNearbyCollection(
            location = midpoint,
            distance = searchRadius,
            ruler = ruler
        )
        for (feature in nearby) {
            val name = (feature as? MvtFeature)?.name ?: continue
            candidatesByName.getOrPut(name) { mutableListOf() }.add(feature)
        }
    }

    val required = minOf(WATER_ADJACENCY_MIN_ALONGSIDE_METRES, wayLength)
    var bestName: String? = null
    var bestAlongside = 0.0
    var bestMeanDistance = Double.MAX_VALUE
    for ((name, features) in candidatesByName) {
        val distances = samples.map { sample ->
            features.mapNotNull { distanceToWaterEdge(sample, it, ruler) }.minOrNull()
                ?: Double.MAX_VALUE
        }

        // The length of the way spent beside this water. A step counts only when both of its ends
        // are close, so a single sample that happens to fall near the water - the moment a road
        // crosses a river, say - contributes nothing.
        var alongside = 0.0
        for (index in 0 until samples.size - 1) {
            if ((distances[index] < WATER_ADJACENCY_DISTANCE_METRES) &&
                (distances[index + 1] < WATER_ADJACENCY_DISTANCE_METRES)
            ) {
                alongside += ruler.distance(samples[index], samples[index + 1])
            }
        }
        // The half-metre slack matters only for a way shorter than the alongside threshold, where
        // "required" is the way's own length and the summed sample steps can fall a hair short of
        // it through floating point.
        if (alongside + 0.5 < required) continue

        // Where several waters qualify, the one the way follows furthest wins, and the closer of
        // two it follows equally far - a path along a river bank that also clips the corner of a
        // pond is named after the river.
        val mean = distances.average()
        if ((alongside > bestAlongside) ||
            ((alongside == bestAlongside) && (mean < bestMeanDistance))
        ) {
            bestAlongside = alongside
            bestMeanDistance = mean
            bestName = name
        }
    }

    if (bestName == null) {
        way.setProperty("waterside", "")
        return null
    }

    val text = nameForWaterside(way, bestName, strings)
    way.setProperty("waterside", bestName)
    return text
}

/**
 * Confects a name for an un-named way, in descending order of how specifically the result
 * identifies it: the road a pavement runs beside, then the water the way follows, then the POIs at
 * either end. Water comes before POI destinations because a path that hugs a river for its whole
 * length is better described by the river than by whatever happens to sit at its two ends.
 *
 * Returns the name it confected, or null if it only added destination tags (which getName() folds
 * into the description itself) or found nothing to say.
 */
fun confectNamesForRoad(
    road: Way,
    gridState: GridState,
    strings: LocalizedStrings?,
): String? {

    // rtree searches take time and so we should avoid them where possible.

    val roadTree = gridState.getFeatureTree(TreeId.WAYS_SELECTION)
    val cycleway = (road.featureType == "highway") && (road.featureValue == "cycleway")
    if ((road.name == null) || cycleway) {

        if (addSidewalk(road, roadTree, gridState.ruler, strings)) {
            return road.name
        }

        addWaterAdjacency(road, gridState, strings)?.let { return it }

        addPoiDestinations(road, gridState, strings)
    }
    return null
}

fun setDestinationTag(
    way: Way,
    forwards: Boolean,
    tagValue: String,
    deadEnd: Boolean = false,
    brunnelOrStepsValue: String
) {

    if (tagValue.isNotEmpty())
        way.setProperty(
            "${if (deadEnd) "dead-end" else "destination"}:${if (forwards) "backward" else "forward"}",
            tagValue
        )
    if (brunnelOrStepsValue.isNotEmpty())
        way.setProperty("passes:${if (forwards) "backward" else "forward"}", brunnelOrStepsValue)
}

fun traverseIntersectionsConfectingNames(
    gridIntersections: HashMap<LngLatAlt, Intersection>,
    intersectionAccumulator: HashMap<LngLatAlt, Intersection> = hashMapOf()
) {
    // Go through every intersection and for any which have at least one named way, add
    // "destination tag" on it's un-named ways to indicate that they arrive there.
    for (intersection in gridIntersections) {
        // Add intersection to accumulator map
        intersectionAccumulator[intersection.key] = intersection.value

        // TODO: Perhaps we could use an intersection name here if there is more than one
        //  named way? e.g. Path to junction of Moor Road and Buchanan Street

        // Does the intersection have any named members?
        var namedRoadToUse: String? = null
        for (road in intersection.value.members) {
            if (namedRoadToUse == null) {
                namedRoadToUse = road.name
            }
        }
        // We've got a named road at this junction, so use if for any un-named roads
        for (road in intersection.value.members) {
            // Skip if the road is named
            if (road.name == null) {

                // We don't confect names for sidewalks or crossings as those will be named from the
                // adjacent road.
                if (road.isSidewalkOrCrossing())
                    continue

                val ways = mutableListOf<Pair<Boolean, Way>>()
                var brunnelOrStepsValue = ""
                road.followWays(intersection.value, ways) { way, _ ->
                    // Break out when the next way has a name and note if it passes a bridge,
                    // steps or a tunnel
                    if (way.featureSubClass == "steps") {
                        brunnelOrStepsValue = "steps"
                    } else if (way.properties?.get("brunnel") != null) {
                        brunnelOrStepsValue = way.properties?.get("brunnel").toString()
                    }

                    (way.name != null)
                }

                for (way in ways) {
                    setDestinationTag(
                        way.second,
                        way.first,
                        namedRoadToUse ?: "",
                        false,
                        brunnelOrStepsValue
                    )
                }
            }
        }
        // Check for dead ends
        for (road in intersection.value.members) {
            val ways = mutableListOf<Pair<Boolean, Way>>()
            road.followWays(intersection.value, ways)
            val way = ways.last()
            if ((way.first and (way.second.intersections[WayEnd.END.id] == null)) or
                (!way.first and (way.second.intersections[WayEnd.START.id] == null))
            ) {
                for (eachWay in ways) {
                    // We currently label all roads, even named ones, with Dead End
                    setDestinationTag(eachWay.second, !eachWay.first, "dead-end", true, "")
                }
            }
        }
    }
}
