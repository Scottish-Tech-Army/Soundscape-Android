package org.scottishtecharmy.soundscape.screens.home.home

import android.util.Log
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
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.locationDetails.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute

@Composable
fun HomeContent(
    latitude: Double?,
    longitude: Double?,
    heading: Float,
    onNavigate: (String) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    onMarkerClick: (Marker) -> Boolean,
    searchBar: @Composable () -> Unit,
    modifier: Modifier = Modifier
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
                            "Barrowland Ballroom",
                            55.8552688,
                            -4.2366753,
                        )
                    onNavigate(generateLocationDetailsRoute(ld))
                },
                text = stringResource(R.string.search_nearby_screen_title),
            )
            // Markers and routes
            NavigationButton(
                onClick = {
                    onNavigate("${HomeRoutes.MarkersAndRoutes.route}/markers")
                    Log.d(
                        "Navigation",
                        "NavController: ${HomeRoutes.MarkersAndRoutes.route}/markers",
                    )
                },
                text = stringResource(R.string.search_view_markers),
            )
            // Current location
            NavigationButton(
                onClick = {
                    if (latitude != null && longitude != null) {
                        val ld =
                            LocationDescription(
                                // TODO handle LocationDescription instantiation in viewmodel ?
                                "Current location",
                                latitude,
                                longitude,
                            )
                        onNavigate(generateLocationDetailsRoute(ld)) // TODO handle at top level the generateLocationDetailsRoute ?
                    }
                },
                text = stringResource(R.string.search_use_current_location),
            )
            if(latitude != null && longitude != null) {
                val mapView = rememberMapViewWithLifecycle()
                MapContainerLibre(
                    map = mapView,
                    latitude = latitude,
                    longitude = longitude,
                    heading = heading,
                    onMapLongClick = onMapLongClick,
                    onMarkerClick = onMarkerClick,
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewHomeContent(){
    HomeContent(
        latitude = null,
        longitude = null,
        heading = 0.0f,
        onNavigate = {},
        onMapLongClick = {},
        onMarkerClick = { true },
        searchBar = {}
    )
}