package org.scottishtecharmy.soundscape.geoengine.utils.rulers

import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class GeodesicRuler() : Ruler() {

    override fun distance(a: LngLatAlt, b: LngLatAlt) : Double {
        return org.scottishtecharmy.soundscape.geoengine.utils.distance(a.latitude, a.longitude, b.latitude, b.longitude)
    }

    override fun bearing(a: LngLatAlt, b: LngLatAlt) : Double {
        return  bearingFromTwoPoints(a, b)
    }

    /**
     * Returns a new point given distance and bearing from the starting point.
     *
     * @param {[number, number]} p point [longitude, latitude]
     * @param {number} dist distance
     * @param {number} bearing
     * @returns {[number, number]} point [longitude, latitude]
     * @example
     * const point = ruler.destination([30.5, 50.5], 0.1, 90);
     * //=point
     */
    override fun destination(p: LngLatAlt, dist: Double, bearing: Double) : LngLatAlt {

        return getDestinationCoordinate(p, bearing, dist)
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
    override fun lineLength(line: LineString) : Double {
        var total = 0.0

        for (i in 0 until line.coordinates.size - 1) {
            val p0 = line.coordinates[i]
            val p1 = line.coordinates[i + 1]
            val d = distance(p0, p1)
            total += d
        }

        return total
    }

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
        // TODO: implement
        assert(false)
        return LngLatAlt()
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
        // TODO: implement
        assert(false)
        return 0.0
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
        // TODO: implement
        assert(false)
        return PointAndDistanceAndHeading()
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
