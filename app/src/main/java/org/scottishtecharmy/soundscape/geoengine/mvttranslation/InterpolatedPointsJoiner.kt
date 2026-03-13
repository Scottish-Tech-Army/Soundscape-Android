package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

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
                        sampleToFractionOfTile(point.first),
                        sampleToFractionOfTile(point.second)
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
                    sampleToFractionOfTile(point.first),
                    sampleToFractionOfTile(point.second)
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
    val x1 = point1.first.toDouble() + 0.5
    val y1 = point1.second.toDouble() + 0.5
    val x2 = point2.first.toDouble() + 0.5
    val y2 = point2.second.toDouble() + 0.5

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
    if (t !in 0.0..1.0) {
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