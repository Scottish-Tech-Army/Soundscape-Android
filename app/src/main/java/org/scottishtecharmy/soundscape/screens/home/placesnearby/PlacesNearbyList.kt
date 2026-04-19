package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.ControlCamera
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.ForkLeft
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavController
import org.scottishtecharmy.soundscape.components.EnabledFunction
import org.scottishtecharmy.soundscape.components.FolderItem
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.process
import org.scottishtecharmy.soundscape.resources.*

data class Folder(
    val nameResource: StringResource,
    val icon: ImageVector,
    val filter: String,
    val talkbackDescriptionResource: StringResource
)

val placesNearbyFolders = listOf(
    Folder(Res.string.filter_all, Icons.Rounded.ControlCamera, "", Res.string.all_places_nearby_description),
    Folder(Res.string.filter_transit, Icons.Rounded.DirectionsBus, "transit", Res.string.public_transit_places_nearby_description),
    Folder(Res.string.filter_food_drink, Icons.Rounded.Fastfood, "food_and_drink", Res.string.food_drink_places_nearby_description),
    Folder(Res.string.filter_groceries, Icons.Rounded.LocalGroceryStore, "groceries", Res.string.groceries_places_nearby_description),
    Folder(Res.string.filter_banks, Icons.Rounded.AttachMoney, "banks", Res.string.banks_places_nearby_description),
    Folder(Res.string.osm_intersection, Icons.Rounded.ForkLeft, "intersections", Res.string.intersections_places_nearby_description),
)

@Composable
fun PlacesNearbyList(
    uiState: PlacesNearbyUiState,
    navController: NavController,
    onClickFolder: (String, String) -> Unit,
    onStartBeacon: (LocationDescription) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val locations = remember(uiState) {
        filterLocations(uiState, context)
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        if(uiState.level  == 0) {
            itemsIndexed(placesNearbyFolders) { index, folderItem ->
                if(index == 0) {
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
                        .testTag("placesNearby-$index")
                        .talkbackHint(stringResource(folderItem.talkbackDescriptionResource))
                )
            }
        } else {
            itemsIndexed(locations) { index, locationDescription ->
                if(index == 0) {
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
                                navController.navigate(
                                    generateLocationDetailsRoute(locationDescription)
                                )
                            }
                        ),
                        startPlayback = EnabledFunction(
                            enabled = true,
                            functionLocation = onStartBeacon,
                            hint = stringResource(Res.string.location_detail_action_beacon_hint)
                        ),
                    ),
                    userLocation = uiState.userLocation,
                    modifier = Modifier.testTag("placesNearby-$index")
                )
            }
        }
    }
}
