package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.test.Test
import kotlin.test.assertEquals

class CompassDirectionsTest {

    // --- normalizeHeading ---

    @Test
    fun normalizeHeadingLeavesInRangeValuesUnchanged() {
        assertEquals(0, normalizeHeading(0))
        assertEquals(180, normalizeHeading(180))
        assertEquals(359, normalizeHeading(359))
    }

    @Test
    fun normalizeHeadingKeeps360AsIs() {
        // Note: upper bound check is strictly ">" so 360 itself is left unreduced,
        // even though it is the same physical heading as 0.
        assertEquals(360, normalizeHeading(360))
    }

    @Test
    fun normalizeHeadingWrapsValuesAbove360() {
        assertEquals(360, normalizeHeading(720)) // 720 - 360 = 360, loop stops (360 is not > 360)
        assertEquals(1, normalizeHeading(721)) // 721 - 360 = 361 (still > 360) - 360 = 1
        assertEquals(45, normalizeHeading(765))
        assertEquals(40, normalizeHeading(400))
    }

    @Test
    fun normalizeHeadingWrapsNegativeValues() {
        assertEquals(350, normalizeHeading(-10))
        assertEquals(0, normalizeHeading(-360))
        assertEquals(350, normalizeHeading(-370))
        assertEquals(320, normalizeHeading(-400))
    }

    @Test
    fun normalizeHeadingHandlesZeroAndNegativeZero() {
        assertEquals(0, normalizeHeading(-0))
    }

    // --- getCompassLabel ---

    @Test
    fun getCompassLabelCardinalCenters() {
        assertEquals(StringKey.DirectionsCardinalNorth, getCompassLabel(0))
        assertEquals(StringKey.DirectionsCardinalNorthEast, getCompassLabel(45))
        assertEquals(StringKey.DirectionsCardinalEast, getCompassLabel(90))
        assertEquals(StringKey.DirectionsCardinalSouthEast, getCompassLabel(135))
        assertEquals(StringKey.DirectionsCardinalSouth, getCompassLabel(180))
        assertEquals(StringKey.DirectionsCardinalSouthWest, getCompassLabel(225))
        assertEquals(StringKey.DirectionsCardinalWest, getCompassLabel(270))
        assertEquals(StringKey.DirectionsCardinalNorthWest, getCompassLabel(315))
    }

    @Test
    fun getCompassLabelNorthWraparoundBoundaries() {
        // North octant spans 338..360 and 0..22
        assertEquals(StringKey.DirectionsCardinalNorth, getCompassLabel(338))
        assertEquals(StringKey.DirectionsCardinalNorth, getCompassLabel(360))
        assertEquals(StringKey.DirectionsCardinalNorth, getCompassLabel(22))
        // Just outside the octant on either side
        assertEquals(StringKey.DirectionsCardinalNorthWest, getCompassLabel(337))
        assertEquals(StringKey.DirectionsCardinalNorthEast, getCompassLabel(23))
    }

    @Test
    fun getCompassLabelOctantBoundaries() {
        assertEquals(StringKey.DirectionsCardinalNorthEast, getCompassLabel(23))
        assertEquals(StringKey.DirectionsCardinalNorthEast, getCompassLabel(67))
        assertEquals(StringKey.DirectionsCardinalEast, getCompassLabel(68))
        assertEquals(StringKey.DirectionsCardinalEast, getCompassLabel(112))
        assertEquals(StringKey.DirectionsCardinalSouthEast, getCompassLabel(113))
        assertEquals(StringKey.DirectionsCardinalSouthEast, getCompassLabel(157))
        assertEquals(StringKey.DirectionsCardinalSouth, getCompassLabel(158))
        assertEquals(StringKey.DirectionsCardinalSouth, getCompassLabel(202))
        assertEquals(StringKey.DirectionsCardinalSouthWest, getCompassLabel(203))
        assertEquals(StringKey.DirectionsCardinalSouthWest, getCompassLabel(247))
        assertEquals(StringKey.DirectionsCardinalWest, getCompassLabel(248))
        assertEquals(StringKey.DirectionsCardinalWest, getCompassLabel(292))
        assertEquals(StringKey.DirectionsCardinalNorthWest, getCompassLabel(293))
        assertEquals(StringKey.DirectionsCardinalNorthWest, getCompassLabel(337))
    }

    @Test
    fun getCompassLabelHandlesNegativeAndOverflowDegrees() {
        // -10 normalizes to 350, which is in the North octant.
        assertEquals(StringKey.DirectionsCardinalNorth, getCompassLabel(-10))
        // 765 normalizes to 45 (NorthEast).
        assertEquals(StringKey.DirectionsCardinalNorthEast, getCompassLabel(765))
    }

    // --- getRelativeClockTime ---

    @Test
    fun getRelativeClockTimeSameHeadingIsTwelve() {
        assertEquals(12, getRelativeClockTime(0, 0))
        assertEquals(12, getRelativeClockTime(90, 90))
    }

    @Test
    fun getRelativeClockTimeCardinalOffsets() {
        assertEquals(3, getRelativeClockTime(90, 0))
        assertEquals(6, getRelativeClockTime(180, 0))
        assertEquals(9, getRelativeClockTime(270, 0))
        assertEquals(12, getRelativeClockTime(360, 0))
    }

    @Test
    fun getRelativeClockTimeHandlesWraparoundSubtraction() {
        // degrees behind userDegrees, crossing the 0/360 boundary.
        // relative = normalizeHeading(10 - 350) = normalizeHeading(-340) = 20 -> bucket [15,45) -> hour 1
        assertEquals(1, getRelativeClockTime(10, 350))
    }

    @Test
    fun getRelativeClockTimeBucketBoundaries() {
        // Buckets are 30 degrees wide, centered on multiples of 30, with rounding at the
        // +15 offset boundary.
        assertEquals(12, getRelativeClockTime(14, 0)) // relative=14 -> still hour 12
        assertEquals(1, getRelativeClockTime(15, 0))  // relative=15 -> rolls over to hour 1
        assertEquals(11, getRelativeClockTime(344, 0)) // relative=344 -> hour 11
        assertEquals(12, getRelativeClockTime(345, 0)) // relative=345 -> hour 12
    }

    @Test
    fun getRelativeClockTimeAllTwelveHours() {
        for (hour in 1..12) {
            val relative = (hour % 12) * 30
            assertEquals(hour, getRelativeClockTime(relative, 0), "hour=$hour relative=$relative")
        }
    }

    // --- getRelativeLeftRightLabel ---

    @Test
    fun getRelativeLeftRightLabelCardinalCenters() {
        assertEquals(StringKey.RelativeLeftRightDirectionAhead, getRelativeLeftRightLabel(0))
        assertEquals(StringKey.RelativeLeftRightDirectionAheadRight, getRelativeLeftRightLabel(45))
        assertEquals(StringKey.RelativeLeftRightDirectionRight, getRelativeLeftRightLabel(90))
        assertEquals(StringKey.RelativeLeftRightDirectionBehindRight, getRelativeLeftRightLabel(135))
        assertEquals(StringKey.RelativeLeftRightDirectionBehind, getRelativeLeftRightLabel(180))
        assertEquals(StringKey.RelativeLeftRightDirectionBehindLeft, getRelativeLeftRightLabel(225))
        assertEquals(StringKey.RelativeLeftRightDirectionLeft, getRelativeLeftRightLabel(270))
        assertEquals(StringKey.RelativeLeftRightDirectionAheadLeft, getRelativeLeftRightLabel(315))
    }

    @Test
    fun getRelativeLeftRightLabelAheadWraparoundBoundaries() {
        assertEquals(StringKey.RelativeLeftRightDirectionAhead, getRelativeLeftRightLabel(338))
        assertEquals(StringKey.RelativeLeftRightDirectionAhead, getRelativeLeftRightLabel(360))
        assertEquals(StringKey.RelativeLeftRightDirectionAhead, getRelativeLeftRightLabel(0))
        assertEquals(StringKey.RelativeLeftRightDirectionAhead, getRelativeLeftRightLabel(22))
        assertEquals(StringKey.RelativeLeftRightDirectionAheadLeft, getRelativeLeftRightLabel(337))
        assertEquals(StringKey.RelativeLeftRightDirectionAheadRight, getRelativeLeftRightLabel(23))
    }

    @Test
    fun getRelativeLeftRightLabelHandlesNegativeAngles() {
        // -45 normalizes to 315 -> AheadLeft
        assertEquals(StringKey.RelativeLeftRightDirectionAheadLeft, getRelativeLeftRightLabel(-45))
        // -90 normalizes to 270 -> Left
        assertEquals(StringKey.RelativeLeftRightDirectionLeft, getRelativeLeftRightLabel(-90))
    }
}
