package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.dto.Circle
import org.scottishtecharmy.soundscape.dto.Tile
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import java.lang.Math.toDegrees
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Gets map coordinates from X and Y GPS coordinates. This is the same calculation as above
 * but returns normalised x and y values scaled between 0 and 1.0. These are what are required
 * by the map-compose library to set markers/positions.
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
            val surroundingTile = Tile(x, y, zoom)
            tiles.add(surroundingTile)
        }
    }
    return tiles
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

fun searchFeaturesByName(featureCollection: FeatureCollection, query: String): FeatureCollection {
    val results = FeatureCollection()
    for (feature in featureCollection) {
        val mvtFeature = feature as MvtFeature
        val name = mvtFeature.name
        if (name != null && name.contains(query, ignoreCase = true)) {
            results.addFeature(feature)
        }
    }
    return results
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

/**
 * Calculate the approximate center coordinates of a circle based on a segment.
 * @param segment
 * segment of circle as LineString
 * @return The coordinates of the center of the circle as LngLatAlt.
 */
fun calculateCenterOfCircle(
    segment: LineString
): Circle {
    val a = segment.coordinates.first()
    val b = segment.coordinates.last()
    val arcMidPoint: LngLatAlt

    if (segment.coordinates.size % 2 == 0) {
        // synthesize the arcPoint
        val firstCoordinate = segment.coordinates[segment.coordinates.size / 2 - 1]
        val secondCoordinate = segment.coordinates[segment.coordinates.size / 2]
        val distanceBetweenCoordinates =
            distance(
                firstCoordinate.latitude,
                firstCoordinate.longitude,
                secondCoordinate.latitude,
                secondCoordinate.longitude
            )
        val bearing =
            bearingFromTwoPoints(firstCoordinate, secondCoordinate)
        arcMidPoint =
            getDestinationCoordinate(firstCoordinate, bearing, distanceBetweenCoordinates / 2)

    } else {
        arcMidPoint = segment.coordinates[segment.coordinates.size / 2]
    }

    val center = calculateCenter(a, b, arcMidPoint)

    return center
}

/**
 * Calculate the approximate center coordinates of a circle based on the start and end coordinates
 * of a segment and the arc midpoint.
 * @param start
 * is start coordinates of segment
 * @param end
 * is end coordinates of segment
 * @param arcMidPoint
 * The coordinates of the arc midpoint as LngLatAlt.
 * @return The coordinates of the center of the circle as LngLatAlt.
 */
fun calculateCenter(
    start: LngLatAlt,
    end: LngLatAlt,
    arcMidPoint: LngLatAlt
): Circle {
    val chordMidpoint =
        LngLatAlt((start.longitude + end.longitude) / 2, (start.latitude + end.latitude) / 2)
    val chordLength = start.createCheapRuler().distance(start, end)
    // calculate radius
    val radius = calculateRadius(chordLength, arcMidPoint, chordMidpoint)
    // is the chord midpoint to the right or left of the segment?
    val chordBearing = if(pointOnRightSide(start, arcMidPoint, end)){
        bearingFromTwoPoints(end, start)
    } else {
        bearingFromTwoPoints(start, end)
    }

    // Calculate chord bearing
    //val chordBearing = bearingFromTwoPoints(end.latitude, end.longitude, start.latitude, start.longitude)
    val circleCenter = findCircleCenter(arcMidPoint, chordBearing, radius)
    val circle = Circle()
    circle.center = circleCenter
    circle.radius = radius

    return circle
}
