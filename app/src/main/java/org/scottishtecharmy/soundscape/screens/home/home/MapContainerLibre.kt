package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.Marker
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.ramani.compose.awaitMap
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.R

const val USER_POSITION_MARKER_NAME = "USER_POSITION_MARKER_NAME"

@Composable
fun MapContainerLibre(
    map: MapView,
    latitude: Double,
    longitude: Double,
    heading: Float,
    modifier: Modifier = Modifier,
    onMapLongClick: (LatLng) -> Unit,
    onMarkerClick: (Marker) -> Boolean,
) {
    val cameraPosition = remember(latitude, longitude, heading) {
        CameraPosition.Builder()
            .target(
                LatLng(
                    latitude = latitude,
                    longitude = longitude,
                ),
            )
            .zoom(15.0)
            .bearing(heading.toDouble())
            .build()
    }

    val symbolOptions = remember(latitude, longitude, heading) {
        SymbolOptions()
            .withLatLng(LatLng(latitude, longitude))
            .withIconImage(USER_POSITION_MARKER_NAME)
            .withIconSize(1.25f)
            .withIconAnchor("bottom")
    }
    val res = LocalContext.current.resources
    val drawable = remember {
        ResourcesCompat.getDrawable(
            res,
            R.drawable.icons8_navigation_24,
            null,
        )
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(map) {
        val mapLibre = map.awaitMap()
        val apiKey = BuildConfig.TILE_PROVIDER_API_KEY
        val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$apiKey"
        mapLibre.setStyle(styleUrl) { style ->
            drawable?.let { drawable ->
                style.addImage(USER_POSITION_MARKER_NAME, drawable)
                val symbolManager = SymbolManager(map, mapLibre, style)
                // Disable symbol collisions
                symbolManager.iconAllowOverlap = true
                symbolManager.iconIgnorePlacement = true

                // update with a new symbol at specified lat/lng
                val symbol =
                    symbolManager.create(symbolOptions)
                symbolManager.update(symbol)
            }

        }

        mapLibre.cameraPosition = cameraPosition

    }
    AndroidView(
        modifier = modifier,
        factory = {
            coroutineScope.launch {
                val mapLibre = map.awaitMap()
                mapLibre.addOnMapLongClickListener { latitudeLongitude ->
                    onMapLongClick(latitudeLongitude)
                    false
                }
                mapLibre.setOnMarkerClickListener { marker ->
                    onMarkerClick(marker)
                }
            }
            map
                  },
        update = { mapView ->
            coroutineScope.launch {
                val mapLibre = mapView.awaitMap()
                // Move camera to the same place to trigger the zoom update
                mapLibre.cameraPosition = cameraPosition
            }
        },
    )
}
