package org.scottishtecharmy.soundscape

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.copyFromRealm
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.database.local.dao.RoutesDao
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.utils.parseGpxFile

// The GPX parsing code requires various pieces of Android infrastructure, so
// this is an Instrumentation test rather than a unit test.

@RunWith(AndroidJUnit4::class)
class GpxTest {
    private fun testParsing(
        filename: String,
        expectedValues: List<MarkerData>,
        expectedName: String = "",
        expectedDescription: String = "",
        nameOverride: String? = null,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val input = context.assets.open(filename)

        val routeData = parseGpxFile(input)
        Assert.assertEquals(expectedValues.size, routeData.waypoints.size)
        Assert.assertEquals(expectedName, routeData.name)
        Assert.assertEquals(expectedDescription, routeData.description)
        var index = 0
        for (point in routeData.waypoints) {
            Log.d(
                "gpxTest",
                "Point: " + point.addressName + " " + point.location?.latitude + " " + point.location?.longitude,
            )

            // Lookup the point in the expected values map
            Assert.assertEquals(point, expectedValues[index])
            index += 1
        }

        if (nameOverride != null) {
            routeData.name = nameOverride
        }

        // The parsing has succeeded, write the result to the database
        val config =
            RealmConfiguration
                .Builder(
                    schema =
                        setOf(
                            RouteData::class,
                            MarkerData::class,
                            Location::class
                        ),
                ).inMemory()
                .build()
        val realm = Realm.open(config)
        Log.d("gpxTest", "Successfully opened an in-memory realm")

        val routesDao = RoutesDao(realm)
        val routesRepository = RoutesRepository(routesDao)

        runBlocking {
            launch {
                // Round trip the routeData to the database
                Log.d("gpxTest", "Inserting route")
                routesRepository.insertRoute(routeData)
                Log.d("gpxTest", "Retrieving route")
                val routes = routesRepository.getRoute(routeData.name)
                if(routes.size != 1) {
                    Log.e("gpxTest", "Expected 1 route, got ${routes.size}")
                }
                Assert.assertEquals(1, routes.size)
                Assert.assertEquals(routeData, routes[0])
            }
        }
    }

    private fun testDatabase(
        name: String,
        expectedValues: List<MarkerData>,
    ) {
        // Open the database
        val config =
            RealmConfiguration
                .Builder(
                    schema =
                        setOf(
                            RouteData::class,
                            MarkerData::class,
                            Location::class,
                        ),
                ).inMemory()
                .build()
        val realm = Realm.open(config)
        Log.d("gpxTest", "Successfully opened an in-memory realm")

        val routesDao = RoutesDao(realm)
        val routesRepository = RoutesRepository(routesDao)
        runBlocking {
            launch {
                // Query waypoints directly
                var waypoints = routesRepository.getMarkers()
                val waypointCount = waypoints.size
                Log.d("gpxTest", "Retrieved waypoints: " + waypoints.size)

                // Geospatial test - pick the first waypoint and get waypoints near it. Should
                // always be greater than 0 as the first waypoint is itself
                waypoints = routesRepository.getMarkersNear(waypoints[0].location, 0.1)
                Log.d("gpxTest", "Retrieved geo waypoints: " + waypoints.size)
                Assert.assertTrue(waypoints.isNotEmpty())

                // Get the route for use later
                val dbRoutes = routesRepository.getRoute(name)
                Assert.assertEquals(1, dbRoutes.size)
                val routeData = dbRoutes[0].copyFromRealm()

                // Delete the route and check that it's gone
                Log.d("gpxTest", "Delete route")
                routesRepository.deleteRoute(routeData.objectId)

// Deleting the route no longer deletes the waypoints
//              waypointCount -= routeData.waypoints.size

                var routes = routesRepository.getRoute(name)
                Assert.assertEquals(0, routes.size)
                waypoints = routesRepository.getMarkers()
                Log.d("gpxTest", "Post delete waypoints: " + waypoints.size)
                Assert.assertEquals(waypointCount, waypoints.size)

                // We allow "double insertion" it should update the old route
                Log.d("gpxTest", "Attempt inserting duplicate route")
                routesRepository.insertRoute(routeData)

//              Re-inserting the same route shouldn't change the number of waypoints, as they
//              should still be present after the first insert/delete
//              waypointCount += routeData.waypoints.size
                assert(routesRepository.insertRoute(routeData))
                waypoints = routesRepository.getMarkers()
                Assert.assertEquals(waypointCount, waypoints.size)

                // Check there's still a route left
                routes = routesRepository.getRoute(name)
                Assert.assertEquals(1, routes.size)
                Assert.assertEquals(routeData, routes[0])

                // Check that we can't get a non-existent route
                routes = routesRepository.getRoute("non-existent-route")
                Assert.assertEquals(0, routes.size)

                // Try deleting a non-existent route
                routesRepository.deleteRoute(ObjectId())

                waypoints = routesRepository.getMarkers()
                Assert.assertEquals(waypointCount, waypoints.size)

                // Update the database route with the waypoints reversed
                routeData.waypoints.reverse()
                routesRepository.updateRoute(routeData)
                waypoints = routesRepository.getMarkers()
                Assert.assertEquals(waypointCount, waypoints.size)

                // Get the route back from the database and check that the waypoints are reversed
                routes = routesRepository.getRoute(name)
                Assert.assertEquals(1, routes.size)
                Assert.assertEquals(expectedValues.size, routes[0].waypoints.size)
                var index = 0
                for (point in routes[0].waypoints.reversed()) {
                    Assert.assertEquals(point, expectedValues[index])
                    index += 1
                }

                // Delete that route to leave the database empty for the next test
                routesRepository.deleteRoute(routeData.objectId)

// Deleting the route no longer deletes the waypoints
//                waypointCount -= routeData.waypoints.size

                // Check there's no route left
                routes = routesRepository.getRoute(name)
                Assert.assertEquals(0, routes.size)

                waypoints = routesRepository.getMarkers()
                Assert.assertEquals(waypointCount, waypoints.size)
            }
        }
    }

    private fun expectedHandcraftedValues(): List<MarkerData> {
        val waypoint1 = MarkerData("George Square, Glasgow", Location(55.8610697, -4.2499327))
        val waypoint2 = MarkerData("Edinburgh Castle", Location(55.9488161, -3.2021476))
        val waypoint3 = MarkerData("Greenwich Prime Meridian, London", Location(51.4779644, 0.0))
        val waypoint4 = MarkerData("Rock of Gibraltar", Location(36.1636308, -5.392716))
        val waypoint5 = MarkerData("Inverness", Location(57.4680357, -4.263057))

        return listOf(waypoint1, waypoint2, waypoint3, waypoint4, waypoint5)
    }

    private fun expectedRideWithGpsValues(): List<MarkerData> {
        val waypoint1 = MarkerData("Slight Left", Location(55.94722, -4.30844))
        val waypoint2 = MarkerData("Right", Location(55.94628, -4.30901))
        val waypoint3 = MarkerData("Right", Location(55.9442, -4.31081))
        val waypoint4 = MarkerData("Left", Location(55.94245, -4.31335))
        val waypoint5 = MarkerData("Right", Location(55.94181, -4.31338))
        val waypoint6 = MarkerData("Right", Location(55.94182, -4.31358))
        val waypoint7 = MarkerData("Right", Location(55.94198, -4.31612))
        val waypoint8 = MarkerData("Left", Location(55.94229, -4.31637))
        val waypoint9 = MarkerData("Right", Location(55.94218, -4.31735))

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

    private fun expectedSoundscapeValues(): List<MarkerData> {
        val waypoint1 = MarkerData("Waypoint", Location(55.947256, -4.305852))
        val waypoint2 = MarkerData("Waypoint", Location(55.946412, -4.305621))
        val waypoint3 = MarkerData("Waypoint", Location(55.946409, -4.304833))
        val waypoint4 = MarkerData("Waypoint", Location(55.945706, -4.304774))
        val waypoint5 = MarkerData("Waypoint", Location(55.945727, -4.307563))
        val waypoint6 = MarkerData("Waypoint", Location(55.946424, -4.307574))
        val waypoint7 = MarkerData("Waypoint", Location(55.946436, -4.305745))
        val waypoint8 = MarkerData("Waypoint", Location(55.947286, -4.305696))

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
        testParsing("gpx/handcrafted.gpx", expectedValues, "Handcrafted", "Handcrafted description")
        testDatabase("Handcrafted", expectedValues)
    }

    @Test
    fun rideWithGpsDatabase() {
        val expectedValues = expectedRideWithGpsValues()
        testParsing("gpx/rideWithGps.gpx", expectedValues, "RideWithGps", "")
        testDatabase("RideWithGps", expectedValues)
    }

    @Test
    fun soundscapeDatabase() {
        val expectedValues = expectedSoundscapeValues()
        testParsing("gpx/soundscape.gpx", expectedValues, "Soundscape", "Soundscape description")
        testDatabase("Soundscape", expectedValues)
    }

    @Test
    fun soundscapeDuplicateDatabase() {
        val expectedValues = expectedSoundscapeValues()
        testParsing("gpx/soundscape.gpx", expectedValues, "Soundscape", "Soundscape description")
        testParsing("gpx/soundscape.gpx", expectedValues, "Soundscape", "Soundscape description", "Soundscape2")
        testDatabase("Soundscape2", expectedValues)
        testDatabase("Soundscape", expectedValues)
    }

    // Double check multiple routes at once in the database
    @Test
    fun allRoutes() {
        // Put multiple routes into the database
        val handcraftedExpectedValues = expectedHandcraftedValues()
        testParsing("gpx/handcrafted.gpx", handcraftedExpectedValues, "Handcrafted", "Handcrafted description")
        val rideWithGpsExpectedValues = expectedRideWithGpsValues()
        testParsing("gpx/rideWithGps.gpx", rideWithGpsExpectedValues, "RideWithGps", "")
        val soundscapeExpectedValues = expectedSoundscapeValues()
        testParsing("gpx/soundscape.gpx", soundscapeExpectedValues, "Soundscape", "Soundscape description")
        testParsing("gpx/soundscape.gpx", soundscapeExpectedValues, "Soundscape", "Soundscape description", "Soundscape2")

        // And test each of them
        testDatabase("Soundscape", soundscapeExpectedValues)
        testDatabase("RideWithGps", rideWithGpsExpectedValues)
        testDatabase("Handcrafted", handcraftedExpectedValues)
        testDatabase("Soundscape2", soundscapeExpectedValues)
    }
}
