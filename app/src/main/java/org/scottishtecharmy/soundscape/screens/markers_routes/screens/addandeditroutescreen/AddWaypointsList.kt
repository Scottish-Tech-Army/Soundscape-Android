package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
    getCurrentLocationDescription: () -> LocationDescription
) {
    val update = remember { mutableStateOf(true) }
    val fullerDescription = remember { getCurrentLocationDescription() }

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
    // Set the switches for those in the route to true
    val routeMember = remember(update.value, uiState.routeMembers, uiState.markers) {
        mutableStateMapOf<LocationDescription, Boolean>()
            .apply {
                uiState.markers.associateWith { uiState.toggledMembers.contains(it) }.also { putAll(it) }
                uiState.routeMembers.associateWith { !uiState.toggledMembers.contains(it) }.also { putAll(it) }
            }
    }
    update.value = false

    println("TOGGLED MEMBERS size ${uiState.toggledMembers.size}")

    // Add PlacesNearby entries
    val levelZeroFolders = listOf(
        Folder(R.string.search_nearby_screen_title, Icons.Rounded.LocationSearching, "", R.string.places_nearby_selection_description),
    )
    val levelOneFolders = placesNearbyFolders
    val context = LocalContext.current
    val nearbyLocations = remember(placesNearbyUiState) {
        filterLocations(placesNearbyUiState, context)
    }

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
                    val summaryDescription = LocationDescription(
                        "Current location",
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
                                    onSelectLocation(fullerDescription)
                                }
                            ),
                        ),
                        userLocation = currentLocation
                    )
                }
            }
            items(locations) { locationDescription ->
                val currentState = routeMember[locationDescription] == true
                LocationItem(
                    item = locationDescription,
                    decoration = LocationItemDecoration(
                        location = false,
                        editRoute = EnabledFunction(
                            enabled = true,
                            functionBoolean = {
                                if (uiState.toggledMembers.contains(locationDescription))
                                    uiState.toggledMembers.remove(locationDescription)
                                else
                                    uiState.toggledMembers.add(locationDescription)
                                update.value = true
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
                    mutableListOf(
                        LocationDescription(name = "Waypoint 2", location = LngLatAlt(), databaseId = 2L),
                        LocationDescription(name = "Waypoint 5", location = LngLatAlt(), databaseId = 5L),
                    )
            ),
        placesNearbyUiState = PlacesNearbyUiState(),
        onClickFolder = {_,_ -> },
        onSelectLocation = {_ -> },
        userLocation = LngLatAlt(),
        getCurrentLocationDescription = { LocationDescription("Location", LngLatAlt()) },
    )
}
