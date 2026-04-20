package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.location_detail_action_beacon
import org.scottishtecharmy.soundscape.resources.location_detail_action_beacon_hint
import org.scottishtecharmy.soundscape.resources.location_detail_title_default
import org.scottishtecharmy.soundscape.resources.universal_links_alert_action_marker
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * Shared LocationDetails screen for KMP.
 * Simplified version of the Android LocationDetailsScreen — no map, no markdown.
 */
@Composable
fun SharedLocationDetailsScreen(
    locationDescription: LocationDescription,
    userLocation: LngLatAlt?,
    onNavigateUp: () -> Unit,
    onStartBeacon: (LngLatAlt, String) -> Unit,
    onSaveMarker: ((LocationDescription) -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            CustomAppBar(
                title = stringResource(Res.string.location_detail_title_default),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            // Location info section
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

                // Distance and direction
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

            HorizontalDivider()

            // Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.none),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium),
            ) {
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

                if (onSaveMarker != null && locationDescription.databaseId == 0L) {
                    IconWithTextButton(
                        icon = Icons.Filled.AddLocation,
                        text = stringResource(Res.string.universal_links_alert_action_marker),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .defaultMinSize(minHeight = spacing.targetSize)
                            .fillMaxWidth()
                            .testTag("locationDetailsSaveAsMarker")
                    ) {
                        onSaveMarker(locationDescription)
                    }
                }
            }
        }
    }
}
