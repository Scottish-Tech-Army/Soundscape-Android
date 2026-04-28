package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.general_alert_cancel
import org.scottishtecharmy.soundscape.resources.general_alert_done
import org.scottishtecharmy.soundscape.resources.general_error_add_marker_duplicate
import org.scottishtecharmy.soundscape.resources.general_error_add_marker_error
import org.scottishtecharmy.soundscape.resources.markers_marker_created
import org.scottishtecharmy.soundscape.resources.route_detail_edit_waypoints_button
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedSaveAndEditMarkerScreen
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
    onToggleMember: (LocationDescription) -> Unit,
    createAndAddMarker: (LocationDescription, String, String, String) -> Unit,
    userLocation: LngLatAlt?,
    getCurrentLocationDescription: () -> LocationDescription,
    heading: Float,
) {
    var showSaveMarker by remember { mutableStateOf(false) }
    val successMessage = stringResource(Res.string.markers_marker_created)
    val failureMessage = stringResource(Res.string.general_error_add_marker_error)
    val duplicateMessage = stringResource(Res.string.general_error_add_marker_duplicate)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        if (showSaveMarker && placesNearbyUiState.markerDescription != null) {
            SharedSaveAndEditMarkerScreen(
                locationDescription = placesNearbyUiState.markerDescription!!,
                userLocation = placesNearbyUiState.userLocation,
                heading = heading,
                onCancel = { showSaveMarker = false },
                onSave = { updated ->
                    createAndAddMarker(updated, successMessage, failureMessage, duplicateMessage)
                    showSaveMarker = false
                },
            )
        } else {
            TextOnlyAppBar(
                title = stringResource(Res.string.route_detail_edit_waypoints_button),
                onNavigateUp = onClickBack,
                navigationButtonTitle = stringResource(Res.string.general_alert_cancel),
                onRightButton = onAddWaypointComplete,
                rightButtonTitle = stringResource(Res.string.general_alert_done),
            )

            Spacer(modifier = Modifier.extraSmallPadding())

            AddWaypointsList(
                uiState = uiState,
                placesNearbyUiState = placesNearbyUiState,
                onClickFolder = onClickFolder,
                onSelectLocation = { location ->
                    showSaveMarker = true
                    onSelectLocation(location)
                },
                onToggleMember = onToggleMember,
                userLocation = userLocation,
                getCurrentLocationDescription = getCurrentLocationDescription,
            )
        }
    }
}
