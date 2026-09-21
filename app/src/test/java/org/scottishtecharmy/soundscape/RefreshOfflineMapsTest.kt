package org.scottishtecharmy.soundscape

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * GridState.locationUpdate() only recomputes the tile grid (and, via updateTileGrid(), rescans
 * the offline map extracts) when the location has moved outside the grid's current central area.
 * refreshOfflineMaps() exists to force that recompute on the very next location update even when
 * the location hasn't moved, so that a newly downloaded (or deleted) offline extract is picked up
 * immediately rather than only the next time the user happens to cross a grid boundary.
 */
class RefreshOfflineMapsTest {

    @Test
    fun refreshOfflineMapsForcesRecomputeWithoutMoving() {
        val location = LngLatAlt(-4.317357, 55.942527)
        val enabledCategories = mutableSetOf(PLACES_AND_LANDMARKS_KEY, MOBILITY_KEY)

        val gridState = FileGridState(MAX_ZOOM_LEVEL, GRID_SIZE)
        gridState.start(offlineExtractPath)

        runBlocking {
            // The first update always recomputes, starting from an empty central bounding box.
            assertTrue(gridState.locationUpdate(location, enabledCategories, null))

            // A second update at the same, unmoved location is a no-op - it's still within the
            // grid's existing central area.
            assertFalse(gridState.locationUpdate(location, enabledCategories, null))

            // refreshOfflineMaps() (called when an offline map extract is downloaded or deleted)
            // must force the very next update to recompute, even without the location moving.
            gridState.refreshOfflineMaps()
            assertTrue(gridState.locationUpdate(location, enabledCategories, null))
        }
    }
}
