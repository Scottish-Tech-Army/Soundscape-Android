package org.scottishtecharmy.soundscape.screens.markers_routes.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.EnabledFunction
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun MarkersAndRoutesList(
    uiState: MarkersAndRoutesUiState,
    userLocation: LngLatAlt?,
    modifier: Modifier = Modifier,
    onSelect: (LocationDescription) -> Unit,
    onStartPlayback: (LocationDescription) -> Unit = {},
    onStartBeacon: (LocationDescription) -> Unit = {}
) {
    val startBeaconHint = stringResource(R.string.location_detail_action_beacon_hint)
    val startRouteHint = stringResource(R.string.route_detail_action_start_route_hint)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        items(uiState.entries) { locationDescription ->
            LocationItem(
                item = locationDescription,
                decoration = LocationItemDecoration(
                    location = uiState.markers,
                    details = EnabledFunction(
                        enabled = true,
                        functionLocation = onSelect
                    ),
                    startPlayback = EnabledFunction(
                        enabled = true,
                        functionLocation = if (uiState.markers) onStartBeacon else onStartPlayback,
                        hint = if (uiState.markers) startBeaconHint else startRouteHint
                    )
                ),
                userLocation = userLocation
            )
        }
    }
}
