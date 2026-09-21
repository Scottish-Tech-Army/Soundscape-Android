package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.parseGpx

class GpxParserTest {

    @Test
    fun soundscapeRoutePoints() {
        val gpx = parseGpx(javaClass.getResource("/gpx/soundscape.gpx")!!.readText())
        assert(gpx.metadata.name == "Soundscape") { "Expected name 'Soundscape', got '${gpx.metadata.name}'" }
        assert(gpx.metadata.desc == "Soundscape description") { "Expected desc" }
        assert(gpx.routes.size == 1) { "Expected 1 route, got ${gpx.routes.size}" }
        assert(gpx.routes[0].routePoints.size == 8) { "Expected 8 route points, got ${gpx.routes[0].routePoints.size}" }
        val first = gpx.routes[0].routePoints[0]
        assert(first.latitude == 55.947256) { "Expected lat 55.947256, got ${first.latitude}" }
        assert(first.longitude == -4.305852) { "Expected lon -4.305852, got ${first.longitude}" }
        assert(first.name == "Waypoint") { "Expected name 'Waypoint', got '${first.name}'" }
    }

    @Test
    fun rideWithGpsWaypoints() {
        val gpx = parseGpx(javaClass.getResource("/gpx/rideWithGps.gpx")!!.readText())
        assert(gpx.metadata.name == "RideWithGps") { "Expected name 'RideWithGps', got '${gpx.metadata.name}'" }
        assert(gpx.waypoints.size == 9) { "Expected 9 waypoints, got ${gpx.waypoints.size}" }
        assert(gpx.tracks.size == 1) { "Expected 1 track, got ${gpx.tracks.size}" }

        val wpt = gpx.waypoints[0]
        assert(wpt.latitude == 55.94722) { "Expected lat 55.94722, got ${wpt.latitude}" }
        assert(wpt.longitude == -4.30844) { "Expected lon -4.30844, got ${wpt.longitude}" }
        assert(wpt.name == "Slight Left") { "Expected name 'Slight Left', got '${wpt.name}'" }
        assert(wpt.desc == "Turn slight left") { "Expected desc 'Turn slight left', got '${wpt.desc}'" }

        val track = gpx.tracks[0]
        assert(track.trackName == "Test") { "Expected track name 'Test'" }
        assert(track.trackSegments.size == 1) { "Expected 1 segment" }
        assert(track.trackSegments[0].trackPoints.size == 43) { "Expected 43 track points, got ${track.trackSegments[0].trackPoints.size}" }
        val tp = track.trackSegments[0].trackPoints[0]
        assert(tp.ele == 85.0) { "Expected ele 85.0, got ${tp.ele}" }
    }

    @Test
    fun handcraftedRoutePoints() {
        val gpx = parseGpx(javaClass.getResource("/gpx/handcrafted.gpx")!!.readText())
        assert(gpx.metadata.name == "Handcrafted") { "Expected name 'Handcrafted'" }
        assert(gpx.metadata.desc == "Handcrafted description") { "Expected desc" }
        assert(gpx.routes.size == 1) { "Expected 1 route" }
        assert(gpx.routes[0].routePoints.size == 5) { "Expected 5 route points" }
        val george = gpx.routes[0].routePoints[0]
        assert(george.name == "George Square, Glasgow") { "Expected George Square" }
        assert(george.latitude == 55.8610697)
        assert(george.longitude == -4.2499327)
    }

    @Test
    fun trackPointsWithExtras() {
        val gpx = parseGpx(javaClass.getResource("/gpx/milngavie-centre.gpx")!!.readText())
        assert(gpx.tracks.size == 1) { "Expected 1 track" }
        val segment = gpx.tracks[0].trackSegments[0]
        assert(segment.trackPoints.isNotEmpty()) { "Expected track points" }
        val tp = segment.trackPoints[0]
        assert(tp.time != null) { "Expected time" }
        assert(tp.speed != null) { "Expected speed" }
        assert(tp.bearing != null) { "Expected bearing" }
    }
}
