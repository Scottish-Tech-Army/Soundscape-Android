package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.NavigationButton
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.beacon_action_mute_beacon
import org.scottishtecharmy.soundscape.resources.beacon_action_mute_beacon_acc_hint
import org.scottishtecharmy.soundscape.resources.beacon_action_unmute_beacon
import org.scottishtecharmy.soundscape.resources.beacon_action_unmute_beacon_acc_hint
import org.scottishtecharmy.soundscape.resources.behavior_experiences_route_nav_title
import org.scottishtecharmy.soundscape.resources.general_loading_start
import org.scottishtecharmy.soundscape.resources.location_detail_full_screen_hint
import org.scottishtecharmy.soundscape.resources.permissions_button
import org.scottishtecharmy.soundscape.resources.permissions_required
import org.scottishtecharmy.soundscape.resources.route_beacon_progress
import org.scottishtecharmy.soundscape.resources.route_detail_action_next
import org.scottishtecharmy.soundscape.resources.route_detail_action_next_disabled_hint
import org.scottishtecharmy.soundscape.resources.route_detail_action_next_hint
import org.scottishtecharmy.soundscape.resources.route_detail_action_previous
import org.scottishtecharmy.soundscape.resources.route_detail_action_previous_disabled_hint
import org.scottishtecharmy.soundscape.resources.route_detail_action_previous_hint
import org.scottishtecharmy.soundscape.resources.route_detail_action_stop_route
import org.scottishtecharmy.soundscape.resources.route_waypoint_progress
import org.scottishtecharmy.soundscape.resources.search_button_current_location_accessibility_hint
import org.scottishtecharmy.soundscape.resources.search_button_markers_accessibility_hint
import org.scottishtecharmy.soundscape.resources.search_button_nearby_accessibility_hint
import org.scottishtecharmy.soundscape.resources.search_nearby_screen_title
import org.scottishtecharmy.soundscape.resources.search_use_current_location
import org.scottishtecharmy.soundscape.resources.search_view_markers
import org.scottishtecharmy.soundscape.resources.voice_cmd_listening
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
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
    val contentDescriptionId: StringResource,
    val hintActive: StringResource? = null,
    val hintInactive: StringResource? = null,
)

@Composable
fun CardStatefulButton(
    states: Array<ButtonState>,
    currentState: Int,
    active: Boolean = true,
    testTag: String,
) {
    val state = remember(currentState) { states[currentState] }
    var modifier = Modifier
        .talkbackDescription(stringResource(state.contentDescriptionId))
        .testTag(testTag)

    if (active) {
        if (state.hintActive != null) {
            modifier = modifier
                .talkbackHint(stringResource(state.hintActive))
                .clearAndSetSemantics { }
        }
    } else {
        modifier = modifier.clearAndSetSemantics { }
    }

    Button(
        onClick = state.onClick,
        shape = MaterialTheme.shapes.small,
        colors = if (!LocalInspectionMode.current) currentAppButtonColors else ButtonDefaults.buttonColors(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        modifier = modifier,
    ) {
        Icon(
            modifier = modifier,
            imageVector = state.imageVector,
            tint = if (active) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            },
            contentDescription = stringResource(state.contentDescriptionId),
        )
    }
}

@Composable
fun CardButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    active: Boolean = true,
    contentDescriptionId: StringResource,
    hintActive: StringResource? = null,
    hintInactive: StringResource? = null,
    testTag: String,
) {
    CardStatefulButton(
        arrayOf(ButtonState(onClick, imageVector, contentDescriptionId, hintActive, hintInactive)),
        0,
        active,
        testTag,
    )
}

@Composable
fun SharedHomeContent(
    location: LngLatAlt?,
    beaconState: BeaconState?,
    routePlayerState: RoutePlayerState,
    heading: Float,
    onNavigate: (String) -> Unit,
    onSelectLocation: (LocationDescription) -> Unit,
    onMapLongClick: ((LngLatAlt) -> Boolean)?,
    getCurrentLocationDescription: () -> LocationDescription,
    searchBar: @Composable () -> Unit,
    streetPreviewState: StreetPreviewState,
    streetPreviewFunctions: StreetPreviewFunctions,
    routeFunctions: RouteFunctions,
    goToAppSettings: () -> Unit,
    fullscreenMap: MutableState<Boolean>,
    permissionsRequired: Boolean,
    showMap: Boolean,
    voiceCommandListening: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var fetchingLocation by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (streetPreviewState.enabled != StreetPreviewEnabled.OFF) {
                StreetPreview(streetPreviewState, streetPreviewFunctions)
            } else {
                searchBar()
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.small),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                NavigationButton(
                    onClick = { onNavigate(SharedRoutes.PLACES_NEARBY) },
                    text = stringResource(Res.string.search_nearby_screen_title),
                    horizontalPadding = spacing.small,
                    modifier = Modifier
                        .semantics { heading() }
                        .talkbackHint(stringResource(Res.string.search_button_nearby_accessibility_hint))
                        .testTag("homePlacesNearby"),
                )
                NavigationButton(
                    onClick = { onNavigate(SharedRoutes.MARKERS_AND_ROUTES) },
                    text = stringResource(Res.string.search_view_markers),
                    horizontalPadding = spacing.small,
                    modifier = Modifier
                        .talkbackHint(stringResource(Res.string.search_button_markers_accessibility_hint))
                        .testTag("homeMarkersAndRoutes"),
                )
                if (fetchingLocation) {
                    var announceLoading by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1500)
                        announceLoading = true
                    }
                    val loadingText = stringResource(Res.string.general_loading_start)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .then(
                                if (announceLoading) {
                                    Modifier.semantics {
                                        contentDescription = loadingText
                                        liveRegion = LiveRegionMode.Polite
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .testTag("homeCurrentLocationLoading"),
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    NavigationButton(
                        onClick = {
                            if (location != null) {
                                fetchingLocation = true
                                coroutineScope.launch {
                                    val ld = withContext(Dispatchers.Default) {
                                        getCurrentLocationDescription()
                                    }
                                    fetchingLocation = false
                                    onSelectLocation(ld)
                                }
                            }
                        },
                        text = stringResource(Res.string.search_use_current_location),
                        horizontalPadding = spacing.small,
                        modifier = Modifier
                            .talkbackHint(stringResource(Res.string.search_button_current_location_accessibility_hint))
                            .testTag("homeCurrentLocation"),
                    )
                }
                if (location != null) {
                    val currentRoute = routePlayerState.routeData
                    if (currentRoute != null) {
                        Card(modifier = Modifier.smallPadding()) {
                            Row {
                                Text(
                                    text = if (currentRoute.markers.size > 1) {
                                        stringResource(
                                            Res.string.route_waypoint_progress,
                                            currentRoute.route.name,
                                            routePlayerState.currentWaypoint + 1,
                                            currentRoute.markers.size,
                                        )
                                    } else {
                                        stringResource(
                                            Res.string.route_beacon_progress,
                                            currentRoute.route.name,
                                        )
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.smallPadding(),
                                )
                            }
                            if (showMap) {
                                Row(modifier = Modifier.fillMaxWidth().aspectRatio(2.0f)) {
                                    PlatformMapContainer(
                                        beaconLocation = beaconState?.location,
                                        routeData = routePlayerState.routeData,
                                        mapCenter = location,
                                        allowScrolling = false,
                                        userLocation = location,
                                        userSymbolRotation = heading,
                                        modifier = Modifier.fillMaxWidth().extraSmallPadding(),
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(spacing.targetSize)
                                    .padding(bottom = spacing.extraSmall),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                if (!routePlayerState.beaconOnly) {
                                    CardButton(
                                        onClick = { routeFunctions.skipPrevious() },
                                        imageVector = Icons.Filled.SkipPrevious,
                                        active = (routePlayerState.currentWaypoint != 0),
                                        contentDescriptionId = Res.string.route_detail_action_previous,
                                        hintActive = Res.string.route_detail_action_previous_hint,
                                        hintInactive = Res.string.route_detail_action_previous_disabled_hint,
                                        testTag = "routeSkipPrevious",
                                    )
                                    CardButton(
                                        onClick = { routeFunctions.skipNext() },
                                        imageVector = Icons.Filled.SkipNext,
                                        active = (routePlayerState.currentWaypoint < currentRoute.markers.size - 1),
                                        contentDescriptionId = Res.string.route_detail_action_next,
                                        hintActive = Res.string.route_detail_action_next_hint,
                                        hintInactive = Res.string.route_detail_action_next_disabled_hint,
                                        testTag = "routeSkipNext",
                                    )
                                    CardButton(
                                        onClick = { onNavigate("${SharedRoutes.ROUTE_DETAILS}/${currentRoute.route.routeId}") },
                                        imageVector = Icons.Filled.Info,
                                        contentDescriptionId = Res.string.behavior_experiences_route_nav_title,
                                        testTag = "routeDetails",
                                    )
                                }
                                CardStatefulButton(
                                    arrayOf(
                                        ButtonState(
                                            { routeFunctions.mute() },
                                            Icons.AutoMirrored.Filled.VolumeOff,
                                            Res.string.beacon_action_unmute_beacon,
                                            Res.string.beacon_action_unmute_beacon_acc_hint,
                                        ),
                                        ButtonState(
                                            { routeFunctions.mute() },
                                            Icons.AutoMirrored.Filled.VolumeMute,
                                            Res.string.beacon_action_mute_beacon,
                                            Res.string.beacon_action_mute_beacon_acc_hint,
                                        ),
                                    ),
                                    currentState = if (beaconState?.muteState == true) 0 else 1,
                                    testTag = "RouteMute",
                                )

                                CardButton(
                                    onClick = { routeFunctions.stop() },
                                    imageVector = Icons.Filled.Stop,
                                    contentDescriptionId = Res.string.route_detail_action_stop_route,
                                    testTag = "routeStop",
                                )

                                if (showMap) {
                                    CardButton(
                                        onClick = { fullscreenMap.value = !fullscreenMap.value },
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescriptionId = Res.string.location_detail_full_screen_hint,
                                        testTag = "routeFullScreenMap",
                                    )
                                }
                            }
                        }
                    } else if (showMap) {
                        PlatformMapContainer(
                            beaconLocation = beaconState?.location,
                            routeData = null,
                            mapCenter = location,
                            allowScrolling = false,
                            userLocation = location,
                            userSymbolRotation = heading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .mediumPadding(),
                        )
                    }
                } else {
                    if (permissionsRequired) {
                        Column {
                            Text(
                                stringResource(Res.string.permissions_required),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .mediumPadding(),
                            )
                            NavigationButton(
                                onClick = { goToAppSettings() },
                                text = stringResource(Res.string.permissions_button),
                                horizontalPadding = spacing.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .mediumPadding(),
                            )
                        }
                    }
                }
            }
        }
        if (voiceCommandListening) {
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = stringResource(Res.string.voice_cmd_listening),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(vertical = spacing.small),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
