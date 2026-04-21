package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedHomeScreen(
    homeState: HomeState,
    modifier: Modifier = Modifier,
    // Bottom bar actions
    onMyLocation: () -> Unit = {},
    onAroundMe: () -> Unit = {},
    onAheadOfMe: () -> Unit = {},
    onNearbyMarkers: () -> Unit = {},
    // Navigation
    onNavigateToPlacesNearby: () -> Unit = {},
    onNavigateToMarkersAndRoutes: () -> Unit = {},
    onNavigateToOfflineMaps: () -> Unit = {},
    // Route controls
    onRouteSkipPrevious: () -> Unit = {},
    onRouteSkipNext: () -> Unit = {},
    onRouteMute: () -> Unit = {},
    onRouteStop: () -> Unit = {},
    // Search
    onSearch: (String) -> Unit = {},
    onSearchItemClick: (LocationDescription) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    Row {
                        IconButton(onClick = onNavigateToPlacesNearby) {
                            Icon(
                                Icons.Rounded.Explore,
                                contentDescription = stringResource(Res.string.search_nearby_screen_title),
                            )
                        }
                        IconButton(onClick = onNavigateToMarkersAndRoutes) {
                            Icon(
                                Icons.Rounded.Route,
                                contentDescription = stringResource(Res.string.search_view_markers),
                            )
                        }
                        IconButton(onClick = onNavigateToOfflineMaps) {
                            Icon(
                                Icons.Rounded.Download,
                                contentDescription = stringResource(Res.string.offline_maps_title),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            SharedHomeBottomAppBar(
                buttonFunctions = BottomButtonFunctions(
                    myLocation = onMyLocation,
                    aroundMe = onAroundMe,
                    aheadOfMe = onAheadOfMe,
                    nearbyMarkers = onNearbyMarkers,
                ),
            )
        },
    ) { innerPadding ->
        SharedHomeContent(
            homeState = homeState,
            modifier = Modifier.padding(innerPadding),
            onRouteSkipPrevious = onRouteSkipPrevious,
            onRouteSkipNext = onRouteSkipNext,
            onRouteMute = onRouteMute,
            onRouteStop = onRouteStop,
            onSearch = onSearch,
            onSearchItemClick = onSearchItemClick,
        )
    }
}
