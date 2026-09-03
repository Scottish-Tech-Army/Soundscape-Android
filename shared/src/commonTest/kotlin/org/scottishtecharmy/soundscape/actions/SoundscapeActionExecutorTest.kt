package org.scottishtecharmy.soundscape.actions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.PluralKey
import org.scottishtecharmy.soundscape.i18n.StringKey
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Returns "KeyName(arg1,arg2)" for every key so assertions can pin down exactly which
 * StringKey the executor chose, without depending on real translated copy.
 */
private class FakeLocalizedStrings : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String =
        "${key.name}(${args.joinToString(",")})"

    override fun getOrNull(key: StringKey, vararg args: Any?): String? = get(key, *args)

    override fun getPlural(key: PluralKey, quantity: Int, vararg args: Any?): String =
        "$key($quantity)"

    override fun resolveFeatureClass(key: String): String? = null
}

/** Only the lookups the executor makes are implemented. */
private class FakeRouteDao(
    private val routes: List<RouteEntity> = emptyList(),
    private val markers: List<MarkerEntity> = emptyList(),
) : RouteDao {
    override suspend fun getAllRoutes(): List<RouteEntity> = routes
    override suspend fun getAllMarkers(): List<MarkerEntity> = markers
    override suspend fun getRouteById(routeId: Long): RouteEntity? =
        routes.firstOrNull { it.routeId == routeId }

    override suspend fun getMarkerById(markerId: Long): MarkerEntity? =
        markers.firstOrNull { it.markerId == markerId }

    private fun unused(): Nothing = throw NotImplementedError("not used by the action executor")

    override suspend fun insertMarker(marker: MarkerEntity): Long = unused()
    override suspend fun updateMarker(marker: MarkerEntity): Unit = unused()
    override suspend fun getMarkerByLocation(longitude: Double, latitude: Double): MarkerEntity? =
        unused()

    override fun getAllMarkersFlow(): Flow<List<MarkerEntity>> = unused()
    override suspend fun insertRoute(route: RouteEntity): Long = unused()
    override suspend fun addMarkerToRoute(crossRef: RouteMarkerCrossRef): Unit = unused()
    override suspend fun removeMarkerFromRoute(routeId: Long, markerId: Long): Unit = unused()
    override suspend fun removeMarkersForRoute(routeId: Long): Unit = unused()
    override suspend fun getMarkerCrossReference(routeId: Long): List<RouteMarkerCrossRef> =
        unused()

    override fun getAllRoutesFlow(): Flow<List<RouteEntity>> = unused()
    override suspend fun removeRoute(routeId: Long): Unit = unused()
    override suspend fun removeMarker(markerId: Long): Unit = unused()
    override suspend fun deleteAllRouteMarkerCrossRefs(): Unit = unused()
    override suspend fun deleteAllMarkers(): Unit = unused()
    override suspend fun deleteAllRoutes(): Unit = unused()
}

/**
 * Records the service calls the executor makes, in order, so tests can assert both
 * *what* was invoked and that cancelCallout precedes the callout it clears for.
 */
private class FakeService : MediaControllableService {
    val calls = mutableListOf<String>()

    val locationFlowState = MutableStateFlow<SoundscapeLocation?>(SoundscapeLocation())
    override val locationFlow: StateFlow<SoundscapeLocation?> = locationFlowState

    val gridStateFlowState = MutableStateFlow<GridState?>(GridState())
    override val gridStateFlow: StateFlow<GridState?> = gridStateFlowState

    /** Each of these mirrors "a route is playing" for the media-key style calls. */
    var routeCommandsSucceed = true

    val startBeaconCalls = mutableListOf<Pair<LngLatAlt, String>>()
    val routeStartCalls = mutableListOf<Long>()
    val routeStartReverseCalls = mutableListOf<Long>()

    override fun myLocation() { calls.add("myLocation") }
    override fun whatsAroundMe() { calls.add("whatsAroundMe") }
    override fun aheadOfMe() { calls.add("aheadOfMe") }
    override fun nearbyMarkers() { calls.add("nearbyMarkers") }
    override fun cancelCallout() { calls.add("cancelCallout") }

    override fun routeMute(): Boolean {
        calls.add("routeMute")
        return routeCommandsSucceed
    }

    override fun routeSkipNext(): Boolean {
        calls.add("routeSkipNext")
        return routeCommandsSucceed
    }

    override fun routeSkipPrevious(): Boolean {
        calls.add("routeSkipPrevious")
        return routeCommandsSucceed
    }

    override fun routeStop() { calls.add("routeStop") }

    override fun routeStartById(routeId: Long) {
        calls.add("routeStartById")
        routeStartCalls.add(routeId)
    }

    override fun routeStartReverse(routeId: Long) {
        calls.add("routeStartReverse")
        routeStartReverseCalls.add(routeId)
    }

    override fun startBeacon(location: LngLatAlt, name: String) {
        calls.add("startBeacon")
        startBeaconCalls.add(location to name)
    }

    override fun destroyBeacon() { calls.add("destroyBeacon") }

    // --- Unused by the executor ---
    override val filteredLocationFlow: StateFlow<SoundscapeLocation?> = MutableStateFlow(null)
    override val orientationFlow: StateFlow<DeviceDirection?> = MutableStateFlow(null)
    override val beaconFlow: StateFlow<BeaconState> = MutableStateFlow(BeaconState())
    override val currentRouteFlow: StateFlow<RoutePlayerState> = MutableStateFlow(RoutePlayerState())
    override var menuActive: Boolean = false

    override fun speakText(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ) = unused()

    override fun clearTextToSpeechQueue() = unused()
    override fun createBeacon(location: LngLatAlt?, headingOnly: Boolean) = unused()
    override fun speak2dText(text: String, clearQueue: Boolean, earcon: String?) = unused()
    override fun callbackHoldOff() = unused()
    override fun requestAudioFocus(): Boolean = unused()
    override fun getLocationDescription(location: LngLatAlt): LocationDescription = unused()
    override suspend fun searchResult(query: String): List<LocationDescription>? = unused()
    override fun isAudioEngineBusy(): Boolean = unused()
    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long = unused()

    /** The executor must never speak for itself — the assistant owns that. */
    private fun unused(): Nothing =
        throw AssertionError("the action executor should not reach this service method")
}

private fun route(id: Long, name: String) = RouteEntity(routeId = id, name = name, description = "")

private fun marker(id: Long, name: String, lon: Double, lat: Double) =
    MarkerEntity(markerId = id, name = name, longitude = lon, latitude = lat)

private fun executor(
    service: FakeService = FakeService(),
    dao: RouteDao = FakeRouteDao(),
) = SoundscapeActionExecutor(service, dao, FakeLocalizedStrings())

class SoundscapeActionExecutorTest {

    // ── Callouts ──────────────────────────────────────────────────────────────

    @Test
    fun eachCalloutActionReachesItsServiceMethod() = runTest {
        val cases = listOf(
            SoundscapeAction.MyLocation to "myLocation",
            SoundscapeAction.AroundMe to "whatsAroundMe",
            SoundscapeAction.AheadOfMe to "aheadOfMe",
            SoundscapeAction.NearbyMarkers to "nearbyMarkers",
        )
        for ((action, expected) in cases) {
            val service = FakeService()
            val result = executor(service).execute(action)
            assertIs<ActionResult.Ok>(result, "$action")
            // Success speaks nothing: the spatial callout is the response.
            assertNull(result.speech, "$action")
            assertContentEquals(listOf("cancelCallout", expected), service.calls, "$action")
        }
    }

    @Test
    fun calloutCancelsAnyInFlightOneFirstSoARepeatRepeats() = runTest {
        // CalloutController toggles a repeated callout off; the executor must clear
        // that state before issuing, or asking Siri twice would silence the app.
        val service = FakeService()
        executor(service).execute(SoundscapeAction.AroundMe)
        assertEquals(0, service.calls.indexOf("cancelCallout"))
        assertEquals(1, service.calls.indexOf("whatsAroundMe"))
    }

    @Test
    fun calloutWithoutLocationFix_isNotReadyAndIssuesNothing() = runTest {
        val service = FakeService().apply { locationFlowState.value = null }
        val result = executor(service).execute(SoundscapeAction.MyLocation)
        assertIs<ActionResult.NotReady>(result)
        assertEquals(ActionResult.Reason.NO_LOCATION_FIX, result.reason)
        assertEquals("ActionNoLocation()", result.speech)
        assertTrue(service.calls.isEmpty())
    }

    @Test
    fun calloutWithoutMapData_isNotReadyAndIssuesNothing() = runTest {
        val service = FakeService().apply { gridStateFlowState.value = null }
        val result = executor(service).execute(SoundscapeAction.AheadOfMe)
        assertIs<ActionResult.NotReady>(result)
        assertEquals(ActionResult.Reason.NO_MAP_DATA, result.reason)
        assertEquals("ActionNoMapData()", result.speech)
        assertTrue(service.calls.isEmpty())
    }

    @Test
    fun calloutWaitsForAColdServiceToBecomeReadyWithinTheTimeout() = runTest {
        // Mirrors an assistant cold start: the geo engine is coming up, and the fix and
        // tiles land after the intent has already been dispatched.
        val service = FakeService().apply {
            locationFlowState.value = null
            gridStateFlowState.value = null
        }
        val ready = launch {
            service.locationFlowState.value = SoundscapeLocation()
            service.gridStateFlowState.value = GridState()
        }
        val result = executor(service).execute(SoundscapeAction.AroundMe, readyTimeoutMs = 5_000)
        ready.join()
        assertIs<ActionResult.Ok>(result)
        assertContentEquals(listOf("cancelCallout", "whatsAroundMe"), service.calls)
    }

    @Test
    fun calloutGivesUpWhenTheServiceNeverBecomesReady() = runTest {
        val service = FakeService().apply { locationFlowState.value = null }
        val result = executor(service).execute(SoundscapeAction.AroundMe, readyTimeoutMs = 5_000)
        assertIs<ActionResult.NotReady>(result)
        assertEquals(ActionResult.Reason.NO_LOCATION_FIX, result.reason)
        assertTrue(service.calls.isEmpty())
    }

    @Test
    fun calloutWithZeroTimeoutDoesNotWait() = runTest {
        // The button path: the app is already up, so a missing fix is a real failure
        // rather than something to sit and wait for.
        val service = FakeService().apply { gridStateFlowState.value = null }
        val result = executor(service).execute(SoundscapeAction.AheadOfMe, readyTimeoutMs = 0)
        assertIs<ActionResult.NotReady>(result)
        assertEquals(ActionResult.Reason.NO_MAP_DATA, result.reason)
    }

    // ── Routes ────────────────────────────────────────────────────────────────

    @Test
    fun startRouteNamed_fuzzyMatchesAndConfirmsWithTheSavedName() = runTest {
        val service = FakeService()
        val dao = FakeRouteDao(routes = listOf(route(1, "Riverside Walk"), route(2, "Tesco Express")))
        val result = executor(service, dao).execute(SoundscapeAction.StartRouteNamed("Tesco"))
        assertIs<ActionResult.Ok>(result)
        // Confirms with the saved name, not the spoken one, so the user hears which
        // route the fuzzy match actually landed on.
        assertEquals("ActionRouteStarted(Tesco Express)", result.speech)
        assertContentEquals(listOf(2L), service.routeStartCalls)
    }

    @Test
    fun startRouteNamed_withNoRoutesSaved_isNotReadyRatherThanNotFound() = runTest {
        val result = executor(dao = FakeRouteDao()).execute(SoundscapeAction.StartRouteNamed("Tesco"))
        assertIs<ActionResult.NotReady>(result)
        assertEquals(ActionResult.Reason.NO_ROUTES_SAVED, result.reason)
        assertEquals("MenuNoRoutes()", result.speech)
    }

    @Test
    fun startRouteNamed_withNoMatch_reportsWhatWasAskedFor() = runTest {
        val service = FakeService()
        val dao = FakeRouteDao(routes = listOf(route(1, "Riverside Walk")))
        val result = executor(service, dao).execute(SoundscapeAction.StartRouteNamed("Zzyzx"))
        assertIs<ActionResult.NotFound>(result)
        assertEquals("Zzyzx", result.query)
        assertEquals("ActionNoSuchRoute(Zzyzx)", result.speech)
        assertTrue(service.routeStartCalls.isEmpty())
    }

    @Test
    fun startRouteById_honoursReverse() = runTest {
        val dao = FakeRouteDao(routes = listOf(route(5, "Commute")))

        val forward = FakeService()
        assertIs<ActionResult.Ok>(
            executor(forward, dao).execute(SoundscapeAction.StartRouteById(5, reverse = false)),
        )
        assertContentEquals(listOf(5L), forward.routeStartCalls)
        assertTrue(forward.routeStartReverseCalls.isEmpty())

        val backward = FakeService()
        assertIs<ActionResult.Ok>(
            executor(backward, dao).execute(SoundscapeAction.StartRouteById(5, reverse = true)),
        )
        assertContentEquals(listOf(5L), backward.routeStartReverseCalls)
        assertTrue(backward.routeStartCalls.isEmpty())
    }

    @Test
    fun startRouteById_withStaleId_reportsItemNotFound() = runTest {
        val service = FakeService()
        val result = executor(service, FakeRouteDao(routes = listOf(route(1, "Commute"))))
            .execute(SoundscapeAction.StartRouteById(99, reverse = false))
        assertIs<ActionResult.NotFound>(result)
        assertEquals("ActionItemNotFound()", result.speech)
        assertTrue(service.routeStartCalls.isEmpty())
    }

    @Test
    fun stopRoute_confirms() = runTest {
        val service = FakeService()
        val result = executor(service).execute(SoundscapeAction.StopRoute)
        assertIs<ActionResult.Ok>(result)
        assertEquals("ActionRouteStopped()", result.speech)
        assertContentEquals(listOf("routeStop"), service.calls)
    }

    @Test
    fun waypointAndMuteCommands_succeedSilentlyWhileARouteIsPlaying() = runTest {
        val cases = listOf(
            SoundscapeAction.NextWaypoint to "routeSkipNext",
            SoundscapeAction.PreviousWaypoint to "routeSkipPrevious",
            SoundscapeAction.ToggleBeaconMute to "routeMute",
        )
        for ((action, expected) in cases) {
            val service = FakeService()
            val result = executor(service).execute(action)
            assertIs<ActionResult.Ok>(result, "$action")
            assertNull(result.speech, "$action")
            assertContentEquals(listOf(expected), service.calls, "$action")
        }
    }

    @Test
    fun waypointAndMuteCommands_reportNoRouteWhenNothingIsPlaying() = runTest {
        for (action in listOf(
            SoundscapeAction.NextWaypoint,
            SoundscapeAction.PreviousWaypoint,
            SoundscapeAction.ToggleBeaconMute,
        )) {
            val service = FakeService().apply { routeCommandsSucceed = false }
            val result = executor(service).execute(action)
            assertIs<ActionResult.NotReady>(result, "$action")
            assertEquals(ActionResult.Reason.NO_ROUTE_ACTIVE, result.reason, "$action")
            assertEquals("ActionNoRouteActive()", result.speech, "$action")
        }
    }

    // ── Lists ─────────────────────────────────────────────────────────────────

    @Test
    fun listRoutes_readsBackEveryNameAfterTheLeadIn() = runTest {
        val dao = FakeRouteDao(routes = listOf(route(1, "Commute"), route(2, "Riverside Walk")))
        val result = executor(dao = dao).execute(SoundscapeAction.ListRoutes)
        assertIs<ActionResult.Ok>(result)
        assertEquals("VoiceCmdRoutesList() Commute, Riverside Walk", result.speech)
    }

    @Test
    fun listMarkers_readsBackEveryNameAfterTheLeadIn() = runTest {
        val dao = FakeRouteDao(
            markers = listOf(
                marker(1, "Post Office", lon = -4.3, lat = 55.9),
                marker(2, "Tesco", lon = -4.4, lat = 55.8),
            ),
        )
        val result = executor(dao = dao).execute(SoundscapeAction.ListMarkers)
        assertIs<ActionResult.Ok>(result)
        assertEquals("VoiceCmdMarkersList() Post Office, Tesco", result.speech)
    }

    @Test
    fun listsReportEmptinessRatherThanAnEmptySentence() = runTest {
        val routes = executor().execute(SoundscapeAction.ListRoutes)
        assertIs<ActionResult.NotReady>(routes)
        assertEquals(ActionResult.Reason.NO_ROUTES_SAVED, routes.reason)

        val markers = executor().execute(SoundscapeAction.ListMarkers)
        assertIs<ActionResult.NotReady>(markers)
        assertEquals(ActionResult.Reason.NO_MARKERS_SAVED, markers.reason)
    }

    // ── Beacons ───────────────────────────────────────────────────────────────

    @Test
    fun beaconOnMarkerNamed_startsTheBeaconAtTheMarkersCoordinates() = runTest {
        val service = FakeService()
        val dao = FakeRouteDao(markers = listOf(marker(3, "Post Office", lon = -4.3, lat = 55.9)))
        val result = executor(service, dao).execute(SoundscapeAction.BeaconOnMarkerNamed("Post Office"))
        assertIs<ActionResult.Ok>(result)
        assertEquals("ActionBeaconStarted(Post Office)", result.speech)
        assertContentEquals(listOf(LngLatAlt(-4.3, 55.9) to "Post Office"), service.startBeaconCalls)
    }

    @Test
    fun beaconOnMarkerById_startsTheBeacon() = runTest {
        val service = FakeService()
        val dao = FakeRouteDao(markers = listOf(marker(3, "Post Office", lon = -4.3, lat = 55.9)))
        val result = executor(service, dao).execute(SoundscapeAction.BeaconOnMarkerById(3))
        assertIs<ActionResult.Ok>(result)
        assertContentEquals(listOf(LngLatAlt(-4.3, 55.9) to "Post Office"), service.startBeaconCalls)
    }

    @Test
    fun beaconOnMarkerById_withStaleId_reportsItemNotFound() = runTest {
        val service = FakeService()
        val dao = FakeRouteDao(markers = listOf(marker(3, "Post Office", lon = -4.3, lat = 55.9)))
        val result = executor(service, dao).execute(SoundscapeAction.BeaconOnMarkerById(99))
        assertIs<ActionResult.NotFound>(result)
        assertEquals("ActionItemNotFound()", result.speech)
        assertTrue(service.startBeaconCalls.isEmpty())
    }

    @Test
    fun beaconOnMarkerNamed_withNoMarkersSaved_isNotReady() = runTest {
        val result = executor().execute(SoundscapeAction.BeaconOnMarkerNamed("Post Office"))
        assertIs<ActionResult.NotReady>(result)
        assertEquals(ActionResult.Reason.NO_MARKERS_SAVED, result.reason)
        assertEquals("ActionNoMarkersSaved()", result.speech)
    }

    @Test
    fun beaconOnMarkerNamed_withNoMatch_reportsWhatWasAskedFor() = runTest {
        val service = FakeService()
        val dao = FakeRouteDao(markers = listOf(marker(3, "Post Office", lon = -4.3, lat = 55.9)))
        val result = executor(service, dao).execute(SoundscapeAction.BeaconOnMarkerNamed("Zzyzx"))
        assertIs<ActionResult.NotFound>(result)
        assertEquals("Zzyzx", result.query)
        assertEquals("ActionNoSuchMarker(Zzyzx)", result.speech)
        assertTrue(service.startBeaconCalls.isEmpty())
    }

    @Test
    fun stopBeacon_confirms() = runTest {
        val service = FakeService()
        val result = executor(service).execute(SoundscapeAction.StopBeacon)
        assertIs<ActionResult.Ok>(result)
        assertEquals("ActionBeaconStopped()", result.speech)
        assertContentEquals(listOf("destroyBeacon"), service.calls)
    }
}
