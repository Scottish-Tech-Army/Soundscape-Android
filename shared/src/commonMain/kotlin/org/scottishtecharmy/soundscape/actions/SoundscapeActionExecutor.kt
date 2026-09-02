package org.scottishtecharmy.soundscape.actions

import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import org.scottishtecharmy.soundscape.intents.bestMarkerMatch
import org.scottishtecharmy.soundscape.intents.bestRouteMatch
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService

/**
 * Runs a [SoundscapeAction] against the platform's service and reports what
 * happened. The single place both platforms' assistant integrations go through,
 * so the iOS App Intents and any future Android equivalent can't drift into two
 * different command sets.
 *
 * Deliberately never speaks. Confirmations come back as [ActionResult] text for
 * the assistant to voice; the app's own callouts still come out of the geo engine
 * as usual. An executor that spoke for itself would talk over the assistant while
 * its audio session is still up.
 */
class SoundscapeActionExecutor(
    private val service: MediaControllableService,
    private val routeDao: RouteDao,
    private val strings: LocalizedStrings = ComposeLocalizedStrings(),
) {

    suspend fun execute(action: SoundscapeAction): ActionResult = when (action) {

        SoundscapeAction.MyLocation -> callout { service.myLocation() }
        SoundscapeAction.AroundMe -> callout { service.whatsAroundMe() }
        SoundscapeAction.AheadOfMe -> callout { service.aheadOfMe() }
        SoundscapeAction.NearbyMarkers -> callout { service.nearbyMarkers() }

        is SoundscapeAction.StartRouteById ->
            routeDao.getRouteById(action.routeId)
                ?.let { startRoute(it, action.reverse) }
                ?: itemNotFound()

        is SoundscapeAction.StartRouteNamed -> {
            val routes = routeDao.getAllRoutes()
            if (routes.isEmpty()) {
                notReady(ActionResult.Reason.NO_ROUTES_SAVED, StringKey.MenuNoRoutes)
            } else {
                routes.bestRouteMatch(action.name)
                    ?.let { startRoute(it, reverse = false) }
                    ?: notFound(action.name, StringKey.ActionNoSuchRoute)
            }
        }

        SoundscapeAction.StopRoute -> {
            service.routeStop()
            ActionResult.Ok(strings.get(StringKey.ActionRouteStopped))
        }

        // routeSkipNext/Previous/Mute already return false when no route is
        // playing — OriginalMediaControls leans on the same signal to decide
        // whether a media key means "next waypoint" or "my location".
        SoundscapeAction.NextWaypoint ->
            if (service.routeSkipNext()) ActionResult.Ok() else noRouteActive()

        SoundscapeAction.PreviousWaypoint ->
            if (service.routeSkipPrevious()) ActionResult.Ok() else noRouteActive()

        SoundscapeAction.ToggleBeaconMute ->
            if (service.routeMute()) ActionResult.Ok() else noRouteActive()

        is SoundscapeAction.BeaconOnMarkerById ->
            routeDao.getMarkerById(action.markerId)
                ?.let { beaconOn(it) }
                ?: itemNotFound()

        is SoundscapeAction.BeaconOnMarkerNamed -> {
            val markers = routeDao.getAllMarkers()
            if (markers.isEmpty()) {
                notReady(ActionResult.Reason.NO_MARKERS_SAVED, StringKey.ActionNoMarkersSaved)
            } else {
                markers.bestMarkerMatch(action.name)
                    ?.let { beaconOn(it) }
                    ?: notFound(action.name, StringKey.ActionNoSuchMarker)
            }
        }

        SoundscapeAction.StopBeacon -> {
            service.destroyBeacon()
            ActionResult.Ok(strings.get(StringKey.ActionBeaconStopped))
        }
    }

    /**
     * Issues a callout, first checking the two things it silently needs: a position
     * fix and loaded map tiles. Without either the geo engine produces nothing, and
     * an assistant reporting that as success leaves the user waiting for audio that
     * is never coming.
     */
    private fun callout(issue: () -> Unit): ActionResult {
        service.locationFlow.value
            ?: return notReady(ActionResult.Reason.NO_LOCATION_FIX, StringKey.ActionNoLocation)
        service.gridStateFlow.value
            ?: return notReady(ActionResult.Reason.NO_MAP_DATA, StringKey.ActionNoMapData)

        // CalloutController treats a second request for an in-flight callout as a
        // cancel. That's right for a button — press again to shut it up — but wrong
        // here, where asking twice means "say it again". Clear first so the issue
        // below always starts a fresh callout.
        service.cancelCallout()
        issue()
        return ActionResult.Ok()
    }

    private fun startRoute(route: RouteEntity, reverse: Boolean): ActionResult {
        if (reverse) {
            service.routeStartReverse(route.routeId)
        } else {
            service.routeStartById(route.routeId)
        }
        return ActionResult.Ok(strings.get(StringKey.ActionRouteStarted, route.name))
    }

    private fun beaconOn(marker: MarkerEntity): ActionResult {
        service.startBeacon(marker.getLngLatAlt(), marker.name)
        return ActionResult.Ok(strings.get(StringKey.ActionBeaconStarted, marker.name))
    }

    private fun noRouteActive() =
        notReady(ActionResult.Reason.NO_ROUTE_ACTIVE, StringKey.ActionNoRouteActive)

    private fun notReady(reason: ActionResult.Reason, key: StringKey) =
        ActionResult.NotReady(reason, strings.get(key))

    private fun notFound(query: String, key: StringKey) =
        ActionResult.NotFound(query, strings.get(key, query))

    /** A saved id that no longer resolves — a stale assistant shortcut or entity. */
    private fun itemNotFound() =
        ActionResult.NotFound("", strings.get(StringKey.ActionItemNotFound))
}
