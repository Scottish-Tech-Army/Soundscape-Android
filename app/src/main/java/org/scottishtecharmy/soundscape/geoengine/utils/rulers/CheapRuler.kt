package org.scottishtecharmy.soundscape.geoengine.utils.rulers

import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** This code is a Kotlin port of the mapbox/cheap-ruler JavaScript code. The original code is under
    this ISC license, reproduced here for ease:

    ISC License

    Copyright (c) 2024, Mapbox

    Permission to use, copy, modify, and/or distribute this software for any purpose
    with or without fee is hereby granted, provided that the above copyright notice
    and this permission notice appear in all copies.

    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
    REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
    FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
    INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
    OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
    TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
    THIS SOFTWARE.
*/

const val meters = 1000.0
//const val kilometers = 1.0
//const val miles = 1000.0 / 1609.344
//const val nauticalMiles = 1000.0 / 1852.0
//const val yards = 1000.0 / 0.9144
//const val feet = 1000.0 / 0.3048
//const val inches = 1000.0 / 0.0254

// Values that define WGS84 ellipsoid model of the Earth
private const val RE = 6378.137
private const val FE = 1 / 298.257223563
private const val E2 = FE * (2 - FE)
private const val RAD = PI / 180

/**
 * normalize a degree value into [-180..180] range
 * @param {number} deg
 */
fun wrap(deg: Double) : Double {
    var tmp = deg
    while (deg < -180.0) tmp += 360.0
    while (deg > 180.0) tmp -= 360.0

    return tmp
}

/**
 * A collection of very fast approximations to common geodesic measurements. Useful for
 * performance-sensitive code that measures things on a city scale.
 *
 * Creates a ruler instance for very fast approximations to common geodesic measurements around a certain latitude.
 *
 * @param {number} lat latitude
 * @example
 * const ruler = cheapRuler(35.05)
 */
class CheapRuler(val lat: Double) : Ruler() {

    val kx: Double
    val ky: Double

    init {
        // Curvature formulas from https://en.wikipedia.org/wiki/Earth_radius#Meridional
        val m = RAD * RE * (meters)
        val cosLat = cos(lat * RAD)
        val w2 = 1 / (1 - E2 * (1 - cosLat * cosLat))
        val w = sqrt(w2)

        // multipliers for converting longitude and latitude degrees into distance
        kx = m * w * cosLat         // based on normal radius of curvature
        ky = m * w * w2 * (1 - E2)  // based on meridional radius of curvature
    }

    fun needsReplacing(newLat: Double) : Boolean {
        // If the latitude changes by more than 0.01 degrees, then create a new ruler to maintain
        // accuracy of calculations.
        return (abs(lat - newLat) > 0.01)
    }

    /**
     * Given two LngLatAlt returns the distance between them
     *
     * @param a a point as a LngLatAlt
     * @param b another point as a LngLatAlt
     * @returns {number} distance between points
     */
    override fun distance(a: LngLatAlt, b: LngLatAlt) : Double {
        val dx = wrap(a.longitude - b.longitude) * kx
        val dy = (a.latitude - b.latitude) * ky

        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Returns the bearing between two points
     *
     * @param a a point as a LngLatAlt
     * @param b another point as a LngLatAlt
     * @returns {number} bearing from point a to point b
     */
    override fun bearing(a: LngLatAlt, b: LngLatAlt) : Double {
        val dx = wrap(b.longitude - a.longitude) * kx
        val dy = (b.latitude - a.latitude) * ky

        return atan2(dx, dy) / RAD
    }

    /**
     * Returns a new point given distance and bearing from the starting point.
     *
     * @param p starting point LngLatAlt
     * @param {number} dist distance
     * @param {number} bearing
     * @returns New point LngLatAlt
     * @example
     * const point = ruler.destination([30.5, 50.5], 0.1, 90);
     * //=point
     */
    override fun destination(p: LngLatAlt, dist: Double, bearing: Double) : LngLatAlt {
        val a = bearing * RAD
        return offset(
            p,
            sin(a) * dist,
            cos(a) * dist
        )
    }

    /**
     * Returns a new point given easting and northing offsets (in ruler units) from the starting point.
     *
     * @param {[number, number]} p point [longitude, latitude]
     * @param {number} dx easting
     * @param {number} dy northing
     * @returns {[number, number]} point [longitude, latitude]
     * @example
     * const point = ruler.offset([30.5, 50.5], 10, 10);
     * //=point
     */
    fun offset(p: LngLatAlt, dx: Double, dy: Double) : LngLatAlt {
        return LngLatAlt(
            p.longitude + dx / kx,
            p.latitude + dy / ky
        )
    }

    /**
     * Given a line (an array of points), returns the total line distance.
     *
     * @param {[number, number][]} points [longitude, latitude]
     * @returns {number} total line distance
     * @example
     * const length = ruler.lineDistance([
     *     [-67.031, 50.458], [-67.031, 50.534],
     *     [-66.929, 50.534], [-66.929, 50.458]
     * ]);
     * //=length
     */
//    fun lineDistance(points) {
//        let total = 0;
//        for (let i = 0; i < points.length - 1; i++) {
//            total += this.distance(points[i], points[i + 1]);
//        }
//        return total;
//    }

    /**
     * Given a polygon (an array of rings, where each ring is an array of points), returns the area.
     *
     * @param {[number, number][][]} polygon
     * @returns {number} area value in the specified units (square kilometers by default)
     * @example
     * const area = ruler.area([[
     *     [-67.031, 50.458], [-67.031, 50.534], [-66.929, 50.534],
     *     [-66.929, 50.458], [-67.031, 50.458]
     * ]]);
     * //=area
     */
//    area(polygon) {
//        let sum = 0;
//
//        for (let i = 0; i < polygon.length; i++) {
//            const ring = polygon[i];
//
//            for (let j = 0, len = ring.length, k = len - 1; j < len; k = j++) {
//            sum += wrap(ring[j][0] - ring[k][0]) * (ring[j][1] + ring[k][1]) * (i ? -1 : 1);
//        }
//        }
//
//        return (abs(sum) / 2) * this.kx * this.ky;
//    }

    /**
     * Returns the point at a specified distance along the line.
     *
     * @param {[number, number][]} line
     * @param {number} dist distance
     * @returns {[number, number]} point [longitude, latitude]
     * @example
     * const point = ruler.along(line, 2.5);
     * //=point
     */
    override fun along(line: LineString, dist: Double) : LngLatAlt {
        var sum = 0.0

        if (dist <= 0.0) return line.coordinates[0]

        for (i in 0 until line.coordinates.size - 1) {
            val p0 = line.coordinates[i];
            val p1 = line.coordinates[i + 1];
            val d = this.distance(p0, p1);
            sum += d;
            if (sum > dist) return cheapInterpolate(p0, p1, (dist - (sum - d)) / d);
        }

        return line.coordinates[line.coordinates.size - 1];
    }

    /**
     * Returns the distance from a point `p` to a line segment `a` to `b`.
     *
     * @pointToSegmentDistance
     * @param {[number, number]} p point [longitude, latitude]
     * @param {[number, number]} a segment point 1 [longitude, latitude]
     * @param {[number, number]} b segment point 2 [longitude, latitude]
     * @returns {number} distance
     * @example
     * const distance = ruler.pointToSegmentDistance([-67.04, 50.5], [-67.05, 50.57], [-67.03, 50.54]);
     * //=distance
     */
    override fun pointToSegmentDistance(p: LngLatAlt, a: LngLatAlt, b: LngLatAlt) : Double {
        var x = a.longitude
        var y = a.latitude
        var dx = wrap(b.longitude - x) * this.kx
        var dy = (b.latitude - y) * this.ky

        if (dx != 0.0 || dy != 0.0) {
            val t = (wrap(p.longitude - x) * this.kx * dx + (p.latitude - y) * this.ky * dy) / (dx * dx + dy * dy)

            if (t > 1) {
                x = b.longitude
                y = b.latitude

            } else if (t > 0) {
                x += (dx / this.kx) * t
                y += (dy / this.ky) * t
            }
        }

        dx = wrap(p.longitude - x) * this.kx;
        dy = (p.latitude - y) * this.ky;

        return sqrt(dx * dx + dy * dy);
    }

    /**
     * Returns an object of the form {point, index, t}, where point is closest point on the line
     * from the given point, index is the start index of the segment with the closest point,
     * and t is a parameter from 0 to 1 that indicates where the closest point is on that segment.
     *
     * @param {[number, number][]} line
     * @param {[number, number]} p point [longitude, latitude]
     * @returns {{point: [number, number], index: number, t: number}} {point, index, t}
     * @example
     * const point = ruler.pointOnLine(line, [-67.04, 50.5]).point;
     * //=point
     */
    override fun distanceToLineString(p: LngLatAlt, line: LineString) : PointAndDistanceAndHeading {
        var minDist = Double.MAX_VALUE
        var minX = line.coordinates[0].longitude
        var minY = line.coordinates[0].latitude
        var minI = 0
        var minT = 0.0

        for(i in 0 until line.coordinates.size - 1) {

            var x = line.coordinates[i].longitude
            var y = line.coordinates[i].latitude
            var dx = wrap(line.coordinates[i+1].longitude - x) * kx
            var dy = (line.coordinates[i+1].latitude - y) * ky
            var t = 0.0

            if (dx != 0.0 || dy != 0.0) {
                t = ((wrap(p.longitude - x) * kx * dx) + ((p.latitude - y) * ky * dy)) / ((dx * dx) + (dy * dy))

                if (t > 1.0) {
                    x = line.coordinates[i+1].longitude
                    y = line.coordinates[i+1].latitude

                } else if (t > 0) {
                    x += (dx / kx) * t
                    y += (dy / ky) * t
                }
            }

            dx = wrap(p.longitude - x) * kx
            dy = (p.latitude - y) * ky

            val sqDist = (dx * dx) + (dy * dy)
            if (sqDist < minDist) {
                minDist = sqDist
                minX = x
                minY = y
                minI = i
                minT = t
            }
        }

        val nearestPoint = cheapInterpolate(line.coordinates[minI], line.coordinates[minI+1], max(0.0, min(1.0, minT)))
        return PointAndDistanceAndHeading(
            nearestPoint,
            this.distance(p, nearestPoint),
            bearingFromTwoPoints(line.coordinates[minI], line.coordinates[minI + 1]),
            minI,
            minI.toDouble() + max(0.0, min(1.0, minT))
        )
    }

    /**
     * Returns a part of the given line between the start and the stop points (or their closest points on the line).
     *
     * @param {[number, number]} start point [longitude, latitude]
     * @param {[number, number]} stop point [longitude, latitude]
     * @param {[number, number][]} line
     * @returns {[number, number][]} line part of a line
     * @example
     * const line2 = ruler.lineSlice([-67.04, 50.5], [-67.05, 50.56], line1);
     * //=line2
     */
//    lineSlice(start, stop, line) {
//        let p1 = this.pointOnLine(line, start);
//        let p2 = this.pointOnLine(line, stop);
//
//        if (p1.index > p2.index || (p1.index === p2.index && p1.t > p2.t)) {
//            const tmp = p1;
//            p1 = p2;
//            p2 = tmp;
//        }
//
//        const slice = [p1.point];
//
//        const l = p1.index + 1;
//        const r = p2.index;
//
//        if (!equals(line[l], slice[0]) && l <= r)
//            slice.push(line[l]);
//
//        for (let i = l + 1; i <= r; i++) {
//            slice.push(line[i]);
//        }
//
//        if (!equals(line[r], p2.point))
//            slice.push(p2.point);
//
//        return slice;
//    }

    /**
     * Returns a part of the given line between the start and the stop points indicated by distance along the line.
     *
     * @param {number} start start distance
     * @param {number} stop stop distance
     * @param {[number, number][]} line
     * @returns {[number, number][]} part of a line
     * @example
     * const line2 = ruler.lineSliceAlong(10, 20, line1);
     * //=line2
     */
//    lineSliceAlong(start, stop, line) {
//        let sum = 0;
//        const slice = [];
//
//        for (let i = 0; i < line.length - 1; i++) {
//            const p0 = line[i];
//            const p1 = line[i + 1];
//            const d = this.distance(p0, p1);
//
//            sum += d;
//
//            if (sum > start && slice.length === 0) {
//                slice.push(cheapInterpolate(p0, p1, (start - (sum - d)) / d));
//            }
//
//            if (sum >= stop) {
//                slice.push(cheapInterpolate(p0, p1, (stop - (sum - d)) / d));
//                return slice;
//            }
//
//            if (sum > start) slice.push(p1);
//        }
//
//        return slice;
//    }

    /**
     * Given a point, returns a bounding box object ([w, s, e, n]) created from the given point buffered by a given distance.
     *
     * @param {[number, number]} p point [longitude, latitude]
     * @param {number} buffer
     * @returns {[number, number, number, number]} bbox ([w, s, e, n])
     * @example
     * const bbox = ruler.bufferPoint([30.5, 50.5], 0.01);
     * //=bbox
     */
//    bufferPoint(p, buffer) {
//        const v = buffer / this.ky;
//        const h = buffer / this.kx;
//        return [
//            p[0] - h,
//            p[1] - v,
//            p[0] + h,
//            p[1] + v
//        ];
//    }

    /**
     * Given a bounding box, returns the box buffered by a given distance.
     *
     * @param {[number, number, number, number]} bbox ([w, s, e, n])
     * @param {number} buffer
     * @returns {[number, number, number, number]} bbox ([w, s, e, n])
     * @example
     * const bbox = ruler.bufferBBox([30.5, 50.5, 31, 51], 0.2);
     * //=bbox
     */
//    bufferBBox(bbox, buffer) {
//        const v = buffer / this.ky;
//        const h = buffer / this.kx;
//        return [
//            bbox[0] - h,
//            bbox[1] - v,
//            bbox[2] + h,
//            bbox[3] + v
//        ];
//    }

    /**
     * Returns true if the given point is inside in the given bounding box, otherwise false.
     *
     * @param {[number, number]} p point [longitude, latitude]
     * @param {[number, number, number, number]} bbox ([w, s, e, n])
     * @returns {boolean}
     * @example
     * const inside = ruler.insideBBox([30.5, 50.5], [30, 50, 31, 51]);
     * //=inside
     */
//    insideBBox(p, bbox) { // eslint-disable-line
//        return wrap(p[0] - bbox[0]) >= 0 &&
//                wrap(p[0] - bbox[2]) <= 0 &&
//                p[1] >= bbox[1] &&
//                p[1] <= bbox[3];
//    }
}

/**
 * @param {[number, number]} a
 * @param {[number, number]} b
 */
//function equals(a, b) {
//    return a[0] === b[0] && a[1] === b[1];
//}

/**
 * @param {[number, number]} a
 * @param {[number, number]} b
 * @param {number} t
 * @returns {[number, number]}
 */
fun cheapInterpolate(a: LngLatAlt, b: LngLatAlt, t: Double) : LngLatAlt {
    val dx = wrap(b.longitude - a.longitude)
    val dy = b.latitude - a.latitude
    return LngLatAlt(a.longitude + (dx * t), a.latitude + (dy * t))
}
