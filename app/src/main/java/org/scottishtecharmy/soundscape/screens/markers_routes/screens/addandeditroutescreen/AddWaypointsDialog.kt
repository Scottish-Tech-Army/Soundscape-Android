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
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

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
                    .padding(top = 20.dp, bottom = 10.dp),
                onClick = {
                    onDone()
                },
                buttonColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = RoundedCornerShape(10.dp),
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
                verticalArrangement = Arrangement.SpaceAround
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
    SoundscapeTheme {
        AddWaypointsDialog(
            modifier = Modifier,
            onCancel = {},
            onDone = { },
            userLocation = null,
            uiState =
                AddAndEditRouteUiState(
                    routeMembers =
                    mutableListOf(
                        LocationDescription(name = "Route point 1", location = LngLatAlt()),
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
                )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddWaypointsScreenPreview() {
    SoundscapeTheme {
        AddWaypointsDialog(
            modifier = Modifier,
            onCancel = {},
            onDone = {},
            uiState = AddAndEditRouteUiState(),
            userLocation = null
        )
    }
}
