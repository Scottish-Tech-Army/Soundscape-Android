package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.MainSearchBar
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.resources.*

@Composable
fun SharedHomeContent(
    homeState: HomeState,
    modifier: Modifier = Modifier,
    onRouteSkipPrevious: () -> Unit = {},
    onRouteSkipNext: () -> Unit = {},
    onRouteMute: () -> Unit = {},
    onRouteStop: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onSearchItemClick: (LocationDescription) -> Unit = {},
    onMapLongClick: ((LngLatAlt) -> Boolean)? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        MainSearchBar(
            results = homeState.searchItems ?: emptyList(),
            onTriggerSearch = onSearch,
            onItemClick = onSearchItemClick,
            userLocation = homeState.location,
            isSearching = homeState.searchInProgress,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )

        // Route player card (if route is active)
        val routeData = homeState.currentRouteData
        if (routeData.routeData != null) {
            RoutePlayerCard(
                routeState = routeData,
                onSkipPrevious = onRouteSkipPrevious,
                onSkipNext = onRouteSkipNext,
                onMute = onRouteMute,
                onStop = onRouteStop,
            )
        }

        // Map filling the remaining space
        PlatformMapContainer(
            mapCenter = homeState.location ?: LngLatAlt(0.0, 0.0),
            allowScrolling = true,
            userLocation = homeState.location,
            userSymbolRotation = homeState.heading,
            beaconLocation = homeState.beaconState?.location,
            routeData = routeData.routeData,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun RoutePlayerCard(
    routeState: RoutePlayerState,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onMute: () -> Unit,
    onStop: () -> Unit,
) {
    val route = routeState.routeData ?: return
    val waypointIndex = routeState.currentWaypoint
    val waypointName = route.markers.getOrNull(waypointIndex)?.name ?: ""
    val totalWaypoints = route.markers.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            // Route name
            Text(
                text = route.route.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Current waypoint
            if (waypointName.isNotEmpty()) {
                Text(
                    text = "${waypointIndex + 1}/$totalWaypoints: $waypointName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onSkipPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(Res.string.route_detail_action_previous))
                }
                IconButton(onClick = onMute) {
                    Icon(
                        if (routeState.beaconOnly) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = stringResource(Res.string.beacon_action_mute_beacon),
                    )
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(Res.string.route_detail_action_stop_route))
                }
                IconButton(onClick = onSkipNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = stringResource(Res.string.route_detail_action_next))
                }
            }
        }
    }
}
