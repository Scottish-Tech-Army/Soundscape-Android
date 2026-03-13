package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.components.EnabledFunction
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.FolderItem
import org.scottishtecharmy.soundscape.screens.home.placesnearby.Folder
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.home.placesnearby.filterLocations
import org.scottishtecharmy.soundscape.screens.home.placesnearby.placesNearbyFolders
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.utils.process

@Composable
fun AddWaypointsList(
    uiState: AddAndEditRouteUiState,
    placesNearbyUiState: PlacesNearbyUiState,
    onClickFolder: (String, String) -> Unit,
    userLocation: LngLatAlt?,
    onSelectLocation: (LocationDescription) -> Unit,
    onToggleMember: (LocationDescription) -> Unit,
    getCurrentLocationDescription: () -> LocationDescription
) {
    // Create our list of locations, with those already in the route first
    val locations = remember(uiState.routeMembers, uiState.markers) {
        mutableStateListOf<LocationDescription>()
            .apply {
                addAll(uiState.routeMembers)
                addAll(uiState.markers.filter { marker ->
                    uiState.routeMembers.none { routeMember ->
                        routeMember.databaseId == marker.databaseId
                    }
                }
            )
        }
    }
    // Set the switches for those in the route to true, keyed by databaseId
    val routeMemberState = remember(uiState.routeMembers, uiState.markers, uiState.toggledMembers) {
        mutableStateMapOf<Long?, Boolean>().apply {
            // Markers not in route: true if toggled in
            uiState.markers.forEach { marker ->
                put(marker.databaseId, uiState.toggledMembers.any { it.databaseId == marker.databaseId })
            }
            // Route members: true unless toggled out
            uiState.routeMembers.forEach { member ->
                put(member.databaseId, !uiState.toggledMembers.any { it.databaseId == member.databaseId })
            }
        }
    }
    val enabledCount = routeMemberState.count { it.value }
    println("${uiState.routeMembers.size} ${uiState.markers.size} ${uiState.toggledMembers.size} -> $enabledCount")

    // Add PlacesNearby entries
    val levelZeroFolders = listOf(
        Folder(R.string.search_nearby_screen_title, Icons.Rounded.LocationSearching, "", R.string.places_nearby_selection_description),
    )
    val levelOneFolders = placesNearbyFolders
    val context = LocalContext.current
    val nearbyLocations = remember(placesNearbyUiState) {
        filterLocations(placesNearbyUiState, context)
    }
    val coroutineScope = rememberCoroutineScope()
    var fetchingLocation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        val folders = when (placesNearbyUiState.level) {
            0 -> levelZeroFolders
            1 -> levelOneFolders
            else -> emptyList()
        }
        if (placesNearbyUiState.level <= 1) {
            itemsIndexed(folders) { index, folderItem ->
                if (index == 0) {
                    HorizontalDivider(
                        thickness = spacing.tiny,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                val name = stringResource(folderItem.nameResource)
                FolderItem(
                    name = name,
                    icon = folderItem.icon,
                    onClick = {
                        onClickFolder(folderItem.filter, name)
                    },
                    modifier = Modifier
                        .talkbackHint(stringResource(folderItem.talkbackDescriptionResource))
                )
            }
        } else {
            itemsIndexed(nearbyLocations) { index, locationDescription ->
                if (index == 0) {
                    HorizontalDivider(
                        thickness = spacing.tiny,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                locationDescription.process()
                LocationItem(
                    item = locationDescription,
                    decoration = LocationItemDecoration(
                        location = true,
                        details = EnabledFunction(
                            true,
                            {
                                onSelectLocation(locationDescription)
                            }
                        )
                    ),
                    userLocation = placesNearbyUiState.userLocation
                )
            }
        }

        if (placesNearbyUiState.level == 0) {
            userLocation?.let { currentLocation ->
                items(1) {
                    if (fetchingLocation) {
                        var announceLoading by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1500)
                            announceLoading = true
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .then(
                                    if (announceLoading) Modifier.semantics {
                                        contentDescription = context.getString(R.string.general_loading_start)
                                        liveRegion = LiveRegionMode.Polite
                                    } else Modifier
                                )
                                .testTag("addWaypointsCurrentLocationLoading"),
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val summaryDescription = LocationDescription(
                            stringResource(R.string.search_use_current_location),
                            location = currentLocation
                        )
                        LocationItem(
                            item = summaryDescription,
                            decoration = LocationItemDecoration(
                                location = true,
                                editRoute = EnabledFunction(false),
                                details = EnabledFunction(
                                    true,
                                    {
                                        fetchingLocation = true
                                        coroutineScope.launch {
                                            val ld = withContext(Dispatchers.IO) {
                                                getCurrentLocationDescription()
                                            }
                                            fetchingLocation = false
                                            onSelectLocation(ld)
                                        }
                                    }
                                ),
                            ),
                            userLocation = currentLocation
                        )
                    }
                }
            }
            items(locations) { locationDescription ->
                val currentState = routeMemberState[locationDescription.databaseId] == true
                LocationItem(
                    item = locationDescription,
                    decoration = LocationItemDecoration(
                        location = false,
                        editRoute = EnabledFunction(
                            enabled = true,
                            functionBoolean = {
                                onToggleMember(locationDescription)
                            },
                            value = currentState,
                            hintWhenOn = stringResource(R.string.location_detail_add_waypoint_existing_hint),
                            hintWhenOff = stringResource(R.string.location_detail_add_waypoint_new_hint)
                        )
                    ),
                    userLocation = userLocation
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddWaypointsListPreview() {
    AddWaypointsList(
        uiState =
            AddAndEditRouteUiState(
                routeMembers =
                    mutableListOf(
                        LocationDescription(name = "Waypoint 1", location = LngLatAlt(), databaseId = 1L),
                        LocationDescription(name = "Waypoint 2", location = LngLatAlt(), databaseId = 2L),
                        LocationDescription(name = "Waypoint 3", location = LngLatAlt(), databaseId = 3L),
                    ),
                markers =
                    mutableListOf(
                        LocationDescription(name = "Waypoint 1", location = LngLatAlt(), databaseId = 1L),
                        LocationDescription(name = "Waypoint 2", location = LngLatAlt(), databaseId = 2L),
                        LocationDescription(name = "Waypoint 3", location = LngLatAlt(), databaseId = 3L),
                        LocationDescription(name = "Waypoint 4", location = LngLatAlt(), databaseId = 4L),
                        LocationDescription(name = "Waypoint 5", location = LngLatAlt(), databaseId = 5L),
                        LocationDescription(name = "Waypoint 6", location = LngLatAlt(), databaseId = 6L),
                        LocationDescription(name = "Waypoint 7", location = LngLatAlt(), databaseId = 7L),
                        LocationDescription(name = "Waypoint 8", location = LngLatAlt(), databaseId = 8L),
                    ),
                toggledMembers =
                    listOf(
                        LocationDescription(name = "Waypoint 2", location = LngLatAlt(), databaseId = 2L),
                        LocationDescription(name = "Waypoint 5", location = LngLatAlt(), databaseId = 5L),
                    )
            ),
        placesNearbyUiState = PlacesNearbyUiState(),
        onClickFolder = {_,_ -> },
        onSelectLocation = {_ -> },
        onToggleMember = {_ -> },
        userLocation = LngLatAlt(),
        getCurrentLocationDescription = { LocationDescription("Location", LngLatAlt()) },
    )
}
