package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import kotlinx.cinterop.ExperimentalForeignApi
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Geometry
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.mapstyle.AccessibleTheme
import org.scottishtecharmy.soundscape.mapstyle.accessibleLayerOverrides
import org.scottishtecharmy.soundscape.mapstyle.argbToRgba
import org.scottishtecharmy.soundscape.mapstyle.buildMapStyle
import org.scottishtecharmy.soundscape.mapstyle.resolveTileSourceUrl
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringByAppendingPathComponent

@OptIn(ExperimentalForeignApi::class)
private fun getIosDocumentsDir(): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    )
    return paths.firstOrNull() as? String ?: ""
}

/**
 * Extracts map assets (sprites, fonts) from the iOS app bundle to the Documents directory
 * so MapLibre can access them via file:// URIs.
 */
@OptIn(ExperimentalForeignApi::class)
private fun extractMapAssets(): String {
    val docsDir = getIosDocumentsDir()
    @Suppress("CAST_NEVER_SUCCEEDS")
    val destDir = (docsDir as NSString).stringByAppendingPathComponent("osm-liberty-accessible")

    val fileManager = NSFileManager.defaultManager
    if (fileManager.fileExistsAtPath(destDir)) {
        return destDir
    }

    // Find the bundled osm-liberty-accessible directory
    val bundle = NSBundle.mainBundle
    val bundlePath = bundle.pathForResource("osm-liberty-accessible", ofType = null)

    if (bundlePath != null) {
        fileManager.copyItemAtPath(bundlePath, destDir, null)
    }

    return destDir
}

private fun getTileProviderUrl(): String {
    return platform.Foundation.NSBundle.mainBundle.objectForInfoDictionaryKey("TileProviderURL") as? String ?: ""
}

@Composable
fun rememberIosMapBaseStyle(location: LngLatAlt?): BaseStyle {
    val foregroundColor = argbToRgba(MaterialTheme.colorScheme.onBackground.toArgb())
    val backgroundColor = argbToRgba(MaterialTheme.colorScheme.background.toArgb())
    val overrides = accessibleLayerOverrides(
        foregroundColor = foregroundColor,
        backgroundColor = backgroundColor,
    )

    return remember(foregroundColor, backgroundColor, location) {
        val assetsDir = extractMapAssets()
        val extractsPath = NSHomeDirectory() + "/Documents"
        val tileSourceUrl = resolveTileSourceUrl(
            location = location,
            extractsPath = extractsPath,
            networkTileUrl = getTileProviderUrl(),
        )

        buildMapStyle(
            theme = AccessibleTheme,
            spritePath = "file://$assetsDir/osm-liberty",
            glyphsPath = "file://$assetsDir/fonts/{fontstack}/{range}.pbf",
            tileSourceUrl = tileSourceUrl,
            overrides = overrides,
            symbolForegroundColor = foregroundColor,
            symbolBackgroundColor = backgroundColor,
        )
    }
}

/**
 * iOS convenience wrapper for [MapContainerLibre] that automatically provides the map style.
 */
@Composable
fun IosMapContainerLibre(
    mapCenter: LngLatAlt,
    allowScrolling: Boolean,
    userLocation: LngLatAlt?,
    userSymbolRotation: Float,
    beaconLocation: LngLatAlt? = null,
    routeData: RouteWithMarkers? = null,
    modifier: Modifier = Modifier,
    extractGeometry: Geometry? = null,
) {
    val baseStyle = rememberIosMapBaseStyle(location = userLocation)

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
