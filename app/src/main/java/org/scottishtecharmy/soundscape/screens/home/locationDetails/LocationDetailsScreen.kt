package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.GsonBuilder
import org.maplibre.android.geometry.LatLng
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.home.MapContainerLibre
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.Foreground2
import org.scottishtecharmy.soundscape.ui.theme.IntroPrimary
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.ui.theme.surfaceBright
import org.scottishtecharmy.soundscape.viewmodels.LocationDetailsViewModel

data class LocationDescription(
    val name: String? = null,
    val streetNumberAndName: String? = null,
    val postcodeAndLocality: String? = null,
    val country: String? = null,
    val distance: String? = null,
    val latitude: Double,
    val longitude: Double,
)

fun generateLocationDetailsRoute(locationDescription: LocationDescription): String {
    // Generate JSON for the LocationDescription and append it to the rout
    val json = GsonBuilder().create().toJson(locationDescription)

    return HomeRoutes.LocationDetails.route + "/" + json
}

@Composable
fun LocationDetailsScreen(
    locationDescription: LocationDescription,
    latitude: Double?,
    longitude: Double?,
    heading: Float,
    onNavigateUp: () -> Unit,
    viewModel: LocationDetailsViewModel = hiltViewModel(),
) {
    LocationDetails(
        onNavigateUp = onNavigateUp,
        locationDescription = locationDescription,
        createBeacon = { lat, lng ->
            viewModel.createBeacon(lat, lng)
        },
        enableStreetPreview = { lat, lng ->
            viewModel.enableStreetPreview(lat, lng)
        },
        latitude = latitude,
        longitude = longitude,
        heading = heading,
    )
}

@Composable
fun LocationDetails(
    locationDescription: LocationDescription,
    onNavigateUp: () -> Unit,
    latitude: Double?,
    longitude: Double?,
    heading: Float,
    createBeacon: (latitude: Double, longitude: Double) -> Unit,
    enableStreetPreview: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
    ) {
        CustomAppBar(
            title = stringResource(R.string.location_detail_title_default),
            onNavigateUp = onNavigateUp,
        )
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 15.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LocationDescriptionTextsSection(locationDescription = locationDescription)
            HorizontalDivider()
            LocationDescriptionButtonsSection(
                createBeacon = createBeacon,
                locationDescription = locationDescription,
                enableStreetPreview = enableStreetPreview,
                onNavigateUp = onNavigateUp,
            )
        }

        MapContainerLibre(
            beaconLocation =
                LatLng(
                    locationDescription.latitude,
                    locationDescription.longitude,
                ),
            allowScrolling = true,
            onMapLongClick = { false },
            onMarkerClick = { false },
            // Center on the beacon
            mapCenter =
                LatLng(
                    locationDescription.latitude,
                    locationDescription.longitude,
                ),
            userLocation =
                LatLng(
                    latitude ?: 0.0,
                    longitude ?: 0.0,
                ),
            mapViewRotation = 0.0F,
            userSymbolRotation = heading,
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1F),
            tileGridGeoJson = "",
        )
    }
}

@Composable
private fun LocationDescriptionButtonsSection(
    createBeacon: (latitude: Double, longitude: Double) -> Unit,
    locationDescription: LocationDescription,
    enableStreetPreview: (latitude: Double, longitude: Double) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconWithTextButton(
            icon = Icons.Filled.LocationOn,
            text = stringResource(R.string.create_an_audio_beacon),
        ) {
            createBeacon(locationDescription.latitude, locationDescription.longitude)
        }

        IconWithTextButton(
            icon = Icons.Filled.Navigation,
            text = stringResource(R.string.enter_street_preview),
        ) {
            enableStreetPreview(
                locationDescription.latitude,
                locationDescription.longitude,
            )
            onNavigateUp()
        }
    }
}

@Composable
private fun LocationDescriptionTextsSection(locationDescription: LocationDescription) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        locationDescription.name?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        locationDescription.distance?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = null,
                    tint = Foreground2,
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Foreground2,
                )
            }
        }
        listOfNotNull(
            locationDescription.streetNumberAndName,
            locationDescription.postcodeAndLocality,
            locationDescription.country,
        ).joinToString("\n").let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = surfaceBright,
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = surfaceBright,
                )
            }
        }
    }
}

@Composable
fun IconWithTextButton(
    icon: ImageVector,
    text: String,
    action: () -> (Unit),
) {
    TextButton(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
        onClick = {
            action()
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, // Aligns icon and text vertically
            modifier = Modifier.fillMaxWidth(), // Ensures the content inside aligns properly
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IntroPrimary,
            )
            Spacer(modifier = Modifier.width(15.dp)) // Space between icon and text
            Text(
                text = text,
                textAlign = TextAlign.Start, // Aligns text to start within the Row
                color = IntroPrimary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IconsWithTextActionsPreview() {
    IconWithTextButton(
        icon = Icons.Filled.LocationOn,
        text = "Start Audio Beacon",
        action = {},
    )
}

@Preview(showBackground = true)
@Composable
fun LocationDetailsPreview() {
    SoundscapeTheme {
        LocationDetails(
            LocationDescription(
                name = "Pizza hut",
                distance = "3,5 km",
                latitude = 0.0,
                longitude = 0.0,
                streetNumberAndName = "139 boulevard gambetta",
                postcodeAndLocality = "59000 Lille",
                country = "France",
            ),
            createBeacon = { _, _ ->
            },
            enableStreetPreview = { _, _ ->
            },
            onNavigateUp = {},
            latitude = null,
            longitude = null,
            heading = 0.0F,
        )
    }
}
