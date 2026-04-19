package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.EnabledFunction
import org.scottishtecharmy.soundscape.components.FolderItem
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.process
import org.scottishtecharmy.soundscape.resources.*

@Composable
fun PlacesNearbyList(
    uiState: PlacesNearbyUiState,
    onSelectItem: (LocationDescription) -> Unit,
    onClickFolder: (String, String) -> Unit,
    onStartBeacon: (LocationDescription) -> Unit,
    modifier: Modifier,
) {
    val locations = remember(uiState) {
        filterLocations(uiState)
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
                                onSelectItem(locationDescription)
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
