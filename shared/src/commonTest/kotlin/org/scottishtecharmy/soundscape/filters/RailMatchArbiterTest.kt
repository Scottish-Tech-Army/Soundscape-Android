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

    private val tunnel = Way().apply {
        osmId = 3L
        name = "North Clyde Line"
        properties = hashMapOf("brunnel" to "tunnel")
    }

    private fun railTunnel(distance: Double?, confident: Boolean = true) =
        RailMatchArbiter.MatchState(tunnel, distance, confident, inTunnel = true)

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

    /**
     * Kent Road runs directly over the North Clyde Line where it tunnels under Charing Cross, so a
     * bus on it matches the tunnel below about as well as it matches the road - measured 0.1-11m to
     * the road against 0.3-8m to the tunnel centreline. However long that goes on for, and however
     * often the rail match happens to come out nearer, it must never make the passenger a train
     * rider.
     */
    @Test
    fun testTunnelUnderneathTheRoadNeverAcquiresATrain() {
        val arbiter = RailMatchArbiter()
        repeat(200) {
            assertNull(
                arbiter.update(road(5.0), railTunnel(2.0)),
                "A tunnel under the road must never acquire a train lock, however good the match",
            )
        }
        // Even with no road match at all to weigh against - the point is that the lock has to be
        // earned above ground, not that the road wins the comparison.
        repeat(200) {
            assertNull(
                arbiter.update(noMatch(), railTunnel(2.0)),
                "A tunnel match must not acquire even when there's no road to compare against",
            )
        }
    }

    /**
     * The other half of the same rule: once the lock has been earned on surface track, going
     * underground has to keep it. Without this the train through Charing Cross was handed back to
     * Kent Road within releaseTicks and announced as "Traveling east along Kent Road".
     */
    @Test
    fun testTunnelSustainsATrainAcquiredAboveGround() {
        val arbiter = RailMatchArbiter()
        repeat(10) { arbiter.update(noMatch(), rail(2.0)) }
        assertEquals(railway, arbiter.update(noMatch(), rail(2.0)), "Should be on a train by now")

        // Underground the road overhead is consistently the nearer of the two, which under the
        // ordinary comparison would release the lock after releaseTicks.
        repeat(50) {
            assertEquals(
                tunnel,
                arbiter.update(road(1.0), railTunnel(8.0)),
                "A train already underway must stay on its line through a tunnel",
            )
        }
    }

    /**
     * Getting off underground still ends the journey: what sustains the lock is the rail match
     * itself, so losing it releases in the ordinary way.
     */
    @Test
    fun testLosingTheRailMatchInATunnelStillReleases() {
        val arbiter = RailMatchArbiter()
        repeat(10) { arbiter.update(noMatch(), rail(2.0)) }
        repeat(10) { arbiter.update(road(1.0), railTunnel(8.0)) }

        repeat(5) { arbiter.update(road(1.0), railTunnel(8.0, confident = false)) }
        assertNull(
            arbiter.update(road(1.0), railTunnel(8.0, confident = false)),
            "Walking away from the line underground should end the train journey",
        )
    }
}
