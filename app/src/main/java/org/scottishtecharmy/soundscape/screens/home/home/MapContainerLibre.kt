package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
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
    beaconLocation: LatLng?,
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
            .bearing(heading.toDouble())
            .build()
    }

    val symbolOptions = remember(latitude, longitude, heading) {
        SymbolOptions()
            .withLatLng(LatLng(latitude, longitude))
            .withIconImage(USER_POSITION_MARKER_NAME)
            .withIconAnchor("center")
    }

    val beaconLocationMarker = remember { mutableStateOf<Marker?>(null) }

    val res = LocalContext.current.resources
    val drawable = remember {
        ResourcesCompat.getDrawable(
            res,
            R.drawable.navigation,
            null
        )
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(map) {
        // init map first time it is displayed
        val mapLibre = map.awaitMap()
        val apiKey = BuildConfig.TILE_PROVIDER_API_KEY
        val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$apiKey"
        mapLibre.setStyle(styleUrl) { style ->
            style.addImage(USER_POSITION_MARKER_NAME, drawable!!)
            val symbolManager = SymbolManager(map, mapLibre, style)
            // Disable symbol collisions
            symbolManager.iconAllowOverlap = true
            symbolManager.iconIgnorePlacement = true

            // update with a new symbol at specified lat/lng
            val symbol = symbolManager.create(symbolOptions)
            symbolManager.update(symbol)

            mapLibre.uiSettings.setAttributionMargins(15, 0, 0, 15)
            mapLibre.uiSettings.isZoomGesturesEnabled = true
            // The map rotation is set by the compass heading, so we disable it from the UI
            mapLibre.uiSettings.isRotateGesturesEnabled = false

            // The phone is always at the center of the map, so listen to the camera position
            // for redrawing the phone location.
            mapLibre.addOnCameraMoveListener {
                //get the camera position and use it to set the symbol location
                symbol.setLatLng(mapLibre.cameraPosition.target)
                symbolManager.update(symbol)
            }
            mapLibre.addOnMapLongClickListener { latitudeLongitude ->
                onMapLongClick(latitudeLongitude)
                false
            }
            mapLibre.setOnMarkerClickListener { marker ->
                onMarkerClick(marker)
            }
        }

        mapLibre.cameraPosition = CameraPosition.Builder()
            .target(
                LatLng(
                    latitude = latitude,
                    longitude = longitude,
                ),
            )
            .zoom(15.0) // we set the zoom only at init
            .bearing(heading.toDouble())
            .build()

    }

    LaunchedEffect(beaconLocation) {
        val mapLibre = map.awaitMap()
        if(beaconLocation != null && beaconLocationMarker.value == null){
            // first time beacon is created
            val markerOptions = MarkerOptions()
                .position(beaconLocation)
            beaconLocationMarker.value = mapLibre.addMarker(markerOptions)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { map },
        update = { mapView ->
            coroutineScope.launch {
                val mapLibre = mapView.awaitMap()
                mapLibre.cameraPosition = cameraPosition
                if(beaconLocation != null) {
                    // beacon to display
                    beaconLocationMarker.value?.let { currentBeaconMarker ->
                        // update beacon position
                        currentBeaconMarker.position = beaconLocation
                        mapLibre.updateMarker(currentBeaconMarker)
                    } ?: {
                        // new beacon to display
                        val markerOptions =
                            MarkerOptions()
                                .position(beaconLocation)
                        beaconLocationMarker.value = mapLibre.addMarker(markerOptions)
                    }
                } else {
                    // if beacon is present we should remove it
                    beaconLocationMarker.value?.let { currentBeacon ->
                        mapLibre.removeMarker(currentBeacon)
                        beaconLocationMarker.value = null
                    }
                }
            }
        },
    )
}
