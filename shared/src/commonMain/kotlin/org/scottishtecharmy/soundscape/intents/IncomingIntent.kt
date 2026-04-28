package org.scottishtecharmy.soundscape.intents

import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

sealed class IncomingIntent {
    data class OpenLocation(val locationDescription: LocationDescription) : IncomingIntent()

    /**
     * Coordinates without a friendly name. Platforms may upgrade this to OpenLocation
     * via a native geocoder before publishing, or fall through and let the navigation
     * layer use the lat/lon string as the display name.
     */
    data class OpenLatLon(
        val latitude: Double,
        val longitude: Double,
        val displayName: String? = null,
    ) : IncomingIntent()

    data class StartRoute(val routeId: Long) : IncomingIntent()

    data object StopRoute : IncomingIntent()

    /** tab is "routes" or "markers". */
    data class OpenFeature(val tab: String) : IncomingIntent()

    data class ImportRoute(val route: RouteWithMarkers) : IncomingIntent()

    data class StartRouteByName(val name: String) : IncomingIntent()
}
