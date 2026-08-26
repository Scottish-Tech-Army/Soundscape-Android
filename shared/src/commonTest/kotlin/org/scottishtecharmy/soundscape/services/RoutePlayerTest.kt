package org.scottishtecharmy.soundscape.services

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// NOTE on org.jetbrains.compose.resources.getString(): RoutePlayer calls it (directly, and
// indirectly through formatDistanceAndDirection -> ComposeLocalizedStrings.get(), which needs a
// decimal separator from the platform's resource environment) from several places:
//   - createBeaconAtWaypoint(): whenever MediaControllableService.filteredLocationFlow.value is
//     non-null at the moment a beacon is (re)created, to build the "next flag"/distance callout.
//   - startMonitoringLocation()'s collect body, when the final waypoint is reached, to build the
//     "route complete" callout.
//   - startRoute(reverse = true), to build the "(Reversed)" route name - unconditionally, not
//     gated on any location.
// Under shared:testAndroidHostTest, compose-resources' getString() throws
// ("Method getSystem in android.content.res.Resources not mocked") because there's no real
// Android resource environment (see AddAndEditRouteViewModelTest.kt for the same limitation
// hit previously). This is a JVM-host-test-only artifact - on a real device/emulator it works
// fine - so these tests are written to keep filteredLocationFlow's *value* reads null at the
// exact moments RoutePlayer would otherwise call getString synchronously (which would fail the
// test outright, since play()/startBeacon() call createBeaconAtWaypoint() directly rather than
// through a launched coroutine). Where a call is dispatched via RoutePlayer's own
// `coroutineScope.launch { ... }` (moveToNext/moveToPrevious's beacon-recreation, startRoute's
// initial play()), an incidental getString crash there is silent/asynchronous and doesn't fail
// the test, but it also means we can't observe the TTS text it would have produced.
// Route-reversal (startRoute(reverse = true)) is consequently *entirely* untestable here: the
// reversed RouteWithMarkers is only constructed after the unconditional getString() call
// succeeds, so under this environment the launched coroutine dies before currentRouteFlow (or
// currentRouteData) is ever updated - there is no synthetic-data trick that avoids this, since
// the reversal itself is inside the same expression as the getString call. No test for it is
// included below; this comment documents why, matching the precedent in
// AddAndEditRouteViewModelTest.kt.

private class FakeRouteDao : RouteDao {
    private var nextMarkerId = 1L
    private var nextRouteId = 1L
    val markersFlow = MutableStateFlow<List<MarkerEntity>>(emptyList())
    val routesFlow = MutableStateFlow<List<RouteEntity>>(emptyList())
    val crossRefs = mutableListOf<RouteMarkerCrossRef>()

    override suspend fun insertMarker(marker: MarkerEntity): Long {
        val id = if (marker.markerId != 0L) marker.markerId else nextMarkerId++
        val stored =
            MarkerEntity(id, marker.name, marker.longitude, marker.latitude, marker.fullAddress)
        markersFlow.value = markersFlow.value.filterNot { it.markerId == id } + stored
        return id
    }

    override suspend fun updateMarker(marker: MarkerEntity) {}
    override suspend fun getMarkerById(markerId: Long): MarkerEntity? =
        markersFlow.value.find { it.markerId == markerId }

    override suspend fun getMarkerByLocation(longitude: Double, latitude: Double): MarkerEntity? =
        null

    override suspend fun getAllMarkers(): List<MarkerEntity> = markersFlow.value
    override fun getAllMarkersFlow() = markersFlow

    override suspend fun insertRoute(route: RouteEntity): Long {
        val id = if (route.routeId != 0L) route.routeId else nextRouteId++
        val stored = RouteEntity(id, route.name, route.description)
        routesFlow.value = routesFlow.value.filterNot { it.routeId == id } + stored
        return id
    }

    override suspend fun addMarkerToRoute(crossRef: RouteMarkerCrossRef) {
        crossRefs.add(crossRef)
    }

    override suspend fun removeMarkerFromRoute(routeId: Long, markerId: Long) {}
    override suspend fun removeMarkersForRoute(routeId: Long) {}
    override suspend fun getAllRoutes(): List<RouteEntity> = routesFlow.value
    override suspend fun getRouteById(routeId: Long): RouteEntity? =
        routesFlow.value.find { it.routeId == routeId }

    override suspend fun getMarkerCrossReference(routeId: Long): List<RouteMarkerCrossRef> =
        crossRefs.filter { it.routeId == routeId }

    override fun getAllRoutesFlow() = routesFlow
    override suspend fun removeRoute(routeId: Long) {}
    override suspend fun removeMarker(markerId: Long) {}
    override suspend fun deleteAllRouteMarkerCrossRefs() {}
    override suspend fun deleteAllMarkers() {}
    override suspend fun deleteAllRoutes() {}

    /** Convenience helper mirroring how a real route + ordered markers gets built up. */
    suspend fun createRoute(name: String, markers: List<MarkerEntity>): Long {
        val routeId = insertRoute(RouteEntity(name = name, description = "desc"))
        markers.forEachIndexed { index, marker ->
            val markerId = insertMarker(marker)
            addMarkerToRoute(RouteMarkerCrossRef(routeId, markerId, index))
        }
        return routeId
    }
}

/**
 * A [StateFlow] whose [value] reads are scripted: it pops through [reads] one at a time as
 * production code re-reads `.value`, then sticks on the last entry (or null, if none were
 * given). This lets a test hand RoutePlayer's initial "where am I" checks a real location while
 * ensuring any *later* `.value` read - in particular createBeaconAtWaypoint()'s, which would
 * otherwise call the untestable getString()/formatDistanceAndDirection() path - sees null and
 * skips it (see the file-level comment above).
 *
 * [collect] (used by startMonitoringLocation()) is independent of the scripted [value] reads -
 * it delegates to a real backing MutableStateFlow that tests drive explicitly via [emit], to
 * simulate later GPS fixes arriving once monitoring is already active.
 */
@OptIn(kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi::class)
private class TestLocationFlow(
    vararg reads: SoundscapeLocation?,
) : StateFlow<SoundscapeLocation?> {
    private val pending = ArrayDeque(reads.toList())
    private val backing = MutableStateFlow<SoundscapeLocation?>(null)

    override val value: SoundscapeLocation?
        get() = if (pending.size > 1) pending.removeFirst() else pending.firstOrNull()

    override val replayCache: List<SoundscapeLocation?> get() = backing.replayCache

    override suspend fun collect(collector: FlowCollector<SoundscapeLocation?>): Nothing {
        backing.collect(collector)
    }

    fun emit(location: SoundscapeLocation?) {
        backing.value = location
    }
}

private class FakeMediaControllableService(
    override val filteredLocationFlow: TestLocationFlow = TestLocationFlow(),
) : MediaControllableService {
    val createBeaconCalls = mutableListOf<Pair<LngLatAlt, Boolean>>()
    var destroyBeaconCallCount = 0
    val speakTextCalls = mutableListOf<String>()
    var clearQueueCallCount = 0

    override fun routeMute(): Boolean = false
    override fun routeSkipNext(): Boolean = false
    override fun routeSkipPrevious(): Boolean = false
    override fun myLocation() {}
    override fun whatsAroundMe() {}

    override fun speakText(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ) {
        speakTextCalls.add(text)
    }

    override fun clearTextToSpeechQueue() {
        clearQueueCallCount++
    }

    override fun createBeacon(location: LngLatAlt?, headingOnly: Boolean) {
        if (location != null) createBeaconCalls.add(location to headingOnly)
    }

    override fun destroyBeacon() {
        destroyBeaconCallCount++
    }

    override var menuActive: Boolean = false
    override fun speak2dText(text: String, clearQueue: Boolean, earcon: String?) {}
    override fun callbackHoldOff() {}
    override fun requestAudioFocus(): Boolean = true
    override fun aheadOfMe() {}
    override fun nearbyMarkers() {}
    override fun routeStop() {}
    override fun routeStartById(routeId: Long) {}
    override fun startBeacon(location: LngLatAlt, name: String) {}

    override val locationFlow: StateFlow<SoundscapeLocation?> = MutableStateFlow(null)
    override val orientationFlow: StateFlow<DeviceDirection?> = MutableStateFlow(null)
    override val beaconFlow: StateFlow<BeaconState> = MutableStateFlow(BeaconState())
    override val currentRouteFlow: StateFlow<RoutePlayerState> = MutableStateFlow(RoutePlayerState())
    override val gridStateFlow: StateFlow<GridState?> = MutableStateFlow(null)

    override fun routeStartReverse(routeId: Long) {}
    override fun getLocationDescription(location: LngLatAlt): LocationDescription =
        LocationDescription(location = location)

    override suspend fun searchResult(query: String): List<LocationDescription>? = null
    override fun isAudioEngineBusy(): Boolean = false
    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long = 0L
}

// Waypoints spaced ~222m apart (0.002 degrees latitude) - far enough apart that the "within 12m"
// arrival threshold and the "within 30m" beacon-only threshold can't be ambiguous between them.
private val MARKER_A = MarkerEntity(name = "A", longitude = -4.2500, latitude = 55.8600)
private val MARKER_B = MarkerEntity(name = "B", longitude = -4.2500, latitude = 55.8620)
private val MARKER_C = MarkerEntity(name = "C", longitude = -4.2500, latitude = 55.8640)

/** ~5.6m from [marker] - well inside the 12m "arrived" threshold. */
private fun nearLocation(marker: MarkerEntity) =
    SoundscapeLocation(latitude = marker.latitude + 0.00005, longitude = marker.longitude)

/** ~111m from [location] - well outside the 30m beacon-only threshold. */
private fun farFrom(location: LngLatAlt) =
    SoundscapeLocation(latitude = location.latitude + 0.001, longitude = location.longitude)

class RoutePlayerTest {

    private suspend fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!condition()) delay(5)
        }
    }

    // ----- startBeacon -----

    @Test
    fun startBeacon_noCurrentLocation_isTreatedAsBeaconOnlyAndDoesNotMonitor() = runBlocking {
        val service = FakeMediaControllableService() // filteredLocationFlow always null
        val player = RoutePlayer(service, FakeRouteDao())
        val beaconLocation = LngLatAlt(-4.30, 55.90)

        player.startBeacon(beaconLocation, "Cafe")

        val state = player.currentRouteFlow.value
        assertTrue(state.beaconOnly)
        assertEquals(0, state.currentWaypoint)
        assertEquals(1, state.routeData?.markers?.size)
        assertEquals("Cafe", state.routeData?.markers?.get(0)?.name)
        assertTrue(player.isPlaying())
        assertEquals(listOf(beaconLocation to false), service.createBeaconCalls)

        // No monitoring job was started (beaconOnly stayed true), so a "GPS fix" arriving right
        // next to the beacon has no effect at all - unlike the far-away case below.
        service.filteredLocationFlow.emit(nearLocation(MarkerEntity(name = "x", longitude = beaconLocation.longitude, latitude = beaconLocation.latitude)))
        delay(200)
        assertEquals(0, service.destroyBeaconCallCount)
        assertTrue(player.isPlaying())
    }

    /**
     * startBeacon()'s exposed currentRouteFlow.beaconOnly must reflect the same >30m distance
     * check used internally to decide whether to call startMonitoringLocation() - a far-away
     * start point is distance-tracked, so beaconOnly must be false, not hardcoded true
     * regardless of distance.
     */
    @Test
    fun startBeacon_farCurrentLocation_reportsBeaconOnlyFalseInState() = runBlocking {
        val beaconLocation = LngLatAlt(-4.30, 55.90)
        val currentLocation = farFrom(beaconLocation)
        // First .value read (the internal, correctly-computed distance check) sees the real,
        // far-away location; every read after that (createBeaconAtWaypoint's) sees null - see
        // the TestLocationFlow doc comment for why.
        val service = FakeMediaControllableService(TestLocationFlow(currentLocation, null))
        val player = RoutePlayer(service, FakeRouteDao())

        player.startBeacon(beaconLocation, "Distant cafe")

        val state = player.currentRouteFlow.value
        assertFalse(state.beaconOnly)
        assertTrue(player.isPlaying())
        assertEquals(listOf(beaconLocation to false), service.createBeaconCalls)
    }

    // ----- startRoute -----

    @Test
    fun startRoute_loadsRouteAndPlaysFirstWaypoint() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B, MARKER_C))
        val service = FakeMediaControllableService()
        val player = RoutePlayer(service, dao)

        player.startRoute(routeId)
        waitUntil { player.currentRouteFlow.value.routeData != null }

        val state = player.currentRouteFlow.value
        assertEquals("Loop", state.routeData?.route?.name)
        assertEquals(listOf("A", "B", "C"), state.routeData?.markers?.map { it.name })
        assertEquals(0, state.currentWaypoint)
        assertFalse(state.beaconOnly)
        assertFalse(state.reverse)
        assertTrue(player.isPlaying())

        // play() -> createBeaconAtWaypoint(0, ...) is dispatched via startRoute's own launch.
        waitUntil { service.createBeaconCalls.isNotEmpty() }
        assertEquals(MARKER_A.getLngLatAlt(), service.createBeaconCalls[0].first)
    }

    @Test
    fun startRoute_resumesAtGivenStartWaypoint() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B, MARKER_C))
        val player = RoutePlayer(FakeMediaControllableService(), dao)

        player.startRoute(routeId, startWaypoint = 1)
        waitUntil { player.currentRouteFlow.value.routeData != null }

        assertEquals(1, player.currentRouteFlow.value.currentWaypoint)
    }

    @Test
    fun startRoute_startWaypointOutOfRange_isCoerced() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B, MARKER_C))
        val player = RoutePlayer(FakeMediaControllableService(), dao)

        player.startRoute(routeId, startWaypoint = 100)
        waitUntil { player.currentRouteFlow.value.routeData != null }
        assertEquals(2, player.currentRouteFlow.value.currentWaypoint) // markers.size - 1

        player.stopRoute()
        player.startRoute(routeId, startWaypoint = -5)
        waitUntil { player.currentRouteFlow.value.routeData != null }
        assertEquals(0, player.currentRouteFlow.value.currentWaypoint)
    }

    @Test
    fun startRoute_unknownRouteId_leavesStateUnset() = runBlocking {
        val player = RoutePlayer(FakeMediaControllableService(), FakeRouteDao())

        player.startRoute(12345L)
        delay(200) // nothing to poll for - route load returns null and the coroutine just exits

        assertNull(player.currentRouteFlow.value.routeData)
        assertFalse(player.isPlaying())
    }

    // ----- moveToNext / moveToPrevious -----

    @Test
    fun moveToNext_advancesThroughWaypointsAndReturnsTrue() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B, MARKER_C))
        val player = RoutePlayer(FakeMediaControllableService(), dao)
        player.startRoute(routeId)
        waitUntil { player.currentRouteFlow.value.routeData != null }

        assertTrue(player.moveToNext(userInitiated = true))
        assertEquals(1, player.currentRouteFlow.value.currentWaypoint)

        assertTrue(player.moveToNext(userInitiated = true))
        assertEquals(2, player.currentRouteFlow.value.currentWaypoint)
    }

    /**
     * At the last waypoint, moveToNext() must return false (making no change: currentWaypoint
     * doesn't move, no beacon is recreated) so a caller (e.g. a "skip to next" UI control) can
     * use the return value to detect "already at the end of the route" - matching
     * moveToPrevious()'s behaviour at the start boundary, tested just below.
     */
    @Test
    fun moveToNext_atLastWaypoint_returnsFalseAndDoesNotAdvance() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B))
        val player = RoutePlayer(FakeMediaControllableService(), dao)
        player.startRoute(routeId)
        waitUntil { player.currentRouteFlow.value.routeData != null }

        assertTrue(player.moveToNext(userInitiated = true)) // 0 -> 1 (last waypoint)
        assertEquals(1, player.currentRouteFlow.value.currentWaypoint)

        val result = player.moveToNext(userInitiated = true) // already at the last waypoint
        assertFalse(result)
        assertEquals(1, player.currentRouteFlow.value.currentWaypoint) // unchanged
    }

    @Test
    fun moveToPrevious_atFirstWaypoint_returnsFalseAndDoesNotChangeState() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B))
        val player = RoutePlayer(FakeMediaControllableService(), dao)
        player.startRoute(routeId)
        waitUntil { player.currentRouteFlow.value.routeData != null }

        assertFalse(player.moveToPrevious(userInitiated = true)) // already at waypoint 0
        assertEquals(0, player.currentRouteFlow.value.currentWaypoint)
    }

    @Test
    fun moveToPrevious_movesBackThroughWaypoints() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B, MARKER_C))
        val player = RoutePlayer(FakeMediaControllableService(), dao)
        player.startRoute(routeId)
        waitUntil { player.currentRouteFlow.value.routeData != null }
        player.moveToNext(true)
        player.moveToNext(true)
        assertEquals(2, player.currentRouteFlow.value.currentWaypoint)

        assertTrue(player.moveToPrevious(userInitiated = true))
        assertEquals(1, player.currentRouteFlow.value.currentWaypoint)
        assertTrue(player.moveToPrevious(userInitiated = true))
        assertEquals(0, player.currentRouteFlow.value.currentWaypoint)
    }

    @Test
    fun moveToNext_and_moveToPrevious_withSingleMarkerRoute_returnFalse() = runBlocking {
        val service = FakeMediaControllableService()
        val player = RoutePlayer(service, FakeRouteDao())
        player.startBeacon(LngLatAlt(-4.30, 55.90), "Solo")

        assertFalse(player.moveToNext(userInitiated = true))
        assertFalse(player.moveToPrevious(userInitiated = true))
        assertEquals(0, player.currentRouteFlow.value.currentWaypoint)
    }

    @Test
    fun moveToNext_and_moveToPrevious_withNoCurrentRoute_returnFalse() {
        val player = RoutePlayer(FakeMediaControllableService(), FakeRouteDao())

        assertFalse(player.moveToNext(userInitiated = true))
        assertFalse(player.moveToPrevious(userInitiated = true))
    }

    // ----- stopRoute -----

    @Test
    fun stopRoute_clearsStateAndDestroysBeacon() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B))
        val service = FakeMediaControllableService()
        val player = RoutePlayer(service, dao)
        player.startRoute(routeId)
        waitUntil { player.currentRouteFlow.value.routeData != null }

        player.stopRoute()

        assertEquals(1, service.destroyBeaconCallCount)
        assertNull(player.currentRouteFlow.value.routeData)
        assertEquals(0, player.currentRouteFlow.value.currentWaypoint)
        assertFalse(player.isPlaying())
    }

    // ----- waypoint advancement as location updates arrive -----

    @Test
    fun locationMonitoring_advancesWaypointAsLocationUpdatesArrive() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B, MARKER_C))
        val service = FakeMediaControllableService()
        val player = RoutePlayer(service, dao)
        player.startRoute(routeId)
        waitUntil { player.currentRouteFlow.value.routeData != null }
        assertEquals(0, player.currentRouteFlow.value.currentWaypoint)

        // Arriving near marker A (the current waypoint) with more of the route left to go
        // advances to marker B - this branch (unlike the final-waypoint one) never touches
        // getString(), so it's safely observable end-to-end here.
        service.filteredLocationFlow.emit(nearLocation(MARKER_A))
        waitUntil { player.currentRouteFlow.value.currentWaypoint == 1 }

        // And again, B -> C.
        service.filteredLocationFlow.emit(nearLocation(MARKER_B))
        waitUntil { player.currentRouteFlow.value.currentWaypoint == 2 }

        assertTrue(player.isPlaying())
    }

    @Test
    fun locationMonitoring_notStartedForBeaconOnlyRoute() = runBlocking {
        val service = FakeMediaControllableService()
        val player = RoutePlayer(service, FakeRouteDao())
        player.startBeacon(LngLatAlt(-4.30, 55.90), "Cafe") // no current location -> beaconOnly

        // Even a location "arriving" right on top of the beacon has no effect, because
        // startMonitoringLocation() was never called for a beaconOnly beacon.
        service.filteredLocationFlow.emit(SoundscapeLocation(latitude = 55.90, longitude = -4.30))
        delay(200)

        assertEquals(0, service.destroyBeaconCallCount)
        assertTrue(player.isPlaying())
        assertEquals(0, player.currentRouteFlow.value.currentWaypoint)
    }

    // ----- reaching the final waypoint / route completion -----
    //
    // Route completion normally happens automatically from within startMonitoringLocation()'s
    // collect body once the final waypoint is reached, but that branch calls getString() to
    // build the "route complete" callout - untestable here (see the file-level comment).
    // stopRoute() is the actual state-clearing/cleanup logic that branch calls into, so it's
    // exercised directly below, reached via moveToNext() (a plain public API, unaffected by the
    // getString limitation) walking to the last waypoint first.

    @Test
    fun reachingFinalWaypoint_thenStopRoute_endsPlayback() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B))
        val service = FakeMediaControllableService()
        val player = RoutePlayer(service, dao)
        player.startRoute(routeId)
        waitUntil { player.currentRouteFlow.value.routeData != null }

        player.moveToNext(userInitiated = false) // 0 -> 1, the final waypoint
        assertEquals(1, player.currentRouteFlow.value.currentWaypoint)

        player.stopRoute()

        assertNull(player.currentRouteFlow.value.routeData)
        assertFalse(player.isPlaying())
        assertEquals(1, service.destroyBeaconCallCount)
    }

    // ----- isPlaying / toString -----

    @Test
    fun isPlaying_reflectsWhetherARouteIsActive() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B))
        val player = RoutePlayer(FakeMediaControllableService(), dao)
        assertFalse(player.isPlaying())

        player.startRoute(routeId)
        waitUntil { player.currentRouteFlow.value.routeData != null }
        assertTrue(player.isPlaying())

        player.stopRoute()
        assertFalse(player.isPlaying())
    }

    @Test
    fun toString_withNoCurrentRoute_returnsPlaceholderMessage() {
        val player = RoutePlayer(FakeMediaControllableService(), FakeRouteDao())

        assertEquals("No current route set.", player.toString())
    }

    @Test
    fun toString_withCurrentRoute_listsWaypointsAndMarksCurrent() = runBlocking {
        val dao = FakeRouteDao()
        val routeId = dao.createRoute("Loop", listOf(MARKER_A, MARKER_B))
        val player = RoutePlayer(FakeMediaControllableService(), dao)
        player.startRoute(routeId)
        waitUntil { player.currentRouteFlow.value.routeData != null }

        val description = player.toString()

        assertTrue(description.contains("Route : Loop"))
        assertTrue(description.contains("A at ${MARKER_A.latitude},${MARKER_A.longitude} <current>"))
        assertTrue(description.contains("B at ${MARKER_B.latitude},${MARKER_B.longitude}"))
    }
}
