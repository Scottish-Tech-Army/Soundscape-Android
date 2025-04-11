package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
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
class UserGeometry(val location: LngLatAlt = LngLatAlt(),
                   var phoneHeading: Double? = null,
                   var fovDistance: Double = 50.0,
                   val speed: Double = 0.0,
                   val mapMatchedWay: Way? = null,
                   val mapMatchedLocation: PointAndDistanceAndHeading? = null,
                   val currentBeacon: LngLatAlt? = null,
                   private val headingMode: HeadingMode = HeadingMode.CourseAuto,
                   private var travelHeading: Double? = null,
                   private var headHeading: Double? = null,
                   private val inStreetPreview: Boolean = false)
{
    private val automotiveRangeMultiplier = 6.0
    private val streetPreviewRangeIncrement = 10.0

    fun inVehicle() : Boolean {
        // The Activity Recognition seemed unreliable, and so we use the current speed instead.
        // Travelling at over 5m/s (10mph) assumes we're in a vehicle. When the vehicle stops at
        // junctions it will switch to non-vehicle mode.
        return speed > 5.0
    }

    private fun transform(distance: Double) : Double {
        if(inVehicle()) return distance * automotiveRangeMultiplier
        if(inStreetPreview) return distance + streetPreviewRangeIncrement
        return distance
    }

    fun getTravelHeading() : Double? {
        if(speed > 0.2 && (travelHeading != null))
            return travelHeading
        return null
    }

    fun snappedHeading() : Double? {
        var heading = heading()
        if(heading != null) {
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
    fun heading() : Double? {
        when(headingMode) {
            // Priority: travel, head, phone
            HeadingMode.CourseAuto -> {
                var heading = getTravelHeading()
                if(heading == null) {
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
                if(heading == null) {
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

    fun presentationHeading() : Double? {
        // Priority: Head, travel, phone
        var heading = headHeading
        if(heading == null) {
            heading = getTravelHeading()
            if (heading == null) {
                heading = phoneHeading
            }
        }
        return heading
    }

    /**
     * getSearchDistance returns the distance to use when searching for POIs
     */
    fun getSearchDistance() : Double {
        return transform(50.0)
    }

    /**
     * getTriggerRange returns the distance to use when detecting POIs to call out
     */
    fun getTriggerRange(category: String) : Double {
        return when(category) {
            "object",
            "safety" -> transform(10.0)

            "place",
            "information",
            "mobility" -> transform(20.0)

            "landmark" -> transform(50.0)

            "marker" -> transform(50.0)

            else -> transform(0.0)
        }
    }

    /**
     * getTriggerRange returns the distance if a POI is still in proximity after a callout
     */
    fun getProximityRange(category: String) : Double {
        return when(category) {
            "object",
            "safety" -> transform(20.0)

            "place",
            "information",
            "mobility" -> transform(30.0)

            "landmark" -> transform(100.0)

            else -> transform(0.0)
        }
    }

    enum class HeadingMode {
        CourseAuto,
        HeadAuto,
        Phone,
        Travel
    }
}