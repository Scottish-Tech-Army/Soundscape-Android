package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.MAXIMUM_TRUSTED_COURSE_ACCURACY_DEGREES
import kotlin.concurrent.Volatile

/**
 * Works out which way the user is travelling, from the GPS course when it can be believed and from
 * the road and the user's own movement when it can't.
 *
 * In order:
 *
 * 1. The GPS course, if its accuracy is within [MAXIMUM_TRUSTED_COURSE_ACCURACY_DEGREES].
 * 2. A poorer course, up to [MAXIMUM_ROAD_COURSE_ACCURACY_DEGREES], when the user is matched to a
 *    road and the course runs within [ALONG_ROAD_DEGREES] of one of its two directions. The road
 *    is a check on the course, not a replacement for it: replacing it with the road's direction
 *    was tried, and was worse - the matched road's heading at the matched point was 45-60° off the
 *    user's actual path often enough (bends, the wrong Way at a junction) to take the 90th
 *    percentile error from 31° to 53°. UserGeometry.snappedHeading still lines the course up with
 *    the road when the two are close.
 * 3. The bearing over the last [MOVEMENT_BASELINE_METRES] of the user's own positions, as it is -
 *    but only if the last [STRAIGHTNESS_BASELINE_METRES] of their path were nearly straight (see
 *    [MINIMUM_STRAIGHTNESS]).
 *
 * Null when none of those has anything to say. UserGeometry only uses the result while the user is
 * in motion, so a bearing worked out from standing-still jitter is never acted on.
 *
 * Measured on a walk recorded on an iPhone and a Pixel side by side (ToFabricBazaar), against the
 * Pixel's course, over the iPhone fixes whose course failed the strict test - which cluster at
 * turns, where any look-back lags:
 *
 *   source                                  error, 68th / 90th percentile
 *   course rated 45-90° (2 above)                 12.5° / 29.7°
 *   bearing over the last 5m (3 above)            20.4° / 39.4°
 *   last trusted heading, held (HeadingHold)      36.6° / 55.2°
 *
 * which is the order they are tried in. The movement bearing gets worse with a longer baseline in
 * exactly these spells (27° / 54° at 15m), because it lags further behind a turn.
 *
 * Positions are the unfiltered fixes, as for the map matchers and StationaryDetector, and as the
 * measurements above were taken.
 */
class TravelHeadingEstimator {

    private data class Fix(val location: LngLatAlt, val timestampMilliseconds: Long)

    // Written once per location update, read by every UserGeometry built - so one immutable list,
    // swapped whole.
    @Volatile
    private var recent: List<Fix> = emptyList()

    /** Records an unfiltered fix the geoengine has accepted. */
    fun addFix(location: LngLatAlt, timestampMilliseconds: Long) {
        recent = (recent + Fix(location, timestampMilliseconds)).filter {
            timestampMilliseconds - it.timestampMilliseconds <= MOVEMENT_WINDOW_MILLISECONDS
        }
    }

    /**
     * The direction of travel, or null if there's no telling.
     *
     * @param bearing the fix's GPS course, null if it carried none
     * @param bearingAccuracy that course's accuracy on Android's scale, null if none was reported
     * @param roadHeading the heading of the matched road at the matched point - either of its two
     * directions - or null with no road match
     */
    fun estimate(
        bearing: Double?,
        bearingAccuracy: Double?,
        roadHeading: Double?,
        ruler: Ruler,
        nowMilliseconds: Long,
    ): Double? {
        if (bearing != null) {
            if ((bearingAccuracy == null) ||
                (bearingAccuracy < MAXIMUM_TRUSTED_COURSE_ACCURACY_DEGREES)
            )
                return bearing

            if ((roadHeading != null) &&
                (bearingAccuracy < MAXIMUM_ROAD_COURSE_ACCURACY_DEGREES) &&
                runsAlongRoad(bearing, roadHeading)
            )
                return bearing
        }

        return movementBearing(ruler, nowMilliseconds)
    }

    /**
     * The bearing from the newest fix back to the most recent one at least
     * [MOVEMENT_BASELINE_METRES] from it, within [MOVEMENT_WINDOW_MILLISECONDS] of now - provided
     * the path back to the most recent fix [STRAIGHTNESS_BASELINE_METRES] away is at least
     * [MINIMUM_STRAIGHTNESS] straight.
     *
     * The straightness test is what keeps GPS wander out. Replaying androidTravel, which spends
     * 50 minutes inside about 90m of one spot with fixes jumping about at up to 4 m/s, the
     * movement bearing there was no better than random: 85° median error against where the user
     * actually went next. Wander does produce short straight runs, so the 5m bearing alone can't
     * tell; over 15m it mostly can. Measured over every movement bearing in the GPX fixtures
     * outside that spell, against the bearing to where the user was next 15m away:
     *
     *                                  fixes, error 50th / 68th / 90th percentile
     *   no test                        2478,    14° / 31° / 119°
     *   15m at least 0.9 straight      1610,     9° / 18° /  61°
     *   (the ones it drops)             868,    37° / 76° / 150°
     *
     * and inside the spell it drops 186 of the 259. The cost is that after a sharp turn
     * there is no movement bearing until about 13m past it - where, lagging the turn, it was
     * wrong anyway.
     */
    private fun movementBearing(ruler: Ruler, nowMilliseconds: Long): Double? {
        val fixes = recent
        val newest = fixes.lastOrNull() ?: return null
        if (nowMilliseconds - newest.timestampMilliseconds > MOVEMENT_WINDOW_MILLISECONDS)
            return null
        var bearing: Double? = null
        var path = 0.0
        var later = newest
        for (i in fixes.size - 2 downTo 0) {
            val earlier = fixes[i]
            path += ruler.distance(earlier.location, later.location)
            later = earlier
            val net = ruler.distance(earlier.location, newest.location)
            if ((bearing == null) && (net >= MOVEMENT_BASELINE_METRES))
                bearing = (ruler.bearing(earlier.location, newest.location) + 360.0) % 360.0
            if (net >= STRAIGHTNESS_BASELINE_METRES)
                return if (net / path >= MINIMUM_STRAIGHTNESS) bearing else null
        }
        return null
    }

    /** Whether [heading] lies within [ALONG_ROAD_DEGREES] of either direction of the road. */
    private fun runsAlongRoad(heading: Double, roadHeading: Double): Boolean {
        val offset = calculateHeadingOffset(heading, roadHeading)
        return (offset <= ALONG_ROAD_DEGREES) || (offset >= 180.0 - ALONG_ROAD_DEGREES)
    }

    companion object {
        /**
         * The worst course accuracy taken when it can be checked against a matched road. iOS
         * reports a stationary course as 180°, 90° once rescaled (see IosLocationProvider), so it
         * stays out.
         */
        const val MAXIMUM_ROAD_COURSE_ACCURACY_DEGREES = 90.0

        /** How far from a road's direction a poorer course can be and still be believed. */
        const val ALONG_ROAD_DEGREES = 60.0

        const val MOVEMENT_BASELINE_METRES = 5.0
        const val STRAIGHTNESS_BASELINE_METRES = 15.0
        /** Net distance over path length - see movementBearing. */
        const val MINIMUM_STRAIGHTNESS = 0.9
        const val MOVEMENT_WINDOW_MILLISECONDS = 30_000L
    }
}
