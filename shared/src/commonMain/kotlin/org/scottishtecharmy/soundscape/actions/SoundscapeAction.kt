package org.scottishtecharmy.soundscape.actions

/**
 * Assistant-level command vocabulary. Deliberately headless: every action here is
 * executable against a [org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService]
 * with no UI composed, which is what lets an iOS App Intent run with
 * `openAppWhenRun = false` and an Android assistant shortcut run without an Activity.
 *
 * Mirrors AudioMenu.buildRootMenu() — if you add an action here, add the matching
 * AudioMenu entry (or vice versa) so the two front-ends onto the same feature set
 * don't drift apart.
 *
 * Not to be confused with [org.scottishtecharmy.soundscape.intents.IncomingIntent],
 * which is the *navigation* vocabulary for inbound URLs and needs SharedNavHost to
 * act on it. Actions that need UI escalate via [ActionResult.NeedsUi].
 */
sealed class SoundscapeAction {

    // ── Callouts — AudioMenu "Callouts" submenu ──────────────────────────────
    data object MyLocation : SoundscapeAction()
    data object AroundMe : SoundscapeAction()
    data object AheadOfMe : SoundscapeAction()
    data object NearbyMarkers : SoundscapeAction()

    // ── Route control — AudioMenu "Route" submenu ────────────────────────────
    /**
     * [reverse] has no default: default parameter values don't cross into Swift,
     * so the App Intents layer would have to pass it anyway.
     */
    data class StartRouteById(val routeId: Long, val reverse: Boolean) : SoundscapeAction()
    data class StartRouteNamed(val name: String) : SoundscapeAction()
    data object StopRoute : SoundscapeAction()
    data object NextWaypoint : SoundscapeAction()
    data object PreviousWaypoint : SoundscapeAction()
    data object ToggleBeaconMute : SoundscapeAction()

    // ── Beacons — AudioMenu "Start Audio Beacon" submenu ─────────────────────
    data class BeaconOnMarkerById(val markerId: Long) : SoundscapeAction()
    data class BeaconOnMarkerNamed(val name: String) : SoundscapeAction()
    data object StopBeacon : SoundscapeAction()

    // ── Enumeration ──────────────────────────────────────────────────────────
    /**
     * Read back what the user has saved. No AudioMenu equivalent: the menu *is* the
     * list, so speaking one would be redundant there, whereas an assistant has no
     * browsable surface of its own.
     */
    data object ListRoutes : SoundscapeAction()
    data object ListMarkers : SoundscapeAction()
}
