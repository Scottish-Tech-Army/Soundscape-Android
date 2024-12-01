package org.scottishtecharmy.soundscape.filters

import android.util.Log
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class TrackedCallout(
    val callout: String,
    val location: LngLatAlt
//    val category: String,
//    val isGenericOSMPOI: Boolean,
//    val trackingKey: String
    )
{
    val time = System.currentTimeMillis()

     init {

//        if let poi = callout.poi {
//            isGenericOSMPOI = poi.isGenericOSMPOI
//            trackingKey = poi.keyForTracking
//            category = SuperCategory(rawValue: poi.superCategory) ?? SuperCategory.undefined
//        } else {
//            isGenericOSMPOI = false
//            trackingKey = callout.key
//            category = SuperCategory.undefined
//        }
    }

    override fun equals(other: Any?) : Boolean {
        if(other is TrackedCallout) {
            return (other.callout == callout) &&
                    (location.distance(other.location) < 10.0)
        }
//        if poi.isGenericOSMPOI {
//            if trackingKey == poi.keyForTracking, let trackedPOI = callout.poi {
//                // If the POIs are both generic OSM POIs and are within the appropriate proximity range+ of each other, treat them as a match
//                return trackedPOI.centroidLocation.distance(from: poi.centroidLocation) < category.proximityRange(context: context)
//            }
//            return false
//        } else {
//            return trackingKey == poi.keyForTracking
//        }
        return false
    }

    override fun hashCode(): Int {
        var result = callout.hashCode()
        result = 31 * result + location.hashCode()
        return result
    }
}

class CalloutHistory(val expiryPeriod : Long = 60000) {

    // List of recent history
    private val history = mutableListOf<TrackedCallout>()

    fun add(callout: TrackedCallout) {
        history.add(callout)
    }

    fun trim(location: LngLatAlt) {
        val now = System.currentTimeMillis()
        // TODO : Remove hardcoded expiry time and distance should be based on category
        history.removeAll {
            val result = ((now - it.time) > expiryPeriod) || (location.distance(it.location) > 50.0)
            if(result)  println("Trim ${it.callout} - ${now - it.time} ${location.distance(it.location)}")
            result
        }
    }

    fun find(callout: TrackedCallout) : Boolean {
        for(tc in history) {
            if(tc == callout) {
                return true
            }
        }
        return false
    }

    fun size() : Int {
        return history.size
    }
    companion object {
        private const val TAG = "TrackedCallout"
    }
}
