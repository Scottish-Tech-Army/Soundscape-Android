package org.scottishtecharmy.soundscape

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.scottishtecharmy.soundscape.geoengine.MOBILITY_KEY
import org.scottishtecharmy.soundscape.geoengine.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.callouts.getRoadsDescriptionFromFov
import org.scottishtecharmy.soundscape.geoengine.callouts.kerbDistance
import org.scottishtecharmy.soundscape.geoengine.filters.MapMatchFilter
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import kotlin.test.Test

/**
 * Replays recorded walks and checks how far away junctions are when they would be announced.
 *
 * Half of the "it's never clear how far away an intersection is" complaint is that "approaching
 * intersection" used to arrive at no particular range - anywhere in the 50m field of view. Adding
 * a spoken distance makes that worse rather than better unless the announcement is tied to a
 * distance, so this is the test that the tie holds on recorded GPS rather than on hand-built
 * geometry.
 *
 * Walking traces specifically. AutoCallout suppresses intersection callouts in a vehicle and for
 * a while afterwards, so a drive is not a case this code ever runs in - and the sparse fixes of
 * one would test the band against samples further apart than the band's own tolerance.
 */
class IntersectionAnnounceBandTest {

    /** Must match AutoCallout.intersectionAnnounceBandMetres. */
    private val announceBandMetres = 30.0

    /**
     * Clear road behind a junction for it to count as one the user could have been given the full
     * warning about.
     */
    private val isolatedJunctionGapMetres = 50.0

    /** A wander around Milngavie, and a walk through the middle of Glasgow. */
    @Test
    fun junctionsAreAnnouncedOnApproachRatherThanUnderfoot() {
        for (trace in listOf("travel-2", "CentralToBuchananStreet")) {
            val announcedAt = replay(trace)
            Assert.assertTrue("$trace: too few junctions announced", announcedAt.size >= 10)

            val isolated = mutableListOf<Double>()
            var previous: LngLatAlt? = null
            for ((junction, distance) in announcedAt) {
                Assert.assertTrue(
                    "$trace: ${junction.first} announced at $distance m, beyond the " +
                        "$announceBandMetres m band",
                    distance <= announceBandMetres
                )

                // Where there was room to give the full warning, it should usually have been
                // given. Junctions packed ten metres apart cannot be announced thirty metres
                // ahead - the user has only just passed the previous one - so only well-spaced
                // ones are counted.
                val gap = previous?.let { ruler.distance(it, junction.second) }
                if ((gap != null) && (gap > isolatedJunctionGapMetres)) isolated.add(distance)
                previous = junction.second
            }

            Assert.assertTrue("$trace: no well-spaced junctions to check", isolated.size >= 5)

            // A proportion rather than every junction, because selection can still surface one
            // late: getRoadsDescriptionFromFov describes one junction at a time, and until it
            // chooses this one there is nothing for the band to gate. So the guarantee the gate
            // can make is that nothing is announced from too far away, not that everything is
            // announced from far enough.
            val onApproach = isolated.count { it > announceBandMetres / 2 }
            Assert.assertTrue(
                "$trace: only $onApproach of ${isolated.size} well-spaced junctions were " +
                    "announced on approach: $isolated",
                onApproach >= (isolated.size * 3) / 4
            )
        }
    }

    private lateinit var ruler: org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler

    /**
     * Walks [trace], returning the distance at the first fix where each junction would have been
     * announced. Keyed on the junction so the repeated fixes of one approach collapse to the
     * first, which is what the callout history does in production.
     */
    private fun replay(trace: String): Map<Pair<String?, LngLatAlt>, Double> {
        val track = parseGpxTrack("$offlineExtractPath/gpxFiles/$trace.gpx")
        Assert.assertTrue("$trace: no track points", track.size > 50)

        val gridState = FileGridState()
        gridState.start(offlineExtractPath)
        ruler = gridState.ruler
        val mapMatchFilter = MapMatchFilter()
        val categories = setOf(PLACES_AND_LANDMARKS_KEY, MOBILITY_KEY)
        val announcedAt = linkedMapOf<Pair<String?, LngLatAlt>, Double>()
        var measured = 0

        for ((location, recordedHeading) in track) {
            runBlocking { gridState.locationUpdate(location, categories, null) }
            mapMatchFilter.filter(location, gridState, FeatureCollection(), false, null)
            val matchedWay = mapMatchFilter.matchedWay ?: continue

            val userGeometry = UserGeometry(
                location = location,
                // The bearing the recording carries, not the matched Way's own heading: a Way's
                // heading runs along the road rather than along the walk, so it is the reverse of
                // the direction of travel about half the time, which points the field of view
                // backwards and hides the junction being approached.
                phoneHeading = recordedHeading,
                fovDistance = 50.0,
                mapMatchedWay = matchedWay,
                mapMatchedLocation = mapMatchFilter.matchedLocation
            )
            val description = getRoadsDescriptionFromFov(gridState, userGeometry, null)
            val junction = description.intersection ?: continue
            val distance = description.kerbDistance(gridState, null) ?: continue
            measured++

            if (distance > announceBandMetres) continue
            announcedAt.getOrPut(junction.name to junction.location) { distance }
        }
        Assert.assertTrue("$trace: no distances measured along the walk", measured > 50)
        return announcedAt
    }
}

/** Track points of a GPX with the bearing recorded at each, in order. */
private fun parseGpxTrack(filename: String): List<Pair<LngLatAlt, Double?>> =
    parseGpxFromFile(filename).features.mapNotNull { feature ->
        (feature.geometry as? Point)?.coordinates?.let { point ->
            point to feature.properties?.get("heading") as? Double
        }
    }
