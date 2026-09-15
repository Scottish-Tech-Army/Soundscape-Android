package org.scottishtecharmy.soundscape.filters

import org.scottishtecharmy.soundscape.geoengine.filters.StationaryDetector
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The numbers these tests are built from are measurements, not invented: they come from four real
 * recorded journeys (TransferAtQueenStreet, ToWaverley, FromWaverley, ToMilngavie), taken over 60s
 * windows with fixes accurate to 25m or better. Standing still, net displacement across the window
 * reaches 19.4m at the 90th percentile; walking, it starts at 66.4m. The jitter offsets below are
 * fixed rather than randomly generated so a failure is always the same failure.
 */
class StationaryDetectorTest {

    private val origin = LngLatAlt(-4.2518, 55.8642)
    private val ruler = CheapRuler(origin.latitude)

    /** Metres east/north of [origin], which is all these tests need to talk about. */
    private fun at(east: Double, north: Double) = ruler.offset(origin, east, north)

    /**
     * A repeating jitter cloud scaled so that consecutive fixes sit a realistic distance apart and
     * the whole cloud stays inside [radius] of the origin.
     */
    private fun jitter(step: Int, radius: Double): LngLatAlt {
        val pattern = listOf(
            0.0 to 0.0, 0.7 to 0.3, -0.4 to 0.8, 0.9 to -0.6, -0.8 to -0.5,
            0.2 to 0.9, -0.9 to 0.1, 0.5 to -0.9, -0.2 to -0.8, 0.8 to 0.6,
        )
        val (x, y) = pattern[step % pattern.size]
        return at(x * radius, y * radius)
    }

    /** Feeds [count] fixes a second apart, returning the verdict after the last one. */
    private fun StationaryDetector.run(
        count: Int,
        startMillis: Long,
        accuracy: Double? = 10.0,
        course: Boolean = false,
        position: (Int) -> LngLatAlt,
    ): Boolean {
        var stationary = false
        for (i in 0 until count) {
            stationary = update(position(i), accuracy, course, startMillis + i * 1000L)
        }
        return stationary
    }

    @Test
    fun testStandingStillIsOnlyDecidedOnceTheMinuteIsUp() {
        val detector = StationaryDetector()

        assertFalse(detector.run(30, 0L) { jitter(it, 8.0) }, "not decided after 30s")
        assertFalse(detector.run(29, 30_000L) { jitter(it, 8.0) }, "not decided after 59s")
        assertTrue(detector.run(2, 59_000L) { jitter(it, 8.0) }, "decided once the minute is up")
    }

    @Test
    fun testAMinuteOfWalkingIsNeverStill() {
        // 1.3 m/s is the measured median walking pace, 1.1 m/s the 10th percentile - the slowest
        // walk in the recordings, and so the hardest case to tell from standing about.
        for (pace in listOf(1.3, 1.1)) {
            val detector = StationaryDetector()
            for (second in 0 until 300) {
                val stationary = detector.update(
                    at(0.0, second * pace), 10.0, false, second * 1000L
                )
                assertFalse(stationary, "walking at $pace m/s read as stationary at ${second}s")
            }
        }
    }

    @Test
    fun testStandingStillSurvivesTheWorstMeasuredJitter() {
        // A cloud sized to the 90th-percentile stationary window - the Queen Street concourse.
        val detector = StationaryDetector()
        assertTrue(detector.run(61, 0L) { jitter(it, 9.7) })
        for (second in 61 until 600) {
            val stationary = detector.update(jitter(second, 9.7), 10.0, false, second * 1000L)
            assertTrue(stationary, "flipped out of stationary at ${second}s")
        }
    }

    @Test
    fun testWalkingAwayEndsIt() {
        val detector = StationaryDetector()
        assertTrue(detector.run(121, 0L) { jitter(it, 8.0) })

        // Walking away at the median pace, with no trustworthy course to escape on - so this is
        // purely the displacement test, the slow path.
        var endedAt: Int? = null
        for (second in 0 until 60) {
            val stationary = detector.update(
                at(0.0, second * 1.3), 10.0, false, 121_000L + second * 1000L
            )
            if (!stationary) { endedAt = second; break }
        }
        assertTrue(endedAt != null && endedAt <= 30, "still stationary after ${endedAt}s of walking")
    }

    @Test
    fun testATrustworthyCourseEndsItFaster() {
        val detector = StationaryDetector()
        assertTrue(detector.run(121, 0L) { jitter(it, 8.0) })

        // Two fixes carrying a course Android itself rates accurate is enough, which is what makes
        // this the fast path - it doesn't wait for thirty metres to be walked.
        assertTrue(detector.update(jitter(0, 8.0), 10.0, true, 122_000L), "one course is not enough")
        assertFalse(detector.update(jitter(1, 8.0), 10.0, true, 123_000L), "two courses ends it")
    }

    @Test
    fun testATrustworthyCourseCannotMakeSomebodyStationary() {
        // The escape only ever ends a stationary spell. Walking with a good course throughout must
        // not be read as standing still just because the course is steady.
        val detector = StationaryDetector()
        for (second in 0 until 300) {
            val stationary = detector.update(at(0.0, second * 1.3), 10.0, true, second * 1000L)
            assertFalse(stationary, "walking with a trustworthy course read as stationary")
        }
    }

    @Test
    fun testPoorFixesAreNotEvidence() {
        // 40m fixes pass the geoengine's own 50m gate but say nothing about a 20m question.
        val detector = StationaryDetector()
        assertFalse(detector.run(300, 0L, accuracy = 40.0) { jitter(it, 8.0) })
    }

    @Test
    fun testAFixWithoutAccuracyStillCounts() {
        // Street Preview and hand-written GPX carry no accuracy, and must still get a verdict.
        val detector = StationaryDetector()
        assertTrue(detector.run(61, 0L, accuracy = null) { jitter(it, 8.0) })
    }

    @Test
    fun testAGapInTheRecordStartsAgain() {
        val detector = StationaryDetector()
        assertTrue(detector.run(121, 0L) { jitter(it, 8.0) })

        // Five minutes later, three metres away. That pair is not evidence that anybody stood
        // there for five minutes.
        assertFalse(detector.update(at(3.0, 0.0), 10.0, false, 421_000L))
        assertFalse(detector.run(30, 422_000L) { jitter(it, 8.0) }, "30s in, still undecided")
        assertTrue(detector.run(31, 452_000L) { jitter(it, 8.0) }, "decided a full minute later")
    }

    @Test
    fun testThereAndBackIsNotStandingStill() {
        // Forty metres to a departure board and forty back inside the window. Net displacement
        // alone cannot see this; the excursion limit can.
        val detector = StationaryDetector()
        var stationary = false
        for (second in 0 until 61) {
            val north = if (second <= 30) second * 1.3 else (60 - second) * 1.3
            stationary = detector.update(at(0.0, north), 10.0, false, second * 1000L)
        }
        assertFalse(stationary, "an out-and-back read as standing still")
    }

    @Test
    fun testStationaryMillisCountsOnlyObservedStillness() {
        val detector = StationaryDetector()
        // Two minutes of standing, of which only the time after the first minute - once the window
        // had decided - is credited.
        detector.run(121, 0L) { jitter(it, 8.0) }
        assertTrue(detector.stationaryMillis in 55_000L..65_000L,
            "expected about 60s of observed stillness, got ${detector.stationaryMillis}")

        // A gap is not stillness, however still the fixes either side of it are.
        val before = detector.stationaryMillis
        detector.update(at(3.0, 0.0), 10.0, false, 421_000L)
        assertEquals(before, detector.stationaryMillis, "a gap must not be credited as stillness")
    }

    @Test
    fun testOutOfOrderFixesAreIgnored() {
        val detector = StationaryDetector()
        assertTrue(detector.run(61, 0L) { jitter(it, 8.0) })
        val stillness = detector.stationaryMillis
        assertTrue(detector.update(jitter(0, 8.0), 10.0, false, 30_000L), "a step back in time")
        assertEquals(stillness, detector.stationaryMillis)
    }

    @Test
    fun testDisplacementIsOnlyReportedOnceDecided() {
        val detector = StationaryDetector()
        detector.run(30, 0L) { jitter(it, 8.0) }
        assertNull(detector.displacementMetres, "no displacement before the window fills")
        detector.run(31, 30_000L) { jitter(it, 8.0) }
        assertTrue((detector.displacementMetres ?: 999.0) < 20.0)
    }
}
