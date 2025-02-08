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
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun AddWaypointsDialog(
    uiState: AddAndEditRouteUiState,
    modifier: Modifier,
    onDone: () -> Unit,
    onCancel: () -> Unit
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
                    uiState = uiState
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
            uiState =
                AddAndEditRouteUiState(
                    routeMembers =
                    mutableListOf(
                        LocationDescription("Route point 1"),
                        LocationDescription("Route point 2"),
                        LocationDescription("Route point 3"),
                        LocationDescription("Route point 4"),
                        LocationDescription("Route point 5"),
                        LocationDescription("Route point 6"),
                        LocationDescription("Route point 7"),
                        LocationDescription("Route point 8"),
                    ),
                    markers =
                    mutableListOf(
                        LocationDescription("Waypoint 1"),
                        LocationDescription("Waypoint 2"),
                        LocationDescription("Waypoint 3"),
                        LocationDescription("Waypoint 4"),
                        LocationDescription("Waypoint 5"),
                        LocationDescription("Waypoint 6"),
                        LocationDescription("Waypoint 7"),
                        LocationDescription("Waypoint 8"),
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
        )
    }
}
