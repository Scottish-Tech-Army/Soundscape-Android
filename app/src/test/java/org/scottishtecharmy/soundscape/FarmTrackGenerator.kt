package org.scottishtecharmy.soundscape

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.callouts.CalloutPoiSelection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import java.io.File
import java.time.Instant
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Generates the test farm's replay track (test-farm/tracks/) from the Glasgow extract: a day out
 * on foot, by bus and by train, sampled once a second with the speed and bearing a phone would
 * report. It only runs when asked for:
 *
 *     ./gradlew :app:testDebugUnitTest --tests '*FarmTrackGenerator*' \
 *         -PfarmTrack=test-farm/tracks/glasgow-multimodal.gpx [-PfarmTrackSeed=1] [-PfarmTrackJourneys=6]
 *
 * Each journey walks to a bus stop or a station, waits, rides and gets off; buses and trains
 * alternate. The tiles have no bus routes, so a "bus" drives the quickest road path between two
 * stops (main roads preferred) and stops at every bus stop it passes. Trains follow the railway,
 * which can't turn sharply, and stop at every station they pass. The same seed always gives the
 * same track.
 */

private enum class Mode { WALK, WAIT, BUS, TRAIN }

private class Fix(val location: LngLatAlt, val speed: Double, val bearing: Double?, val mode: Mode)

private class Stop(val name: String, val location: LngLatAlt)

// A grid of 9 x 9 z14 tiles (about 12 km across) around the city centre holds over 50 railway
// stations. Every stop the track uses is kept well inside it, so routes never run off the edge.
private val GRID_CENTRE = LngLatAlt(-4.2518, 55.8642)
private const val GRID_SIZE_TILES = 9
private const val MAX_FROM_CENTRE_M = 4500.0

private const val WALK_SPEED = 1.4
private const val BUS_SPEED = 10.0
private const val BUS_ACCELERATION = 1.0
private const val TRAIN_SPEED = 22.0
private const val TRAIN_ACCELERATION = 0.6

private val busRoadCost = mapOf(
    "primary" to 1.0, "trunk" to 1.0, "secondary" to 1.1, "tertiary" to 1.3, "minor" to 2.0
)
private val noWalking = setOf("motorway", "raceway")

/** The point on [way] nearest a location: between coordinates [index] and [index] + 1. */
private class Snap(val way: Way, val point: LngLatAlt, val index: Int)

/**
 * A* over the Ways of one network, from and to arbitrary points. Unlike findShortestDistance it
 * doesn't add temporary Ways to the graph, it can leave Ways out (a bus keeps off footpaths even
 * though they share intersections), and it can forbid sharp turns, because a train can't take
 * a junction backwards.
 */
private class Router(
    private val ruler: Ruler,
    private val tree: FeatureTree,
    allow: (Way) -> Boolean,
    cost: (Way) -> Double = { 1.0 },
    private val maxTurnDegrees: Double = 179.0,
) {
    // The grid links its tiles with JOINER Ways, which have no class of their own and are under a
    // metre long. Every network crosses them, and they carry the heading straight through.
    private val allow: (Way) -> Boolean = { it.wayType == WayType.JOINER || allow(it) }
    private val cost: (Way) -> Double = { if (it.wayType == WayType.JOINER) 1.0 else cost(it) }

    private class State(val node: Intersection, val via: Way) {
        override fun equals(other: Any?) = other is State && other.node === node && other.via === via
        override fun hashCode() = 31 * System.identityHashCode(node) + System.identityHashCode(via)
    }

    private class Entry(val estimate: Double, val cost: Double, val state: State?, val parent: State?,
                        val piece: List<LngLatAlt>, val finish: Boolean)

    /** The nearest few Ways to a location, nearest first. */
    private fun snaps(location: LngLatAlt): List<Snap> =
        tree.getNearestCollection(location, 200.0, 3, ruler,
            include = { it is Way && it.wayType != WayType.JOINER && allow(it) }).features
            .map { way ->
                val nearest = ruler.distanceToLineString(location, way.geometry as LineString)
                Snap(way as Way, nearest.point, nearest.index)
            }

    /**
     * The route from one point to another, or null. The nearest Way isn't always connected to
     * anything (a station's own platform path, say), so it tries the next nearest at each end.
     */
    fun route(from: LngLatAlt, to: LngLatAlt): List<LngLatAlt>? {
        val ends = snaps(to)
        for (start in snaps(from)) {
            for (end in ends) {
                route(start, end)?.let { return it }
            }
        }
        return null
    }

    private fun route(start: Snap, end: Snap): List<LngLatAlt>? {
        val queue = PriorityQueue<Entry>(compareBy { it.estimate })
        val best = HashMap<State, Double>()
        val previous = HashMap<State, Pair<State?, List<LngLatAlt>>>()
        val heading = HashMap<State, Double?>()
        val settled = HashSet<State>()

        fun push(state: State, parent: State?, piece: List<LngLatAlt>, total: Double) {
            if (total >= (best[state] ?: Double.MAX_VALUE)) return
            best[state] = total
            queue.add(Entry(total + ruler.distance(state.node.location, end.point), total, state, parent, piece, false))
        }

        // Leave the start along its Way, either way.
        val startCoords = coords(start.way)
        val towardsEnd = listOf(start.point) + startCoords.drop(start.index + 1)
        val towardsStart = listOf(start.point) + startCoords.take(start.index + 1).reversed()
        start.way.intersections[WayEnd.END.id]?.let {
            push(State(it, start.way), null, towardsEnd, length(towardsEnd) * cost(start.way))
        }
        start.way.intersections[WayEnd.START.id]?.let {
            push(State(it, start.way), null, towardsStart, length(towardsStart) * cost(start.way))
        }
        if (start.way === end.way) {
            val direct = slice(startCoords, start, end)
            queue.add(Entry(length(direct), length(direct), null, null, direct, true))
        }

        val endCoords = coords(end.way)
        var expansions = 0
        while (queue.isNotEmpty() && expansions++ < 2_000_000) {
            val entry = queue.poll()!!
            if (entry.finish) return assemble(entry.parent, entry.piece, previous)
            val state = entry.state!!
            if (!settled.add(state)) continue
            previous[state] = entry.parent to entry.piece
            val arriving = arrivalHeading(entry.piece) ?: entry.parent?.let { heading[it] }
            heading[state] = arriving

            // Finish along the end's Way, from whichever of its ends this is.
            for ((endId, tail) in listOf(
                WayEnd.START.id to endCoords.take(end.index + 1) + end.point,
                WayEnd.END.id to endCoords.drop(end.index + 1).reversed() + end.point,
            )) {
                if (end.way.intersections[endId] === state.node && end.way !== state.via &&
                    turnAllowed(arriving, tail)) {
                    val total = entry.cost + length(tail) * cost(end.way)
                    queue.add(Entry(total, total, null, state, tail, true))
                }
            }

            for (way in state.node.members) {
                if (way === state.via || !allow(way)) continue
                val forwards = way.intersections[WayEnd.START.id] === state.node
                val next = way.intersections[if (forwards) WayEnd.END.id else WayEnd.START.id] ?: continue
                val piece = if (forwards) coords(way) else coords(way).reversed()
                if (!turnAllowed(arriving, piece)) continue
                push(State(next, way), state, piece, entry.cost + way.length * cost(way))
            }
        }
        return null
    }

    private fun coords(way: Way) = (way.geometry as LineString).coordinates

    private fun length(line: List<LngLatAlt>) =
        line.zipWithNext().sumOf { (a, b) -> ruler.distance(a, b) }

    /** Along [coords] from [a] to [b], both on the same Way, in whichever direction that is. */
    private fun slice(coords: List<LngLatAlt>, a: Snap, b: Snap): List<LngLatAlt> {
        val aFirst = a.index < b.index ||
            (a.index == b.index && ruler.distance(coords[a.index], a.point) <= ruler.distance(coords[a.index], b.point))
        return if (aFirst) listOf(a.point) + coords.subList(a.index + 1, b.index + 1) + b.point
        else listOf(a.point) + coords.subList(b.index + 1, a.index + 1).reversed() + b.point
    }

    private fun arrivalHeading(piece: List<LngLatAlt>): Double? {
        val last = piece.last()
        val before = piece.lastOrNull { ruler.distance(it, last) > 1.0 } ?: return null
        return ruler.bearing(before, last)
    }

    private fun turnAllowed(arriving: Double?, piece: List<LngLatAlt>): Boolean {
        if (arriving == null) return true
        val first = piece.first()
        val after = piece.firstOrNull { ruler.distance(first, it) > 1.0 } ?: return true
        return turnAngle(arriving, ruler.bearing(first, after)) <= maxTurnDegrees
    }

    private fun assemble(
        parent: State?,
        tail: List<LngLatAlt>,
        previous: Map<State, Pair<State?, List<LngLatAlt>>>
    ): List<LngLatAlt> {
        val pieces = mutableListOf(tail)
        var state = parent
        while (state != null) {
            val (before, piece) = previous.getValue(state)
            pieces.add(piece)
            state = before
        }
        val line = mutableListOf<LngLatAlt>()
        for (piece in pieces.asReversed()) {
            for (p in piece) if (line.isEmpty() || ruler.distance(line.last(), p) > 0.01) line.add(p)
        }
        return line
    }
}

private fun turnAngle(a: Double, b: Double) = abs(((b - a) % 360 + 540) % 360 - 180)

/** Positions along a line by distance from its start. */
private class Polyline(val ruler: Ruler, val points: List<LngLatAlt>) {
    val cumulative = points.zipWithNext().runningFold(0.0) { d, (a, b) -> d + ruler.distance(a, b) }
    val length: Double get() = cumulative.last()

    private fun segment(d: Double) =
        cumulative.indexOfLast { it <= d }.coerceIn(0, points.size - 2)

    fun at(d: Double): LngLatAlt {
        val i = segment(d)
        val seg = cumulative[i + 1] - cumulative[i]
        val f = if (seg > 0) ((d - cumulative[i]) / seg).coerceIn(0.0, 1.0) else 0.0
        val (a, b) = points[i] to points[i + 1]
        return LngLatAlt(a.longitude + (b.longitude - a.longitude) * f,
                         a.latitude + (b.latitude - a.latitude) * f)
    }

    fun bearing(d: Double): Double {
        var i = segment(d)
        while (i < points.size - 2 && cumulative[i + 1] - cumulative[i] < 0.1) i++
        return (ruler.bearing(points[i], points[i + 1]) + 360) % 360
    }

    /** How far along the line a point (near it) lies. */
    fun distanceAlong(p: LngLatAlt): Double {
        val nearest = ruler.distanceToLineString(p, LineString(*points.toTypedArray()))
        return cumulative[nearest.index] + ruler.distance(points[nearest.index], nearest.point)
    }
}

class FarmTrackGenerator {

    private lateinit var ruler: Ruler
    private lateinit var random: Random
    private lateinit var walk: Router
    private lateinit var bus: Router
    private lateinit var train: Router
    // Every stop in the grid, which buses and trains call at...
    private lateinit var allBusStops: List<Stop>
    private lateinit var allStations: List<Stop>
    // ...and those well inside it, where journeys start and end.
    private lateinit var busStops: List<Stop>
    private lateinit var stations: List<Stop>
    private val fixes = mutableListOf<Fix>()
    private val waypoints = mutableListOf<Pair<Stop, String>>()

    @Test
    fun generate() {
        val out = System.getProperty("farmTrack")
        assumeTrue("Set -PfarmTrack=<file.gpx> to generate a test farm track", !out.isNullOrBlank())
        val seed = System.getProperty("farmTrackSeed")?.toLongOrNull() ?: 1L
        val journeys = System.getProperty("farmTrackJourneys")?.toIntOrNull() ?: 6

        val gridState = FileGridState(MAX_ZOOM_LEVEL, GRID_SIZE_TILES)
        gridState.start(offlineExtractPath)
        runBlocking { gridState.locationUpdate(GRID_CENTRE, CalloutPoiSelection.EVERYTHING, null) }
        ruler = gridState.ruler
        random = Random(seed)

        walk = Router(ruler, gridState.getFeatureTree(TreeId.ROADS_AND_PATHS),
            allow = { it.featureValue !in noWalking && it.featureValue?.endsWith("_construction") != true })
        bus = Router(ruler, gridState.getFeatureTree(TreeId.ROADS),
            allow = { it.featureValue in busRoadCost }, cost = { busRoadCost.getValue(it.featureValue!!) },
            maxTurnDegrees = 135.0)
        train = Router(ruler, gridState.getFeatureTree(TreeId.TRANSIT),
            allow = { it.featureType == "rail" }, maxTurnDegrees = 40.0)

        val stops = gridState.getFeatureTree(TreeId.TRANSIT_STOPS).getAllCollection().features
            .filterIsInstance<MvtFeature>()
            .filter { it.geometry is Point && !it.name.isNullOrBlank() }
        allBusStops = stops.filter { it.featureValue == "bus_stop" }.map { Stop(it.name!!, (it.geometry as Point).coordinates) }
        allStations = stops.filter { it.featureType == "railway" && it.featureValue == "station" }
            .map { Stop(it.name!!, (it.geometry as Point).coordinates) }
        busStops = allBusStops.filter { ruler.distance(GRID_CENTRE, it.location) < MAX_FROM_CENTRE_M }
        stations = allStations.filter { ruler.distance(GRID_CENTRE, it.location) < MAX_FROM_CENTRE_M }
        println("Farm track: ${busStops.size} bus stops and ${stations.size} stations, seed $seed")

        var here = GRID_CENTRE
        for (journey in 0 until journeys) {
            here = if (journey % 2 == 0) busJourney(here) else trainJourney(here)
        }

        val file = File(out!!)
        writeGpx(file, seed)
        val minutes = fixes.size / 60
        println("Farm track: wrote ${fixes.size} fixes (${minutes / 60}h${"%02d".format(minutes % 60)}m) to $file")
        for (mode in Mode.entries) {
            println("  $mode: ${fixes.count { it.mode == mode } / 60} min")
        }
    }

    private fun busJourney(from: LngLatAlt): LngLatAlt {
        repeat(50) {
            val board = busStops.filter { ruler.distance(from, it.location) in 200.0..900.0 }.randomOrNull(random)
                ?: return@repeat
            val alight = busStops.filter { ruler.distance(board.location, it.location) in 1500.0..4000.0 }
                .randomOrNull(random) ?: return@repeat
            val ride = bus.route(board.location, alight.location) ?: return@repeat
            // Bus stops sit beside the road, a few metres from where the bus stops.
            val line = Polyline(ruler, listOf(board.location) + ride + alight.location)
            if (line.length > 2.0 * ruler.distance(board.location, alight.location)) return@repeat
            val walkThere = walk.route(from, board.location) ?: return@repeat

            val calls = stopsAlong(line, allBusStops, 15.0, minGap = 150.0, exclude = setOf(board.name, alight.name))
            println("Bus: ${board.name} to ${alight.name}, ${line.length.toInt()} m, ${calls.size} stops between")
            walkAlong(listOf(from) + walkThere + board.location)
            wait(board.location, random.nextInt(60, 300))
            waypoints.add(board to "Wait for bus")
            rideAlong(line, calls, Mode.BUS, BUS_SPEED, BUS_ACCELERATION) { random.nextInt(15, 40) }
            waypoints.add(alight to "Get off bus")
            return alight.location
        }
        error("No bus journey found from $from")
    }

    private fun trainJourney(from: LngLatAlt): LngLatAlt {
        val nearby = stations.filter { ruler.distance(from, it.location) in 200.0..1500.0 }
            .ifEmpty { stations.sortedBy { ruler.distance(from, it.location) }.take(2) }
        repeat(50) {
            val board = nearby.random(random)
            val alight = stations.filter { ruler.distance(board.location, it.location) in 1500.0..6000.0 }
                .randomOrNull(random) ?: return@repeat
            val ride = train.route(board.location, alight.location) ?: return@repeat
            val line = Polyline(ruler, ride)
            if (line.length > 1.8 * ruler.distance(board.location, alight.location)) return@repeat
            val walkThere = walk.route(from, board.location) ?: return@repeat

            val calls = stopsAlong(line, allStations, 100.0, minGap = 400.0, exclude = setOf(board.name, alight.name))
            println("Train: ${board.name} to ${alight.name}, ${line.length.toInt()} m, " +
                if (calls.isEmpty()) "no stops between" else "calling at ${calls.joinToString { it.second.name }}")
            // A station's point can be 100 m from the track, so walk between it and the platform.
            walkAlong(listOf(from) + walkThere + board.location + ride.first())
            wait(ride.first(), random.nextInt(120, 360))
            waypoints.add(board to "Wait for train")
            rideAlong(line, calls, Mode.TRAIN, TRAIN_SPEED, TRAIN_ACCELERATION) { random.nextInt(30, 60) }
            waypoints.add(alight to "Get off train")
            walkAlong(listOf(ride.last(), alight.location))
            return alight.location
        }
        error("No train journey found from $from")
    }

    /** The stops within [radius] of the line, by distance along it, at least [minGap] apart. */
    private fun stopsAlong(
        line: Polyline, candidates: List<Stop>, radius: Double, minGap: Double, exclude: Set<String>
    ): List<Pair<Double, Stop>> {
        val shape = LineString(*line.points.toTypedArray())
        val near = candidates
            .filter { it.name !in exclude && ruler.distanceToLineString(it.location, shape).distance <= radius }
            .map { line.distanceAlong(it.location) to it }
            .filter { (d, _) -> d > minGap && d < line.length - minGap }
            .sortedBy { it.first }
        val kept = mutableListOf<Pair<Double, Stop>>()
        for (stop in near) if (kept.isEmpty() || stop.first - kept.last().first >= minGap) kept.add(stop)
        return kept
    }

    private fun walkAlong(points: List<LngLatAlt>) {
        val line = Polyline(ruler, points.zipWithNext().filter { (a, b) -> ruler.distance(a, b) > 0.01 }
            .map { it.second }.let { listOf(points.first()) + it })
        if (line.points.size < 2) return
        var d = 0.0
        while (d < line.length) {
            fixes.add(Fix(line.at(d), WALK_SPEED, line.bearing(d), Mode.WALK))
            d += WALK_SPEED
        }
    }

    private fun wait(location: LngLatAlt, seconds: Int) {
        repeat(seconds) { fixes.add(Fix(location, 0.0, null, Mode.WAIT)) }
    }

    /**
     * Drives along [line], speeding up and slowing down at [acceleration] m/s², stopping at
     * each of [calls] for [dwell] seconds and at the end.
     */
    private fun rideAlong(
        line: Polyline, calls: List<Pair<Double, Stop>>, mode: Mode,
        topSpeed: Double, acceleration: Double, dwell: () -> Int
    ) {
        var d = 0.0
        for ((target, stop) in calls.map { it.first to it.second as Stop? } + (line.length to null)) {
            var v = 0.0
            while (target - d > 0.01) {
                val remaining = target - d
                v = min(min(topSpeed, v + acceleration), sqrt(2 * acceleration * remaining))
                v = maxOf(v, 0.5)
                fixes.add(Fix(line.at(d), v, line.bearing(d), mode))
                d += min(v, remaining)
            }
            if (stop != null) {
                waypoints.add(stop to "Stop")
                repeat(dwell()) { fixes.add(Fix(line.at(d), 0.0, null, mode)) }
            }
        }
        fixes.add(Fix(line.at(line.length), 0.0, null, mode))
    }

    private fun writeGpx(file: File, seed: Long) {
        val start = Instant.parse("2026-01-01T09:00:00Z")
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"FarmTrackGenerator\">\n")
        // farm.py replays a track by its timestamps only when it's marked like this. Real
        // recordings have timestamps too, but start with minutes of standing-still GPS jitter.
        sb.append("<metadata><desc>Synthetic walk, bus and train track from glasgow-gb.pmtiles, seed $seed</desc>")
        sb.append("<keywords>farm-timed-replay</keywords></metadata>\n")
        for ((stop, label) in waypoints) {
            sb.append("<wpt lat=\"%.6f\" lon=\"%.6f\"><name>%s</name><type>%s</type></wpt>\n".format(
                stop.location.latitude, stop.location.longitude, xml(stop.name), label))
        }
        sb.append("<trk><name>Glasgow walk, bus and train</name><trkseg>\n")
        for ((i, fix) in fixes.withIndex()) {
            sb.append("<trkpt lat=\"%.6f\" lon=\"%.6f\"><time>%s</time><speed>%.1f</speed>".format(
                fix.location.latitude, fix.location.longitude, start.plusSeconds(i.toLong()), fix.speed))
            fix.bearing?.let { sb.append("<bearing>%.0f</bearing>".format(it)) }
            sb.append("<type>${fix.mode.name.lowercase()}</type></trkpt>\n")
        }
        sb.append("</trkseg></trk>\n</gpx>\n")
        file.absoluteFile.parentFile?.mkdirs()
        file.writeText(sb.toString())
    }

    private fun xml(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
