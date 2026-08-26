package org.scottishtecharmy.soundscape.screens.home.locationDetails

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.audio.AudioTourHost
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef
import org.scottishtecharmy.soundscape.geoengine.GridState
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
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LocationDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(
        service: MediaControllableService? = FakeMediaControllableService(),
        dao: FakeRouteDao = FakeRouteDao(),
    ): Triple<LocationDetailsViewModel, FakeRouteDao, MediaControllableService?> {
        val connection = FakeServiceConnection(service = service)
        val vm = LocationDetailsViewModel(connection, dao, AudioTour(FakeAudioTourHost()))
        return Triple(vm, dao, service)
    }

    @Test
    fun startBeacon_delegatesToBoundService() = runTest {
        val service = FakeMediaControllableService()
        val (vm, _, _) = newViewModel(service)
        val location = LngLatAlt(-4.25, 55.86)

        vm.startBeacon(location, "Home")

        assertEquals(listOf(location to "Home"), service.startBeaconCalls)
    }

    @Test
    fun enableStreetPreview_delegatesToBoundService() = runTest {
        val service = FakeMediaControllableService()
        val (vm, _, _) = newViewModel(service)
        val location = LngLatAlt(1.0, 2.0)

        vm.enableStreetPreview(location)

        assertEquals(listOf<Pair<Boolean, LngLatAlt?>>(true to location), service.setStreetPreviewModeCalls)
    }

    @Test
    fun getLocationDescription_delegatesToService() = runTest {
        val service = FakeMediaControllableService()
        val expected = LocationDescription(name = "Result", location = LngLatAlt(1.0, 2.0))
        service.locationDescriptionToReturn = expected
        val (vm, _, _) = newViewModel(service)

        assertEquals(expected, vm.getLocationDescription(LngLatAlt(1.0, 2.0)))
    }

    @Test
    fun getLocationDescription_noBoundService_returnsNull() = runTest {
        val (vm, _, _) = newViewModel(service = null)

        assertNull(vm.getLocationDescription(LngLatAlt(1.0, 2.0)))
    }

    @Test
    fun createMarker_newLocation_insertsMarkerAndSpeaksSuccess() = runTest {
        val service = FakeMediaControllableService()
        val dao = FakeRouteDao()
        val (vm, _, _) = newViewModel(service, dao)
        val description = LocationDescription(name = "Cafe", location = LngLatAlt(-4.25, 55.86))

        vm.createMarker(description, "Marker saved", "Marker failed", "duplicate")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, dao.markersById.size)
        assertNotEquals(0L, description.databaseId)
        assertEquals(1, service.speakCalloutCalls.size)
        assertEquals(
            "Marker saved",
            service.speakCalloutCalls.single()?.positionedStrings?.single()?.text,
        )
    }

    @Test
    fun createMarker_daoFailure_speaksFailureMessage() = runTest {
        val service = FakeMediaControllableService()
        val dao = FakeRouteDao(failInsert = true)
        val (vm, _, _) = newViewModel(service, dao)
        val description = LocationDescription(name = "Cafe", location = LngLatAlt(-4.25, 55.86))

        vm.createMarker(description, "Marker saved", "Marker failed", "duplicate")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Marker failed",
            service.speakCalloutCalls.single()?.positionedStrings?.single()?.text,
        )
    }

    @Test
    fun deleteMarker_removesFromDao() = runTest {
        val dao = FakeRouteDao()
        val (vm, _, _) = newViewModel(dao = dao)
        val id = dao.seedMarker(MarkerEntity(name = "Cafe", longitude = -4.25, latitude = 55.86))

        vm.deleteMarker(id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, dao.markersById[id])
    }

    @Test
    fun getMarkerAtLocation_returnsDaoResult() = runTest {
        val dao = FakeRouteDao()
        val marker = MarkerEntity(name = "Cafe", longitude = -4.25, latitude = 55.86)
        dao.seedMarker(marker)
        val (vm, _, _) = newViewModel(dao = dao)

        val result = vm.getMarkerAtLocation(LngLatAlt(-4.25, 55.86))

        assertEquals("Cafe", result?.name)
    }

    @Test
    fun showDialog_doesNotThrow() = runTest {
        val (vm, _, _) = newViewModel()

        vm.showDialog()
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
 * Hand-rolled in-memory [MediaControllableService]. Action methods record whether
 * they were invoked (and with what arguments) instead of doing anything real.
 */
private class FakeMediaControllableService : MediaControllableService {
    override val locationFlow: StateFlow<SoundscapeLocation?> = MutableStateFlow(null)
    override val orientationFlow: StateFlow<DeviceDirection?> = MutableStateFlow(null)
    override val headHeadingFlow: StateFlow<HeadHeading?> = MutableStateFlow(null)
    override val beaconFlow: StateFlow<BeaconState> = MutableStateFlow(BeaconState())
    override val currentRouteFlow: StateFlow<RoutePlayerState> = MutableStateFlow(RoutePlayerState())
    override val gridStateFlow: StateFlow<GridState?> = MutableStateFlow(null)
    override val streetPreviewFlow: StateFlow<StreetPreviewState> = MutableStateFlow(StreetPreviewState())
    override val voiceCommandStateFlow: StateFlow<VoiceCommandState> =
        MutableStateFlow(VoiceCommandState.Idle)
    override val filteredLocationFlow: StateFlow<SoundscapeLocation?> = MutableStateFlow(null)

    override var menuActive: Boolean = false

    val startBeaconCalls = mutableListOf<Pair<LngLatAlt, String>>()
    val setStreetPreviewModeCalls = mutableListOf<Pair<Boolean, LngLatAlt?>>()
    val speakCalloutCalls = mutableListOf<TrackedCallout?>()
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

    override fun setStreetPreviewMode(on: Boolean, location: LngLatAlt?) {
        setStreetPreviewModeCalls.add(on to location)
    }

    override fun getLocationDescription(location: LngLatAlt): LocationDescription =
        locationDescriptionToReturn

    override suspend fun searchResult(query: String): List<LocationDescription>? = null
    override fun isAudioEngineBusy(): Boolean = false

    override fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long {
        speakCalloutCalls.add(callout)
        return 0L
    }
}

/**
 * Minimal in-memory [RouteDao], modelled on the equivalent fake in
 * `migration/LegacyMigrationTest.kt`. Only the members [LocationDetailsViewModel] and
 * [org.scottishtecharmy.soundscape.database.createMarker] actually touch are implemented
 * for real; everything else throws to flag unexpected usage.
 */
private class FakeRouteDao(private val failInsert: Boolean = false) : RouteDao {

    private var nextMarkerId: Long = 1
    val markersById = mutableMapOf<Long, MarkerEntity>()

    /** Test helper: seed a marker directly, bypassing [insertMarker]. */
    fun seedMarker(marker: MarkerEntity): Long {
        val id = nextMarkerId++
        markersById[id] = MarkerEntity(
            markerId = id,
            name = marker.name,
            longitude = marker.longitude,
            latitude = marker.latitude,
            fullAddress = marker.fullAddress,
        )
        return id
    }

    override suspend fun insertMarker(marker: MarkerEntity): Long {
        if (failInsert) throw IllegalStateException("insertMarker failed")
        val id = nextMarkerId++
        markersById[id] = MarkerEntity(
            markerId = id,
            name = marker.name,
            longitude = marker.longitude,
            latitude = marker.latitude,
            fullAddress = marker.fullAddress,
        )
        return id
    }

    override suspend fun updateMarker(marker: MarkerEntity) {
        markersById[marker.markerId] = marker
    }

    override suspend fun getMarkerById(markerId: Long): MarkerEntity? = markersById[markerId]

    override suspend fun getMarkerByLocation(longitude: Double, latitude: Double): MarkerEntity? =
        markersById.values.firstOrNull { it.longitude == longitude && it.latitude == latitude }

    override suspend fun removeMarker(markerId: Long) {
        markersById.remove(markerId)
    }

    // --- unused -----------------------------------------------------------
    private fun nope(): Nothing = error("FakeRouteDao: not implemented for tests")

    override suspend fun getAllMarkers(): List<MarkerEntity> = markersById.values.toList()
    override fun getAllMarkersFlow(): Flow<List<MarkerEntity>> = flowOf(emptyList())
    override suspend fun insertRoute(route: RouteEntity): Long = nope()
    override suspend fun addMarkerToRoute(crossRef: RouteMarkerCrossRef) = nope()
    override suspend fun removeMarkerFromRoute(routeId: Long, markerId: Long) = nope()
    override suspend fun removeMarkersForRoute(routeId: Long) = nope()
    override suspend fun getAllRoutes(): List<RouteEntity> = nope()
    override suspend fun getRouteById(routeId: Long): RouteEntity? = nope()
    override suspend fun getMarkerCrossReference(routeId: Long): List<RouteMarkerCrossRef> = nope()
    override fun getAllRoutesFlow(): Flow<List<RouteEntity>> = flowOf(emptyList())
    override suspend fun removeRoute(routeId: Long) = nope()
    override suspend fun deleteAllRouteMarkerCrossRefs() = nope()
    override suspend fun deleteAllMarkers() = nope()
    override suspend fun deleteAllRoutes() = nope()
}
