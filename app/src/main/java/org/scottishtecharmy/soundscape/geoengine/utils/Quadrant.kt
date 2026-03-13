package org.scottishtecharmy.soundscape.geoengine.utils

open class Segment(heading: Double, width: Double) {

    val left: Double = (heading + 360-(width/2)) % 360.0
    val right: Double = (heading + width/2) % 360.0

    fun contains(headingToCheck: Double): Boolean {
        val wrappedHeading = headingToCheck % 360.0

        return if (wrappedHeading in left..<right) {
            true
        } else right < left && (wrappedHeading !in right..<left)
    }
}

class Quadrant(heading: Double) : Segment(heading, 90.0)