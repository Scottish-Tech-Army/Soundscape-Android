package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import org.scottishtecharmy.soundscape.resources.general_loading_start
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.EnabledFunction
import org.scottishtecharmy.soundscape.components.FolderItem
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.platform.ioDispatcher
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.general_alert_cancel
import org.scottishtecharmy.soundscape.resources.markers_action_create
import org.scottishtecharmy.soundscape.resources.markers_create_instructions
import org.scottishtecharmy.soundscape.resources.search_use_current_location
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedSaveAndEditMarkerScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.home.placesnearby.filterLocations
import org.scottishtecharmy.soundscape.screens.home.placesnearby.placesNearbyFolders
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.talkbackHidden
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.process

/**
 * Reached from the New button on the Markers tab. Like the Add Waypoints dialog, it lists Current
 * Location and Places Nearby, but with Current Location first and the Places Nearby categories
 * already expanded. Picking a place opens the save marker screen for it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SharedAddMarkerScreen(
    placesNearbyUiState: PlacesNearbyUiState,
    userLocation: LngLatAlt?,
    heading: Float,
    preferencesProvider: PreferencesProvider?,
    getCurrentLocationDescription: () -> LocationDescription,
    onClickFolder: (String, String) -> Unit,
    onClickBack: () -> Unit,
    onCancel: () -> Unit,
    onSave: (LocationDescription) -> Unit,
) {
    var selectedLocation by remember { mutableStateOf<LocationDescription?>(null) }

    // System back must leave the save screen or drill up a category, the same as the AppBar
    // button. See PlacesNearbyScreen for why this is the deprecated BackHandler.
    @Suppress("DEPRECATION")
    BackHandler(enabled = true) {
        when {
            selectedLocation != null -> selectedLocation = null
            placesNearbyUiState.level > 0 -> onClickBack()
            else -> onCancel()
        }
    }

    val selected = selectedLocation
    if (selected != null) {
        SharedSaveAndEditMarkerScreen(
            locationDescription = selected,
            userLocation = userLocation,
            heading = heading,
            preferencesProvider = preferencesProvider,
            onCancel = { selectedLocation = null },
            onSave = onSave,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        if (placesNearbyUiState.level == 0) {
            CustomAppBar(
                title = stringResource(Res.string.markers_action_create),
                navigationButtonTitle = stringResource(Res.string.general_alert_cancel),
                onNavigateUp = onCancel,
            )
        } else {
            CustomAppBar(
                title = placesNearbyUiState.title,
                navigationButtonTitle = stringResource(Res.string.ui_back_button_title),
                onNavigateUp = onClickBack,
            )
        }

        Spacer(modifier = Modifier.extraSmallPadding())

        if (placesNearbyUiState.level == 0) {
            Text(
                text = stringResource(Res.string.markers_create_instructions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium, vertical = spacing.small),
            )
        }

        AddMarkerList(
            placesNearbyUiState = placesNearbyUiState,
            userLocation = userLocation,
            getCurrentLocationDescription = getCurrentLocationDescription,
            onClickFolder = onClickFolder,
            onSelectLocation = { selectedLocation = it },
        )
    }
}

@Composable
private fun AddMarkerList(
    placesNearbyUiState: PlacesNearbyUiState,
    userLocation: LngLatAlt?,
    getCurrentLocationDescription: () -> LocationDescription,
    onClickFolder: (String, String) -> Unit,
    onSelectLocation: (LocationDescription) -> Unit,
) {
    val localizedStrings = remember { ComposeLocalizedStrings() }
    val nearbyLocations = remember(placesNearbyUiState) {
        filterLocations(placesNearbyUiState, localizedStrings)
    }
    val coroutineScope = rememberCoroutineScope()
    var fetchingLocation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        if (placesNearbyUiState.level == 0) {
            item {
                HorizontalDivider(
                    thickness = spacing.tiny,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            userLocation?.let { currentLocation ->
                item {
                    // The row stays in place while the address is looked up, so that TalkBack
                    // keeps its focus on it rather than jumping back to the top of the screen.
                    // The spinner stays silent unless the lookup is slow, when it announces once.
                    // LocationItem emits its row and divider as siblings, so the Column keeps
                    // them stacked inside the Box.
                    Box(contentAlignment = Alignment.CenterEnd) {
                        Column {
                            LocationItem(
                                item = LocationDescription(
                                    stringResource(Res.string.search_use_current_location),
                                    location = currentLocation
                                ),
                                decoration = LocationItemDecoration(
                                    location = true,
                                    details = EnabledFunction(
                                        true,
                                        {
                                            if (!fetchingLocation) {
                                                fetchingLocation = true
                                                coroutineScope.launch {
                                                    val ld = withContext(ioDispatcher) {
                                                        getCurrentLocationDescription()
                                                    }
                                                    fetchingLocation = false
                                                    onSelectLocation(ld)
                                                }
                                            }
                                        }
                                    ),
                                ),
                                userLocation = currentLocation,
                                modifier = Modifier.testTag("addMarkerCurrentLocation"),
                            )
                        }
                        if (fetchingLocation) {
                            var announceLoading by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(3000)
                                announceLoading = true
                            }
                            val loadingLabel = stringResource(Res.string.general_loading_start)
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = spacing.targetSize)
                                    .size(spacing.medium)
                                    .then(
                                        if (announceLoading) Modifier.semantics {
                                            contentDescription = loadingLabel
                                            liveRegion = LiveRegionMode.Polite
                                        } else Modifier.talkbackHidden()
                                    )
                                    .testTag("addMarkerCurrentLocationLoading"),
                            )
                        }
                    }
                }
            }
            itemsIndexed(placesNearbyFolders) { index, folderItem ->
                val name = stringResource(folderItem.nameResource)
                FolderItem(
                    name = name,
                    icon = folderItem.icon,
                    onClick = { onClickFolder(folderItem.filter, name) },
                    modifier = Modifier
                        .testTag("addMarkerFolder-$index")
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
                locationDescription.process(localizedStrings, placesNearbyUiState.countryCode)
                LocationItem(
                    item = locationDescription,
                    decoration = LocationItemDecoration(
                        location = true,
                        details = EnabledFunction(
                            true,
                            { onSelectLocation(locationDescription) }
                        )
                    ),
                    userLocation = placesNearbyUiState.userLocation,
                    modifier = Modifier.testTag("addMarkerPlace-$index"),
                )
            }
        }
    }
}
