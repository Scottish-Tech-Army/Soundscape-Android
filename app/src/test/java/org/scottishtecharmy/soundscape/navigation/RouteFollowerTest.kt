package org.scottishtecharmy.soundscape.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteFollowerTest {
    @Test
    fun reportsCurrentInstructionWhenUserIsNearStartOfRoute() {
        val follower = RouteFollower(
            testRoute(),
            RouteFollowerConfig(arrivalThresholdMeters = 4.0)
        )

        val update = follower.update(NavigationPoint(latitude = 55.0, longitude = -4.0))

        assertEquals(RouteProgressStatus.ON_ROUTE, update.status)
        assertFalse(update.isOffRoute)
        assertFalse(update.isArrived)
        assertEquals(0, update.nextInstructionIndex)
        assertEquals("Continue", update.nextInstruction?.text)
    }

    @Test
    fun advancesToNextInstructionWhenUserReachesInstructionEnd() {
        val follower = RouteFollower(
            testRoute(),
            RouteFollowerConfig(arrivalThresholdMeters = 4.0)
        )
        follower.update(NavigationPoint(latitude = 55.0, longitude = -4.0))

        val update = follower.update(NavigationPoint(latitude = 55.0001, longitude = -4.0))

        assertEquals(RouteProgressStatus.ON_ROUTE, update.status)
        assertEquals(1, update.nextInstructionIndex)
        assertEquals("Turn right", update.nextInstruction?.text)
    }

    @Test
    fun reportsOffRouteWhenUserIsTooFarFromRouteGeometry() {
        val follower = RouteFollower(
            testRoute(),
            RouteFollowerConfig(offRouteThresholdMeters = 25.0)
        )

        val update = follower.update(NavigationPoint(latitude = 55.0, longitude = -4.001))

        assertEquals(RouteProgressStatus.OFF_ROUTE, update.status)
        assertTrue(update.isOffRoute)
        assertFalse(update.isArrived)
        assertEquals(0, update.nextInstructionIndex)
    }

    @Test
    fun reportsArrivalWhenUserReachesFinalRoutePoint() {
        val follower = RouteFollower(
            testRoute(),
            RouteFollowerConfig(arrivalThresholdMeters = 8.0)
        )

        val update = follower.update(NavigationPoint(latitude = 55.0002, longitude = -4.0))

        assertEquals(RouteProgressStatus.ARRIVED, update.status)
        assertFalse(update.isOffRoute)
        assertTrue(update.isArrived)
        assertNull(update.nextInstruction)
        assertNull(update.nextInstructionIndex)
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
