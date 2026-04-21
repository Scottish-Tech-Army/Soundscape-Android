package org.scottishtecharmy.soundscape.services

import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers

data class RoutePlayerState(
    val routeData: RouteWithMarkers? = null,
    val currentWaypoint: Int = 0,
    val beaconOnly: Boolean = false,
)
