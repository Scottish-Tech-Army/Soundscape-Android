package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.services.ServiceConnection
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeRouteDao : RouteDao {
    private var nextMarkerId = 1L
    private var nextRouteId = 1L

    val markersFlow = MutableStateFlow<List<MarkerEntity>>(emptyList())
    val routesFlow = MutableStateFlow<List<RouteEntity>>(emptyList())
    val crossRefs = mutableListOf<RouteMarkerCrossRef>()

    override suspend fun insertMarker(marker: MarkerEntity): Long {
        val id = if (marker.markerId != 0L) marker.markerId else nextMarkerId++
        val stored = MarkerEntity(id, marker.name, marker.longitude, marker.latitude, marker.fullAddress)
        markersFlow.value = markersFlow.value.filterNot { it.markerId == id } + stored
        return id
    }

    override suspend fun updateMarker(marker: MarkerEntity) {
        markersFlow.value = markersFlow.value.map { if (it.markerId == marker.markerId) marker else it }
    }

    override suspend fun getMarkerById(markerId: Long): MarkerEntity? =
        markersFlow.value.find { it.markerId == markerId }

    override suspend fun getMarkerByLocation(longitude: Double, latitude: Double): MarkerEntity? =
        markersFlow.value.find { it.longitude == longitude && it.latitude == latitude }

    override suspend fun getAllMarkers(): List<MarkerEntity> = markersFlow.value

    override fun getAllMarkersFlow(): Flow<List<MarkerEntity>> = markersFlow

    override suspend fun insertRoute(route: RouteEntity): Long {
        val id = if (route.routeId != 0L) route.routeId else nextRouteId++
        val stored = RouteEntity(id, route.name, route.description)
        routesFlow.value = routesFlow.value.filterNot { it.routeId == id } + stored
        return id
    }

    override suspend fun addMarkerToRoute(crossRef: RouteMarkerCrossRef) {
        crossRefs.add(crossRef)
    }

    override suspend fun removeMarkerFromRoute(routeId: Long, markerId: Long) {
        crossRefs.removeAll { it.routeId == routeId && it.markerId == markerId }
    }

    override suspend fun removeMarkersForRoute(routeId: Long) {
        crossRefs.removeAll { it.routeId == routeId }
    }

    override suspend fun getAllRoutes(): List<RouteEntity> = routesFlow.value

    override suspend fun getRouteById(routeId: Long): RouteEntity? =
        routesFlow.value.find { it.routeId == routeId }

    override suspend fun getMarkerCrossReference(routeId: Long): List<RouteMarkerCrossRef> =
        crossRefs.filter { it.routeId == routeId }

    override fun getAllRoutesFlow(): Flow<List<RouteEntity>> = routesFlow

    override suspend fun removeRoute(routeId: Long) {
        routesFlow.value = routesFlow.value.filterNot { it.routeId == routeId }
        crossRefs.removeAll { it.routeId == routeId }
    }

    override suspend fun removeMarker(markerId: Long) {
        markersFlow.value = markersFlow.value.filterNot { it.markerId == markerId }
    }

    override suspend fun deleteAllRouteMarkerCrossRefs() {
        crossRefs.clear()
    }

    override suspend fun deleteAllMarkers() {
        markersFlow.value = emptyList()
    }

    override suspend fun deleteAllRoutes() {
        routesFlow.value = emptyList()
    }
}

private class FakeMediaControllableService : MediaControllableService {
    var routeStartByIdCalledWith: Long? = null
    var routeStartReverseCalledWith: Long? = null
    var routeStopCalled = false

    override fun routeMute(): Boolean = false
    override fun routeSkipNext(): Boolean = false
    override fun routeSkipPrevious(): Boolean = false
    override fun myLocation() {}
    override fun whatsAroundMe() {}

    override val filteredLocationFlow: StateFlow<SoundscapeLocation?> = MutableStateFlow(null)

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

    override var menuActive: Boolean = false
    override fun speak2dText(text: String, clearQueue: Boolean, earcon: String?) {}
    override fun callbackHoldOff() {}
    override fun requestAudioFocus(): Boolean = true
    override fun aheadOfMe() {}
    override fun nearbyMarkers() {}
    override fun routeStop() {
        routeStopCalled = true
    }

    override fun routeStartById(routeId: Long) {
        routeStartByIdCalledWith = routeId
    }

    override fun startBeacon(location: LngLatAlt, name: String) {}

    override val locationFlow: StateFlow<SoundscapeLocation?> = MutableStateFlow(null)
    override val orientationFlow: StateFlow<DeviceDirection?> = MutableStateFlow(null)
    override val beaconFlow: StateFlow<BeaconState> = MutableStateFlow(BeaconState())
    override val currentRouteFlow: StateFlow<RoutePlayerState> = MutableStateFlow(RoutePlayerState())
    override val gridStateFlow: StateFlow<GridState?> = MutableStateFlow(null)

    override fun routeStartReverse(routeId: Long) {
        routeStartReverseCalledWith = routeId
    }

    override fun getLocationDescription(location: LngLatAlt): LocationDescription =
        LocationDescription(location = location)

    override suspend fun searchResult(query: String): List<LocationDescription>? = null
    override fun isAudioEngineBusy(): Boolean = false
    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long = 0L
}

private class FakeServiceConnection(
    initialBound: Boolean = false,
    override val service: MediaControllableService? = null,
) : ServiceConnection {
    override val serviceBoundState: StateFlow<Boolean> = MutableStateFlow(initialBound)
}

@OptIn(ExperimentalCoroutinesApi::class)
class RouteDetailsViewModelTest {

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
    fun initialState_isEmpty() = runTest {
        val vm = RouteDetailsViewModel(FakeRouteDao(), FakeServiceConnection())

        val state = vm.uiState.value
        assertNull(state.route)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun getRouteById_found_populatesRoute() = runTest {
        val dao = FakeRouteDao()
        val routeId = dao.insertRoute(RouteEntity(name = "Loop", description = "desc"))
        val markerId = dao.insertMarker(MarkerEntity(name = "M1", longitude = 1.0, latitude = 2.0))
        dao.addMarkerToRoute(RouteMarkerCrossRef(routeId, markerId, 0))
        val vm = RouteDetailsViewModel(dao, FakeServiceConnection())

        vm.getRouteById(routeId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.route)
        assertEquals("Loop", state.route.route.name)
        assertEquals(1, state.route.markers.size)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun getRouteById_notFound_leavesRouteNullWithoutError() = runTest {
        val vm = RouteDetailsViewModel(FakeRouteDao(), FakeServiceConnection())

        vm.getRouteById(12345L)
        testDispatcher.scheduler.advanceUntilIdle()

        // RouteDao.getRouteWithMarkers just returns null for an unknown id (it doesn't
        // throw), so the ViewModel's catch block is never hit here - the "not found" state
        // is represented by uiState.route == null, not by errorMessage.
        val state = vm.uiState.value
        assertNull(state.route)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun startRoute_forwardsToService() = runTest {
        val service = FakeMediaControllableService()
        val vm = RouteDetailsViewModel(FakeRouteDao(), FakeServiceConnection(service = service))

        vm.startRoute(42L)

        assertEquals(42L, service.routeStartByIdCalledWith)
    }

    @Test
    fun startRoute_noBoundService_doesNotThrow() = runTest {
        val vm = RouteDetailsViewModel(FakeRouteDao(), FakeServiceConnection())

        vm.startRoute(42L)
    }

    @Test
    fun startRouteInReverse_forwardsToService() = runTest {
        val service = FakeMediaControllableService()
        val vm = RouteDetailsViewModel(FakeRouteDao(), FakeServiceConnection(service = service))

        vm.startRouteInReverse(7L)

        assertEquals(7L, service.routeStartReverseCalledWith)
    }

    @Test
    fun stopRoute_forwardsToService() = runTest {
        val service = FakeMediaControllableService()
        val vm = RouteDetailsViewModel(FakeRouteDao(), FakeServiceConnection(service = service))

        vm.stopRoute()

        assertTrue(service.routeStopCalled)
    }

    @Test
    fun clearErrorMessage_isANoOpWhenThereIsNoError() = runTest {
        val vm = RouteDetailsViewModel(FakeRouteDao(), FakeServiceConnection())

        vm.clearErrorMessage()

        assertNull(vm.uiState.value.errorMessage)
    }
}
