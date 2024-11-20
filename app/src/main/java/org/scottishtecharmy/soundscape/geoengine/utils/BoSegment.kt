package org.scottishtecharmy.soundscape.geoengine.utils

class BoSegment(
    private val p1: BoPoint,
    private val p2: BoPoint
) {
    var value: Double

    init {
        this.value = calculateValue(first().x_coord)
    }

    fun first(): BoPoint {
        return if (p1.x_coord <= p2.x_coord) {
            p1
        } else {
            p2
        }
    }

    fun second(): BoPoint {
        return if (p1.x_coord <= p2.x_coord) {
            p2
        } else {
            p1
        }
    }

    fun calculateValue(value: Double): Double {
        val x1 = first().x_coord
        val x2 = second().x_coord
        val y1 = first().y_coord
        val y2 = second().y_coord
        return y1 + (((y2 - y1) / (x2 - x1)) * (value - x1))
    }

    fun set_Value(value: Double) {
        this.value = value
    }

    fun get_Value(): Double {
        return this.value
    }
}