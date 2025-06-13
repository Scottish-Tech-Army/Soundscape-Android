package org.scottishtecharmy.soundscape

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.utils.parseGpxFile

// The GPX parsing code requires various pieces of Android infrastructure, so
// this is an Instrumentation test rather than a unit test.


@RunWith(AndroidJUnit4::class)
class GpxTest {
    private fun testParsing(
        filename: String,
        expectedValues: List<MarkerEntity>,
        expectedName: String = "",
        expectedDescription: String = "",
        nameOverride: String? = null,
    ) : Long {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val input = context.assets.open(filename)

        val routeData = parseGpxFile(input)
        if (routeData == null) {
            Assert.fail("Parsing GPX file failed")
            return -1
        }

        Assert.assertEquals(expectedValues.size, routeData.markers.size)
        Assert.assertEquals(expectedName, routeData.route.name)
        Assert.assertEquals(expectedDescription, routeData.route.description)
        var index = 0
        for (point in routeData.markers) {
            Log.d(
                "gpxTest",
                "Point: " + point.name + " " + point.latitude + " " + point.longitude
            )

            // Lookup the point in the expected values map
            Assert.assertEquals(point, expectedValues[index])
            index += 1
        }

        if (nameOverride != null) {
            routeData.route.name = nameOverride
        }

        // The parsing has succeeded, write the result to the database
        val roomDb = MarkersAndRoutesDatabase.getMarkersInstance(context)
        Log.d("gpxTest", "Successfully opened an in-memory realm")

        val routesDao = roomDb.routeDao()

        var id = 0L
        runBlocking {
            launch {
                // Round trip the routeData to the database
                Log.d("gpxTest", "Inserting route")
                id = routesDao.insertRouteWithNewMarkers (routeData.route, routeData.markers)
                Log.d("gpxTest", "Retrieving route")
                val route = routesDao.getRouteWithMarkers(id)
                Assert.assertEquals(expectedValues.size, route?.markers?.size)
                Assert.assertEquals(routeData, route)
            }
        }

        return id
    }

    private fun testDatabase(
        id: Long,
        expectedValues: List<MarkerEntity>,
    ) {
        // Open the database
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val roomDb = MarkersAndRoutesDatabase.getMarkersInstance(context)

        val routesDao = roomDb.routeDao()
        runBlocking {
            launch {
                // Query waypoints directly
                var waypoints = routesDao.getAllMarkers()
                val waypointCount = waypoints.size
                Log.d("gpxTest", "Retrieved waypoints: " + waypoints.size)

                // Get the route for use later
                var routeData = routesDao.getRouteWithMarkers(id)

                // Delete the route and check that it's gone
                Log.d("gpxTest", "Delete route")
                routesDao.removeRoute(routeData!!.route.routeId)

// Deleting the route no longer deletes the waypoints
//              waypointCount -= routeData.waypoints.size

                waypoints = routesDao.getAllMarkers()
                Log.d("gpxTest", "Post delete waypoints: " + waypoints.size)
                Assert.assertEquals(waypointCount, waypoints.size)

                // We allow "double insertion" it should update the old route
                Log.d("gpxTest", "Attempt inserting duplicate route")
                routesDao.insertRouteWithNewMarkers(
                    routeData.route,
                    routeData.markers
                )

//              Re-inserting the same route shouldn't change the number of waypoints, as they
//              should still be present after the first insert/delete
//              waypointCount += routeData.waypoints.size
                //assert(routesDao.insert(routeData))
                waypoints = routesDao.getAllMarkers()
                Assert.assertEquals(waypointCount, waypoints.size)

                // Check there's still a route left
                val routeData2 = routesDao.getRouteWithMarkers(id)
                //Assert.assertEquals(1, routes.size)
                Assert.assertEquals(routeData, routeData2)

                // Check that we can't get a non-existent route
                val invalidRoute = routesDao.getRouteWithMarkers(-1)
                Assert.assertEquals(null, invalidRoute)

                // Try deleting a non-existent route
                routesDao.removeRoute(-1)

                waypoints = routesDao.getAllMarkers()
                Assert.assertEquals(waypointCount, waypoints.size)

                // Update the database route with the waypoints reversed
                routeData.markers = routeData.markers.reversed()
                routesDao.insertRouteWithNewMarkers(routeData.route, routeData.markers)
                waypoints = routesDao.getAllMarkers()
                Assert.assertEquals(waypointCount, waypoints.size)

                // Get the route back from the database and check that the waypoints are reversed
                routeData = routesDao.getRouteWithMarkers(id)
                Assert.assertEquals(expectedValues.size, routeData!!.markers.size)
                var index = 0
                for (point in routeData.markers.reversed()) {
                    Assert.assertEquals(point, expectedValues[index])
                    index += 1
                }

                // Delete that route to leave the database empty for the next test
                routesDao.removeRoute(id)

// Deleting the route no longer deletes the waypoints
//                waypointCount -= routeData.waypoints.size

                // Check there's no route left
                routeData = routesDao.getRouteWithMarkers(id)
                Assert.assertEquals(null, routeData)

                waypoints = routesDao.getAllMarkers()
                Assert.assertEquals(waypointCount, waypoints.size)
            }
        }
    }

    private fun expectedHandcraftedValues(): List<MarkerEntity> {
        val waypoint1 = MarkerEntity(0, "George Square, Glasgow", -4.2499327, 55.8610697)
        val waypoint2 = MarkerEntity(0, "Edinburgh Castle", -3.2021476, 55.9488161)
        val waypoint3 = MarkerEntity(0, "Greenwich Prime Meridian, London", 0.0, 51.4779644)
        val waypoint4 = MarkerEntity(0, "Rock of Gibraltar", -5.392716, 36.1636308)
        val waypoint5 = MarkerEntity(0, "Inverness", -4.263057, 57.4680357)

        return listOf(waypoint1, waypoint2, waypoint3, waypoint4, waypoint5)
    }

    private fun expectedRideWithGpsValues(): List<MarkerEntity> {
        val waypoint1 = MarkerEntity(0, "Slight Left", -4.30844, 55.94722)
        val waypoint2 = MarkerEntity(0, "Right", -4.30901, 55.94628)
        val waypoint3 = MarkerEntity(0, "Right", -4.31081, 55.9442)
        val waypoint4 = MarkerEntity(0, "Left", -4.31335, 55.94245)
        val waypoint5 = MarkerEntity(0, "Right", -4.31338, 55.94181)
        val waypoint6 = MarkerEntity(0, "Right", -4.31358, 55.94182)
        val waypoint7 = MarkerEntity(0, "Right", -4.31612, 55.94198)
        val waypoint8 = MarkerEntity(0, "Left", -4.31637, 55.94229)
        val waypoint9 = MarkerEntity(0, "Right", -4.31735, 55.94218)

        return listOf(
            waypoint1,
            waypoint2,
            waypoint3,
            waypoint4,
            waypoint5,
            waypoint6,
            waypoint7,
            waypoint8,
            waypoint9,
        )
    }

    private fun expectedSoundscapeValues(): List<MarkerEntity> {
        val waypoint1 = MarkerEntity(0, "Waypoint", -4.305852, 55.947256)
        val waypoint2 = MarkerEntity(0, "Waypoint", -4.305621, 55.946412)
        val waypoint3 = MarkerEntity(0, "Waypoint", -4.304833, 55.946409)
        val waypoint4 = MarkerEntity(0, "Waypoint", -4.304774, 55.945706)
        val waypoint5 = MarkerEntity(0, "Waypoint", -4.307563, 55.945727)
        val waypoint6 = MarkerEntity(0, "Waypoint", -4.307574, 55.946424)
        val waypoint7 = MarkerEntity(0, "Waypoint", -4.305745, 55.946436)
        val waypoint8 = MarkerEntity(0, "Waypoint", -4.305696, 55.947286)

        return listOf(
            waypoint1,
            waypoint2,
            waypoint3,
            waypoint4,
            waypoint5,
            waypoint6,
            waypoint7,
            waypoint8,
        )
    }

    // Test the database more thoroughly
    @Test
    fun handcraftedDatabase() {
        val expectedValues = expectedHandcraftedValues()
        val id = testParsing("gpx/handcrafted.gpx", expectedValues, "Handcrafted", "Handcrafted description")
        testDatabase(id, expectedValues)
    }

    @Test
    fun rideWithGpsDatabase() {
        val expectedValues = expectedRideWithGpsValues()
        val id = testParsing("gpx/rideWithGps.gpx", expectedValues, "RideWithGps", "")
        testDatabase(id, expectedValues)
    }

    @Test
    fun soundscapeDatabase() {
        val expectedValues = expectedSoundscapeValues()
        val id = testParsing("gpx/soundscape.gpx", expectedValues, "Soundscape", "Soundscape description")
        testDatabase(id, expectedValues)
    }

    @Test
    fun soundscapeDuplicateDatabase() {
        val expectedValues = expectedSoundscapeValues()
        val id1 = testParsing("gpx/soundscape.gpx", expectedValues, "Soundscape", "Soundscape description")
        val id2 = testParsing("gpx/soundscape.gpx", expectedValues, "Soundscape", "Soundscape description", "Soundscape2")
        testDatabase(id2, expectedValues)
        testDatabase(id1, expectedValues)
    }

    // Double check multiple routes at once in the database
    @Test
    fun allRoutes() {
        // Put multiple routes into the database
        val handcraftedExpectedValues = expectedHandcraftedValues()
        val id1 = testParsing("gpx/handcrafted.gpx", handcraftedExpectedValues, "Handcrafted", "Handcrafted description")
        val rideWithGpsExpectedValues = expectedRideWithGpsValues()
        val id2 = testParsing("gpx/rideWithGps.gpx", rideWithGpsExpectedValues, "RideWithGps", "")
        val soundscapeExpectedValues = expectedSoundscapeValues()
        val id3 = testParsing("gpx/soundscape.gpx", soundscapeExpectedValues, "Soundscape", "Soundscape description")
        val id4 = testParsing("gpx/soundscape.gpx", soundscapeExpectedValues, "Soundscape", "Soundscape description", "Soundscape2")

        // And test each of them
        testDatabase(id4, soundscapeExpectedValues)
        testDatabase(id2, rideWithGpsExpectedValues)
        testDatabase(id1, handcraftedExpectedValues)
        testDatabase(id3, soundscapeExpectedValues)
    }
}
