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
    currentBeaconWaypointIndex: Int,
    extractGeometry: Geometry?,
    forceOnlineTiles: Boolean,
    onInteractionChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current

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
        } catch (_: Exception) {
            ""
        }
    }

    // Use offline extracts if available, otherwise fall back to network — unless
    // the caller explicitly requires online tiles (e.g. the offline-map details
    // preview, which should always render from the network regardless of what's
    // already downloaded).
    //
    // Extracts are downloaded to <user-selected storage>/Downloads (see
    // AndroidOfflineMapsManager.extractsDir() and SoundscapeService.startGeoEngine()
    // in the app module), not the app's external-files root — this must match or
    // downloaded extracts are silently never found and the map falls back to
    // network tiles. MainActivity.SELECTED_STORAGE_KEY/_DEFAULT aren't reachable
    // from here (shared can't depend on the app module), so the well-known
    // AndroidX preference key/file naming convention is replicated directly, same
    // as the BuildConfig.TILE_PROVIDER_URL reflection lookup above.
    val extractsPath = remember {
        val prefs = context.getSharedPreferences(
            "${context.packageName}_preferences",
            android.content.Context.MODE_PRIVATE,
        )
        val selectedStorage = prefs.getString("SelectedStorage", "") ?: ""
        "$selectedStorage/${android.os.Environment.DIRECTORY_DOWNLOADS}"
    }
    val tileSourceUrl = remember(mapCenter, forceOnlineTiles) {
        if (forceOnlineTiles) {
            resolveTileSourceUrl(
                location = null,
                extractsPath = "",
                networkTileUrl = tileProviderUrl
            )
        } else {
            resolveTileSourceUrl(
                location = mapCenter,
                extractsPath = extractsPath,
                networkTileUrl = tileProviderUrl,
            )
        }
    }

    val baseStyle = remember(foregroundColor, backgroundColor, tileSourceUrl) {
        buildMapStyle(
            theme = AccessibleTheme,
            spritePath = "asset://osm-liberty-accessible/osm-liberty",
            glyphsPath = "asset://osm-liberty-accessible/fonts/{fontstack}/{range}.pbf",
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
        currentBeaconWaypointIndex = currentBeaconWaypointIndex,
        baseStyle = baseStyle,
        extractGeometry = extractGeometry,
        onInteractionChanged = onInteractionChanged,
    )
}
