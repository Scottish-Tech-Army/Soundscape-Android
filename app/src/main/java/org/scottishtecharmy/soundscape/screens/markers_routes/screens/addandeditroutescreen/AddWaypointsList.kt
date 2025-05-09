package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.ControlCamera
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.ForkLeft
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.components.EnabledFunction
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.FolderItem
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.utils.featureIsInFilterGroup
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.screens.home.placesnearby.Folder
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState

@Composable
fun AddWaypointsList(
    uiState: AddAndEditRouteUiState,
    placesNearbyUiState: PlacesNearbyUiState,
    onClickFolder: (String, String) -> Unit,
    userLocation: LngLatAlt?,
    onSelectLocation: (LocationDescription) -> Unit
) {
    // Create our list of locations, with those already in the route first
    val locations = remember(uiState) {
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
    val routeMember = remember(uiState) {
        mutableStateMapOf<LocationDescription, Boolean>()
            .apply {
                uiState.markers.associateWith { false }.also { putAll(it) }
                uiState.routeMembers.associateWith { true }.also { putAll(it) }
            }
    }

    // Add PlacesNearby entries
    val levelZeroFolders = listOf(
        Folder(stringResource(R.string.search_nearby_screen_title), Icons.Rounded.LocationSearching, ""),
    )
    val levelOneFolders = listOf(
        Folder(stringResource(R.string.filter_all), Icons.Rounded.ControlCamera, ""),
        Folder(stringResource(R.string.filter_transit), Icons.Rounded.DirectionsBus, "transit"),
        Folder(stringResource(R.string.filter_food_drink), Icons.Rounded.Fastfood, "food_and_drink"),
        Folder(stringResource(R.string.filter_groceries), Icons.Rounded.LocalGroceryStore, "groceries"),
        Folder(stringResource(R.string.filter_banks), Icons.Rounded.AttachMoney, "banks"),
        Folder(stringResource(R.string.osm_tag_intersection), Icons.Rounded.ForkLeft, "intersections"),
    )
    val context = LocalContext.current
    val ruler = placesNearbyUiState.userLocation?.createCheapRuler() ?: LngLatAlt().createCheapRuler()
    val nearbyLocations = remember(placesNearbyUiState) {
        if(placesNearbyUiState.filter == "intersections") {
            placesNearbyUiState.nearbyIntersections.features.filter { feature ->
                // Filter out un-named intersections
                feature.properties?.get("name").toString().isNotEmpty()
            }.map { feature ->
                LocationDescription(
                    name = feature.properties?.get("name").toString(),
                    location = getDistanceToFeature(LngLatAlt(), feature, ruler).point
                )
            }.sortedBy {
                placesNearbyUiState.userLocation?.let { location ->
                    ruler.distance(location, it.location)
                } ?: 0.0
            }

        } else {
            placesNearbyUiState.nearbyPlaces.features.filter { feature ->
                // Filter based on any folder selected
                featureIsInFilterGroup(feature, placesNearbyUiState.filter)

            }.map { feature ->
                LocationDescription(
                    name = getTextForFeature(context, feature).text,
                    location = getDistanceToFeature(LngLatAlt(), feature, ruler).point
                )
            }.sortedBy {
                placesNearbyUiState.userLocation?.let { location ->
                    ruler.distance(location, it.location)
                } ?: 0.0
            }
        }
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
                FolderItem(
                    name = folderItem.name,
                    icon = folderItem.icon,
                    onClick = {
                        onClickFolder(folderItem.filter, folderItem.name)
                    }
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
            items(locations) { locationDescription ->
                LocationItem(
                    item = locationDescription,
                    decoration = LocationItemDecoration(
                        location = false,
                        editRoute = EnabledFunction(
                            enabled = true,
                            functionBoolean = {
                                routeMember[locationDescription] = it
                                val updatedList = uiState.routeMembers.toMutableList()
                                if (it)
                                    updatedList.add(locationDescription)
                                else
                                    updatedList.remove(locationDescription)
                                uiState.routeMembers = updatedList
                            },
                            value = routeMember[locationDescription] == true,
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
