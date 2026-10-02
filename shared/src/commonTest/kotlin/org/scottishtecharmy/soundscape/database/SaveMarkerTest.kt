package org.scottishtecharmy.soundscape.database

import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.database.local.dao.FakeRouteDao
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import kotlin.test.Test
import kotlin.test.assertEquals

class SaveMarkerTest {

    private val glasgow = LngLatAlt(-4.2518, 55.8642)

    @Test
    fun newLocation_insertsMarker() = runTest {
        val dao = FakeRouteDao()

        val id = saveMarker(LocationDescription("Home", glasgow), dao)

        val markers = dao.getAllMarkers()
        assertEquals(1, markers.size)
        assertEquals(id, markers[0].markerId)
        assertEquals("Home", markers[0].name)
    }

    @Test
    fun sameLocationAgain_updatesExistingMarkerInsteadOfDuplicating() = runTest {
        val dao = FakeRouteDao()
        val firstId = saveMarker(LocationDescription("Home", glasgow, description = "1 Main St"), dao)

        val secondId = saveMarker(LocationDescription("My house", glasgow, description = "Front door"), dao)

        val markers = dao.getAllMarkers()
        assertEquals(1, markers.size)
        assertEquals(firstId, secondId)
        assertEquals("My house", markers[0].name)
        assertEquals("Front door", markers[0].fullAddress)
    }

    @Test
    fun differentLocation_addsSecondMarker() = runTest {
        val dao = FakeRouteDao()
        saveMarker(LocationDescription("Home", glasgow), dao)

        saveMarker(LocationDescription("Shop", LngLatAlt(-4.2600, 55.8600)), dao)

        assertEquals(2, dao.getAllMarkers().size)
    }

    @Test
    fun existingDatabaseId_updatesThatMarker() = runTest {
        val dao = FakeRouteDao()
        val id = saveMarker(LocationDescription("Home", glasgow), dao)
        val moved = LngLatAlt(-4.2520, 55.8643)

        saveMarker(LocationDescription("Home", moved, databaseId = id), dao)

        val markers = dao.getAllMarkers()
        assertEquals(1, markers.size)
        assertEquals(moved.latitude, markers[0].latitude)
        assertEquals(moved.longitude, markers[0].longitude)
    }

    @Test
    fun emptyName_fallsBackToDescription() = runTest {
        val dao = FakeRouteDao()

        saveMarker(LocationDescription("", glasgow, description = "1 Main St"), dao)

        assertEquals("1 Main St", dao.getAllMarkers()[0].name)
    }
}
