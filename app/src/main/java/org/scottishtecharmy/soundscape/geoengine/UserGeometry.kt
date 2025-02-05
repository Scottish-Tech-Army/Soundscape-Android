package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/** UserGeometry contains all of the data relating to the location and motion of the user. It's
 * aim is to reduces the number of arguments to many of the API calls and to concentrate some of
 * the logic around heading choice.
 *
 * @param phoneHeading is the direction in which the phone is pointing
 * @param travelHeading is the direction in which the phone is moving
 * @param headHeading is the direction in which the head tracking is pointing
 *
 * On iOS there were two types of heading prioritization:
 *  collection: course (travel?), user (phone?), device (head?)
 *  presentation:user (phone?), course (travel?), device (head?)
 *
 * It's very hard to follow the Heading code, but because head tracking isn't always (usually)
 * present and I think `user` must be the phone direction and `device` the head tracking
 * direction.
 *
 *  presentationHeading is used for audio beacons. This makes sense - it's the direction of the
 *  phone that is used, though I'm surprised it ever uses directly of travel.
 *  collectionHeading is used for intersections - the direction of travel is most significant
 *  here.
 *
 * However, if the user has thrown their phone into their bag, we need to detect this and ignore
 * the phone direction.
 */
class UserGeometry(val location: LngLatAlt = LngLatAlt(),
                   var phoneHeading: Double = 0.0,
                   val fovDistance: Double = 50.0,
                   val inVehicle: Boolean = false,
                   val inMotion: Boolean = false,
                   val speed: Double = 0.0,
                   private val headingMode: HeadingMode = HeadingMode.Auto,
                   private var travelHeading: Double = Double.NaN,
                   private var headHeading: Double = Double.NaN,
                   private val inStreetPreview: Boolean = false)
{
    private val automotiveRangeMultiplier = 6.0
    private val streetPreviewRangeIncrement = 10.0

    private fun transform(distance: Double) : Double {
        if(inVehicle) return distance * automotiveRangeMultiplier
        if(inStreetPreview) return distance + streetPreviewRangeIncrement
        return distance
    }

    fun heading() : Double {
        when(headingMode) {
            HeadingMode.Auto -> {
                if(speed > 0.2 && !travelHeading.isNaN())
                    return travelHeading

                return phoneHeading
            }
            HeadingMode.Phone -> return phoneHeading
            HeadingMode.Travel -> return travelHeading
        }
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
        Auto,
        Phone,
        Travel
    }
}