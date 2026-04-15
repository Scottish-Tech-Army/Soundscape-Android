package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler

import android.content.Context
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Polygon as JtsPolygon
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import vector_tile.VectorTile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Math.toDegrees
import java.util.zip.GZIPInputStream
import kotlin.collections.iterator
import kotlin.collections.toTypedArray
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.sinh
import kotlin.math.tan
/**
 * Parses out the super category Features contained in the Points of Interest (POI) Feature Collection.
 * @param superCategory
 * String for super category. Options are "information", "object", "place", "landmark", "mobility", "safety"
 * @param poiFeatureCollection
 * POI Feature Collection for a tile.
 * @return a Feature Collection object containing only the Features from the super category.
 */
fun getPoiFeatureCollectionBySuperCategory(
    superCategory: SuperCategoryId,
    poiFeatureCollection: FeatureCollection
): FeatureCollection {

    val features = poiFeatureCollection.features.filter {
        feature -> (feature as MvtFeature).superCategory == superCategory
    }

    val tempFeatureCollection = FeatureCollection()
    tempFeatureCollection.features += features

    return tempFeatureCollection
}

fun featureIsInFilterGroup(feature: Feature, filter: String): Boolean {

    val tags = when(filter) {
        "transit" -> listOf("bus_stop", "train_station", "tram_stop", "ferry_terminal", "station")
        "food_and_drink" -> listOf(
            "restaurant", "fast_food", "cafe", "bar", "ice_cream", "pub", "coffee_shop")
        "parks" -> listOf(
            "park", "garden", "green_space", "recreation_area", "playground", "nature_reserve",
            "botanical_garden", "public_garden", "field", "reserve"
        )
        "groceries" -> listOf("supermarket", "convenience", "grocery")
        "banks" -> listOf("bank", "atm")
        else -> emptyList()
    }
    if(tags.isEmpty()) return true

    for (tag in tags) {
        val mvtFeature = feature as MvtFeature
        if (mvtFeature.featureValue == tag)
            return true
    }
    return false
}


/** isDuplicateByOsmId returns true if the OSM id for the feature has already been entered into
 * the existingSet. It returns false if it's the first time, or there's no OSM id.
 */
fun isDuplicateByOsmId(existingSet : MutableSet<Any>, feature : MvtFeature) : Boolean {
    val osmId = feature.osmId
    if(existingSet.contains(osmId))
        return true
    existingSet.add(osmId)
    return false
}

/** processFeatureCollection goes through the feature collection from a tile and adds it to the
 * feature collection for the grid, deduplicating by OSM is as it goes.
 */
fun deduplicateFeatureCollection(outputFeatureCollection: FeatureCollection,
                                 inputFeatureCollection: FeatureCollection?,
                                 existingSet : MutableSet<Any>) {
    inputFeatureCollection?.let { collection ->
        for (feature in collection.features) {
            if (!isDuplicateByOsmId(existingSet, feature as MvtFeature)) {
                outputFeatureCollection.features.add(feature)
            }
        }
    }
}

/**
 * Given a FeatureCollection checks for duplicate OSM IDs and removes them.
 * @param featureCollection
 * A Feature Collection.
 * @return a Feature Collection object with Features with duplicate osm ids removed.
 */
fun removeDuplicateOsmIds(
    featureCollection: FeatureCollection
): FeatureCollection{
    val processedOsmIds = mutableSetOf<Any>()
    val tempFeatureCollection = FeatureCollection()

    deduplicateFeatureCollection(tempFeatureCollection, featureCollection, processedOsmIds)

    return tempFeatureCollection
}

fun getFovTriangle(userGeometry: UserGeometry, forceLocation: Boolean = false) : Triangle {
    val heading = userGeometry.snappedHeading() ?: 0.0
    val quadrant = Quadrant(heading)
    val location = if(forceLocation) userGeometry.location
        else if(userGeometry.mapMatchedLocation != null) userGeometry.mapMatchedLocation.point
        else userGeometry.location

    return Triangle(location,
        getDestinationCoordinate(
            location,
            quadrant.left,
            userGeometry.fovDistance
        ),
        getDestinationCoordinate(
            location,
            quadrant.right,
            userGeometry.fovDistance
        )
    )
}


/**
 * Given an array of Segments and some user geometry with the location and Field of View distance it
 * which represent the FoV triangles it will generate a FeatureCollection of triangles.
 * @param segments
 * An Array<Segment> of degrees to construct triangles
 * @param userGeometry
 * UserGeometry containing the location and Field of View distance
 * @return a Feature Collection containing triangles for the relative directions.
 */
fun makeTriangles(
    segments: Array<Segment>,
    userGeometry: UserGeometry
): FeatureCollection{

    val newFeatureCollection = FeatureCollection()
    for ((count, segment) in segments.withIndex()) {

        val aheadTriangle = createPolygonFromTriangle(
            Triangle(
                userGeometry.location,
                getDestinationCoordinate(userGeometry.location, segment.left, userGeometry.fovDistance),
                getDestinationCoordinate(userGeometry.location, segment.right, userGeometry.fovDistance)
            )
        )
        val featureAheadTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("Direction", count)
            it.properties = ars3
        }
        featureAheadTriangle.geometry = aheadTriangle
        newFeatureCollection.addFeature(featureAheadTriangle)
    }
    return newFeatureCollection
}

/**
 * A wrapper around:
 * getCombinedDirectionPolygons, getIndividualDirectionPolygons, getAheadBehindDirectionPolygons, getLeftRightDirectionPolygons
 * @param userGeometry
 * Location, heading and FOV distance
 * @param relativeDirectionType
 * Enum for the function you want to use
 * @return a Feature Collection containing triangles for the relative directions.
 */
fun getRelativeDirectionsPolygons(
    userGeometry: UserGeometry,
    relativeDirectionType: RelativeDirections
): FeatureCollection {

    val heading = userGeometry.heading() ?: 0.0
    val segments =
        when(relativeDirectionType){
            RelativeDirections.COMBINED -> getCombinedDirectionSegments(heading)
            RelativeDirections.INDIVIDUAL -> getIndividualDirectionSegments(heading)
            RelativeDirections.AHEAD_BEHIND -> getAheadBehindDirectionSegments(heading)
            RelativeDirections.LEFT_RIGHT -> getLeftRightDirectionSegments(heading)
        }

    return makeTriangles(segments, userGeometry)
}

fun checkWhetherIntersectionIsOfInterest(
    intersection: Intersection,
    testNearestRoad:Way?
): Int {
    //println("Number of roads that make up intersection ${intersectionNumber}: ${intersectionRoadNames.features.size}")
    if(testNearestRoad == null)
        return 0

    // We don't announce intersections with only 2 or fewer Ways
    if(intersection.members.size <= 2)
        return -1

    var needsFurtherChecking = 0
    val setOfNames = mutableListOf<String>()
    for (way in intersection.members) {
        val roadName = way.name
        val isMatch = testNearestRoad.name == roadName

        if (isMatch) {
            // Ignore the road we're on
        } else if(roadName == null) {
            // Give no points to ways named from their type
            // TODO: give negative points if it's also a dead end i.e. don't call out dead-end
            //  service roads? The current 'priority' isn't good enough, need a better way of
            //  classifying.
        }
        else {
            if(setOfNames.contains(roadName)) {
                // Don't increment the priority if the name is here for the second time
            } else {
                needsFurtherChecking++
                setOfNames.add(roadName)
            }
        }
    }
    return needsFurtherChecking
}

fun polygonFeaturesOverlap(feature1: Feature, feature2: Feature): Boolean {
    for(point in (feature1.geometry as Polygon).coordinates[0]) {
        if(polygonContainsCoordinates(point, (feature2.geometry as Polygon)))
            return true
    }
    return false
}


fun mergeAllPolygonsInFeatureCollection(
    polygonFeatureCollection: FeatureCollection
): FeatureCollection{

    // We return a FeatureCollection which contains all the points and lines in the original,
    // but with any duplicated polygons merged.
    val resultantFeatureCollection = FeatureCollection()

    // Create a HashMap of any polygons with the same osm_id. Each hash map entry contains a List
    // of FeatureCollections. Each FeatureCollections contains one or more polygons. When there's
    // more than one, they've been tested to see if they overlap.
    val features = hashMapOf<Any, MutableList<FeatureCollection> >()
    for (feature in polygonFeatureCollection.features) {
        if(feature.geometry.type == "Polygon") {
            val osmId = (feature as MvtFeature).osmId
            if (!features.containsKey(osmId)) {
                // This is the first feature with this osm_id
                features[osmId] = mutableListOf()
            }
            var foundOverlap = false
            for(featureCollection in features[osmId]!!) {
                for(existingFeature in featureCollection) {
                    if(polygonFeaturesOverlap(feature, existingFeature)) {
                        featureCollection.addFeature(feature)
                        foundOverlap = true
                        break
                    }
                }
            }
            if(!foundOverlap) {
                // We found no overlap, so create a new FeatureCollection for this feature
                val newFeatureCollection = FeatureCollection()
                newFeatureCollection.addFeature(feature)
                features[osmId]!!.add(newFeatureCollection)
            }
        } else {
            // Not a polygon, so just copy it over to our results
            resultantFeatureCollection.addFeature(feature)
        }
    }

    for(featureCollectionList in features) {
        // For each FeatureCollection merge any overlapping polygons. If there are no duplicates,
        // then the only Feature in the collection is returned.
        for(featureCollection in featureCollectionList.value) {
            var mergedFeature: Feature? = null
            for ((index, feature) in featureCollection.features.withIndex()) {
                val tempMergedFeature = mergedFeature
                mergedFeature = if (index == 0) {
                    feature
                } else {
                    mergePolygons(mergedFeature!!, feature)
                }
                if(mergedFeature == feature) {
                    if(tempMergedFeature != null)
                        resultantFeatureCollection.addFeature(tempMergedFeature)
                }
            }
            resultantFeatureCollection.addFeature(mergedFeature!!)
        }
    }
    return resultantFeatureCollection
}

fun polygonOuterRingToCoordinateArray(polygon: Polygon?, geometryFactory: GeometryFactory) : LinearRing? {
    return geometryFactory.createLinearRing(
        polygon?.coordinates?.firstOrNull()
            ?.map {
                position -> Coordinate(position.longitude, position.latitude)
            }?.toTypedArray()
    )
}

fun polygonInteriorRingsToCoordinateArray(polygon: Polygon?, geometryFactory: GeometryFactory) : Array<LinearRing>? {
    if(polygon == null) return null

    val result = mutableListOf<LinearRing>()
    val innerRings = polygon.getInteriorRings()
    for(ring in innerRings) {
        result.add(geometryFactory.createLinearRing(
                ring.map {
                    position -> Coordinate(position.longitude, position.latitude)
                }.toTypedArray()
            )
        )
    }
    return result.toTypedArray()
}

fun createJtsPolygonFromPolygon(polygon: Polygon?): JtsPolygon? {

    if(polygon == null) return null

    val geometryFactory = GeometryFactory()
    val outerRing = polygonOuterRingToCoordinateArray(polygon, geometryFactory)
    val innerRings = polygonInteriorRingsToCoordinateArray(polygon, geometryFactory)

    return geometryFactory.createPolygon(outerRing, innerRings)
}

fun mergePolygons(
    polygon1: Feature,
    polygon2: Feature
): Feature {

    val polygon1GeometryJTS = createJtsPolygonFromPolygon(polygon1.geometry as? Polygon)
    val polygon2GeometryJTS = createJtsPolygonFromPolygon(polygon2.geometry as? Polygon)

    // merge/union the polygons
    val mergedGeometryJTSInitial = polygon1GeometryJTS?.union(polygon2GeometryJTS)
    if(mergedGeometryJTSInitial is org.locationtech.jts.geom.MultiPolygon) {
        // If the merge resulted in a MultiPolygon, then we don't need to use it,
        // we just need to add both polygons. Return the second, and the caller
        // can add the first.
        return polygon2
    }

    val mergedGeometryJTS = mergedGeometryJTSInitial as JtsPolygon
    // create a new Polygon with a single outer ring using the coordinates from the JTS merged geometry
    val mergedPolygon = MvtFeature().also { feature ->
        feature.properties = polygon1.properties
        feature.type = "Feature"
        feature.copyProperties(polygon1 as MvtFeature)
        feature.geometry = Polygon().also { polygon ->
            //Convert JTS to GeoJSON coordinates
            // Start with exterior ring
            val outerRing = mergedGeometryJTS.exteriorRing.coordinates?.map { coordinate ->
                LngLatAlt(coordinate.x, coordinate.y)
            }?.let {
                arrayListOf(arrayListOf(*it.toTypedArray()))
            }
            polygon.coordinates = outerRing ?: arrayListOf()

            // Now process interior rings
            val ringCount = mergedGeometryJTS.numInteriorRing
            for(ring in 0 until ringCount) {
                val innerRing = mergedGeometryJTS.getInteriorRingN(ring).coordinates?.map { coordinate ->
                    LngLatAlt(coordinate.x, coordinate.y)
                }?.let {
                    arrayListOf(*it.toTypedArray())
                }
                if(innerRing != null) {
                    polygon.addInteriorRing(innerRing)
                }
            }
        }
    }
    return mergedPolygon
}

fun addSidewalk(currentRoad: Way,
                roadTree: FeatureTree,
                ruler: Ruler,
                localizedContext: Context? = null,
) : Boolean {

    var found = false
    if(currentRoad.isSidewalkOrCrossing()){
        if(currentRoad.properties?.containsKey("pavement") == true)
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
        for(road in startRoads) {
            if((road as Way).isSidewalkOrCrossing()) continue
            name = road.name
            if(name != null) {
                for (road2 in endRoads) {
                    if((road2 as Way).isSidewalkOrCrossing()) continue
                    if (road2.name == name) {
                        // The distance between the pavement and the road should be similar at both ends.
                        val delta = abs(
                            ruler.distanceToLineString(start, road.geometry as LineString).distance -
                            ruler.distanceToLineString(end, road2.geometry as LineString).distance
                        )
                        if((delta < 5.0) && (delta < currentRoad.length / 2)) {
                            found = true
                            break
                        }
                    }
                }
                if(found)
                    break
            }
        }

        if (found) {
            if (name != null) {
                val text = localizedContext?.getString(R.string.confect_name_pavement_next_to)
                    ?.format(name) ?: "Pavement next to $name"
                currentRoad.name = text
            } else {
                val text = localizedContext?.getString(R.string.confect_name_pavement)
                    ?.format(name) ?: "Pavement"
                currentRoad.name = text
            }
        }
        (currentRoad.properties ?: HashMap()).also { properties ->
            // Set the property on the map (either the existing one or the new one)
            if(found)
                properties["pavement"] = name.toString()
            else
                properties["pavement"] = ""

            // Assign the map back to poi.properties, which is crucial if it was initially null
            currentRoad.properties = properties
        }
    }
    return found
}
fun checkNearbyPoi(tree: FeatureTree,
                   location: LngLatAlt,
                   polygonPoiToCompare: Feature?,
                   ruler: Ruler) : Feature? {

    // Get the nearest 2 features so that we can exclude polygonPoiToCompare.
    // Otherwise we never find features within other Polygons like parks.
    val nearbyPois = tree.getNearestCollection(
        location = location,
        distance = 20.0,
        2,
        ruler = ruler
    )
    for(poi in nearbyPois) {
        // Return the startPoi so long as we haven't matched against the polygonEndPoi
        if (poi != polygonPoiToCompare) {
            return poi
        }
    }
    return null
}

fun addPoiDestinations(way: Way,
                       gridState: GridState) : Boolean {

    // We want to use the locations at the furthest extent of the way as the start and end points.
    val line = way.geometry as LineString
    var startLocation = line.coordinates.first()
    var endLocation = line.coordinates.last()

    val startIntersection = way.intersections[WayEnd.START.id]
    val endIntersection = way.intersections[WayEnd.END.id]
    if(startIntersection != null) {
        val waysFromStart = mutableListOf<Pair<Boolean, Way>>()
        way.followWays(startIntersection, waysFromStart)
        // When followWays from the start intersection will head towards the end of the line
        endLocation = if(waysFromStart.last().first)
            (waysFromStart.last().second.geometry as LineString).coordinates.last()
        else
            (waysFromStart.last().second.geometry as LineString).coordinates.first()
    }
    if(endIntersection != null) {
        val waysFromEnd = mutableListOf<Pair<Boolean, Way>>()
        way.followWays(endIntersection, waysFromEnd)
        // When followWays from the end intersection will head towards the start of the line
        startLocation = if(waysFromEnd.last().first)
            (waysFromEnd.last().second.geometry as LineString).coordinates.last()
        else
            (waysFromEnd.last().second.geometry as LineString).coordinates.first()
    }

    // Only add in destinations tag if they don't already exist
    val startDestinationAdded = way.properties?.get("destination:backward") != null
    val endDestinationAdded = way.properties?.get("destination:forward") != null

    if(startDestinationAdded && endDestinationAdded) return false

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
    if((polygonEndPoi != null) || (polygonStartPoi != null)) {
        if(polygonEndPoi != polygonStartPoi) {
            // The way crosses across a polygon boundary
            if(startPoi == null) startPoi = polygonStartPoi
            if(endPoi == null) endPoi = polygonEndPoi
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
        startPoi = checkNearbyPoi(placesAndLandmarkTree, startLocation, polygonEndPoi, gridState.ruler)
    if (endPoi == null)
        endPoi = checkNearbyPoi(placesAndLandmarkTree, endLocation, polygonStartPoi, gridState.ruler)

    val safetyTree = gridState.getFeatureTree(TreeId.SAFETY_POIS)
    if (startPoi == null) {
        startPoi = safetyTree.getContainingPolygons(startLocation).features.firstOrNull()
    }
    if (endPoi == null) {
        endPoi = safetyTree.getContainingPolygons(endLocation).features.firstOrNull()
    }

    var addedDestinations = false

    if(startPoi != endPoi) {
        if(!startDestinationAdded) {
            val startName = (startPoi as MvtFeature?)?.name
            if (startName != null) {
                way.setProperty("destination:backward", startName)
                addedDestinations = true
            }
        }
        if(!endDestinationAdded) {
            val endName = (endPoi as MvtFeature?)?.name
            if (endName != null) {
                way.setProperty("destination:forward", endName)
                addedDestinations = true
            }
        }
    }
    return addedDestinations
}

fun confectNamesForRoad(road: Way,
                        gridState: GridState) {

    // rtree searches take time and so we should avoid them where possible.

    val roadTree = gridState.getFeatureTree(TreeId.WAYS_SELECTION)
    val cycleway = (road.featureType == "highway")  && (road.featureValue == "cycleway")
    if ((road.name == null) || cycleway) {

        if (addSidewalk(road, roadTree, gridState.ruler)) {
            return
        }

        addPoiDestinations(road, gridState)
    }
}

fun setDestinationTag(
    way: Way,
    forwards: Boolean,
    tagValue: String,
    deadEnd: Boolean = false,
    brunnelOrStepsValue: String) {

    if(tagValue.isNotEmpty())
        way.setProperty("${if (deadEnd) "dead-end" else "destination"}:${if (forwards) "backward" else "forward"}", tagValue)
    if(brunnelOrStepsValue.isNotEmpty())
        way.setProperty("passes:${if (forwards) "backward" else "forward"}", brunnelOrStepsValue)
}

fun traverseIntersectionsConfectingNames(gridIntersections: HashMap<LngLatAlt, Intersection>,
                                         intersectionAccumulator:  HashMap<LngLatAlt, Intersection> = hashMapOf()) {
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
                if(road.isSidewalkOrCrossing())
                    continue

                val ways = mutableListOf<Pair<Boolean, Way>>()
                var brunnelOrStepsValue = ""
                road.followWays(intersection.value, ways) { way, _ ->
                    // Break out when the next way has a name and note if it passes a bridge,
                    // steps or a tunnel
                    if(way.featureSubClass == "steps") {
                        brunnelOrStepsValue = "steps"
                    } else if(way.properties?.get("brunnel") != null) {
                        brunnelOrStepsValue = way.properties?.get("brunnel").toString()
                    }

                    (way.name != null)
                }

                for(way in ways) {
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

fun decompressGzip(compressedData: ByteArray): ByteArray? {
    // Create a ByteArrayInputStream from the compressed data
    val byteArrayInputStream = ByteArrayInputStream(compressedData)
    var gzipInputStream: GZIPInputStream? = null
    val outputStream = ByteArrayOutputStream()

    try {
        // Wrap the ByteArrayInputStream with GZIPInputStream
        gzipInputStream = GZIPInputStream(byteArrayInputStream)

        // Buffer for reading decompressed data
        val buffer = ByteArray(1024) // Adjust buffer size as needed
        var len: Int

        // Read from GZIPInputStream and write to ByteArrayOutputStream
        while (gzipInputStream.read(buffer).also { len = it } > 0) {
            outputStream.write(buffer, 0, len)
        }

        return outputStream.toByteArray()

    } catch (e: IOException) {
        // Handle potential IOExceptions during decompression
        e.printStackTrace() // Log the error or handle it appropriately
        return null
    } finally {
        // Ensure streams are closed
        try {
            gzipInputStream?.close()
            outputStream.close()
            byteArrayInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun decompressTile(compressionType: Byte?, rawTileData: ByteArray) : VectorTile.Tile? {
    //println("File reader got a tile for worker $workerIndex")
    when (compressionType) {
        1.toByte() -> {
            // No compression
            return VectorTile.Tile.parseFrom(rawTileData)
        }

        2.toByte() -> {
            // Gzip compression
            val decompressedTile = decompressGzip(rawTileData)
            return VectorTile.Tile.parseFrom(decompressedTile)
        }

        else -> assert(false)
    }
    return null
}
