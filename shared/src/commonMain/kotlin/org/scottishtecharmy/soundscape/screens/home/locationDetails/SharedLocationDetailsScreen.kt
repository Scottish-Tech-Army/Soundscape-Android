package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.preferences.rememberBooleanPreference
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.FullScreenMapFab
import org.scottishtecharmy.soundscape.screens.home.home.PlatformMapContainer
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * Shared LocationDetails screen matching the Android layout:
 * location info, action buttons, and a map showing the location.
 */
@Composable
fun SharedLocationDetailsScreen(
    locationDescription: LocationDescription,
    userLocation: LngLatAlt?,
    heading: Float = 0f,
    preferencesProvider: PreferencesProvider? = null,
    onNavigateUp: () -> Unit,
    onStartBeacon: (LngLatAlt, String) -> Unit,
    onSaveMarker: ((LocationDescription) -> Unit)? = null,
    onEditMarker: ((LocationDescription) -> Unit)? = null,
    onDeleteMarker: ((Long) -> Unit)? = null,
    onEnableStreetPreview: ((LngLatAlt) -> Unit)? = null,
    onShareLocation: ((LocationDescription) -> Unit)? = null,
    onOfflineMaps: ((LocationDescription) -> Unit)? = null,
) {
    val showMap by rememberBooleanPreference(
        preferencesProvider,
        PreferenceKeys.SHOW_MAP,
        PreferenceDefaults.SHOW_MAP,
    )
    val fullscreenMap = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CustomAppBar(
                title = stringResource(Res.string.location_detail_title_default),
                onNavigateUp = onNavigateUp,
            )
        },
        floatingActionButton = {
            if (showMap) FullScreenMapFab(fullscreenMap)
        },
    ) { padding ->
        if (fullscreenMap.value && showMap) {
            PlatformMapContainer(
                beaconLocation = locationDescription.location,
                allowScrolling = true,
                mapCenter = locationDescription.location,
                userLocation = userLocation,
                userSymbolRotation = heading,
                routeData = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surface),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                // Location info section
                LocationDescriptionTextsSection(
                    locationDescription = locationDescription,
                    userLocation = userLocation,
                )

                HorizontalDivider()

                // Action buttons
                LocationDescriptionButtonsSection(
                    locationDescription = locationDescription,
                    onStartBeacon = onStartBeacon,
                    onSaveMarker = onSaveMarker,
                    onEditMarker = onEditMarker,
                    onEnableStreetPreview = onEnableStreetPreview,
                    onShareLocation = onShareLocation,
                    onOfflineMaps = onOfflineMaps,
                )

                // Map showing the location
                if (showMap) {
                    PlatformMapContainer(
                        mapCenter = locationDescription.location,
                        allowScrolling = true,
                        userLocation = userLocation,
                        userSymbolRotation = heading,
                        beaconLocation = locationDescription.location,
                        routeData = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.0f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationDescriptionTextsSection(
    locationDescription: LocationDescription,
    userLocation: LngLatAlt?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.small),
        modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small),
    ) {
        Text(
            text = locationDescription.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        locationDescription.typeDescription?.let {
            val additional = it.additionalText
            if (!additional.isNullOrEmpty()) {
                Text(
                    text = additional,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        val distanceString = remember(userLocation) {
            if (userLocation == null) return@remember ""
            val ruler = userLocation.createCheapRuler()
            formatDistanceAndDirection(
                ruler.distance(userLocation, locationDescription.location),
                ruler.bearing(userLocation, locationDescription.location),
                ComposeLocalizedStrings()
            )
        }
        if (distanceString.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = distanceString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        locationDescription.description?.let { desc ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun LocationDescriptionButtonsSection(
    locationDescription: LocationDescription,
    onStartBeacon: (LngLatAlt, String) -> Unit,
    onSaveMarker: ((LocationDescription) -> Unit)?,
    onEditMarker: ((LocationDescription) -> Unit)?,
    onEnableStreetPreview: ((LngLatAlt) -> Unit)?,
    onShareLocation: ((LocationDescription) -> Unit)?,
    onOfflineMaps: ((LocationDescription) -> Unit)?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.none),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium),
    ) {
        // Start beacon
        IconWithTextButton(
            icon = Icons.Filled.LocationOn,
            text = stringResource(Res.string.location_detail_action_beacon),
            talkbackHint = stringResource(Res.string.location_detail_action_beacon_hint),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .defaultMinSize(minHeight = spacing.targetSize)
                .fillMaxWidth()
                .testTag("locationDetailsStartBeacon")
        ) {
            onStartBeacon(locationDescription.location, locationDescription.name)
        }

        // Edit or save marker
        if (locationDescription.databaseId != 0L && onEditMarker != null) {
            IconWithTextButton(
                icon = Icons.Filled.EditLocation,
                text = stringResource(Res.string.markers_edit_screen_title_edit),
                talkbackHint = stringResource(Res.string.location_detail_action_edit_hint),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .fillMaxWidth()
                    .testTag("locationDetailsEditMarker")
            ) {
                onEditMarker(locationDescription)
            }
        } else if (onSaveMarker != null && locationDescription.databaseId == 0L) {
            IconWithTextButton(
                icon = Icons.Filled.AddLocation,
                text = stringResource(Res.string.universal_links_alert_action_marker),
                talkbackHint = stringResource(Res.string.location_detail_action_save_hint),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .fillMaxWidth()
                    .testTag("locationDetailsSaveAsMarker")
            ) {
                onSaveMarker(locationDescription)
            }
        }

        // Street preview
        if (onEnableStreetPreview != null) {
            IconWithTextButton(
                icon = Icons.Filled.Navigation,
                text = stringResource(Res.string.preview_title),
                talkbackHint = stringResource(Res.string.location_detail_action_preview_hint),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .fillMaxWidth()
                    .testTag("locationDetailsStreetPreview")
            ) {
                onEnableStreetPreview(locationDescription.location)
            }
        }

        // Share
        if (onShareLocation != null) {
            IconWithTextButton(
                icon = Icons.Filled.ShareLocation,
                text = stringResource(Res.string.share_title),
                talkbackHint = stringResource(Res.string.location_detail_action_share_hint),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .fillMaxWidth()
                    .testTag("locationDetailsShare")
            ) {
                onShareLocation(locationDescription)
            }
        }

        // Offline maps
        if (onOfflineMaps != null) {
            IconWithTextButton(
                icon = Icons.Rounded.Download,
                text = stringResource(Res.string.offline_maps_nearby),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = spacing.targetSize)
                    .fillMaxWidth()
                    .testTag("locationDetailsOfflineMaps")
            ) {
                onOfflineMaps(locationDescription)
            }
        }
    }
}
