package org.scottishtecharmy.soundscape.filters

import org.scottishtecharmy.soundscape.geoengine.filters.OnFootDetector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnFootDetectorTest {

    /** A vehicle at speed up to t=0, then [fix] for each second, returning when it first said so. */
    private fun firstOnFoot(
        seconds: Int = 120,
        detector: OnFootDetector = OnFootDetector(),
        fix: (Int) -> Pair<Double, Boolean>,
    ): Int? {
        detector.update(10.0, true, 0L)
        for (second in 1..seconds) {
            val (speed, course) = fix(second)
            if (detector.update(speed, course, second * 1000L)) return second
        }
        return null
    }

    @Test
    fun walkingStraightAwayIsSpottedAtTwentySeconds() {
        assertEquals(20, firstOnFoot { 1.4 to true })
    }

    @Test
    fun walkingAfterAWaitIsSpottedWithinAFewSeconds() {
        assertEquals(46, firstOnFoot { if (it > 40) 1.4 to true else 0.0 to false })
    }

    @Test
    fun standingStillIsNeverOnFoot() {
        assertNull(firstOnFoot { 1.3 to false })
    }

    /** The course a vehicle has when it brakes is exactly what the grace period is there to skip. */
    @Test
    fun brakingIsNotWalking() {
        assertNull(firstOnFoot { if (it < 14) 3.0 to true else 0.0 to false })
    }

    /** As bad as the recordings get: seven trustworthy fixes in ten, on and off, in a queue. */
    @Test
    fun aVehicleInAQueueIsNotWalking() {
        assertNull(firstOnFoot { 1.0 to (it % 3 != 0) })
    }

    @Test
    fun pullingAwayIsNotWalking() {
        assertNull(firstOnFoot { if (it > 30) 4.0 to true else 0.0 to false })
    }

    @Test
    fun fixesTooSparseToSayGetTheLongerWait() {
        assertNull(firstOnFoot { if (it % 5 == 0) 1.4 to true else 1.4 to false })
    }

    @Test
    fun aBreakInTheCourseDoesNotUndoTheVerdict() {
        val detector = OnFootDetector()
        assertEquals(20, firstOnFoot(detector = detector) { 1.4 to true })
        assertTrue(detector.update(1.4, false, 30_000L))
    }

    @Test
    fun gettingBackInAVehicleEndsTheVerdict() {
        val detector = OnFootDetector()
        assertEquals(20, firstOnFoot(detector = detector) { 1.4 to true })
        assertFalse(detector.update(10.0, true, 30_000L))
        assertFalse(detector.update(1.4, true, 31_000L))
    }

    @Test
    fun walkingWithNoVehicleBeforeItIsNotThisDetectorsBusiness() {
        val detector = OnFootDetector()
        for (second in 1..60) assertFalse(detector.update(1.4, true, second * 1000L))
    }
}
