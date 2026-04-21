package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.DrawerMenuItem
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
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    // Route controls
    onRouteSkipPrevious: () -> Unit = {},
    onRouteSkipNext: () -> Unit = {},
    onRouteMute: () -> Unit = {},
    onRouteStop: () -> Unit = {},
    // Search
    onSearch: (String) -> Unit = {},
    onSearchItemClick: (LocationDescription) -> Unit = {},
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerContentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    DrawerMenuItem(
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToPlacesNearby()
                        },
                        label = stringResource(Res.string.search_nearby_screen_title),
                        icon = Icons.Rounded.Explore,
                    )
                    DrawerMenuItem(
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToMarkersAndRoutes()
                        },
                        label = stringResource(Res.string.search_view_markers),
                        icon = Icons.Rounded.Route,
                    )
                    DrawerMenuItem(
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToOfflineMaps()
                        },
                        label = stringResource(Res.string.offline_maps_title),
                        icon = Icons.Rounded.Download,
                    )
                    DrawerMenuItem(
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        },
                        label = stringResource(Res.string.settings_screen_title),
                        icon = Icons.Rounded.Settings,
                    )
                    DrawerMenuItem(
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToHelp()
                        },
                        label = stringResource(Res.string.menu_help),
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    )
                }
            }
        },
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
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Rounded.Menu,
                                contentDescription = stringResource(Res.string.ui_menu),
                            )
                        }
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
}
