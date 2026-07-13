package org.scottishtecharmy.soundscape

import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.filters.MapMatchFilter
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import java.io.File
import kotlin.math.abs

/**
 * There is no real recorded vehicle-speed GPX in the repo - the existing "travel*.gpx" fixtures
 * are all walking pace (max ~2m/s, see conversation history). This builds a synthetic one by
 * following a real stretch of the M8/M80 through a junction fork and resampling it at motorway
 * speed with a realistic (1Hz) GPS sample rate, to check whether map matching can hold a lock on
 * the correct road at vehicle speed rather than the dense pedestrian-rate samples it was
 * originally tuned against.
 *
 * The grid is recentred on the traversal frontier every step (like the real app does as a user
 * moves) rather than snapshotted once, in case moving out of the loaded tile window is what's
 * limiting how far the route can be traced. In practice this made no difference here -
 * `locationUpdate` never reported a grid change (checked via its Boolean return value) before the
 * traversal ran out of connected "highway" Ways, so the ~1.3km cap on this particular route is a
 * genuine property of this OSM extract's parsed road graph at that spot (e.g. a `brunnel` bridge
 * gap, or how the custom planetiler build split/joined this stretch), not a testing-tool
 * limitation. The recentring is kept anyway since it costs nothing and is the more correct thing
 * to do for a route long enough to actually leave the initial tile window.
 */
private fun buildContinuousRoute(
    gridState: GridState,
    startWay: Way,
    targetDistance: Double
): List<LngLatAlt> {
    val ruler = gridState.ruler
    val visited = mutableSetOf<Way>()
    val orderedCoords = mutableListOf<LngLatAlt>()
    var totalDistance = 0.0

    fun appendCoords(way: Way, reversed: Boolean, isFirst: Boolean) {
        val coords = (way.geometry as LineString).coordinates
        val ordered = if (reversed) coords.reversed() else coords
        val toAdd = if (isFirst) ordered else ordered.drop(1)
        orderedCoords.addAll(toAdd)
        for (i in 0 until ordered.size - 1) {
            totalDistance += ruler.distance(ordered[i], ordered[i + 1])
        }
    }

    var currentWay = startWay
    visited.add(currentWay)
    appendCoords(currentWay, reversed = false, isFirst = true)
    println("  way: ref=${currentWay.properties?.get("ref")} name=${currentWay.name} class=${currentWay.featureValue} length=${currentWay.length}")

    while (totalDistance < targetDistance) {
        // Recentre the grid on where we've got to, so the tiles ahead of us are loaded before we
        // reach their edge - mirroring how the real app follows a moving user.
        val gridChanged = runBlocking { gridState.locationUpdate(orderedCoords.last(), emptySet()) }
        println("  locationUpdate at totalDistance=$totalDistance gridChanged=$gridChanged")

        val endIntersection = currentWay.intersections[WayEnd.END.id]
        if (endIntersection == null) {
            println("  stopped: no END intersection (dead end / tile edge)")
            break
        }
        val candidates = endIntersection.members.filter {
            (it != currentWay) && !visited.contains(it) && (it.featureType == "highway")
        }
        if (candidates.isEmpty()) {
            println("  stopped: no unvisited highway candidates at intersection (${endIntersection.members.size} members)")
            break
        }
        println("  intersection has ${endIntersection.members.size} members, ${candidates.size} unvisited highway candidates")

        val lastCoords = (currentWay.geometry as LineString).coordinates
        val approachHeading = ruler.bearing(lastCoords[lastCoords.size - 2], lastCoords.last())

        var bestCandidate: Way? = null
        var bestReversed = false
        var bestAngleDiff = Double.MAX_VALUE
        for (candidate in candidates) {
            val candCoords = (candidate.geometry as LineString).coordinates
            if (candCoords.size < 2) continue
            val touchesStart = candidate.intersections[WayEnd.START.id] == endIntersection
            val reversed = !touchesStart
            val firstTwo = if (reversed) candCoords.takeLast(2).reversed() else candCoords.take(2)
            val departHeading = ruler.bearing(firstTwo[0], firstTwo[1])
            var diff = abs(departHeading - approachHeading)
            if (diff > 180) diff = 360 - diff
            if (diff < bestAngleDiff) {
                bestAngleDiff = diff
                bestCandidate = candidate
                bestReversed = reversed
            }
        }

        val next = bestCandidate ?: break
        visited.add(next)
        appendCoords(next, bestReversed, isFirst = false)
        currentWay = next
        println("  way: ref=${currentWay.properties?.get("ref")} name=${currentWay.name} class=${currentWay.featureValue} length=${currentWay.length} totalDistance=$totalDistance")
    }

    return orderedCoords
}

data class VehicleSample(val location: LngLatAlt, val bearing: Double)

private fun resampleAtSpeed(
    coords: List<LngLatAlt>,
    ruler: Ruler,
    speedMps: Double,
    sampleIntervalSeconds: Double
): List<VehicleSample> {
    val stepDistance = speedMps * sampleIntervalSeconds
    val samples = mutableListOf<VehicleSample>()
    samples.add(VehicleSample(coords[0], ruler.bearing(coords[0], coords[1])))

    var nextSampleDistance = stepDistance
    var accumulated = 0.0
    var i = 0
    while (i < coords.size - 1) {
        val segStart = coords[i]
        val segEnd = coords[i + 1]
        val segLength = ruler.distance(segStart, segEnd)
        if (segLength <= 0.0) {
            i++
            continue
        }
        if (accumulated + segLength >= nextSampleDistance) {
            val t = (nextSampleDistance - accumulated) / segLength
            val lat = segStart.latitude + (segEnd.latitude - segStart.latitude) * t
            val lon = segStart.longitude + (segEnd.longitude - segStart.longitude) * t
            samples.add(VehicleSample(LngLatAlt(lon, lat), ruler.bearing(segStart, segEnd)))
            nextSampleDistance += stepDistance
        } else {
            accumulated += segLength
            i++
        }
    }
    return samples
}

private fun writeGpx(file: File, samples: List<VehicleSample>, speedMps: Double, startTimeMillis: Long) {
    val sb = StringBuilder()
    sb.append("<?xml version='1.0' encoding='utf-8'?>\n")
    sb.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" version=\"1.0\" creator=\"Soundscape\">\n")
    sb.append("<trk>\n<name>Synthetic vehicle route</name>\n<number>0</number>\n<trkseg>\n")
    for ((index, sample) in samples.withIndex()) {
        sb.append("<trkpt lat=\"${sample.location.latitude}\" lon=\"${sample.location.longitude}\">\n")
        sb.append("<ele>0.0</ele>\n")
        sb.append("<accuracy>5.0</accuracy>\n")
        sb.append("<speed>$speedMps</speed>\n")
        sb.append("<bearing>${sample.bearing}</bearing>\n")
        sb.append("<bearingAccuracyDegrees>5.0</bearingAccuracyDegrees>\n")
        sb.append("<time>${startTimeMillis + index * 1000}</time>\n")
        sb.append("</trkpt>\n")
    }
    sb.append("</trkseg>\n</trk>\n</gpx>\n")
    file.parentFile?.mkdirs()
    file.writeText(sb.toString())
}

class TravelModeMapMatchTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun generateAndValidateVehicleGpx() {
        // Near the Robroyston/Provan/Cumbernauld Road cluster of M8/M80 junctions (see the
        // TreeId.HIGHWAY_JUNCTIONS work), so the route crosses a real junction fork.
        val startLocation = LngLatAlt(-4.1848, 55.8854)

        val gridState = FileGridState(MAX_ZOOM_LEVEL, 3)
        gridState.start(offlineExtractPath)
        runBlocking {
            gridState.locationUpdate(startLocation, emptySet())
        }

        val startWay = gridState.getFeatureTree(TreeId.ROADS)
            .getNearestFeature(startLocation, gridState.ruler, 50.0) as? Way
        assertTrue("Expected to find a road near the start location", startWay != null)

        val routeCoords = buildContinuousRoute(gridState, startWay!!, targetDistance = 10000.0)
        println("Route: ${routeCoords.size} coordinates")
        assertTrue("Route too short to be useful", routeCoords.size > 10)

        val speedMps = 25.0 // ~90km/h motorway speed
        val samples = resampleAtSpeed(routeCoords, gridState.ruler, speedMps, sampleIntervalSeconds = 1.0)
        println("Resampled to ${samples.size} points at ${speedMps}m/s, 1Hz")

        val gpxFile = File("$offlineExtractPath/gpxFiles/synthetic-vehicle-m8.gpx")
        writeGpx(gpxFile, samples, speedMps, startTimeMillis = 1744373562419)

        val mapMatchFilter = MapMatchFilter()
        val refsSeen = mutableListOf<String?>()
        var unmatchedCount = 0
        for (sample in samples) {
            runBlocking {
                gridState.locationUpdate(sample.location, emptySet())
            }
            mapMatchFilter.filter(sample.location, gridState, FeatureCollection(), false, true)
            val matched = mapMatchFilter.matchedWay
            if (matched == null) {
                unmatchedCount++
                refsSeen.add(null)
            } else {
                val ref = matched.properties?.get("ref") as? String
                refsSeen.add(ref ?: matched.name ?: matched.featureValue)
            }
        }

        println("Per-sample trace (index: matched):")
        for ((index, ref) in refsSeen.withIndex()) {
            println("  $index: $ref")
        }
        println("Distinct roads matched along route: ${refsSeen.distinct()}")
        println("Unmatched samples: $unmatchedCount / ${samples.size}")

        assertTrue(
            "Too many unmatched samples ($unmatchedCount/${samples.size}) - map matching lost lock",
            unmatchedCount < samples.size / 5
        )
    }
}
