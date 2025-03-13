package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class UserGeometry(val location: LngLatAlt = LngLatAlt(),
                   var phoneHeading: Double? = null,
                   var fovDistance: Double = 50.0,
                   val speed: Double = 0.0,
                   private val headingMode: HeadingMode = HeadingMode.CourseAuto,
                   private var travelHeading: Double? = null,
                   private var headHeading: Double? = null,
                   private val inStreetPreview: Boolean = false)
/**
 * UserGeometry contains all of the data relating to the location and motion of the user. It's
 * aim is to reduces the number of arguments to many of the API calls and to concentrate some of
 * the logic around heading choice.
 *
 * @param phoneHeading is the direction in which the phone is pointing
 * @param travelHeading is the direction in which the phone is moving
 * @param headHeading is the direction in which the head tracking is pointing (not currently implemented)
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