package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Geometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class InterpolatedPointsJoiner {

    private val interpolatedPoints: HashMap<Double, MutableList<LngLatAlt>> = hashMapOf()

    fun addInterpolatedPoints(feature: Feature): Boolean {
        // We add all edgePoint coordinates to our HashMap of interpolated points by OSM id
        if (feature.properties?.containsKey("class")!!) {
            if (feature.properties!!["class"] == "edgePoint") {
                val geometry = feature.geometry as Geometry<LngLatAlt>
                val osmId = feature.foreign!!["osm_id"] as Double
                if (!interpolatedPoints.containsKey(osmId)) {
                    interpolatedPoints[osmId] = mutableListOf()
                }
                for (point in geometry.coordinates) {
                    // Add the point
                    interpolatedPoints[osmId]!!.add(point)
                }
                return false
            }
        }
        return true
    }

    fun addJoiningLines(featureCollection : FeatureCollection) {
        val start = System.currentTimeMillis()
        var highestSeparation = 0.0
        var highestSeparationOsmId = 0.0
        for (entries in interpolatedPoints) {
            if (entries.value.size > 1) {
                // We want to find points that we can join together. Go through the list of points
                // for the OSM id comparing against the other members in the list to see if any are
                // almost at the same point. We only want a single line to join the points together
                // not a line from A -> B and from B -> A so we de-duplicate as we go.
                var linesAdded = 0
                var perfectlyCoincident = 0
                val deduplicateSet = mutableSetOf<LngLatAlt>()
                for ((index1, point1) in entries.value.withIndex()) {
                    if(!deduplicateSet.contains(point1)) {
                        var nearestPoint = Double.POSITIVE_INFINITY
                        var nearestPointIndex = -1
                        for ((index2, point2) in entries.value.withIndex()) {
                            if(index1 == index2) {
                                // This covers comparing the same point to itself
                                continue
                            }

                            if(point1 == point2) {
                                // This covers points which although on different tiles are
                                // perfectly coincident. In that case there's no need to add a
                                // joining line.
                                ++perfectlyCoincident
                                deduplicateSet.add(point1)
                                deduplicateSet.add(point2)
                                continue
                            }
                            if (!deduplicateSet.contains(point2)) {
                                // We haven't already added this line
                                val separation = distance(
                                        point1.latitude,
                                        point1.longitude,
                                        point2.latitude,
                                        point2.longitude
                                    )
                                if(separation < nearestPoint) {
                                    nearestPoint = separation
                                    nearestPointIndex = index2
                                }
                            }
                        }

                        if(nearestPoint < 1.0) {
                            // We've found the nearest point, and it's within 1m so join their
                            // LineStrings together.
                            val joining = Feature()
                            val foreign: HashMap<String, Any?> = hashMapOf()
                            val osmIds = arrayListOf<Double>()
                            osmIds.add(entries.key)
                            foreign["osm_ids"] = osmIds
                            foreign["tileJoiner"] = 1
                            joining.foreign = foreign
                            joining.geometry = LineString(point1, entries.value[nearestPointIndex])
                            joining.properties = foreign

                            featureCollection.addFeature(joining)
                            deduplicateSet.add(point1)
                            deduplicateSet.add(entries.value[nearestPointIndex])
                            ++linesAdded
                            if(nearestPoint > highestSeparation) {
                                highestSeparation = nearestPoint
                                highestSeparationOsmId = entries.key
                            }
                        }
                        else {
//                            if(nearestPoint < 10.0) {
//                                println("Skipped separation ${entries.key.toLong()}: $nearestPoint")
//                            }
                        }
                    }
                }
                if((linesAdded +  perfectlyCoincident) != (entries.value.size / 2)) {
                    // The most likely reasons for this are that the line goes off grid at both ends
                    // or that it has some crossing points within the grid amd then goes off grid.
                    //println("Some points were not paired for ${entries.key.toLong()}: ($linesAdded + $perfectlyCoincident) / ${entries.value.size / 2}")
                }
            } else {
                // This is point must be on the outer edge or our grid, so we need do nothing
            }
        }
        val end = System.currentTimeMillis()
        println("Highest added separation $highestSeparation for OSM id ${highestSeparationOsmId.toLong()}")
        println("Interpolated points join time: ${end - start}ms for ${interpolatedPoints.size} points")
    }
}

/**
 * convertGeometryAndClipLineToTile takes a line and converts it into a List of LineStrings. In the
 * simplest case, the points are all within the tile and so there will just be a single LineString
 * output. However, if the line goes off and on the tile (bouncing around in the buffer region) then
 * there can be multiple segments returned.
 * We also store all of the interpolated points that we've been created so that we can more easily
 * connect them to the adjacent tiles in the grid.
 */
fun convertGeometryAndClipLineToTile(
    tileX: Int,
    tileY: Int,
    tileZoom: Int,
    line: ArrayList<Pair<Int, Int>>,
    interpolatedNodes: MutableList<LngLatAlt>
) : List<LineString> {
    val returnList = mutableListOf<LineString>()

    if(line.isEmpty()) {
        return returnList
    }

    // We want to iterate through the line detecting when it goes off/on tile and creating line
    // segments for each. The ends of the line as it goes off tile need to be in LatLng as we want
    // to interpolate as precisely as possible so that the line end is at the same point on adjacent
    // tiles. The only other thing to bear in mind is that it's possible for two points to be off
    // tile but the line between them to cross through the tile.
    var offTile = pointIsOffTile(line[0].first, line[0].second)
    val segment = arrayListOf<LngLatAlt>()
    var lastPoint = line[0]
    for(point in line) {
        if(pointIsOffTile(point.first, point.second) != offTile){
            if(offTile) {
                // We started off tile and this point is now on tile
                // Add interpolated point from lastPoint to this point
                val interpolatedPoint = getTileCrossingPoint(lastPoint, point)
                val interpolatedLatLon = getLatLonTileWithOffset(
                    tileX,
                    tileY,
                    tileZoom,
                    interpolatedPoint[0].first / 4096.0,
                    interpolatedPoint[0].second / 4096.0
                )
                segment.add(interpolatedLatLon)
                interpolatedNodes.add(interpolatedLatLon)

                // Add the new point
                segment.add(
                    getLatLonTileWithOffset(
                        tileX,
                        tileY,
                        tileZoom,
                        point.first.toDouble() / 4096.0,
                        point.second.toDouble() / 4096.0
                    )
                )
            } else {
                // We started on tile and this point is now off tile
                // Add interpolated point from lastPoint to this point
                val interpolatedPoint = getTileCrossingPoint(lastPoint, point)
                val interpolatedLatLon = getLatLonTileWithOffset(
                    tileX,
                    tileY,
                    tileZoom,
                    interpolatedPoint[0].first / 4096.0,
                    interpolatedPoint[0].second / 4096.0
                )

                segment.add(interpolatedLatLon)
                interpolatedNodes.add(interpolatedLatLon)
                returnList.add(LineString(ArrayList(segment)))
                segment.clear()
            }

            // Update the current point state
            offTile = offTile.xor(true)
        }
        else if(!offTile) {
            segment.add(
                getLatLonTileWithOffset(
                    tileX,
                    tileY,
                    tileZoom,
                    point.first.toDouble() / 4096.0,
                    point.second.toDouble() / 4096.0
                )
            )
        } else {
            // We're continuing off tile, but we need to check if the line between the two off tile
            // points crossed over the tile.
            val interpolatedPoints = getTileCrossingPoint(lastPoint, point)
            for(ip in interpolatedPoints) {
                val interpolatedLatLon = getLatLonTileWithOffset(
                    tileX,
                    tileY,
                    tileZoom,
                    ip.first / 4096.0,
                    ip.second / 4096.0
                )
                segment.add(interpolatedLatLon)
                interpolatedNodes.add(interpolatedLatLon)
            }
            if(segment.isNotEmpty()) {
                returnList.add(LineString(ArrayList(segment)))
                segment.clear()
            }
        }

        lastPoint = point
    }
    if(segment.isNotEmpty()) {
        returnList.add(LineString(segment))
    }
    return returnList
}

/** getTileCrossingPoint returns the point at which the line connecting lastPoint and point crosses
 * the tile boundary. If both points are outside the tile there can be two intersection points
 * returned. Otherwise there can only be a single intersection point.
 * @param point1 Point on line that might cross tile boundary
 * @param point2 Another point on the line that might cross the tile boundary
 *
 * @return The coordinates at which the line crosses the tile boundary as a list of pairs of Doubles
 * to give  us the best precision.
 */
fun getTileCrossingPoint(point1 : Pair<Int, Int>, point2 : Pair<Int, Int>) : List<Pair<Double, Double>> {

    // Extract the coordinates of the points and square boundaries
    val x1 = point1.first.toDouble()
    val y1 = point1.second.toDouble()
    val x2 = point2.first.toDouble()
    val y2 = point2.second.toDouble()

    val intersections = mutableListOf<Pair<Double, Double>>()

    // Check intersections with the four sides of the square

    // Left side (x = 0)
    intersectVertical(0.0, y1, y2, x1, x2)?.let { yIntersection ->
        if (yIntersection in 0.0..4096.0) {
            intersections.add(Pair(0.0, yIntersection))
        }
    }

    // Right side (x = 4096)
    intersectVertical(4096.0, y1, y2, x1, x2)?.let { yIntersection ->
        if (yIntersection in 0.0..4096.0) {
            intersections.add(Pair(4096.0, yIntersection))
        }
    }

    // Bottom side (y = 0.0)
    intersectHorizontal(0.0, x1, x2, y1, y2)?.let { xIntersection ->
        if (xIntersection in 0.0..4096.0) {
            intersections.add(Pair(xIntersection, 0.0))
        }
    }

    // Top side (y = 4096)
    intersectHorizontal(4096.0, x1, x2, y1, y2)?.let { xIntersection ->
        if (xIntersection in 0.0..4096.0) {
            intersections.add(Pair(xIntersection, 4096.0))
        }
    }

    // Return any intersections that we found
    return intersections
}

fun calculateSlope(aConst: Double, a1: Double, a2: Double) : Double? {
    if (a1 == a2) {
        // Parallel lines, so no intersection
        return null
    }
    val t = (aConst - a1) / (a2 - a1)
    if (t < 0.0 || t > 1.0) {
        // Intersection point is outside the segment
        return null
    }
    return t
}

// Function to calculate the intersection with a vertical line (x = constant)
fun intersectVertical(xConst: Double, y1: Double, y2: Double, x1: Double, x2: Double): Double? {
    val t = calculateSlope(xConst, x1, x2)
    if(t != null) {
        return y1 + t * (y2 - y1)
    }
    return null
}

// Function to calculate the intersection with a horizontal line (y = constant)
fun intersectHorizontal(yConst: Double, x1: Double, x2: Double, y1: Double, y2: Double): Double? {
    val t = calculateSlope(yConst, y1, y2)
    if(t != null) {
        return x1 + t * (x2 - x1)
    }
    return null
}