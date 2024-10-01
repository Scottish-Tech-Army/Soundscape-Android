package org.scottishtecharmy.soundscape.screens.home.locationDetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import org.scottishtecharmy.soundscape.screens.home.home.rememberMapViewWithLifecycle
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.viewmodels.LocationDetailsViewModel

data class LocationDescription(val title : String,
                               val latitude : Double,
                               val longitude : Double)

fun generateLocationDetailsRoute(locationDescription: LocationDescription) : String {
    // Generate JSON for the LocationDescription and append it to the rout
    val gson = GsonBuilder().create()
    val json = gson.toJson(locationDescription)

    return HomeRoutes.LocationDetails.route + "/" + json
}



@Composable
fun LocationDetailsScreen(
                    locationDescription : LocationDescription,
                    latitude : Double?,
                    longitude: Double?,
                    heading : Float,
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
        heading = heading
    )
}

@Composable
fun LocationDetails(
                    locationDescription : LocationDescription,
                    onNavigateUp: () -> Unit,
                    latitude: Double?,
                    longitude: Double?,
                    heading: Float,
                    createBeacon: (latitude: Double, longitude: Double) -> Unit,
                    enableStreetPreview: (latitude: Double, longitude: Double) -> Unit,
                    modifier: Modifier = Modifier) {

    Column(
        modifier = modifier
            .fillMaxHeight(),
    ) {
        CustomAppBar(
            customTitle =  stringResource(R.string.location_detail_title_default),
            onNavigateUp = onNavigateUp,
        )
        Text(
            text = locationDescription.title,
            modifier = Modifier.padding(top = 20.dp, bottom = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.surfaceBright
        )
        Text(
            text = "A DESCRIPTION BASED ON THE TILE DATA!",
            modifier = Modifier.padding(top = 20.dp, bottom = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.surfaceBright
        )

        val mapView = rememberMapViewWithLifecycle()
        MapContainerLibre(
            map = mapView,
            beaconLocation = LatLng(
                locationDescription.latitude,
                locationDescription.longitude
            ),
            onMapLongClick = { },
            onMarkerClick = { false },
            // Center on the beacon
            mapCenter = LatLng(
                locationDescription.latitude,
                locationDescription.longitude
            ),
            userLocation = LatLng(
                latitude ?: 0.0,
                longitude ?: 0.0
            ),

        mapViewRotation = 0.0F,
            userSymbolRotation = heading,
            modifier = modifier.fillMaxWidth().aspectRatio(1.0F),
        )

        Button(
            onClick = {
                createBeacon(locationDescription.latitude, locationDescription.longitude)
            }
        ) {
            Text(
                text = "Create an audio beacon",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            onClick = {
                enableStreetPreview(locationDescription.latitude, locationDescription.longitude)
                onNavigateUp()
            }
        ) {
            Text(
                text = "Enter Street Preview",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
   }
}

@Preview(showBackground = true)
@Composable
fun LocationDetailsPreview() {
    SoundscapeTheme {
        LocationDetails(
            LocationDescription("", 0.0, 0.0),
            createBeacon = { _,_ ->
            },
            enableStreetPreview = { _,_ ->
            },
            onNavigateUp = {},
            latitude = null,
            longitude = null,
            heading = 0.0F
        )
    }
}