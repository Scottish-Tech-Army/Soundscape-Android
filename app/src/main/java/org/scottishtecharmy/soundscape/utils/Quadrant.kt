package org.scottishtecharmy.soundscape.utils

class Quadrant(private val heading: Double) {
    val left: Double
    val right: Double

    init {
        left = (heading + 315) % 360.0
        right = (heading + 45) % 360.0
    }

    fun contains(headingToCheck: Double): Boolean {
        val wrappedHeading = headingToCheck % 360.0

        return if (wrappedHeading >= left && wrappedHeading < right) {
            true
        } else right < left && (wrappedHeading >= left || wrappedHeading < right)
    }
}