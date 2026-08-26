package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

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
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
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

private class FakePreferencesProvider : PreferencesProvider {
    private val booleans = mutableMapOf<String, Boolean>()
    private val strings = mutableMapOf<String, String>()
    private val floats = mutableMapOf<String, Float>()

    override fun getBoolean(key: String, default: Boolean): Boolean = booleans[key] ?: default
    override fun getString(key: String, default: String): String = strings[key] ?: default
    override fun getFloat(key: String, default: Float): Float = floats[key] ?: default

    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }

    override fun putString(key: String, value: String) {
        strings[key] = value
    }

    override fun clearAll() {
        booleans.clear()
        strings.clear()
        floats.clear()
    }

    override fun addListener(listener: PreferencesListener) {}
    override fun removeListener(listener: PreferencesListener) {}
}

private class FakeMediaControllableService : MediaControllableService {
    var routeStartByIdCalledWith: Long? = null

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
    override fun routeStartById(routeId: Long) {
        routeStartByIdCalledWith = routeId
    }

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

private class FakeServiceConnection(
    initialBound: Boolean = false,
    override val service: MediaControllableService? = null,
) : ServiceConnection {
    override val serviceBoundState: StateFlow<Boolean> = MutableStateFlow(initialBound)
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoutesViewModelTest {

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
    fun initialState_marksRoutesScreenAndUsesPrefDefaults() = runTest {
        val vm = RoutesViewModel(FakeRouteDao(), FakePreferencesProvider(), FakeServiceConnection())

        val state = vm.uiState.value
        assertFalse(state.markers)
        assertFalse(state.isSortByName) // distance by default
        assertTrue(state.isSortAscending)
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun initialState_readsPersistedSortPreferences() = runTest {
        val prefs = FakePreferencesProvider()
        prefs.putBoolean(PreferenceKeys.MARKERS_SORT_BY_NAME, true)
        prefs.putBoolean(PreferenceKeys.MARKERS_SORT_ASCENDING, false)

        val vm = RoutesViewModel(FakeRouteDao(), prefs, FakeServiceConnection())

        assertTrue(vm.uiState.value.isSortByName)
        assertFalse(vm.uiState.value.isSortAscending)
    }

    @Test
    fun init_populatesEntriesFromDaoFlow() = runTest {
        val dao = FakeRouteDao()
        val routeId = dao.insertRoute(RouteEntity(name = "Loop", description = "desc"))
        val markerId = dao.insertMarker(MarkerEntity(name = "Start", longitude = 1.0, latitude = 2.0))
        dao.addMarkerToRoute(RouteMarkerCrossRef(routeId, markerId, 0))

        val vm = RoutesViewModel(dao, FakePreferencesProvider(), FakeServiceConnection())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.entries.size)
        assertEquals("Loop", vm.uiState.value.entries[0].name)
    }

    @Test
    fun init_routeWithNoMarkers_usesDefaultLocation() = runTest {
        val dao = FakeRouteDao()
        dao.insertRoute(RouteEntity(name = "Empty route", description = "desc"))

        val vm = RoutesViewModel(dao, FakePreferencesProvider(), FakeServiceConnection())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.entries.size)
        assertEquals(LngLatAlt(), vm.uiState.value.entries[0].location)
    }

    @Test
    fun toggleSortByName_flipsStateAndPersistsPreference() = runTest {
        val prefs = FakePreferencesProvider()
        val vm = RoutesViewModel(FakeRouteDao(), prefs, FakeServiceConnection())
        val initial = vm.uiState.value.isSortByName

        vm.toggleSortByName()

        assertEquals(!initial, vm.uiState.value.isSortByName)
        assertEquals(!initial, prefs.getBoolean(PreferenceKeys.MARKERS_SORT_BY_NAME, initial))
    }

    @Test
    fun toggleSortOrder_flipsStateAndPersistsPreference() = runTest {
        val prefs = FakePreferencesProvider()
        val vm = RoutesViewModel(FakeRouteDao(), prefs, FakeServiceConnection())
        val initial = vm.uiState.value.isSortAscending

        vm.toggleSortOrder()

        assertEquals(!initial, vm.uiState.value.isSortAscending)
        assertEquals(!initial, prefs.getBoolean(PreferenceKeys.MARKERS_SORT_ASCENDING, initial))
    }

    @Test
    fun clearErrorMessage_clearsError() = runTest {
        val vm = RoutesViewModel(FakeRouteDao(), FakePreferencesProvider(), FakeServiceConnection())

        vm.clearErrorMessage()

        assertEquals(null, vm.uiState.value.errorMessage)
    }

    @Test
    fun startRoute_forwardsToService() = runTest {
        val service = FakeMediaControllableService()
        val vm = RoutesViewModel(
            FakeRouteDao(),
            FakePreferencesProvider(),
            FakeServiceConnection(service = service),
        )

        vm.startRoute(99L)

        assertEquals(99L, service.routeStartByIdCalledWith)
    }
}
