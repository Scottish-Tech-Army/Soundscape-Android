package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.dto.IntersectionRelativeDirections
import org.scottishtecharmy.soundscape.dto.Tile
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
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import java.io.FileOutputStream
import java.lang.Math.toDegrees
import kotlin.collections.toTypedArray
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sinh
import kotlin.math.tan


/**
 * Gets Slippy Map Tile Name from GPS coordinates and Zoom (fixed at 16 for Soundscape).
 * @param location
 * Location in LngLatAlt
 * @param zoom
 * The zoom level.
 * @return a Pair(xtile, ytile).
 */
fun getXYTile(
    location: LngLatAlt,
    zoom: Int = 16
): Pair<Int, Int> {
    val latRad = toRadians(location.latitude)
    var xtile = floor((location.longitude + 180) / 360 * (1 shl zoom)).toInt()
    var ytile = floor((1.0 - asinh(tan(latRad)) / PI) / 2 * (1 shl zoom)).toInt()

    if (xtile < 0) {
        xtile = 0
    }
    if (xtile >= (1 shl zoom)) {
        xtile = (1 shl zoom) - 1
    }
    if (ytile < 0) {
        ytile = 0
    }
    if (ytile >= (1 shl zoom)) {
        ytile = (1 shl zoom) - 1
    }
    return Pair(xtile, ytile)
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
 * Gets map coordinates from X and Y GPS coordinates. This is the same calculation as above
 * but returns normalised x and y values scaled between 0 and 1.0. These are what are required
 * by the mapcompose library to set markers/positions.
 * @param lat
 * Latitude in decimal degrees.
 * @param lon
 * Longitude in decimal degrees.
 * @return a Pair(x, y).
 */
fun getNormalizedFromGpsMapCoordinates(
    lat: Double,
    lon: Double
): Pair<Double, Double> {

    val latRad = toRadians(lat)
    var x = (lon + 180.0) / 360.0
    var y = (1.0 - asinh(tan(latRad)) / PI) / 2

    // Keep result within bounds
    x = minOf(1.0, maxOf(0.0, x))
    y = minOf(1.0, maxOf(0.0, y))

    return Pair(x, y)
}

fun getGpsFromNormalizedMapCoordinates(
    x: Double,
    y: Double
): Pair<Double, Double> {

//    val latRad = toRadians(lat)
//    var x = (lon + 180.0) / 360.0
//    var y = (1.0 - asinh(tan(latRad)) / PI) / 2

    val latitude = toDegrees(atan(sinh((1.0 - (2 * y)) * PI)))
    val longitude = (360.0 * x) - 180.0

    return Pair(latitude, longitude)
}

/**
 * Given a radius and location it calculates the set of tiles (VectorTiles) that cover a
 * circular region around the specified location.
 * @param currentLatitude
 * The center of the region to search.
 * @param currentLongitude
 * The center of the region to search.
 * @param radius
 * The radius of the region to get adjoining tiles in meters
 * @return  A MutableList of VectorTiles covering the searched region
 */
fun getTilesForRegion(
    currentLatitude: Double = 0.0,
    currentLongitude: Double = 0.0,
    radius: Double = 250.0,
    zoom: Int = 16
): MutableList<Tile> {

    val (pixelX, pixelY) = getPixelXY(currentLatitude, currentLongitude, zoom)
    val radiusPixels = radius / groundResolution(currentLatitude, zoom).toInt()

    val startX = pixelX - radiusPixels
    val startY = pixelY - radiusPixels
    val endX = pixelX + radiusPixels
    val endY = pixelY + radiusPixels

    val (startTileX, startTileY) = getTileXY(startX.toInt(), startY.toInt())
    val (endTileX, endTileY) = getTileXY(endX.toInt(), endY.toInt())

    val tiles: MutableList<Tile> = mutableListOf()

    for (y in startTileY..endTileY) {
        for (x in startTileX..endTileX) {
            val surroundingTile = Tile("", x, y, zoom)
            surroundingTile.quadkey = getQuadKey(x, y, zoom)
            tiles.add(surroundingTile)
        }
    }
    return tiles
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
    val superCategoryList = getSuperCategoryElements(superCategory)

    for (feature in poiFeatureCollection) {
        for (featureType in superCategoryList) {
            feature.foreign?.let { foreign ->
                if (foreign["feature_type"] == featureType || foreign["feature_value"] == featureType) {
                    tempFeatureCollection.addFeature(feature)
                    feature.foreign?.put("category",superCategory)
                }
            }
        }
    }
    return tempFeatureCollection
}

fun featureIsInFilterGroup(feature: Feature, filter: String): Boolean {

    val tags = when(filter) {
        "transit" -> listOf("bus_stop", "train_station", "tram_stop", "ferry_terminal", "subway")
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
        feature.foreign?.let { foreign ->
            if (foreign["feature_value"] == tag) {
                return true
            }
        }
    }
    return false
}


/** isDuplicateByOsmId returns true if the OSM id for the feature has already been entered into
 * the existingSet. It returns false if it's the first time, or there's no OSM id.
 */
fun isDuplicateByOsmId(existingSet : MutableSet<Any>, feature : Feature) : Boolean {
    val osmId = feature.foreign?.get("osm_ids")
    if (osmId != null) {
        if(existingSet.contains(osmId))
            return true
        existingSet.add(osmId)
    }
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
            if (!isDuplicateByOsmId(existingSet, feature)) {
                outputFeatureCollection.features.add(feature)
            }
        }
    }
}

/**
 * Given a FeatureCollection checks for duplicate OSM IDs and removes them.
 * @param featureCollection
 * A Feature Collection.
 * @return a Feature Collection object with Features with duplicate "osm_ids" removed.
 */
fun removeDuplicateOsmIds(
    featureCollection: FeatureCollection
): FeatureCollection{
    val processedOsmIds = mutableSetOf<Any>()
    val tempFeatureCollection = FeatureCollection()

    deduplicateFeatureCollection(tempFeatureCollection, featureCollection, processedOsmIds)

    return tempFeatureCollection
}

fun getFovTriangle(userGeometry: UserGeometry) : Triangle {
    val heading = userGeometry.heading() ?: 0.0
    val quadrant = Quadrant(heading)
    return Triangle(userGeometry.location,
        getDestinationCoordinate(
            userGeometry.location,
            quadrant.left,
            userGeometry.fovDistance
        ),
        getDestinationCoordinate(
            userGeometry.location,
            quadrant.right,
            userGeometry.fovDistance
        )
    )
}

/**
 * Get the road names/ref/road type that make up the intersection. Intersection objects only
 * contain "osm_ids" so we need to hook the osm_ids up with the information from the roads
 * feature collection
 * @param intersectionFeature
 * A single intersection
 * @param roadsFeatureCollection
 * A roads feature collection.
 * @return A Feature Collection that contains the roads that make up the nearest intersection.
 */
fun getIntersectionRoadNames(
    intersectionFeature: Feature?,
    roadsFeatureCollection: FeatureCollection
): FeatureCollection {

    val intersectionRoads = FeatureCollection()
    if(intersectionFeature == null) return intersectionRoads

    val osmIds = intersectionFeature.foreign?.get("osm_ids") as? List<*> ?: return intersectionRoads

    for (item in osmIds) {
        for (roadFeature in roadsFeatureCollection) {
            val roadOsmIds = roadFeature.foreign?.get("osm_ids") as? List<*> ?: return intersectionRoads
            if (roadOsmIds.firstOrNull() == item) {
                intersectionRoads.addFeature(roadFeature)
            }
        }
    }
    return intersectionRoads
}

data class PointAndDistanceAndHeading(var point: LngLatAlt = LngLatAlt(),
                                      var distance: Double = Double.MAX_VALUE,
                                      var heading: Double = 0.0)

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
    feature: Feature
): PointAndDistanceAndHeading {
    when (feature.geometry.type) {
        "Point" -> {
            val point = feature.geometry as Point
            val distanceToFeaturePoint = currentLocation.distance(
                LngLatAlt(point.coordinates.longitude, point.coordinates.latitude)
            )
            return PointAndDistanceAndHeading(point.coordinates, distanceToFeaturePoint)
        }

        "MultiPoint" -> {
            val multiPoint = feature.geometry as MultiPoint
            var shortestDistance = Double.MAX_VALUE
            var nearestPoint = LngLatAlt()

            for (point in multiPoint.coordinates) {
                val distanceToPoint = currentLocation.distance(point)
                if (distanceToPoint < shortestDistance) {
                    shortestDistance = distanceToPoint
                    nearestPoint = point
                }
            }
            // this is the closest point to the current location from the collection of points
            return PointAndDistanceAndHeading(nearestPoint, shortestDistance)
        }

        "LineString" -> {
            val lineString = feature.geometry as LineString
            return currentLocation.distanceToLineString(lineString)
        }

        "MultiLineString" -> {
            val multiLineString = feature.geometry as MultiLineString
            var nearest = PointAndDistanceAndHeading()

            for (arrCoordinates in multiLineString.coordinates) {
                val segmentNearest = currentLocation.distanceToLineString(
                    LineString(arrCoordinates),
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
            var distance = distanceToPolygon(
                currentLocation,
                polygon,
                nearestPoint
            )
            // TODO: Could we return negative distance and then interpret that differently in the
            //  caller?
            // If we are inside the polygon, return 0m
            if(polygonContainsCoordinates(currentLocation, polygon)) {
                distance = 0.0
            }
            return PointAndDistanceAndHeading(nearestPoint, distance)
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
                    pointOnPolygon
                )
                if (distance < shortestDistance) {
                    shortestDistance = distance
                    nearestPoint = pointOnPolygon
                }
            }
            // this is the shortest distance from current location to the collection of Polygons
            return PointAndDistanceAndHeading(nearestPoint, shortestDistance)
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
        feature.foreign?.put("distance_to", getDistanceToFeature(currentLocation, feature).distance)
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
        .sortedBy {(it.foreign?.get("distance_to") as? Number)?.toDouble() ?: Double.MAX_VALUE
        }
    // loop through the list of sorted Features and add to a new Feature Collection
    val featuresSortedByDistance = FeatureCollection()
    for (feature in featuresSortedByDistanceList) {
        featuresSortedByDistance.addFeature(feature)
    }
    return featuresSortedByDistance
}

fun removeDuplicates(
    intersectionToCheck: Feature?
): Feature? {

    if(intersectionToCheck == null) return null

    val osmIdsAtIntersection =
        intersectionToCheck.foreign?.get("osm_ids") as? List<*>
            ?: // Handle case where osmIds is null
            return null // Or handle the error differently

    val uniqueOsmIds = osmIdsAtIntersection.toSet()
    val cleanOsmIds = uniqueOsmIds.toList() // Convert back to list for potential modification

    val intersectionClean = intersectionToCheck
    intersectionClean.foreign?.set("osm_ids", cleanOsmIds)
    return intersectionClean
}


/**
 * This represent the original iOS COMBINED direction type which they described like this:
 *
 *  Ahead, Right, Behind, and Left all get a 150 degree window centered in their respective
 *  directions (e.g. right is 15 degrees to 165 degrees). In the areas where these windows
 *  overlap, the relative directions get combined. For example, 0 degrees is "ahead", while
 *  20 degrees is "ahead to the right."

 * @param heading
 * Direction the device is pointing in degrees
 * @return an array of Segments describing the angles
 */
fun getCombinedDirectionSegments(
    heading: Double
): Array<Segment> {
    return arrayOf(
            // 30 degree "behind" triangle
            Segment(heading + 180.0, 30.0),
            // 60 degree "behind left" triangle
            Segment(heading + 225.0, 60.0),
            // 30 degree "left" triangle
            Segment(heading + 270.0, 30.0),
            // 60 degree "ahead left" triangle
            Segment(heading + 315.0, 60.0),
            // 30 degree "ahead" triangle
            Segment(heading, 30.0),
            // 60 degree "ahead right" triangle
            Segment(heading + 45, 60.0),
            // 30 degree "right" triangle
            Segment(heading + 90, 30.0),
            // 60 degree "behind right" triangle
            Segment(heading + 135, 60.0)
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
 * Returns a road direction type in relation to the intersection:
 * The road is leading up to an intersection - LEADING
 *        →
 * --------------⦿
 * The road starts from an intersection - TRAILING
 *        →
 * ⦿--------------
 * The road is leading up to and continues on from an intersection - LEADING_AND_TRAILING
 *    →       →
 * -------⦿-------
 * The road is not part of the intersection - NONE
 *        ⦿
 * --------------
 * @param intersection
 * The intersection as a Feature
 * @param road
 * The road as a Feature
 * @return A RoadDirectionAtIntersection
 */
fun getDirectionAtIntersection(intersection: Feature, road: Feature): RoadDirectionAtIntersection {
    val roadCoordinates = (road.geometry as LineString).coordinates
    val intersectionCoordinate = (intersection.geometry as Point).coordinates

    return if (intersectionCoordinate.longitude == roadCoordinates.first().longitude && intersectionCoordinate.latitude == roadCoordinates.first().latitude) {
        RoadDirectionAtIntersection.LEADING
    } else if (intersectionCoordinate.longitude == roadCoordinates.last().longitude && intersectionCoordinate.latitude == roadCoordinates.last().latitude) {
        RoadDirectionAtIntersection.TRAILING
    } else {
        val coordinateFound = roadCoordinates.any{ it.latitude == intersectionCoordinate.latitude && it.longitude == intersectionCoordinate.longitude}
        if (coordinateFound) {
            // Now that we have split all of the roads/paths at intersections as we parse them in, we
            // should never reach this code where the intersection is in the middle of a road
            assert(false)
            RoadDirectionAtIntersection.LEADING_AND_TRAILING
        } else {
            // Why would we ever hit this code?
            assert(false)
            RoadDirectionAtIntersection.NONE
        }
    }
}


/**
 * Given an intersection Feature and a road Feature will split the road into two based on the
 * coordinate of the intersection.
 * @param intersection
 * intersection Feature that is used to split the road using the intersection coordinates.
 * @param road
 * The road that is being split into two
 * @return a Feature Collection containing two roads. One road will contain the intersection
 * coordinates at the "end" and the second road will contain the intersection coordinates at the "start"
 */
fun splitRoadByIntersection(
    intersection: Feature,
    road: Feature
): FeatureCollection {
    val intersectionCoordinate = (intersection.geometry as Point).coordinates
    return splitRoadAtNode(intersectionCoordinate, road)
}
fun splitRoadAtNode(
    node: LngLatAlt,
    road: Feature
): FeatureCollection {
    val roadCoordinates = (road.geometry as LineString).coordinates

    val coordinateFound = roadCoordinates.any{ it.latitude == node.latitude && it.longitude == node.longitude}
    if (!coordinateFound) {
        // Intersection not found, return empty
        return FeatureCollection()
    }

    val indexOfIntersection = roadCoordinates.indexOfFirst { it == node }
    val part1 = roadCoordinates.subList(0, indexOfIntersection + 1)
    val part2 = roadCoordinates.subList(indexOfIntersection, roadCoordinates.size)

    val roadLineString1 = LineString(*part1.toTypedArray())
    val roadLineString2 = LineString(*part2.toTypedArray())
    val newFeatureCollection = FeatureCollection()

    val featureRoad1 = Feature()
    @Suppress("unchecked_cast") // Suppress warning
    val clonedProperties = road.properties?.clone() as? HashMap<String, Any?> // Safe cast and null check
    featureRoad1.properties = clonedProperties
    @Suppress("unchecked_cast") // Suppress warning
    road.foreign?.clone().also { featureRoad1.foreign = it as? HashMap<String, Any?> } // Safe cast and null check

    featureRoad1.geometry = roadLineString1
    newFeatureCollection.addFeature(featureRoad1)

    val featureRoad2 = Feature()
    @Suppress("unchecked_cast") // Suppress warning
    val clonedProperties2 = road.properties?.clone() as? HashMap<String, Any?> // Safe cast and null check
    featureRoad2.properties = clonedProperties2
    @Suppress("unchecked_cast") // Suppress warning
    road.foreign?.clone().also { featureRoad2.foreign = it as java.util.HashMap<String, Any?>? }

    featureRoad2.geometry = roadLineString2
    newFeatureCollection.addFeature(featureRoad2)

    return newFeatureCollection
}


/**
 * Given an intersection Feature and a road Feature will return the bearing of the road to the
 * intersection
 * @param intersection
 * intersection Feature
 * @param road
 * road Feature
 * @return the bearing from the road to the intersection
 */
fun getRoadBearingToIntersection(
    intersection: Feature?,
    road: Feature?,
    deviceHeading: Double
): Double {

    if((road == null) || (intersection == null))
        return 0.0

    val roadCoordinates = (road.geometry as LineString).coordinates
    val intersectionCoordinate = (intersection.geometry as Point).coordinates

    // if the intersection location doesn't match the start/finish location of the road
    // then we are dealing with a LEADING_AND_TRAILING road. We need to split the road into two
    // where the intersection coordinate is on the road
    if (roadCoordinates.first() != intersectionCoordinate && roadCoordinates.last() != intersectionCoordinate){
        val splitRoads = splitRoadByIntersection(intersection, road)
        // we've got two split roads but which one do we want the bearing for?
        // get the bearing for both

        val bearingArray: MutableList<Double> = mutableListOf()

        for(splitRoad in splitRoads){
            val indexOfIntersection = (splitRoad.geometry as LineString).coordinates.indexOfFirst { it == intersectionCoordinate }
            val testReferenceCoordinate: LngLatAlt = if (indexOfIntersection == 0) {
                getReferenceCoordinate(
                    splitRoad.geometry as LineString,
                    3.0,
                    false
                )
            } else {
                getReferenceCoordinate(
                    splitRoad.geometry as LineString,
                    3.0,
                    true
                )
            }
            val bearing = bearingFromTwoPoints(testReferenceCoordinate, intersectionCoordinate)
            bearingArray.add(bearing)
        }
        if(bearingArray.size >= 2)
            return findClosestDirection(deviceHeading, bearingArray[0], bearingArray[1])

        return 0.0
    }

    val indexOfIntersection = roadCoordinates.indexOfFirst { it == intersectionCoordinate }

    val testReferenceCoordinate: LngLatAlt = if (indexOfIntersection == 0) {
        getReferenceCoordinate(
            road.geometry as LineString,
            3.0,
            false
        )
    } else {
        getReferenceCoordinate(
            road.geometry as LineString,
            3.0,
            true
        )
    }

    //  bearing from synthetic coordinate on road to intersection
    return bearingFromTwoPoints(testReferenceCoordinate, intersectionCoordinate)
}

/**
 * Given a road Feature and a set of intersectionRelativeDirections this will return a feature
 * collection with an entry for the road each time it appears in the intersection. Normally this
 * would be a single Feature (one road leaving the intersection) but if the road loops around and
 * back into the roundabout then there can be two entries - see intersectionsLoopBackTest for an
 * example e.g. https://geojson.io/#map=18/37.339112/-122.038756
 */
fun getFeaturesWithRoadDirection(road: Feature,
                                 intersectionRelativeDirections: FeatureCollection) : FeatureCollection {
    val testReferenceCoordinateForRoad = getReferenceCoordinate(
        road.geometry as LineString, 1.0, false
    )
    // test if the reference coordinate we've created is in any of the relative direction triangles
    val newFeatureCollection = FeatureCollection()
    for (direction in intersectionRelativeDirections) {
        val iAmHere1 = polygonContainsCoordinates(
            testReferenceCoordinateForRoad, (direction.geometry as Polygon)
        )
        if (iAmHere1) {
            // at this point we need to take the road and direction and merge their properties
            // and create a new Feature and add it to the FeatureCollection
            val newFeature = mergeRoadAndDirectionFeatures(road, direction)
            newFeatureCollection.addFeature(newFeature)
            //println("Road name: ${splitRoad.properties!!["name"]}")
            //println("Road direction: ${direction.properties!!["Direction"]}")
        } else {
            // reverse the LineString, create the ref coordinate and test it again
            val testReferenceCoordinateReverse = getReferenceCoordinate(
                road.geometry as LineString, 1.0, true
            )
            val iAmHere2 = polygonContainsCoordinates(
                testReferenceCoordinateReverse, (direction.geometry as Polygon)
            )
            if (iAmHere2) {
                val newFeature = mergeRoadAndDirectionFeatures(road, direction)
                newFeatureCollection.addFeature(newFeature)
                //println("Road name: ${splitRoad.properties!!["name"]}")
                //println("Road direction: ${direction.properties!!["Direction"]}")
            }
        }
    }
    return newFeatureCollection
}

/**
 * Given an intersection Road names FeatureCollection, a nearest Intersection FeatureCollection
 * and an intersectionRelativeDirections FeatureCollection. This will return a feature collection of the roads
 * that make up the intersection tagged with their relative directions.
 * 0 = Behind, 1 = Behind Left, 2 = Left, 3 = Ahead Left,
 * 4 = Ahead, 5 = Ahead Right, 6 = Right, 7 = Behind Right
 * @param intersectionRoadNames
 * Roads FeatureCollection that contains the roads that make up the intersection.
 * @param nearestIntersection
 * Intersection Feature
 * @param intersectionRelativeDirections
 * Feature collection that consists of relative direction polygons that we are using to determine relative direction
 * @return A feature collection sorted by "Direction" that contains the roads that make up the intersection tagged with their relative direction
 */
fun getIntersectionRoadNamesRelativeDirections(
    intersectionRoadNames: FeatureCollection,
    nearestIntersection: Feature?,
    intersectionRelativeDirections: FeatureCollection
): FeatureCollection {

    val newFeatureCollection = FeatureCollection()
    if(nearestIntersection == null)
        return newFeatureCollection

    for (road in intersectionRoadNames) {
        val testRoadDirectionAtIntersection =
            getDirectionAtIntersection(nearestIntersection, road)
        //println("Road name: ${road.properties!!["name"]} and $testRoadDirectionAtIntersection")
        // Our roads are all now pre-split when we parse them in from MVT
//        if (testRoadDirectionAtIntersection == RoadDirectionAtIntersection.LEADING_AND_TRAILING){
//            // split the road into two
//            val roadCoordinatesSplitIntoTwo = splitRoadByIntersection(
//                nearestIntersection,
//                road
//            )
//            // for each split road work out the relative direction from the intersection
//            for (splitRoad in roadCoordinatesSplitIntoTwo) {
//                newFeatureCollection.plusAssign(getFeaturesWithRoadDirection(splitRoad, intersectionRelativeDirections))
//            }
//        }
//        else{
            newFeatureCollection.plusAssign(getFeaturesWithRoadDirection(road, intersectionRelativeDirections))
//        }
    }

    return sortFeatureCollectionByDirectionProperty(newFeatureCollection)
}

fun sortFeatureCollectionByDirectionProperty(
    featureCollectionWithDirection: FeatureCollection
): FeatureCollection {
    val newFeatureCollection = FeatureCollection()
    val intersectionRelativeDirections: MutableList<IntersectionRelativeDirections> = arrayListOf()

    for (feature in featureCollectionWithDirection){
        val newFeature = Feature()
        @Suppress("unchecked_cast") // Suppress warning
        feature.properties?.clone().also { newFeature.properties = it as? HashMap<String, Any?>? }
        val fineBeLikeThat = feature.properties?.get("Direction").toString().toInt()

        intersectionRelativeDirections.add(
            IntersectionRelativeDirections(
                fineBeLikeThat,
                newFeature
            )
        )
    }
    intersectionRelativeDirections.sortBy { d -> d.direction }

    for (item in intersectionRelativeDirections){
        newFeatureCollection.addFeature(item.feature)
    }
    return newFeatureCollection
}

fun mergeRoadAndDirectionFeatures(
    road: Feature,
    direction: Feature
): Feature {
    val newFeature = Feature()

    // Here we want to:
    // Create a new Feature
    // take the road Feature and its properties and add it to the new Feature
    // add the "Direction" property from the direction Feature and add it to the new Feature
    newFeature.geometry = road.geometry
    @Suppress("unchecked_cast") // Suppress warning
    road.properties?.clone().also { newFeature.properties = it as? HashMap<String, Any?>? }
    newFeature.foreign = road.foreign
    newFeature.type = road.type
    newFeature.bbox = road.bbox
    val fineBeLikeThat = direction.properties?.get("Direction")

    newFeature.properties?.put("Direction", fineBeLikeThat)

    return newFeature
}

fun findClosestDirection(reference: Double, option1: Double, option2: Double): Double {
    val distance1 = abs(reference - option1)
    val distance2 = abs(reference - option2)

    // Handle cases where directions wrap around the circle (0 to 360 degrees)
    val adjustedDistance1 = min(distance1, 360 - distance1)
    val adjustedDistance2 = min(distance2, 360 - distance2)

    return if (adjustedDistance1 < adjustedDistance2) option1 else option2
}

fun searchFeaturesByName(featureCollection: FeatureCollection, query: String): FeatureCollection {
    val results = FeatureCollection()
    for (feature in featureCollection) {
        val name = feature.properties?.get("name") as? String
        if (name != null && name.contains(query, ignoreCase = true)) {
            results.addFeature(feature)
        }
    }
    return results
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
    intersectionRoadNames: FeatureCollection,
    testNearestRoad:Feature?
): Int {
    //println("Number of roads that make up intersection ${intersectionNumber}: ${intersectionRoadNames.features.size}")
    if(testNearestRoad == null)
        return 0

    var needsFurtherChecking = 0
    val setofNames = emptySet<String>().toMutableSet()
    for (road in intersectionRoadNames) {
        val roadName = road.properties?.get("name")
        val isMatch = testNearestRoad.properties?.get("name") == roadName
        val nameIsDefault = road.properties?.get("default_name") != null

        if (isMatch) {
            // Ignore the road we're on
        } else if(nameIsDefault) {
            // Give no points to ways named from their type
        }
        else if(roadName != null) {
            val name = roadName.toString()
            if(setofNames.contains(name)) {
                // Don't increment the priority if the name is here for the second time
            } else {
                needsFurtherChecking++
                setofNames.add(name)
            }
        }
    }
    return needsFurtherChecking
}

/**
 * Given a Feature Collection that contains a LineString it will return a Feature Collection
 * that contains the "exploded" LineString which are the individual segments of the LineString.
 * @param featureCollection
 * A FeatureCollection containing the whole LineString
 * @return a Feature Collection containing the segments of the LineString.
 */
fun explodeLineString(featureCollection: FeatureCollection): FeatureCollection {
    val explodedFeatureCollection = FeatureCollection()

    for (feature in featureCollection.features) {
        if (feature.geometry is LineString) {
            val lineString = feature.geometry as LineString
            val coordinates = lineString.coordinates

            for (i in 0 until coordinates.size - 1) {
                val start = coordinates[i]
                val end = coordinates[i + 1]

                val segmentLineString = LineString().also {
                    it.coordinates = arrayListOf(start, end)
                }

                val segmentFeature = Feature().also {
                    it.geometry = segmentLineString
                }

                explodedFeatureCollection.addFeature(segmentFeature)
            }
        }
    }

    return explodedFeatureCollection
}

/**
 * Given a Feature Collection that contains a LineString it will return a Feature Collection of Points
 * that trace along the LineString at given distance intervals.
 * This is useful for faking the locations of a user walking down a road but isn't super accurate if you
 * enter big distance intervals and the LineString has lots of curves or bends.
 * @param featureCollection
 * A FeatureCollection containing the LineString we want to trace along.
 * @param distanceBetweenPoints
 * The distance between the Points tracing the LineString.
 * @return a Feature Collection containing the Points tracing the LineString.
 */
fun traceLineString(
    featureCollection: FeatureCollection,
    distanceBetweenPoints: Double
):FeatureCollection {
    val pointFeatures = FeatureCollection()
    var pointId = 1

    for (feature in featureCollection.features) {
        if (feature.geometry is LineString) {
            val lineString = feature.geometry as LineString
            val coordinates = lineString.coordinates

            // Add the first point of the LineString
            val firstPointFeature = Feature().also {
                val ars3: HashMap<String, Any?> = HashMap()
                ars3 += Pair("id", pointId++)
                it.properties = ars3
                it.geometry =Point(coordinates[0].longitude, coordinates[0].latitude)
            }
            pointFeatures.addFeature(firstPointFeature)

            var currentDistance = 0.0 // Accumulated distance along the LineString
            var previousPoint = coordinates[0]

            for (i in 1 until coordinates.size) {
                val currentPoint = coordinates[i]
                val segmentDistance = distance(previousPoint.latitude, previousPoint.longitude, currentPoint.latitude, currentPoint.longitude)

                // Calculate the remaining distance needed to reach the next point
                val remainingDistanceToNextPoint = distanceBetweenPoints - currentDistance

                // If the segment is longer than the remaining distance, add a point
                if (segmentDistance >= remainingDistanceToNextPoint) {
                    val fraction = remainingDistanceToNextPoint / segmentDistance
                    val interpolatedPoint = interpolate(previousPoint, currentPoint, fraction)

                    val pointFeature = Feature().also {
                        val ars3: HashMap<String, Any?> = HashMap()
                        ars3 += Pair("id", pointId++)
                        it.properties = ars3
                        it.geometry = Point(interpolatedPoint.longitude, interpolatedPoint.latitude)
                    }
                    pointFeatures.addFeature(pointFeature)

                    // Update currentDistance and previousPoint
                    currentDistance = 0.0 // Reset for the next point
                    previousPoint = interpolatedPoint

                    // Recalculate segmentDistance for the remaining part of the segment
                    val remainingSegmentDistance = distance(previousPoint.latitude, previousPoint.longitude, currentPoint.latitude, currentPoint.longitude)

                    // If there's still distance to cover in the segment, add more points
                    if (remainingSegmentDistance > distanceBetweenPoints) {
                        var segmentCurrentDistance = 0.0 // Distance covered within the current segment

                        while (segmentCurrentDistance + distanceBetweenPoints <= remainingSegmentDistance) {
                            segmentCurrentDistance += distanceBetweenPoints
                            val innerFraction = segmentCurrentDistance / remainingSegmentDistance
                            val innerInterpolatedPoint = interpolate(previousPoint, currentPoint, innerFraction)

                            val innerPointFeature = Feature().also {
                                val ars3: HashMap<String, Any?> = HashMap()
                                ars3 += Pair("id", pointId++)
                                it.properties = ars3
                                it.geometry = Point(innerInterpolatedPoint.longitude, innerInterpolatedPoint.latitude)
                            }
                            pointFeatures.addFeature(innerPointFeature)
                        }
                    }
                } else {
                    // If the segment is shorter than the remaining distance, accumulate the distance
                    currentDistance += segmentDistance
                }

                // Update previousPoint for the next segment
                previousPoint = currentPoint
            }

            // Add the last point of the LineString
            val lastPointFeature = Feature().also {
                val ars3: HashMap<String, Any?> = HashMap()
                ars3 += Pair("id", pointId++)
                it.properties = ars3
                it.geometry = Point(previousPoint.longitude, previousPoint.latitude)
            }
            pointFeatures.addFeature(lastPointFeature)
        }
    }

    return pointFeatures
}
/**
 * Given a Feature Collection that contains a Polygon it will return a Feature Collection
 * that contains the "exploded" Polygon which are the individual segments of the Polygon as LineStrings.
 * @param featureCollection
 * A FeatureCollection containing the whole Polygon
 * @return a Feature Collection containing the segments of the Polygon.
 */
fun explodePolygon(featureCollection: FeatureCollection): FeatureCollection {
    val explodedFeatureCollection = FeatureCollection()

    for (feature in featureCollection.features) {
        if (feature.geometry is Polygon) {
            val polygon = feature.geometry as Polygon
            val coordinates = polygon.coordinates[0]

            for (i in 0 until coordinates.size - 1) {
                val start = coordinates[i]
                val end = coordinates[i + 1]

                val segmentLineString = LineString().also {
                    it.coordinates = arrayListOf(start, end)
                }

                val segmentFeature = Feature().also {
                    it.geometry = segmentLineString
                }

                explodedFeatureCollection.addFeature(segmentFeature)
            }
        }
    }

    return explodedFeatureCollection
}


fun interpolate(
    point1: LngLatAlt,
    point2: LngLatAlt,
    fraction: Double
): LngLatAlt {
    val lon = point1.longitude + (point2.longitude - point1.longitude) * fraction
    val lat = point1.latitude + (point2.latitude - point1.latitude) * fraction
    return LngLatAlt(lon, lat)
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

    // Create a HashMap of any polygons with the same osm_ids. Each hash map entry contains a List
    // of FeatureCollections. Each FeatureCollections contains one or more polygons. When there's
    // more than one, they've been tested to see if they overlap.
    val features = hashMapOf<Any, MutableList<FeatureCollection> >()
    for (feature in polygonFeatureCollection.features) {
        if(feature.geometry.type == "Polygon") {
            val osmId = feature.foreign?.get("osm_ids")
            if (osmId != null) {
                if (!features.containsKey(osmId)) {
                    // This is the first feature with this osm_id
                    features[osmId] = emptyList<FeatureCollection>().toMutableList()
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
                mergedFeature = if (index == 0) {
                    feature
                } else {
                    mergePolygons(mergedFeature!!, feature)
                }
            }
            resultantFeatureCollection.addFeature(mergedFeature!!)
        }
    }
    return resultantFeatureCollection
}

fun isPolygonClockwise(
    feature: Feature
): Boolean {
    // get outer ring coordinates (don't care about inner rings at the moment)
    val coordinates = (feature.geometry as Polygon).coordinates[0]
    var area = 0.0
    val n = coordinates.size
    for(i in 0 until n) {
        val j = (i + 1) % n
        area += (coordinates[j].longitude - coordinates[i].longitude) * (coordinates[j].latitude + coordinates[i].latitude)

    }
    return area > 0
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

    val result: MutableList<LinearRing> = emptyList<LinearRing>().toMutableList()
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
    val mergedGeometryJTS = polygon1GeometryJTS?.union(polygon2GeometryJTS) as JtsPolygon
    // create a new Polygon with a single outer ring using the coordinates from the JTS merged geometry
    val mergedPolygon = Feature().also { feature ->
        feature.properties = polygon1.properties
        feature.foreign = polygon1.foreign
        feature.type = "Feature"
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
 * String for super category. Options are "information", "object", "place", "landmark", "mobility", "safety"
 * @return a mutable list of things in the super category.
 */
fun getSuperCategoryElements(category: String): MutableList<String> {
    return when (category) {
        "information" -> mutableListOf(
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

        "object" -> mutableListOf(
            "turntable",
            "survey_point",
            "snow_net",
            "silo",
            "mast",
            "bird_hide",
            "transformer_tower",
            "generic_object",
            "waste_basket",
            "post_box",
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
            "street_lamp",
            "shed",
            "traffic_cones",
            "firepit",
            "bench",
            "grit_bin",
            "stone",
            "surveillance",
            "street_cabinet",
            "monitoring_station",
            "wayside_shrine",
            "wayside_cross",
            "tomb",
            "traffic_signals",
            "fire_hydrant",
            "hut",
            "static_caravan",
            "bollard",
            "block",
            "waste_disposal",
            "photo_booth",
            "bbq",
            "telephone"
        )

        "place" -> mutableListOf(
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

        "landmark" -> mutableListOf(
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

        "mobility" -> mutableListOf(
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

        "safety" -> mutableListOf(
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

        else -> mutableListOf("Unknown category")
    }
}

fun generateDebugFovGeoJson(
    userGeometry: UserGeometry,
    featureCollection: FeatureCollection
) {
    val triangle = getFovTriangle(userGeometry)

    // Take care not to add the triangle to the passed in collection
    val mapCollection = FeatureCollection()
    mapCollection.plusAssign(featureCollection)

    val triangleFeature = Feature()
    triangleFeature.geometry = Polygon(
        arrayListOf(
            triangle.origin,
            triangle.left,
            triangle.right,
            userGeometry.location
        )
    )
    mapCollection.addFeature(triangleFeature)

    val outputFile = FileOutputStream("markers.geojson")
    val adapter = GeoJsonObjectMoshiAdapter()
    outputFile.write(adapter.toJson(mapCollection).toByteArray())
    outputFile.close()
}
