package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

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
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeRouteDao : RouteDao {
    private var nextMarkerId = 1L
    private var nextRouteId = 1L

    val markersFlow = MutableStateFlow<List<MarkerEntity>>(emptyList())
    val routesFlow = MutableStateFlow<List<RouteEntity>>(emptyList())
    val crossRefs = mutableListOf<RouteMarkerCrossRef>()

    var removeRouteCalledWith: Long? = null

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
        removeRouteCalledWith = routeId
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
    val speakCalloutCalls = mutableListOf<TrackedCallout?>()

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
    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long {
        speakCalloutCalls.add(callout)
        return 0L
    }
}

private class FakeServiceConnection(
    initialBound: Boolean = false,
    override val service: MediaControllableService? = null,
) : ServiceConnection {
    override val serviceBoundState: StateFlow<Boolean> = MutableStateFlow(initialBound)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddAndEditRouteViewModelTest {

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
        val vm = AddAndEditRouteViewModel(FakeRouteDao(), FakeServiceConnection())

        val state = vm.uiState.value
        assertEquals("", state.name)
        assertEquals("", state.description)
        assertTrue(state.routeMembers.isEmpty())
        assertFalse(state.showDoneButton)
    }

    @Test
    fun onNameChange_and_onDescriptionChange_toggleShowDoneButton() = runTest {
        val vm = AddAndEditRouteViewModel(FakeRouteDao(), FakeServiceConnection())

        vm.onNameChange("My route")
        assertFalse(vm.uiState.value.showDoneButton)

        vm.onDescriptionChange("A nice walk")
        assertTrue(vm.uiState.value.showDoneButton)
        assertEquals("My route", vm.uiState.value.name)
        assertEquals("A nice walk", vm.uiState.value.description)
    }

    @Test
    fun loadMarkers_populatesMarkersFromDao() = runTest {
        val dao = FakeRouteDao()
        dao.markersFlow.value = listOf(MarkerEntity(1L, "Home", 1.0, 2.0))
        val vm = AddAndEditRouteViewModel(dao, FakeServiceConnection())

        vm.loadMarkers()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.markers.size)
        assertEquals("Home", vm.uiState.value.markers[0].name)
    }

    @Test
    fun initializeFromImport_populatesNameDescriptionAndMembers() = runTest {
        val vm = AddAndEditRouteViewModel(FakeRouteDao(), FakeServiceConnection())

        val route = RouteWithMarkers(
            route = RouteEntity(0, "Imported route", "Imported description"),
            markers = listOf(MarkerEntity(0, "Waypoint 1", 1.0, 2.0)),
        )
        vm.initializeFromImport(route)

        val state = vm.uiState.value
        assertEquals("Imported route", state.name)
        assertEquals("Imported description", state.description)
        assertEquals(1, state.routeMembers.size)
        assertEquals(0L, state.routeObjectId)
        assertTrue(state.showDoneButton)
    }

    // Note: AddAndEditRouteViewModel's "route not found" branches (initializeRouteFromDatabase,
    // deleteRoute, editComplete failures) all call getString(Res.string.error_message_*) from
    // inside a viewModelScope-launched coroutine. That relies on a real platform resource
    // environment that compose-resources' getString() doesn't have under this JVM host test
    // (shared:testAndroidHostTest), so exercising those branches throws an uncaught
    // RuntimeException that fails whichever test drives it via advanceUntilIdle() - even though
    // the assertion itself never runs. There's no fake to substitute for this (it isn't a
    // RouteDao/ServiceConnection dependency), so those error-message branches are left
    // uncovered here; RouteDetailsViewModel avoids the same trap because its only reachable
    // "not found" case just returns null rather than throwing.

    @Test
    fun initializeRouteFromDatabase_found_populatesState() = runTest {
        val dao = FakeRouteDao()
        val routeId = dao.insertRoute(RouteEntity(name = "Loop", description = "desc"))
        val markerId = dao.insertMarker(MarkerEntity(name = "M1", longitude = 1.0, latitude = 2.0))
        dao.addMarkerToRoute(RouteMarkerCrossRef(routeId, markerId, 0))
        val vm = AddAndEditRouteViewModel(dao, FakeServiceConnection())

        vm.initializeRouteFromDatabase(routeId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Loop", state.name)
        assertEquals(routeId, state.routeObjectId)
        assertEquals(1, state.routeMembers.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun editComplete_withNewMarkers_savesRouteAndMarksDone() = runTest {
        val dao = FakeRouteDao()
        val vm = AddAndEditRouteViewModel(dao, FakeServiceConnection())
        vm.onNameChange("New route")
        vm.onDescriptionChange("New description")

        val member = LocationDescription(name = "Stop 1", location = LngLatAlt(1.0, 2.0), databaseId = 0L)
        vm.editComplete(listOf(member))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.doneActionCompleted)
        assertEquals(ActionType.UPDATE, vm.uiState.value.actionType)
        assertEquals(1, dao.routesFlow.value.size)
        assertEquals("New route", dao.routesFlow.value[0].name)
        assertEquals(1, dao.markersFlow.value.size)
    }

    @Test
    fun deleteRoute_removesRouteAndMarksDone() = runTest {
        val dao = FakeRouteDao()
        val routeId = dao.insertRoute(RouteEntity(name = "ToDelete", description = "d"))
        val vm = AddAndEditRouteViewModel(dao, FakeServiceConnection())

        vm.deleteRoute(routeId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(routeId, dao.removeRouteCalledWith)
        assertTrue(vm.uiState.value.doneActionCompleted)
        assertEquals(ActionType.DELETE, vm.uiState.value.actionType)
        assertTrue(dao.routesFlow.value.isEmpty())
    }

    @Test
    fun resetDoneActionState_clearsCompletionFlags() = runTest {
        val dao = FakeRouteDao()
        val routeId = dao.insertRoute(RouteEntity(name = "ToDelete", description = "d"))
        val vm = AddAndEditRouteViewModel(dao, FakeServiceConnection())
        vm.deleteRoute(routeId)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.resetDoneActionState()

        assertFalse(vm.uiState.value.doneActionCompleted)
        assertEquals(ActionType.NONE, vm.uiState.value.actionType)
    }

    @Test
    fun clearErrorMessage_isANoOpWhenThereIsNoError() = runTest {
        val vm = AddAndEditRouteViewModel(FakeRouteDao(), FakeServiceConnection())

        vm.clearErrorMessage()

        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun toggleMember_addsAndRemovesFromToggledMembers() = runTest {
        val vm = AddAndEditRouteViewModel(FakeRouteDao(), FakeServiceConnection())
        val member = LocationDescription(name = "M", location = LngLatAlt(1.0, 2.0), databaseId = 5L)

        vm.toggleMember(member)
        assertEquals(1, vm.uiState.value.toggledMembers.size)

        vm.toggleMember(member)
        assertTrue(vm.uiState.value.toggledMembers.isEmpty())
    }

    @Test
    fun onClickFolder_and_onClickBack_updateEmbeddedPlacesNearbyLevel() = runTest {
        val vm = AddAndEditRouteViewModel(FakeRouteDao(), FakeServiceConnection())

        vm.onClickFolder("filter", "title")
        assertEquals(1, vm.logic.uiState.value.level)
        assertEquals("filter", vm.logic.uiState.value.filter)

        vm.onClickBack()
        assertEquals(0, vm.logic.uiState.value.level)
    }

    @Test
    fun createAndAddMarker_newLocation_createsMarkerAndSpeaksSuccess() = runTest {
        val dao = FakeRouteDao()
        val service = FakeMediaControllableService()
        val vm = AddAndEditRouteViewModel(dao, FakeServiceConnection(service = service))

        val location = LocationDescription(name = "New place", location = LngLatAlt(3.0, 4.0))
        vm.createAndAddMarker(location, "success", "failure", "duplicate")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, dao.markersFlow.value.size)
        assertEquals(1, service.speakCalloutCalls.size)
        assertEquals("success", service.speakCalloutCalls[0]?.positionedStrings?.get(0)?.text)
    }

    @Test
    fun createAndAddMarker_existingLocationNotInRoute_addsToToggledAndSpeaksSuccess() = runTest {
        val dao = FakeRouteDao()
        dao.insertMarker(MarkerEntity(name = "Existing", longitude = 3.0, latitude = 4.0))
        val service = FakeMediaControllableService()
        val vm = AddAndEditRouteViewModel(dao, FakeServiceConnection(service = service))

        val location = LocationDescription(name = "Existing", location = LngLatAlt(3.0, 4.0))
        vm.createAndAddMarker(location, "success", "failure", "duplicate")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.toggledMembers.size)
        assertEquals("success", service.speakCalloutCalls[0]?.positionedStrings?.get(0)?.text)
    }
}
