package org.scottishtecharmy.soundscape

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class SoundscapeServiceConnectionTest {
    @Test
    fun startTurnByTurnNavigation_queuesRequestUntilServiceCanStartIt() {
        val connection = SoundscapeServiceConnection()
        val destination = LngLatAlt(7.4213, 43.7339)

        connection.startTurnByTurnNavigation(destination, "Monaco destination")

        var startedLocation: LngLatAlt? = null
        var startedName: String? = null
        val drained = connection.drainPendingTurnByTurnNavigation { location, name ->
            startedLocation = location
            startedName = name
        }

        assertTrue(drained)
        assertEquals(destination, startedLocation)
        assertEquals("Monaco destination", startedName)
        assertFalse(connection.drainPendingTurnByTurnNavigation { _, _ -> })
    }

    @Test
    fun startTurnByTurnNavigation_keepsLatestRequestWhenServiceIsNotReady() {
        val connection = SoundscapeServiceConnection()
        val firstDestination = LngLatAlt(7.4213, 43.7339)
        val latestDestination = LngLatAlt(7.4200, 43.7320)

        connection.startTurnByTurnNavigation(firstDestination, "First destination")
        connection.startTurnByTurnNavigation(latestDestination, "Latest destination")

        var startedLocation: LngLatAlt? = null
        var startedName: String? = null
        val drained = connection.drainPendingTurnByTurnNavigation { location, name ->
            startedLocation = location
            startedName = name
        }

        assertTrue(drained)
        assertEquals(latestDestination, startedLocation)
        assertEquals("Latest destination", startedName)
        assertFalse(connection.drainPendingTurnByTurnNavigation { _, _ -> })
    }
}
