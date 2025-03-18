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
import org.mongodb.kbson.ObjectId
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
    modifier: Modifier,
    onAddWaypointComplete: () -> Unit,
    onClickFolder: (String, String) -> Unit,
    onClickBack: () -> Unit,
    onSelectLocation: (LocationDescription) -> Unit,
    createAndAddMarker: (LocationDescription, String, String) -> Unit,
    userLocation: LngLatAlt?
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
                heading = 0.0F,
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
                onRightButton = onAddWaypointComplete,
                rightButtonTitle = stringResource(R.string.general_alert_done)
            )

            Spacer(modifier = Modifier.extraSmallPadding())

            // Display the list of routes
            AddWaypointsList(
                uiState = uiState,
                placesNearbyUiState = placesNearbyUiState,
                onClickFolder = onClickFolder,
                onSelectLocation = { location ->
                    saveMarkerDialog.value = true
                    onSelectLocation(location)
                },
                userLocation = userLocation
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddWaypointsScreenPopulatedPreview() {
    AddWaypointsDialog(
        modifier = Modifier,
        onAddWaypointComplete = { },
        userLocation = null,
        uiState =
            AddAndEditRouteUiState(
                routeMembers =
                mutableListOf(
                    LocationDescription(name = "Long named route point as if we need one", location = LngLatAlt()),
                    LocationDescription(name = "Route point 2", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Route point 3", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Route point 4", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Route point 5", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Route point 6", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Route point 7", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Route point 8", location = LngLatAlt(), databaseId = ObjectId()),
                ),
                markers =
                mutableListOf(
                    LocationDescription(name = "Waypoint 1", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Waypoint 2", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Waypoint 3", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Waypoint 4", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Waypoint 5", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Waypoint 6", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Waypoint 7", location = LngLatAlt(), databaseId = ObjectId()),
                    LocationDescription(name = "Waypoint 8", location = LngLatAlt(), databaseId = ObjectId()),
                )
            ),
        placesNearbyUiState = PlacesNearbyUiState(),
        onClickFolder = {_,_ -> },
        onClickBack = {},
        onSelectLocation = {_ -> },
        createAndAddMarker = {_,_,_ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun AddWaypointsScreenPreview() {
    AddWaypointsDialog(
        modifier = Modifier,
        onAddWaypointComplete = {},
        uiState = AddAndEditRouteUiState(),
        userLocation = null,
        placesNearbyUiState = PlacesNearbyUiState(),
        onClickFolder = {_,_ -> },
        onClickBack = {},
        onSelectLocation = {_ -> },
        createAndAddMarker = {_,_,_ -> }
    )
}
