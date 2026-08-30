package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class TrackedCallout(
    val userGeometry: UserGeometry? = null,
    val trackedText: String = "",
    val location: LngLatAlt = LngLatAlt(),
    var positionedStrings: List<PositionedString> = emptyList(),
    val isPoint: Boolean = false,
    private val isGeneric: Boolean = false,
    private val filter: Boolean = true,
    var calloutHistory: CalloutHistory? = null,
    var locationFilter: LocationUpdateFilter? = null,
    // Overrides trackedText for equality/hashCode comparison only. Lets a callout whose spoken
    // text embeds an ever-changing value (e.g. a live "distance since X") still dedup against
    // an earlier callout that differs only in that value.
    private val dedupText: String? = null,
    /**
     * A second, broader key this callout records in the history when it's spoken, but which it
     * does *not* itself match against - see [CalloutHistory.add]. Deliberately asymmetric: a
     * motorway junction callout ("On M80 at Junction 2, Robroyston") should stop a plain "still
     * on the M80" callout following it a few seconds later, without a plain M80 callout ever
     * stopping the junction one. Reaching a junction is always worth announcing; being told
     * again which road you're on straight afterwards is not.
     */
    val extraDedupText: String? = null,
) {
    val time = userGeometry?.timestampMilliseconds ?: 0L
    val ruler = location.createCheapRuler()
    private val comparableText = dedupText ?: trackedText

    override fun equals(other: Any?): Boolean {
        if (!filter) return false

        if (other is TrackedCallout) {
            if (isGeneric && other.isGeneric) {
                // If the POIs are both generic OSM POIs and are within the appropriate proximity
                // range+ of each other, treat them as a match
                // TODO: Don't hard code the distance here - also, we need to compare more than
                //  just isGeneric as that would match benches with top up taps etc.
                return ruler.distance(location, other.location) < 20.0
            }
            // If the TrackedCallout isn't for a point i.e. it's a Polygon, then we can't compare
            // it's location, as the nearest point on a Polygon changes as we move.
            return (other.comparableText == comparableText)
                    && (!isPoint || ruler.distance(location, other.location) < 10.0)
        }
        return false
    }

    override fun hashCode(): Int {
        var result = comparableText.hashCode()
        result = 31 * result + location.hashCode()
        return result
    }
}

class CalloutHistory(expiryPeriodMilliseconds: Long = 60000) {

    // List of recent history
    private val history = mutableListOf<TrackedCallout>()

    private var expiryPeriod: Long = 0

    init {
        expiryPeriod = expiryPeriodMilliseconds
    }

    fun add(callout: TrackedCallout) {
        history.add(callout)
        // Recorded as a separate entry rather than as a second key on the callout itself, so
        // that matching stays one-way - see TrackedCallout.extraDedupText. It shares the
        // callout's timestamp and location, so it expires with it on the next trim().
        callout.extraDedupText?.let { extra ->
            history.add(
                TrackedCallout(
                    userGeometry = callout.userGeometry,
                    trackedText = extra,
                    location = callout.location,
                    isPoint = callout.isPoint
                )
            )
        }
    }

    fun trim(userGeometry: UserGeometry) {
        val now = userGeometry.timestampMilliseconds
        // TODO : Remove hardcoded expiry time and distance should be based on category
        history.removeAll {
            val result =
                ((now - it.time) > expiryPeriod) || (it.isPoint && userGeometry.ruler.distance(
                    userGeometry.location,
                    it.location
                ) > 50.0)
//            if(result)  println("Trim ${it.callout} - ${now - it.time} ${userGeometry.location.distance(it.location)}")
            result
        }
    }

    fun find(callout: TrackedCallout): Boolean {
        for (tc in history) {
            if (tc == callout) {
                return true
            }
        }
        return false
    }

    fun size(): Int {
        return history.size
    }
}
