package org.scottishtecharmy.soundscape.mapstyle

import okio.FileSystem
import okio.Path.Companion.toPath
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import org.scottishtecharmy.soundscape.geoengine.utils.getXYTile
import org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.findExtractPaths

/**
 * Resolves the tile source URL to use for the map style.
 *
 * If offline map extracts are available and cover the given location, returns a
 * `pmtiles://file://` URI pointing at the best (largest) local extract.
 * Otherwise returns the network tile server URL.
 *
 * @param location Current user location, or null if unknown
 * @param extractsPath Base directory where .pmtiles files are stored
 * @param networkTileUrl Network tile server base URL (e.g. "https://server.com")
 * @return The tile source URL to use in the map style
 */
fun resolveTileSourceUrl(
    location: LngLatAlt?,
    extractsPath: String,
    networkTileUrl: String,
): String {
    val networkUrl = "${networkTileUrl.trimEnd('/')}/$PROTOMAPS_SERVER_PATH.json"

    if (extractsPath.isEmpty()) return networkUrl

    val offlineExtractPaths = findExtractPaths(extractsPath)
    if (offlineExtractPaths.isEmpty()) return networkUrl

    if (location == null) {
        // No location — use the first available extract
        return "pmtiles://file://${offlineExtractPaths[0]}"
    }

    // Pick the largest extract that contains the user's location
    val tileXY = getXYTile(location, MAX_ZOOM_LEVEL)
    var bestPath: String? = null
    var bestSize = 0L

    for (extract in offlineExtractPaths) {
        try {
            val reader = PmTilesReader(extract.toPath())
            val tile = reader.getTile(MAX_ZOOM_LEVEL, tileXY.first, tileXY.second)
            reader.close()

            if (tile != null) {
                val fileSize = FileSystem.SYSTEM.metadata(extract.toPath()).size ?: 0L
                if (fileSize > bestSize) {
                    bestPath = extract
                    bestSize = fileSize
                }
            }
        } catch (_: Exception) {
            // Skip unreadable files
        }
    }

    return if (bestPath != null) {
        "pmtiles://file://$bestPath"
    } else {
        networkUrl
    }
}
