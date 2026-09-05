package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.WayCursor
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.distanceAlongLineString
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.abs

/**
 * UserGeometry contains all of the data relating to the location and motion of the user. It's
 * aim is to reduces the number of arguments to many of the API calls and to concentrate some of
 * the logic around heading choice.
 *
 * @param location is the current location of the user from the location provider
 * @param phoneHeading is the direction in which the phone is pointing
 * @param travelHeading is the direction in which the phone is moving
 * @param headHeading is the direction in which the head tracking is pointing (not currently implemented)
 * @param fovDistance is the distance in which the user can see, used when searching for POI
 * @param speed is the speed of the user (currently straight from the location provider)
 * @param mapMatchedWay is the Way that has been map matched to the location
 * @param mapMatchedLocation os the location that has been map matched to the location, it will be a
 * point on the mapMatchedWay
 * @param mapMatchedRailway is the railway Way that has been map matched to the location, from a
 * separate matcher against the transit network - see [probablyOnTrain]
 * @param currentBeacon is the location of any current audio beacon. This affects various callouts
 * which is why it's a property of the UserGeometry class.
 * @param headingMode is the method used to calculate the heading
 * @param inStreetPreview is true if the user is in StreetPreview mode
 *
 * The heading prioritization comes from iOS - see https://github.com/Scottish-Tech-Army/Soundscape-Android/issues/364
 *
 *  collection - used for calculating callouts, two possibilities:
 *      course (travel), user (head), device (phone), or
 *      user (head), device (phone), course (travel)
 *
 *  presentation - user for audio positioning:
 *      user (head), course (travel), device (phone)
 *
 */
class UserGeometry(
    val location: LngLatAlt = LngLatAlt(),
    var phoneHeading: Double? = null,
    var fovDistance: Double = 50.0,
    val speed: Double = 0.0,
    val mapMatchedWay: Way? = null,
    val mapMatchedLocation: PointAndDistanceAndHeading? = null,
    val mapMatchedRailway: Way? = null,
    val currentBeacon: LngLatAlt? = null,
    val ruler: Ruler = CheapRuler(location.latitude),
    val timestampMilliseconds: Long = 0L,
    private val headingMode: HeadingMode = HeadingMode.CourseAuto,
    private var travelHeading: Double? = null,
    private var headHeading: Double? = null,
    val errorDistance: Double = 0.0,
    val errorHeading: Double = 0.0,
    val inStreetPreview: Boolean = false
) {
    private val automotiveRangeMultiplier = 6.0
    private val streetPreviewRangeIncrement = 10.0

    fun inVehicle(): Boolean {
        // The Activity Recognition seemed unreliable, and so we use the current speed instead.
        // Travelling at over 5m/s (10mph) assumes we're in a vehicle. When the vehicle stops at
        // junctions it will switch to non-vehicle mode.
        return speed > VEHICLE_SPEED_THRESHOLD_MPS
    }

    fun inMotion(): Boolean {
        return speed > 0.2
    }

    /**
     * Roads and railways are matched independently (see MapMatchFilter's networkTree), since
     * they're separate connectivity graphs.
     *
     * A confident lock onto a railway is NOT on its own a safe proxy for being on a train, however
     * much it looks like one. Motorways are routinely built alongside railway lines for kilometres
     * - the M90 past Winchburgh runs 35-70m from the Winchburgh Chord for about a minute at 70mph,
     * which used to be enough for a driver to be told "On Winchburgh Chord". Deciding this needs
     * the railway match to be weighed against the road match from the same update, which is
     * RailMatchArbiter's job; by the time mapMatchedRailway is set here, that's already happened.
     */
    fun probablyOnTrain(): Boolean {
        return inVehicle() && (mapMatchedRailway != null)
    }

    private fun transform(distance: Double): Double {
        if (inVehicle()) return distance * automotiveRangeMultiplier
        if (inStreetPreview) return distance + streetPreviewRangeIncrement
        return distance
    }

    fun getTravelHeading(): Double? {
        if (inMotion() && (travelHeading != null))
            return travelHeading
        return null
    }

    fun snappedHeading(): Double? {
        var heading = heading()
        if (heading != null) {
            // Snap heading to matched way heading if we're close to it
            val wayHeading = mapMatchedLocation?.heading
            if (wayHeading != null) {
                val headingOffset = abs(heading - wayHeading)
                if (headingOffset < 30.0)
                    heading = wayHeading
                else if ((headingOffset > 150.0) && ((headingOffset < 210.0)))
                    heading = (wayHeading + 180.0) % 360.0
                else if (headingOffset > 330.0)
                    heading = wayHeading
            }
        }
        return heading
    }

    fun heading(): Double? {
        when (headingMode) {
            // Priority: travel, head, phone
            HeadingMode.CourseAuto -> {
                var heading = getTravelHeading()
                if (heading == null) {
                    heading = headHeading
                    if (heading == null) {
                        heading = phoneHeading
                    }
                }
                return heading
            }

            // Priority: Head, phone, travel
            HeadingMode.HeadAuto -> {
                var heading = headHeading
                if (heading == null) {
                    heading = phoneHeading
                    if (heading == null) {
                        heading = getTravelHeading()
                    }
                }
                return heading
            }

            HeadingMode.Phone -> return phoneHeading
            HeadingMode.Travel -> return travelHeading
        }
    }

    fun presentationHeading(): Double? {
        // Priority: Head, travel, phone
        var heading = headHeading
        if (heading == null) {
            heading = getTravelHeading()
            if (heading == null) {
                heading = phoneHeading
            }
        }
        return heading
    }

    /**
     * Where the user is along [way], as a cursor the along-way queries can walk from - see
     * WayCursor and nextAlongWayFeature.
     *
     * [way] is passed in rather than assumed to be [mapMatchedWay] because roads and railways are
     * matched independently: on a train the caller wants a cursor on [mapMatchedRailway], and the
     * road match is whatever happens to run alongside.
     *
     * The position is projected onto the Way from [mapMatchedLocation] where there is one - it's
     * already been smoothed against the road network, so it's a better answer than the raw GPS
     * fix - and from [location] otherwise. Note that mapMatchedLocation's own index/
     * positionAlongLine can't be used directly: they're relative to the follower's accumulated
     * LineString across several Ways, not to any one Way (see MapMatchFilter).
     *
     * Direction comes from the travel heading against the Way's own heading at that point.
     * [fallbackHeading] stands in when there is no travel heading - the GPS fix carried no bearing,
     * or its bearing accuracy was too poor to use (see GeoEngine) - and callers pass the bearing
     * from where the user was on the previous fix, which is what movement itself says about which
     * way they are going. With neither, the direction is left null and the queries look both ways
     * rather than guess.
     */
    fun cursorOn(way: Way, fallbackHeading: Double? = null): WayCursor? {
        val line = way.geometry as? LineString ?: return null
        if (line.coordinates.size < 2) return null

        val point = mapMatchedLocation?.point ?: location
        val pdh = ruler.distanceToLineString(point, line)
        val forwards = (getTravelHeading() ?: fallbackHeading)?.let { heading ->
            calculateHeadingOffset(heading, pdh.heading) < 90.0
        }
        return WayCursor(way, distanceAlongLineString(line, pdh, ruler), forwards)
    }

    /**
     * getSearchDistance returns the distance to use when searching for POIs
     */
    fun getSearchDistance(): Double {
        return transform(50.0)
    }

    /**
     * getTriggerRange returns the distance to use when detecting POIs to call out
     */
    fun getTriggerRange(category: SuperCategoryId): Double {
        return when (category) {
            SuperCategoryId.OBJECT,
            SuperCategoryId.SAFETY -> transform(10.0)

            SuperCategoryId.PLACE,
            SuperCategoryId.INFORMATION,
            SuperCategoryId.MOBILITY -> transform(20.0)

            SuperCategoryId.LANDMARK -> transform(50.0)

            SuperCategoryId.MARKER -> transform(50.0)

            else -> transform(0.0)
        }
    }

    /**
     * getTriggerRange returns the distance if a POI is still in proximity after a callout
     */
    fun getProximityRange(category: SuperCategoryId): Double {
        return when (category) {
            SuperCategoryId.OBJECT,
            SuperCategoryId.SAFETY -> transform(20.0)

            SuperCategoryId.PLACE,
            SuperCategoryId.INFORMATION,
            SuperCategoryId.MOBILITY -> transform(30.0)

            SuperCategoryId.LANDMARK -> transform(100.0)

            else -> transform(0.0)
        }
    }

    enum class HeadingMode {
        CourseAuto,
        HeadAuto,
        Phone,
        Travel
    }

    companion object {
        const val VEHICLE_SPEED_THRESHOLD_MPS = 5.0

        /**
         * 30mph. Above this, distances are read out in kilometres/miles regardless of how short
         * they are - see formatDistanceAndDirection. Metre/foot precision is meaningless when
         * you're covering more than 13m every second, and the extra syllables cost time that
         * matters far more at speed.
         */
        const val BIG_UNIT_SPEED_THRESHOLD_MPS = 13.4
    }
}
