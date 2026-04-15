package org.scottishtecharmy.soundscape.rtree

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

sealed interface Geometry {
    fun mbr(): Rectangle
    fun intersects(r: Rectangle): Boolean
    fun distanceToPoint(px: Double, py: Double): Double
}

data class Point(val x: Double, val y: Double) : Geometry {
    override fun mbr(): Rectangle = Rectangle(x, y, x, y)
    override fun intersects(r: Rectangle): Boolean =
        x in r.x1..r.x2 && y in r.y1..r.y2
    override fun distanceToPoint(px: Double, py: Double): Double = hypot(px - x, py - y)
}

data class Line(val x1: Double, val y1: Double, val x2: Double, val y2: Double) : Geometry {
    override fun mbr(): Rectangle = Rectangle(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2))
    override fun intersects(r: Rectangle): Boolean {
        if (!mbr().intersects(r)) return false
        if (r.containsPoint(x1, y1) || r.containsPoint(x2, y2)) return true
        return segmentIntersects(x1, y1, x2, y2, r.x1, r.y1, r.x2, r.y1) ||
            segmentIntersects(x1, y1, x2, y2, r.x2, r.y1, r.x2, r.y2) ||
            segmentIntersects(x1, y1, x2, y2, r.x2, r.y2, r.x1, r.y2) ||
            segmentIntersects(x1, y1, x2, y2, r.x1, r.y2, r.x1, r.y1)
    }
    override fun distanceToPoint(px: Double, py: Double): Double {
        val dx = x2 - x1; val dy = y2 - y1
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else (((px - x1) * dx + (py - y1) * dy) / len2).coerceIn(0.0, 1.0)
        val cx = x1 + t * dx; val cy = y1 + t * dy
        return hypot(px - cx, py - cy)
    }
}

data class Rectangle(val x1: Double, val y1: Double, val x2: Double, val y2: Double) : Geometry {
    init {
        require(x1 <= x2 && y1 <= y2) { "Rectangle requires x1<=x2 and y1<=y2" }
    }
    override fun mbr(): Rectangle = this
    override fun intersects(r: Rectangle): Boolean =
        x1 <= r.x2 && r.x1 <= x2 && y1 <= r.y2 && r.y1 <= y2
    override fun distanceToPoint(px: Double, py: Double): Double {
        val dx = max(max(x1 - px, 0.0), px - x2)
        val dy = max(max(y1 - py, 0.0), py - y2)
        return hypot(dx, dy)
    }
    fun containsPoint(x: Double, y: Double): Boolean = x in x1..x2 && y in y1..y2
    fun union(r: Rectangle): Rectangle =
        Rectangle(min(x1, r.x1), min(y1, r.y1), max(x2, r.x2), max(y2, r.y2))
    val centerX: Double get() = (x1 + x2) * 0.5
    val centerY: Double get() = (y1 + y2) * 0.5

    companion object {
        fun of(geometries: List<Geometry>): Rectangle {
            require(geometries.isNotEmpty())
            var r = geometries[0].mbr()
            for (i in 1 until geometries.size) r = r.union(geometries[i].mbr())
            return r
        }
    }
}

private fun segmentIntersects(
    ax: Double, ay: Double, bx: Double, by: Double,
    cx: Double, cy: Double, dx: Double, dy: Double
): Boolean {
    val d1 = cross(cx, cy, dx, dy, ax, ay)
    val d2 = cross(cx, cy, dx, dy, bx, by)
    val d3 = cross(ax, ay, bx, by, cx, cy)
    val d4 = cross(ax, ay, bx, by, dx, dy)
    if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
        ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) return true
    return false
}

private fun cross(ox: Double, oy: Double, ax: Double, ay: Double, bx: Double, by: Double): Double =
    (ax - ox) * (by - oy) - (ay - oy) * (bx - ox)
