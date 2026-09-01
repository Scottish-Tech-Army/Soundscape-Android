package org.scottishtecharmy.soundscape.screens.home.placesnearby

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.HeadHeading
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.services.ServiceConnection
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Waits (on the real [Dispatchers.Default], not the virtual test scheduler) for [condition]
 * to become true. Needed because [GridState.treeContext] is a real single-threaded dispatcher
 * (not the [setMain]-swapped test dispatcher), so PlacesNearbyViewModel's
 * `withContext(gridState.treeContext) { ... }` hop completes on real time and
 * `advanceUntilIdle()` alone cannot observe it.
 */
private suspend fun awaitTrue(timeoutMs: Long = 2000, condition: () -> Boolean) {
    withContext(Dispatchers.Default) {
        withTimeout(timeoutMs) {
            while (!condition()) {
                delay(5)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlacesNearbyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isDefault() = runTest {
        val vm = PlacesNearbyViewModel(FakeServiceConnection())

        // FeatureCollection doesn't override equals, so compare the fields individually
        // rather than the whole (data class) PlacesNearbyUiState against a fresh default.
        val state = vm.uiState.value
        assertEquals(null, state.userLocation)
        assertEquals(0, state.level)
        assertTrue(state.nearbyPlaces.features.isEmpty())
        assertTrue(state.nearbyIntersections.features.isEmpty())
        assertEquals("", state.filter)
        assertEquals("", state.title)
        assertEquals(null, state.markerDescription)
    }

    @Test
    fun onClickFolder_updatesLevelFilterAndTitle() = runTest {
        val vm = PlacesNearbyViewModel(FakeServiceConnection())

        vm.onClickFolder("category=cafe", "Cafes")

        val state = vm.uiState.value
        assertEquals(1, state.level)
        assertEquals("category=cafe", state.filter)
        assertEquals("Cafes", state.title)
    }

    @Test
    fun onClickBack_resetsToRootLevel() = runTest {
        val vm = PlacesNearbyViewModel(FakeServiceConnection())
        vm.onClickFolder("category=cafe", "Cafes")

        vm.onClickBack()

        val state = vm.uiState.value
        assertEquals(0, state.level)
        assertEquals("", state.filter)
        assertEquals("", state.title)
    }

    @Test
    fun startBeacon_delegatesToBoundService() = runTest {
        val service = FakeMediaControllableService()
        val vm = PlacesNearbyViewModel(FakeServiceConnection(service = service))
        val location = LngLatAlt(-4.25, 55.86)

        vm.startBeacon(location, "Home")

        assertEquals(listOf(location to "Home"), service.startBeaconCalls)
    }

    @Test
    fun startBeacon_noBoundService_doesNotThrow() = runTest {
        val vm = PlacesNearbyViewModel(FakeServiceConnection(service = null))

        vm.startBeacon(LngLatAlt(0.0, 0.0), "Nowhere")
    }

    @Test
    fun serviceBound_updatesUserLocationAndNearbyPlaces() = runTest {
        val service = FakeMediaControllableService()
        val connection = FakeServiceConnection(service = service)
        val vm = PlacesNearbyViewModel(connection)

        connection.boundState.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        val poi = Feature().apply { geometry = Point(LngLatAlt(-4.25, 55.86)) }
        val gridState = GridState()
        gridState.featureTrees[TreeId.POIS.id] = FeatureTree(FeatureCollection().addFeature(poi))
        try {
            service.gridStateFlowState.value = gridState
            service.locationFlowState.value = SoundscapeLocation(latitude = 55.86, longitude = -4.25)
            testDispatcher.scheduler.advanceUntilIdle()

            // GridState.treeContext is a real background thread, so the withContext hop in
            // PlacesNearbyViewModel completes on real time rather than the virtual test clock.
            awaitTrue { vm.uiState.value.nearbyPlaces.features.isNotEmpty() }

            val state = vm.uiState.value
            assertEquals(LngLatAlt(-4.25, 55.86), state.userLocation)
            assertEquals(1, state.nearbyPlaces.features.size)
            assertTrue(state.nearbyIntersections.features.isEmpty())
        } finally {
            gridState.treeContext.close()
        }
    }

    @Test
    fun serviceUnbound_stopsMonitoring() = runTest {
        val service = FakeMediaControllableService()
        val connection = FakeServiceConnection(service = service)
        val vm = PlacesNearbyViewModel(connection)

        connection.boundState.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        service.locationFlowState.value = SoundscapeLocation(latitude = 1.0, longitude = 2.0)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(LngLatAlt(2.0, 1.0), vm.uiState.value.userLocation)

        connection.boundState.value = false
        testDispatcher.scheduler.advanceUntilIdle()

        service.locationFlowState.value = SoundscapeLocation(latitude = 9.0, longitude = 9.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(LngLatAlt(2.0, 1.0), vm.uiState.value.userLocation)
    }

    @Test
    fun dispose_cancelsViewModelScope() = runTest {
        val vm = PlacesNearbyViewModel(FakeServiceConnection())

        // Should not throw, and further state mutation should still be a plain field write.
        vm.dispose()
        vm.onClickFolder("category=cafe", "Cafes")

        assertEquals(1, vm.uiState.value.level)
    }
}

private class FakeServiceConnection(
    initialBound: Boolean = false,
    override var service: MediaControllableService? = null,
) : ServiceConnection {
    val boundState = MutableStateFlow(initialBound)
    override val serviceBoundState: StateFlow<Boolean> = boundState
}

/**
 * Hand-rolled in-memory [MediaControllableService]. Every flow is independently
 * settable so tests can drive PlacesNearbyViewModel's collectors directly; action
 * methods record whether they were invoked (and with what arguments) instead of
 * doing anything real.
 */
private class FakeMediaControllableService : MediaControllableService {

    val locationFlowState = MutableStateFlow<SoundscapeLocation?>(null)
    override val locationFlow: StateFlow<SoundscapeLocation?> = locationFlowState

    override val orientationFlow: StateFlow<DeviceDirection?> = MutableStateFlow(null)
    override val headHeadingFlow: StateFlow<HeadHeading?> = MutableStateFlow(null)
    override val beaconFlow: StateFlow<BeaconState> = MutableStateFlow(BeaconState())
    override val currentRouteFlow: StateFlow<RoutePlayerState> = MutableStateFlow(RoutePlayerState())

    val gridStateFlowState = MutableStateFlow<GridState?>(null)
    override val gridStateFlow: StateFlow<GridState?> = gridStateFlowState

    override val streetPreviewFlow: StateFlow<StreetPreviewState> = MutableStateFlow(StreetPreviewState())
    override val filteredLocationFlow: StateFlow<SoundscapeLocation?> = MutableStateFlow(null)

    override var menuActive: Boolean = false

    val startBeaconCalls = mutableListOf<Pair<LngLatAlt, String>>()
    var locationDescriptionToReturn = LocationDescription(location = LngLatAlt())

    override fun routeMute(): Boolean = true
    override fun routeSkipNext(): Boolean = true
    override fun routeSkipPrevious(): Boolean = true
    override fun myLocation() {}
    override fun whatsAroundMe() {}

    override fun speakText(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double,
    ) {
    }

    override fun clearTextToSpeechQueue() {}
    override fun createBeacon(location: LngLatAlt?, headingOnly: Boolean) {}
    override fun destroyBeacon() {}
    override fun speak2dText(text: String, clearQueue: Boolean, earcon: String?) {}
    override fun callbackHoldOff() {}
    override fun requestAudioFocus(): Boolean = true
    override fun aheadOfMe() {}
    override fun nearbyMarkers() {}
    override fun routeStop() {}
    override fun routeStartById(routeId: Long) {}

    override fun startBeacon(location: LngLatAlt, name: String) {
        startBeaconCalls.add(location to name)
    }

    override fun routeStartReverse(routeId: Long) {}
    override fun getLocationDescription(location: LngLatAlt): LocationDescription =
        locationDescriptionToReturn

    override suspend fun searchResult(query: String): List<LocationDescription>? = null
    override fun isAudioEngineBusy(): Boolean = false
    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long = 0L
}
