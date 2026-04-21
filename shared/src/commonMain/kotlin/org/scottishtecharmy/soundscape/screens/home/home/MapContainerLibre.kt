package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.Point
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
    routeMarkerImages: List<ImageBitmap>? = null
) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(mapCenter.longitude, mapCenter.latitude),
            zoom = 15.0,
        )
    )
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

            if (routeData != null && routeMarkerImages != null) {
                for ((index, waypoint) in routeData.markers.withIndex()) {
                    if (index >= routeMarkerImages.size) break
                    val position = Position(
                        latitude = waypoint.latitude,
                        longitude = waypoint.longitude
                    )
                    val markerLocationGeoJson =
                        rememberGeoJsonSource(
                            data = GeoJsonData.Features(Point(position))
                        )

                    SymbolLayer(
                        id = "marker-$index",
                        source = markerLocationGeoJson,
                        iconImage = image(routeMarkerImages[index]),
                        iconSize = const(1.2F),
                        iconAllowOverlap = const(true),
                        iconAnchor = const(SymbolAnchor.Bottom)
                    )
                }
            }
        }
    }
}
