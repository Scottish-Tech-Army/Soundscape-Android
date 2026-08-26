package org.scottishtecharmy.soundscape.screens.home.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef
import org.scottishtecharmy.soundscape.utils.MarkersAndRoutesIo
import org.scottishtecharmy.soundscape.utils.NamedGpx
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

private class FakeMarkersAndRoutesIo : MarkersAndRoutesIo {
    var exportedFiles: List<NamedGpx>? = null
    var exportedFilename: String? = null
    var exportedShareTitle: String? = null

    var filesToReturnOnPick: List<NamedGpx>? = null
    var pickShouldThrow: Boolean = false

    override suspend fun exportGpxZip(files: List<NamedGpx>, suggestedFilename: String, shareTitle: String) {
        exportedFiles = files
        exportedFilename = suggestedFilename
        exportedShareTitle = shareTitle
    }

    override suspend fun pickGpxZip(): List<NamedGpx>? {
        if (pickShouldThrow) throw IllegalStateException("picker failed")
        return filesToReturnOnPick
    }
}

private fun gpxFixture(
    name: String,
    desc: String,
    waypoints: List<Triple<String, Double, Double>>,
): String = buildString {
    append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    append("<gpx version=\"1.1\" creator=\"Soundscape\">\n")
    append("  <metadata>\n")
    append("    <name>$name</name>\n")
    append("    <desc>$desc</desc>\n")
    append("  </metadata>\n")
    for ((waypointName, lat, lon) in waypoints) {
        append("      <wpt lat=\"$lat\" lon=\"$lon\">\n")
        append("        <name>$waypointName</name>\n")
        append("        <desc></desc>\n")
        append("      </wpt>\n")
    }
    append("</gpx>")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedMarkersAndRoutesSettingsViewModelTest {

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
    fun initialState_userFeedbackIsEmpty() = runTest {
        val vm = AdvancedMarkersAndRoutesSettingsViewModel(FakeRouteDao(), FakeMarkersAndRoutesIo())

        assertEquals("", vm.userFeedback.value)
    }

    @Test
    fun deleteAllMarkersAndRoutes_clearsDaoAndSetsFeedback() = runTest {
        val dao = FakeRouteDao()
        val routeId = dao.insertRoute(RouteEntity(name = "Loop", description = "desc"))
        val markerId = dao.insertMarker(MarkerEntity(name = "Start", longitude = 1.0, latitude = 2.0))
        dao.addMarkerToRoute(RouteMarkerCrossRef(routeId, markerId, 0))
        val vm = AdvancedMarkersAndRoutesSettingsViewModel(dao, FakeMarkersAndRoutesIo())

        vm.deleteAllMarkersAndRoutes("Deleted everything")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(dao.markersFlow.value.isEmpty())
        assertTrue(dao.routesFlow.value.isEmpty())
        assertTrue(dao.crossRefs.isEmpty())
        assertEquals("Deleted everything", vm.userFeedback.value)
    }

    @Test
    fun exportMarkersAndRoutes_bundlesAllMarkersAndEachRouteAsGpx() = runTest {
        val dao = FakeRouteDao()
        val routeId = dao.insertRoute(RouteEntity(name = "Loop", description = "desc"))
        val routeMarkerId = dao.insertMarker(MarkerEntity(name = "Stop 1", longitude = 1.0, latitude = 2.0))
        dao.addMarkerToRoute(RouteMarkerCrossRef(routeId, routeMarkerId, 0))
        dao.insertMarker(MarkerEntity(name = "Standalone", longitude = 3.0, latitude = 4.0))
        val io = FakeMarkersAndRoutesIo()
        val vm = AdvancedMarkersAndRoutesSettingsViewModel(dao, io)

        vm.exportMarkersAndRoutes("Share title")
        testDispatcher.scheduler.advanceUntilIdle()

        val files = io.exportedFiles
        requireNotNull(files)
        // One file for all standalone markers, plus one per route.
        assertEquals(2, files.size)
        assertEquals("Share title", io.exportedShareTitle)
        assertEquals("soundscape-routes-export", io.exportedFilename)
        assertTrue(files.any { it.content.contains("Standalone") })
        assertTrue(files.any { it.content.contains("Stop 1") })
    }

    @Test
    fun importMarkersAndRoutes_mergesStandaloneMarkersAndAddsRoutes() = runTest {
        val dao = FakeRouteDao()
        val io = FakeMarkersAndRoutesIo()
        io.filesToReturnOnPick = listOf(
            NamedGpx(
                filename = "${AdvancedMarkersAndRoutesSettingsViewModel.GLOBAL_MARKERS_NAME}.gpx",
                content = gpxFixture("markers", "", listOf(Triple("Home", 1.0, 2.0))),
            ),
            NamedGpx(
                filename = "My Route.gpx",
                content = gpxFixture("My Route", "A nice walk", listOf(Triple("Stop 1", 3.0, 4.0))),
            ),
        )
        val vm = AdvancedMarkersAndRoutesSettingsViewModel(dao, io)

        vm.importMarkersAndRoutes("Imported", "Import failed")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Imported", vm.userFeedback.value)
        assertTrue(dao.markersFlow.value.any { it.name == "Home" })
        assertTrue(dao.routesFlow.value.any { it.name == "My Route" })
    }

    @Test
    fun importMarkersAndRoutes_userCancelled_leavesFeedbackEmpty() = runTest {
        val io = FakeMarkersAndRoutesIo()
        io.filesToReturnOnPick = null
        val vm = AdvancedMarkersAndRoutesSettingsViewModel(FakeRouteDao(), io)

        vm.importMarkersAndRoutes("Imported", "Import failed")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", vm.userFeedback.value)
    }

    @Test
    fun importMarkersAndRoutes_pickerThrows_setsFailureFeedback() = runTest {
        val io = FakeMarkersAndRoutesIo()
        io.pickShouldThrow = true
        val vm = AdvancedMarkersAndRoutesSettingsViewModel(FakeRouteDao(), io)

        vm.importMarkersAndRoutes("Imported", "Import failed")
        testDispatcher.scheduler.advanceUntilIdle()

        // pickGpxZip's own try/catch swallows picker failures into a null result, which the
        // ViewModel treats as "cancelled" (no feedback at all), not the failureString.
        assertEquals("", vm.userFeedback.value)
    }

    @Test
    fun importMarkersAndRoutes_unparsableFiles_setsFailureFeedback() = runTest {
        val io = FakeMarkersAndRoutesIo()
        io.filesToReturnOnPick = listOf(NamedGpx("bad.gpx", "not xml at all"))
        val vm = AdvancedMarkersAndRoutesSettingsViewModel(FakeRouteDao(), io)

        vm.importMarkersAndRoutes("Imported", "Import failed")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Import failed", vm.userFeedback.value)
    }

    @Test
    fun userFeedbackShown_resetsFeedback() = runTest {
        val dao = FakeRouteDao()
        val vm = AdvancedMarkersAndRoutesSettingsViewModel(dao, FakeMarkersAndRoutesIo())
        vm.deleteAllMarkersAndRoutes("Done")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Done", vm.userFeedback.value)

        vm.userFeedbackShown()

        assertEquals("", vm.userFeedback.value)
    }
}
