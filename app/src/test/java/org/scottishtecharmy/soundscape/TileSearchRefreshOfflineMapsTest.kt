package org.scottishtecharmy.soundscape

import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.TileSearch
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * TileSearch.stringCache is keyed only by tile (x, y), not by which extract the data came from,
 * so it has to be discarded whenever the on-disk offline extracts change - otherwise a search
 * could keep returning strings read from an extract that's since been replaced or deleted.
 */
class TileSearchRefreshOfflineMapsTest {

    @Test
    fun refreshOfflineMapsClearsStringCache() {
        val location = LngLatAlt(-4.3108846, 55.9495440)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val settlementState = getGridStateForLocation(location, 12, 3)
        val tileSearch = TileSearch(offlineExtractPath, gridState, settlementState)

        tileSearch.search(location, "Craigmillar", null, emptySet())

        assertTrue(
            "search() should have populated the tile string cache",
            tileSearch.stringCache.isNotEmpty()
        )

        tileSearch.refreshOfflineMaps()

        assertTrue(
            "refreshOfflineMaps() must discard cached tile strings so a superseded extract's " +
                "data isn't returned after the offline map changes",
            tileSearch.stringCache.isEmpty()
        )
    }
}
