package org.scottishtecharmy.soundscape.viewmodels.home

import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState

data class HomeState(
    var heading: Float = 0.0f,
    var location: LngLatAlt? = null,
    var beaconState: BeaconState? = null,
    var streetPreviewState: StreetPreviewState = StreetPreviewState(StreetPreviewEnabled.OFF),
    var isSearching: Boolean = false,
    var searchInProgress: Boolean = false,
    var searchItems: List<LocationDescription>? = null,
    var routesTabSelected: Boolean = true,
    var currentRouteData: RoutePlayerState = RoutePlayerState(),
    var permissionsRequired: Boolean = false,
)
