package org.scottishtecharmy.soundscape.screens.home

import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState

data class HomeState(
    val heading: Float = 0f,
    val location: LngLatAlt? = null,
    val beaconState: BeaconState? = null,
    val streetPreviewState: StreetPreviewState = StreetPreviewState(),
    val currentRouteData: RoutePlayerState = RoutePlayerState(),
    val isSearching: Boolean = false,
    val searchInProgress: Boolean = false,
    val searchItems: List<LocationDescription>? = null,
    val routesTabSelected: Boolean = true,
    val permissionsRequired: Boolean = false,
    val voiceCommandListening: Boolean = false,
)
