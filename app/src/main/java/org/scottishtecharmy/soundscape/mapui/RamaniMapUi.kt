package org.scottishtecharmy.soundscape.mapui

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.maplibre.android.geometry.LatLng
import org.ramani.compose.CameraPosition
import org.ramani.compose.MapLibre
import org.ramani.compose.Symbol
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.locationDetails.LocationDescription

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun RamaniMapUiPreview() {
    RamaniMapUi(LocationDescription("Dummy location", 55.9554608,-3.2262405))
}

@Composable
fun RamaniMapUi(locationDescription : LocationDescription) {

    val cameraPosition = rememberSaveable {
        mutableStateOf(
            CameraPosition(
                target = LatLng(locationDescription.latitude, locationDescription.longitude),
                zoom = 15.0
            )
        )
    }

    val apiKey = BuildConfig.TILE_PROVIDER_API_KEY
    val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$apiKey"
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.0F),
        color = MaterialTheme.colorScheme.background
    ) {
        MapLibre(
            modifier = Modifier.fillMaxSize(),
            styleUrl = styleUrl,
            cameraPosition = cameraPosition.value,
        ) {
            Symbol(
                center = LatLng(locationDescription.latitude, locationDescription.longitude),
                color = "red",
                isDraggable = false,
                size = 1.0F,
                imageId = R.drawable.nearby_markers
            )
        }
    }
}
