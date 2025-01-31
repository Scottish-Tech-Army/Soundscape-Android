package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.maplibre.android.annotations.Marker
import org.maplibre.android.geometry.LatLng
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.NavigationButton
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute

@Composable
fun HomeContent(
    location: LngLatAlt?,
    beaconLocation: LngLatAlt?,
    heading: Float,
    onNavigate: (String) -> Unit,
    onMapLongClick: (LatLng) -> Boolean,
    onMarkerClick: (Marker) -> Boolean,
    getCurrentLocationDescription: () -> LocationDescription,
    searchBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    tileGridGeoJson: String,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        searchBar()

        Column(
            verticalArrangement = Arrangement.spacedBy((1).dp),
        ) {
            // Places Nearby
            NavigationButton(
                onClick = {
                    // This isn't the final code, just an example of how the LocationDetails could work
                    // The LocationDetails screen takes some JSON as an argument which tells the
                    // screen which location to provide details of. The JSON is appended to the route.
                    val ld =
                        LocationDescription(
                            addressName = "Barrowland Ballroom",
                            location = LngLatAlt(-4.2366753, 55.8552688)
                        )
                    onNavigate(generateLocationDetailsRoute(ld))
                },
                text = stringResource(R.string.search_nearby_screen_title),
            )
            // Markers and routes
            NavigationButton(
                onClick = {
                    onNavigate(HomeRoutes.MarkersAndRoutes.route)
                },
                text = stringResource(R.string.search_view_markers),
            )
            // Current location
            NavigationButton(
                onClick = {
                    if (location != null) {
                        val ld = getCurrentLocationDescription()
                        onNavigate(generateLocationDetailsRoute(ld))
                    }
                },
                text = stringResource(R.string.search_use_current_location),
            )
            if (location != null) {
                MapContainerLibre(
                    beaconLocation = beaconLocation,
                    mapCenter = location,
                    allowScrolling = false,
                    mapViewRotation = 0.0F,
                    userLocation = location,
                    userSymbolRotation = heading,
                    onMapLongClick = onMapLongClick,
                    onMarkerClick = onMarkerClick,
                    tileGridGeoJson = tileGridGeoJson,
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewHomeContent() {
    HomeContent(
        location = null,
        beaconLocation = null,
        heading = 0.0f,
        onNavigate = {},
        onMapLongClick = { false },
        onMarkerClick = { true },
        searchBar = {},
        tileGridGeoJson = "",
        getCurrentLocationDescription = { LocationDescription() }
    )
}
