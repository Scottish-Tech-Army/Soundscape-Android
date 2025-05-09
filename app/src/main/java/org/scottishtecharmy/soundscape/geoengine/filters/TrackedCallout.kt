package org.scottishtecharmy.soundscape.geoengine.filters

import android.os.Build
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class TrackedCallout(
    val callout: String,
    val location: LngLatAlt,
    val isPoint: Boolean,
    private val isGeneric: Boolean
    )
{
    val time = System.currentTimeMillis()
    val ruler = location.createCheapRuler()

    override fun equals(other: Any?) : Boolean {
        if(other is TrackedCallout) {
            if(isGeneric && other.isGeneric) {
                // If the POIs are both generic OSM POIs and are within the appropriate proximity
                // range+ of each other, treat them as a match
                // TODO: Don't hard code the distance here - also, we need to compare more than
                //  just isGeneric as that would match benches with top up taps etc.
                return ruler.distance(location, other.location) < 20.0
            }
            // If the TrackedCallout isn't for a point i.e. it's a Polygon, then we can't compare
            // it's location, as the nearest point on a Polygon changes as we move.
            return (other.callout == callout)
                    && (!isPoint || ruler.distance(location, other.location) < 10.0)
        }
        return false
    }

    override fun hashCode(): Int {
        var result = callout.hashCode()
        result = 31 * result + location.hashCode()
        return result
    }
}

class CalloutHistory(expiryPeriodMilliseconds : Long = 60000) {

    // List of recent history
    private val history = mutableListOf<TrackedCallout>()

    private var expiryPeriod : Long = 0
    init {
        // Unit tests must have a zero expiryPeriod as they don't run in realtime
        if(Build.VERSION.SDK_INT != 0) {
            expiryPeriod = expiryPeriodMilliseconds
        }
    }

    fun add(callout: TrackedCallout) {
        history.add(callout)
    }

    fun trim(userGeometry: UserGeometry) {
        val now = System.currentTimeMillis()
        // TODO : Remove hardcoded expiry time and distance should be based on category
        history.removeAll {
            val result = ((now - it.time) > expiryPeriod) || (it.isPoint && userGeometry.ruler.distance(userGeometry.location, it.location) > 50.0)
//            if(result)  println("Trim ${it.callout} - ${now - it.time} ${userGeometry.location.distance(it.location)}")
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

    /**
     * checkAndAdd checks the history to see if a callout already exists. If it does it returns
     * false, otherwise it adds it and returns true.
     * @return Returns true if the callout can go ahead, false if it should be skipped.
     */
    fun checkAndAdd(callout: TrackedCallout) : Boolean {
        if (find(callout)) {
            println("Discard ${callout.callout} as in history")
            return false
        } else {
            add(callout)
        }
        return true

    }
    fun size() : Int {
        return history.size
    }
}
