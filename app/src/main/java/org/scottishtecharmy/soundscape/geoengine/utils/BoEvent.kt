package org.scottishtecharmy.soundscape.geoengine.utils

import java.util.ArrayList

class BoEvent(
    private var point: BoPoint,
    private val segments: ArrayList<BoSegment>,
    private var type: Int
) {

    var value: Double = point.x_coord

    constructor(p: BoPoint, s: BoSegment, type: Int) : this(p, arrayListOf(s), type)

    fun addPoint(p: BoPoint) {
        this.point = p
    }

    fun getPoint(): BoPoint {
        return this.point
    }

    fun addSegment(s: BoSegment) {
        this.segments.add(s)
    }

    fun getSegments(): ArrayList<BoSegment> {
        return this.segments
    }

    fun setType(type: Int) {
        this.type = type
    }

    fun getType(): Int {
        return this.type
    }

    fun set_Value(value: Double) {
        this.value = value
    }

    fun get_Value(): Double {
        return this.value
    }
}