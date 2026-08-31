package org.scottishtecharmy.soundscape.filters

import org.scottishtecharmy.soundscape.geoengine.filters.RailMatchArbiter
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The case that prompted all this: driving the M90 past Winchburgh, the rail matcher locks onto the
 * Winchburgh Chord running 35-70m away and the driver is told "On Winchburgh Chord". The road match
 * is sitting a couple of metres away the whole time, which is what should settle it.
 */
class RailMatchArbiterTest {

    private val railway = Way().apply {
        osmId = 1L
        name = "Winchburgh Chord"
    }

    private fun road(distance: Double?, confident: Boolean = true) =
        RailMatchArbiter.MatchState(
            Way().apply { osmId = 2L; name = "M90" },
            distance,
            confident,
        )

    private fun rail(distance: Double?, confident: Boolean = true) =
        RailMatchArbiter.MatchState(railway, distance, confident)

    private fun noMatch() = RailMatchArbiter.MatchState(null, null, false)

    @Test
    fun testRailwayBesideTheRoadIsNeverATrain() {
        val arbiter = RailMatchArbiter()
        // Far longer than the ~60 fixes the real M90 stretch lasts.
        repeat(200) {
            assertNull(
                arbiter.update(road(3.0), rail(45.0)),
                "A railway 45m away must never beat a road 3m away",
            )
        }
    }

    @Test
    fun testTrainIsAcquiredOnlyAfterASustainedRun() {
        val arbiter = RailMatchArbiter()
        // Nine passing updates isn't enough - acquiring takes ten.
        repeat(9) {
            assertNull(arbiter.update(road(40.0), rail(2.0)), "Should not acquire this early")
        }
        assertEquals(railway, arbiter.update(road(40.0), rail(2.0)))
    }

    @Test
    fun testRailStandsAloneWhenThereIsNoRoadMatch() {
        val arbiter = RailMatchArbiter()
        repeat(9) { assertNull(arbiter.update(noMatch(), rail(2.0))) }
        assertEquals(
            railway,
            arbiter.update(noMatch(), rail(2.0)),
            "With no road to compare against, a confident rail match should stand on its own",
        )
    }

    @Test
    fun testBriefRoadDropoutDoesNotAcquireATrain() {
        val arbiter = RailMatchArbiter()
        // This is the shape of the Winchburgh failure: on a road the whole time, but the road match
        // drops out for a few updates at a junction while a railway runs alongside.
        repeat(20) { arbiter.update(road(3.0), rail(45.0)) }
        repeat(5) {
            assertNull(
                arbiter.update(noMatch(), rail(45.0)),
                "A junction-length road dropout must not be enough to become a train",
            )
        }
        assertNull(arbiter.update(road(3.0), rail(45.0)))
    }

    @Test
    fun testAcquiredTrainSurvivesAShortDropoutButReleasesOnASustainedOne() {
        val arbiter = RailMatchArbiter()
        repeat(10) { arbiter.update(noMatch(), rail(2.0)) }
        assertEquals(railway, arbiter.update(noMatch(), rail(2.0)), "Should be on a train by now")

        // A line running briefly beside a road shouldn't flip the callouts back and forth.
        repeat(5) {
            assertEquals(
                railway,
                arbiter.update(road(1.0), rail(30.0)),
                "A short spell of the road matching better should be ridden out",
            )
        }
        // Sustained, though, and we're not on a train any more.
        assertNull(arbiter.update(road(1.0), rail(30.0)))
    }

    @Test
    fun testLosingRailConfidenceEndsIt() {
        val arbiter = RailMatchArbiter()
        repeat(11) { arbiter.update(noMatch(), rail(2.0)) }
        repeat(6) { arbiter.update(noMatch(), rail(2.0, confident = false)) }
        assertNull(
            arbiter.update(noMatch(), rail(2.0, confident = false)),
            "No confident rail match means no train",
        )
    }
}
