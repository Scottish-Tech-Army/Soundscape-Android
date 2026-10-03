package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.NavigationButton
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.resolveGrammarMarkers
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.beacon_action_mute_beacon
import org.scottishtecharmy.soundscape.resources.beacon_action_mute_beacon_acc_hint
import org.scottishtecharmy.soundscape.resources.beacon_action_unmute_beacon
import org.scottishtecharmy.soundscape.resources.beacon_action_unmute_beacon_acc_hint
import org.scottishtecharmy.soundscape.resources.beacon_action_callout_beacon
import org.scottishtecharmy.soundscape.resources.behavior_experiences_route_nav_title
import org.scottishtecharmy.soundscape.resources.callouts_action_more_info
import org.scottishtecharmy.soundscape.resources.markers_action_add_to_markers
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
import org.scottishtecharmy.soundscape.resources.routes_title
import org.scottishtecharmy.soundscape.resources.markers_title
import org.scottishtecharmy.soundscape.resources.search_button_current_location_accessibility_hint
import org.scottishtecharmy.soundscape.resources.search_button_markers_only_accessibility_hint
import org.scottishtecharmy.soundscape.resources.search_button_routes_accessibility_hint
import org.scottishtecharmy.soundscape.resources.search_button_nearby_accessibility_hint
import org.scottishtecharmy.soundscape.resources.search_nearby_screen_title
import org.scottishtecharmy.soundscape.resources.search_use_current_location
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
    onNavigate: (String) -> Unit,
    onSelectLocation: (LocationDescription) -> Unit,
    onShowRouteDetails: (LocationDescription) -> Unit,
    /** Saves the beacon's location as a marker, for the beacon card's "Add to Markers" action. */
    onSaveMarker: ((LocationDescription) -> Unit)? = null,
    getCurrentLocationDescription: () -> LocationDescription,
    searchBar: @Composable () -> Unit,
    streetPreviewState: StreetPreviewState,
    streetPreviewFunctions: StreetPreviewFunctions,
    routeFunctions: RouteFunctions,
    goToAppSettings: () -> Unit,
    map: FullScreenableMap,
    permissionsRequired: Boolean,
    showMap: Boolean,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var fetchingLocation by remember { mutableStateOf(false) }
    var mapInteracting by remember { mutableStateOf(false) }
    val contentScrollState = rememberScrollState()

    // The map takes whatever height the rest of the scrolling content leaves free, so that the
    // home screen fits without scrolling where it can. It's never smaller than MAP_MIN_HEIGHT,
    // or taller than it is wide; if even the minimum doesn't fit, the screen scrolls as before.
    // Until the first layout has been measured the map keeps its old fixed shape.
    val density = LocalDensity.current
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var contentHeightPx by remember { mutableIntStateOf(0) }
    var mapSlotSize by remember { mutableStateOf(IntSize.Zero) }
    val mapSlotHeight: Dp? =
        if (viewportHeightPx == 0 || contentHeightPx == 0 || mapSlotSize == IntSize.Zero) {
            null
        } else {
            with(density) {
                val minPx = MAP_MIN_HEIGHT.roundToPx()
                val otherContentPx = contentHeightPx - mapSlotSize.height
                (viewportHeightPx - otherContentPx)
                    .coerceIn(minPx, mapSlotSize.width.coerceAtLeast(minPx))
                    .toDp()
            }
        }
    fun Modifier.mapSlot(defaultAspectRatio: Float) =
        (if (mapSlotHeight != null) height(mapSlotHeight) else aspectRatio(defaultAspectRatio))
            .onSizeChanged { mapSlotSize = it }

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
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { viewportHeightPx = it.height }
                    .verticalScroll(contentScrollState, enabled = !mapInteracting)
                    // The scroll container stretches short content to fill the viewport, which
                    // would hide free space from the map sizing; measure the natural height.
                    .wrapContentHeight(Alignment.Top)
                    .onSizeChanged { contentHeightPx = it.height },
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
                    onClick = { onNavigate(SharedRoutes.MARKERS) },
                    text = stringResource(Res.string.markers_title),
                    horizontalPadding = spacing.small,
                    modifier = Modifier
                        .talkbackHint(stringResource(Res.string.search_button_markers_only_accessibility_hint))
                        .testTag("homeMarkers"),
                )
                NavigationButton(
                    onClick = { onNavigate(SharedRoutes.ROUTES) },
                    text = stringResource(Res.string.routes_title),
                    horizontalPadding = spacing.small,
                    modifier = Modifier
                        .talkbackHint(stringResource(Res.string.search_button_routes_accessibility_hint))
                        .testTag("homeRoutes"),
                )
                // The button stays in place while the address is looked up, so that TalkBack
                // keeps its focus on it (see SlowLoadingIndicator).
                NavigationButton(
                        onClick = {
                            if (location != null && !fetchingLocation) {
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
                        loading = fetchingLocation,
                        loadingTestTag = "homeCurrentLocationLoading",
                    )
                if (location != null) {
                    val currentRoute = routePlayerState.routeData
                    if (currentRoute != null) {
                        Card(modifier = Modifier.smallPadding()) {
                            // The waypoint the beacon is currently on. For a beacon (rather than
                            // a route) this is RoutePlayer's single-waypoint pseudo-route, whose
                            // marker carries the name the beacon was started with.
                            val beaconWaypoint =
                                currentRoute.markers.getOrNull(routePlayerState.currentWaypoint)
                            val beaconLocation =
                                beaconWaypoint?.getLngLatAlt() ?: beaconState?.location
                            val beaconName = beaconWaypoint?.name ?: currentRoute.route.name
                            // Resolved here rather than at the Text, because the card's semantics
                            // replace the Text's and the screen reader reads this string directly.
                            val progressText = resolveGrammarMarkers(
                                if (currentRoute.markers.size > 1) {
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
                                }
                            )

                            // "700 m, NW" on screen, "700 metres, north west" for the screen
                            // reader - the same split the original iOS app used, where there is
                            // room for the word in speech but not in the panel.
                            val distanceStrings = remember(location, beaconLocation) {
                                if (beaconLocation == null) {
                                    "" to ""
                                } else {
                                    val ruler = location.createCheapRuler()
                                    val distance = ruler.distance(location, beaconLocation)
                                    val bearing = ruler.bearing(location, beaconLocation)
                                    val localized = ComposeLocalizedStrings()
                                    formatDistanceAndDirection(
                                        distance,
                                        bearing,
                                        localized,
                                        abbreviatedDirection = true,
                                    ) to formatDistanceAndDirection(
                                        distance,
                                        bearing,
                                        localized,
                                        forAccessibility = true,
                                    )
                                }
                            }
                            val (distanceString, distanceStringA11y) = distanceStrings

                            val calloutLabel = stringResource(Res.string.beacon_action_callout_beacon)
                            val moreInfoLabel = stringResource(Res.string.callouts_action_more_info)
                            val addToMarkersLabel =
                                stringResource(Res.string.markers_action_add_to_markers)
                            // Mute and Remove are the buttons below and deliberately aren't
                            // repeated here - iOS offers both ways and the duplication confuses
                            // more than it helps (issue #908). Call out and More Info both speak
                            // through the service's TTS, not the screen reader, so that they can
                            // be triggered without the screen too.
                            val beaconActions = buildList {
                                add(
                                    CustomAccessibilityAction(calloutLabel) {
                                        routeFunctions.calloutBeacon()
                                        true
                                    }
                                )
                                if (beaconLocation != null) {
                                    add(
                                        CustomAccessibilityAction(moreInfoLabel) {
                                            routeFunctions.beaconMoreInfo()
                                            true
                                        }
                                    )
                                    // Already a saved marker, so nothing to add - the same test
                                    // the iOS app made before offering this action.
                                    if (onSaveMarker != null &&
                                        (beaconWaypoint?.markerId ?: 0L) == 0L
                                    ) {
                                        add(
                                            CustomAccessibilityAction(addToMarkersLabel) {
                                                onSaveMarker(
                                                    LocationDescription(
                                                        name = beaconName,
                                                        location = beaconLocation,
                                                    )
                                                )
                                                true
                                            }
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .smallPadding()
                                    .testTag("routeBeaconTitle")
                                    .clearAndSetSemantics {
                                        contentDescription = listOf(progressText, distanceStringA11y)
                                            .filter { it.isNotEmpty() }
                                            .joinToString(", ")
                                        onClick(label = calloutLabel) {
                                            routeFunctions.calloutBeacon()
                                            true
                                        }
                                        customActions = beaconActions
                                    },
                            ) {
                                Text(
                                    text = progressText,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                if (distanceString.isNotEmpty()) {
                                    Text(
                                        text = distanceString,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.testTag("routeBeaconDistance"),
                                    )
                                }
                            }
                            if (showMap) {
                                Row(modifier = Modifier.fillMaxWidth().mapSlot(2.0f)) {
                                    map.Inline(
                                        modifier = Modifier.fillMaxWidth().extraSmallPadding(),
                                        onInteractionChanged = { mapInteracting = it },
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
                                        // The route details screen takes its route through the
                                        // NavigationStateHolder, keyed by back stack entry, like
                                        // every other per-entry payload in this graph - there is no
                                        // "route_details_screen/{id}" destination to navigate to.
                                        onClick = {
                                            onShowRouteDetails(
                                                LocationDescription(
                                                    name = currentRoute.route.name,
                                                    location = currentRoute.markers.firstOrNull()
                                                        ?.let { LngLatAlt(it.longitude, it.latitude) }
                                                        ?: LngLatAlt(),
                                                    description = currentRoute.route.description,
                                                    databaseId = currentRoute.route.routeId,
                                                )
                                            )
                                        },
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
                                        onClick = { map.fullscreen.value = !map.fullscreen.value },
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescriptionId = Res.string.location_detail_full_screen_hint,
                                        testTag = "routeFullScreenMap",
                                    )
                                }
                            }
                        }
                    } else if (showMap) {
                        map.Inline(
                            modifier = Modifier
                                .fillMaxWidth()
                                .mapSlot(1f)
                                .mediumPadding(),
                            onInteractionChanged = { mapInteracting = it },
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
                                    .mediumPadding()
                                    .testTag("homeAppSettings"),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The smallest the home screen map shrinks to when fitting it into the free space. */
private val MAP_MIN_HEIGHT = 160.dp
