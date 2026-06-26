package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.dto.BoundingBoxCorners
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import java.util.ArrayList
import kotlin.math.PI
import kotlin.math.abs
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

    return (EARTH_RADIUS_METERS * c)//.round(2)
}

/**
 * Calculates the centroid of a polygon.
 *
 * This function works for both convex and non-convex (concave) simple polygons.
 * It assumes the polygon is not self-intersecting.
 * * @param polygon The polygon for which to calculate the centroid. It is assumed the first
 *                and last points of the polygon's outer ring are the same (closed).
 * @return The centroid coordinate as a LngLatAlt object, or null if the polygon is invalid.
 */
fun getCentroidOfPolygon(polygon: Polygon): LngLatAlt? {
    val ring = polygon.coordinates.firstOrNull() ?: return null
    if (ring.size < 4) {
        return null
    }

    var signedArea = 0.0
    var centroidX = 0.0
    var centroidY = 0.0

    // Iterate over the edges of the polygon's outer ring.
    for (i in 0 until ring.size - 1) {
        val p1 = ring[i]
        val p2 = ring[i + 1]

        // Use the Shoelace formula component to calculate the signed area of the
        // triangle formed by the current segment and the origin (0,0).
        val areaComponent = (p1.longitude * p2.latitude) - (p2.longitude * p1.latitude)
        signedArea += areaComponent

        // Sum the centroids of these triangles, weighted by the area component.
        centroidX += (p1.longitude + p2.longitude) * areaComponent
        centroidY += (p1.latitude + p2.latitude) * areaComponent
    }

    // The total signed area of the polygon is half the accumulated value.
    signedArea *= 0.5

    // Avoid division by zero for invalid or zero-area polygons.
    if (signedArea == 0.0) {
        return null
    }

    // The centroid's coordinates are the weighted sums divided by 6 times the signed area.
    val finalCentroidX = centroidX / (6.0 * signedArea)
    val finalCentroidY = centroidY / (6.0 * signedArea)

    return LngLatAlt(finalCentroidX, finalCentroidY)
}
fun getCentralPointForFeature(feature: Feature) : LngLatAlt? {
    return when(feature.geometry.type) {
        "Point" -> (feature.geometry as Point).coordinates
        "Polygon" -> getCentroidOfPolygon(feature.geometry as Polygon)
        else -> null
    }
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
fun getBoundingBoxesOfMultiPolygon(multiPolygon: MultiPolygon): List<BoundingBox> {
    val boundingBoxes = mutableListOf<BoundingBox>()
    for (polygon in multiPolygon.coordinates) {
        var westLon = Int.MAX_VALUE.toDouble()
        var southLat = Int.MAX_VALUE.toDouble()
        var eastLon = Int.MIN_VALUE.toDouble()
        var northLat = Int.MIN_VALUE.toDouble()
        // Just use the outer ring of each polygon
        for (point in polygon[0]) {
            westLon = min(westLon, point.longitude)
            southLat = min(southLat, point.latitude)
            eastLon = max(eastLon, point.longitude)
            northLat = max(northLat, point.latitude)
        }
        boundingBoxes.add(BoundingBox(westLon, southLat, eastLon, northLat))
    }
    return boundingBoxes
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
 * @param loc1
 * @param loc2
 * @return The heading in degrees clockwise from north.
 */
fun bearingFromTwoPoints(
    loc1: LngLatAlt,
    loc2: LngLatAlt,
): Double {
    val latitude1 = toRadians(loc1.latitude)
    val latitude2 = toRadians(loc2.latitude)
    val longDiff = toRadians(loc2.longitude - loc1.longitude)
    val y = sin(longDiff) * cos(latitude2)
    val x = cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(latitude2) * cos(longDiff)
    return ((fromRadians(atan2(y, x)) + 360) % 360).round(1)
}

/**
 * Determine if a coordinate is contained within a polygon.
 * @param lngLatAlt
 * Coordinates to test as LngLatAlt.
 * @param regionCoordinates
 * An ArrayList of the points of the polygon to test.
 * @return If coordinate is in polygon.
 */
fun regionContainsCoordinates(lngLatAlt: LngLatAlt, regionCoordinates: ArrayList<LngLatAlt>): Boolean {

    var intersections = 0
    for (i in 1 until regionCoordinates.size) {
        val v1 = regionCoordinates[i - 1]
        val v2 = regionCoordinates[i]

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

    return intersections % 2 != 0
}

fun polygonContainsCoordinates(lngLatAlt: LngLatAlt, polygon: Polygon): Boolean {
    return regionContainsCoordinates(lngLatAlt, polygon.coordinates[0])
}

fun multiPolygonContainsCoordinates(lngLatAlt: LngLatAlt, multiPolygon: MultiPolygon): Boolean {

    for(polygon in multiPolygon.coordinates)
        if(regionContainsCoordinates(lngLatAlt, polygon[0]))
            return true

    return false
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
 * @param reverseLineString
 * Reverse the sort order of the LineString coordinates so "first" is "last"
 * @return The new coordinate as a LngLatAlt object.
 */
fun getReferenceCoordinate(path: LineString, targetDistance: Double, reverseLineString: Boolean): LngLatAlt {

    if (path.coordinates.size == 1 || targetDistance <= 0.0) return path.coordinates.first()

    if (targetDistance == Double.MAX_VALUE) return path.coordinates.last()

    //if (reverseLineString) path.coordinates.reverse()
    if (reverseLineString) {
        val reversedCoordinates = path.coordinates.toMutableList().asReversed()
        val reversedPath = LineString().also {
            it.coordinates = ArrayList(reversedCoordinates)
        }
        var totalDistance = 0.0
        // work our way along the linestring to check the distance
        for (i in 0 until reversedPath.coordinates.lastIndex) {
            val c1 = reversedPath.coordinates[i]
            val c2 = reversedPath.coordinates[i + 1]

            val cDistance = distance(
                c1.latitude,
                c1.longitude,
                c2.latitude,
                c2.longitude
            )
            totalDistance += cDistance

            if (totalDistance == targetDistance) {
                return c2
            }

            if (totalDistance > targetDistance) {
                // Target coordinate is between two coordinates so synthesize it
                val prevTotalDistance = totalDistance - cDistance
                val prevTotalDistanceToTargetDistance = targetDistance - prevTotalDistance
                val bearing = bearingFromTwoPoints(c1, c2)
                return getDestinationCoordinate(c1, bearing, prevTotalDistanceToTargetDistance)
            }
        }

    } else {
        var totalDistance = 0.0
        // work our way along the linestring to check the distance
        for (i in 0 until path.coordinates.lastIndex) {
            val c1 = path.coordinates[i]
            val c2 = path.coordinates[i + 1]

            val cDistance = distance(
                c1.latitude,
                c1.longitude,
                c2.latitude,
                c2.longitude
            )
            totalDistance += cDistance

            if (totalDistance == targetDistance) {
                return c2
            }

            if (totalDistance > targetDistance) {
                // Target coordinate is between two coordinates so synthesize it
                val prevTotalDistance = totalDistance - cDistance
                val prevTotalDistanceToTargetDistance = targetDistance - prevTotalDistance
                val bearing = bearingFromTwoPoints(c1, c2)
                return getDestinationCoordinate(c1, bearing, prevTotalDistanceToTargetDistance)
            }
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
fun tileToBoundingBox(x: Int, y: Int, zoom: Int): BoundingBox {
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
fun tileToLat(x: Int, zoom: Int): Double {
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
fun tileToLon(y: Int, zoom: Int): Double {
    return y / 2.0.pow(zoom) * 360.0 - 180
}

/**
 * Return a triangle Polygon e.g. for use as a "field of view".
 * @param triangle
 * The points of the triangle
 * @return A Polygon object with the triangle coordinates.
 */
fun createPolygonFromTriangle(triangle: Triangle): Polygon {
    val polygonTriangleFOV = Polygon().also {
        it.coordinates = arrayListOf(
            arrayListOf(
                triangle.left,
                triangle.origin,
                triangle.right,
                // Close the polygon
                triangle.left
            )
        )
    }
    return polygonTriangleFOV
}

fun getTriangleForDirection(featureCollection: FeatureCollection, direction: Int) : Triangle {
    val geometry = featureCollection.features[direction].geometry as Polygon
    // The order of coordinates is as passed in to createTriangleFOV
    return Triangle(
        geometry.coordinates[0][1],     // location
        geometry.coordinates[0][0],     // left
        geometry.coordinates[0][2]      // right
    )
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
 * Distance to a Polygon from current location.
 * @param pointCoordinates
 * LngLatAlt of current location
 * @param outerRing
 * The outerRing of the Polygon that we are working out the distance from
 * @return The closest distance of the point to the Polygon
 */
fun distanceToRegion(
    pointCoordinates: LngLatAlt,
    outerRing: LineString,
    ruler: Ruler,
    nearestPoint: LngLatAlt? = null) : Double
{
    val pdh = ruler.distanceToLineString(pointCoordinates, outerRing)
    if(nearestPoint != null) {
        nearestPoint.latitude = pdh.point.latitude
        nearestPoint.longitude = pdh.point.longitude
    }
    return pdh.distance
}

fun distanceToPolygon(
    pointCoordinates: LngLatAlt,
    polygon: Polygon,
    ruler: Ruler,
    nearestPoint: LngLatAlt? = null)
: Double {

    // We're only looking at the outer ring, which is really just a LineString
    val lineString = LineString()
    lineString.coordinates = polygon.coordinates[0]
    return if(polygonContainsCoordinates(pointCoordinates, polygon)) {
        nearestPoint?.latitude = pointCoordinates.latitude
        nearestPoint?.longitude = pointCoordinates.longitude
        0.0
    }
    else
        distanceToRegion(pointCoordinates, lineString, ruler, nearestPoint)
}

fun distanceToMultiPolygon(
    pointCoordinates: LngLatAlt,
    polygon: MultiPolygon,
    ruler: Ruler,
    nearestPoint: LngLatAlt? = null)
        : Double {

    // Check each polygon in turn
    var shortestDistance = Double.POSITIVE_INFINITY
    for(region in polygon.coordinates) {
        if(
            polygonContainsCoordinates(
            pointCoordinates, Polygon(region[0])
            )
        ) {
            return 0.0
        }
        // We're only looking at the outer ring, which is really just a LineString
        val lineString = LineString()
        lineString.coordinates = region[0]
        val distance = distanceToRegion(pointCoordinates, lineString, ruler, nearestPoint)
        if(distance < shortestDistance)
            shortestDistance = distance
    }
    return shortestDistance
}

/**
 * Calculate distance of a point (pLat,pLon) to a line defined by two other points (lat1,lon1) and
 * (lat2,lon2). This is only valid for lines that are 'near' each other as we're using straight
 * lines and not following the curve of the earth.
 * @param x1 double
 * @param y1 double
 * @param x2 double
 * @param y2 double
 * @param x double
 * @param y double
 * @return the distance of the point to the line
 */
fun distance(x1: Double,
             y1: Double,
             x2: Double,
             y2: Double,
             x: Double,
             y: Double,
             nearestPoint: LngLatAlt? = null): Double {
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

    nearestPoint?.latitude = xx
    nearestPoint?.longitude = yy

    return if (onSegment(xx, yy, x1, y1, x2, y2)) {
        sqrt((x - xx) * (x - xx) + (y - yy) * (y - yy))
    } else {
        val distance1 = sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1))
        val distance2 = sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2))

        // Set nearestPoint to be the nearest location on the line
        if(distance1 < distance2) {
            nearestPoint?.latitude = x1
            nearestPoint?.longitude = y1
        } else {
            nearestPoint?.latitude = x2
            nearestPoint?.longitude = y2
        }
        min(distance1, distance2)
    }
}

fun onSegment(x: Double, y: Double, x1: Double, y1: Double, x2: Double, y2: Double): Boolean {
    val minX = min(x1, x2)
    val maxX = max(x1, x2)

    val miny = min(y1, y2)
    val maxy = max(y1, y2)

    return x in minX..maxX && y >= miny && y <= maxy
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

/**
 * Calculate the center coordinates of a circle based on the arc midpoint, chord bearing and radius.
 * @param arcMidPoint
 * the mid point of the arc as LngLatAlt
 * @param chordBearing
 * The bearing of the chord as a double.
 * @param radius
 * The radius of the arc as a double.
 * @return The coordinates of the center of the circle as LngLatAlt.
 */
fun findCircleCenter(
    arcMidPoint: LngLatAlt,
    chordBearing: Double,
    radius: Double
): LngLatAlt {
    // Calculate the bearing of the perpendicular bisector
    val perpendicularBearing = (chordBearing + 90.0) % 360.0
    // Calculate the circle center using the arc midpoint, bearing, and radius
    val centerCoordinates = getDestinationCoordinate(arcMidPoint, perpendicularBearing, radius)

    return centerCoordinates
}

/**
 * Calculate the radius of a circle based on the chord length, arc midpoint and chord midpoint
 * @param chordLength
 * the length of the segment chord
 * @param arcMidPoint
 * coordinates of the arc midpoint as LngLatAlt
 * @param chordMidPoint
 * The coordinates of the chord midpoint as LngLatAlt.
 * @return The coordinates of the center of the circle as LngLatAlt.
 */
fun calculateRadius(
    chordLength: Double,
    arcMidPoint: LngLatAlt,
    chordMidPoint: LngLatAlt
): Double {
    val h =
        distance(arcMidPoint.latitude, arcMidPoint.longitude, chordMidPoint.latitude, chordMidPoint.longitude)
    // https://math.stackexchange.com/questions/2809531/how-to-find-a-circle-given-a-segment
    var radius = (h / 2) + (chordLength * chordLength / (8 * h))
    // Iterate to refine radius (more repeats more accuracy but we don't need to be super accurate
    // as we are looking for an approximate value to eventually splat our relative directions polygon)
    repeat(10) {
        val centralAngle = 2 * asin(chordLength / (2 * radius))
        radius = chordLength / (2 * sin(centralAngle / 2))
    }
    // we've got the radius but now we need to work out the circle center coordinates
    return radius
}


/**
 * Checks if a point is on the right side of a line segment or not.
 * @param start
 * coordinate of line segment as LngLatAlt
 * @param pointToCheck
 * point to check as LngLatAlt
 * @param end
 * coordinate of line segment as LngLatAlt
 * @return true if b is right of the line defined by start and end coordinates.
 */
fun pointOnRightSide(
    start: LngLatAlt,
    pointToCheck: LngLatAlt,
    end: LngLatAlt
): Boolean {
    return (pointToCheck.longitude - start.longitude) * (end.latitude - start.latitude) - (pointToCheck.latitude - start.latitude) * (end.longitude - start.longitude) > 0
}

/**
 * Check if two LineStrings intersect.
 * @param lineString1
 * Line1 as LineString.
 * @param lineString2
 * Line2 as LineString.
 * @return true if the line strings intersect each other.
 */
fun lineStringsIntersect(
    lineString1: LineString,
    lineString2: LineString
): Boolean {
    var lineStringsIntersect = false

    // Loop through segments of lineString1
    for (i in 0 until lineString1.coordinates.size - 1) {
        val line1Start = lineString1.coordinates[i]
        val line1End = lineString1.coordinates[i + 1]

        // Loop through segments of lineString2
        for (j in 0 until lineString2.coordinates.size - 1) {
            val line2Start = lineString2.coordinates[j]
            val line2End = lineString2.coordinates[j + 1]

            // Check if segments intersect
            if (straightLinesIntersect(line1Start, line1End, line2Start, line2End)) {
                lineStringsIntersect = true
                break
            }
        }

        if (lineStringsIntersect) break
    }
    return lineStringsIntersect

}

/**
 * Check if straight lines defined by line1Start, line1End, line2Start, line2End intersect.
 * @param line1Start
 * Start of line1 as LngLatAlt.
 * @param line1End
 * End of line1 as LngLatAlt.
 * @param line2Start
 * Start of line2 as LngLatAlt.
 * @param line2End
 * End of line2 as LngLatAlt.
 * @return true if the lines intersect each other.
 */
fun straightLinesIntersect(
    line1Start: LngLatAlt,
    line1End: LngLatAlt,
    line2Start: LngLatAlt,
    line2End: LngLatAlt,
): Boolean {

    val line1Vertical = line1Start.latitude == line1End.latitude
    val line2Vertical = line2Start.latitude == line2End.latitude
    val line1Horizontal = isLineHorizontal(line1Start, line1End)
    val line2Horizontal = isLineHorizontal(line2Start, line2End)
    return when {
        line1Vertical && line2Vertical ->
            if (line1Start.latitude == line2Start.latitude) {
                // lines are both vertical check whether they overlap
                line1Start.longitude <= line2Start.longitude && line2Start.longitude < line1End.longitude || line1Start.longitude <= line2End.longitude && line2End.longitude < line1End.longitude
            } else {
                // parallel -> they don't intersect
                false
            }
        line1Horizontal && line2Horizontal -> {
            if (line1Start.longitude == line2Start.longitude) {
                // lines are both horizontal check whether they overlap
                if (isBetween(line1Start.latitude, line1End.latitude, line2Start.latitude) || isBetween(line1Start.latitude, line1End.latitude, line2End.latitude)) {
                    true
                } else {
                    false // No intersection
                }
            } else {
                false // Parallel lines, no intersection
            }
        }
        line1Vertical && line2Horizontal -> {
            val intersectLon = line1Start.longitude
            val intersectLat = line2Start.latitude

            // Check if the intersection point is within the bounds of both lines
            if (isBetween(line1Start.longitude, line1End.longitude, intersectLon) &&
                isBetween(line2Start.latitude, line2End.latitude, intersectLat)
            ) {
                true
            } else {
                false // No intersection
            }
        }
        line1Horizontal && line2Vertical -> {
            // If line1 is horizontal and line2 is vertical
            // the intersection point is the longitude of line2 and the latitude of line1
            val intersectLon = line2Start.longitude
            val intersectLat = line1Start.latitude

            // Check if the intersection point is within the bounds of both lines
            if (isBetween(line1Start.latitude, line1End.latitude, intersectLat) &&
                isBetween(line2Start.longitude, line2End.longitude, intersectLon)
            ) {
                true
            } else {
                false // No intersection
            }
        }
        line1Vertical -> {
            val gradient2 = (line2End.longitude - line2Start.longitude) / (line2End.latitude - line2Start.latitude)
            val a2 = line2Start.longitude - gradient2 * line2Start.latitude
            val yi = a2 + gradient2 * line1Start.latitude

            isBetween(line1Start.longitude, line1End.longitude, yi) && isBetween(line2Start.longitude, line2End.longitude, yi)
        }
        line2Vertical -> {
            val gradient1 = (line1End.longitude - line1Start.longitude) / (line1End.latitude - line1Start.latitude)
            val a1 = line1Start.longitude - gradient1 * line1Start.latitude
            val yi = a1 + gradient1 * line2Start.latitude

            isBetween(line1Start.longitude, line1End.longitude, yi) && isBetween(line2Start.longitude, line2End.longitude, yi)
        }
        else -> {
            if((line1Start == line2Start) ||
               (line1Start == line2End) ||
               (line1End == line2Start) ||
               (line1End == line2End)) {
                true
            } else {
                val gradient1 =
                    (line1End.longitude - line1Start.longitude) / (line1End.latitude - line1Start.latitude)
                val gradient2 =
                    (line2End.longitude - line2Start.longitude) / (line2End.latitude - line2Start.latitude)

                val a1 = line1Start.longitude - gradient1 * line1Start.latitude
                val a2 = line2Start.longitude - gradient2 * line2Start.latitude

                if (gradient1 - gradient2 == 0.0) {
                    // same gradient
                    if (abs(a1 - a2) < .0000001) {
                        // lines are definitely the same within a margin of error, check if overlaps
                        isBetween(
                            line1Start.latitude,
                            line1End.latitude,
                            line2Start.latitude
                        ) || isBetween(line1Start.latitude, line1End.latitude, line2End.latitude)
                    } else {
                        // parallel
                        false
                    }
                } else {
                    // calculate intersection coordinates
                    val intersectLat = -(a1 - a2) / (gradient1 - gradient2)
                    val intersectLon = a1 + gradient1 * intersectLat

                    (line1Start.latitude - intersectLat) * (intersectLat - line1End.latitude) >= 0 &&
                            (line2Start.latitude - intersectLat) * (intersectLat - line2End.latitude) >= 0 &&
                            (line1Start.longitude - intersectLon) * (intersectLon - line1End.longitude) >= 0 &&
                            (line2Start.longitude - intersectLon) * (intersectLon - line2End.longitude) >= 0
                }
            }
        }
    }
}


/**
 * Check if straight lines defined by line1Start, line1End, line2Start, line2End intersect
 * and if they do return the intersection coordinates
 * @param line1Start
 * Start of line1 as LngLatAlt.
 * @param line1End
 * End of line1 as LngLatAlt.
 * @param line2Start
 * Start of line2 as LngLatAlt.
 * @param line2End
 * End of line2 as LngLatAlt.
 * @return true if the lines intersect each other.
 */
fun straightLinesIntersectLngLatAlt(
    line1Start: LngLatAlt,
    line1End: LngLatAlt,
    line2Start: LngLatAlt,
    line2End: LngLatAlt,
): LngLatAlt? {
    val line1Vertical = isLineVertical(line1Start, line1End)
    val line2Vertical = isLineVertical(line2Start, line2End)
    val line1Horizontal = isLineHorizontal(line1Start, line1End)
    val line2Horizontal = isLineHorizontal(line2Start, line2End)

    return when {
        line1Vertical && line2Vertical -> {
            if (line1Start.latitude == line2Start.latitude) {
                // lines are both vertical check whether they overlap
                if (isBetween(line1Start.longitude, line1End.longitude, line2Start.longitude) || isBetween(line1Start.longitude, line1End.longitude, line2End.longitude)) {
                    line2Start // Return line2Start as intersection point
                } else {
                    null // No intersection
                }
            } else {
                null // Parallel lines, no intersection
            }
        }
        line1Horizontal && line2Horizontal -> {
            if (line1Start.longitude == line2Start.longitude) {
                // lines are both horizontal check whether they overlap
                if (isBetween(line1Start.latitude, line1End.latitude, line2Start.latitude) || isBetween(line1Start.latitude, line1End.latitude, line2End.latitude)) {
                    line2Start // Return line2Start as intersection point
                } else {
                    null // No intersection
                }
            } else {
                null // Parallel lines, no intersection
            }
        }
        line1Vertical && line2Horizontal -> {
            val intersectLon = line1Start.longitude
            val intersectLat = line2Start.latitude

            // Check if the intersection point is within the bounds of both lines
            if (isBetween(line1Start.longitude, line1End.longitude, intersectLon) &&
                isBetween(line2Start.latitude, line2End.latitude, intersectLat)
            ) {
                LngLatAlt(intersectLon, intersectLat, line1Start.altitude) // Return intersection point
            } else {
                null // No intersection
            }
        }
        line1Horizontal && line2Vertical -> {
            // If line1 is horizontal and line2 is vertical
            // the intersection point is the longitude of line2 and the latitude of line1
            val intersectLon = line2Start.longitude
            val intersectLat = line1Start.latitude

            // Check if the intersection point is within the bounds of both lines
            if (isBetween(line1Start.latitude, line1End.latitude, intersectLat) &&
                isBetween(line2Start.longitude, line2End.longitude, intersectLon)
            ) {
                LngLatAlt(intersectLon, intersectLat, line1Start.altitude) // Return intersection point
            } else {
                null // No intersection
            }
        }
        line1Vertical -> {
            val gradient2 = (line2End.longitude - line2Start.longitude) / (line2End.latitude - line2Start.latitude)
            val a2 = line2Start.longitude - gradient2 * line2Start.latitude
            val intersectLon = a2 + gradient2 * line1Start.latitude


            // Check if the intersection point is within the bounds of both lines
            if (isBetween(line1Start.longitude, line1End.longitude, intersectLon) && isBetween(line2Start.latitude, line2End.latitude, a2)) {
                LngLatAlt(intersectLon, line1Start.latitude, line1Start.altitude) // Return intersection point with calculated longitude
            } else {
                null // No intersection
            }
        }
        line2Vertical -> {

            val gradient1 = (line1End.latitude - line1Start.latitude) / (line1End.longitude - line1Start.longitude)
            val a1 = line1Start.longitude - gradient1 * line1Start.latitude
            val intersectLon = a1 + gradient1 * line2Start.latitude

            // Check if the intersection point is within the bounds of both lines
            if (isBetween(line1Start.latitude, line1End.latitude, a1) && isBetween(line2Start.longitude, line2End.longitude, intersectLon)) {
                LngLatAlt(intersectLon, a1, line2Start.altitude) // Return intersection point with calculated longitude
            } else {
                null // No intersection
            }
        }
        else -> {
            val gradient1 = (line1End.longitude - line1Start.longitude) / (line1End.latitude - line1Start.latitude)
            val gradient2 = (line2End.longitude - line2Start.longitude) / (line2End.latitude - line2Start.latitude)

            if (gradient1 == gradient2) {
                // Lines are parallel, check if they overlap
                val a1 = line1Start.longitude - gradient1 * line1Start.latitude
                val a2 = line2Start.longitude - gradient2 * line2Start.latitude

                if (a1 == a2) {
                    // Lines overlap, return an arbitrary point on the line (e.g., line1Start)
                    line1Start
                } else {
                    null // Lines are parallel and do not overlap
                }
            } else {
                // Lines are not parallel, calculate intersection point
                val intersectLat = (line2Start.longitude - line1Start.longitude + gradient1 * line1Start.latitude - gradient2 * line2Start.latitude) / (gradient1 - gradient2)
                val intersectLon = line1Start.longitude + gradient1 * (intersectLat - line1Start.latitude)

                // Check if the intersection point is within the bounds of both lines
                if (isBetween(line1Start.latitude, line1End.latitude, intersectLat) &&
                    isBetween(line2Start.latitude, line2End.latitude, intersectLat) &&
                    isBetween(line1Start.longitude, line1End.longitude, intersectLon) &&
                    isBetween(line2Start.longitude, line2End.longitude, intersectLon)
                ) {
                    LngLatAlt(intersectLon, intersectLat, line1Start.altitude) // Return intersection point
                } else {
                    null // Lines do not intersect within their bounds
                }
            }
        }
    }
}

fun isLineHorizontal(lineStart: LngLatAlt, lineEnd: LngLatAlt, tolerance: Double = 1e-6): Boolean {
    return abs(lineStart.latitude - lineEnd.latitude) < tolerance
}

fun isLineVertical(lineStart: LngLatAlt, lineEnd: LngLatAlt, tolerance: Double = 1e-6): Boolean {
    return abs(lineStart.longitude - lineEnd.longitude) < tolerance
}

fun isBetween(x1: Double, x2: Double, value: Double): Boolean {
    return if (x1 > x2) {
        value in x2..x1
    } else {
        value in x1..x2
    }
}

fun pointIsWithinBoundingBox(point: LngLatAlt?, box: BoundingBox) : Boolean {
    if(point == null)
        return true

    return (isBetween(box.westLongitude, box.eastLongitude, point.longitude) &&
            isBetween(box.southLatitude, box.northLatitude, point.latitude))
}

/**
 * calculateHeadingOffset calculates the angle between two headings e.g. the user heading and the
 * heading of a road.
 * @param heading1
 * @param heading2
 * @return The inner angle between the two headings in degrees. This can be up to 180.0 degrees
 * because that's the maximum difference between headings.
 */
fun calculateHeadingOffset(heading1: Double, heading2: Double): Double {
    val normalizedHeading1 = (heading1 % 360.0 + 360.0) % 360.0
    val normalizedHeading2 = (heading2 % 360.0 + 360.0) % 360.0
    var diff = abs(normalizedHeading1 - normalizedHeading2)
    if (diff > 180.0) diff = 360.0 - diff

    return diff
}

/**
 * calculateSmallestAngleBetweenLines calculates the smallest angle between two lines.
 *
 * heading of a road.
 * @param heading1
 * @param heading2
 * @return The inner angle between the two lines in degrees. This can be up to 90.0 degrees as it's
 * the smallest angle between the two lines.
 */
fun calculateSmallestAngleBetweenLines(heading1: Double, heading2: Double): Double {
    var innerAngle = calculateHeadingOffset(heading1, heading2)
    if (innerAngle > 90.0)
        innerAngle = 180.0 - innerAngle
    return innerAngle
}

enum class Side {
    LEFT, RIGHT, INLINE
}

/**
 * Determines which side of a line segment (from p1 to p2) a point (h) is on.
 *
 * @param p1 The starting point of the line segment (e.g., first vertex of a street Way).
 * @param p2 The ending point of the line segment (e.g., second vertex of a street Way).
 * @param h The point to check (e.g., the house's location).
 * @return The Side (LEFT, RIGHT, or INLINE) the house is on relative to the direction from p1 to p2.
 */
fun getSideOfLine(p1: LngLatAlt, p2: LngLatAlt, h: LngLatAlt): Side {
    // Using longitude as x and latitude as y
    val crossProduct = (p2.longitude - p1.longitude) * (h.latitude - p1.latitude) -
            (p2.latitude - p1.latitude) * (h.longitude - p1.longitude)

    return when {
        crossProduct > 0 -> Side.LEFT
        crossProduct < 0 -> Side.RIGHT
        else -> Side.INLINE
    }
}

