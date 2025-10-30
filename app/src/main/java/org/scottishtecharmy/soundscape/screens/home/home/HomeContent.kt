package org.scottishtecharmy.soundscape.screens.home.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import org.scottishtecharmy.soundscape.screens.talkbackDescription
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

data class ButtonState(
    val onClick: () -> Unit,
    val imageVector: ImageVector,
    val contentDescriptionId: Int,
    val hintActive: Int = 0,
    val hintInactive: Int = 0,
)
@Composable
fun CardStatefulButton(states: Array<ButtonState>,
                       currentState: Int,
                       active: Boolean = true,
                       testTag: String) {

    val state = remember(currentState) { states[currentState] }
    var modifier = Modifier
        .talkbackDescription(stringResource(state.contentDescriptionId))
        .testTag(testTag)

    if(active) {
        if (state.hintActive != 0)
            modifier = modifier
                .talkbackHint(stringResource(state.hintActive))
                .clearAndSetSemantics {  } // The built in description is laborious
    } else
        modifier = modifier
            .clearAndSetSemantics {  } // The built in description is laborious

    Button(
        onClick = state.onClick,
        shape = MaterialTheme.shapes.small,
        colors = if (!LocalInspectionMode.current) currentAppButtonColors else ButtonDefaults.buttonColors(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        modifier = modifier
    )
    {
        Icon(
            modifier = modifier,
            imageVector = state.imageVector,
            tint = if (active)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            contentDescription =
                stringResource(state.contentDescriptionId)
        )
    }
}

@Composable
fun CardButton(onClick: () -> Unit,
               imageVector: ImageVector,
               active: Boolean = true,
               contentDescriptionId: Int,
               hintActive: Int = 0,
               hintInactive: Int = 0,
               testTag: String) {
    CardStatefulButton(
        arrayOf(ButtonState(onClick, imageVector, contentDescriptionId, hintActive, hintInactive)),
        0,
        active,
        testTag
    )
}
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
        if (streetPreviewState.enabled != StreetPreviewEnabled.OFF) {
            StreetPreview(streetPreviewState, heading, streetPreviewFunctions)
        }
        else {
            searchBar()
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
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
            if (location != null) {
                if (routePlayerState.routeData != null) {
                    Card(modifier = Modifier
                        .smallPadding(),
                    ) {
                            Row {
                                Text(
                                    text = stringResource(
                                        R.string.route_waypoint_progress).format(
                                            routePlayerState.routeData.route.name,
                                            routePlayerState.currentWaypoint + 1,
                                            routePlayerState.routeData.markers.size
                                        ),
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
                                if(!routePlayerState.beaconOnly) {
                                    CardButton(
                                        onClick = { routeFunctions.skipPrevious() },
                                        imageVector = Icons.Filled.SkipPrevious,
                                        active = (routePlayerState.currentWaypoint != 0),
                                        contentDescriptionId = R.string.route_detail_action_previous,
                                        hintActive = R.string.route_detail_action_previous_hint,
                                        hintInactive = R.string.route_detail_action_previous_disabled_hint,
                                        testTag = "routeSkipPrevious"
                                    )
                                    CardButton(
                                        onClick = { routeFunctions.skipNext() },
                                        imageVector = Icons.Filled.SkipNext,
                                        active = (routePlayerState.currentWaypoint < routePlayerState.routeData.markers.size - 1),
                                        contentDescriptionId = R.string.route_detail_action_next,
                                        hintActive = R.string.route_detail_action_next_hint,
                                        hintInactive = R.string.route_detail_action_next_disabled_hint,
                                        testTag = "routeSkipNext"
                                    )
                                    CardButton(
                                        onClick = { onNavigate("${HomeRoutes.RouteDetails.route}/${routePlayerState.routeData.route.routeId}") },
                                        imageVector = Icons.Filled.Info,
                                        contentDescriptionId = R.string.behavior_experiences_route_nav_title,
                                        testTag = "routeDetails"
                                    )
                                }
                                CardStatefulButton(
                                    arrayOf(
                                        ButtonState({ routeFunctions.mute() }, Icons.AutoMirrored.Filled.VolumeOff, R.string.beacon_action_unmute_beacon, R.string.beacon_action_unmute_beacon_acc_hint),
                                        ButtonState({ routeFunctions.mute() }, Icons.AutoMirrored.Filled.VolumeMute, R.string.beacon_action_mute_beacon, R.string.beacon_action_mute_beacon_acc_hint),
                                    ),
                                    currentState = if (beaconState?.muteState == true) 0 else 1,
                                    testTag = "RouteMute"
                                )

                                CardButton(
                                    onClick = { routeFunctions.stop() },
                                    imageVector = Icons.Filled.Stop,
                                    contentDescriptionId = R.string.route_detail_action_stop_route,
                                    testTag = "routeStop"
                                )

                                if(showMap) {
                                    CardButton(
                                        onClick = { fullscreenMap.value = !fullscreenMap.value },
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescriptionId = R.string.location_detail_full_screen_hint,
                                        testTag = "routeFullScreenMap"
                                    )
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
                name = "Test",
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

@Preview
@Composable
fun PreviewHomeContentBeacon() {
    val routePlayerState = RoutePlayerState(

        routeData = RouteWithMarkers(
            RouteEntity(
                routeId = 0,
                name = "Milngavie Town Hall",
                description = ""
            ),
            listOf(
                MarkerEntity(
                    name = "Marker 1",
                    longitude = -74.0060,
                    latitude = .7128,
                    fullAddress = "Description 1"
                )
            )
        ),
        beaconOnly = true,
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
