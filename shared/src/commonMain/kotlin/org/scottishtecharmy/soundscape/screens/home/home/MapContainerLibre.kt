package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.*

@Composable
fun FullScreenMapFab(
    fullscreenMap: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    openMapHint: StringResource = Res.string.location_detail_full_screen_hint,
    closeMapHint: StringResource = Res.string.location_detail_exit_full_screen_hint) {
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
                stringResource(closeMapHint)
            else
                stringResource(openMapHint)
        )
    }
}

/**
 * A map component that uses maplibre-compose.
 *
 * @param mapCenter The location to center the map around
 * @param allowScrolling Whether the user can scroll/pan the map
 * @param userLocation The location to draw the user location symbol at
 * @param userSymbolRotation The rotation of the user location symbol
 * @param beaconLocation An optional location to show a beacon marker
 * @param routeData Optional route data to display route waypoint markers
 * @param modifier Modifier for the map container
 * @param editBeaconLocation If true, the beacon location tracks the camera center
 * @param onMapLongClick Callback when the map is long-pressed, receives the location
 * @param styleUri The URI of the map style to use
 * @param routeMarkerImages Pre-rendered marker images for route waypoints
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
    onMapLongClick: ((LngLatAlt) -> Boolean)? = null,
    baseStyle: BaseStyle,
    routeMarkerImages: List<ImageBitmap>? = null,
    extractGeometry: Geometry? = null,
) {
    val extractBounds = remember(extractGeometry) { extractGeometry?.computeBounds() }
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(mapCenter.longitude, mapCenter.latitude),
            zoom = 15.0,
        )
    )
    // When an extract polygon is provided, fit the camera to its bounds.
    // Otherwise re-center on mapCenter when it changes (e.g. when location
    // becomes available on iOS).
    LaunchedEffect(mapCenter.latitude, mapCenter.longitude, extractBounds) {
        if (extractBounds != null) {
            cameraState.animateTo(
                boundingBox = extractBounds,
                padding = PaddingValues(16.dp),
            )
        } else {
            cameraState.animateTo(
                CameraPosition(
                    target = Position(mapCenter.longitude, mapCenter.latitude),
                    zoom = cameraState.position.zoom,
                )
            )
        }
    }
    val styleState = rememberStyleState()

    if(editBeaconLocation && (beaconLocation != null)) {
        beaconLocation.longitude = cameraState.position.target.longitude
        beaconLocation.latitude = cameraState.position.target.latitude
    }

    Box(modifier = modifier) {
        MaplibreMap(
            baseStyle = baseStyle,
            cameraState = cameraState,
            styleState = styleState,
            options = MapOptions(
                gestureOptions = GestureOptions(
                    isScrollEnabled = allowScrolling,
                    isTiltEnabled = false,
                    isRotateEnabled = false
                ),
                ornamentOptions = OrnamentOptions(
                    isLogoEnabled = false,
                    isAttributionEnabled = true,
                    attributionAlignment = Alignment.BottomEnd,
                    isCompassEnabled = true,
                    compassAlignment = Alignment.TopEnd,
                    isScaleBarEnabled = true,
                    scaleBarAlignment = Alignment.BottomEnd
                )
            )
        ) {
            if (extractGeometry != null) {
                val extractSource = rememberGeoJsonSource(
                    data = GeoJsonData.Features(extractGeometry)
                )
                FillLayer(
                    id = "extract-fill",
                    source = extractSource,
                    color = const(MaterialTheme.colorScheme.primary),
                    opacity = const(0.25f),
                    outlineColor = const(MaterialTheme.colorScheme.primary),
                )
                LineLayer(
                    id = "extract-outline",
                    source = extractSource,
                    color = const(MaterialTheme.colorScheme.primary),
                    width = const(2.dp),
                )
            }
            if (userLocation != null) {
                val marker = painterResource(Res.drawable.navigation)

                val position = Position(
                    latitude = userLocation.latitude,
                    longitude = userLocation.longitude
                )
                val userLocationGeoJson =
                    rememberGeoJsonSource(
                        data = GeoJsonData.Features(Point(position))
                    )

                SymbolLayer(
                    id = "user-location",
                    source = userLocationGeoJson,
                    iconImage = image(marker),
                    iconSize = const(1.2F),
                    iconRotate = const(userSymbolRotation),
                    iconAllowOverlap = const(true)
                )
            }
            if (beaconLocation != null) {
                val marker = painterResource(Res.drawable.location_marker)

                val position = Position(
                    latitude = beaconLocation.latitude,
                    longitude = beaconLocation.longitude
                )
                val beaconLocationGeoJson =
                    rememberGeoJsonSource(
                        data = GeoJsonData.Features(Point(position))
                    )

                SymbolLayer(
                    id = "beacon-location",
                    source = beaconLocationGeoJson,
                    iconImage = image(marker),
                    iconSize = const(1.2F),
                    iconAllowOverlap = const(true),
                    iconAnchor = const(SymbolAnchor.Bottom)
                )
            }

            if (routeData != null) {
                val waypointMarker = painterResource(Res.drawable.location_marker)
                for ((index, waypoint) in routeData.markers.withIndex()) {
                    val position = Position(
                        latitude = waypoint.latitude,
                        longitude = waypoint.longitude
                    )
                    val markerLocationGeoJson =
                        rememberGeoJsonSource(
                            data = GeoJsonData.Features(Point(position))
                        )

                    val preRendered = routeMarkerImages?.getOrNull(index)
                    if (preRendered != null) {
                        SymbolLayer(
                            id = "marker-$index",
                            source = markerLocationGeoJson,
                            iconImage = image(preRendered),
                            iconSize = const(1.2F),
                            iconAllowOverlap = const(true),
                            iconAnchor = const(SymbolAnchor.Bottom)
                        )
                    } else {
                        SymbolLayer(
                            id = "marker-$index",
                            source = markerLocationGeoJson,
                            iconImage = image(waypointMarker),
                            iconSize = const(1.2F),
                            iconAllowOverlap = const(true),
                            iconAnchor = const(SymbolAnchor.Bottom),
                            // Default font stack (Open Sans, Arial Unicode MS) is not
                            // bundled — must use a Roboto variant that ships with the
                            // glyph PBFs in osm-liberty-accessible/fonts/.
                            textField = format(span("${index + 1}")),
                            textFont = const(listOf("Roboto Bold")),
                            textColor = const(Color.White),
                            textSize = const(11.sp),
                            textAllowOverlap = const(true),
                            textAnchor = const(SymbolAnchor.Center),
                            textOffset = offset(0f.em, (-1.3f).em),
                        )
                    }
                }
            }
        }
    }
}

private fun Geometry.computeBounds(): BoundingBox? {
    val positions: List<Position> = when (this) {
        is Polygon -> coordinates.flatten()
        is MultiPolygon -> coordinates.flatten().flatten()
        else -> return null
    }
    if (positions.isEmpty()) return null
    var west = Double.POSITIVE_INFINITY
    var east = Double.NEGATIVE_INFINITY
    var south = Double.POSITIVE_INFINITY
    var north = Double.NEGATIVE_INFINITY
    for (p in positions) {
        if (p.longitude < west) west = p.longitude
        if (p.longitude > east) east = p.longitude
        if (p.latitude < south) south = p.latitude
        if (p.latitude > north) north = p.latitude
    }
    return BoundingBox(west, south, east, north)
}
