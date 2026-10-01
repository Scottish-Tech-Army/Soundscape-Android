package org.scottishtecharmy.soundscape.filters

import org.scottishtecharmy.soundscape.geoengine.filters.TravelHeadingEstimator
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TravelHeadingEstimatorTest {

    private val origin = LngLatAlt(-4.2244, 55.8649)
    private val ruler = CheapRuler(origin.latitude)

    /** Metres east/north of [origin]. */
    private fun at(east: Double, north: Double) = ruler.offset(origin, east, north)

    /** Walks south at a metre a second for [seconds], ending at time [seconds] * 1000. */
    private fun TravelHeadingEstimator.walkSouth(seconds: Int) {
        for (second in 0..seconds) addFix(at(0.0, -second.toDouble()), second * 1000L)
    }

    private fun TravelHeadingEstimator.estimate(
        bearing: Double?,
        bearingAccuracy: Double?,
        roadHeading: Double?,
        now: Long = 0L,
    ) = estimate(bearing, bearingAccuracy, roadHeading, ruler, now)

    @Test
    fun testATrustedCourseIsTakenAsItIs() {
        val estimator = TravelHeadingEstimator()
        assertEquals(170.0, estimator.estimate(170.0, 20.0, roadHeading = 0.0))
        // No accuracy reported at all is trusted too, as it always has been.
        assertEquals(170.0, estimator.estimate(170.0, null, roadHeading = null))
    }

    @Test
    fun testAPoorerCourseAlongTheRoadIsBelieved() {
        val estimator = TravelHeadingEstimator()
        // A road running north-south, its heading given either way round. The course is taken as
        // it is: the road only vouches for it.
        assertEquals(150.0, estimator.estimate(150.0, 60.0, roadHeading = 0.0))
        assertEquals(150.0, estimator.estimate(150.0, 60.0, roadHeading = 180.0))
        assertEquals(40.0, estimator.estimate(40.0, 80.0, roadHeading = 0.0))
    }

    @Test
    fun testAPoorerCourseNeedsARoadToCheckItAgainst() {
        val estimator = TravelHeadingEstimator()
        assertNull(estimator.estimate(150.0, 60.0, roadHeading = null))
    }

    @Test
    fun testAPoorerCourseAcrossTheRoadIsNotBelieved() {
        // 90° off the road could be either direction along it - or turning off it.
        val estimator = TravelHeadingEstimator()
        assertNull(estimator.estimate(90.0, 60.0, roadHeading = 0.0))
    }

    @Test
    fun testAStationaryIosCourseIsNeverTaken() {
        // iOS reports 180° while standing still, 90° once rescaled.
        val estimator = TravelHeadingEstimator()
        assertNull(estimator.estimate(10.0, 90.0, roadHeading = 0.0))
    }

    @Test
    fun testMovementGivesTheDirectionWhenTheCourseCannot() {
        val estimator = TravelHeadingEstimator()
        estimator.walkSouth(10)
        val heading =
            assertNotNull(estimator.estimate(null, null, roadHeading = null, now = 10_000L))
        assertTrue(abs180(heading - 180.0) < 1.0, "heading was $heading")
    }

    @Test
    fun testMovementIsTakenAsItIs() {
        // Whatever the road does - the user may be turning off it.
        val estimator = TravelHeadingEstimator()
        estimator.walkSouth(10)
        val heading =
            assertNotNull(estimator.estimate(null, null, roadHeading = 90.0, now = 10_000L))
        assertTrue(abs180(heading - 180.0) < 1.0, "heading was $heading")
    }

    @Test
    fun testAPoorCourseFallsBackToMovementRatherThanNothing() {
        val estimator = TravelHeadingEstimator()
        estimator.walkSouth(10)
        // A 60° course across an east-west road - no use - but the user is visibly walking south.
        val heading =
            assertNotNull(estimator.estimate(0.0, 60.0, roadHeading = 90.0, now = 10_000L))
        assertTrue(abs180(heading - 180.0) < 1.0, "heading was $heading")
    }

    @Test
    fun testTooLittleMovementSaysNothing() {
        val estimator = TravelHeadingEstimator()
        estimator.walkSouth(3)
        assertNull(estimator.estimate(null, null, roadHeading = null, now = 3_000L))
    }

    @Test
    fun testOldMovementSaysNothing() {
        val estimator = TravelHeadingEstimator()
        estimator.walkSouth(10)
        assertNull(
            estimator.estimate(
                null, null, roadHeading = null,
                now = 10_000L + TravelHeadingEstimator.MOVEMENT_WINDOW_MILLISECONDS + 1
            )
        )
    }

    @Test
    fun testMovementUsesTheMostRecentStretch() {
        // South for ten metres, then east: the heading is east, not some average of the two.
        val estimator = TravelHeadingEstimator()
        estimator.walkSouth(10)
        for (second in 1..6) {
            estimator.addFix(at(second.toDouble(), -10.0), 10_000L + second * 1000L)
        }
        val heading =
            assertNotNull(estimator.estimate(null, null, roadHeading = null, now = 16_000L))
        assertTrue(abs180(heading - 90.0) < 1.0, "heading was $heading")
    }

    /** How far [degrees] is from zero, either way round. */
    private fun abs180(degrees: Double): Double = abs(((degrees % 360.0) + 540.0) % 360.0 - 180.0)
}
