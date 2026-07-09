package org.scottishtecharmy.soundscape.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGuidanceTest {
    @Test
    fun announcesInitialInstructionOnce() {
        val guidance = RouteGuidance(testRoute(), testConfig())

        val firstEvent = guidance.update(NavigationPoint(latitude = 55.0, longitude = -4.0))
        val secondEvent = guidance.update(NavigationPoint(latitude = 55.00001, longitude = -4.0))

        assertInstructionEvent(firstEvent, 0, "Continue")
        assertNull(secondEvent)
    }

    @Test
    fun announcesNextInstructionWhenProgressAdvances() {
        val guidance = RouteGuidance(testRoute(), testConfig())
        guidance.update(NavigationPoint(latitude = 55.0, longitude = -4.0))

        val event = guidance.update(NavigationPoint(latitude = 55.0001, longitude = -4.0))

        assertInstructionEvent(event, 1, "Turn right")
    }

    @Test
    fun announcesOffRouteOnlyOnceUntilUserReturnsToRoute() {
        val guidance = RouteGuidance(testRoute(), testConfig())

        val firstOffRoute = guidance.update(NavigationPoint(latitude = 55.0, longitude = -4.001))
        val repeatedOffRoute = guidance.update(NavigationPoint(latitude = 55.00002, longitude = -4.001))
        val backOnRoute = guidance.update(NavigationPoint(latitude = 55.0, longitude = -4.0))
        val secondOffRoute = guidance.update(NavigationPoint(latitude = 55.0, longitude = -4.001))

        assertTrue(firstOffRoute is RouteGuidanceEvent.OffRoute)
        assertNull(repeatedOffRoute)
        assertInstructionEvent(backOnRoute, 0, "Continue")
        assertTrue(secondOffRoute is RouteGuidanceEvent.OffRoute)
    }

    @Test
    fun announcesArrivalOnce() {
        val guidance = RouteGuidance(testRoute(), testConfig())

        val firstArrival = guidance.update(NavigationPoint(latitude = 55.0002, longitude = -4.0))
        val repeatedArrival = guidance.update(NavigationPoint(latitude = 55.0002, longitude = -4.0))

        assertTrue(firstArrival is RouteGuidanceEvent.Arrived)
        assertNull(repeatedArrival)
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
            arrivalThresholdMeters = 4.0
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
}
