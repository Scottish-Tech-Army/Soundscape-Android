package org.scottishtecharmy.soundscape.actions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkActionsTest {

    @Test
    fun successWithItsOwnAudioSaysNothing() {
        assertNull(deepLinkSpeech(SoundscapeAction.AroundMe, ActionResult.Ok()))
        assertNull(
            deepLinkSpeech(
                SoundscapeAction.BeaconOnMarkerNamed("Home"),
                ActionResult.Ok("Beacon started on Home"),
            )
        )
    }

    @Test
    fun stoppingTheBeaconIsConfirmed() {
        assertEquals(
            "Beacon stopped",
            deepLinkSpeech(SoundscapeAction.StopBeacon, ActionResult.Ok("Beacon stopped")),
        )
    }

    @Test
    fun failuresAreSpoken() {
        assertEquals(
            "No route is playing",
            deepLinkSpeech(
                SoundscapeAction.NextWaypoint,
                ActionResult.NotReady(ActionResult.Reason.NO_ROUTE_ACTIVE, "No route is playing"),
            ),
        )
        assertEquals(
            "No marker called Hmoe",
            deepLinkSpeech(
                SoundscapeAction.BeaconOnMarkerNamed("Hmoe"),
                ActionResult.NotFound("Hmoe", "No marker called Hmoe"),
            ),
        )
    }
}
