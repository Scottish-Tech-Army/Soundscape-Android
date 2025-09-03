package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SaveAndEditMarkerDialog
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.components.TextOnlyAppBar
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding

@Composable
fun AddWaypointsDialog(
    uiState: AddAndEditRouteUiState,
    placesNearbyUiState: PlacesNearbyUiState,
    routeList: MutableList<LocationDescription>,
    modifier: Modifier,
    onAddWaypointComplete: () -> Unit,
    onClickFolder: (String, String) -> Unit,
    onClickBack: () -> Unit,
    onSelectLocation: (LocationDescription) -> Unit,
    createAndAddMarker: (LocationDescription, String, String) -> Unit,
    userLocation: LngLatAlt?,
    getCurrentLocationDescription: () -> LocationDescription,
    heading: Float,
) {
    val saveMarkerDialog = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (saveMarkerDialog.value) {
            SaveAndEditMarkerDialog(
                locationDescription = placesNearbyUiState.markerDescription!!,
                location = placesNearbyUiState.userLocation,
                heading = heading,
                saveMarker = { description, success, failure ->
                    createAndAddMarker(description, success, failure)
                },
                deleteMarker = {},
                dialogState = saveMarkerDialog
            )
        } else {
            TextOnlyAppBar(
                title = stringResource(R.string.route_detail_edit_waypoints_button),
                onNavigateUp = { onClickBack() },
                navigationButtonTitle = stringResource(R.string.ui_back_button_title),
                onRightButton = {
                    // Update the list of waypoints in the route
                    onAddWaypointComplete()
                },
                rightButtonTitle = stringResource(R.string.general_alert_done)
            )

            Spacer(modifier = Modifier.extraSmallPadding())

            // Display the list of routes
            AddWaypointsList(
                uiState = uiState,
                placesNearbyUiState = placesNearbyUiState,
                routeList = routeList,
                onClickFolder = onClickFolder,
                onSelectLocation = { location ->
                    saveMarkerDialog.value = true
                    onSelectLocation(location)
                },
                userLocation = userLocation,
                getCurrentLocationDescription = getCurrentLocationDescription
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddWaypointsScreenPopulatedPreview() {
    AddWaypointsDialog(
        uiState =
            AddAndEditRouteUiState(
                routeMembers =
                mutableListOf(
                    LocationDescription(name = "Long named route point as if we need one", location = LngLatAlt()),
                    LocationDescription(name = "Route point 2", location = LngLatAlt()),
                    LocationDescription(name = "Route point 3", location = LngLatAlt()),
                    LocationDescription(name = "Route point 4", location = LngLatAlt()),
                    LocationDescription(name = "Route point 5", location = LngLatAlt()),
                    LocationDescription(name = "Route point 6", location = LngLatAlt()),
                    LocationDescription(name = "Route point 7", location = LngLatAlt()),
                    LocationDescription(name = "Route point 8", location = LngLatAlt()),
                ),
                markers =
                mutableListOf(
                    LocationDescription(name = "Waypoint 1", location = LngLatAlt()),
                    LocationDescription(name = "Waypoint 2", location = LngLatAlt()),
                    LocationDescription(name = "Waypoint 3", location = LngLatAlt()),
                    LocationDescription(name = "Waypoint 4", location = LngLatAlt()),
                    LocationDescription(name = "Waypoint 5", location = LngLatAlt()),
                    LocationDescription(name = "Waypoint 6", location = LngLatAlt()),
                    LocationDescription(name = "Waypoint 7", location = LngLatAlt()),
                    LocationDescription(name = "Waypoint 8", location = LngLatAlt()),
                )
            ),
        placesNearbyUiState = PlacesNearbyUiState(),
        routeList = emptyList<LocationDescription>().toMutableList(),
        modifier = Modifier,
        onAddWaypointComplete = { },
        onClickFolder = {_,_ -> },
        onClickBack = {},
        onSelectLocation = {_ -> },
        createAndAddMarker = {_,_,_ -> },
        userLocation = LngLatAlt(),
        heading = 45.0F,
        getCurrentLocationDescription = { LocationDescription("Location", LngLatAlt()) },
    )
}

@Preview(showBackground = true)
@Composable
fun AddWaypointsScreenPreview() {
    AddWaypointsDialog(
        uiState = AddAndEditRouteUiState(),
        placesNearbyUiState = PlacesNearbyUiState(),
        routeList = emptyList<LocationDescription>().toMutableList(),
        modifier = Modifier,
        onAddWaypointComplete = {},
        onClickFolder = {_,_ -> },
        onClickBack = {},
        onSelectLocation = {_ -> },
        createAndAddMarker = {_,_,_ -> },
        userLocation = null,
        heading = 45.0F,
        getCurrentLocationDescription = { LocationDescription("Location", LngLatAlt()) },
    )
}
