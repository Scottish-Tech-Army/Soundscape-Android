package org.scottishtecharmy.soundscape.screens.home.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import java.io.File
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import org.maplibre.android.maps.MapLibreMap.OnMapLongClickListener


const val USER_POSITION_MARKER_NAME = "USER_POSITION_MARKER_NAME"
const val LOCATION_MARKER_NAME = "LOCATION-%d"

/**
 * Create a location marker drawable which has location_marker as it's background, and an integer
 * in the foreground. These are to mark on the map locations of waypoints within a route.
 * @param context The context to use
 * @param number The number to display within the drawable
 * @return A composited drawable
 */
fun createLocationMarkerDrawable(context: Context, number: Int): Drawable {
    // Create a FrameLayout to hold the marker components
    val frameLayout = FrameLayout(context)
    frameLayout.layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )

    val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.location_marker)
    backgroundDrawable?.let {
        val imageView = ImageView(context)
        imageView.setImageDrawable(it)
        frameLayout.addView(imageView)
    }

    // Create the TextView for the number
    val numberTextView = TextView(context)
    numberTextView.apply {
        text = "$number"
        setTextColor(android.graphics.Color.WHITE)
        textSize = 11f
        gravity = Gravity.CENTER
    }

    // Add the TextView to the FrameLayout
    frameLayout.addView(numberTextView)

    // Measure and layout the FrameLayout
    frameLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    frameLayout.layout(0, 0, frameLayout.measuredWidth, frameLayout.measuredHeight)

    // Create a Bitmap from the FrameLayout
    val bitmap = createBitmap(frameLayout.measuredWidth, frameLayout.measuredHeight)
    val canvas = android.graphics.Canvas(bitmap)
    frameLayout.draw(canvas)

    // Create a Drawable from the Bitmap
    return bitmap.toDrawable(context.resources)
}

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
    mapCenter: LngLatAlt,
    allowScrolling: Boolean,
    mapViewRotation: Float,
    userLocation: LngLatAlt?,
    userSymbolRotation: Float,
    beaconLocation: LngLatAlt?,
    routeData: RouteData?,
    modifier: Modifier = Modifier,
    onMapLongClick: OnMapLongClickListener,
) {
    val context = LocalContext.current

    // We don't run the map code when in a Preview as it does not render
    if(!LocalInspectionMode.current) {
        val cameraPosition = remember(mapCenter, mapViewRotation, allowScrolling) {

            // We always use the mapViewRotation, but we only recenter the map if scrolling has
            // been disallowed
            val cp = CameraPosition.Builder().bearing(mapViewRotation.toDouble())
            if(!allowScrolling)
                cp.target(mapCenter.toLatLng())
            cp.build()
        }

        val symbolOptions = remember(userLocation, userSymbolRotation) {
            SymbolOptions()
                .withLatLng(userLocation?.toLatLng() ?: LatLng())
                .withIconImage(USER_POSITION_MARKER_NAME)
                .withIconAnchor("center")
                .withIconRotate(userSymbolRotation)
        }

        val routeMarkers = remember { mutableStateOf<List<Symbol>?>(null) }
        val beaconLocationMarker = remember { mutableStateOf<Symbol?>(null) }
        val symbol = remember { mutableStateOf<Symbol?>(null) }
        val symbolManager = remember { mutableStateOf<SymbolManager?>(null) }
        val filesDir = context.filesDir.toString()

        val res = context.resources
        val userPositionDrawable = remember {
            ResourcesCompat.getDrawable(
                res,
                R.drawable.navigation,
                null
            )
        }
        val markerDrawables = remember {
            Array(100) { index ->
                createLocationMarkerDrawable(context, index + 1)
            }
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
                        symbolManager.value?.delete(currentBeacon)
                        beaconLocationMarker.value = null
                    }

                    routeMarkers.value?.let { markers ->
                        for(marker in markers) {
                            symbolManager.value?.delete(marker)
                        }
                        routeMarkers.value = null
                    }
                    map.removeOnMapLongClickListener(onMapLongClick)

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
                val styleUrl = Uri.fromFile(File("$filesDir/osm-liberty-accessible/processedStyle.json")).toString()
                mapLibre.setStyle(styleUrl) { style ->

                    // Add the icons we might need to the style
                    //  - user location
                    //  - numbered location markers
                    style.addImage(USER_POSITION_MARKER_NAME, userPositionDrawable!!)
                    for(i in 0 .. 99) {
                        style.addImage(LOCATION_MARKER_NAME.format(i), markerDrawables[i])
                    }

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
                    // Disable tilting of the map - keep it 2D
                    mapLibre.uiSettings.isTiltGesturesEnabled = false

                    // Disable the mapLibre logo as there's not enough screen estate for it
                    mapLibre.uiSettings.isLogoEnabled = false
                    // Disable attribution as it is awkward for Talkback. In its place we provide
                    // full attribution on app startup as per the OpenStreetMap guidelines:
                    // https://osmfoundation.org/wiki/Licence/Attribution_Guidelines#Interactive_maps
                    mapLibre.uiSettings.isAttributionEnabled = false

                    mapLibre.addOnMapLongClickListener(onMapLongClick)
                }

                mapLibre.cameraPosition = CameraPosition.Builder()
                    .target(mapCenter.toLatLng())
                    .zoom(15.0) // we set the zoom only at init
                    .bearing(mapViewRotation.toDouble())
                    .build()
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
                            if(userLocation != null) {
                                // We have a symbol, so update it
                                sym.latLng = userLocation.toLatLng()
                                sym.iconRotate = userSymbolRotation
                                symbolManager.value?.update(sym)
                            }
                        }

                        if((symbolManager.value != null) and
                           (beaconLocation != null) and
                           ( beaconLocationMarker.value == null)) {
                            val markerOptions = SymbolOptions()
                                .withLatLng(beaconLocation!!.toLatLng())
                                .withIconImage(LOCATION_MARKER_NAME.format(0))
                                .withIconAnchor("bottom")
                            val sym = symbolManager.value?.create(markerOptions)
                            symbolManager.value?.update(sym)
                            beaconLocationMarker.value = sym
                        }

                        if((symbolManager.value != null) and
                            (routeData != null) and
                            ( routeMarkers.value == null)) {

                            if(routeData?.waypoints != null) {
                                val markersList = emptyList<Symbol>().toMutableList()
                                for ((index, waypoint) in routeData.waypoints.withIndex()) {
                                    val markerOptions = SymbolOptions()
                                        .withLatLng(waypoint.location!!.location().toLatLng())
                                        .withIconImage(LOCATION_MARKER_NAME.format(index))
                                        .withIconAnchor("bottom")
                                    val sym = symbolManager.value?.create(markerOptions)
                                    if(sym != null) {
                                        symbolManager.value?.update(sym)
                                        markersList.add(sym)
                                    }
                                }
                                routeMarkers.value = markersList
                            }
                        }
                    }
                }
            },
        )
    }
}
