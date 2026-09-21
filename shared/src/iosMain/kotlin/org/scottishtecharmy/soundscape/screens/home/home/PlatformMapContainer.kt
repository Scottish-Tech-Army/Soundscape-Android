package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.spatialk.geojson.Geometry
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

@Composable
actual fun PlatformMapContainer(
    mapCenter: LngLatAlt,
    allowScrolling: Boolean,
    userLocation: LngLatAlt?,
    userSymbolRotation: Float,
    beaconLocation: LngLatAlt?,
    routeData: RouteWithMarkers?,
    modifier: Modifier,
    currentBeaconWaypointIndex: Int,
    extractGeometry: Geometry?,
    forceOnlineTiles: Boolean,
    onInteractionChanged: (Boolean) -> Unit,
) {
    IosMapContainerLibre(
        mapCenter = mapCenter,
        allowScrolling = allowScrolling,
        userLocation = userLocation,
        userSymbolRotation = userSymbolRotation,
        beaconLocation = beaconLocation,
        routeData = routeData,
        modifier = modifier,
        currentBeaconWaypointIndex = currentBeaconWaypointIndex,
        extractGeometry = extractGeometry,
        forceOnlineTiles = forceOnlineTiles,
        onInteractionChanged = onInteractionChanged,
    )
}
