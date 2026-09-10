package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayKind
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.WayContinuation
import org.scottishtecharmy.soundscape.geoengine.utils.WayCursor
import org.scottishtecharmy.soundscape.geoengine.utils.nextAlongWayFeature
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

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
 * So a rail match has to earn the lock twice over:
 *
 *  - it must beat the road match. Driving on a road, the road match sits within a few metres while
 *    the parallel railway is tens of metres away, which settles it outright. A rail match only
 *    stands unchallenged when there's no confident road match to compare against.
 *  - it must hold up. A brief road-match dropout - crossing a junction, say, which is exactly where
 *    the Winchburgh callout fired - would otherwise hand the decision straight to the railway.
 *
 * Keeping the lock is a different question from earning it, and asking the same one twice is what
 * used to end train rides half way. Comparing distances tick by tick assumes the road can only be
 * nearer than the line if the user is on it, and in a city that's simply untrue: on the Argyle Line
 * out of Partick the recorded track runs under a dead-end service road through Yorkhill Park, and
 * for thirteen consecutive fixes at 40mph the road match came out nearer than the line - 0.2m at
 * its closest, against the line's 5-9m. The line had been matched continuously for the best part
 * of a kilometre by then and the road for two ticks, yet the road won, the ride ended at
 * Kelvinhaugh Street, and the passenger was told they were travelling along a service road - and
 * then, because [acquireTicks] never came round again before the line dropped underground and the
 * tunnel rule below blocks re-acquisition, heard nothing about the Finnieston Tunnel or Exhibition
 * Centre either.
 *
 * So once the lock is earned, what keeps it is what actually keeps a passenger on a train: there is
 * nowhere to get off, and no way to do it. A ride can only end where the line has a stop and the
 * train has slowed enough to be got off at - nobody steps from a moving train onto a moving bus -
 * so anywhere else the road isn't weighed against the railway at all, however near it comes. Where
 * the ride really could be ending the comparison resumes, and even there a fix still sitting on the
 * track, within [onTheLineDistanceMetres], is explained by the line rather than by whatever runs
 * over the station. This is the argument the tunnel case already made for itself, applied to the
 * open line as well.
 *
 * Both matchers have their own internal hysteresis, but neither can see the other, so this is the
 * only place the comparison can be made. Deliberately not folded into MapMatchFilter: the matchers
 * are correct in isolation, and each is separately useful.
 */
class RailMatchArbiter {

    /**
     * What one matcher had to say about a single location update. [distance] is how far the fix was
     * from the matched way; null when there's nothing matched.
     *
     * [inTunnel] and [nearAStop] are only meaningful for the rail side: the first decides whether
     * the match may acquire a train lock or only sustain one, the second whether the ride is
     * anywhere it could end.
     */
    data class MatchState(
        val way: Way?,
        val distance: Double?,
        val confident: Boolean,
        val inTunnel: Boolean = false,
        val nearAStop: Boolean = false,
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

    /**
     * How near the line the fix has to stay, within reach of a stop, for the railway to go on
     * explaining it by itself. A train is on the track, so its fixes sit within a few metres of the
     * centreline even in a city - through Yorkhill and Kelvinhaugh the Argyle Line match ran 3-10m,
     * and the two tracks of a double line are themselves about 3m either side of the one that gets
     * matched. Beyond this the fix isn't on the railway at all, and whatever the road matcher says
     * deserves a hearing: the M90 alongside the Winchburgh Chord, the case this class exists for,
     * measured 35-70m out.
     */
    private val onTheLineDistanceMetres = 15.0

    /**
     * How near a stop on the line, measured along the rails, counts as somewhere the ride could
     * end. Generous enough to cover the length of a platform, a train that has overshot it and the
     * GPS error of a station in a cutting, and still far shorter than the gap between stops even on
     * an urban line - Partick to Exhibition Centre, the two either side of the Kelvinhaugh failure,
     * are about 1.5km apart, so nothing in between is within reach of either.
     *
     * The same walk the station callouts use (see stationAtDistanceMetres in GeoEngineHelpers),
     * measured along the line rather than as the crow flies: what matters is whether the train
     * could have stopped here, not what happens to be nearby on another line.
     */
    private val stopWithinReachMetres = 250.0

    private var consecutivePasses = 0
    private var ticksSinceLastPass = 0
    private var onTrain = false

    /**
     * Call once per location update, after both filters have been run for that location. Returns
     * the railway [Way] to treat the user as travelling on, or null if they're not on a train.
     */
    fun update(road: MapMatchFilter, rail: MapMatchFilter, speed: Double): Way? =
        update(road.matchState(), rail.railMatchState(stopWithinReachMetres), speed)

    fun update(road: MatchState, rail: MatchState, speed: Double): Way? {
        val railway = rail.way.takeIf { rail.confident }
        if (railway == null) {
            fail()
            return null
        }

        if (onTrain) {
            if (!railStillExplainsThePosition(road, rail, speed)) {
                fail()
                // Keep reporting the railway through a short dropout - a ride shouldn't end over a
                // tick or two of doubt.
                return if (onTrain) railway else null
            }
            pass()
            return railway
        }

        // A rail tunnel can keep a train ride going, but must never start one. This is the
        // road-above-the-line hazard that used to keep tunnels and subway lines out of
        // TreeId.TRANSIT altogether: Kent Road runs directly over the North Clyde Line at Charing
        // Cross, and Byres Road over the Glasgow Subway, so a bus on either matches the tunnel below
        // just as well as it matches the road, and would otherwise be announced as being on a
        // train. Requiring the lock to be earned on track that's actually above ground rules that
        // out however long the road runs over the tunnel - and means a metro ride is only picked up
        // where its line comes to the surface.
        if (rail.inTunnel) {
            fail()
            return null
        }

        if (!railBeatsRoad(road, rail)) {
            fail()
            return null
        }

        pass()
        if (consecutivePasses >= acquireTicks) {
            onTrain = true
        }
        return if (onTrain) railway else null
    }

    /**
     * Whether the railway still accounts for where the user is, now that they're reckoned to be
     * on a train. Deliberately not the same test as [railBeatsRoad], which decides whether to
     * believe the railway in the first place - see the class comment.
     */
    private fun railStillExplainsThePosition(
        road: MatchState,
        rail: MatchState,
        speed: Double
    ): Boolean {
        // Underground the road overhead is routinely *nearer* the fix than the line is - measured
        // through the Charing Cross tunnel, 0.1-11m to Kent Road against 0.3-8m to the tunnel
        // centreline - and the road above isn't a credible alternative to a tunnel at all.
        if (rail.inTunnel) return true

        // Still travelling, so whatever the road matcher has found, the user didn't step onto it.
        // The road running alongside a line is carrying traffic at much the same speed, so the two
        // are indistinguishable on speed alone - which is why this only ever holds a lock, and
        // never earns one.
        if (speed > UserGeometry.VEHICLE_SPEED_THRESHOLD_MPS) return true

        // Slowed down, but between stops, so there is still nothing to get off at. This is what a
        // service road passing over the line at Kelvinhaugh - or a road running beside it for a
        // mile - amounts to: a better fit for the fix, and still nowhere the passenger can be.
        if (!rail.nearAStop) return true

        // Stopped at a station, then, and the ride really could be ending. Still on the track,
        // though, so the line explains the fix better than whatever runs over the station does.
        val railDistance = rail.distance ?: return false
        if (railDistance <= onTheLineDistanceMetres) return true

        // Off the railway, at a place and a speed it's possible to have got off it. Now the road
        // matcher's opinion is worth having again.
        return railBeatsRoad(road, rail)
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

    private fun pass() {
        consecutivePasses++
        ticksSinceLastPass = 0
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
    RailMatchArbiter.MatchState(matchedWay, matchedLocation?.distance, isMatchConfident)

private fun MapMatchFilter.railMatchState(
    stopWithinReachMetres: Double
): RailMatchArbiter.MatchState {
    val way = matchedWay
    val matched = matchedLocation
    return RailMatchArbiter.MatchState(
        way,
        matched?.distance,
        isMatchConfident,
        inTunnel = way?.properties?.get("brunnel") == "tunnel",
        // Only read for a confident match that already holds the lock, and the walk isn't free,
        // so don't do it for a match that can't be on a train anyway.
        nearAStop = isMatchConfident && (way != null) && (matched != null) &&
            way.hasStopWithin(matched.point, stopWithinReachMetres),
    )
}

/**
 * Whether this railway Way has a stop within [maxDistance] of [point], measured along the rails in
 * either direction.
 *
 * [point] is the rail matcher's own matched location, so it already lies on this Way and its
 * position along it is exact - no reprojection, and none of the road matcher's offset. The walk
 * follows the line by name through the Ways it is split into (WayContinuation.SAME_ROAD), the same
 * way the station callouts do: a stop 200m off is routinely on the next Way along rather than this
 * one.
 */
private fun Way.hasStopWithin(point: LngLatAlt, maxDistance: Double): Boolean {
    val ruler = CheapRuler(point.latitude)
    val cursor = WayCursor(this, distanceAlongWay(point, ruler), forwards = null)
    return nextAlongWayFeature(
        cursor, maxDistance, AlongWayKind.RAILWAY_STOP, WayContinuation.SAME_ROAD
    ) != null
}
