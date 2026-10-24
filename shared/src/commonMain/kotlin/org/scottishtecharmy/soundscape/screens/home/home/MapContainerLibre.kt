package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.dsl.textOffset
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.LocationIndicatorDefaults
import org.maplibre.compose.layers.LocationIndicatorLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.MultiPolygon
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.degrees
import org.maplibre.spatialk.units.extensions.meters
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.mapstyle.OfflineExtractTiles
import org.scottishtecharmy.soundscape.platform.nativeMapRenderOptions
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.current_beacon_location_marker
import org.scottishtecharmy.soundscape.resources.location_detail_exit_full_screen_hint
import org.scottishtecharmy.soundscape.resources.location_detail_full_screen_hint
import org.scottishtecharmy.soundscape.resources.location_marker

@Composable
fun FullScreenMapFab(
    fullscreenMap: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    openMapHint: StringResource = Res.string.location_detail_full_screen_hint,
    closeMapHint: StringResource = Res.string.location_detail_exit_full_screen_hint
) {
    FloatingActionButton(
        onClick = { fullscreenMap.value = !fullscreenMap.value },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (fullscreenMap.value) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = if (fullscreenMap.value)
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
 * @param userLocationAccuracy The horizontal accuracy of [userLocation] in metres, drawn as a
 * circle around it, or null for no circle
 * @param userHeadingAccuracy The error either side of [userSymbolRotation] in degrees, drawn as a
 * sector around it, or null for no sector
 * @param beaconLocation An optional location to show a beacon marker
 * @param routeData Optional route data to display route waypoint markers
 * @param modifier Modifier for the map container
 * @param onBeaconLocationEdited If non-null, the beacon marker is pinned to the camera center so
 * that panning the map moves it, and each new location is reported through this callback
 * @param currentBeaconWaypointIndex The waypoint index the beacon marker currently represents
 * (0-based); shown as a 1-based number label on the beacon marker, matching route waypoint markers
 * @param styleUri The URI of the map style to use
 * @param routeMarkerImages Pre-rendered marker images for route waypoints
 * @param onInteractionChanged Callback fired with true while a multi-touch (pinch) gesture is
 * active over the map and false once it ends. Callers embedding the map inside a scrollable
 * container should use this to disable that container's scrolling for the duration - see
 * https://github.com/maplibre/maplibre-compose/issues/726. The underlying native map's pinch
 * gesture detector and Compose's own scroll gesture detection both race for the same touch
 * stream on Android, and without this, MotionEvents get dropped/misrouted between them, making
 * pinch-to-zoom on the map unreliable.
 */
// The Kotlin/Native backend doesn't propagate maplibre-compose's @MaplibreComposable
// applier-target metadata across the klib boundary the way the JVM backend does, so
// this correctly-nested MaplibreMap/FillLayer/LineLayer/SymbolLayer usage spuriously
// trips the applier check under iosArm64/iosSimulatorArm64 only.
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
fun MapContainerLibre(
    mapCenter: LngLatAlt,
    allowScrolling: Boolean,
    userLocation: LngLatAlt?,
    userSymbolRotation: Float,
    beaconLocation: LngLatAlt?,
    routeData: RouteWithMarkers?,
    modifier: Modifier = Modifier,
    onBeaconLocationEdited: ((LngLatAlt) -> Unit)? = null,
    currentBeaconWaypointIndex: Int = 0,
    baseStyle: BaseStyle,
    routeMarkerImages: List<ImageBitmap>? = null,
    extractGeometry: Geometry? = null,
    onInteractionChanged: (Boolean) -> Unit = {},
    userLocationAccuracy: Float? = null,
    userHeadingAccuracy: Float? = null,
) {
    val extractBounds = remember(extractGeometry) { extractGeometry?.computeBounds() }
    val editingBeacon = onBeaconLocationEdited != null
    val currentOnBeaconLocationEdited by rememberUpdatedState(onBeaconLocationEdited)

    val locationMarker = painterResource(Res.drawable.location_marker)
    val currentBeaconLocationMarker = painterResource(Res.drawable.current_beacon_location_marker)
    val primaryColor = MaterialTheme.colorScheme.primary

    // Offline extracts are read through our own tile provider, not MapLibre's PMTiles source,
    // which leaks native memory for every tile it reads. It must be in place before the first map.
    OfflineExtractTiles.install()

    // The map's state owns its native map, and is closed (freeing that map) when it leaves the
    // composition. Sources and layers are declared in its content block.
    val mapState = rememberMapState(
        baseStyle = baseStyle,
        initialCameraPosition = CameraPosition(
            center = Position(mapCenter.longitude, mapCenter.latitude),
            zoom = 15.0,
        ),
    ) {
        if (extractGeometry != null) {
            val extractSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(extractGeometry)
            )
            FillLayer(
                id = "extract-fill",
                source = extractSource,
                color = const(primaryColor),
                opacity = const(0.25f),
                outlineColor = const(primaryColor),
            )
            LineLayer(
                id = "extract-outline",
                source = extractSource,
                color = const(primaryColor),
                width = const(2.dp),
            )
        }
        // Driven by the location and heading the rest of the app is using, rather than
        // maplibre-compose's own providers, which would make their own platform requests and know
        // nothing of the head tracker, GPX replay or a simulated location. It animates between
        // fixes where a symbol layer would jump. It's the same red in every theme, as the arrow it
        // replaced was.
        LocationIndicatorLayer(
            id = "user-location",
            location = userLocation?.let {
                Position(longitude = it.longitude, latitude = it.latitude)
            },
            bearing = Bearing.North + userSymbolRotation.toDouble().degrees,
            accuracyRadius = userLocationAccuracy?.toDouble()?.meters,
            bearingAccuracy = userHeadingAccuracy?.toDouble()?.degrees,
            topImage = LocationIndicatorDefaults.topImage(color = USER_LOCATION_COLOR),
            bearingImage = LocationIndicatorDefaults.bearingImage(color = USER_LOCATION_COLOR),
            bearingAccuracyColor = const(USER_LOCATION_COLOR.copy(alpha = 0.35f)),
            accuracyRadiusColor = USER_LOCATION_COLOR.copy(alpha = 0.15f),
            accuracyRadiusBorderColor = USER_LOCATION_COLOR,
        )

        if (routeData != null) {
            for ((index, waypoint) in routeData.markers.withIndex()) {
                val markerLocationGeoJson = rememberGeoJsonSource(
                    data = GeoJsonData.Features(
                        Point(Position(longitude = waypoint.longitude, latitude = waypoint.latitude))
                    )
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
                        iconImage = image(locationMarker),
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
                        textOffset = textOffset(0f.em, (-1.3f).em),
                    )
                }
            }
        }

        // Draw beacon location marker last to prevent waypoint marker being drawn on top.
        val drawnBeaconLocation = if (editingBeacon) null else beaconLocation
        if (drawnBeaconLocation != null) {
            BeaconMarker(drawnBeaconLocation, currentBeaconLocationMarker, currentBeaconWaypointIndex)
        }
    }

    // When an extract polygon is provided, fit the camera to its bounds.
    // Otherwise re-center on mapCenter when it changes (e.g. when location
    // becomes available on iOS).
    LaunchedEffect(mapCenter.latitude, mapCenter.longitude, extractBounds) {
        if (extractBounds != null) {
            mapState.animateCameraToBounds(
                boundingBox = extractBounds,
                fit = CameraFit(fitPadding = DpPadding(16.dp, 16.dp, 16.dp, 16.dp)),
            )
        } else {
            mapState.animateCamera(
                CameraUpdate(center = Position(mapCenter.longitude, mapCenter.latitude))
            )
        }
    }
    if (editingBeacon) {
        LaunchedEffect(mapState) {
            snapshotFlow { mapState.cameraPosition.center }.collect { center ->
                currentOnBeaconLocationEdited?.invoke(LngLatAlt(center.longitude, center.latitude))
            }
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            // Only take over from the parent scroll on genuine multi-touch (pinch).
            // MapLibre's native gesture detector for pinch/rotate/tilt requires 2+
            // pointers by construction, so a single-finger drag here was never going
            // to pan the map anyway (panning is off unless allowScrolling) -
            // treating it as "interacting" too would just make single-finger swipes
            // starting on the map do nothing instead of scrolling the page.
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val pointerCount = event.changes.count { it.pressed }
                    onInteractionChanged(pointerCount >= 2)
                }
            }
        }
    ) {
        MaplibreMap(
            state = mapState,
            renderOptions = nativeMapRenderOptions(),
            // Zoom (pinch/double-tap) is always on. Panning only when allowScrolling, so that
            // inside a scrollable parent a single-finger drag scrolls the page instead.
            // Rotation and tilt are always off.
            interactions = MapInteractions {
                camera {
                    pan { enabled = allowScrolling }
                    rotate { enabled = false }
                    pitch { enabled = false }
                }
            },
            // No ornaments: no compass, scale bar, zoom buttons or attribution
            overlay = {},
        )
        if (editingBeacon) {
            // While editing, the beacon is pinned to the centre of the map, so panning moves it.
            // It's drawn over the map rather than as a layer so that it doesn't lag the camera,
            // and laid out to match BeaconMarker: the pin's point at the centre, 1.2x its size,
            // with the number in its head.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -BEACON_EDIT_MARKER_SIZE / 2)
                    .size(BEACON_EDIT_MARKER_SIZE),
            ) {
                Icon(
                    painter = currentBeaconLocationMarker,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = "${currentBeaconWaypointIndex + 1}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private val USER_LOCATION_COLOR = Color(0xFFEF0000)

// The marker drawable's 24dp, scaled by BeaconMarker's iconSize of 1.2
private val BEACON_EDIT_MARKER_SIZE = 28.8.dp

@Composable
@MaplibreComposable
private fun BeaconMarker(
    location: LngLatAlt,
    marker: Painter,
    waypointIndex: Int,
) {
    val beaconLocationGeoJson = rememberGeoJsonSource(
        data = GeoJsonData.Features(
            Point(Position(longitude = location.longitude, latitude = location.latitude))
        )
    )
    SymbolLayer(
        id = "beacon-location",
        source = beaconLocationGeoJson,
        iconImage = image(marker),
        iconSize = const(1.2F),
        iconAllowOverlap = const(true),
        iconAnchor = const(SymbolAnchor.Bottom),
        // Default font stack (Open Sans, Arial Unicode MS) is not
        // bundled — must use a Roboto variant that ships with the
        // glyph PBFs in osm-liberty-accessible/fonts/.
        textField = format(span("${waypointIndex + 1}")),
        textFont = const(listOf("Roboto Bold")),
        textColor = const(Color.White),
        textSize = const(11.sp),
        textAllowOverlap = const(true),
        textAnchor = const(SymbolAnchor.Center),
        textOffset = textOffset(0f.em, (-1.3f).em),
    )
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
