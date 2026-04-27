package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import org.maplibre.spatialk.geojson.Geometry
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.mapstyle.AccessibleTheme
import org.scottishtecharmy.soundscape.mapstyle.accessibleLayerOverrides
import org.scottishtecharmy.soundscape.mapstyle.argbToRgba
import org.scottishtecharmy.soundscape.mapstyle.buildMapStyle
import org.scottishtecharmy.soundscape.mapstyle.resolveTileSourceUrl

@Composable
actual fun PlatformMapContainer(
    mapCenter: LngLatAlt,
    allowScrolling: Boolean,
    userLocation: LngLatAlt?,
    userSymbolRotation: Float,
    beaconLocation: LngLatAlt?,
    routeData: RouteWithMarkers?,
    modifier: Modifier,
    extractGeometry: Geometry?,
    forceOnlineTiles: Boolean,
) {
    val context = LocalContext.current
    val filesDir = context.filesDir.toString()

    val foregroundColor = argbToRgba(MaterialTheme.colorScheme.onBackground.toArgb())
    val backgroundColor = argbToRgba(MaterialTheme.colorScheme.background.toArgb())
    val overrides = accessibleLayerOverrides(
        foregroundColor = foregroundColor,
        backgroundColor = backgroundColor
    )

    // Read TILE_PROVIDER_URL from Android BuildConfig via reflection
    val tileProviderUrl = remember {
        try {
            val buildConfigClass = Class.forName("org.scottishtecharmy.soundscape.BuildConfig")
            buildConfigClass.getField("TILE_PROVIDER_URL").get(null) as? String ?: ""
        } catch (_: Exception) { "" }
    }

    // Use offline extracts if available, otherwise fall back to network — unless
    // the caller explicitly requires online tiles (e.g. the offline-map details
    // preview, which should always render from the network regardless of what's
    // already downloaded).
    val extractsPath = context.getExternalFilesDir(null)?.absolutePath ?: ""
    val tileSourceUrl = remember(userLocation, forceOnlineTiles) {
        if (forceOnlineTiles) {
            resolveTileSourceUrl(location = null, extractsPath = "", networkTileUrl = tileProviderUrl)
        } else {
            resolveTileSourceUrl(
                location = userLocation,
                extractsPath = extractsPath,
                networkTileUrl = tileProviderUrl,
            )
        }
    }

    val baseStyle = remember(foregroundColor, backgroundColor, tileSourceUrl) {
        buildMapStyle(
            theme = AccessibleTheme,
            spritePath = "file://$filesDir/osm-liberty-accessible/osm-liberty",
            glyphsPath = "file://$filesDir/osm-liberty-accessible/fonts/{fontstack}/{range}.pbf",
            tileSourceUrl = tileSourceUrl,
            overrides = overrides,
            symbolForegroundColor = foregroundColor,
            symbolBackgroundColor = backgroundColor,
        )
    }

    MapContainerLibre(
        mapCenter = mapCenter,
        allowScrolling = allowScrolling,
        userLocation = userLocation,
        userSymbolRotation = userSymbolRotation,
        beaconLocation = beaconLocation,
        routeData = routeData,
        modifier = modifier,
        baseStyle = baseStyle,
        extractGeometry = extractGeometry,
    )
}
