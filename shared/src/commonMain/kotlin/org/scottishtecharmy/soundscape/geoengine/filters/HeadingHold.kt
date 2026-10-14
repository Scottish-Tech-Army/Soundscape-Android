package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.concurrent.Volatile

/**
 * Remembers the user's last trusted direction of travel, so that a pause doesn't leave them with
 * no heading at all.
 *
 * The travel heading only exists while the user is moving and the GPS course is accurate enough to
 * believe (see GeoEngine.createUserGeometry and UserGeometry.getTravelHeading). Stop at a crossing
 * with the phone locked in a pocket and it's gone, and with it every callout that needs to know
 * which way is ahead - getFovTriangle returns null and the intersection callouts fall silent at
 * exactly the place they matter. Somebody who walked up to a crossing and stopped is still facing
 * the way they were walking, so that's the best guess there is, and a much better one than none.
 *
 * Bounded both ways, because the guess goes stale: the longer someone stands, or the further they
 * go without a course to say where, the more likely they've turned. Measured on a walk recorded on
 * an iPhone and a Pixel side by side (ToFabricBazaar), the spells without a trusted course while
 * under way were mostly a few seconds long, the longest 37s and the furthest 17m.
 * [MAXIMUM_AGE_MILLISECONDS] and [MAXIMUM_DISTANCE_METRES] cover those with a little to spare. A
 * longer wait ends with no heading, as it should: nothing says which way the user turned in it.
 *
 * Only the heading is held, not its snapping: UserGeometry.snappedHeading lines the held heading up
 * with whatever road the user is matched to now.
 */
class HeadingHold {

    private data class Held(
        val heading: Double,
        val location: LngLatAlt,
        val timestampMilliseconds: Long,
    )

    // Written by the location update, read by every UserGeometry built in between, including the
    // audio engine's - so one immutable snapshot, swapped whole.
    @Volatile
    private var held: Held? = null

    /**
     * Takes the travel heading from [userGeometry], if it has one. Called once per location update
     * with the geometry the callouts are built from.
     *
     * [course] is the fix's GPS course if its accuracy is within
     * TravelHeadingEstimator.MAXIMUM_ROAD_COURSE_ACCURACY_DEGREES, whether or not it was used. One
     * that disagrees with the held heading by more than [CONTRADICTING_COURSE_DEGREES] drops it:
     * the user has turned, even if not by a course good enough to steer by. Standing still doesn't
     * produce courses that good - iOS reports 180° then, and the ToFabricBazaar Pixel had 8 in
     * the whole walk - but manoeuvring does. Replaying androidTravel, a car turning about a car park
     * slowly enough for StationaryDetector to call it stationary had the hold describe what lay
     * 100-250° away from where it was going; at one of those moments the fix's own course, rated
     * 67°, was within 10° of it.
     */
    fun update(userGeometry: UserGeometry, course: Double? = null) {
        val heading = userGeometry.getTravelHeading()
        if (heading != null) {
            held = Held(heading, userGeometry.location, userGeometry.timestampMilliseconds)
            return
        }
        val current = held ?: return
        if ((course != null) &&
            (calculateHeadingOffset(course, current.heading) > CONTRADICTING_COURSE_DEGREES)
        )
            held = null
    }

    /** The held heading if it is still fresh enough at [location] and [nowMilliseconds]. */
    fun heading(location: LngLatAlt, ruler: Ruler, nowMilliseconds: Long): Double? {
        val held = held ?: return null
        val age = nowMilliseconds - held.timestampMilliseconds
        if ((age < 0) || (age > MAXIMUM_AGE_MILLISECONDS)) return null
        if (ruler.distance(held.location, location) > MAXIMUM_DISTANCE_METRES) return null
        return held.heading
    }

    companion object {
        const val MAXIMUM_AGE_MILLISECONDS = 60_000L
        const val MAXIMUM_DISTANCE_METRES = 25.0
        const val CONTRADICTING_COURSE_DEGREES = 60.0
    }
}
