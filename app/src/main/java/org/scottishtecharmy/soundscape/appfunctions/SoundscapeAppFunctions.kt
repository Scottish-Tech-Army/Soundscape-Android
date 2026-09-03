package org.scottishtecharmy.soundscape.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import org.scottishtecharmy.soundscape.actions.ActionResult
import org.scottishtecharmy.soundscape.actions.SoundscapeAction
import org.scottishtecharmy.soundscape.actions.SoundscapeActionExecutor
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabaseProvider
import org.scottishtecharmy.soundscape.services.SoundscapeService

/**
 * What an assistant gets back from a Soundscape function.
 *
 * Soundscape answers through its own spatial audio, so for most functions the useful
 * result is only whether it worked: the callout itself carries direction information no
 * text could. [message] is filled where the app makes no sound of its own — the lists,
 * and every failure.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SoundscapeResult(
    /** Whether the action was carried out. */
    val success: Boolean,
    /** What to tell the user, or empty when the app has already answered aloud. */
    val message: String,
)

/**
 * Exposes Soundscape's actions to Gemini and other agents.
 *
 * Every function is a thin wrapper over [SoundscapeActionExecutor], the same shared
 * vocabulary the iOS App Intents run on, so the two platforms cannot drift into
 * describing different capabilities.
 *
 * The KDoc on each function below is not decoration: with `isDescribedByKDoc = true` it
 * becomes the natural-language description the agent reads to decide when to call it.
 *
 * Requires API 36, well above the app's minSdk of 30. Nothing else references this class,
 * so on older devices the service simply never starts.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "SoundscapeAppFunctionService",
    appFunctionXmlFileName = "soundscape_app_function_service",
)
abstract class BaseSoundscapeAppFunctionService : AppFunctionService() {

    /**
     * Describes where the user is now — the street, and what is nearby.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun myLocation(): SoundscapeResult = run(SoundscapeAction.MyLocation)

    /**
     * Calls out the points of interest around the user, positioned in spatial audio.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun aroundMe(): SoundscapeResult = run(SoundscapeAction.AroundMe)

    /**
     * Calls out what lies ahead of the user in the direction they are facing.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun aheadOfMe(): SoundscapeResult = run(SoundscapeAction.AheadOfMe)

    /**
     * Calls out the user's saved markers that are nearby.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun nearbyMarkers(): SoundscapeResult = run(SoundscapeAction.NearbyMarkers)

    /**
     * Starts one of the user's saved routes, matched by name.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun startRoute(
        /** The name of the saved route to start. Matched loosely, so a partial name works. */
        name: String,
    ): SoundscapeResult = run(SoundscapeAction.StartRouteNamed(name))

    /**
     * Stops the route that is currently playing.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun stopRoute(): SoundscapeResult = run(SoundscapeAction.StopRoute)

    /**
     * Skips forward to the next waypoint on the route being played.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun nextWaypoint(): SoundscapeResult = run(SoundscapeAction.NextWaypoint)

    /**
     * Goes back to the previous waypoint on the route being played.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun previousWaypoint(): SoundscapeResult = run(SoundscapeAction.PreviousWaypoint)

    /**
     * Mutes the audio beacon, or unmutes it if it is already muted.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun toggleBeaconMute(): SoundscapeResult = run(SoundscapeAction.ToggleBeaconMute)

    /**
     * Sets an audio beacon on one of the user's saved markers, matched by name. The beacon
     * plays a repeating tone from the direction of the marker until it is stopped.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun startBeacon(
        /** The name of the saved marker to place the beacon on. Matched loosely. */
        name: String,
    ): SoundscapeResult = run(SoundscapeAction.BeaconOnMarkerNamed(name))

    /**
     * Switches the audio beacon off.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun stopBeacon(): SoundscapeResult = run(SoundscapeAction.StopBeacon)

    /**
     * Lists the names of the user's saved routes. Works whether or not Soundscape is
     * running, because it only reads saved data.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listRoutes(): SoundscapeResult = run(SoundscapeAction.ListRoutes)

    /**
     * Lists the names of the user's saved markers. Works whether or not Soundscape is
     * running, because it only reads saved data.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listMarkers(): SoundscapeResult = run(SoundscapeAction.ListMarkers)

    // ── Plumbing ─────────────────────────────────────────────────────────────

    /**
     * The running service's executor, or one backed by the database alone.
     *
     * Never starts the service. A location-type foreground service cannot be launched
     * from the background, and an AppFunction is invoked with the app not necessarily
     * running — so anything needing audio reports SERVICE_NOT_RUNNING and says so, while
     * the list functions answer from the database regardless.
     *
     * `actions` can be null even with a live service, during the window between onCreate
     * and the startup block that builds it, so that is treated the same way.
     */
    private fun executor(): SoundscapeActionExecutor =
        SoundscapeService.runningInstance?.actions
            ?: SoundscapeActionExecutor(
                service = null,
                routeDao = MarkersAndRoutesDatabaseProvider.getInstance(applicationContext)
                    .routeDao(),
            )

    /**
     * [READY_TIMEOUT_MS] mirrors the iOS side: a callout needs a position fix and loaded
     * tiles, and when the app has only just come up both land a moment later.
     */
    private suspend fun run(action: SoundscapeAction): SoundscapeResult =
        when (val result = executor().execute(action, READY_TIMEOUT_MS)) {
            is ActionResult.Ok ->
                SoundscapeResult(success = true, message = result.speech.orEmpty())
            is ActionResult.NotReady ->
                SoundscapeResult(success = false, message = result.speech)
            is ActionResult.NotFound ->
                SoundscapeResult(success = false, message = result.speech)
            is ActionResult.NeedsUi ->
                SoundscapeResult(success = false, message = result.speech.orEmpty())
        }

    private companion object {
        const val READY_TIMEOUT_MS = 5_000L
    }
}
