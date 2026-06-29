package org.scottishtecharmy.soundscape.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnByTurnNavigationSessionTest {
    @Test
    fun returnsNoEventBeforeRouteStarts() {
        val session = TurnByTurnNavigationSession(FakeRouteProvider(testRoute()))

        val event = session.updateLocation(NavigationPoint(latitude = 55.0, longitude = -4.0))

        assertNull(event)
    }

    @Test
    fun startsRouteAndEmitsGuidanceEventsFromLocationUpdates() {
        val provider = FakeRouteProvider(testRoute())
        val session = TurnByTurnNavigationSession(provider, testConfig())
        val request = GraphHopperRouteRequest(
            start = NavigationPoint(latitude = 55.0, longitude = -4.0),
            destination = NavigationPoint(latitude = 55.0002, longitude = -4.0)
        )

        val route = session.start(request)
        val firstEvent = session.updateLocation(NavigationPoint(latitude = 55.0, longitude = -4.0))
        val nextEvent = session.updateLocation(NavigationPoint(latitude = 55.0001, longitude = -4.0))

        assertEquals(request, provider.lastRequest)
        assertEquals(testRoute(), route)
        assertInstructionEvent(firstEvent, 0, "Continue")
        assertInstructionEvent(nextEvent, 1, "Turn right")
    }

    @Test
    fun stopClearsActiveRoute() {
        val session = TurnByTurnNavigationSession(FakeRouteProvider(testRoute()), testConfig())
        session.start(
            GraphHopperRouteRequest(
                start = NavigationPoint(latitude = 55.0, longitude = -4.0),
                destination = NavigationPoint(latitude = 55.0002, longitude = -4.0)
            )
        )

        session.stop()
        val event = session.updateLocation(NavigationPoint(latitude = 55.0, longitude = -4.0))

        assertNull(event)
    }

    @Test
    fun reroutesFromCurrentLocationAfterOffRoutePersistsForFiveSeconds() {
        val firstRoute = testRoute()
        val reroutedRoute = reroutedRoute()
        val provider = FakeRouteProvider(firstRoute, reroutedRoute)
        val clock = FakeClock()
        val session = TurnByTurnNavigationSession(provider, testConfig(), clock::now)
        val request = GraphHopperRouteRequest(
            start = NavigationPoint(latitude = 55.0, longitude = -4.0),
            destination = NavigationPoint(latitude = 55.0002, longitude = -4.0)
        )
        val offRouteLocation = NavigationPoint(latitude = 55.001, longitude = -4.002)

        session.start(request)
        val firstOffRouteUpdate = session.updateLocation(offRouteLocation)
        clock.advanceBy(4_999)
        val beforeDebounce = session.updateLocation(offRouteLocation)
        clock.advanceBy(1)
        val afterDebounce = session.updateLocation(offRouteLocation)

        assertNull(firstOffRouteUpdate)
        assertNull(beforeDebounce)
        assertInstructionEvent(afterDebounce, 0, "Continue on rerouted path")
        assertEquals(2, provider.requests.size)
        assertEquals(offRouteLocation, provider.requests.last().start)
        assertEquals(request.destination, provider.requests.last().destination)
    }

    private class FakeRouteProvider(
        private vararg val routes: NavigationRoute
    ) : NavigationRouteProvider {
        var lastRequest: GraphHopperRouteRequest? = null
        val requests = mutableListOf<GraphHopperRouteRequest>()

        override fun route(request: GraphHopperRouteRequest): NavigationRoute {
            lastRequest = request
            requests.add(request)
            return routes[(requests.size - 1).coerceAtMost(routes.lastIndex)]
        }
    }

    private class FakeClock {
        private var timeMillis = 0L

        fun now(): Long = timeMillis

        fun advanceBy(millis: Long) {
            timeMillis += millis
        }
    }

    private fun assertInstructionEvent(
        event: RouteGuidanceEvent?,
        expectedIndex: Int,
        expectedText: String
    ) {
        assertTrue(event is RouteGuidanceEvent.Instruction)
        val instructionEvent = event as RouteGuidanceEvent.Instruction
        assertEquals(expectedIndex, instructionEvent.instructionIndex)
        assertEquals(expectedText, instructionEvent.instruction.text)
    }

    private fun testConfig(): RouteFollowerConfig {
        return RouteFollowerConfig(
            offRouteThresholdMeters = 25.0,
            arrivalThresholdMeters = 4.0,
            rerouteDebounceMillis = 5_000
        )
    }

    private fun testRoute(): NavigationRoute {
        return NavigationRoute(
            distanceMeters = 22.0,
            durationMillis = 120000,
            geometry = listOf(
                NavigationPoint(latitude = 55.0, longitude = -4.0),
                NavigationPoint(latitude = 55.0001, longitude = -4.0),
                NavigationPoint(latitude = 55.0002, longitude = -4.0)
            ),
            instructions = listOf(
                NavigationInstruction(
                    text = "Continue",
                    streetName = "",
                    distanceMeters = 11.0,
                    durationMillis = 60000,
                    sign = 0,
                    geometryStartIndex = 0,
                    geometryEndIndex = 1
                ),
                NavigationInstruction(
                    text = "Turn right",
                    streetName = "Main Street",
                    distanceMeters = 11.0,
                    durationMillis = 60000,
                    sign = 2,
                    geometryStartIndex = 1,
                    geometryEndIndex = 2
                )
            )
        )
    }

    private fun reroutedRoute(): NavigationRoute {
        return NavigationRoute(
            distanceMeters = 22.0,
            durationMillis = 120000,
            geometry = listOf(
                NavigationPoint(latitude = 55.001, longitude = -4.002),
                NavigationPoint(latitude = 55.0011, longitude = -4.002),
                NavigationPoint(latitude = 55.0002, longitude = -4.0)
            ),
            instructions = listOf(
                NavigationInstruction(
                    text = "Continue on rerouted path",
                    streetName = "",
                    distanceMeters = 11.0,
                    durationMillis = 60000,
                    sign = 0,
                    geometryStartIndex = 0,
                    geometryEndIndex = 1
                ),
                NavigationInstruction(
                    text = "Turn right after reroute",
                    streetName = "Main Street",
                    distanceMeters = 11.0,
                    durationMillis = 60000,
                    sign = 2,
                    geometryStartIndex = 1,
                    geometryEndIndex = 2
                )
            )
        )
    }
}
