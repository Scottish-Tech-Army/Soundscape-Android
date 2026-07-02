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
    var voiceCommandListening: Boolean = false,
    // I've disabled auto tutorial startup until I've checked how it behaves with users who have
    // been using the app a long time and also the new release dialog.
    val initialTutorialState: InitialTutorialState = InitialTutorialState.DISMISSED,
)

fun HomeState.shouldShowInitialTutorialDialog() =
    initialTutorialState == InitialTutorialState.INCOMPLETE

enum class InitialTutorialState {
    INCOMPLETE,
    DISMISSED,
    COMPLETED,
}