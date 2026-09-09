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

    private fun rail(distance: Double?, confident: Boolean = true, nearAStop: Boolean = false) =
        RailMatchArbiter.MatchState(railway, distance, confident, nearAStop = nearAStop)

    /** The line within reach of somewhere the passenger could actually have got off. */
    private fun railAtAStop(distance: Double?, confident: Boolean = true) =
        rail(distance, confident, nearAStop = true)

    private val tunnel = Way().apply {
        osmId = 3L
        name = "North Clyde Line"
        properties = hashMapOf("brunnel" to "tunnel")
    }

    private fun railTunnel(distance: Double?, confident: Boolean = true) =
        RailMatchArbiter.MatchState(tunnel, distance, confident, inTunnel = true)

    private fun noMatch() = RailMatchArbiter.MatchState(null, null, false)

    /** Line speed, and the speed of the motorway traffic beside it. */
    private val travelling = 31.0

    /** Stopped at a platform, or walking away from one. */
    private val stopped = 0.5

    @Test
    fun testRailwayBesideTheRoadIsNeverATrain() {
        val arbiter = RailMatchArbiter()
        // Far longer than the ~60 fixes the real M90 stretch lasts.
        repeat(200) {
            assertNull(
                arbiter.update(road(3.0), rail(45.0), travelling),
                "A railway 45m away must never beat a road 3m away",
            )
        }
    }

    @Test
    fun testTrainIsAcquiredOnlyAfterASustainedRun() {
        val arbiter = RailMatchArbiter()
        // Nine passing updates isn't enough - acquiring takes ten.
        repeat(9) {
            assertNull(
                arbiter.update(road(40.0), rail(2.0), travelling),
                "Should not acquire this early",
            )
        }
        assertEquals(railway, arbiter.update(road(40.0), rail(2.0), travelling))
    }

    @Test
    fun testRailStandsAloneWhenThereIsNoRoadMatch() {
        val arbiter = RailMatchArbiter()
        repeat(9) { assertNull(arbiter.update(noMatch(), rail(2.0), travelling)) }
        assertEquals(
            railway,
            arbiter.update(noMatch(), rail(2.0), travelling),
            "With no road to compare against, a confident rail match should stand on its own",
        )
    }

    @Test
    fun testBriefRoadDropoutDoesNotAcquireATrain() {
        val arbiter = RailMatchArbiter()
        // This is the shape of the Winchburgh failure: on a road the whole time, but the road match
        // drops out for a few updates at a junction while a railway runs alongside.
        repeat(20) { arbiter.update(road(3.0), rail(45.0), travelling) }
        repeat(5) {
            assertNull(
                arbiter.update(noMatch(), rail(45.0), travelling),
                "A junction-length road dropout must not be enough to become a train",
            )
        }
        assertNull(arbiter.update(road(3.0), rail(45.0), travelling))
    }

    @Test
    fun testAcquiredTrainSurvivesAShortDropoutButReleasesOnASustainedOne() {
        val arbiter = RailMatchArbiter()
        repeat(10) { arbiter.update(noMatch(), rail(2.0), travelling) }
        assertEquals(
            railway,
            arbiter.update(noMatch(), rail(2.0), travelling),
            "Should be on a train by now",
        )

        // A line running briefly beside a road shouldn't flip the callouts back and forth.
        repeat(5) {
            assertEquals(
                railway,
                arbiter.update(road(1.0), railAtAStop(30.0), stopped),
                "A short spell of the road matching better should be ridden out",
            )
        }
        // Sustained, though, and we're not on a train any more.
        assertNull(arbiter.update(road(1.0), railAtAStop(30.0), stopped))
    }

    @Test
    fun testLosingRailConfidenceEndsIt() {
        val arbiter = RailMatchArbiter()
        repeat(11) { arbiter.update(noMatch(), rail(2.0), travelling) }
        repeat(6) { arbiter.update(noMatch(), rail(2.0, confident = false), travelling) }
        assertNull(
            arbiter.update(noMatch(), rail(2.0, confident = false), travelling),
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
                arbiter.update(road(5.0), railTunnel(2.0), travelling),
                "A tunnel under the road must never acquire a train lock, however good the match",
            )
        }
        // Even with no road match at all to weigh against - the point is that the lock has to be
        // earned above ground, not that the road wins the comparison.
        repeat(200) {
            assertNull(
                arbiter.update(noMatch(), railTunnel(2.0), travelling),
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
        repeat(10) { arbiter.update(noMatch(), rail(2.0), travelling) }
        assertEquals(
            railway,
            arbiter.update(noMatch(), rail(2.0), travelling),
            "Should be on a train by now",
        )

        // Underground the road overhead is consistently the nearer of the two, which under the
        // ordinary comparison would release the lock after releaseTicks.
        repeat(50) {
            assertEquals(
                tunnel,
                arbiter.update(road(1.0), railTunnel(8.0), travelling),
                "A train already underway must stay on its line through a tunnel",
            )
        }
    }

    /**
     * The Argyle Line out of Partick runs under a dead-end service road through Yorkhill Park, and
     * for thirteen consecutive fixes at 40mph the road match sat nearer the track than the line did
     * - 0.2m at its closest, against the line's 5-9m. Under the old comparison that ended the ride
     * at Kelvinhaugh Street, and since the line went into the Finnieston Tunnel immediately
     * afterwards - where a lock can never be acquired - the rest of the journey through Exhibition
     * Centre went with it.
     */
    @Test
    fun testARoadRunningOverTheLineDoesNotEndTheRide() {
        val arbiter = RailMatchArbiter()
        repeat(10) { arbiter.update(noMatch(), rail(2.0), travelling) }
        assertEquals(
            railway,
            arbiter.update(noMatch(), rail(2.0), travelling),
            "Should be on a train by now",
        )

        // The road matcher's own distances through Yorkhill Park, against the line's.
        val roadDistances = listOf(4.9, 4.0, 3.3, 3.3, 1.7, 0.7, 0.3, 0.5, 0.2, 1.6, 3.2, 5.1, 7.0)
        val railDistances = listOf(5.6, 5.4, 5.6, 6.0, 6.3, 6.5, 7.7, 8.7, 9.2, 9.4, 9.0, 8.0, 7.0)
        for ((roadDistance, railDistance) in roadDistances.zip(railDistances)) {
            assertEquals(
                railway,
                arbiter.update(road(roadDistance), rail(railDistance), travelling),
                "A fix ${railDistance}m from the line is on the train, whatever runs over it",
            )
        }
    }

    /**
     * Off the railway at a station, though, the road gets its say again - this is how the ride ends
     * for someone who got off and walked or drove away along a road beside the line.
     */
    @Test
    fun testWanderingOffTheLineAtAStopEndsTheRide() {
        val arbiter = RailMatchArbiter()
        repeat(11) { arbiter.update(noMatch(), rail(2.0), travelling) }

        repeat(5) { arbiter.update(road(4.0), railAtAStop(40.0), stopped) }
        assertNull(
            arbiter.update(road(4.0), railAtAStop(40.0), stopped),
            "A fix 40m from the line and 4m from a road, at a stopped train, is on the road",
        )
    }

    /**
     * The same fix nowhere near a stop is still on the train, because there is nowhere to have got
     * off. This is the M6-beside-the-West-Coast-Main-Line shape, and the reason it doesn't reopen
     * that bug is that a drive like it can never acquire the lock in the first place - see
     * testRailwayBesideTheRoadIsNeverATrain.
     */
    @Test
    fun testWanderingOffTheLineBetweenStopsDoesNotEndTheRide() {
        val arbiter = RailMatchArbiter()
        repeat(11) { arbiter.update(noMatch(), rail(2.0), travelling) }

        repeat(100) {
            assertEquals(
                railway,
                arbiter.update(road(4.0), rail(40.0), stopped),
                "There is no way off a train between stops, however well the road fits",
            )
        }
    }

    /**
     * And the same fix at a stop, but still travelling, is still on the train: nobody steps off a
     * moving train onto a moving bus. This is what carried the ride through Partick, where the line
     * passes the platforms at speed and the road matcher had the station approach roads to offer.
     */
    @Test
    fun testPassingThroughAStopAtSpeedDoesNotEndTheRide() {
        val arbiter = RailMatchArbiter()
        repeat(11) { arbiter.update(noMatch(), rail(2.0), travelling) }

        repeat(100) {
            assertEquals(
                railway,
                arbiter.update(road(4.0), railAtAStop(40.0), travelling),
                "A train running through a station at line speed is nobody's alighting point",
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
        repeat(10) { arbiter.update(noMatch(), rail(2.0), travelling) }
        repeat(10) { arbiter.update(road(1.0), railTunnel(8.0), travelling) }


        repeat(5) { arbiter.update(road(1.0), railTunnel(8.0, confident = false), travelling) }
        assertNull(
            arbiter.update(road(1.0), railTunnel(8.0, confident = false), travelling),
            "Walking away from the line underground should end the train journey",
        )
    }
}
