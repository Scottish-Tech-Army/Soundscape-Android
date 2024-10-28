package org.scottishtecharmy.soundscape.screens.home.home

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.R
import java.io.File

const val USER_POSITION_MARKER_NAME = "USER_POSITION_MARKER_NAME"

/**
 * A map disable component that uses maplibre.
 *
 * @mapCenter The `LatLng` to center around
 * @mapHeading
 * @userLocation The `LatLng` to draw the user location symbol at
 * @userHeading
 * @beaconLocation An optional `LatLng` to show a beacon marker
 * @onMapLongClick Action to take if a long click is made on the map
 * @onMarkerClick Action to take if a beacon marker is clicked on
 */
@Composable
fun MapContainerLibre(
    mapCenter: LatLng,
    allowScrolling: Boolean,
    mapViewRotation: Float,
    userLocation: LatLng,
    userSymbolRotation: Float,
    beaconLocation: LatLng?,
    modifier: Modifier = Modifier,
    onMapLongClick: (LatLng) -> Boolean,
    onMarkerClick: (Marker) -> Boolean,
    tileGridGeoJson: String
) {
    val cameraPosition = remember(mapCenter, mapViewRotation, allowScrolling) {

        // We always use the mapViewRotation, but we only recenter the map if scrolling has
        // been disallowed
        val cp = CameraPosition.Builder().bearing(mapViewRotation.toDouble())
        if(!allowScrolling)
            cp.target(mapCenter)
        cp.build()
    }

    val symbolOptions = remember(userLocation, userSymbolRotation) {
        SymbolOptions()
            .withLatLng(userLocation)
            .withIconImage(USER_POSITION_MARKER_NAME)
            .withIconAnchor("center")
            .withIconRotate(userSymbolRotation)
    }

    val beaconLocationMarker = remember { mutableStateOf<Marker?>(null) }
    val symbol = remember { mutableStateOf<Symbol?>(null) }
    val symbolManager = remember { mutableStateOf<SymbolManager?>(null) }
    val filesDir = LocalContext.current.filesDir.toString()
    var lastTileGridGeoJson by remember { mutableStateOf(tileGridGeoJson)}

    val res = LocalContext.current.resources
    val drawable = remember {
        ResourcesCompat.getDrawable(
            res,
            R.drawable.navigation,
            null
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val map = rememberMapViewWithLifecycle { mapView: MapView ->
        // This code will be run just before the MapView is destroyed to tidy up any map
        // allocations that have been made. I did try replacing the LaunchedEffects below with
        // DisposableEffects and putting the code there, but by the time onDisposal is called it's
        // too late - the view has already been destroyed. Passing it into the helper means that
        // it's called prior to the onDestroy call on mapView. As a result Leak Canary no longer
        // detects any leaks.
        runBlocking {
            mapView.getMapAsync { map ->
                Log.d("MapContainer", "MapView is being disposed, tidy up")
                beaconLocationMarker.value?.let { currentBeacon ->
                    map.removeMarker(currentBeacon)
                    beaconLocationMarker.value = null
                }
                map.removeOnMapLongClickListener(onMapLongClick)
                map.setOnMarkerClickListener(null)

                symbol.value?.let { sym ->
                    symbolManager.value?.delete(sym)
                    symbol.value = null
                }
                symbolManager.value?.onDestroy()
                symbolManager.value = null
            }
        }
    }

    LaunchedEffect(map) {
        // init map first time it is displayed
        map.getMapAsync { mapLibre ->
            // val apiKey = BuildConfig.TILE_PROVIDER_API_KEY
            val styleUrl = Uri.fromFile(File("$filesDir/osm-bright-gl-style/style.json")).toString()
            mapLibre.setStyle(styleUrl) { style ->
                style.addImage(USER_POSITION_MARKER_NAME, drawable!!)

                val sm = SymbolManager(map, mapLibre, style)
                // Disable symbol collisions
                sm.iconAllowOverlap = true
                sm.iconIgnorePlacement = true

                // update with a new symbol at specified lat/lng
                val sym = sm.create(symbolOptions)
                sm.update(sym)

                // Update our remembered state with the symbol manager and symbol
                symbolManager.value = sm
                symbol.value = sym

                mapLibre.uiSettings.setAttributionMargins(15, 0, 0, 15)
                mapLibre.uiSettings.isZoomGesturesEnabled = true
                // The map rotation is set by the compass heading, so we disable it from the UI
                mapLibre.uiSettings.isRotateGesturesEnabled = false
                // When allowScrolling is false, the centering of the map is set by the location
                // provider, and in that case we disable scrolling from the UI
                mapLibre.uiSettings.isScrollGesturesEnabled = allowScrolling

                // Disable the mapLibre logo as there's not enough screen estate for it
                mapLibre.uiSettings.isLogoEnabled = false
                // Enable the attribution so that we can still get to see who provided the maps
                mapLibre.uiSettings.isAttributionEnabled = true

                mapLibre.addOnMapLongClickListener(onMapLongClick)
                mapLibre.setOnMarkerClickListener(onMarkerClick)
            }

            mapLibre.cameraPosition = CameraPosition.Builder()
                .target(mapCenter)
                .zoom(15.0) // we set the zoom only at init
                .bearing(mapViewRotation.toDouble())
                .build()
        }
    }

    LaunchedEffect(beaconLocation) {
        map.getMapAsync { mapLibre ->
            if (beaconLocation != null && beaconLocationMarker.value == null) {
                // first time beacon is created
                val markerOptions = MarkerOptions()
                    .position(beaconLocation)
                beaconLocationMarker.value = mapLibre.addMarker(markerOptions)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { map },
        update = { mapView ->
            coroutineScope.launch {
                mapView.getMapAsync { mapLibre ->

                    mapLibre.cameraPosition = cameraPosition
                    symbol.value?.let { sym ->
                        // We have a symbol, so update it
                        sym.latLng = userLocation
                        sym.iconRotate = userSymbolRotation
                        symbolManager.value?.update(sym)
                    }

                    if (beaconLocation != null) {
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

                    if (BuildConfig.DEBUG && tileGridGeoJson.isNotEmpty()) {
                        mapLibre.style?.let { style ->
                            // Add the source to the style if it doesn't already exist, or if the
                            // GeoJSON for it has changed
                            if((style.getLayer("current-grid") == null) ||
                               (tileGridGeoJson != lastTileGridGeoJson)) {
                                Log.d("MapContainerLibre", "Redraw grid for new GeoJSON")
                                // Remove any previously adder layer and source
                                style.removeLayer("current-grid")
                                style.removeSource("current-grid")

                                // Create a GeoJson Source from our feature GeoJSON which
                                // was generated from the GeoEngine tile grid.
                                val tileGeoJson = FeatureCollection.fromJson(tileGridGeoJson)
                                val geojsonSource = GeoJsonSource("current-grid", tileGeoJson)
                                // Add our new source
                                style.addSource(geojsonSource)
                                // And our new layer
                                val layer = LineLayer("current-grid", "current-grid")
                                    .withProperties(
                                        PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
                                        PropertyFactory.lineJoin(Property.LINE_JOIN_MITER),
                                        PropertyFactory.lineOpacity(0.7f),
                                        PropertyFactory.lineWidth(4f),
                                        PropertyFactory.lineColor("#0094ff")
                                    )
                                style.addLayer(layer)
                                lastTileGridGeoJson = tileGridGeoJson
                            }
                        }
                    }
                }
            }
        },
    )
}
