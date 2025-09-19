package org.scottishtecharmy.soundscape.screens.home.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.maplibre.android.maps.MapLibreMap.OnMapLongClickListener
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.NavigationButton
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.RouteFunctions
import org.scottishtecharmy.soundscape.screens.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun HomeContent(
    location: LngLatAlt?,
    beaconState: BeaconState?,
    routePlayerState: RoutePlayerState,
    heading: Float,
    onNavigate: (String) -> Unit,
    onMapLongClick: OnMapLongClickListener,
    getCurrentLocationDescription: () -> LocationDescription,
    searchBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    streetPreviewState: StreetPreviewState,
    streetPreviewFunctions: StreetPreviewFunctions,
    routeFunctions: RouteFunctions,
    goToAppSettings: (Context) -> Unit,
    fullscreenMap: MutableState<Boolean>,
    permissionsRequired: Boolean,
    showMap: Boolean) {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.small),
        modifier = modifier
    ) {
        searchBar()

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            if (streetPreviewState.enabled != StreetPreviewEnabled.OFF) {
                StreetPreview(streetPreviewState, heading, streetPreviewFunctions)
            } else {
                // Places Nearby
                NavigationButton(
                    onClick = {
                        onNavigate(HomeRoutes.PlacesNearby.route)
                    },
                    text = stringResource(R.string.search_nearby_screen_title),
                    horizontalPadding = spacing.small,
                    modifier = Modifier
                        .semantics { heading() }
                        .talkbackHint(stringResource(R.string.search_button_nearby_accessibility_hint))
                        .testTag("homePlacesNearby")
                )
                // Markers and routes
                NavigationButton(
                    onClick = {
                        onNavigate(HomeRoutes.MarkersAndRoutes.route)
                    },
                    text = stringResource(R.string.search_view_markers),
                    horizontalPadding = spacing.small,
                    modifier = Modifier
                        .talkbackHint(stringResource(R.string.search_button_markers_accessibility_hint))
                        .testTag("homeMarkersAndRoutes")
                )
                // Current location
                NavigationButton(
                    onClick = {
                        if (location != null) {
                            val ld = getCurrentLocationDescription()
                            onNavigate(generateLocationDetailsRoute(ld))
                        }
                    },
                    text = stringResource(R.string.search_use_current_location),
                    horizontalPadding = spacing.small,
                    modifier = Modifier
                        .talkbackHint(stringResource(R.string.search_button_current_location_accessibility_hint))
                        .testTag("homeCurrentLocation")
                )
            }
            if (location != null) {
                if (routePlayerState.routeData != null) {
                    Card(modifier = Modifier
                        .smallPadding(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                            Row {
                                Text(
                                    text = "${routePlayerState.routeData.route.name} - ${routePlayerState.currentWaypoint + 1}/${routePlayerState.routeData.markers.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.smallPadding()
                                )
                            }
                            if(showMap) {
                                Row(modifier = Modifier.fillMaxWidth().aspectRatio(2.0f)) {
                                    MapContainerLibre(
                                        beaconLocation = beaconState?.location,
                                        routeData = routePlayerState.routeData,
                                        mapCenter = location,
                                        allowScrolling = false,
                                        userLocation = location,
                                        userSymbolRotation = heading,
                                        onMapLongClick = onMapLongClick,
                                        modifier = Modifier.fillMaxWidth().extraSmallPadding(),
                                        showMap = true
                                    )
                                }
                            }
                            Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(spacing.targetSize)
                                    .padding(bottom = spacing.extraSmall),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = androidx.compose.ui.Alignment.Bottom
                            ) {
                                Button(
                                    onClick = { routeFunctions.skipPrevious() },
                                    colors = currentAppButtonColors,
                                    modifier = Modifier.testTag("routeSkipPrevious")
                                )
                                {
                                    Icon(
                                        modifier = Modifier.talkbackHint(
                                            stringResource(R.string.route_detail_action_previous_hint)
                                        ),
                                        imageVector = Icons.Filled.SkipPrevious,
                                        tint = if (routePlayerState.currentWaypoint == 0)
                                                   MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                               else
                                                   MaterialTheme.colorScheme.onSurface,
                                        contentDescription = stringResource(R.string.route_detail_action_previous)
                                    )
                                }
                                Button(
                                    onClick = { routeFunctions.skipNext() },
                                    colors = currentAppButtonColors,
                                    modifier = Modifier.testTag("routeSkipNext")
                                )
                                {
                                    Icon(
                                        modifier = Modifier.talkbackHint(
                                            stringResource(R.string.route_detail_action_next_hint)
                                        ),
                                        imageVector = Icons.Filled.SkipNext,
                                        tint = if (routePlayerState.currentWaypoint < routePlayerState.routeData.markers.size - 1)
                                                   MaterialTheme.colorScheme.onSurface
                                               else
                                                   MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        contentDescription = stringResource(R.string.route_detail_action_next)
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (routePlayerState.beaconOnly) {
                                            routeFunctions.stop()
                                        } else {
                                            onNavigate("${HomeRoutes.RouteDetails.route}/${routePlayerState.routeData.route.routeId}")
                                        }
                                    },
                                    colors = currentAppButtonColors,
                                    modifier = Modifier.testTag("routeStopOrDetails")
                                )
                                {
                                    if (routePlayerState.beaconOnly) {
                                        Icon(
                                            modifier = Modifier,
                                            imageVector = Icons.Filled.Stop,
                                            contentDescription = stringResource(R.string.route_detail_action_stop_route),
                                        )
                                    } else {
                                        Icon(
                                            modifier = Modifier,
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = stringResource(R.string.behavior_experiences_route_nav_title),
                                        )
                                    }
                                }
                                Button(
                                    onClick = { routeFunctions.mute() },
                                    colors = currentAppButtonColors,
                                    modifier = Modifier.testTag("routeMute")
                                )
                                {
                                    Icon(
                                        modifier = Modifier.talkbackHint(
                                            if (beaconState?.muteState == true)
                                                stringResource(R.string.beacon_action_unmute_beacon_acc_hint)
                                            else
                                                stringResource(R.string.beacon_action_mute_beacon_acc_hint)
                                        ),
                                        imageVector = if (beaconState?.muteState == true)
                                            Icons.AutoMirrored.Filled.VolumeOff
                                        else
                                            Icons.AutoMirrored.Filled.VolumeMute,
                                        contentDescription = if (beaconState?.muteState == true)
                                            stringResource(R.string.beacon_action_unmute_beacon)
                                        else
                                            stringResource(R.string.beacon_action_mute_beacon),
                                    )
                                }
                                if(showMap) {
                                    Button(
                                        onClick = { fullscreenMap.value = !fullscreenMap.value },
                                        colors = currentAppButtonColors,
                                        modifier = Modifier.testTag("routeFullScreenMap")
                                    )
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Fullscreen,
                                            contentDescription = stringResource(R.string.location_detail_full_screen_hint)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    MapContainerLibre(
                        beaconLocation = beaconState?.location,
                        routeData = null,
                        mapCenter = location,
                        allowScrolling = false,
                        userLocation = location,
                        userSymbolRotation = heading,
                        onMapLongClick = onMapLongClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .mediumPadding(),
                        showMap = showMap
                    )
                }
            } else {
                if(permissionsRequired) {
                    Column {
                        Text(
                            stringResource(R.string.permissions_required),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .mediumPadding()
                        )
                        NavigationButton(
                            onClick = {
                                goToAppSettings(context)
                            },
                            text = stringResource(R.string.permissions_button),
                            horizontalPadding = spacing.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .mediumPadding()
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun StreetPreviewHomeContent() {
    HomeContent(
        location = null,
        beaconState = null,
        routePlayerState = RoutePlayerState(),
        heading = 45.0f,
        onNavigate = {},
        onMapLongClick = { false },
        searchBar = {},
        streetPreviewState = StreetPreviewState(StreetPreviewEnabled.ON),
        streetPreviewFunctions = StreetPreviewFunctions(null),
        routeFunctions = RouteFunctions(null),
        getCurrentLocationDescription = { LocationDescription("Current location", LngLatAlt()) },
        goToAppSettings = {},
        fullscreenMap = remember { mutableStateOf(false) },
        permissionsRequired = false,
        showMap = true
    )
}

@Preview
@Composable
fun PreviewHomeContent() {
    val routePlayerState = RoutePlayerState(
        routeData = RouteWithMarkers(
            RouteEntity(
                name = "Route 1",
                description = "Description 1"
            ),
            listOf(
                MarkerEntity(
                    name = "Marker 1",
                    longitude = -74.0060,
                    latitude = .7128,
                    fullAddress = "Description 1"
                ),
                MarkerEntity(
                    name = "Marker 2",
                    longitude = -74.0060,
                    latitude = .7128,
                    fullAddress = "Description 2"
                )
            )
        ),
        currentWaypoint = 0
    )

    HomeContent(
        location = LngLatAlt(),
        beaconState = null,
        routePlayerState = routePlayerState,
        heading = 45.0f,
        onNavigate = {},
        onMapLongClick = { false },
        searchBar = {},
        streetPreviewState = StreetPreviewState(StreetPreviewEnabled.OFF),
        streetPreviewFunctions = StreetPreviewFunctions(null),
        routeFunctions = RouteFunctions(null),
        getCurrentLocationDescription = { LocationDescription("Current location", LngLatAlt()) },
        goToAppSettings = {},
        fullscreenMap = remember { mutableStateOf(false) },
        permissionsRequired = false,
        showMap = true
    )
}
