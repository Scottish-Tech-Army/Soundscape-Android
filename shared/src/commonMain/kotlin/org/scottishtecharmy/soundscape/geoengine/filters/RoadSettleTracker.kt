package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * Tracks how far a vehicle has gone along the road it is map matched to, so that the travel
 * callout can wait until a newly joined road is one the vehicle is actually staying on.
 *
 * Driving through a housing estate is a string of short hops - a hundred metres along one street,
 * a turn, eighty metres along the next - and every one of them was announced, most of them after
 * the vehicle had already turned off again. Holding a new road back until [SETTLE_DISTANCE_METRES]
 * have been travelled on it means the hops pass unremarked and the street the vehicle ends up on
 * is still named. On a long road it costs a few seconds.
 *
 * A road is identified the way the travel callout dedups it: by its route number if it has one,
 * since the A81 is one road through all its changes of street name, and otherwise by its name.
 * Unnamed roads - slip roads, the "Service that joins..." links - all count as one road, which is
 * what they are to a passenger: a short connection to somewhere else, not worth naming unless the
 * vehicle stays on it.
 *
 * The matcher doesn't hold a road cleanly through a junction: replaying BusTripToMilngavie it
 * flicked from Hope Street to Sauchiehall Street for 15m, and from the A82 to Scott Street for a
 * single fix and to an unnamed link for 33m. Starting again from nothing at each of those would
 * hold back the road the vehicle never left, so an excursion of less than [EXCURSION_METRES]
 * that comes back to the same road carries on counting where it left off, and coming back to the
 * road that last settled is settled straight away. A fix with no matched road at all leaves the
 * road as it was, for the same reason.
 */
class RoadSettleTracker {
    private class Stretch(val key: String, var distance: Double)

    private var current: Stretch? = null
    private var previous: Stretch? = null
    private var settledKey: String? = null
    private var lastLocation: LngLatAlt? = null

    /** Called once per location update, whether or not a callout is made. */
    fun update(userGeometry: UserGeometry) {
        val step = lastLocation?.let { userGeometry.ruler.distance(it, userGeometry.location) } ?: 0.0
        lastLocation = userGeometry.location

        val way = userGeometry.mapMatchedWay
        val key = if (way == null) current?.key else (way.ref ?: way.name ?: "")
        if (key == null) return

        val stretch = current
        when {
            stretch != null && stretch.key == key -> stretch.distance += step
            stretch != null && previous?.key == key && stretch.distance < EXCURSION_METRES -> {
                // Back from a brief excursion: carry on along the road from before it, counting
                // the excursion too, since the vehicle was travelling the whole time.
                val resumed = previous!!
                resumed.distance += stretch.distance + step
                previous = stretch
                current = resumed
            }
            else -> {
                // The step onto the new road counts towards it, so that a flicker and back loses
                // none of the distance travelled.
                previous = stretch
                current = Stretch(key, step)
            }
        }
        current?.let { if (it.distance >= SETTLE_DISTANCE_METRES) settledKey = it.key }
    }

    /**
     * True once the vehicle has gone far enough along the current road for it to be worth
     * announcing, or if no road has been matched at all - there is nothing to wait for then.
     */
    fun settled(): Boolean {
        val stretch = current ?: return true
        return stretch.key == settledKey
    }

    companion object {
        /**
         * Longer than a typical residential block, so a street that is only driven along to
         * reach the next turn isn't named, but short enough to be a few seconds at road speed.
         */
        const val SETTLE_DISTANCE_METRES = 150.0

        /** The longest stretch on another road that still counts as a matcher flicker. */
        const val EXCURSION_METRES = 40.0
    }
}
