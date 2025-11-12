package org.scottishtecharmy.soundscape.screens.home.home

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import java.io.File
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.preference.PreferenceManager
import ch.poole.geo.pmtiles.Reader
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap.OnMapLongClickListener
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.android.tile.TileOperation
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Polygon
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.MainActivity.Companion.ACCESSIBLE_MAP_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.ACCESSIBLE_MAP_KEY
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import org.scottishtecharmy.soundscape.geoengine.utils.getXYTile
import org.scottishtecharmy.soundscape.utils.findExtractPaths
import kotlin.io.path.Path
import kotlin.io.path.fileSize


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
        setTextColor(Color.WHITE)
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

@Composable
fun FullScreenMapFab(fullscreenMap: MutableState<Boolean>, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = { fullscreenMap.value = !fullscreenMap.value },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Icon(
            imageVector = if(fullscreenMap.value) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = if(fullscreenMap.value)
                stringResource(R.string.location_detail_exit_full_screen_hint)
            else
                stringResource(R.string.location_detail_full_screen_hint)
        )
    }
}

@Preview
@Composable
fun PreviewFullScreenMapFab(){
    FullScreenMapFab(remember { mutableStateOf(false) }, Modifier)
}

fun updateRouteMarkers(
    sm: SymbolManager,
    annotationList: MutableList<Symbol>,
    routeData: RouteWithMarkers?,
    routeMarkers: MutableState<List<Symbol>?>
) {
    // Remove any previous markers
    routeMarkers.value?.let { markers ->
        for (marker in markers) {
            sm.delete(marker)
        }
        routeMarkers.value = null
    }

    if (routeData != null) {
        val markersList = mutableListOf<Symbol>()
        for ((index, waypoint) in routeData.markers.withIndex()) {
            val markerOptions = SymbolOptions()
                .withLatLng(
                    LatLng(
                        waypoint.latitude,
                        waypoint.longitude
                    )
                )
                .withIconImage(LOCATION_MARKER_NAME.format(index))
                .withIconAnchor("bottom")
                .withIconSize(1.5f)
            val marker = sm.create(markerOptions)
            if (marker != null) {
                annotationList.add(marker)
                markersList.add(marker)
            }
        }
        routeMarkers.value = markersList
    }
}

private fun getSourceUri(appContext: Context, location: LngLatAlt?, forceNetworkSource: Boolean) : String {

    var urlReplacement = "${BuildConfig.TILE_PROVIDER_URL}/$PROTOMAPS_SERVER_PATH.json"
    if(!forceNetworkSource) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        val extractsPath = sharedPreferences.getString(MainActivity.SELECTED_STORAGE_KEY, MainActivity.SELECTED_STORAGE_DEFAULT)!!

        // Get locally downloaded files
        val offlineExtractPaths =  findExtractPaths(extractsPath + "/" + Environment.DIRECTORY_DOWNLOADS)
        if(offlineExtractPaths.isNotEmpty()) {
            if(location == null){
                urlReplacement = "pmtiles://file://${offlineExtractPaths[0]}"
            } else {
                // We have a location and more than one offline extract, so pick the largest one
                // that includes our location
                val tileXY = getXYTile(location, MAX_ZOOM_LEVEL)
                var largestSize = 0L
                for(extract in offlineExtractPaths) {
                    val reader = Reader(File(extract))
                    val fileTile = reader.getTile(MAX_ZOOM_LEVEL, tileXY.first, tileXY.second)
                    reader.close()
                    if(fileTile != null) {
                        // This extract contains our location, check its size
                        val fileSize = Path(extract).fileSize()
                        if(fileSize > largestSize) {
                            urlReplacement = "pmtiles://file://$extract"
                            largestSize = fileSize
                            println("Choosing $extract")
                        } else {
                            println("Rejecting $extract due to smaller size")
                        }
                    } else {
                        println("Rejecting $extract due to no tile")
                    }
                }
            }
        }
    }

    return urlReplacement
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
    userLocation: LngLatAlt?,
    userSymbolRotation: Float,
    beaconLocation: LngLatAlt?,
    routeData: RouteWithMarkers?,
    modifier: Modifier = Modifier,
    editBeaconLocation: Boolean = false,
    onMapLongClick: OnMapLongClickListener,
    showMap: Boolean,
    overlayGeoJson: String = ""
) {
    if(showMap) {
        val context = LocalContext.current
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val accessibleMapEnabled =
            sharedPreferences.getBoolean(ACCESSIBLE_MAP_KEY, ACCESSIBLE_MAP_DEFAULT)

        // Setup map colors. We use the background and onBackground theme colors as the main map
        // colors so that we automatically switch with the theme.
        val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
        val foregroundColor = MaterialTheme.colorScheme.onBackground.toArgb()

        // The other colors are currently the same for both light and dark themes
        val motorwayColor: Int = Color.rgb(128, 128, 255)
        val trunkRoadColor: Int = Color.rgb(255, 200, 30)
        val waterColor = Color.rgb(200, 200, 240)
        val greeneryColor = Color.argb(20, 240, 255, 240)

        // We don't run the map code when in a Preview as it does not render
        if (!LocalInspectionMode.current) {
            val cameraPosition = remember(mapCenter, allowScrolling) {

                // Only recenter the map if scrolling has been disallowed
                val cp = CameraPosition.Builder().bearing(0.0)
                if (!allowScrolling)
                    cp.target(mapCenter.toLatLng())
                cp.build()
            }

            val symbolOptions = remember(userLocation, userSymbolRotation) {
                SymbolOptions()
                    .withLatLng(userLocation?.toLatLng() ?: LatLng())
                    .withIconImage(USER_POSITION_MARKER_NAME)
                    .withIconAnchor("center")
                    .withIconRotate(userSymbolRotation)
                    .withIconSize(1.5f)
            }

            val currentRouteData = remember { mutableStateOf<RouteWithMarkers?>(routeData) }
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
                            for (marker in markers) {
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
                    val styleName =
                        if (accessibleMapEnabled) "style.json" else "originalStyle.json"
                    val styleUrl =
                        Uri.fromFile(File("$filesDir/osm-liberty-accessible/$styleName")).toString()
                    mapLibre.setStyle(styleUrl) { style ->

                        // Dynamically add tile source to our style. We force use of the network
                        // tile server when we're displaying overlays for offline extracts to ensure
                        // that the map is available for the extract region.
                        val tileSource = getSourceUri(context, mapCenter, overlayGeoJson.isNotEmpty())

                        println("tileSource: $tileSource")

                        val vectorSource = VectorSource("openmaptiles", tileSource)
                        if(tileSource.startsWith("pmtiles")) {
                            vectorSource.isVolatile = true
                        }
                        style.addSource(vectorSource)

                        // Add the icons we might need to the style
                        //  - user location
                        //  - numbered location markers
                        style.addImage(USER_POSITION_MARKER_NAME, userPositionDrawable!!)
                        for (i in 0..99) {
                            style.addImage(LOCATION_MARKER_NAME.format(i), markerDrawables[i])
                        }

                        if (accessibleMapEnabled) {
                            val lineToColorMap = mapOf(
                                "aeroway_runway" to foregroundColor,
                                "aeroway_taxiway" to foregroundColor,
                                "tunnel_motorway_link_casing" to foregroundColor,
                                "tunnel_service_track_casing" to foregroundColor,
                                "tunnel_link_casing" to foregroundColor,
                                "tunnel_street_casing" to foregroundColor,
                                "tunnel_secondary_tertiary_casing" to foregroundColor,
                                "tunnel_trunk_primary_casing" to foregroundColor,
                                "tunnel_motorway_casing" to foregroundColor,
                                "tunnel_path_pedestrian" to foregroundColor,
                                "tunnel_motorway_link" to motorwayColor,
                                "tunnel_service_track" to backgroundColor,
                                "tunnel_link" to backgroundColor,
                                "tunnel_minor" to backgroundColor,
                                "tunnel_secondary_tertiary" to trunkRoadColor,
                                "tunnel_trunk_primary" to trunkRoadColor,
                                "tunnel_motorway" to motorwayColor,
                                "tunnel_major_rail" to foregroundColor,
                                "tunnel_major_rail_hatching" to foregroundColor,
                                "tunnel_transit_rail" to foregroundColor,
                                "tunnel_transit_rail_hatching" to foregroundColor,
                                "road_area_pattern" to foregroundColor,
                                "road_motorway_link_casing" to foregroundColor,
                                "road_service_track_casing" to foregroundColor,
                                "road_link_casing" to foregroundColor,
                                "road_minor_casing" to foregroundColor,
                                "road_secondary_tertiary_casing" to foregroundColor,
                                "road_trunk_primary_casing" to foregroundColor,
                                "road_motorway_casing" to foregroundColor,
                                "road_path_pedestrian" to foregroundColor,
                                "road_motorway_link" to motorwayColor,
                                "road_service_track" to backgroundColor,
                                "road_link" to backgroundColor,
                                "road_minor" to backgroundColor,
                                "road_secondary_tertiary" to trunkRoadColor,
                                "road_trunk_primary" to trunkRoadColor,
                                "road_motorway" to motorwayColor,
                                "road_major_rail" to foregroundColor,
                                "road_major_rail_hatching" to foregroundColor,
                                "road_transit_rail" to foregroundColor,
                                "road_transit_rail_hatching" to foregroundColor,
                                "bridge_motorway_link_casing" to foregroundColor,
                                "bridge_service_track_casing" to foregroundColor,
                                "bridge_link_casing" to foregroundColor,
                                "bridge_street_casing" to foregroundColor,
                                "bridge_path_pedestrian_casing" to foregroundColor,
                                "bridge_secondary_tertiary_casing" to foregroundColor,
                                "bridge_trunk_primary_casing" to foregroundColor,
                                "bridge_motorway_casing" to foregroundColor,
                                "bridge_path_pedestrian" to backgroundColor,
                                "bridge_motorway_link" to motorwayColor,
                                "bridge_service_track" to backgroundColor,
                                "bridge_link" to backgroundColor,
                                "bridge_street" to backgroundColor,
                                "bridge_secondary_tertiary" to trunkRoadColor,
                                "bridge_trunk_primary" to trunkRoadColor,
                                "bridge_motorway" to motorwayColor,
                                "bridge_major_rail" to foregroundColor,
                                "bridge_major_rail_hatching" to foregroundColor,
                                "bridge_transit_rail" to foregroundColor,
                                "bridge_transit_rail_hatching" to foregroundColor,
                                "waterway_tunnel" to waterColor,
                                "waterway_river" to waterColor,
                                "waterway_other" to waterColor,
                                "park_outline" to foregroundColor,
                                "boundary_3" to foregroundColor,
                                "boundary_2_z0-4" to foregroundColor,
                                "boundary_2_z5-" to foregroundColor
                            )

                            val layers = style.layers
                            for (layer in layers) {
                                //println("Layer: ${layer.id}")
                                when (layer) {
                                    is BackgroundLayer -> {
                                        layer.setProperties(
                                            PropertyFactory.backgroundColor(backgroundColor)
                                        )
                                    }

                                    is LineLayer -> {
                                        layer.setProperties(
                                            PropertyFactory.lineColor(lineToColorMap[layer.id]!!)
                                        )
                                    }

                                    is FillLayer -> {
                                        when (layer.id) {
                                            "park",
                                            "landcover_wood",
                                            "landcover_grass",
                                            "landuse_cemetery" ->
                                                layer.setProperties(
                                                    PropertyFactory.fillColor(greeneryColor)
                                                )
                                        }
                                    }

                                    is FillExtrusionLayer -> {
                                        if (layer.id == "building-3d") {
                                            layer.setProperties(
                                                PropertyFactory.fillExtrusionColor(foregroundColor)
                                            )
                                        }
                                    }

                                    is SymbolLayer -> {
                                        layer.setProperties(
                                            PropertyFactory.textColor(foregroundColor),
                                            PropertyFactory.textHaloColor(backgroundColor),
                                        )
                                    }

                                    else -> assert(false)
                                }
                            }
                        }

                        val sm = SymbolManager(map, mapLibre, style)
                        // Disable symbol collisions
                        sm.iconAllowOverlap = true
                        sm.iconIgnorePlacement = true

                        val annotationList = mutableListOf<Symbol>()
                        // update with a new symbol at specified lat/lng
                        val sym = sm.create(symbolOptions)
                        symbol.value = sym
                        annotationList.add(sym)

                        if (beaconLocation != null) {
                            val markerOptions = SymbolOptions()
                                .withLatLng(beaconLocation.toLatLng())
                                .withIconImage(LOCATION_MARKER_NAME.format(0))
                                .withIconAnchor("bottom")
                                .withIconSize(1.5f)
                            val beacon = sm.create(markerOptions)
                            beaconLocationMarker.value = beacon
                            annotationList.add(beacon)
                        }

                        updateRouteMarkers(sm, annotationList, routeData, routeMarkers)

                        // Update our remembered state with the symbol manager and symbol
                        sm.update(annotationList)
                        symbolManager.value = sm

                        if(overlayGeoJson.isNotEmpty()) {
                            // Create a GeoJson Source from our feature GeoJSON which
                            // was generated from the GeoEngine tile grid.
                            val tileGeoJson = FeatureCollection.fromJson(overlayGeoJson)
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
                                    PropertyFactory.lineColor("#000000")
                                )
                            style.addLayer(layer)
                        }

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
                        mapLibre.addOnCameraMoveListener {
                            if (editBeaconLocation) {
                                if (beaconLocationMarker.value != null) {
                                    val center =
                                        mapLibre.projection.visibleRegion.latLngBounds.center

                                    // Update our marker location from the center of the screen
                                    beaconLocationMarker.value?.latLng = center
                                    symbolManager.value?.update(beaconLocationMarker.value)

                                    beaconLocation?.latitude = center.latitude
                                    beaconLocation?.longitude = center.longitude
                                }
                            }
                        }
                    }

                    var overlayBounds: LatLngBounds? = null
                    if(overlayGeoJson.isNotEmpty()) {
                        val tileGeoJson = FeatureCollection.fromJson(overlayGeoJson)

                        val boundsBuilder = LatLngBounds.Builder()
                        var pointsFound = 0

                        tileGeoJson.features()?.forEach { feature ->
                            when (val geometry = feature.geometry()) {
                                is Polygon -> {
                                    geometry.coordinates().forEach { ring ->
                                        ring.forEach { point ->
                                            boundsBuilder.include(
                                                LatLng(
                                                    point.coordinates()[1],
                                                    point.coordinates()[0]
                                                )
                                            )
                                            pointsFound++
                                        }
                                    }
                                }

                                is MultiPolygon -> {
                                    geometry.coordinates().forEach { polygon ->
                                        polygon.forEach { ring ->
                                            ring.forEach { point ->
                                                boundsBuilder.include(
                                                    LatLng(
                                                        point.coordinates()[1],
                                                        point.coordinates()[0]
                                                    )
                                                )
                                                pointsFound++
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (pointsFound > 0) {
                            overlayBounds = boundsBuilder.build()
                        }
                    }
                    if(overlayBounds != null) {
                        mapLibre.cameraPosition = mapLibre.getCameraForLatLngBounds(
                            overlayBounds,
                            intArrayOf( 50,50,50,50)
                        )!!
                    } else {
                        mapLibre.cameraPosition = CameraPosition.Builder()
                            .target(mapCenter.toLatLng())
                            .zoom(15.0) // we set the zoom only at init
                            .bearing(0.0)
                            .build()
                    }
                }
            }

            // Check if the route has been updated
            if (routeData != currentRouteData.value) {
                symbolManager.value?.let() { sm ->
                    // And add new ones
                    val annotationList = mutableListOf<Symbol>()
                    updateRouteMarkers(sm, annotationList, routeData, routeMarkers)
                    sm.update(annotationList)
                    currentRouteData.value = routeData
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
                                if (userLocation != null) {
                                    // We have a symbol, so update it
                                    sym.latLng = userLocation.toLatLng()
                                    sym.iconRotate = userSymbolRotation
                                    symbolManager.value?.update(sym)
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}
