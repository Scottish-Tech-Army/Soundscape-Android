package com.kersnazzle.soundscapealpha.utils

import com.kersnazzle.soundscapealpha.dto.BoundingBox
import com.kersnazzle.soundscapealpha.dto.BoundingBoxCorners
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiLineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiPoint
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiPolygon
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Point
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Polygon
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

const val DEGREES_TO_RADIANS = 2.0 * PI / 360.0
const val RADIANS_TO_DEGREES = 1.0 / DEGREES_TO_RADIANS
const val EARTH_RADIUS_METERS =
    6378137.0 //  Original Soundscape uses 6378137.0 not 6371000.0
const val MIN_LATITUDE = -85.05112878
const val MAX_LATITUDE = 85.05112878
const val MIN_LONGITUDE: Double = -180.0002
const val MAX_LONGITUDE: Double = 180.0002


/**
 * Compute the Haversine distance between the two coordinates.
 * @param lat1
 * The start latitude in decimal degrees.
 * @param long1
 * The start longitude in decimal degrees.
 * @param lat2
 * The finish latitude in decimal degrees.
 * @param long2
 * the finish longitude in decimal degrees.
 * @return The distance in meters.
 */
fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {

    val deltaLat = toRadians(lat2 - lat1)
    val deltaLon = toRadians(long2 - long1)

    val a =
        sin(deltaLat / 2) * sin(deltaLat / 2) + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(
            deltaLon / 2
        ) * sin(
            deltaLon / 2
        )

    val c = 2 * asin(sqrt(a))

    return (EARTH_RADIUS_METERS * c).round(2)
}

/**
 * The size of the map in pixels for the given zoom level assuming the base is 256 pixels.
 * @param zoom
 * A zoom level (between 0 and 23).
 * @return Side length of the map in pixels.
 */
fun mapSize(zoom: Int): Int {
    val base = 256
    return base shl (zoom)
}

/**
 * Clips a value to the range [max, min].
 * @param value
 * Value to clip.
 * @param minimum
 * Minimum value of range.
 * @param maximum
 * Maximum value of range.
 * @return The clipped value.
 */
fun clip(value: Double, minimum: Double, maximum: Double): Double {
    return min(max(value, minimum), maximum)
}

/**
 * Determines the ground resolution (in meters per pixel) at a specified
 * latitude and level of detail.
 * @param latitude
 * Latitude (in degrees) at which to measure the ground resolution.
 * @param zoom
 * Level of detail, from 1 (lowest detail) to 23 (highest detail).
 * @return The ground resolution, in meters per pixel.
 */
fun groundResolution(latitude: Double, zoom: Int): Double {
    val clippedLat = clip(latitude, MIN_LATITUDE, MAX_LATITUDE)

    return cos(clippedLat * PI / 180.0) * 2 * PI * EARTH_RADIUS_METERS / mapSize(zoom)
}

/**
 * Calculates the coordinates of the Tile containing the provided pixel coordinates.
 * @param pixelX
 * X coordinate of the pixel.
 * @param pixelY
 * Y coordinate of the pixel.
 * @return Tile coordinate as a Pair(x, y).
 */
fun getTileXY(pixelX: Int, pixelY: Int) = Pair(pixelX / 256, pixelY / 256)

/**
 * Generates a quad key string from the tile X, Y coordinates and zoom level provided.
 * Here's the Microsoft info: https://learn.microsoft.com/en-us/bingmaps/articles/bing-maps-tile-system
 * @param tileX
 * X coordinate of a tile.
 * @param tileY
 * Y coordinate of the tile.
 * @param zoomLevel
 * Zoom level of the tile.
 * @return A quad key String.
 */
fun getQuadKey(tileX: Int, tileY: Int, zoomLevel: Int): String {

    var quadKey = ""

    for (level in zoomLevel downTo 1) {
        var digit = 0
        val mask = 1 shl (level - 1)
        if (tileX and mask != 0) {
            digit += 1
        }
        if (tileY and mask != 0) {
            digit += 2
        }
        quadKey += digit.toString()
    }
    return quadKey
}

/**
 * Calculates the pixel coordinate of the provided latitude and longitude location at the given zoom level.
 * @param latitude
 * Current Latitude.
 * @param longitude
 * Current Longitude.
 * @param zoom
 * Zoom level.
 * @return Pixel coordinate as a Pair (x, y).
 */
fun getPixelXY(latitude: Double, longitude: Double, zoom: Int): Pair<Double, Double> {

    val lat = clip(latitude, MIN_LATITUDE, MAX_LATITUDE)
    val lon = clip(longitude, MIN_LONGITUDE, MAX_LONGITUDE)

    val sinLat = sin(lat * PI / 180)
    val x = (lon + 180) / 360
    val y = 0.5 - ln((1 + sinLat) / (1 - sinLat)) / (PI * 4)

    val size = mapSize(zoom).toDouble()

    val pixelX = clip(x * size + 0.5, 0.0, size - 1)
    val pixelY = clip(y * size + 0.5, 0.0, size - 1)

    return Pair(pixelX, pixelY)
}

/**
 * Calculates the lat and lon coordinates given a pixelX, pixelY and zoom.
 * @param pixelX
 * pixelX of the tile.
 * @param pixelY
 * pixelY of the tile.
 * @param zoom
 * Zoom level of the tile.
 * @return Lat lon coordinates as a Pair(x, y).
 */
fun pixelXYToLatLon(pixelX: Double, pixelY: Double, zoom: Int): Pair<Double, Double> {

    val mapSize = mapSize(zoom)
    val x = (clip(pixelX, 0.0, mapSize.toDouble() - 1) / mapSize) - 0.5
    val y = 0.5 - (clip(pixelY, 0.0, mapSize.toDouble() - 1) / mapSize)
    val latitude = 90 - 360 * atan(exp(-y * 2 * Math.PI)) / Math.PI
    val longitude = 360 * x

    return Pair(latitude, longitude)

}

/**
 * Given a Point object returns the bounding box for it.
 * @param point
 * Point object.
 * @return A Bounding Box for the Point.
 */
fun getBoundingBoxOfPoint(point: Point): BoundingBox {
    val bbOfPoint = BoundingBox()

    bbOfPoint.westLongitude = point.coordinates.longitude
    bbOfPoint.southLatitude = point.coordinates.latitude
    bbOfPoint.eastLongitude = point.coordinates.longitude
    bbOfPoint.northLatitude = point.coordinates.latitude

    return bbOfPoint
}

/**
 * Given a LineString object returns the bounding box for it.
 * @param lineString
 * LineString object with multiple points.
 * @return A Bounding Box for the LineString.
 */
fun getBoundingBoxOfLineString(lineString: LineString): BoundingBox {

    var westLon = Int.MAX_VALUE.toDouble()
    var southLat = Int.MAX_VALUE.toDouble()
    var eastLon = Int.MIN_VALUE.toDouble()
    var northLat = Int.MIN_VALUE.toDouble()

    for (point in lineString.coordinates) {
        westLon = min(westLon, point.longitude)
        southLat = min(southLat, point.latitude)
        eastLon = max(eastLon, point.longitude)
        northLat = max(northLat, point.latitude)
    }

    return BoundingBox(westLon, southLat, eastLon, northLat)
}

/**
 * Given a MultiPoint object return the bounding box for it.
 * @param multiPoint
 * MultiLineString object.
 * @return A Bounding Box for the MultiPoint object.
 */
fun getBoundingBoxOfMultiPoint(multiPoint: MultiPoint): BoundingBox {
    var westLon = Int.MAX_VALUE.toDouble()
    var southLat = Int.MAX_VALUE.toDouble()
    var eastLon = Int.MIN_VALUE.toDouble()
    var northLat = Int.MIN_VALUE.toDouble()

    for (point in multiPoint.coordinates) {
        westLon = min(westLon, point.longitude)
        southLat = min(southLat, point.latitude)
        eastLon = max(eastLon, point.longitude)
        northLat = max(northLat, point.latitude)

    }
    return BoundingBox(westLon, southLat, eastLon, northLat)
}

/**
 * Given a MultiLineString object returns the bounding box for it.
 * @param multiLineString
 * MultiLineString object.
 * @return A Bounding Box for the MultiLineString.
 */
fun getBoundingBoxOfMultiLineString(multiLineString: MultiLineString): BoundingBox {
    var westLon = Int.MAX_VALUE.toDouble()
    var southLat = Int.MAX_VALUE.toDouble()
    var eastLon = Int.MIN_VALUE.toDouble()
    var northLat = Int.MIN_VALUE.toDouble()

    for (lineString in multiLineString.coordinates) {
        for (point in lineString) {
            westLon = min(westLon, point.longitude)
            southLat = min(southLat, point.latitude)
            eastLon = max(eastLon, point.longitude)
            northLat = max(northLat, point.latitude)
        }
    }
    return BoundingBox(westLon, southLat, eastLon, northLat)
}

/**
 * Given a Polygon object returns the bounding box.
 * @param polygon
 * Polygon object.
 * @return A Bounding Box for the Polygon.
 */
fun getBoundingBoxOfPolygon(polygon: Polygon): BoundingBox{
    var westLon = Int.MAX_VALUE.toDouble()
    var southLat = Int.MAX_VALUE.toDouble()
    var eastLon = Int.MIN_VALUE.toDouble()
    var northLat = Int.MIN_VALUE.toDouble()

    for (geometry in polygon.coordinates) {
        for (point in geometry) {
            westLon = min(westLon, point.longitude)
            southLat = min(southLat, point.latitude)
            eastLon = max(eastLon, point.longitude)
            northLat = max(northLat, point.latitude)
        }
    }
    return BoundingBox(westLon, southLat, eastLon, northLat)
}

/**
 * Given a MultiPolygon object returns the bounding box for it.
 * @param multiPolygon
 * MultiPolygon object with multiple polygons.
 * @return A Bounding Box for the MultiPolygon.
 */
fun getBoundingBoxOfMultiPolygon(multiPolygon: MultiPolygon): BoundingBox {
    var westLon = Int.MAX_VALUE.toDouble()
    var southLat = Int.MAX_VALUE.toDouble()
    var eastLon = Int.MIN_VALUE.toDouble()
    var northLat = Int.MIN_VALUE.toDouble()

    for (polygon in multiPolygon.coordinates) {
        for (linearRing in polygon) {
            for (point in linearRing) {
                westLon = min(westLon, point.longitude)
                southLat = min(southLat, point.latitude)
                eastLon = max(eastLon, point.longitude)
                northLat = max(northLat, point.latitude)
            }
        }
    }
    return BoundingBox(westLon, southLat, eastLon, northLat)
}

/**
 * Given a BoundingBox returns the coordinates for the corners.
 * @param boundingBox
 * A BoundingBox object.
 * @return BoundingBoxCorners object (NW corner, SW corner, SE corner and NE corner).
 */
fun getBoundingBoxCorners(boundingBox: BoundingBox): BoundingBoxCorners {
    val boundingBoxCorners = BoundingBoxCorners()
    boundingBoxCorners.northWestCorner = LngLatAlt(boundingBox.westLongitude, boundingBox.northLatitude)
    boundingBoxCorners.southWestCorner = LngLatAlt(boundingBox.westLongitude, boundingBox.southLatitude)
    boundingBoxCorners.southEastCorner = LngLatAlt(boundingBox.eastLongitude, boundingBox.southLatitude)
    boundingBoxCorners.northEastCorner = LngLatAlt(boundingBox.eastLongitude, boundingBox.northLatitude)

    return boundingBoxCorners
}

/**
 * Gives the coordinates for the center of a bounding box.
 * @param bbCorners
 * The corners of the bounding box.
 * @return The lon lat of the center as LngLatAlt.
 */
fun getCenterOfBoundingBox(
    bbCorners: BoundingBoxCorners
): LngLatAlt {
    val maxLat = maxOf(bbCorners.northWestCorner.latitude, bbCorners.southWestCorner.latitude, bbCorners.southEastCorner.latitude, bbCorners.northEastCorner.latitude)
    val minLat = minOf(bbCorners.northWestCorner.latitude, bbCorners.southWestCorner.latitude, bbCorners.southEastCorner.latitude, bbCorners.northEastCorner.latitude)
    val maxLon = maxOf(bbCorners.northWestCorner.longitude, bbCorners.southWestCorner.longitude, bbCorners.southEastCorner.longitude, bbCorners.northEastCorner.longitude)
    val minLon = minOf(bbCorners.northWestCorner.longitude, bbCorners.southWestCorner.longitude, bbCorners.southEastCorner.longitude, bbCorners.northEastCorner.longitude)
    val e1 = (minLat + maxLat) / 2
    val e2 = (maxLon + minLon) / 2

    return LngLatAlt(e2, e1)
}

/**
 * Given a BoundingBox returns a closed Polygon.
 * @param boundingBox
 * A BoundingBox object.
 * @return A closed Polygon.
 */
fun getPolygonOfBoundingBox(boundingBox: BoundingBox): Polygon{
    val cornerCoordinates = getBoundingBoxCorners(boundingBox)
    val polygonObject = Polygon().also {
        it.coordinates = arrayListOf(
            arrayListOf(
                LngLatAlt(cornerCoordinates.northWestCorner.longitude, cornerCoordinates.northWestCorner.latitude ),
                LngLatAlt(cornerCoordinates.southWestCorner.longitude, cornerCoordinates.southWestCorner.latitude),
                LngLatAlt(cornerCoordinates.southEastCorner.longitude, cornerCoordinates.southEastCorner.latitude),
                LngLatAlt(cornerCoordinates.northEastCorner.longitude, cornerCoordinates.northEastCorner.latitude),
                LngLatAlt(cornerCoordinates.northWestCorner.longitude, cornerCoordinates.northWestCorner.latitude)
            )
        )
    }
    return polygonObject
}

/**
 * Gives the heading from one point to another point.
 * @param lat1
 * @param lon2
 * @param lat2
 * @param lon2
 * @return The heading in degrees clockwise from north.
 */
fun bearingFromTwoPoints(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val latitude1 = toRadians(lat1)
    val latitude2 = toRadians(lat2)
    val longDiff = toRadians(lon2 - lon1)
    val y = sin(longDiff) * cos(latitude2)
    val x = cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(latitude2) * cos(longDiff)
    return ((fromRadians(atan2(y, x)) + 360) % 360).round(1)
}

/**
 * Determine if a coordinate is contained within a polygon.
 * @param lngLatAlt
 * Coordinates to test as LngLatAlt.
 * @param polygon
 * The Polygon to test.
 * @return If coordinate is in polygon.
 */
fun polygonContainsCoordinates(lngLatAlt: LngLatAlt, polygon: Polygon): Boolean {

    var intersections = 0
    for (coordinate in polygon.coordinates) {
        for (i in 1 until coordinate.size) {
            val v1 = coordinate[i - 1]
            val v2 = coordinate[i]

            if (lngLatAlt == v2) {
                return true
            }

            if (v1.latitude == v2.latitude
                && v1.latitude == lngLatAlt.latitude
                && lngLatAlt.longitude > (if (v1.longitude > v2.longitude) v2.longitude else v1.longitude)
                && lngLatAlt.longitude < if (v1.longitude < v2.longitude) v2.longitude else v1.longitude
            ) {
                // Is horizontal polygon boundary
                return true
            }

            if (lngLatAlt.latitude > (if (v1.latitude < v2.latitude) v1.latitude else v2.latitude)
                && lngLatAlt.latitude <= (if (v1.latitude < v2.latitude) v2.latitude else v1.latitude)
                && lngLatAlt.longitude <= (if (v1.longitude < v2.longitude) v2.longitude else v1.longitude)

            ) {
                val intersection =
                    (lngLatAlt.latitude - v1.latitude) * (v2.longitude - v1.longitude) / (v2.latitude - v1.latitude) + v1.longitude

                if (intersection == lngLatAlt.longitude) {
                    // Is other boundary
                    return true
                }

                if (v1.longitude == v2.longitude || lngLatAlt.longitude <= intersection) {
                    intersections++
                }
            }
        }
    }

    return intersections % 2 != 0
}

/**
 * Return a destination coordinate based on a starting point, bearing and distance.
 * @param start
 * Starting coordinate.
 * @param bearing
 * Bearing to the destination point in degrees.
 * @param distance
 * Distance to the destination point in meters.
 * @return The destination coordinate as LngLatAlt object.
 */
fun getDestinationCoordinate(start: LngLatAlt, bearing: Double, distance: Double): LngLatAlt {
    val lat1 = toRadians(start.latitude)
    val lon1 = toRadians(start.longitude)

    val d = distance / EARTH_RADIUS_METERS // Distance in radians

    val bearingRadians = toRadians(bearing)

    val lat2 = asin(
        sin(lat1) * cos(d) +
                cos(lat1) * sin(d) * cos(bearingRadians)
    )
    val lon2 = lon1 + atan2(
        sin(bearingRadians) * sin(d) * cos(lat1),
        cos(d) - sin(lat1) * sin(lat2)
    )

    return LngLatAlt(fromRadians(lon2), fromRadians(lat2))
}

/**
 * Calculates a coordinate on a LineString at a target distance from the first coordinate of the LineString.
 * - note: If the target distance is greater than the LineString distance, the last LineString coordinate is returned.
 * - note: If the target distance is between two coordinates on the LineString, a synthesized coordinate between the coordinates is returned.
 * - note: If the target distance is smaller or equal to zero, the first LineString coordinate is returned.
 * @param path
 * The LineString that we want to generate a new coordinate for.
 * @param targetDistance
 * The distance in meters that we want the coordinate to be on the LineString.
 * @return The new coordinate as a LngLatAlt object.
 */
fun getReferenceCoordinate(path: LineString, targetDistance: Double, reversePath: Boolean): LngLatAlt {

    if (path.coordinates.size == 1 || targetDistance <= 0.0) return path.coordinates.first()

    if (targetDistance == Double.MAX_VALUE) return path.coordinates.last()

    if (reversePath) path.coordinates.reverse()

    var totalDistance = 0.0
    // work our way along the linestring to check the distance
    for (i in 0 until path.coordinates.lastIndex) {
        val coord1 = path.coordinates[i]
        val coord2 = path.coordinates[i + 1]

        val coordDistance = distance(
            coord1.latitude,
            coord1.longitude,
            coord2.latitude,
            coord2.longitude
        )
        totalDistance += coordDistance

        if (totalDistance == targetDistance) {
            return coord2
        }

        if (totalDistance > targetDistance) {
            // Target coordinate is between two coordinates so synthesize it
            val prevTotalDistance = totalDistance - coordDistance
            val prevTotalDistanceToTargetDistance = targetDistance - prevTotalDistance
            val bearing = bearingFromTwoPoints(coord1.latitude, coord1.longitude, coord2.latitude, coord2.longitude)
            return getDestinationCoordinate(coord1, bearing, prevTotalDistanceToTargetDistance)
        }
    }
    return path.coordinates.last()
}

/**
 * Calculates the Bounding Box coordinates for a Slippy Tile with a given zoom level.
 * @param x
 * X of the slippy tile name.
 * @param y
 * Y of the slippy tile name.
 * @param zoom
 * Zoom level of the slippy tile name.
 * @return A bounding box object that contains coordinates for West/South/East/North edges
 * or min Lon / min Lat / max Lon / max Lat.
 */
fun tileToBoundingBox(x: Int, y: Int, zoom: Double): BoundingBox {
    val boundingBox = BoundingBox()
    boundingBox.northLatitude = tileToLat(y, zoom)
    boundingBox.southLatitude = tileToLat(y + 1, zoom)
    boundingBox.westLongitude = tileToLon(x, zoom)
    boundingBox.eastLongitude = tileToLon(x + 1, zoom)
    return boundingBox
}

/**
 * Converts the X of a Slippy Tile with a given zoom level into a latitude.
 * @param x
 * X of the Slippy Tile name.
 * @param zoom
 * Zoom level of the Slippy Tile
 * @return a latitude coordinate.
 */
fun tileToLat(x: Int, zoom: Double): Double {
    val n: Double = Math.PI - (2.0 * Math.PI * x) / 2.0.pow(zoom)
    return Math.toDegrees(atan(sinh(n)))
}

/**
 * Converts the Y of a Slippy Tile with a given zoom level into a longitude.
 * @param y
 * Y of the Slippy Tile name.
 * @param zoom
 * Zoom level of the Slippy Tile.
 * @return a longitude coordinate.
 */
fun tileToLon(y: Int, zoom: Double): Double {
    return y / 2.0.pow(zoom) * 360.0 - 180
}

/**
 * Return a triangle that is used as a "field of view".
 * @param left
 * The left most point from the starting point.
 * @param location
 * The starting location of the triangle.
 * @param right
 * The right most point from the starting point.
 * @return A triangle that is a Polygon object.
 */
fun createTriangleFOV(left: LngLatAlt, location: LngLatAlt, right: LngLatAlt): Polygon {
    val polygonTriangleFOV = Polygon().also {
        it.coordinates = arrayListOf(
            arrayListOf(
                left,
                location,
                right,
                // Close the polygon
                left
            )
        )
    }
    return polygonTriangleFOV
}

/**
 * Converts a circle to a polygon "circle".
 * @param segments
 * Number of segments the polygon should have.
 * @param centerLat
 * Latitude of the center of the circle.
 * @param centerLon
 * Longitude of the center of the circle.
 * @param radius
 * Radius of the circle.
 * @return Polygon object.
 */
fun circleToPolygon(segments: Int, centerLat: Double, centerLon: Double, radius: Double): Polygon {

    val points = mutableListOf<LngLatAlt>()
    val relativeLatitude = radius / EARTH_RADIUS_METERS * 180 / PI
    val relativeLongitude = relativeLatitude / cos(toRadians(centerLat)) % 90

    for (i in 0 until segments) {
        var theta = 2.0 * PI * i.toDouble() / segments

        theta += 0.001
        if (theta >= 2 * PI) {
            theta -= 2 * PI
        }

        var latOnCircle = centerLat + relativeLatitude * sin(theta)
        var lonOnCircle = centerLon + relativeLongitude * cos(theta)
        if (lonOnCircle > 180) {
            lonOnCircle = -180 + (lonOnCircle - 180)
        } else if (lonOnCircle < -180) {
            lonOnCircle = 180 - (lonOnCircle + 180)
        }

        if (latOnCircle > 90) {
            latOnCircle = 90 - (latOnCircle - 90)
        } else if (latOnCircle < -90) {
            latOnCircle = -90 - (latOnCircle + 90)
        }

        points.add(LngLatAlt(lonOnCircle, latOnCircle))
    }
    // should end with same point as the origin
    points.add(LngLatAlt(points[0].longitude, points[0].latitude))

    val tempCirclePoints = arrayListOf(LngLatAlt())
    for(point in points){
        tempCirclePoints.add(point)
    }
    tempCirclePoints.removeAt(0)

    val polygonObject = Polygon().also {
        it.coordinates = arrayListOf(
            tempCirclePoints
        )
    }
    return polygonObject
}

/**
 * Calculates the bounds of the quadrants that should be used for filtering POIs
 * These quadrants are rotated from the standard cardinal direction
 * quadrants (north: [315.0, 45.0), east: [45.0, 135.0), etc.) such that the provided
 * heading becomes the center of the standard cardinal direction quadrant that it belongs
 * to. This ensures that callouts filtered into quadrants will always be centered around
 * the user's current heading if possible (if quadrants were fixed to the standard cardinal
 * direction, and the user were facing 315.0, then all callouts for `north` would be to the
 * user's right).
 *  Here are several examples to illustrate how this works:
 *
 *  Heading := 0.0 In this case, the heading is already aligned with the center of
 *  the `north` quadrant, so the standard cardinal direction quadrants are returned.
 *  Quadrants returned:
 *     north: [315.0, 45.0]
 *     east: [45.0, 135.0]
 *     south: [135.0, 225.0]
 *     west: [225.0, 315.0]
 *
 *  Heading := 95.0 In this case, the heading is in the `east` quadrant, but it is rotated
 *  5.0 degrees clockwise from the center of the `east` quadrant, so the
 *  standard cardinal direction quadrants are rotated 5.0 degrees clockwise.
 *  Quadrants returned:
 *     north: [320.0, 50.0]
 *     east: [50.0, 140.0]
 *     south: [140.0, 230.0]
 *     west: [230.0, 320.0]
 *
 *  Heading := 230.0 In this case, the heading is in the `west` quadrant, but it is rotated
 *  40.0 degrees counter-clockwise from the center of the `west` quadrant, so
 *  the standard cardinal direction quadrants are rotated 40.0 degrees counter-clockwise.
 *  Quadrants returned:
 *     north: [275.0, 5.0]
 *     east: [5.0, 95.0]
 *     south: [95.0, 185.0]
 *     west: [185.0, 275.0]
 * @param heading
 * Heading in degrees.
 * @return A List of Quadrant objects that hold the heading for each quadrant.
 */
fun getQuadrants(heading: Double): List<Quadrant> {
    // Find the quadrant the heading is currently in
    val quadrantIndex = ((heading + 45.0).rem(360.0)).toInt() / 90

    val northHeading = when (quadrantIndex) {
        0 -> heading
        else -> (heading + 90 * (4 - quadrantIndex).toDouble()).rem(360.0)
    }

    // Define the quadrants based off the offset heading to the north
    return listOf(
        Quadrant(northHeading),         // North
        Quadrant(northHeading + 90.0),  // East
        Quadrant(northHeading + 180.0), // South
        Quadrant(northHeading + 270.0)  // West
    )
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
 * Distance to an intersection from current location.
 * @param location
 * LngLatAlt of current location
 * @param intersection
 * Coordinates for intersection that we want to know the distance to as Point
 * @return The distance of the current location to the intersection in meters.
 */
fun distanceToIntersection(
    location: LngLatAlt,
    intersection: Point
): Double {
    return distance(
        location.latitude,
        location.longitude,
        intersection.coordinates.latitude,
        intersection.coordinates.longitude
    )

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

fun toRadians(degrees: Double): Double {
    return degrees * DEGREES_TO_RADIANS
}

fun fromRadians(degrees: Double): Double {
    return degrees * RADIANS_TO_DEGREES
}

fun Double.round(digitLength: Int): Double {
    val pow = 10.0.pow(digitLength)
    return (this * pow).roundToLong() / pow
}