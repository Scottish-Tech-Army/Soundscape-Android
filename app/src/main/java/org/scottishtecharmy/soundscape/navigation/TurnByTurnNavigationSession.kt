package org.scottishtecharmy.soundscape.navigation

class TurnByTurnNavigationSession(
    private val routeProvider: NavigationRouteProvider,
    private val config: RouteFollowerConfig = RouteFollowerConfig(),
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() }
) {
    private var guidance: RouteGuidance? = null
    private var activeRequest: GraphHopperRouteRequest? = null
    private var offRouteSinceMillis: Long? = null

    fun start(request: GraphHopperRouteRequest): NavigationRoute {
        val route = routeProvider.route(request)
        activeRequest = request
        offRouteSinceMillis = null
        guidance = RouteGuidance(route, config)
        return route
    }

    fun updateLocation(location: NavigationPoint): RouteGuidanceEvent? {
        val activeGuidance = guidance ?: return null
        val event = activeGuidance.update(location)

        if (event !is RouteGuidanceEvent.OffRoute && !activeGuidance.isOffRoute()) {
            offRouteSinceMillis = null
            return event
        }

        val offRouteSince = offRouteSinceMillis ?: currentTimeMillis().also {
            offRouteSinceMillis = it
        }
        if (currentTimeMillis() - offRouteSince < config.rerouteDebounceMillis) {
            return null
        }

        val previousRequest = activeRequest ?: return null
        val rerouteRequest = previousRequest.copy(start = location)
        start(rerouteRequest)
        offRouteSinceMillis = null
        return guidance?.update(location)
    }

    fun stop() {
        guidance = null
        activeRequest = null
        offRouteSinceMillis = null
    }
}
