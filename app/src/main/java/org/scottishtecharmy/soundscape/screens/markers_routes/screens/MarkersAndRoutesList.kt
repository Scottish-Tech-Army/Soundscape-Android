package org.scottishtecharmy.soundscape.screens.markers_routes.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    onSelect: (LocationDescription) -> Unit
) {
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
                    )
                ),
                userLocation = userLocation
            )
        }
    }
}
