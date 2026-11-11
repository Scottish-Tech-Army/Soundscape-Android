package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.IntersectionType
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.confectNamesForRoad
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import java.util.Locale

/**
 * Checks a GPX replay's junction callouts against where the user actually went.
 *
 * A junction counts as arrived at where the user's matched position comes closest to its node,
 * provided that's within [ARRIVAL_METRES]. The arrivals, in order, are the junctions the user
 * really came to. Each junction callout is then checked against them: the next junction arrived
 * at after the callout should be the one it described. Arrivals at junctions with another named
 * road which no callout described are listed too, though whether they should have been is a
 * judgement - the verbosity tier and the 30m announce band both hold some back on purpose.
 *
 * Intersections are keyed by location rather than identity, since the grid rebuilds them.
 */
class JunctionTracer {
    private class Arrival(
        val key: String,
        val location: LngLatAlt,
        val roads: String,
        var interest: Int,
        var closest: Double,
        var index: Int,
        var lastSeenTick: Int,
    )

    private class Announcement(
        val index: Int, val key: String, val location: LngLatAlt, val text: String
    )

    private val active = HashMap<String, Arrival>()
    private val arrivals = mutableListOf<Arrival>()
    private val announcements = mutableListOf<Announcement>()
    private var tick = 0

    private fun key(location: LngLatAlt) =
        String.format(Locale.ROOT, "%.6f,%.6f", location.latitude, location.longitude)

    /** Called every replay tick, after the matcher has run. */
    /** The GPX point being replayed, for labelling other logs */
    var currentIndex = 0
        private set

    fun update(index: Int, userGeometry: UserGeometry, gridState: GridState) {
        tick++
        currentIndex = index
        val here = userGeometry.mapMatchedLocation?.point ?: userGeometry.location
        val matched = userGeometry.mapMatchedWay
        // The road being walked, by name: a pavement is known by the road it runs along
        if ((matched != null) && matched.isSidewalkOrCrossing() &&
            (matched.properties?.get("pavement") == null)
        ) {
            confectNamesForRoad(matched, gridState, null)
        }
        val ourRoad = matched?.name ?: matched?.properties?.get("pavement") as String?
        val nearby = gridState.getFeatureTree(TreeId.INTERSECTIONS)
            .getNearbyCollection(here, SEARCH_METRES, gridState.ruler)
        for (feature in nearby) {
            val intersection = feature as Intersection
            if (intersection.intersectionType != IntersectionType.REGULAR) continue
            // Pavements count towards it being a junction - one walked along a pavement is where
            // a path leaves it - but not towards what it's called
            if (intersection.members.count { it.wayType != WayType.JOINER } < 3) continue
            val roads = intersection.members.filter {
                !it.isSidewalkOrCrossing() && (it.wayType != WayType.JOINER)
            }
            val names = roads.mapNotNull { it.name ?: it.ref }.toSortedSet()
            val interest = (names - setOfNotNull(ourRoad)).size
            val distance = gridState.ruler.distance(here, intersection.location)
            val k = key(intersection.location)
            val arrival = active.getOrPut(k) {
                Arrival(
                    k, intersection.location,
                    roads.joinToString(", ") { it.name ?: it.ref ?: "(${it.featureValue})" },
                    interest, distance, index, tick
                )
            }
            arrival.lastSeenTick = tick
            if (distance < arrival.closest) {
                arrival.closest = distance
                arrival.index = index
                arrival.interest = interest
            }
        }
        // Junctions left behind
        val gone = active.values.filter { it.lastSeenTick < tick }
        for (arrival in gone) {
            active.remove(arrival.key)
            if (arrival.closest < ARRIVAL_METRES) arrivals.add(arrival)
        }
    }

    /** Called for every callout the replay speaks. */
    fun callout(index: Int, callout: TrackedCallout) {
        val text = callout.positionedStrings.joinToString(" | ") { it.text }
        val first = callout.positionedStrings.firstOrNull()?.text ?: return
        if (first.startsWith("Intersection") || first.startsWith("Approaching intersection")) {
            announcements.add(Announcement(index, key(callout.location), callout.location, text))
        }
    }

    /** The report, and how many order problems it found. */
    fun report(): Pair<String, Int> {
        for (arrival in active.values) if (arrival.closest < ARRIVAL_METRES) arrivals.add(arrival)
        active.clear()
        arrivals.sortBy { it.index }

        fun Arrival.near(other: Arrival) =
            (key == other.key) || (distance(location, other.location) < CLUSTER_METRES)
        fun arrivalFor(a: Announcement) =
            arrivals.firstOrNull { it.index >= a.index && it.key == a.key }
        // A pavement crossing the mouth of a side road is a node of its own, a few metres from
        // the side road's junction, so an announcement covers every node close to it
        fun announcedBefore(arrival: Arrival) = announcements.any {
            it.index <= arrival.index &&
                ((it.key == arrival.key) || (distance(it.location, arrival.location) < CLUSTER_METRES))
        }

        val out = StringBuilder()
        var problems = 0
        val lastIndex = arrivals.lastOrNull()?.index ?: 0
        val events = announcements.map { it.index to it } + arrivals.map { it.index to it }
        for ((index, event) in events.sortedWith(compareBy({ it.first }, { it.second is Arrival }))) {
            when (event) {
                is Announcement -> {
                    val target = arrivalFor(event)
                    val notes = mutableListOf<String>()
                    if (target == null) {
                        // Only a problem if the user went on to reach other junctions
                        if (arrivals.any { it.index > index && it.interest > 0 }) {
                            problems++
                            notes += "NEVER REACHED"
                        } else {
                            notes += "not reached before the clip ends"
                        }
                    } else {
                        notes += "arrived at ${target.index}"
                        // An earlier callout for a junction the user only reaches after this one
                        val inverted = announcements.filter { other ->
                            other.index < index && other.key != event.key &&
                                (arrivalFor(other)?.let { it.index > target.index && !it.near(target) } == true)
                        }
                        for (other in inverted) {
                            problems++
                            notes += "OUT OF ORDER: spoken after ${other.key} (at ${other.index}) which is reached later"
                        }
                        // Junctions worth naming that the user comes to first, without a word
                        val skipped = arrivals.filter {
                            it.index in index until target.index && it.interest > 0 &&
                                !it.near(target) && !announcedBefore(it)
                        }
                        for (s in skipped) {
                            problems++
                            notes += "PASSES UNANNOUNCED {${s.roads}} at ${s.index} first"
                        }
                    }
                    out.append(String.format(Locale.ROOT, "[%5d] ANNOUNCE %s  %s\n          -> %s\n", index, event.key, event.text, notes.joinToString("; ")))
                }
                is Arrival -> {
                    val announced = announcements.lastOrNull { it.key == event.key && it.index <= event.index }
                    val note = when {
                        announced != null -> "announced at ${announced.index}"
                        event.interest > 0 -> "not announced"
                        else -> "(nothing new to name)"
                    }
                    out.append(String.format(Locale.ROOT, "[%5d] ARRIVE   %s  %.0fm  {%s}  %s\n", event.index, event.key, event.closest, event.roads, note))
                }
            }
        }
        val unannounced = arrivals.count { it.interest > 0 && !announcedBefore(it) }
        out.append("\n${announcements.size} junction callouts, $problems order problems, " +
            "$unannounced junctions with another named road reached unannounced (last arrival $lastIndex)\n")
        return out.toString() to problems
    }

    private fun distance(a: LngLatAlt, b: LngLatAlt): Double {
        val dLat = (a.latitude - b.latitude) * 111320.0
        val dLon = (a.longitude - b.longitude) * 111320.0 * kotlin.math.cos(Math.toRadians(a.latitude))
        return kotlin.math.hypot(dLat, dLon)
    }

    companion object {
        const val SEARCH_METRES = 30.0
        const val ARRIVAL_METRES = 20.0
        /** Junction nodes this close together are one junction to someone walking through */
        const val CLUSTER_METRES = 15.0
    }
}
