package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * Platform-specific map container that provides the correct map style and assets
 * for each platform (Android/iOS).
 */
@Composable
expect fun PlatformMapContainer(
    mapCenter: LngLatAlt,
    allowScrolling: Boolean,
    userLocation: LngLatAlt?,
    userSymbolRotation: Float,
    beaconLocation: LngLatAlt?,
    routeData: RouteWithMarkers?,
    modifier: Modifier,
)
