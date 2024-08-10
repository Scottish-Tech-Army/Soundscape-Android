package org.scottishtecharmy.soundscape

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.database.local.dao.RoutesDao
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.local.model.RoutePoint
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.utils.parseGpxFile


// The GPX parsing code requires various pieces of Android infrastructure, so
// this is an Instrumentation test rather than a unit test.

class GpxTest {
    private fun testParsing(filename: String,
                                    expectedValues: List<RoutePoint>,
                                    expectedName: String = "",
                                    expectedDescription: String = "") {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val input = context.assets.open(filename)

        val routeData = parseGpxFile(input)
        Assert.assertEquals(expectedValues.size, routeData.waypoints.size)
        Assert.assertEquals(expectedName, routeData.name)
        Assert.assertEquals(expectedDescription, routeData.description)
        var index = 0
        for (point in routeData.waypoints) {
            Log.d("gpxTest", "Point: " + point.name + " " + point.location?.latitude + " " + point.location?.longitude)

            // Lookup the point in the expected values map
            Assert.assertEquals(point, expectedValues[index])
            index += 1
        }

        // The parsing has succeeded, test writing and retrieving from database
        val config = RealmConfiguration.Builder(schema = setOf(RouteData::class, RoutePoint::class, Location::class))
            .inMemory()
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
                var routes = routesRepository.getRoute(routeData.name)
                Assert.assertEquals(1, routes.size)
                //Assert.assertEquals(routeData, routes[0])

                // Query waypoints directly
                var waypoints = routesRepository.getWaypoints()
                Log.d("gpxTest", "Retrieved waypoints: " + waypoints.size)

                // Geospatial test
                waypoints = routesRepository.getWaypointsNear(waypoints[0].location, 0.1)
                Log.d("gpxTest", "Retrieved geo waypoints: " + waypoints.size)

                // Delete the route and check that it's gone
                Log.d("gpxTest", "Delete route")
                routesRepository.deleteRoute(routeData.name)
                routes = routesRepository.getRoute(routeData.name)
                Assert.assertEquals(0, routes.size)
                waypoints = routesRepository.getWaypoints()
                Log.d("gpxTest", "Post delete waypoints: " + waypoints.size)
                Assert.assertEquals(0, waypoints.size)

                // Check that double insertion is rejected
                Log.d("gpxTest", "Attempt inserting duplicate route")
                routesRepository.insertRoute(routeData)

                if(routesRepository.insertRoute(routeData)) {
                    Assert.fail("Duplicate insertion should have failed")
                } else {
                    Log.d("gpxTest", "Duplicate insertion failed correctly")
                }

                // Check there's still a route left
                routes = routesRepository.getRoute(routeData.name)
                Assert.assertEquals(1, routes.size)
                Assert.assertEquals(routeData, routes[0])

                // Check that we can't get a non-existent route
                routes = routesRepository.getRoute("non-existent-route")
                Assert.assertEquals(0, routes.size)

                // Try deleting a non-existent route
                routesRepository.deleteRoute("non-existent-route")

                // Update the database route with the waypoints reversed
                routeData.waypoints.clear()
                for(point in expectedValues.reversed()) {
                    routeData.waypoints.add(point)
                }
                routesRepository.updateRoute(routeData)

                // Get the route back from the database and check that the waypoints are reversed
                routes = routesRepository.getRoute(routeData.name)
                Assert.assertEquals(1, routes.size)
                Assert.assertEquals(expectedValues.size, routes[0].waypoints.size)
                index = 0
                for(point in routes[0].waypoints.reversed()) {
                    Assert.assertEquals(point, expectedValues[index])
                    index += 1
                }

                // Delete that route to leave the database empty for the next test
                routesRepository.deleteRoute(routeData.name)

                waypoints = routesRepository.getWaypoints()
                Assert.assertEquals(0, waypoints.size)
            }
        }
    }

    @Test
    fun handcraftedParsing(){

        val waypoint1 = RoutePoint("George Square, Glasgow", Location(55.8610697, -4.2499327))
        val waypoint2 = RoutePoint("Edinburgh Castle", Location(55.9488161, -3.2021476))
        val waypoint3 = RoutePoint("Greenwich Prime Meridian, London", Location(51.4779644, 0.0))
        val waypoint4 = RoutePoint("Rock of Gibraltar", Location(36.1636308, -5.392716))
        val waypoint5 = RoutePoint("Inverness", Location(57.4680357, -4.263057))
        val expectedValues = listOf(waypoint1, waypoint2, waypoint3, waypoint4, waypoint5)

        testParsing("gpx/handcrafted.gpx", expectedValues, "Test", "Test?")
    }

    @Test
    fun rideWithGpsParsing(){

        val waypoint1 = RoutePoint("Slight Left", Location(55.94722, -4.30844))
        val waypoint2 = RoutePoint("Right", Location(55.94628, -4.30901))
        val waypoint3 = RoutePoint("Right", Location(55.9442, -4.31081))
        val waypoint4 = RoutePoint("Left", Location(55.94245, -4.31335))
        val waypoint5 = RoutePoint("Right", Location(55.94181, -4.31338))
        val waypoint6 = RoutePoint("Right", Location(55.94182, -4.31358))
        val waypoint7 = RoutePoint("Right", Location(55.94198, -4.31612))
        val waypoint8 = RoutePoint("Left", Location(55.94229, -4.31637))
        val waypoint9 = RoutePoint("Right", Location(55.94218, -4.31735))

        val expectedValues = listOf(waypoint1, waypoint2, waypoint3, waypoint4, waypoint5, waypoint6, waypoint7, waypoint8, waypoint9)

        testParsing("gpx/rideWithGps.gpx", expectedValues, "Test", "")
    }

    @Test
    fun soundscapeParsing(){

        val waypoint1 = RoutePoint("Waypoint", Location(55.947256, -4.305852))
        val waypoint2 = RoutePoint("Waypoint", Location(55.946412, -4.305621))
        val waypoint3 = RoutePoint("Waypoint", Location(55.946409, -4.304833))
        val waypoint4 = RoutePoint("Waypoint", Location(55.945706, -4.304774))
        val waypoint5 = RoutePoint("Waypoint", Location(55.945727, -4.307563))
        val waypoint6 = RoutePoint("Waypoint", Location(55.946424, -4.307574))
        val waypoint7 = RoutePoint("Waypoint", Location(55.946436, -4.305745))
        val waypoint8 = RoutePoint("Waypoint", Location(55.947286, -4.305696))

        val expectedValues = listOf(waypoint1, waypoint2, waypoint3, waypoint4, waypoint5, waypoint6, waypoint7, waypoint8)

        testParsing("gpx/soundscape.gpx", expectedValues, "Test", "Test?")
    }
}