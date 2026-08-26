package org.scottishtecharmy.soundscape.screens.home

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
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.audio.AudioTourHost
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.TourButton
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.HeadHeading
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.services.ServiceConnection
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import org.scottishtecharmy.soundscape.services.mediacontrol.VoiceCommandState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Waits (on the real [Dispatchers.Default], not the virtual test scheduler) for [condition]
 * to become true. Needed because [HomeViewModel]'s action methods deliberately dispatch onto
 * [Dispatchers.Default] rather than the Main dispatcher swapped in by [setMain], so
 * `advanceUntilIdle()` alone cannot observe their completion.
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
class HomeViewModelTest {

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
        val vm = HomeViewModel(FakeServiceConnection())

        assertEquals(HomeState(), vm.state.value)
    }

    @Test
    fun setRoutesAndMarkersTab_updatesState() = runTest {
        val vm = HomeViewModel(FakeServiceConnection())

        vm.setRoutesAndMarkersTab(false)

        assertFalse(vm.state.value.routesTabSelected)
    }

    @Test
    fun onTriggerSearch_populatesSearchItemsAndTogglesProgress() = runTest {
        val service = FakeMediaControllableService()
        val expected = listOf(LocationDescription(name = "Cafe", location = LngLatAlt(-4.25, 55.86)))
        service.searchResultToReturn = expected
        val vm = HomeViewModel(FakeServiceConnection(service = service))

        vm.onTriggerSearch("cafe")

        awaitTrue { vm.state.value.searchItems != null }

        assertEquals("cafe", service.searchResultQuery)
        assertEquals(expected, vm.state.value.searchItems)
        assertFalse(vm.state.value.searchInProgress)
    }

    @Test
    fun serviceBound_startsMonitoringLocationHeadingBeaconAndRoute() = runTest {
        val service = FakeMediaControllableService()
        val connection = FakeServiceConnection(service = service)
        val vm = HomeViewModel(connection)

        connection.boundState.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        service.locationFlowState.value = SoundscapeLocation(latitude = 55.86, longitude = -4.25)
        service.orientationFlowState.value =
            DeviceDirection(floatArrayOf(0f, 0f, 0f, 0f), 90f, 5f, 0L)
        service.beaconFlowState.value = BeaconState(name = "Home")
        service.currentRouteFlowState.value = RoutePlayerState(currentWaypoint = 2)
        service.voiceCommandStateFlowState.value = VoiceCommandState.Listening
        service.activeCalloutFlowState.value = TourButton.AROUND_ME
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertEquals(LngLatAlt(-4.25, 55.86), state.location)
        assertEquals(90f, state.heading)
        assertEquals("Home", state.beaconState?.name)
        assertEquals(2, state.currentRouteData.currentWaypoint)
        assertTrue(state.voiceCommandListening)
        assertEquals(TourButton.AROUND_ME, state.activeCallout)
    }

    @Test
    fun headTrackerHeading_takesPrecedenceOverPhoneOrientation() = runTest {
        val service = FakeMediaControllableService()
        val connection = FakeServiceConnection(service = service)
        val vm = HomeViewModel(connection)

        connection.boundState.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        service.orientationFlowState.value =
            DeviceDirection(floatArrayOf(0f, 0f, 0f, 0f), 90f, 5f, 0L)
        service.headHeadingFlowState.value = HeadHeading(45.0, 1.0, 0L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(45f, vm.state.value.heading)
    }

    @Test
    fun serviceUnbound_stopsMonitoringLocation() = runTest {
        val service = FakeMediaControllableService()
        val connection = FakeServiceConnection(service = service)
        val vm = HomeViewModel(connection)

        connection.boundState.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        service.locationFlowState.value = SoundscapeLocation(latitude = 1.0, longitude = 2.0)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(LngLatAlt(2.0, 1.0), vm.state.value.location)

        connection.boundState.value = false
        testDispatcher.scheduler.advanceUntilIdle()

        // Emitted after unbinding: should no longer reach state.
        service.locationFlowState.value = SoundscapeLocation(latitude = 9.0, longitude = 9.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(LngLatAlt(2.0, 1.0), vm.state.value.location)
    }

    @Test
    fun streetPreviewEnabledChange_restartsLocationMonitoring() = runTest {
        val service = FakeMediaControllableService()
        val connection = FakeServiceConnection(service = service)
        val vm = HomeViewModel(connection)

        connection.boundState.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        service.streetPreviewFlowState.value = StreetPreviewState(enabled = StreetPreviewEnabled.ON)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StreetPreviewEnabled.ON, vm.state.value.streetPreviewState.enabled)

        // Location monitoring should have been restarted and still be functional.
        service.locationFlowState.value = SoundscapeLocation(latitude = 3.0, longitude = 4.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(LngLatAlt(4.0, 3.0), vm.state.value.location)
    }

    @Test
    fun myLocation_delegatesToBoundService() = runTest {
        val service = FakeMediaControllableService()
        val vm = HomeViewModel(FakeServiceConnection(service = service))

        vm.myLocation()

        awaitTrue { service.myLocationCalled }
    }

    @Test
    fun myLocation_noBoundService_doesNotThrow() = runTest {
        val vm = HomeViewModel(FakeServiceConnection(service = null))

        vm.myLocation()

        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun routeStop_delegatesToServiceAndAudioTour() = runTest {
        val service = FakeMediaControllableService()
        val audioTour = AudioTour(FakeAudioTourHost())
        val vm = HomeViewModel(FakeServiceConnection(service = service), audioTour)

        vm.routeStop()

        awaitTrue { service.routeStopCalled }
    }

    @Test
    fun getLocationDescription_delegatesToService() = runTest {
        val service = FakeMediaControllableService()
        val expected = LocationDescription(name = "Result", location = LngLatAlt(1.0, 2.0))
        service.locationDescriptionToReturn = expected
        val vm = HomeViewModel(FakeServiceConnection(service = service))

        val result = vm.getLocationDescription(LngLatAlt(1.0, 2.0))

        assertEquals(expected, result)
    }

    @Test
    fun getLocationDescription_noBoundService_returnsNull() = runTest {
        val vm = HomeViewModel(FakeServiceConnection(service = null))

        assertNull(vm.getLocationDescription(LngLatAlt(1.0, 2.0)))
    }
}

private class FakeAudioTourHost : AudioTourHost {
    override fun isAudioEngineBusy(): Boolean = false
    override fun clearTextToSpeechQueue() {}
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
 * settable so tests can drive HomeViewModel's collectors directly; action methods
 * record whether they were invoked (and with what arguments) instead of doing
 * anything real.
 */
private class FakeMediaControllableService : MediaControllableService {

    val locationFlowState = MutableStateFlow<SoundscapeLocation?>(null)
    override val locationFlow: StateFlow<SoundscapeLocation?> = locationFlowState

    val orientationFlowState = MutableStateFlow<DeviceDirection?>(null)
    override val orientationFlow: StateFlow<DeviceDirection?> = orientationFlowState

    val headHeadingFlowState = MutableStateFlow<HeadHeading?>(null)
    override val headHeadingFlow: StateFlow<HeadHeading?> = headHeadingFlowState

    val beaconFlowState = MutableStateFlow(BeaconState())
    override val beaconFlow: StateFlow<BeaconState> = beaconFlowState

    val currentRouteFlowState = MutableStateFlow(RoutePlayerState())
    override val currentRouteFlow: StateFlow<RoutePlayerState> = currentRouteFlowState

    val gridStateFlowState = MutableStateFlow<GridState?>(null)
    override val gridStateFlow: StateFlow<GridState?> = gridStateFlowState

    val streetPreviewFlowState = MutableStateFlow(StreetPreviewState())
    override val streetPreviewFlow: StateFlow<StreetPreviewState> = streetPreviewFlowState

    val voiceCommandStateFlowState = MutableStateFlow<VoiceCommandState>(VoiceCommandState.Idle)
    override val voiceCommandStateFlow: StateFlow<VoiceCommandState> = voiceCommandStateFlowState

    val activeCalloutFlowState = MutableStateFlow<TourButton?>(null)
    override val activeCalloutFlow: StateFlow<TourButton?> = activeCalloutFlowState

    override val filteredLocationFlow: StateFlow<SoundscapeLocation?> = MutableStateFlow(null)

    override var menuActive: Boolean = false

    var myLocationCalled = false
    var aheadOfMeCalled = false
    var whatsAroundMeCalled = false
    var nearbyMarkersCalled = false
    var streetPreviewGoCalled = false
    val setStreetPreviewModeCalls = mutableListOf<Pair<Boolean, LngLatAlt?>>()
    var routeSkipPreviousCalled = false
    var routeSkipNextCalled = false
    var routeMuteCalled = false
    var routeStopCalled = false
    val startBeaconCalls = mutableListOf<Pair<LngLatAlt, String>>()
    val speakCalloutCalls = mutableListOf<TrackedCallout?>()
    var searchResultQuery: String? = null
    var searchResultToReturn: List<LocationDescription>? = null
    var locationDescriptionToReturn = LocationDescription(location = LngLatAlt())

    override fun routeMute(): Boolean {
        routeMuteCalled = true
        return true
    }

    override fun routeSkipNext(): Boolean {
        routeSkipNextCalled = true
        return true
    }

    override fun routeSkipPrevious(): Boolean {
        routeSkipPreviousCalled = true
        return true
    }

    override fun myLocation() {
        myLocationCalled = true
    }

    override fun whatsAroundMe() {
        whatsAroundMeCalled = true
    }

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

    override fun aheadOfMe() {
        aheadOfMeCalled = true
    }

    override fun nearbyMarkers() {
        nearbyMarkersCalled = true
    }

    override fun routeStop() {
        routeStopCalled = true
    }

    override fun routeStartById(routeId: Long) {}

    override fun startBeacon(location: LngLatAlt, name: String) {
        startBeaconCalls.add(location to name)
    }

    override fun routeStartReverse(routeId: Long) {}

    override fun setStreetPreviewMode(on: Boolean, location: LngLatAlt?) {
        setStreetPreviewModeCalls.add(on to location)
    }

    override fun streetPreviewGo() {
        streetPreviewGoCalled = true
    }

    override fun getLocationDescription(location: LngLatAlt): LocationDescription =
        locationDescriptionToReturn

    override suspend fun searchResult(query: String): List<LocationDescription>? {
        searchResultQuery = query
        return searchResultToReturn
    }

    override fun isAudioEngineBusy(): Boolean = false

    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long {
        speakCalloutCalls.add(callout)
        return 0L
    }
}
