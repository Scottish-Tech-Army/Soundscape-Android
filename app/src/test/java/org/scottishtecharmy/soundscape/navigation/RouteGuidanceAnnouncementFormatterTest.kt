package org.scottishtecharmy.soundscape.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteGuidanceAnnouncementFormatterTest {
    @Test
    fun formatsInstructionTextDirectlyFromRouteEngine() {
        val text = RouteGuidanceAnnouncementFormatter.format(
            RouteGuidanceEvent.Instruction(
                instructionIndex = 1,
                instruction = testInstruction("Turn right onto Main Street"),
                update = testUpdate(RouteProgressStatus.ON_ROUTE)
            )
        )

        assertEquals("Turn right onto Main Street", text)
    }

    @Test
    fun formatsOffRouteWarning() {
        val text = RouteGuidanceAnnouncementFormatter.format(
            RouteGuidanceEvent.OffRoute(testUpdate(RouteProgressStatus.OFF_ROUTE))
        )

        assertEquals("You are off route.", text)
    }

    @Test
    fun formatsArrival() {
        val text = RouteGuidanceAnnouncementFormatter.format(
            RouteGuidanceEvent.Arrived(testUpdate(RouteProgressStatus.ARRIVED))
        )

        assertEquals("Arrived.", text)
    }

    private fun testInstruction(text: String): NavigationInstruction {
        return NavigationInstruction(
            text = text,
            streetName = "Main Street",
            distanceMeters = 20.0,
            durationMillis = 10000,
            sign = 2,
            geometryStartIndex = 0,
            geometryEndIndex = 1
        )
    }

    private fun testUpdate(status: RouteProgressStatus): RouteProgressUpdate {
        return RouteProgressUpdate(
            status = status,
            isOffRoute = status == RouteProgressStatus.OFF_ROUTE,
            isArrived = status == RouteProgressStatus.ARRIVED,
            nextInstructionIndex = null,
            nextInstruction = null,
            distanceFromRouteMeters = 0.0,
            matchedGeometryIndex = 0
        )
    }
}
