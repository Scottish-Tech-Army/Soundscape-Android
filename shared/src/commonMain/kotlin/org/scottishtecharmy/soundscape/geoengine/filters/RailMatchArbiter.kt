package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way

/**
 * Decides whether a confident railway map-match really means the user is on a train, by weighing it
 * against the road match from the same location update.
 *
 * Roads and railways are matched by two entirely independent [MapMatchFilter]s (see its networkTree
 * parameter), and the rail one used to be trusted on its own. That assumed road and rail geometry
 * never coincide for long, which simply isn't true: motorways are routinely built alongside railway
 * lines for kilometres. On the M90 past Winchburgh the recorded track runs 35-70m from the
 * Winchburgh Chord for around sixty consecutive fixes at 70mph, which is inside the rail follower's
 * own DISTANT threshold at that speed (max(30.0, pointGap * 1.5), so roughly 45m when fixes are 30m
 * apart) and long enough to build the sustained history isMatchConfident needs. The driver was
 * announced as being "On Winchburgh Chord". The same thing happened on the M6 alongside the West
 * Coast Main Line, and on a road passing *underneath* a chord in Glasgow.
 *
 * So a rail match has to earn it twice over:
 *
 *  - it must beat the road match. Driving on a road, the road match sits within a few metres while
 *    the parallel railway is tens of metres away, which settles it outright. A rail match only
 *    stands unchallenged when there's no confident road match to compare against.
 *  - it must hold up. A brief road-match dropout - crossing a junction, say, which is exactly where
 *    the Winchburgh callout fired - would otherwise hand the decision straight to the railway.
 *
 * Both matchers have their own internal hysteresis, but neither can see the other, so this is the
 * only place the comparison can be made. Deliberately not folded into MapMatchFilter: the matchers
 * are correct in isolation, and each is separately useful.
 */
class RailMatchArbiter {

    /**
     * What one matcher had to say about a single location update. [distance] is how far the fix was
     * from the matched way; null when there's nothing matched. [inTunnel] is only meaningful for the
     * rail side, where it decides whether the match may acquire a train lock or only sustain one.
     */
    data class MatchState(
        val way: Way?,
        val distance: Double?,
        val confident: Boolean,
        val inTunnel: Boolean = false,
    )

    /**
     * How many consecutive updates the rail match has to out-perform the road match before the user
     * is treated as being on a train. At roughly one location update per second this is ~10s, which
     * comfortably outlasts a junction-length gap in the road match (MapMatchFilter's own confidence
     * grace is GRACE_TICKS_AFTER_LOSING_CONFIDENCE, 5 ticks) without noticeably delaying a real
     * journey - a train trip produces callouts for many minutes, and travel callouts are rate
     * limited to one per 10s/50m anyway.
     */
    private val acquireTicks = 10

    /**
     * Once we've decided the user is on a train, how many consecutive failing updates to tolerate
     * before giving that up. Mirrors MapMatchFilter's own grace window: on a real journey the road
     * matcher can briefly acquire something as the line runs beside a road, and dropping out of
     * train mode for a tick or two would flip the callouts back and forth.
     */
    private val releaseTicks = 5

    private var consecutivePasses = 0
    private var ticksSinceLastPass = 0
    private var onTrain = false

    /**
     * Call once per location update, after both filters have been run for that location. Returns
     * the railway [Way] to treat the user as travelling on, or null if they're not on a train.
     */
    fun update(road: MapMatchFilter, rail: MapMatchFilter): Way? =
        update(road.matchState(), rail.matchState())

    fun update(road: MatchState, rail: MatchState): Way? {
        val railway = rail.way.takeIf { rail.confident }
        if (railway == null) {
            fail()
            return null
        }

        // A rail tunnel can keep a train ride going, but must never start one. This is the same
        // road-above-the-line hazard that used to keep tunnels out of TreeId.TRANSIT altogether
        // (see isUnmatchableRailway in MvtToGeoJson.kt): Kent Road runs directly over the North
        // Clyde Line at Charing Cross, so a bus on it matches the tunnel below just as well as it
        // matches the road, and would otherwise be announced as being on a train. Requiring the
        // lock to be earned on track that's actually above ground rules that out however long the
        // road runs over the tunnel.
        if (rail.inTunnel && !onTrain) {
            fail()
            return null
        }

        // Once on a train the problem inverts. Underground the road overhead is routinely *nearer*
        // the fix than the line is - measured through the Charing Cross tunnel, 0.1-11m to Kent
        // Road against 0.3-8m to the tunnel centreline - so railBeatsRoad flips back and forth
        // tick by tick and would drop the lock inside releaseTicks, handing the callouts straight
        // back to the road above. While the matched line is a tunnel the road simply isn't a
        // credible alternative, so hold the lock rather than weighing the two.
        if (rail.inTunnel) {
            consecutivePasses++
            ticksSinceLastPass = 0
            return railway
        }

        if (!railBeatsRoad(road, rail)) {
            fail()
            // Keep reporting the railway through a short dropout, but only if we'd already decided
            // the user was on a train - never as a way of acquiring it.
            return if (onTrain) railway else null
        }

        consecutivePasses++
        ticksSinceLastPass = 0
        if (consecutivePasses >= acquireTicks) {
            onTrain = true
        }
        return if (onTrain) railway else null
    }

    /**
     * Whether the railway is a better explanation of where the user is than the road. With no
     * confident road match there's nothing to weigh it against, so the railway stands on its own.
     */
    private fun railBeatsRoad(road: MatchState, rail: MatchState): Boolean {
        if (!road.confident) return true
        val roadDistance = road.distance ?: return true
        val railDistance = rail.distance ?: return false
        return railDistance < roadDistance
    }

    private fun fail() {
        consecutivePasses = 0
        ticksSinceLastPass++
        if (ticksSinceLastPass > releaseTicks) {
            onTrain = false
        }
    }
}

private fun MapMatchFilter.matchState() =
    RailMatchArbiter.MatchState(
        matchedWay,
        matchedLocation?.distance,
        isMatchConfident,
        matchedWay?.properties?.get("brunnel") == "tunnel",
    )
