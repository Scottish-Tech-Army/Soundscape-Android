package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.preferences.rememberBooleanPreference
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.FullScreenMapFab
import org.scottishtecharmy.soundscape.screens.home.home.PlatformMapContainer
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun SharedRouteDetailsScreen(
    routeName: String,
    routeDescription: String,
    waypoints: List<LocationDescription>,
    isRoutePlaying: Boolean,
    userLocation: LngLatAlt?,
    heading: Float,
    preferencesProvider: PreferencesProvider? = null,
    onNavigateUp: () -> Unit,
    onStartRoute: () -> Unit,
    onStartRouteInReverse: () -> Unit,
    onStopRoute: () -> Unit,
    onEditRoute: () -> Unit,
    onShareRoute: (() -> Unit)? = null,
) {
    val showMap by rememberBooleanPreference(
        preferencesProvider,
        PreferenceKeys.SHOW_MAP,
        PreferenceDefaults.SHOW_MAP,
    )
    val routeWithMarkers = remember(waypoints, routeName, routeDescription) {
        RouteWithMarkers(
            route = RouteEntity(name = routeName, description = routeDescription),
            markers = waypoints.map { wp ->
                MarkerEntity(
                    name = wp.name,
                    longitude = wp.location.longitude,
                    latitude = wp.location.latitude,
                )
            }
        )
    }

    val firstWaypoint = waypoints.firstOrNull()?.location ?: LngLatAlt()
    val fullscreenMap = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CustomAppBar(
                title = stringResource(Res.string.behavior_experiences_route_nav_title),
                onNavigateUp = onNavigateUp,
            )
        },
        floatingActionButton = {
            if (showMap) FullScreenMapFab(fullscreenMap)
        }
    ) { innerPadding ->
        if (fullscreenMap.value && showMap) {
            PlatformMapContainer(
                beaconLocation = null,
                routeData = routeWithMarkers,
                allowScrolling = true,
                mapCenter = firstWaypoint,
                userLocation = userLocation,
                userSymbolRotation = heading,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (waypoints.isEmpty() && routeName.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(Res.string.route_not_found))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .smallPadding()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .smallPadding(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = routeName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(bottom = spacing.extraSmall),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (routeDescription.isNotEmpty()) {
                                    Text(
                                        text = routeDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.smallPadding()) {
                            if (isRoutePlaying) {
                                IconWithTextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.Stop,
                                    textModifier = Modifier.padding(horizontal = spacing.extraSmall),
                                    text = stringResource(Res.string.route_detail_action_stop_route),
                                    talkbackHint = stringResource(Res.string.route_detail_action_stop_route_hint),
                                    color = MaterialTheme.colorScheme.onSurface
                                ) {
                                    onStopRoute()
                                }
                            } else {
                                IconWithTextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.PlayArrow,
                                    textModifier = Modifier.padding(horizontal = spacing.extraSmall),
                                    talkbackHint = stringResource(Res.string.route_detail_action_start_route_hint),
                                    text = stringResource(Res.string.route_detail_action_start_route),
                                    color = MaterialTheme.colorScheme.onSurface
                                ) {
                                    onStartRoute()
                                }
                                IconWithTextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.SwapVert,
                                    textModifier = Modifier.padding(horizontal = spacing.extraSmall),
                                    talkbackHint = stringResource(Res.string.route_detail_action_start_route_reverse_hint),
                                    text = stringResource(Res.string.route_detail_action_start_route_reverse),
                                    color = MaterialTheme.colorScheme.onSurface
                                ) {
                                    onStartRouteInReverse()
                                }
                            }
                            IconWithTextButton(
                                modifier = Modifier.fillMaxWidth()
                                    .defaultMinSize(minHeight = spacing.targetSize),
                                icon = Icons.Default.Edit,
                                textModifier = Modifier.padding(horizontal = spacing.extraSmall),
                                text = stringResource(Res.string.route_detail_action_edit),
                                talkbackHint = stringResource(Res.string.route_detail_action_edit_hint),
                                color = MaterialTheme.colorScheme.onSurface
                            ) {
                                onEditRoute()
                            }
                            if (onShareRoute != null) {
                                IconWithTextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.Share,
                                    textModifier = Modifier.padding(horizontal = spacing.extraSmall),
                                    text = stringResource(Res.string.share_title),
                                    talkbackHint = stringResource(Res.string.route_detail_action_share_hint),
                                    color = MaterialTheme.colorScheme.onSurface
                                ) {
                                    onShareRoute()
                                }
                            }
                        }
                        if (showMap) {
                            PlatformMapContainer(
                                beaconLocation = null,
                                routeData = routeWithMarkers,
                                allowScrolling = true,
                                mapCenter = firstWaypoint,
                                userLocation = userLocation,
                                userSymbolRotation = heading,
                                modifier = Modifier.fillMaxWidth().weight(1f).smallPadding(),
                            )
                        }
                        Spacer(modifier = Modifier.size(spacing.medium))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(spacing.tiny),
                            modifier = Modifier.weight(2f)
                        ) {
                            itemsIndexed(waypoints) { index, waypoint ->
                                LocationItem(
                                    item = LocationDescription(
                                        name = waypoint.name,
                                        location = waypoint.location,
                                    ),
                                    decoration = LocationItemDecoration(
                                        location = false,
                                        index = index,
                                        indexDescription = stringResource(Res.string.waypoint_title),
                                    ),
                                    userLocation = userLocation
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
