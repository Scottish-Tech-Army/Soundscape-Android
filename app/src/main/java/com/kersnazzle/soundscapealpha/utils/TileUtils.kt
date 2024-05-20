package com.kersnazzle.soundscapealpha.utils

import com.kersnazzle.soundscapealpha.dto.VectorTile
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Feature
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.FeatureCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeoMoshi
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiLineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiPoint
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiPolygon
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Point
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Polygon
import com.squareup.moshi.Moshi
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

//TODO getFovIntersectionFeatureCollection, getFovRoadsFeatureCollection and getFovPoiFeatureCollection can be rolled into one as just repeating the same thing

/**
 * Gets Slippy Map Tile Name from X and Y GPS coordinates and Zoom (fixed at 16 for Soundscape)
 * @param lat
 * latitude in decimal degrees
 * @param lon
 * longitude in decimal degrees
 * @param zoom
 * the zoom level.
 * @return a Pair(xtile, ytile)
 */
fun getXYTile(lat: Double, lon: Double, zoom: Int = 16): Pair<Int, Int> {
    val latRad = toRadians(lat)
    var xtile = floor((lon + 180) / 360 * (1 shl zoom)).toInt()
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

/** Given a radius and location it calculates the set of tiles (VectorTiles) that cover a
 * circular region around the specified location.
 * @param currentLatitude
 * The center of the region to search
 * @param currentLongitude
 * The center of the region to search
 * @param radius
 * The radius of the region to get adjoining tiles in meters
 * @return  A MutableList of VectorTiles covering the searched region
 */
fun getTilesForRegion(
    currentLatitude: Double,
    currentLongitude: Double,
    radius: Double,
    zoom: Int
): MutableList<VectorTile> {

    val (pixelX, pixelY) = getPixelXY(currentLatitude, currentLongitude, zoom)
    val radiusPixels = radius / groundResolution(currentLatitude, zoom).toInt()

    val startX = pixelX - radiusPixels
    val startY = pixelY - radiusPixels
    val endX = pixelX + radiusPixels
    val endY = pixelY + radiusPixels

    val (startTileX, startTileY) = getTileXY(startX.toInt(), startY.toInt())
    val (endTileX, endTileY) = getTileXY(endX.toInt(), endY.toInt())

    val tiles: MutableList<VectorTile> = mutableListOf()

    for (y in startTileY..endTileY) {
        for (x in startTileX..endTileX) {
            val surroundingTile = VectorTile("", x, y, zoom)
            surroundingTile.quadkey = getQuadKey(x, y, zoom)
            tiles.add(surroundingTile)
        }
    }
    return tiles
}

/**
 * Given a valid Tile features collection this will parse the collection and return a roads
 * feature collection. Uses the "highway" feature_type to extract roads from GeoJSON.
 * @param tileFeatureCollection
 * valid Tile Feature Collection
 * @return a FeatureCollection that contains only roads.
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
        if (feature.foreign!!["feature_type"] == "highway"
            && feature.foreign!!["feature_value"] != "gd_intersection"
            && feature.foreign!!["feature_value"] != "footway"
            && feature.foreign!!["feature_value"] != "path"
            && feature.foreign!!["feature_value"] != "cycleway"
            && feature.foreign!!["feature_value"] != "bridleway") {
            roadsFeatureCollection.addFeature(feature)
        }
    }
    return roadsFeatureCollection
}

/**
 * Given a valid Tile feature collection this will parse the collection and return a paths
 * feature collection. Uses the "footway", "path", "cycleway", "bridleway" feature_value to extract
 * paths from Feature Collection
 * @param tileFeatureCollection
 * valid Tile Feature Collection
 * @return a FeatureCollection that contains only paths.
 */
fun getPathsFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection{
    val pathsFeatureCollection = FeatureCollection()

    for(feature in tileFeatureCollection) {
        if (feature.foreign!!["feature_type"] == "highway")
            when(feature.foreign!!["feature_value"]){
                "footway" -> pathsFeatureCollection.addFeature(feature)
                "path" -> pathsFeatureCollection.addFeature(feature)
                "cycleway" -> pathsFeatureCollection.addFeature(feature)
                "bridleway" -> pathsFeatureCollection.addFeature(feature)
            }
    }
    return pathsFeatureCollection
}

/**
 * Parses out all the Intersections in a tile FeatureCollection using the "gd_intersection" feature_value
 * @param tileFeatureCollection
 * takes a FeatureCollection object
 * @return a Feature collection object that only contains intersections
 */
fun getIntersectionsFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection {
    val intersectionsFeatureCollection = FeatureCollection()
    // split out the intersections into their own intersections FeatureCollection
    for (feature in tileFeatureCollection) {
        if (feature.foreign!!["feature_type"] == "highway" && feature.foreign!!["feature_value"] == "gd_intersection") {
            intersectionsFeatureCollection.addFeature(feature)
        }
    }
    return intersectionsFeatureCollection
}

/**
 * Parses out all the Entrances in a tile FeatureCollection using the "gd_entrance_list" feature_type
 * @param tileFeatureCollection
 * takes a FeatureCollection object
 * @return a feature collection object that only contains entrances
 */
fun getEntrancesFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection {
    val entrancesFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        if (feature.foreign!!["feature_type"] == "gd_entrance_list") {
            entrancesFeatureCollection.addFeature(feature)
        }
    }
    return entrancesFeatureCollection
}

/**
 * Parses out all the Points of Interest (POI) in a tile FeatureCollection
 * @param tileFeatureCollection
 * takes a FeatureCollection object
 * @return a Feature collection object that only contains POI
 */
fun getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection {
    val poiFeaturesCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        if (feature.foreign!!["feature_type"] != "highway" && feature.foreign!!["feature_type"] != "gd_entrance_list") {
            poiFeaturesCollection.addFeature(feature)
        }
    }
    return poiFeaturesCollection
}

/**
 * Parses out the super category Features contained in the Points of Interest (POI) Feature Collection.
 * @param superCategory
 * String for super category. Options are "information", "object", "place", "landmark", "mobility", "safety"
 * @param poiFeatureCollection
 * POI Feature Collection for a tile
 * @return a Feature Collection containing only the Features from the super category
 */
fun getPoiFeatureCollectionBySuperCategory(
    superCategory: String,
    poiFeatureCollection: FeatureCollection
): FeatureCollection {

    val superCategoryPoiFeatureCollection = FeatureCollection()
    val superCategoryList = getSuperCategoryElements(superCategory)

    for (feature in poiFeatureCollection) {
        for (featureType in superCategoryList) {
            if (feature.foreign!!["feature_type"] == featureType || feature.foreign!!["feature_value"] == featureType) {
                superCategoryPoiFeatureCollection.addFeature(feature)
            }
        }
    }
    return superCategoryPoiFeatureCollection
}

/**
 * There's a bug in the backend API where the GeoJSON can contain data that isn't part of the tile
 * for example the 16/32277/21812.json contains MultiPolygons for the University of Law,
 * University of Bristol, and Monarchs Way walking route. This strips out Features that someone has
 * wrapped giant polygons around in the original OSM data. Need to see if there is a fix for this on the backend...
 * @param tileX
 * Slippy tile X
 * @param tileY
 * Slippy tile Y
 * @param zoom
 * Zoom level should be 16.0
 * @param geoJSONTile
 * String that represents the tile
 * @return String representing the cleaned tile
 */
fun cleanTileGeoJSON(tileX: Int, tileY: Int, zoom: Double, geoJSONTile: String): String {
    // create a feature collection from the string
    val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
    val tileFeatureCollection: FeatureCollection? =
        moshi.adapter(FeatureCollection::class.java).fromJson(geoJSONTile)

    val tileBoundingBox = tileToBoundingBox(tileX, tileY, zoom)
    val tilePolygon = getPolygonOfBoundingBox(tileBoundingBox)
    // loop through the tile feature collection and add anything that is contained in the tile polygon
    // to a new feature collection and dump the weirdness
    val cleanTileFeatureCollection = FeatureCollection()

    if (tileFeatureCollection != null) {
        for (feature in tileFeatureCollection){
            when (feature.geometry.type) {
                "Point" -> {
                    val lngLatAlt = (feature.geometry as Point).coordinates
                    val testPoint = polygonContainsCoordinates(lngLatAlt, tilePolygon)
                    if (testPoint){
                        cleanTileFeatureCollection.addFeature(feature)
                    }
                }

                "MultiPoint" -> {
                    for (point in (feature.geometry as MultiPoint).coordinates) {
                        val testPoint = polygonContainsCoordinates(point, tilePolygon)
                        if (testPoint){
                            cleanTileFeatureCollection.addFeature(feature)
                            // at least one of the points is in the tile so add the entire
                            // MultiPoint Feature and break
                            break
                        }
                    }
                }

                "LineString" -> {
                    for (point in (feature.geometry as LineString).coordinates){
                        val testPoint = polygonContainsCoordinates(point, tilePolygon)
                        if (testPoint) {
                            cleanTileFeatureCollection.addFeature(feature)
                            // at least one of the points is in the tile so add the entire
                            // LineString Feature and break
                            break
                        }
                    }
                }

                "MultiLineString" -> {
                    for (lineString in (feature.geometry as MultiLineString).coordinates) {
                        for (point in lineString) {
                            val testPoint = polygonContainsCoordinates(point, tilePolygon)
                            if (testPoint) {
                                cleanTileFeatureCollection.addFeature(feature)
                                // at least one of the points is in the tile so add the entire
                                // MultiLineString Feature and break
                                break
                            }
                        }
                    }
                }

                "Polygon" -> {
                    for (geometry in (feature.geometry as Polygon).coordinates) {
                        for (point in geometry) {
                            val testPoint = polygonContainsCoordinates(point, tilePolygon)
                            if (testPoint) {
                                cleanTileFeatureCollection.addFeature(feature)
                                // at least one of the points is in the tile so add the entire
                                // Polygon Feature and break
                                break
                            }
                        }
                    }
                }

                "MultiPolygon" -> {
                    for (polygon in (feature.geometry as MultiPolygon).coordinates) {
                        for (linearRing in polygon) {
                            for (point in linearRing) {
                                val testPoint = polygonContainsCoordinates(point, tilePolygon)
                                if (testPoint) {
                                    cleanTileFeatureCollection.addFeature(feature)
                                    // at least one of the points is in the tile so add the entire
                                    // MultiPolygon Feature and break
                                    break
                                }
                            }
                        }
                    }
                }

                else -> println("Unknown type ${feature.geometry.type}")
            }
        }
    }
    return moshi.adapter(FeatureCollection::class.java).toJson(cleanTileFeatureCollection)
}

/**
 * Return a Feature Collection that contains the Intersections in the "field of view" triangle.
 * @param location
 * location of the device
 * @param heading
 * direction the device is pointing
 * @param distance
 * distance to extend the "field of view"
 * @param intersectionsFeatureCollection
 * The intersections feature collection that we want to filter
 * @return A Feature Collection that contains the Intersections in the FOV triangle which is 90
 * degrees from the heading of the device
 */
fun getFovIntersectionFeatureCollection(
    location: LngLatAlt,
    heading: Double,
    distance: Double,
    intersectionsFeatureCollection: FeatureCollection
): FeatureCollection {
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
    val destinationCoordinateLeft = getDestinationCoordinate(
        LngLatAlt(location.longitude, location.latitude),
        quadrants[quadrantIndex].left,
        distance
    )

    //Get the coordinate for the "Right" of the FOV
    val destinationCoordinateRight = getDestinationCoordinate(
        LngLatAlt(location.longitude, location.latitude),
        quadrants[quadrantIndex].right,
        distance
    )

    // We can now construct our FOV polygon (triangle)
    val polygonTriangleFOV = createTriangleFOV(
        destinationCoordinateLeft,
        location,
        destinationCoordinateRight
    )

    // only the intersections Features that are in the FOV triangle are returned
    return getIntersectionsFOVFeatureCollection(intersectionsFeatureCollection, polygonTriangleFOV)
}

/**
 * Return a Feature Collection that contains the intersections in the "field of view" triangle.
 * @param intersectionsFeatureCollection
 * The intersections feature collection for a tile
 * @param polygonTriangleFOV
 * The triangle or any other shape you feel like that is being tested to see what intersections it contains
 * @return A Feature Collection that contains the intersections in the FOV triangle
 */
fun getIntersectionsFOVFeatureCollection(
    intersectionsFeatureCollection: FeatureCollection,
    polygonTriangleFOV: Polygon): FeatureCollection {
    // Are any of the points from the intersectionsFeatureCollection contained in the polygonTriangleFOV
    val intersectionsFOVFeatureCollection = FeatureCollection()

    for (feature in intersectionsFeatureCollection) {
        when(feature.geometry.type) {
            "Point" -> {
                val testPoint = LngLatAlt(
                    (feature.geometry as Point).coordinates.longitude,
                    (feature.geometry as Point).coordinates.latitude,

                    )
                val containsCoordinate =
                    polygonContainsCoordinates(testPoint, polygonTriangleFOV)
                if (containsCoordinate) {
                    intersectionsFOVFeatureCollection.addFeature(feature)
                }
            }
        }
    }
    // only the intersections Features that are in the FOV triangle are returned
    return intersectionsFOVFeatureCollection
}

/**
 * Return a roads feature collection that is contained in the "field of view".
 * @param location
 * location where the device is
 * @param heading
 * direction the device is pointing
 * @param distance
 * Distance to the destination points ("left" point and "right" point) in meters
 * @param roadsFeatureCollection
 * Feature Collection that contains the roads to check
 * @return The road features that are contained in the FOV triangle
 */
fun getFovRoadsFeatureCollection(
    location: LngLatAlt,
    heading: Double,
    distance: Double,
    roadsFeatureCollection: FeatureCollection
): FeatureCollection {
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
    val destinationCoordinateLeft = getDestinationCoordinate(
        LngLatAlt(location.longitude, location.latitude),
        quadrants[quadrantIndex].left,
        distance
    )

    //Get the coordinate for the "Right" of the FOV
    val destinationCoordinateRight = getDestinationCoordinate(
        LngLatAlt(location.longitude, location.latitude),
        quadrants[quadrantIndex].right,
        distance
    )

    // We can now construct our FOV polygon (triangle)
    val polygonTriangleFOV = createTriangleFOV(
        destinationCoordinateLeft,
        location,
        destinationCoordinateRight
    )

    // only the road Features that are in the FOV triangle are returned
    return getRoadsFovFeatureCollection(roadsFeatureCollection, polygonTriangleFOV)
}

/**
 * Return a Feature Collection that contains the roads in the "field of view" triangle.
 * @param roadsFeatureCollection
 * The roads feature collection for a tile
 * @param polygonTriangleFOV
 * The triangle that is being tested to see what roads it contains
 * @return A Feature Collection that contains the roads in the FOV triangle
 */
fun getRoadsFovFeatureCollection(
    roadsFeatureCollection: FeatureCollection,
    polygonTriangleFOV: Polygon ): FeatureCollection {

    // Are any of the points from the roadsFeatureCollection contained in the polygonTriangleFOV
    val roadsFOVFeatureCollection = FeatureCollection()
    for (feature in roadsFeatureCollection) {
        when(feature.geometry.type) {
            "LineString" -> {
                for (coordinate in (feature.geometry as LineString).coordinates) {
                    val containsCoordinate =
                        polygonContainsCoordinates(coordinate, polygonTriangleFOV)
                    if (containsCoordinate) {
                        roadsFOVFeatureCollection.addFeature(feature)
                        break
                    }
                }
            }
            "Point" -> {
                val testPoint = LngLatAlt(
                    (feature.geometry as Point).coordinates.longitude,
                    (feature.geometry as Point).coordinates.latitude,

                    )
                val containsCoordinate =
                    polygonContainsCoordinates(testPoint, polygonTriangleFOV)
                if (containsCoordinate) {
                    roadsFOVFeatureCollection.addFeature(feature)
                }
            }
        }
    }
    // only the road Features that are in the FOV triangle are returned
    return roadsFOVFeatureCollection
}

/**
 * Return a poi feature collection that is contained in the "field of view".
 * @param location
 * location where the device is
 * @param heading
 * direction the device is pointing
 * @param distance
 * Distance to the destination points ("left" point and "right" point) in meters
 * @param poiFeatureCollection
 * Points Of Interest Feature Collection to check
 * @return The poi features that are contained in the FOV triangle
 */
fun getFovPoiFeatureCollection(
    location: LngLatAlt,
    heading: Double,
    distance: Double,
    poiFeatureCollection: FeatureCollection): FeatureCollection{
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
    val destinationCoordinateLeft = getDestinationCoordinate(
        LngLatAlt(location.longitude, location.latitude),
        quadrants[quadrantIndex].left,
        distance
    )

    //Get the coordinate for the "Right" of the FOV
    val destinationCoordinateRight = getDestinationCoordinate(
        LngLatAlt(location.longitude, location.latitude),
        quadrants[quadrantIndex].right,
        distance
    )

    // We can now construct our FOV polygon (triangle)
    val polygonTriangleFOV = createTriangleFOV(
        destinationCoordinateLeft,
        location,
        destinationCoordinateRight
    )

    // only the road Features that are in the FOV triangle are returned
    return getPoiFovFeatureCollection(poiFeatureCollection, polygonTriangleFOV)

}

/**
 * Return a Feature Collection that contains the Points Of Interest in the "field of view" triangle.
 * @param poiFeatureCollection
 * The poi feature collection for a tile
 * @param polygonTriangleFOV
 * The triangle that is being tested to see what poi it contains
 * @return A Feature Collection that contains the Points of Interest in the FOV triangle
 */
fun getPoiFovFeatureCollection(
    poiFeatureCollection: FeatureCollection,
    polygonTriangleFOV: Polygon): FeatureCollection {

    // Are any of the points from the poiFeatureCollection contained in the polygonTriangleFOV
    val poiFOVFeatureCollection = FeatureCollection()
    for (feature in poiFeatureCollection) {
        when(feature.geometry.type) {
            "LineString" -> {
                for (coordinate in (feature.geometry as LineString).coordinates) {
                    val containsCoordinate =
                        polygonContainsCoordinates(coordinate, polygonTriangleFOV)
                    if (containsCoordinate) {
                        poiFOVFeatureCollection.addFeature(feature)
                        break
                    }
                }
            }
            "MultiLineString" -> {
                for (lineString in (feature.geometry as MultiLineString).coordinates) {
                    for (coordinate in lineString) {
                        val testPoint = polygonContainsCoordinates(coordinate, polygonTriangleFOV)
                        if (testPoint) {
                            poiFOVFeatureCollection.addFeature(feature)
                            break
                        }
                    }
                }
            }
            "Polygon" -> {
                for (geometry in (feature.geometry as Polygon).coordinates) {
                    for (point in geometry) {
                        val containsCoordinate =
                            polygonContainsCoordinates(point, polygonTriangleFOV)
                        if (containsCoordinate) {
                            poiFOVFeatureCollection.addFeature(feature)
                            break
                        }
                    }
                }
            }
            "MultiPolygon" -> {
                for (polygon in (feature.geometry as MultiPolygon).coordinates) {
                    for (linearRing in polygon) {
                        for (coordinate in linearRing) {
                            val containsCoordinate =
                                polygonContainsCoordinates(coordinate, polygonTriangleFOV)
                            if (containsCoordinate) {
                                poiFOVFeatureCollection.addFeature(feature)
                                break
                            }
                        }
                    }
                }
            }
            "Point" -> {
                val testPoint = LngLatAlt(
                    (feature.geometry as Point).coordinates.longitude,
                    (feature.geometry as Point).coordinates.latitude
                    )
                val containsCoordinate =
                    polygonContainsCoordinates(testPoint, polygonTriangleFOV)
                if (containsCoordinate) {
                    poiFOVFeatureCollection.addFeature(feature)
                }
            }
            "MultiPoint" -> {
                for (point in (feature.geometry as MultiPoint).coordinates) {
                    val containsCoordinate =
                        polygonContainsCoordinates(point, polygonTriangleFOV)
                    if (containsCoordinate){
                        poiFOVFeatureCollection.addFeature(feature)
                        break
                    }
                }
            }
        }
    }
    // only the poi Features that are in the FOV triangle are returned
    return poiFOVFeatureCollection
}

/**
 * Get nearest intersection from intersections Feature Collection.
 * WARNING: This is just a "straight line" haversine distance to an intersection it doesn't
 * care which direction the intersection is.
 * @param currentLocation
 * Location of device
 * @param intersectionFeatureCollection
 * The intersection feature collection that contains the intersections we want to test
 * @return A Feature Collection that contains the nearest intersection
 */
fun getNearestIntersection(
    currentLocation: LngLatAlt,
    intersectionFeatureCollection: FeatureCollection
): FeatureCollection{

    var maxDistanceToIntersection = Int.MAX_VALUE.toDouble()
    var nearestIntersection = Feature()

    for (feature in intersectionFeatureCollection) {
        val distanceToIntersection = distance(
            currentLocation.latitude,
            currentLocation.longitude,
            (feature.geometry as Point).coordinates.latitude,
            (feature.geometry as Point).coordinates.longitude
        )
        if (distanceToIntersection < maxDistanceToIntersection){
            nearestIntersection = feature
            maxDistanceToIntersection = distanceToIntersection
        }
    }
    val nearestIntersectionFeatureCollection = FeatureCollection()
    // TODO As the distance to the intersection has already been calculated
    //  perhaps we could insert the distance to the intersection as a property/foreign member of the Feature?
    return nearestIntersectionFeatureCollection.addFeature(nearestIntersection)
}


/**
 * Get nearest road from roads Feature Collection.
 * WARNING: It doesn't care which direction the road is.
 * Roads can contain crossings which are Points not LineStrings
 * @param currentLocation
 * Location of device
 * @param roadFeatureCollection
 * The intersection feature collection that contains the intersections we want to test
 * @return A Feature Collection that contains the nearest road
 */
fun getNearestRoad(
    currentLocation: LngLatAlt,
    roadFeatureCollection: FeatureCollection
): FeatureCollection {

    //TODO I have no idea if roads can also be represented with MultiLineStrings.
    // In which case this will fail. Need to have a look at some tiles with motorways/dual carriageways

    var maxDistanceToRoad = Int.MAX_VALUE.toDouble()
    var nearestRoad = Feature()

    for (feature in roadFeatureCollection) {
        if (feature.geometry.type == "LineString") {
            val distanceToRoad = distanceToLineString(
                LngLatAlt(currentLocation.longitude, currentLocation.latitude),
                (feature.geometry as LineString)
            )
            if (distanceToRoad < maxDistanceToRoad) {
                nearestRoad = feature
                maxDistanceToRoad = distanceToRoad
            }
        } else {
            val distanceToRoad = distance(
                currentLocation.latitude,
                currentLocation.longitude,
                (feature.geometry as Point).coordinates.latitude,
                (feature.geometry as Point).coordinates.longitude
            )
            if (distanceToRoad < maxDistanceToRoad) {
                nearestRoad = feature
                maxDistanceToRoad = distanceToRoad
            }
        }
    }
    val nearestRoadFeatureCollection = FeatureCollection()
    // TODO As the distance to the road has already been calculated
    //  perhaps we could insert the distance to the road as a property/foreign member of the Feature?
    return nearestRoadFeatureCollection.addFeature(nearestRoad)
}

/**
 * Distance to a LineString from current location.
 * @param pointCoordinates
 * LngLatAlt of current location
 * @param lineStringCoordinates
 * LineString that we are working out the distance from
 * @return The distance of the point to the LineString
 */
fun distanceToLineString(
    pointCoordinates: LngLatAlt,
    lineStringCoordinates: LineString
): Double {

    var minDistance = Double.MAX_VALUE
    var last = lineStringCoordinates.coordinates[0]
    for (i in 1 until lineStringCoordinates.coordinates.size) {
        val current = lineStringCoordinates.coordinates[i]
        val distance = distance(last, current, pointCoordinates)
        minDistance = min(minDistance, distance)
        last = current
    }
    return minDistance
}

/**
 * Calculate distance of a point p to a line defined by two other points l1 and l2.
 * @param l1
 * point 1 on the line
 * @param l2
 * point 2 on the line
 * @param p
 * current location point
 * @return the distance of the point to the line
 */
fun distance(l1: LngLatAlt, l2: LngLatAlt, p: LngLatAlt): Double {
    return distance(l1.latitude, l1.longitude, l2.latitude, l2.longitude, p.latitude, p.longitude)
}

/**
 * Calculate distance of a point (pLat,pLon) to a line defined by two other points (lat1,lon1) and (lat2,lon2)
 * @param x1 double
 * @param y1 double
 * @param x2 double
 * @param y2 double
 * @param x double
 * @param y double
 * @return the distance of the point to the line
 */
fun distance(x1: Double, y1: Double, x2: Double, y2: Double, x: Double, y: Double): Double {
    val xx: Double
    val yy: Double
    when {
        y1 == y2 -> {
            // horizontal line
            xx = x
            yy = y1
        }
        x1 == x2 -> {
            // vertical line
            xx = x1
            yy = y
        }
        else -> {
            // y=s*x  +c
            val s = (y2 - y1) / (x2 - x1)
            val c = y1 - s * x1

            // y=ps*x + pc
            val ps = -1 / s
            val pc = y - ps * x

            // solve    ps*x +pc = s*x + c
            //          (ps-s) *x = c -pc
            //          x= (c-pc)/(ps-s)
            xx = (c - pc) / (ps - s)
            yy = s * xx + c
        }
    }
    return if (onSegment(xx, yy, x1, y1, x2, y2)) {
        distance(x, y, xx, yy)
    } else {
        min(distance(x, y, x1, y1), distance(x, y, x2, y2))
    }
}

fun onSegment(x: Double, y: Double, x1: Double, y1: Double, x2: Double, y2: Double): Boolean {
    val minx = min(x1, x2)
    val maxx = max(x1, x2)

    val miny = min(y1, y2)
    val maxy = max(y1, y2)

    return x in minx..maxx && y >= miny && y <= maxy
}



/**
 * Given a super category string returns a mutable list of things in the super category.
 * Categories taken from original Soundscape
 * @param category
 * String for super category. Options are "information", "object", "place", "landmark", "mobility", "safety"
 * @return a mutable list of things in the super category
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