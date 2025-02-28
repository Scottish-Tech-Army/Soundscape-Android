package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun AddWaypointsDialog(
    uiState: AddAndEditRouteUiState,
    modifier: Modifier,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    userLocation: LngLatAlt?
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                CustomAppBar(
                    title = stringResource(R.string.route_detail_edit_waypoints_button),
                    navigationButtonTitle = stringResource(R.string.general_alert_cancel),
                    onNavigateUp = onCancel
                )
            }
        },
        bottomBar = {
            CustomButton(
                Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.medium, bottom = spacing.small),
                onClick = {
                    onDone()
                },
                buttonColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(spacing.small),
                text = stringResource(R.string.general_alert_done),
                textStyle = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Display the list of routes
                AddWaypointsList(
                    uiState = uiState,
                    userLocation = userLocation
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddWaypointsScreenPopulatedPreview() {
    AddWaypointsDialog(
        modifier = Modifier,
        onCancel = {},
        onDone = { },
        userLocation = null,
        uiState =
            AddAndEditRouteUiState(
                routeMembers =
                mutableListOf(
                    LocationDescription(name = "Long named route point as if we need one", location = LngLatAlt()),
                    LocationDescription(name = "Route point 2", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Route point 3", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Route point 4", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Route point 5", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Route point 6", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Route point 7", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Route point 8", location = LngLatAlt(), markerObjectId = ObjectId()),
                ),
                markers =
                mutableListOf(
                    LocationDescription(name = "Waypoint 1", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Waypoint 2", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Waypoint 3", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Waypoint 4", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Waypoint 5", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Waypoint 6", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Waypoint 7", location = LngLatAlt(), markerObjectId = ObjectId()),
                    LocationDescription(name = "Waypoint 8", location = LngLatAlt(), markerObjectId = ObjectId()),
                )
            )
    )
}

@Preview(showBackground = true)
@Composable
fun AddWaypointsScreenPreview() {
    AddWaypointsDialog(
        modifier = Modifier,
        onCancel = {},
        onDone = {},
        uiState = AddAndEditRouteUiState(),
        userLocation = null
    )
}
