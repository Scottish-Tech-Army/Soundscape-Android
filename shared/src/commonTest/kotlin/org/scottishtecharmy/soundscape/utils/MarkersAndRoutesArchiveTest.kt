package org.scottishtecharmy.soundscape.utils

import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef
import org.scottishtecharmy.soundscape.database.local.dao.FakeRouteDao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The archive is what both Settings → Advanced markers and routes and the iCloud backup store, so
 * what matters is that a library survives the round trip intact.
 */
class MarkersAndRoutesArchiveTest {

    @Test
    fun aLibrarySurvivesTheRoundTrip() = runTest {
        val source = FakeRouteDao()
        val stop = source.insertMarker(
            MarkerEntity(name = "Stop 1", longitude = 1.0, latitude = 2.0, fullAddress = "A road"),
        )
        val routeId = source.insertRoute(RouteEntity(name = "Loop", description = "scenic"))
        source.addMarkerToRoute(RouteMarkerCrossRef(routeId, stop, 0))
        source.insertMarker(MarkerEntity(name = "Standalone", longitude = 3.0, latitude = 4.0))

        val restored = FakeRouteDao()
        restoreMarkersAndRoutesArchive(buildMarkersAndRoutesArchive(source), restored)

        assertEquals(
            listOf("Standalone", "Stop 1"),
            restored.getAllMarkers().map { it.name }.sorted(),
        )
        val route = restored.getAllRoutesWithMarkers().single()
        assertEquals("Loop", route.route.name)
        assertEquals("scenic", route.route.description)
        assertEquals(listOf("Stop 1"), route.markers.map { it.name })
        assertEquals("A road", restored.getAllMarkers().first { it.name == "Stop 1" }.fullAddress)
    }

    @Test
    fun waypointOrderIsPreserved() = runTest {
        val source = FakeRouteDao()
        val first = source.insertMarker(MarkerEntity(name = "First", longitude = 1.0, latitude = 1.0))
        val second = source.insertMarker(MarkerEntity(name = "Second", longitude = 2.0, latitude = 2.0))
        val third = source.insertMarker(MarkerEntity(name = "Third", longitude = 3.0, latitude = 3.0))
        val routeId = source.insertRoute(RouteEntity(name = "Ordered", description = ""))
        source.addMarkerToRoute(RouteMarkerCrossRef(routeId, third, 2))
        source.addMarkerToRoute(RouteMarkerCrossRef(routeId, first, 0))
        source.addMarkerToRoute(RouteMarkerCrossRef(routeId, second, 1))

        val restored = FakeRouteDao()
        restoreMarkersAndRoutesArchive(buildMarkersAndRoutesArchive(source), restored)

        assertEquals(
            listOf("First", "Second", "Third"),
            restored.getAllRoutesWithMarkers().single().markers.map { it.name },
        )
    }

    /**
     * Users name markers, and so does map data - "Marks & Spencer" is a real shop. Unescaped, the
     * ampersand makes the GPX invalid and the parser drops the entire document, taking every other
     * marker in it with it.
     */
    @Test
    fun namesWithXmlSyntaxInThemSurviveTheRoundTrip() = runTest {
        val source = FakeRouteDao()
        source.insertMarker(
            MarkerEntity(
                name = "Marks & Spencer",
                longitude = 1.0,
                latitude = 2.0,
                fullAddress = "<no address> \"quoted\"",
            ),
        )
        source.insertMarker(MarkerEntity(name = "Ordinary", longitude = 3.0, latitude = 4.0))

        val restored = FakeRouteDao()
        restoreMarkersAndRoutesArchive(buildMarkersAndRoutesArchive(source), restored)

        val markers = restored.getAllMarkers()
        assertEquals(2, markers.size, "the whole document is lost if one name breaks the XML")
        val awkward = markers.first { it.name.startsWith("Marks") }
        assertEquals("Marks & Spencer", awkward.name)
        assertEquals("<no address> \"quoted\"", awkward.fullAddress)
    }

    @Test
    fun anEmptyLibraryRoundTripsToAnEmptyOne() = runTest {
        val restored = FakeRouteDao()
        restoreMarkersAndRoutesArchive(buildMarkersAndRoutesArchive(FakeRouteDao()), restored)

        assertEquals(0, restored.getAllMarkers().size)
        assertEquals(0, restored.getAllRoutes().size)
    }

    // MARK: the single-string envelope the iCloud backup stores

    @Test
    fun theEnvelopeRoundTripsAnArchive() = runTest {
        val source = FakeRouteDao()
        source.insertMarker(MarkerEntity(name = "Home", longitude = 1.0, latitude = 2.0))
        val archive = buildMarkersAndRoutesArchive(source)

        val decoded = decodeArchiveEnvelope(encodeArchiveEnvelope(archive))

        assertNotNull(decoded)
        assertEquals(archive.map { it.filename }, decoded.map { it.filename })
        assertEquals(archive.map { it.content }, decoded.map { it.content })
    }

    @Test
    fun aLibraryRestoresFromItsEnvelope() = runTest {
        val source = FakeRouteDao()
        source.insertMarker(MarkerEntity(name = "Home", longitude = 1.0, latitude = 2.0))

        val envelope = encodeArchiveEnvelope(buildMarkersAndRoutesArchive(source))
        val restored = FakeRouteDao()
        restoreMarkersAndRoutesArchive(assertNotNull(decodeArchiveEnvelope(envelope)), restored)

        assertEquals(listOf("Home"), restored.getAllMarkers().map { it.name })
    }

    @Test
    fun somethingThatIsntAnEnvelopeIsRejected() {
        assertNull(decodeArchiveEnvelope("not json"))
        assertNull(decodeArchiveEnvelope("{}"))
        assertNull(decodeArchiveEnvelope("""{"version": 1}"""))
    }

    @Test
    fun anEnvelopeFromANewerAppIsRefusedRatherThanHalfRestored() {
        // Restoring only the parts this version understands would quietly drop whatever the newer
        // one added, and the user would have no way of knowing.
        assertNull(decodeArchiveEnvelope("""{"version": 99, "files": []}"""))
    }

    @Test
    fun theEnvelopeIsSmallEnoughForICloud() = runTest {
        // iCloud allows 1 MB per key. This is the ceiling that governs how large a library can be
        // backed up, so it's worth knowing roughly where it sits.
        val source = FakeRouteDao()
        repeat(1000) { index ->
            source.insertMarker(
                MarkerEntity(
                    name = "Marker number $index",
                    longitude = index.toDouble(),
                    latitude = index.toDouble(),
                    fullAddress = "$index Some Reasonably Long Street Name, A Town",
                ),
            )
        }

        val bytes = encodeArchiveEnvelope(buildMarkersAndRoutesArchive(source))
            .encodeToByteArray().size

        assertTrue(bytes < 900 * 1024, "1000 markers came to $bytes bytes")
    }
}
