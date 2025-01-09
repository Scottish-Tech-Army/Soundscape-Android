package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.dto.IntersectionRelativeDirections
import org.scottishtecharmy.soundscape.dto.Tile
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import com.squareup.moshi.Moshi
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import java.io.FileOutputStream
import java.lang.Math.toDegrees
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
 * Given a valid Tile feature collection this will parse the collection and return a roads
 * feature collection. Uses the "highway" feature_type to extract roads from GeoJSON.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return A FeatureCollection object that contains only roads.
 */
fun getRoadsFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection {

    val roadsFeatureCollection = FeatureCollection()

    // Original Soundscape excludes the below feature_value (s) even though they have the
    // feature_type == highway
    // and creates a separate Paths Feature Collection for them
    // "footway", "path", "cycleway", "bridleway"
    // gd_intersection are a special case and get their own Intersections Feature Collection


    for (feature in tileFeatureCollection) {
        feature.foreign?.let { foreign ->
            if (foreign["feature_type"] == "highway"
                && foreign["feature_value"] != "gd_intersection"
                && foreign["feature_value"] != "footway"
                && foreign["feature_value"] != "path"
                && foreign["feature_value"] != "cycleway"
                && foreign["feature_value"] != "bridleway"
                && foreign["feature_value"] != "bus_stop"
                && foreign["feature_value"] != "crossing") {
                    // We're only going to add linestrings to the roads feature collection
                    when(feature.geometry.type) {
                        "LineString", "MultiLineString" ->
                            roadsFeatureCollection.addFeature(feature)
                    }
            }
        }
    }
    return roadsFeatureCollection
}

/**
 * Given a valid Tile feature collection this will parse the collection and return a bus stops
 * feature collection. Uses the "bus_stop" feature_value to extract bus stops from GeoJSON.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return A FeatureCollection object that contains only bus stops.
 */
fun getBusStopsFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection{
    val busStopFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        feature.foreign?.let { foreign ->
            if (foreign["feature_type"] == "highway" && foreign["feature_value"] == "bus_stop") {
                busStopFeatureCollection.addFeature(feature)
            }
        }
    }

    return busStopFeatureCollection
}

/**
 * Given a valid Tile feature collection this will parse the collection and return a crossing
 * feature collection. Uses the "crossing" feature_value to extract crossings from GeoJSON.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return A FeatureCollection object that contains only crossings.
 */
fun getCrossingsFromTileFeatureCollection(tileFeatureCollection: FeatureCollection): FeatureCollection{
    val crossingsFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        feature.foreign?.let { foreign ->
            if (foreign["feature_type"] == "highway" && foreign["feature_value"] == "crossing") {
                crossingsFeatureCollection.addFeature(feature)
            }
        }
    }
    return crossingsFeatureCollection
}

/**
 * Given a valid Tile feature collection this will parse the collection and return an interpolation
 * points feature collection. Uses the "edgePoint" feature_value to extract crossings from GeoJSON.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return A FeatureCollection object that contains only edgePoints
 */
fun getInterpolationPointsFromTileFeatureCollection(tileFeatureCollection: FeatureCollection): FeatureCollection{
    val interpolationPointsFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        feature.properties?.let { properties ->
            if (properties["class"] == "edgePoint") {
                interpolationPointsFeatureCollection.addFeature(feature)
            }
        }
    }
    return interpolationPointsFeatureCollection
}

/**
 * Given a valid Tile feature collection this will parse the collection and return a paths
 * feature collection. Uses the "footway", "path", "cycleway", "bridleway" feature_value to extract
 * Paths from Feature Collection.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return A FeatureCollection object that contains only paths.
 */
fun getPathsFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection{
    val pathsFeatureCollection = FeatureCollection()

    for(feature in tileFeatureCollection) {
        feature.foreign?.let { foreign ->
            // We're only going to add linestrings to the roads feature collection
            when(feature.geometry.type) {
                "LineString", "MultiLineString" -> {
                    if (foreign["feature_type"] == "highway")
                        when (foreign["feature_value"]) {
                            "footway" -> pathsFeatureCollection.addFeature(feature)
                            "path" -> pathsFeatureCollection.addFeature(feature)
                            "cycleway" -> pathsFeatureCollection.addFeature(feature)
                            "bridleway" -> pathsFeatureCollection.addFeature(feature)
                        }
                }
            }
        }
    }
    return pathsFeatureCollection
}

/**
 * Parses out all the Intersections in a tile FeatureCollection using the "gd_intersection" feature_value.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return a Feature collection object that only contains intersections.
 */
fun getIntersectionsFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection {
    val intersectionsFeatureCollection = FeatureCollection()
    // split out the intersections into their own intersections FeatureCollection
    for (feature in tileFeatureCollection) {
        feature.foreign?.let { foreign ->
            if (foreign["feature_type"] == "highway" && foreign["feature_value"] == "gd_intersection") {
                intersectionsFeatureCollection.addFeature(feature)
            }
        }
    }
    return intersectionsFeatureCollection
}

/**
 * Parses out all the Entrances in a tile FeatureCollection using the "gd_entrance_list" feature_type.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return a feature collection object that contains only entrances.
 */
fun getEntrancesFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection {
    val entrancesFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        feature.foreign?.let { foreign ->
            if (foreign["feature_type"] == "gd_entrance_list") {
                entrancesFeatureCollection.addFeature(feature)
            }
        }
    }
    return entrancesFeatureCollection
}

/**
 * Parses out all the Points of Interest (POI) in a tile FeatureCollection.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return a Feature collection object that contains only POI.
 */
fun getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection {
    val poiFeaturesCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        feature.foreign?.let { foreign ->
            if (foreign["feature_type"] != "highway" && foreign["feature_type"] != "gd_entrance_list") {
                poiFeaturesCollection.addFeature(feature)
            }
        }
    }
    return poiFeaturesCollection
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
                }
            }
        }
    }
    return tempFeatureCollection
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

/**
 * Parses out roads, paths, intersections, entrances, pois, bus stops and crossings from a tile string.
 * @return a TileData object with the string parsed into separate strings.
 */

fun processTileString(tileString: String): Array<FeatureCollection> {
    val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
    val tileFeatureCollection = moshi.adapter(FeatureCollection::class.java)
        .fromJson(tileString)
    if(tileFeatureCollection == null)
        return emptyArray()

    return processTileFeatureCollection(tileFeatureCollection)
}

fun processTileFeatureCollection(tileFeatureCollection: FeatureCollection): Array<FeatureCollection> {

    val tileData = Array(GeoEngine.Fc.MAX_COLLECTION_ID.id) { FeatureCollection() }

    // We have separate collections for the different types of Feature. ROADS_AND_PATHS adds PATHS
    // to the ROADS features already contained in ROADS. This slight extra cost in terms of memory
    // is made up for by the ease of searching a single collection.
    tileData[GeoEngine.Fc.ROADS.id] = getRoadsFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
    tileData[GeoEngine.Fc.ROADS_AND_PATHS.id] = getPathsFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
    tileData[GeoEngine.Fc.ROADS_AND_PATHS.id].plusAssign(tileData[GeoEngine.Fc.ROADS.id])
    tileData[GeoEngine.Fc.INTERSECTIONS.id] = getIntersectionsFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
    tileData[GeoEngine.Fc.ENTRANCES.id] = getEntrancesFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
    tileData[GeoEngine.Fc.POIS.id] = getPointsOfInterestFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
    tileData[GeoEngine.Fc.BUS_STOPS.id] = getBusStopsFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
    tileData[GeoEngine.Fc.CROSSINGS.id] = getCrossingsFromTileFeatureCollection(tileFeatureCollection)
    tileData[GeoEngine.Fc.INTERPOLATIONS.id] = getInterpolationPointsFromTileFeatureCollection(tileFeatureCollection)

    return  tileData
}

data class FovLeftRight(val left: LngLatAlt, val right: LngLatAlt)

fun getFovTrianglePoints(location: LngLatAlt,
                         heading: Double,
                         distance: Double) : FovLeftRight {
    // Direction the device is pointing
    val quadrants = getQuadrants(heading)
    // get the quadrant index from the heading so we can construct a FOV triangle using the correct quadrant
    var quadrantIndex = 0
    for (quadrant in quadrants) {
        val containsHeading = quadrant.contains(heading)
        if (containsHeading) {
            break
        } else {
            quadrantIndex++
        }
    }
    // Get the coordinate for the "Left" of the FOV
    val points = FovLeftRight(
        getDestinationCoordinate(
            LngLatAlt(location.longitude, location.latitude),
            quadrants[quadrantIndex].left,
            distance
        ),
        getDestinationCoordinate(
            LngLatAlt(location.longitude, location.latitude),
            quadrants[quadrantIndex].right,
            distance
        )
    )

    return points
}

/**
 * Return a Feature Collection that contains the Features within the "field of view" triangle.
 * @param location
 * Location of the device.
 * @param heading
 * Direction the device is pointing.
 * @param distance
 * Distance to extend the "field of view"
 * @param featureTree
 * The feature tree that we want to filter.
 * @return A Feature Collection that contains the Features in the FOV triangle.
 */
fun getFovFeatureCollection(
    location: LngLatAlt,
    heading: Double,
    distance: Double,
    featureTree: FeatureTree
): FeatureCollection {

    val points = getFovTrianglePoints(location, heading, distance)
    return featureTree.generateFeatureCollectionWithinTriangle(location, points.left, points.right)
}

fun getNearestFovFeature(
    location: LngLatAlt,
    heading: Double,
    distance: Double,
    featureTree: FeatureTree
): Feature? {

    val points = getFovTrianglePoints(location, heading, distance)
    return featureTree.getNearestFeatureWithinTriangle(location, points.left, points.right)
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

/**
 * Get nearest road from roads Feature Collection.
 * WARNING: It doesn't care which direction the road is.
 * Roads can contain crossings which are Points not LineStrings.
 * @param currentLocation
 * Location of device.
 * @param roadFeatureCollection
 * The intersection feature collection that contains the intersections we want to test.
 * @return A Feature that is the nearest road.
 */
fun getNearestRoad(
    currentLocation: LngLatAlt,
    roadFeatureCollection: FeatureCollection
): Feature? {

    //TODO I have no idea if roads can also be represented with MultiLineStrings.
    // In which case this will fail. Need to have a look at some tiles with motorways/dual carriageways

    var maxDistanceToRoad = Int.MAX_VALUE.toDouble()
    var nearestRoad : Feature? = null

    for (feature in roadFeatureCollection) {
        if (feature.geometry.type == "LineString") {
            val distanceToRoad = distanceToLineString(
                currentLocation,
                (feature.geometry as LineString)
            )
            if (distanceToRoad < maxDistanceToRoad) {
                nearestRoad = feature
                maxDistanceToRoad = distanceToRoad
            }
        } else if (feature.geometry.type == "Polygon") {
            val distanceToRoad = distanceToPolygon(
                currentLocation,
                (feature.geometry as Polygon)
            )
            if (distanceToRoad < maxDistanceToRoad) {
                nearestRoad = feature
                maxDistanceToRoad = distanceToRoad
            }
        }  else {
            val distanceToRoad = currentLocation.distance(
                LngLatAlt((feature.geometry as Point).coordinates.latitude,
                          (feature.geometry as Point).coordinates.longitude)
            )
            if (distanceToRoad < maxDistanceToRoad) {
                nearestRoad = feature
                maxDistanceToRoad = distanceToRoad
            }
        }
    }

    // TODO As the distance to the road has already been calculated
    //  perhaps we could insert the distance to the road as a property/foreign member of the Feature?
    return nearestRoad
}

fun getFeatureNearestPoint(
    currentLocation: LngLatAlt,
    feature: Feature
): LngLatAlt? {

    // Get the bounding box for the feature, and return the nearest point on it
    val box = getBoundingBoxForFeature(feature) ?: return null
    return nearestPointOnBoundingBox(box, currentLocation)
}

/**
 * Given a Feature this will return its bounding box.
 * @param feature
 * @return a bounding box
 */
private fun getBoundingBoxForFeature(
    feature: Feature?
): BoundingBox? {
    if(feature == null) return null

    // Return the bounding box for the Feature
    when (feature.geometry.type) {
        "Point"             -> return getBoundingBoxOfPoint(feature.geometry as Point)
        "MultiPoint"        -> return getBoundingBoxOfMultiPoint(feature.geometry as MultiPoint)
        "LineString"        -> return getBoundingBoxOfLineString(feature.geometry as LineString)
        "MultiLineString"   -> return getBoundingBoxOfMultiLineString(feature.geometry as MultiLineString)
        "Polygon"           -> return getBoundingBoxOfPolygon(feature.geometry as Polygon)
        "MultiPolygon"      -> return getBoundingBoxOfMultiPolygon(feature.geometry as MultiPolygon)
        else                -> println("Unknown type ${feature.geometry.type}")
    }
    return null
}

/**
 * Given a Feature and a location this will calculate the nearest distance to it
 * @param currentLocation
 * Current location as LngLatAlt
 * @param feature
 * @return The distance between currentLocation and feature
 */
fun getDistanceToFeature(
    currentLocation: LngLatAlt,
    feature: Feature?
): Double {
    if(feature == null) return Double.NaN

    when (feature.geometry.type) {
        "Point" -> {
            val point = feature.geometry as Point
            val distanceToFeaturePoint = currentLocation.distance(
                LngLatAlt(point.coordinates.longitude, point.coordinates.latitude)
            )
            return distanceToFeaturePoint
        }

        "MultiPoint" -> {
            val multiPoint = feature.geometry as MultiPoint
            var shortestDistance = Double.MAX_VALUE

            for (point in multiPoint.coordinates) {
                val distanceToPoint = currentLocation.distance(point)
                if (distanceToPoint < shortestDistance) {
                    shortestDistance = distanceToPoint
                }
            }
            // this is the closest point to the current location from the collection of points
            return shortestDistance
        }

        "LineString" -> {
            val lineString = feature.geometry as LineString
            val distanceToFeatureLineString = distanceToLineString(
                currentLocation,
                lineString
            )
            return distanceToFeatureLineString
        }

        "MultiLineString" -> {
            val multiLineString = feature.geometry as MultiLineString
            var shortestDistance = Double.MAX_VALUE

            for (arrCoordinates in multiLineString.coordinates) {
                for (coordinate in arrCoordinates) {
                    val distanceToPoint = currentLocation.distance(
                        LngLatAlt(coordinate.longitude, coordinate.latitude)
                    )
                    if (distanceToPoint < shortestDistance) {
                        shortestDistance = distanceToPoint
                    }
                }
            }
            return shortestDistance
        }

        "Polygon" -> {
            val polygon = feature.geometry as Polygon
            val distanceToFeaturePolygon = distanceToPolygon(
                currentLocation,
                polygon
            )
            return distanceToFeaturePolygon
        }

        "MultiPolygon" -> {
            val multiPolygon = feature.geometry as MultiPolygon
            var shortestDistance = Double.MAX_VALUE

            for (arrCoordinates in multiPolygon.coordinates) {
                for (arr in arrCoordinates) {
                    for (coordinate in arr) {
                        val distanceToPoint = currentLocation.distance(
                            LngLatAlt(coordinate.longitude, coordinate.latitude)
                        )
                        if (distanceToPoint < shortestDistance) {
                            shortestDistance = distanceToPoint
                        }
                    }
                }
            }
            // this is the shortest distance from current location to the collection of Polygons
            return shortestDistance
        }

        else -> {
            println("Unknown type ${feature.geometry.type}")
            return Double.NaN
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
        feature.foreign?.put("distance_to", getDistanceToFeature(currentLocation, feature))
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
 * Given a location, device heading and distance this will create a feature collection of triangles
 * that represent relative directions for the given heading. The triangles represent "ahead", "ahead right", "right", "behind right"
 * "behind", "behind left", "left" and "ahead left" this represents original Soundscapes COMBINED direction type.
 * @param location
 * LngLatAlt object
 * @param deviceHeading
 * Direction the device is pointing in degrees
 * @param distance
 * Length of left and right side of triangle in meters.
 * @return a Feature Collection containing triangles for the relative directions.
 */
fun getCombinedDirectionPolygons(
    location: LngLatAlt,
    deviceHeading: Double,
    distance: Double = 50.0
): FeatureCollection {

    val newFeatureCollection = FeatureCollection()

    val triangleDirectionsQuad = Quadrant(deviceHeading)
    // Take the  original 45 degree "ahead"/quadrant triangle and making
    // it a 30 degree "behind" triangle
    val triangle5Left = (triangleDirectionsQuad.left + 210.0) % 360.0
    val triangle5Right = (triangleDirectionsQuad.right + 150.0) % 360.0
    val degreesList = mutableListOf(Pair(triangle5Left, triangle5Right))

    // Take the  original 45 degree "ahead"/quadrant triangle and making
    // it a 30 degree "behind left" triangle
    val triangle6Left = (triangleDirectionsQuad.left + 240.0) % 360.0
    val triangle6Right = (triangleDirectionsQuad.right + 210.0) % 360.0
    degreesList.add(Pair(triangle6Left, triangle6Right))

    // Take the original 45 degree "ahead"/quadrant triangle and making
    // it a 30 degree "left" triangle
    val triangle7Left = (triangleDirectionsQuad.left + 300.0) % 360.0
    val triangle7Right = (triangleDirectionsQuad.right + 240.0) % 360.0
    degreesList.add(Pair(triangle7Left, triangle7Right))

    // Take the original 45 degree "ahead"/quadrant triangle and making
    // it a 60 degree "ahead left" triangle
    val triangle8Left = (triangleDirectionsQuad.left - 30.0) % 360.0
    val triangle8Right = (triangleDirectionsQuad.right - 60.0) % 360.0
    degreesList.add(Pair(triangle8Left, triangle8Right))

    // Take the original 45 degree "ahead"/quadrant triangle and cutting it down
    // to a 30 degree "ahead" triangle
    val triangle1Left = (triangleDirectionsQuad.left + 30.0) % 360.0
    val triangle1Right = (triangleDirectionsQuad.right - 30.0) % 360.0
    degreesList.add(Pair(triangle1Left, triangle1Right))

    // Take the original 45 degree "ahead"/quadrant triangle and making
    // it a 60 degree "ahead right" triangle
    val triangle2Left = (triangleDirectionsQuad.left + 60.0) % 360.0
    val triangle2Right = (triangleDirectionsQuad.right + 30.0) % 360.0
    degreesList.add(Pair(triangle2Left, triangle2Right))

    // Take the original 45 degree "ahead"/quadrant triangle and making
    // it a 30 degree "right" triangle
    val triangle3Left = (triangleDirectionsQuad.left + 120.0) % 360.0
    val triangle3Right = (triangleDirectionsQuad.right + 60.0) % 360.0
    degreesList.add(Pair(triangle3Left, triangle3Right))

    // Take the  original 45 degree "ahead"/quadrant triangle and making
    // it a 60 degree "behind right" triangle
    val triangle4Left = (triangleDirectionsQuad.left + 150.0) % 360.0
    val triangle4Right = (triangleDirectionsQuad.right + 120.0) % 360.0
    degreesList.add(Pair(triangle4Left, triangle4Right))

    for ((count, degreePair) in degreesList.withIndex()) {
        val ahead1 = getDestinationCoordinate(
            location,
            degreePair.first,
            distance
        )
        val ahead2 = getDestinationCoordinate(
            location,
            degreePair.second,
            distance
        )
        val aheadTriangle = createTriangleFOV(
            ahead1,
            location,
            ahead2
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
 * Given a location, device heading and distance this will create a feature collection of triangles
 * that represent relative directions for the given heading. The triangles represent "ahead", "right",
 * "behind", "left". This represents original Soundscapes INDIVIDUAL direction type.
 * @param location
 * LngLatAlt object
 * @param deviceHeading
 * Direction the device is pointing in degrees
 * @param distance
 * Length of left and right side of triangle in meters.
 * @return a Feature Collection containing triangles for the relative directions.
 */
fun getIndividualDirectionPolygons(
    location: LngLatAlt,
    deviceHeading: Double,
    distance: Double = 50.0
): FeatureCollection {

    // Take the original 45 degree "ahead"
    val triangle1DirectionsQuad = Quadrant(deviceHeading)
    // I got the ordering wrong with this originally as Soundscape works clockwise starting from behind, left, ahead, right
    // so my triangle numbering looks weird
    val triangle3Left = (triangle1DirectionsQuad.left + 180.0) % 360.0
    val triangle3Right = (triangle1DirectionsQuad.right + 180.0) % 360.0
    val degreesList = mutableListOf(Pair(triangle3Left, triangle3Right))
    val triangle4Left = (triangle1DirectionsQuad.left + 270.0) % 360.0
    val triangle4Right = (triangle1DirectionsQuad.right + 270.0) % 360.0
    degreesList.add(Pair(triangle4Left, triangle4Right))
    val triangle1Left = triangle1DirectionsQuad.left
    val triangle1Right = triangle1DirectionsQuad.right
    degreesList.add(Pair(triangle1Left, triangle1Right))
    val triangle2Left = (triangle1DirectionsQuad.left + 90.0) % 360.0
    val triangle2Right = (triangle1DirectionsQuad.right + 90.0) % 360.0
    degreesList.add(Pair(triangle2Left, triangle2Right))

    return makeTriangles(degreesList, location, distance)

}

/**
 * Given a location, device heading and distance this will create a feature collection of triangles
 * that represent relative directions for the given heading. The triangles represent "ahead", "right",
 * "behind", "left". This represents original Soundscapes AHEAD_BEHIND direction type.
 * @param location
 * LngLatAlt object
 * @param deviceHeading
 * Direction the device is pointing in degrees
 * @param distance
 * Length of left and right side of triangle in meters.
 * @return a Feature Collection containing triangles for the relative directions.
 */
fun getAheadBehindDirectionPolygons(
    location: LngLatAlt,
    deviceHeading: Double,
    distance: Double = 50.0
): FeatureCollection {

    // Take the original 45 degree "ahead" and give it a bias for ahead (285 to 75 degrees)
    val triangle1DirectionsQuad = Quadrant(deviceHeading)
    // I got the ordering wrong with this originally as Soundscape works clockwise from behind, left, ahead, right
    // so my triangle numbering looks weird
    val triangle3Left = (triangle1DirectionsQuad.left + 150.0) % 360.0
    val triangle3Right = (triangle1DirectionsQuad.right + 210.0) % 360.0
    val degreesList = mutableListOf(Pair(triangle3Left, triangle3Right))

    val triangle4Left = (triangle1DirectionsQuad.left + 300.0) % 360.0
    val triangle4Right = (triangle1DirectionsQuad.right + 240.0) % 360.0
    degreesList.add(Pair(triangle4Left, triangle4Right))

    val triangle1Left = (triangle1DirectionsQuad.left - 30.0) % 360.0
    val triangle1Right = (triangle1DirectionsQuad.right + 30.0) % 360
    degreesList.add(Pair(triangle1Left, triangle1Right))

    val triangle2Left = (triangle1DirectionsQuad.left + 120.0) % 360.0
    val triangle2Right = (triangle1DirectionsQuad.right + 60.0) % 360.0
    degreesList.add(Pair(triangle2Left, triangle2Right))

    return makeTriangles(degreesList, location, distance)
}

/**
* Given a list of Pairs() of degrees which represent left and right for a FoV triangle, a location
 * and a distance it wil generate lots of triangles
* @param degreesList
* A MutableList<Pair<Double, Double>> of degrees to construct triangles
* @param location
* location to radiate triangles from
* @param distance
* Length of left and right side of triangle in meters.
* @return a Feature Collection containing triangles for the relative directions.
*/
fun makeTriangles(
    degreesList: MutableList<Pair<Double, Double>>,
    location: LngLatAlt,
    distance: Double,
): FeatureCollection{

    val newFeatureCollection = FeatureCollection()
    for ((count, degreePair) in degreesList.withIndex()) {
        val ahead1 = getDestinationCoordinate(
            location,
            degreePair.first,
            distance
        )
        val ahead2 = getDestinationCoordinate(
            location,
            degreePair.second,
            distance
        )
        val aheadTriangle = createTriangleFOV(
            ahead1,
            location,
            ahead2
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
    val coordinateFound = roadCoordinates.any{ it.latitude == intersectionCoordinate.latitude && it.longitude == intersectionCoordinate.longitude}

    return if (intersectionCoordinate.longitude == roadCoordinates.first().longitude && intersectionCoordinate.latitude == roadCoordinates.first().latitude) {
        RoadDirectionAtIntersection.LEADING
    } else if (intersectionCoordinate.longitude == roadCoordinates.last().longitude && intersectionCoordinate.latitude == roadCoordinates.last().latitude) {
        RoadDirectionAtIntersection.TRAILING
    } else if (coordinateFound) {
        RoadDirectionAtIntersection.LEADING_AND_TRAILING
    } else {
        RoadDirectionAtIntersection.NONE
    }
}


/**
 * Given a location, device heading and distance this will create a feature collection of triangles
 * that represent relative directions for the given heading. The triangles represent "ahead", "right",
 * "behind", "left". This represents original Soundscapes LEFT_RIGHT direction type.
 * @param location
 * LngLatAlt object
 * @param deviceHeading
 * Direction the device is pointing in degrees
 * @param distance
 * Length of left and right side of triangle in meters.
 * @return a Feature Collection containing triangles for the relative directions.
 */
fun getLeftRightDirectionPolygons(
    location: LngLatAlt,
    deviceHeading: Double,
    distance: Double = 50.0
): FeatureCollection {

    val newFeatureCollection = FeatureCollection()
    // Take the original 45 degree "ahead" and give it a bias for left and right (ahead is 330 to 30 degrees)
    val triangle1DirectionsQuad = Quadrant(deviceHeading)
    // Behind
    val triangle3Left = (triangle1DirectionsQuad.left + 195.0) % 360.0
    val triangle3Right = (triangle1DirectionsQuad.right + 165.0) % 360.0
    val degreesList = mutableListOf(Pair(triangle3Left, triangle3Right))

    // Left
    val triangle4Left = (triangle1DirectionsQuad.left + 255.0) % 360.0
    val triangle4Right = (triangle1DirectionsQuad.right + 285.0) % 360.0
    degreesList.add(Pair(triangle4Left, triangle4Right))
    // Ahead
    val triangle1Left = (triangle1DirectionsQuad.left + 15.0) % 360.0
    val triangle1Right = (triangle1DirectionsQuad.right - 15.0) % 360
    degreesList.add(Pair(triangle1Left, triangle1Right))
    // Right
    val triangle2Left = (triangle1DirectionsQuad.left + 75.0) % 360.0
    val triangle2Right = (triangle1DirectionsQuad.right + 105.0) % 360.0
    degreesList.add(Pair(triangle2Left, triangle2Right))

    for ((count, degreePair) in degreesList.withIndex()) {
        val ahead1 = getDestinationCoordinate(
            location,
            degreePair.first,
            distance
        )
        val ahead2 = getDestinationCoordinate(
            location,
            degreePair.second,
            distance
        )
        val aheadTriangle = createTriangleFOV(
            ahead1,
            location,
            ahead2
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
        if (testRoadDirectionAtIntersection == RoadDirectionAtIntersection.LEADING_AND_TRAILING){
            // split the road into two
            val roadCoordinatesSplitIntoTwo = splitRoadByIntersection(
                nearestIntersection,
                road
            )
            // for each split road work out the relative direction from the intersection
            for (splitRoad in roadCoordinatesSplitIntoTwo) {
                newFeatureCollection.plusAssign(getFeaturesWithRoadDirection(splitRoad, intersectionRelativeDirections))
            }
        }
        else{
            newFeatureCollection.plusAssign(getFeaturesWithRoadDirection(road, intersectionRelativeDirections))
        }
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
 * @param location
 * LngLatAlt object
 * @param deviceHeading
 * Direction the device is pointing in degrees
 * @param distance
 * Length of left and right side of triangle in meters.
 * @param relativeDirectionType
 * Enum for the function you want to use
 * @return a Feature Collection containing triangles for the relative directions.
 */
fun getRelativeDirectionsPolygons(
    location: LngLatAlt,
    deviceHeading: Double,
    distance: Double = 50.0,
    relativeDirectionType: RelativeDirections
): FeatureCollection {

    return when(relativeDirectionType){
        RelativeDirections.COMBINED -> getCombinedDirectionPolygons(location, deviceHeading, distance)
        RelativeDirections.INDIVIDUAL -> getIndividualDirectionPolygons(location, deviceHeading, distance)
        RelativeDirections.AHEAD_BEHIND -> getAheadBehindDirectionPolygons(location, deviceHeading, distance)
        RelativeDirections.LEFT_RIGHT -> getLeftRightDirectionPolygons(location, deviceHeading, distance)
    }
}

fun checkWhetherIntersectionIsOfInterest(
    intersectionRoadNames: FeatureCollection,
    testNearestRoad:Feature?
): Boolean {
    //println("Number of roads that make up intersection ${intersectionNumber}: ${intersectionRoadNames.features.size}")
    if(testNearestRoad == null)
        return false

    var needsFurtherChecking = true
    for (road in intersectionRoadNames) {
        val roadName = road.properties?.get("name")
        val isOneWay = road.properties?.get("oneway") == "yes"
        val isMatch = testNearestRoad.properties?.get("name") == roadName

        //println("The road name is: $roadName")
        if (isMatch && isOneWay) {
            //println("Intersection $intersectionNumber is probably a compound roundabout or compound intersection and we don't want to call it out.")
            needsFurtherChecking = false
        } else if (isMatch) {
            //println("Intersection $intersectionNumber is probably a compound roundabout or compound intersection and we don't want to call it out.")
            needsFurtherChecking = false
        } else {
            //println("Intersection $intersectionNumber is probably NOT a compound roundabout or compound intersection and we DO want to call it out.")
            needsFurtherChecking = true
            break
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

fun mergeAllPolygonsInFeatureCollection(
    polygonFeatureCollection: FeatureCollection
): FeatureCollection{
    val processOsmIds = mutableSetOf<Any>()
    val notDuplicateFeaturesFeatureCollection = FeatureCollection()
    val duplicateFeaturesFeatureCollection = FeatureCollection()

    for (feature in polygonFeatureCollection.features) {
        if (!isDuplicateByOsmId(processOsmIds, feature)) {
            notDuplicateFeaturesFeatureCollection.features.add(feature)
        } else {
            duplicateFeaturesFeatureCollection.features.add(feature)
        }
    }

    val mergedPolygonsFeatureCollection = FeatureCollection()
    val duplicateLineStringsAndPoints = FeatureCollection()
    val originalPolygonsUsedInMerge = mutableSetOf<Any>() // Track original polygons

    for (duplicate in duplicateFeaturesFeatureCollection) {
        // Find the original Feature
        val originalFeature = notDuplicateFeaturesFeatureCollection.features.find {
            it.foreign?.get("osm_ids") == duplicate.foreign?.get("osm_ids")
        }

        // Merge duplicate polygons
        if (originalFeature != null && originalFeature.geometry.type == "Polygon" && duplicate.geometry.type == "Polygon") {
            mergedPolygonsFeatureCollection.features.add(mergePolygons(originalFeature, duplicate))
            // Add to the set
            originalFeature.foreign?.get("osm_ids")?.let { originalPolygonsUsedInMerge.add(it) }
            // Add to the set
            duplicate.foreign?.get("osm_ids")?.let { originalPolygonsUsedInMerge.add(it) }
        } else {
            // TODO Merge the linestrings so we get a contiguous road/path
            if (duplicate.geometry.type == "LineString" || duplicate.geometry.type == "Point"){
                duplicateLineStringsAndPoints.features.add(duplicate)
            }
        }
    }

    val finalFeatureCollection = FeatureCollection()

    // Add merged Polygons
    finalFeatureCollection.features.addAll(mergedPolygonsFeatureCollection.features)


    // Add original Features but excluding the Polygons that were merged
    for (feature in notDuplicateFeaturesFeatureCollection.features) {
        if (!isDuplicateByOsmId(originalPolygonsUsedInMerge, feature)) { // Check object identity
            finalFeatureCollection.features.add(feature)
        }
    }

    // Add the duplicate linestrings and points back in... need to sort out/merge the linestrings at later date
    finalFeatureCollection.features.addAll(duplicateLineStringsAndPoints)

    // The original GeoJson from the MVT tile has some features that aren't valid GeoJSON and
    // GeoJSON.io is having a huff: "Polygons and MultiPolygons should follow the right hand rule"
    // so fix that.
    for (feature in finalFeatureCollection.features) {
        if (feature.geometry.type == "Polygon") {
            if (isPolygonClockwise(feature)){
                (feature.geometry as Polygon).coordinates[0].reverse()
            }
        }
    }

    //TODO: figure out why the MVT tile has a linestring with only one coordinate? GeoJSON has a huff about it
    val thisIsTheFinalFeatureCollectionHonest = FeatureCollection()
    for (feature in finalFeatureCollection) {
        if (feature.geometry.type == "LineString" && (feature.geometry as LineString).coordinates.size < 2 ){
            println("Bug: This is a linestring with only one coordinate")
        } else {
            thisIsTheFinalFeatureCollectionHonest.features.add(feature)
        }
    }

    return thisIsTheFinalFeatureCollectionHonest

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

fun mergePolygons(
    polygon1: Feature,
    polygon2: Feature
): Feature {

    val geometryFactory = GeometryFactory()
    val feature1Coordinates = (polygon1.geometry as? Polygon)?.coordinates?.firstOrNull()
        ?.map {
                position -> Coordinate(position.longitude, position.latitude)
        }?.toTypedArray()
    val feature2Coordinates = (polygon2.geometry as? Polygon)?.coordinates?.firstOrNull()
        ?.map {
                position -> Coordinate(position.longitude, position.latitude)
        }?.toTypedArray()

    val polygon1GeometryJTS = feature1Coordinates?.let { geometryFactory.createPolygon(it)}
    val polygon2GeometryJTS = feature2Coordinates?.let { geometryFactory.createPolygon(it)}
    // merge/union the polygons
    val mergedGeometryJTS = polygon1GeometryJTS?.union(polygon2GeometryJTS)
    // create a new Polygon with a single outer ring using the coordinates from the JTS merged geometry
    val mergedPolygon = Feature().also { feature ->
        feature.properties = polygon1.properties
        feature.foreign = polygon1.foreign
        feature.type = "Feature"
        feature.geometry = Polygon().also { polygon ->
            //Convert JTS to GeoJSON coordinates
            val geoJsonCoordinates = mergedGeometryJTS?.coordinates?.map { coordinate ->
                LngLatAlt(coordinate.x, coordinate.y )
            }?.let {
                arrayListOf(arrayListOf(*it.toTypedArray()))
            }
            polygon.coordinates = geoJsonCoordinates ?: arrayListOf()
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
    location: LngLatAlt,
    heading: Double,
    distance: Double,
    featureCollection: FeatureCollection
) {
    val points = getFovTrianglePoints(location, heading, distance)

    // Take care not to add the triangle to the passed in collection
    val mapCollection = FeatureCollection()
    mapCollection.plusAssign(featureCollection)

    val triangle = Feature()
    triangle.geometry = Polygon(
        arrayListOf(
            location,
            points.left,
            points.right,
            location
        )
    )
    mapCollection.addFeature(triangle)

    val outputFile = FileOutputStream("markers.geojson")
    val adapter = GeoJsonObjectMoshiAdapter()
    outputFile.write(adapter.toJson(mapCollection).toByteArray())
    outputFile.close()
}