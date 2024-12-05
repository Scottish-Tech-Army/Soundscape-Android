package org.scottishtecharmy.soundscape.geoengine.filters

import android.util.Log
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class TrackedCallout(
    val callout: String,
    val location: LngLatAlt,
    private val isPoint: Boolean,
    private val isGeneric: Boolean
    )
{
    val time = System.currentTimeMillis()

    override fun equals(other: Any?) : Boolean {
        if(other is TrackedCallout) {
            if(isGeneric && other.isGeneric) {
                // If the POIs are both generic OSM POIs and are within the appropriate proximity
                // range+ of each other, treat them as a match
                // TODO: Don't hard code the distance here
                return location.distance(other.location) < 20.0
            }
            // If the TrackedCallout isn't for a point i.e. it's a Polygon, then we can't compare
            // it's location, as the nearest point on a Polygon changes as we move.
            return (other.callout == callout)
                    && (!isPoint || location.distance(other.location) < 10.0)
        }
        return false
    }

    override fun hashCode(): Int {
        var result = callout.hashCode()
        result = 31 * result + location.hashCode()
        return result
    }
}

class CalloutHistory(private val expiryPeriod : Long = 60000) {

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
