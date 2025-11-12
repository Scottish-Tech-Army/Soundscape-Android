package org.scottishtecharmy.soundscape.geoengine.utils

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
import java.lang.Math.toDegrees
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
 * Gets Slippy Map Tile Name from GPS coordinates and Zoom (fixed at 16 for Soundscape).
 * @param location
 * Location in LngLatAlt
 * @param zoom
 * The zoom level.
 * @return a Pair(xTile, yTile).
 */
fun getXYTile(
    location: LngLatAlt,
    zoom: Int = 16
): Pair<Int, Int> {
    val latRad = toRadians(location.latitude)
    var xTile = floor((location.longitude + 180) / 360 * (1 shl zoom)).toInt()
    var yTile = floor((1.0 - asinh(tan(latRad)) / PI) / 2 * (1 shl zoom)).toInt()

    if (xTile < 0) {
        xTile = 0
    }
    if (xTile >= (1 shl zoom)) {
        xTile = (1 shl zoom) - 1
    }
    if (yTile < 0) {
        yTile = 0
    }
    if (yTile >= (1 shl zoom)) {
        yTile = (1 shl zoom) - 1
    }
    return Pair(xTile, yTile)
}

fun getLatLonTileWithOffset(
    xTile : Int,
    yTile : Int,
    zoom: Int,
    xOffset : Double,
    yOffset : Double,
): LngLatAlt {

    val x : Double = xTile.toDouble() + xOffset
    val y : Double = yTile.toDouble() + yOffset
    val lon : Double = ((x / (1 shl zoom)) * 360.0) - 180.0
    val latRad = atan(sinh(Math.PI * (1 - 2 * y / (1 shl zoom))))

    return LngLatAlt(lon, toDegrees(latRad))
}

/**
 * Parses out the super category Features contained in the Points of Interest (POI) Feature Collection.
 * @param superCategory
 * String for super category. Options are "information", "object", "place", "landmark", "mobility", "safety"
 * @param poiFeatureCollection
 * POI Feature Collection for a tile.
 * @return a Feature Collection object containing only the Features from the super category.
 */
fun getPoiFeatureCollectionBySuperCategory(
    superCategory: String,
    poiFeatureCollection: FeatureCollection
): FeatureCollection {

    val tempFeatureCollection = FeatureCollection()
    val superCategorySet = getSuperCategoryElements(superCategory)

    for (feature in poiFeatureCollection) {
        val mvtFeature = feature as MvtFeature
        if (superCategorySet.contains(mvtFeature.featureType) or superCategorySet.contains(mvtFeature.featureValue)) {
            tempFeatureCollection.addFeature(feature)
            feature.properties?.put("category", superCategory)
        }
    }
    return tempFeatureCollection
}

fun featureHasEntrances(feature: Feature): Boolean {
    return (feature.properties?.get("has_entrances") == "yes")
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

data class PointAndDistanceAndHeading(var point: LngLatAlt = LngLatAlt(),
                                      var distance: Double = Double.MAX_VALUE,
                                      var heading: Double = 0.0,
                                      var index: Int = -1,
                                      var positionAlongLine: Double = Double.NaN)


fun PointAndDistanceAndHeading.clone(): PointAndDistanceAndHeading {
    return PointAndDistanceAndHeading(point.clone(), distance, heading, index, positionAlongLine)
}

/**
 * Given a Feature and a location this will calculate the nearest distance to it
 * @param currentLocation
 * Current location as LngLatAlt
 * @param feature
 * @return The a PointAndDistance object which contains the distance between currentLocation and
 *  feature and the point to which the distance is measured.
 */
fun getDistanceToFeature(
    currentLocation: LngLatAlt,
    feature: Feature,
    ruler: Ruler
): PointAndDistanceAndHeading {
    when (feature.geometry.type) {
        "Point" -> {
            val point = feature.geometry as Point
            val distanceToFeaturePoint = ruler.distance(currentLocation,point.coordinates)
            val heading = ruler.bearing(currentLocation, point.coordinates)
            return PointAndDistanceAndHeading(
                point.coordinates,
                distanceToFeaturePoint,
                heading)
        }

        "MultiPoint" -> {
            val multiPoint = feature.geometry as MultiPoint
            var shortestDistance = Double.MAX_VALUE
            var nearestPoint = LngLatAlt()

            for (point in multiPoint.coordinates) {
                val distanceToPoint = ruler.distance(currentLocation, point)
                if (distanceToPoint < shortestDistance) {
                    shortestDistance = distanceToPoint
                    nearestPoint = point
                }
            }
            // this is the closest point to the current location from the collection of points
            val heading = ruler.bearing(currentLocation, nearestPoint)
            return PointAndDistanceAndHeading(nearestPoint, shortestDistance, heading)
        }

        "LineString" -> {
            val lineString = feature.geometry as LineString
            return ruler.distanceToLineString(currentLocation, lineString)
        }

        "MultiLineString" -> {
            val multiLineString = feature.geometry as MultiLineString
            var nearest = PointAndDistanceAndHeading()

            for (arrCoordinates in multiLineString.coordinates) {
                val segmentNearest = ruler.distanceToLineString(
                    currentLocation,
                    LineString(arrCoordinates)
                )
                if (segmentNearest.distance < nearest.distance) {
                    nearest = segmentNearest
                }
            }
            return nearest
        }

        "Polygon" -> {
            val polygon = feature.geometry as Polygon
            val nearestPoint = LngLatAlt()
            val distance = distanceToPolygon(
                currentLocation,
                polygon,
                ruler,
                nearestPoint
            )
            val heading = ruler.bearing(currentLocation, nearestPoint)
            return PointAndDistanceAndHeading(nearestPoint, distance, heading)
        }

        "MultiPolygon" -> {
            val multiPolygon = feature.geometry as MultiPolygon
            var shortestDistance = Double.MAX_VALUE
            var nearestPoint = LngLatAlt()

            for (arrCoordinates in multiPolygon.coordinates) {
                val pointOnPolygon = LngLatAlt()
                val distance = distanceToPolygon(
                    currentLocation,
                    Polygon(arrCoordinates[0]), // Use outer ring
                    ruler,
                    pointOnPolygon
                )
                if (distance < shortestDistance) {
                    shortestDistance = distance
                    nearestPoint = pointOnPolygon
                }
            }
            // this is the shortest distance from current location to the collection of Polygons
            val heading = ruler.bearing(currentLocation, nearestPoint)
            return PointAndDistanceAndHeading(nearestPoint, shortestDistance, heading)
        }

        else -> {
            println("Unknown type ${feature.geometry.type}")
            assert(false)
            return PointAndDistanceAndHeading()
        }
    }
}

/**
 * Given a Feature Collection and location this will calculate the nearest distance to each Feature,
 * and return a Feature Collection that contains the distance_to data for each Feature.
 * @param currentLocation
 * Current location as LngLatAlt.
 * @param featureCollection
 * @return a Feature Collection with the "distance_to" from the current location as a foreign member in meters for each Feature.
 */
fun getDistanceToFeatureCollection(
    currentLocation: LngLatAlt,
    featureCollection: FeatureCollection
): FeatureCollection {
    for (feature in featureCollection) {
        feature.properties?.put(
            "distance_to",
            getDistanceToFeature(currentLocation, feature, currentLocation.createCheapRuler()).distance
        )
    }
    return featureCollection
}

/**
 * Given a Feature Collection and location this will calculate the nearest distance to each Feature,
 * and return a sorted Feature Collection by the distance_to foreign member for each Feature.
 * @param currentLocation
 * Current location as LngLatAlt.
 * @param featureCollection
 * @return a sorted Feature Collection with the "distance_to" from the current location as a foreign member in meters for each Feature.
 */
fun sortedByDistanceTo(
    currentLocation: LngLatAlt,
    featureCollection: FeatureCollection
): FeatureCollection {

    val featuresWithDistance = getDistanceToFeatureCollection(
        currentLocation,
        featureCollection
    )
    val featuresSortedByDistanceList = featuresWithDistance.features
        .sortedBy {(it.properties?.get("distance_to") as? Number)?.toDouble() ?: Double.MAX_VALUE
        }
    // loop through the list of sorted Features and add to a new Feature Collection
    val featuresSortedByDistance = FeatureCollection()
    for (feature in featuresSortedByDistanceList) {
        featuresSortedByDistance.addFeature(feature)
    }
    return featuresSortedByDistance
}

/**
 * This is based on the original iOS COMBINED direction type which they described like this:
 *
 *  Ahead, Right, Behind, and Left all get a 150 degree window centered in their respective
 *  directions (e.g. right is 15 degrees to 165 degrees). In the areas where these windows
 *  overlap, the relative directions get combined. For example, 0 degrees is "ahead", while
 *  20 degrees is "ahead to the right."
 *
 *  However, this gives a very small Ahead window which leads to less good intersection
 *  descriptions, especially when all ahead/behind Left are reduced to Left. As a result, we've
 *  biased ahead over ahead left/right so that it's 60 degrees, and the ahead/behind left/right are
 *  only 30 degrees. This is basically shrinking the 150 degree window to 120 degrees.

 * @param heading
 * Direction the device is pointing in degrees
 * @return an array of Segments describing the angles
 */
fun getCombinedDirectionSegments(
    heading: Double
): Array<Segment> {
    return arrayOf(
            // "behind" triangle
            Segment(heading + 180.0, 60.0),
            // "behind left" triangle
            Segment(heading + 225.0, 30.0),
            // "left" triangle
            Segment(heading + 270.0, 60.0),
            // "ahead left" triangle
            Segment(heading + 315.0, 30.0),
            // "ahead" triangle
            Segment(heading, 60.0),
            // "ahead right" triangle
            Segment(heading + 45, 30.0),
            // "right" triangle
            Segment(heading + 90, 60.0),
            // "behind right" triangle
            Segment(heading + 135, 30.0)
        )
}

/**
 * This represent the original iOS INDIVIDUAL direction type which they described like this:
 *
 *  Ahead, Right, Behind, and Left all get a 90 degree window centered in their respective
 *  directions (e.g. right is from 45 degrees to 135 degrees). These windows do not overlap,
 *  so relative directions can only be "ahead", "to the right", "behind", or "to the left".
 *
 * @param heading
 * Direction the device is pointing in degrees
 * @return an array of Segments describing the angles
 */

fun getIndividualDirectionSegments(
    heading: Double
): Array<Segment> {
    return arrayOf(
        // 90 degree "behind" triangle
        Quadrant(heading + 180.0),
        // 90 degree "left" triangle
        Quadrant(heading + 270.0),
        // 90 degree "ahead" triangle
        Quadrant(heading + 0.0),
        // 90 degree "right" triangle
        Quadrant(heading + 90.0),
    )
}

/**
 * This represent the original iOS AHEAD_BEHIND direction type which they described like this:
 *
 *  Ahead and Behind get a 150 degree window, while Left and Right get 30 degree windows in their
 *  respective directions (e.g. right is 75 degrees to 105 degrees and behind is 105 degrees to
 *  255 degrees). These windows do not overlap, so relative directions can only be "ahead",
 *  "to the right", "behind", or "to the left". This style of relative direction is bias towards
 *  calling out things as either ahead or behind unless they are directly to the left or right.
 *
 * @param heading
 * Direction the device is pointing in degrees
 * @return an array of Segments describing the angles
 */
fun getAheadBehindDirectionSegments(
    heading: Double
): Array<Segment> {

    return arrayOf(
        // 150 degree "behind" triangle
        Segment(heading + 180.0, 150.0),
        // 30 degree "left" triangle
        Segment(heading + 270.0, 30.0),
        // 150 degree "ahead" triangle
        Segment(heading + 0.0, 150.0),
        // 30 degree "right" triangle
        Segment(heading + 90.0, 30.0),
    )
}

/**
 * This represent the original iOS LEFT_RIGHT direction type which they described like this:
 *
 *  Left and Right get a 120 degree window, while Ahead and Behind get 60 degree windows in their
 *  respective directions (e.g. right is 30 degrees to 150 degrees and behind is 150 degrees to
 *  210 degrees). These windows do not overlap, so relative directions can only be "ahead",
 *  "to the right", "behind", or "to the left". This style of relative direction is bias towards
 *  calling out things as either left or right unless they are directly ahead or behind.
 *
 * @param heading
 * Direction the device is pointing in degrees
 * @return an array of Segments describing the angles
 */
fun getLeftRightDirectionSegments(
    heading: Double
): Array<Segment> {

    return arrayOf(
        // 60 degree "behind" triangle
        Segment(heading + 180.0, 60.0),
        // 120 degree "left" triangle
        Segment(heading + 270.0, 120.0),
        // 60 degree "ahead" triangle
        Segment(heading + 0.0, 60.0),
        // 120 degree "right" triangle
        Segment(heading + 90.0, 120.0),
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
            val name = roadName.toString()
            if(setOfNames.contains(name)) {
                // Don't increment the priority if the name is here for the second time
            } else {
                needsFurtherChecking++
                setOfNames.add(name)
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
        feature.osmId = (polygon1 as MvtFeature).osmId
        feature.name = polygon1.name
        feature.featureType = polygon1.featureType
        feature.featureSubClass = polygon1.featureSubClass
        feature.featureClass = polygon1.featureClass
        feature.featureValue = polygon1.featureValue
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

/**
 * Given a super category string returns a mutable list of things in the super category.
 * Categories taken from original Soundscape.
 * @param category
 * String for super category. Options are "information", "object", "place", "landmark", "mobility",
 * "safety", "settlementCity", "settlementTown", "settlementVillage", "settlementHamlet"
 * @return a mutable list of things in the super category.
 */
fun getSuperCategoryElements(category: String): Set<String> {
    return when (category) {
        "settlementCity" -> setOf(
            "city",
        )
        "settlementTown" -> setOf(
            "town",
            "borough"
        )
        "settlementVillage" -> setOf(
            "village",
            "suburb",
        )
        "settlementHamlet" -> setOf(
            "hamlet",
            "quarter",
            "neighbourhood",
            "city_block",
        )

        "information" -> setOf(
            "information",
            "assembly_point",
            "fire_extinguisher",
            "defibrillator",
            "guide",
            "water",
            "fire_hose",
            "fire_flapper",
            "information_point",
            "wetland",
            "mud",
            "access_point",
            "life_ring",
            "generic_info"
        )

        "object" -> setOf(
            "turntable",
            "survey_point",
            "snow_net",
            "silo",
            "mast",
            "bird_hide",
            "transformer_tower",
            "generic_object",
            "waste_basket",
            "signal",
            "rock",
            "kiln",
            "crane",
            "rune_stone",
            "milestone",
            "lifeguard_platform",
            "water_tank",
            "sty",
            "navigationaid",
            "vending_machine",
            "terminal",
            "traverser",
            "water_tap",
            "water_well",
            "petroleum_well",
            "cross",
            "gallows",
            "speed_camera",
            "siren",
            "pylon",
            "mineshaft",
            "flagpole",
            "optical_telegraph",
            "cannon",
            "boundary_stone",
//            "street_lamp",
            "shed",
            "traffic_cones",
            "firepit",
            "bench",
//            "grit_bin",
            "stone",
            "surveillance",
//            "street_cabinet",
            "monitoring_station",
            "wayside_shrine",
            "wayside_cross",
            "tomb",
            "traffic_signals",
//            "fire_hydrant",
            "hut",
            "static_caravan",
            "bollard",
            "block",
            "waste_disposal",
            "photo_booth",
            "bbq",
        )

        "place" -> setOf(
            "post_box",
            "telephone",
            "shop",
            "newsagent",
            "anime",
            "musical_instrument",
            "vacuum_cleaner",
            "mobile_phone",
            "carpet",
            "trade",
            "garden_centre",
            "florist",
            "fireplace",
            "massage",
            "herbalist",
            "bag",
            "pastry",
            "deli",
            "beverages",
            "alcohol",
            "substation",
            "travel_agent",
            "research",
            "newspaper",
            "ammunition",
            "wildlife_hide",
            "playground",
            "watchmaker",
            "tinsmith",
            "sun_protection",
            "sculptor",
            "metal_construction",
            "handicraft",
            "cowshed",
            "cabin",
            "barn",
            "warehouse",
            "houseboat",
            "book_store",
            "generic_place",
            "hunting_stand",
            "game_feeding",
            "crypt",
            "animal_shelter",
            "animal_boarding",
            "blood_donation",
            "nursing_home",
            "dentist",
            "baby_hatch",
            "language_school",
            "public_bookcase",
            "biergarten",
            "running",
            "glaziery",
            "garages",
            "retail",
            "office",
            "hotel",
            "camp_site",
            "rugby_league",
            "roller_skating",
            "multi",
            "ice_hockey",
            "hapkido",
            "croquet",
            "cricket",
            "cockfighting",
            "boxing",
            "bmx",
            "billiards",
            "toys",
            "pyrotechnics",
            "laundry",
            "funeral_directors",
            "dry_cleaning",
            "copyshop",
            "chalet",
            "apartment",
            "water_ski",
            "water_polo",
            "table_soccer",
            "table_tennis",
            "skateboard",
            "sailing",
            "safety_training",
            "rowing",
            "model_aerodrome",
            "korfball",
            "ice_stock",
            "gymnastics",
            "football",
            "field_hockey",
            "equestrian",
            "cycling",
            "curling",
            "cricket_nets",
            "cliff_diving",
            "boules",
            "bobsleigh",
            "baseball",
            "aikido",
            "10pin",
            "weapons",
            "pet",
            "money_lender",
            "gift",
            "books",
            "bookmaker",
            "photo",
            "craft",
            "motorcycle",
            "hunting",
            "window_blind",
            "curtain",
            "antiques",
            "paint",
            "tattoo",
            "nutrition_supplements",
            "hearing_aids",
            "cosmetics",
            "watches",
            "jewelry",
            "boutique",
            "baby_goods",
            "tea",
            "pasta",
            "coffee",
            "quango",
            "political_party",
            "association",
            "architect",
            "advertising_agency",
            "summer_camp",
            "pitch",
            "dance",
            "amusement_arcade",
            "adult_gaming_centre",
            "window_construction",
            "upholsterer",
            "shoemaker",
            "sawmill",
            "pottery",
            "key_cutter",
            "hvac",
            "clockmaker",
            "carpenter",
            "builder",
            "bookbinder",
            "boatbuilder",
            "brewery",
            "blacksmith",
            "basket_maker",
            "greenhouse",
            "farm_auxiliary",
            "civic",
            "bungalow",
            "detached",
            "hair_dresser",
            "clothing_store",
            "user",
            "dojo",
            "nightclub",
            "community_centre",
            "brothel",
            "veterinary",
            "social_facility",
            "clinic",
            "charging_station",
            "kindergarten",
            "ice_cream",
            "fast_food",
            "commercial",
            "canoe",
            "scuba_diving",
            "swimming_pool",
            "fishing",
            "optician",
            "confectionery",
            "bunker",
            "sleeping_pods",
            "picnic_site",
            "motel",
            "guest_house",
            "wrestling",
            "toboggan",
            "skiing",
            "rc_car",
            "paddle_tennis",
            "hockey",
            "fencing",
            "bowls",
            "badminton",
            "archery",
            "american_football",
            "travel_agency",
            "tobacco",
            "e-cigarette",
            "video",
            "car_repair",
            "hifi",
            "lamps",
            "kitchen",
            "interior_decoration",
            "houseware",
            "erotic",
            "beauty",
            "wine",
            "dairy",
            "cheese",
            "bakery",
            "telecommunication",
            "tax",
            "real_estate_agent",
            "notary",
            "ngo",
            "lawyer",
            "it",
            "foundation",
            "employment_agency",
            "educational_institution",
            "adoption_agency",
            "miniature_golf",
            "garden",
            "building",
            "winery",
            "tiler",
            "chimney_sweeper",
            "stand_builder",
            "saddler",
            "plumber",
            "plasterer",
            "painter",
            "jeweller",
            "floorer",
            "distillery",
            "carpet_layer",
            "beekeeper",
            "public",
            "dormitory",
            "apartments",
            "internet_cafe",
            "shoe_shop",
            "generic_shop",
            "coffee_shop",
            "recycling",
            "coworking_space",
            "stripclub",
            "ev_charging",
            "restaurant",
            "pub",
            "obstacle_course",
            "volleyball",
            "tennis",
            "soccer",
            "shooting",
            "rugby_union",
            "orienteering",
            "netball",
            "motor",
            "kitesurfing",
            "karting",
            "judo",
            "horseshoes",
            "handball",
            "golf",
            "gaelic_games",
            "diving",
            "darts",
            "climbing_adventure",
            "basketball",
            "bandy",
            "australian_football",
            "9pin",
            "vacant",
            "lottery",
            "trophy",
            "music",
            "games",
            "tyres",
            "sports",
            "outdoor",
            "car",
            "electronics",
            "computer",
            "furniture",
            "candles",
            "hardware",
            "gas",
            "energy",
            "doityourself",
            "bathroom_furnishing",
            "medical_supply",
            "variety_store",
            "second_hand",
            "charity",
            "fashion",
            "fabric",
            "clothes",
            "convenience",
            "butcher",
            "water_utility",
            "realtor",
            "company",
            "accountant",
            "bunker_silo",
            "hackerspace",
            "lifeguard_base",
            "roofer",
            "rigger",
            "parquet_layer",
            "gardener",
            "stable",
            "garage",
            "transportation",
            "house",
            "helipad",
            "apron",
            "consumer_electronics_store",
            "speciality_store",
            "defined",
            "shower",
            "sauna",
            "gym",
            "crematorium",
            "gambling",
            "bank",
            "music_school",
            "cafe",
            "bar",
            "farm",
            "bicycle",
            "tailor",
            "locksmith",
            "industrial",
            "wilderness_hut",
            "hostel",
            "caravan_site",
            "weightlifting",
            "taekwondo",
            "swimming",
            "surfing",
            "skating",
            "racquet",
            "pelota",
            "paragliding",
            "parachuting",
            "motocross",
            "ice_skating",
            "horse_racing",
            "dog_racing",
            "climbing",
            "chess",
            "canadian_football",
            "beachvolleyball",
            "base",
            "athletics",
            "pawnbroker",
            "ticket",
            "stationery",
            "video_games",
            "model",
            "frame",
            "art",
            "car_parts",
            "radiotechnics",
            "bed",
            "garden_furniture",
            "electrical",
            "perfumery",
            "hairdresser",
            "drugstore",
            "shoes",
            "leather",
            "general",
            "seafood",
            "organic",
            "greengrocer",
            "chocolate",
            "brewing_supplies",
            "tax_advisor",
            "private_investigator",
            "government",
            "forestry",
            "estate_agent",
            "spring",
            "golf_course",
            "ses_station",
            "lifeguard_place",
            "stonemason",
            "scaffolder",
            "sailmaker",
            "photographic_laboratory",
            "photographer",
            "insulation",
            "electrician",
            "dressmaker",
            "caterer",
            "terrace",
            "toy_shop",
            "dive_centre",
            "swingerclub",
            "doctors",
            "car_wash",
            "driving_school",
            "free_flying",
            "religion",
            "kiosk",
            "residential",
            "food"
        )

        "landmark" -> setOf(
            "waterfall",
            "boatyard",
            "theme_park",
            "roundhouse",
            "generator",
            "beach",
            "naval_base",
            "works",
            "water_works",
            "telescope",
            "pier",
            "observatory",
            "reservoir",
            "monument",
            "battlefield",
            "post_office",
            "planetarium",
            "social_centre",
            "prison",
            "courthouse",
            "bridge",
            "hangar",
            "tower",
            "attraction",
            "zoo",
            "gallery",
            "artwork",
            "alpine_hut",
            "plant",
            "insurance",
            "airfield",
            "water_tower",
            "pumping_station",
            "hot_water_tank",
            "campanile",
            "sports_centre",
            "fitness_centre",
            "beach_resort",
            "village_green",
            "ship",
            "memorial",
            "synagogue",
            "mosque",
            "chapel",
            "cathedral",
            "train_terminal",
            "college",
            "arts_centre",
            "ranger_station",
            "hospital",
            "fountain",
            "track",
            "conference_centre",
            "viewpoint",
            "supermarket",
            "peak",
            "storage_tank",
            "lighthouse",
            "beacon",
            "park",
            "port",
            "archaeological_site",
            "train_station",
            "shrine",
            "church",
            "historic_monument",
            "generic_landmark",
            "tourism_museum",
            "register_office",
            "grave_yard",
            "school",
            "marketplace",
            "fire_station",
            "ruins",
            "weir",
            "museum",
            "mall",
            "volcano",
            "hot_spring",
            "glacier",
            "wastewater_plant",
            "offshore_platform",
            "gasometer",
            "water_park",
            "bandstand",
            "wreck",
            "pillory",
            "monastery",
            "locomotive",
            "fort",
            "services",
            "lifeguard_tower",
            "temple",
            "national_park",
            "heliport",
            "public_park",
            "department_store",
            "studio",
            "public_building",
            "place_of_worship",
            "clock",
            "casino",
            "ferry_terminal",
            "stadium",
            "dam",
            "dock",
            "geyser",
            "bay",
            "barracks",
            "windmill",
            "watermill",
            "communications_tower",
            "swimming_area",
            "slipway",
            "nature_reserve",
            "marina",
            "ice_rink",
            "manor",
            "city_gate",
            "castle",
            "aircraft",
            "digester",
            "sally_port",
            "aerodrome",
            "shopping_mall",
            "cinema",
            "rescue_station",
            "airport",
            "theatre",
            "library",
            "university",
            "townhall",
            "police",
            "embassy",
            "bus_station",
            "station"
        )

        "mobility" -> setOf(
            "toll_booth",
            "lift_gate",
            "lift",
            "steps",
            "unmanaged_crossing",
            "pharmacy",
            "kneipp_water_cure",
            "food_court",
            "toilets",
            "chemist",
            "checkpoint",
            "dog_park",
            "kissing_gate",
            "fuel",
            "car_rental",
            "pedestrianised_area",
            "escalator",
            "shelter",
            "water_point",
            "subway_entrance",
            "cave_entrance",
            "turnstile",
            "swing_gate",
            "stile",
            "car_sharing",
            "customer_service",
            "watering_place",
            "atm",
            "drinking_water",
            "platform",
            "crossing",
            "elevator",
            "horse_stile",
            "bureau_de_change",
            "stairs",
            "bicycle_rental",
            "bicycle_parking",
            "bus_stop",
            "tram_stop",
            "subway",
            "hampshire_gate",
            "full-height_turnstile",
            "boat_sharing",
            "help_point",
            "open_space",
            "spending_area",
            "bicycle_repair_station",
            "taxi",
            "gate"
        )

        "safety" -> setOf(
            "motorcycle_barrier",
            "kent_carriage_gap",
            "shared_space",
            "construction",
            "cliff",
            "training_area",
            "log",
            "jersey_barrier",
            "cycle_barrier",
            "construction_site",
            "ridge",
            "nuclear_explosion_site",
            "dyke",
            "sump_buster",
            "rope",
            "debris",
            "road_works",
            "lock_gate",
            "sinkhole",
            "range",
            "ambulance_station",
            "spikes",
            "cattle_grid",
            "generic_hazard",
            "contact_line",
            "danger_area",
            "chain",
            "parking_entrance",
            "parking_space",
            "parking",
            "motorcycle_parking"
        )

        else -> setOf("Unknown category")
    }
}


fun addSidewalk(currentRoad: Way,
                roadTree: FeatureTree,
                ruler: Ruler,
                localizedContext: Context? = null,
) : Boolean {

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
        var found = false
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
            // Store the name of the associated road
            currentRoad.properties?.set("pavement", name.toString())
            return true
        } else {
            // No road found - inhibit future search?
            currentRoad.properties?.set("pavement", "")
        }
    }
    return false
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
    val poiTree = gridState.featureTrees[TreeId.POIS.id]
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
    val entrancesTree = gridState.featureTrees[TreeId.ENTRANCES.id]
    if (startPoi == null)
        startPoi = checkNearbyPoi(entrancesTree, startLocation, polygonEndPoi, gridState.ruler)
    if (endPoi == null)
        endPoi = checkNearbyPoi(entrancesTree, endLocation, polygonStartPoi, gridState.ruler)

    // Does the unnamed way start or end near a Landmark or a place?
    val placesAndLandmarkTree = gridState.featureTrees[TreeId.PLACES_AND_LANDMARKS.id]
    if (startPoi == null)
        startPoi = checkNearbyPoi(placesAndLandmarkTree, startLocation, polygonEndPoi, gridState.ruler)
    if (endPoi == null)
        endPoi = checkNearbyPoi(placesAndLandmarkTree, endLocation, polygonStartPoi, gridState.ruler)

    val safetyTree = gridState.featureTrees[TreeId.SAFETY_POIS.id]
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
                way.properties?.set("destination:backward", startName)
                addedDestinations = true
            }
        }
        if(!endDestinationAdded) {
            val endName = (endPoi as MvtFeature?)?.name
            if (endName != null) {
                way.properties?.set("destination:forward", endName)
                addedDestinations = true
            }
        }
    }
    return addedDestinations
}

fun confectNamesForRoad(road: Way,
                        gridState: GridState) {

    // rtree searches take time and so we should avoid them where possible.

    val roadTree = gridState.featureTrees[TreeId.ROADS_AND_PATHS.id]
    if (road.name == null) {

        if (addSidewalk(road, roadTree, gridState.ruler)) {
            return
        }

        addPoiDestinations(road, gridState)
    }
}

fun setDestinationTag(
    properties: HashMap<String, Any?>?,
    forwards: Boolean,
    tagValue: String,
    deadEnd: Boolean = false,
    brunnelOrStepsValue: String) {

    if(tagValue.isNotEmpty())
        properties?.set("${if (deadEnd) "dead-end" else "destination"}:${if (forwards) "backward" else "forward"}", tagValue)
    if(brunnelOrStepsValue.isNotEmpty())
        properties?.set("passes:${if (forwards) "backward" else "forward"}", brunnelOrStepsValue)
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
                    if(way.properties?.get("subclass") == "steps") {
                        brunnelOrStepsValue = "steps"
                    } else if(way.properties?.get("brunnel") != null) {
                        brunnelOrStepsValue = way.properties?.get("brunnel").toString()
                    }

                    (way.name != null)
                }

                for(way in ways) {
                    setDestinationTag(
                        way.second.properties,
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
                    setDestinationTag(eachWay.second.properties, !eachWay.first, "dead-end", true, "")
                }
            }
        }
    }
}
