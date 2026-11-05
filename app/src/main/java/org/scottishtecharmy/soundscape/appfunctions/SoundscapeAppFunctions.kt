package org.scottishtecharmy.soundscape.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import org.scottishtecharmy.soundscape.actions.ActionResult
import org.scottishtecharmy.soundscape.actions.SoundscapeAction
import org.scottishtecharmy.soundscape.actions.SoundscapeActionExecutor
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabaseProvider
import org.scottishtecharmy.soundscape.geoengine.callouts.CalloutVerbosity
import org.scottishtecharmy.soundscape.services.SoundscapeService

/**
 * What an assistant gets back from a Soundscape function.
 *
 * Soundscape answers through its own spatial audio, so for most functions the useful
 * result is only whether it worked: the callout itself carries direction information no
 * text could. [message] is filled where the app makes no sound of its own — the lists,
 * the confirmations, and every failure.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SoundscapeResult(
    /** Whether the action was carried out. */
    val success: Boolean,
    /**
     * What to say to the user, in their language. Empty when Soundscape has already
     * answered aloud itself, in which case say nothing more.
     */
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
 * becomes the natural-language description the agent reads to decide when to call it,
 * and the agent sees each one on its own. So each says for itself whether Soundscape
 * must be open and whether the answer is spoken by the app, even where that repeats.
 * The app-wide context (who the app is for, how it answers) is in
 * res/xml/app_functions_app_metadata.xml.
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
     * Answers "where am I?": Soundscape says the street the user is on or the junction
     * they are at, and the nearest landmarks. For points of interest all around, use
     * aroundMe; for what is in front, aheadOfMe.
     *
     * Soundscape speaks the answer itself in spatial audio, so on success the message is
     * empty: say nothing more, and do not describe the location yourself. Soundscape must
     * be open; if it is not, or has no GPS fix yet, the message explains and should be
     * read to the user.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun myLocation(): SoundscapeResult = run(SoundscapeAction.MyLocation)

    /**
     * Answers "what's around me?": Soundscape calls out points of interest on every side
     * of the user, each positioned in spatial audio so it sounds from its real direction.
     * For the street the user is on, use myLocation; for only what is in front, aheadOfMe.
     *
     * Soundscape speaks the answer itself, so on success the message is empty: say nothing
     * more, and do not list places yourself. Soundscape must be open; if it is not, or has
     * no GPS fix yet, the message explains and should be read to the user.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun aroundMe(): SoundscapeResult = run(SoundscapeAction.AroundMe)

    /**
     * Answers "what's ahead of me?": Soundscape calls out the roads, junctions and places
     * lying ahead in the direction the user is facing or walking.
     *
     * Soundscape speaks the answer itself, so on success the message is empty: say nothing
     * more. Soundscape must be open; if it is not, or has no GPS fix yet, the message
     * explains and should be read to the user.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun aheadOfMe(): SoundscapeResult = run(SoundscapeAction.AheadOfMe)

    /**
     * Calls out the user's own saved markers (places they saved in Soundscape) that are
     * close by, each from its direction. To hear all saved markers by name wherever they
     * are, use listMarkers.
     *
     * Soundscape speaks the answer itself, so on success the message is empty: say nothing
     * more. Soundscape must be open; if it is not, the message explains and should be
     * read to the user.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun nearbyMarkers(): SoundscapeResult = run(SoundscapeAction.NearbyMarkers)

    /**
     * A quick "how far is it?" for the audio beacon: Soundscape says the distance, heard
     * from the beacon's direction. For the beacon's name and address as well, use
     * beaconInfo.
     *
     * Soundscape speaks the answer itself, so on success the message is empty: say nothing
     * more. Needs Soundscape open and a beacon or route playing; otherwise the message
     * explains and should be read to the user.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun calloutBeacon(): SoundscapeResult = run(SoundscapeAction.CalloutBeacon)

    /**
     * Answers "what is the beacon on?" or "where is my beacon?": Soundscape says the
     * beacon's name, distance, direction and street address. For just the distance, use
     * calloutBeacon.
     *
     * Soundscape speaks the answer itself, so on success the message is empty: say nothing
     * more. Needs Soundscape open and a beacon or route playing; otherwise the message
     * explains and should be read to the user.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun beaconInfo(): SoundscapeResult = run(SoundscapeAction.BeaconInfo)

    /**
     * Starts playing one of the user's saved routes: an audio beacon guides them to each
     * waypoint in turn. Only routes the user has already saved in Soundscape can be
     * started; it cannot plan a route to a new place. If unsure of the name, call
     * listRoutes first.
     *
     * Read the message to the user: it confirms which route started, or says no route
     * matched. Soundscape must be open.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun startRoute(
        /** The name of the saved route to start. Matched loosely, so a partial name works. */
        name: String,
    ): SoundscapeResult = run(SoundscapeAction.StartRouteNamed(name))

    /**
     * Stops the route that is playing, and its beacon. Read the message to the user: it
     * confirms the route stopped. Soundscape must be open.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun stopRoute(): SoundscapeResult = run(SoundscapeAction.StopRoute)

    /**
     * Skips forward to the next waypoint on the route that is playing, moving the beacon
     * there.
     *
     * Soundscape announces the new waypoint itself, so on success the message is empty:
     * say nothing more. Needs Soundscape open and a route playing; if there is none, or
     * the route is already at its last waypoint, the message says so and should be read
     * to the user.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun nextWaypoint(): SoundscapeResult = run(SoundscapeAction.NextWaypoint)

    /**
     * Goes back to the previous waypoint on the route that is playing, moving the beacon
     * there.
     *
     * Soundscape announces the new waypoint itself, so on success the message is empty:
     * say nothing more. Needs Soundscape open and a route playing; if there is none, or
     * the route is already at its first waypoint, the message says so and should be read
     * to the user.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun previousWaypoint(): SoundscapeResult = run(SoundscapeAction.PreviousWaypoint)

    /**
     * Silences or restores the audio beacon's repeating tone, without stopping the beacon
     * or route: it keeps its place and can be unmuted later. Asking for the state it is
     * already in changes nothing. To remove the beacon altogether, use stopBeacon or
     * stopRoute.
     *
     * On success the message is empty, as the change is heard directly: say nothing more.
     * Needs Soundscape open and a beacon or route playing; otherwise the message explains
     * and should be read to the user.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun setBeaconMuted(
        /** True to mute the beacon, false to unmute it. */
        muted: Boolean,
    ): SoundscapeResult = run(SoundscapeAction.SetBeaconMuted(muted))

    /**
     * Sets an audio beacon on one of the user's saved markers: a repeating tone that
     * sounds from the marker's direction, so the user can walk towards it, until it is
     * stopped. Only places the user has already saved as markers in Soundscape can be
     * used; it cannot search for a shop, address or other new place. If unsure of the
     * name, call listMarkers first.
     *
     * Read the message to the user: it confirms which marker the beacon is on, or says no
     * marker matched. Soundscape must be open.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun startBeacon(
        /** The name of the saved marker to place the beacon on. Matched loosely. */
        name: String,
    ): SoundscapeResult = run(SoundscapeAction.BeaconOnMarkerNamed(name))

    /**
     * Switches the audio beacon off. To only silence it for a while, use setBeaconMuted.
     * Read the message to the user: it confirms the beacon stopped. Soundscape must be
     * open.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun stopBeacon(): SoundscapeResult = run(SoundscapeAction.StopBeacon)

    /**
     * Sets how much Soundscape says automatically as the user walks: Silent turns automatic
     * callouts off altogether, leaving beacons, routes and on-demand callouts; Essential calls out only
     * streets, junctions and landmarks; Simplified skips minor paths and repeats itself less;
     * Detailed calls out everything nearby. Useful when a busy street is too noisy, or a quiet
     * one too sparse. The change is saved, the same as choosing it in Settings.
     *
     * Read the message to the user: it confirms the new level. Soundscape must be open.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun setCalloutDetail(
        /** How much to call out automatically. */
        @AppFunctionStringValueConstraint(
            enumValues = ["Silent", "Essential", "Simplified", "Detailed"],
        )
        level: String,
    ): SoundscapeResult = run(
        CALLOUT_DETAIL_LEVELS[level.trim().lowercase()]
            ?.let { SoundscapeAction.SetCalloutDetail(it) }
            ?: SoundscapeAction.SetCalloutDetailNamed(level),
    )

    /**
     * Lists the names of the routes the user has saved in Soundscape, for choosing one to
     * pass to startRoute. Works whether or not Soundscape is open, because it only reads
     * saved data. Read the message to the user, or use it to pick the route they meant.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun listRoutes(): SoundscapeResult = run(SoundscapeAction.ListRoutes)

    /**
     * Lists the names of the places the user has saved as markers in Soundscape, for
     * choosing one to pass to startBeacon. Works whether or not Soundscape is open,
     * because it only reads saved data. Read the message to the user, or use it to pick
     * the marker they meant.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
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
     * and the startup block that builds it, so that is treated the same way - as is a
     * service snoozing for wake on leave, whose geo engine isn't running.
     */
    private fun executor(): SoundscapeActionExecutor =
        SoundscapeService.runningInstance?.takeUnless { it.snoozing }?.actions
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

        /**
         * The schema's fixed English words for setCalloutDetail, mapped here rather than
         * matched by name: two are English display names ("Essential" is stored as
         * Quiet, "Simplified" as Balanced), and the executor matches display names only
         * in the app's own language, so on a French phone they would match nothing.
         * Anything else still goes through SetCalloutDetailNamed.
         */
        val CALLOUT_DETAIL_LEVELS = mapOf(
            "silent" to CalloutVerbosity.SILENT,
            "essential" to CalloutVerbosity.QUIET,
            "simplified" to CalloutVerbosity.BALANCED,
            "detailed" to CalloutVerbosity.DETAILED,
        )
    }
}
