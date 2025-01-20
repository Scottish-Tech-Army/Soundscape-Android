package org.scottishtecharmy.soundscape

import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GridState.Companion.createFromGeoJson
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.utils.searchFeaturesByName

class SearchTest {

    @Test
    fun testSearch() {
        // This does a really crude search through the "name" property of the POI features
        val gridState = createFromGeoJson(GeoJSONData3x3gridnoduplicates.tileGrid)
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        // As there isn't much going on in the tiles then it should return the local village shop/coffee place
        val searchResults = searchFeaturesByName(testPoiCollection, "honey")
        Assert.assertEquals(1, searchResults.features.size)

    }

}