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
        estimator.walkSouth(20)
        val heading =
            assertNotNull(estimator.estimate(null, null, roadHeading = null, now = 20_000L))
        assertTrue(abs180(heading - 180.0) < 1.0, "heading was $heading")
    }

    @Test
    fun testMovementIsTakenAsItIs() {
        // Whatever the road does - the user may be turning off it.
        val estimator = TravelHeadingEstimator()
        estimator.walkSouth(20)
        val heading =
            assertNotNull(estimator.estimate(null, null, roadHeading = 90.0, now = 20_000L))
        assertTrue(abs180(heading - 180.0) < 1.0, "heading was $heading")
    }

    @Test
    fun testAPoorCourseFallsBackToMovementRatherThanNothing() {
        val estimator = TravelHeadingEstimator()
        estimator.walkSouth(20)
        // A 60° course across an east-west road - no use - but the user is visibly walking south.
        val heading =
            assertNotNull(estimator.estimate(0.0, 60.0, roadHeading = 90.0, now = 20_000L))
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
        estimator.walkSouth(20)
        assertNull(
            estimator.estimate(
                null, null, roadHeading = null,
                now = 20_000L + TravelHeadingEstimator.MOVEMENT_WINDOW_MILLISECONDS + 1
            )
        )
    }

    @Test
    fun testMovementUsesTheMostRecentStretch() {
        // South for twenty metres, then east for fifteen: the heading is east.
        val estimator = TravelHeadingEstimator()
        estimator.walkSouth(20)
        for (second in 1..15) {
            estimator.addFix(at(second.toDouble(), -20.0), 20_000L + second * 1000L)
        }
        val heading =
            assertNotNull(estimator.estimate(null, null, roadHeading = null, now = 35_000L))
        assertTrue(abs180(heading - 90.0) < 1.0, "heading was $heading")
    }

    @Test
    fun testNothingJustAfterATurn() {
        // Six metres past a right-angled turn the last five metres say east, but the path over
        // the last fifteen isn't straight enough to trust it yet.
        val estimator = TravelHeadingEstimator()
        estimator.walkSouth(20)
        for (second in 1..6) {
            estimator.addFix(at(second.toDouble(), -20.0), 20_000L + second * 1000L)
        }
        assertNull(estimator.estimate(null, null, roadHeading = null, now = 26_000L))
    }

    @Test
    fun testWanderSaysNothing() {
        // Fixes jumping about one spot, a few metres at a time: more than fifteen metres of it
        // lies within reach of the newest, but along a path three times that long, so no
        // direction.
        val estimator = TravelHeadingEstimator()
        val wander = listOf(
            0.0 to 0.0, 4.0 to 4.0, 0.0 to 4.0, 0.0 to 0.0, -4.0 to -3.0, -2.0 to 0.0,
            -6.0 to 2.0, -9.0 to 2.0, -7.0 to -3.0, -1.0 to -2.0, -3.0 to 1.0, -2.0 to 6.0,
            -4.0 to 10.0, 0.0 to 11.0, 2.0 to 7.0, 6.0 to 7.0, 10.0 to 8.0, 6.0 to 10.0,
            3.0 to 8.0, 6.0 to 11.0, 1.0 to 10.0,
        )
        wander.forEachIndexed { second, (east, north) ->
            estimator.addFix(at(east, north), second * 1000L)
        }
        assertNull(
            estimator.estimate(null, null, roadHeading = null, now = (wander.size - 1) * 1000L)
        )
    }

    /** How far [degrees] is from zero, either way round. */
    private fun abs180(degrees: Double): Double = abs(((degrees % 360.0) + 540.0) % 360.0 - 180.0)
}
