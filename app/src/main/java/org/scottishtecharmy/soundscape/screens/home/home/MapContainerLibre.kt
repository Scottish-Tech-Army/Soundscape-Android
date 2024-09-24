package org.scottishtecharmy.soundscape.screens.home.home

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.annotations.Marker
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.utils.BitmapUtils
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.R

@Composable
fun MapContainerLibre(
    latitude: Double?,
    longitude: Double?,
    heading: Float,
    modifier: Modifier = Modifier,
    onMapLongClick: (LatLng) -> Unit,
    onMarkerClick: (Marker) -> Boolean,
) {
    SideEffect {
        Log.d("Fanny", "MapContainerLibre composed")
    }
    val res = LocalContext.current.resources
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapLibre.getInstance(
                context,
                BuildConfig.TILE_PROVIDER_API_KEY,
                WellKnownTileServer.MapTiler,
            )
            val mapView = MapView(context)
            mapView.onCreate(null)

            mapView.getMapAsync { map ->
                map.addOnMapLongClickListener { latitudeLongitude ->
                    onMapLongClick(latitudeLongitude)
                    false
                }
                map.setOnMarkerClickListener { marker -> // TODO use something else as deprecated ?
                    onMarkerClick(marker)
                }
            }
            mapView
        },
        update = { mapView ->
            mapView.getMapAsync { map ->
                val apiKey = BuildConfig.TILE_PROVIDER_API_KEY
                val styleUrl =
                    "https://api.maptiler.com/maps/streets-v2/style.json?key=$apiKey" // TODO see is we move that
                map.setStyle(styleUrl) { style ->
                    if (latitude != null && longitude != null) {
                        val drawable =
                            ResourcesCompat.getDrawable(
                                res,
                                R.drawable.icons8_navigation_24,
                                null,
                            )
                        style.addImage("MARKER_NAME", BitmapUtils.getBitmapFromDrawable(drawable)!!)

                        // Create a SymbolManager
                        val symbolManager = SymbolManager(mapView, map, style)
                        // Disable symbol collisions
                        symbolManager.iconAllowOverlap = true
                        symbolManager.iconIgnorePlacement = true

                        // Add a new symbol at specified lat/lon.
                        val symbol =
                            symbolManager.create(
                                SymbolOptions()
                                    .withLatLng(LatLng(latitude, longitude))
                                    .withIconImage("MARKER_NAME") // TODO improve
                                    .withIconSize(1.25f)
                                    .withIconAnchor("bottom"),
                            )
                        symbolManager.update(symbol)

                        // Add a listener to trigger markers clicks.
                        symbolManager.addClickListener {
                            // TODO ?
                            true
                        }
                    }
                }
                map.uiSettings.setAttributionMargins(15, 0, 0, 15)
                // TODO set lat and long here not in VM
                if (latitude != null && longitude != null) {
                    map.cameraPosition =
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
            }
        },
    )
}
