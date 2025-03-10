package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.ControlCamera
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.EnabledFunction
import org.scottishtecharmy.soundscape.components.FolderItem
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.utils.featureIsInFilterGroup
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.ui.theme.spacing

data class Folder(
    val name: String,
    val icon: ImageVector,
    val filter: String
)

@Composable
fun PlacesNearbyList(
    uiState: PlacesNearbyUiState,
    navController: NavController,
    onClickFolder: (String) -> Unit,
    modifier: Modifier
) {
    val folders = listOf(
        Folder(stringResource(R.string.filter_all), Icons.Rounded.ControlCamera, ""),
        Folder(stringResource(R.string.filter_transit), Icons.Rounded.DirectionsBus, "transit"),
        Folder(stringResource(R.string.filter_food_drink), Icons.Rounded.Fastfood, "food_and_drink"),
        Folder(stringResource(R.string.filter_groceries), Icons.Rounded.LocalGroceryStore, "groceries"),
        Folder(stringResource(R.string.filter_banks), Icons.Rounded.AttachMoney, "banks"),
    )
    val context = LocalContext.current
    val osmSet = mutableSetOf<Any>()
    val locations = remember(uiState) {
        uiState.nearbyPlaces.features.filter { feature ->

            // TODO: This simple deduplication based on OSM id is a workaround. We
            //  really want polygons to be merged and de-duplicated at the point that
            //  the GridState is updated.
            val osmId = feature.foreign?.get("osm_ids")
            if (osmId != null) {
                if (osmSet.contains(osmId)) {
                    false
                } else {
                    osmSet.add(osmId)
                }
            }

            // Filter based on any folder selected
            featureIsInFilterGroup(feature, uiState.filter)

        }.map { feature ->
            LocationDescription(
                name = getTextForFeature(context, feature).text,
                location = getDistanceToFeature(LngLatAlt(), feature).point
            )
        }.sortedBy { uiState.userLocation?.distance(it.location) }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        if(uiState.topLevel) {
            items(folders) { folderItem ->
                FolderItem(
                    name = folderItem.name,
                    icon = folderItem.icon,
                    onClick = {
                        onClickFolder(folderItem.filter)
                    }
                )
            }
        } else {
            items(locations) { locationDescription ->
                LocationItem(
                    item = locationDescription,
                    decoration = LocationItemDecoration(
                        location = true,
                        details = EnabledFunction(
                            true,
                            {
                                navController.navigate(
                                    generateLocationDetailsRoute(locationDescription)
                                )
                            }
                        )
                    ),
                    userLocation = uiState.userLocation
                )
            }
        }
    }
}
