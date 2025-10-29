package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.dto.Circle
import org.scottishtecharmy.soundscape.dto.IntersectionRelativeDirections
import org.scottishtecharmy.soundscape.dto.Tile
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
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
): Double {

    if((road == null) || (intersection == null))
        return 0.0

    val roadCoordinates = (road.geometry as LineString).coordinates
    val intersectionCoordinate = (intersection.geometry as Point).coordinates
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
        // Our roads are all now pre-split when we parse them in from MVT
        newFeatureCollection += getFeaturesWithRoadDirection(road, intersectionRelativeDirections)
    }

    return sortFeatureCollectionByDirectionProperty(newFeatureCollection)
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

fun interpolate(
    point1: LngLatAlt,
    point2: LngLatAlt,
    fraction: Double
): LngLatAlt {
    val lon = point1.longitude + (point2.longitude - point1.longitude) * fraction
    val lat = point1.latitude + (point2.latitude - point1.latitude) * fraction
    return LngLatAlt(lon, lat)
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
