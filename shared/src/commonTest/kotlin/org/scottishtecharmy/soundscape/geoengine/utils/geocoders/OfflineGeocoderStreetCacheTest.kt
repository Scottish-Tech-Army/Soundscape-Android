@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * Tests OfflineGeocoder's StreetDescription cache (getOrBuildStreetDescription) in isolation,
 * without going through the full suspend getAddressFromLngLat()/tile pipeline.
 */
class OfflineGeocoderStreetCacheTest {

    private fun singleWayFixture(name: String = "Test Street"): Pair<GridState, Way> {
        val pointA = LngLatAlt(-2.657, 51.430)
        val pointB = getDestinationCoordinate(pointA, 0.0, 40.0)
        val ruler = pointA.createCheapRuler()

        val way = Way().apply {
            this.name = name
            geometry = LineString(pointA, pointB)
            length = ruler.distance(pointA, pointB)
        }
        // Dead-end Intersections at both ends so createDescription() actually adds this Way to
        // its `ways` list (a Way with no Intersections at either end is never added).
        way.intersections[WayEnd.START.id] = Intersection().apply {
            location = pointA
            members = mutableListOf(way)
        }
        way.intersections[WayEnd.END.id] = Intersection().apply {
            location = pointB
            members = mutableListOf(way)
        }

        val gridState = GridState()
        gridState.validateContext = false
        gridState.ruler = ruler
        gridState.featureTrees[TreeId.ROADS_AND_PATHS.id] = FeatureTree(
            FeatureCollection().apply { addFeature(way) }
        )

        return Pair(gridState, way)
    }

    private fun newGeocoder(gridState: GridState): OfflineGeocoder =
        OfflineGeocoder(gridState, gridState)

    @Test
    fun getOrBuildStreetDescription_sameStreetAndGeneration_reusesCachedInstance() {
        val (gridState, way) = singleWayFixture()
        val geocoder = newGeocoder(gridState)

        val first = geocoder.getOrBuildStreetDescription("Test Street", way, null)
        val second = geocoder.getOrBuildStreetDescription("Test Street", way, null)

        assertSame(first, second)
    }

    @Test
    fun getOrBuildStreetDescription_gridGenerationChanged_rebuilds() {
        val (gridState, way) = singleWayFixture()
        val geocoder = newGeocoder(gridState)

        val first = geocoder.getOrBuildStreetDescription("Test Street", way, null)
        gridState.generation++
        val second = geocoder.getOrBuildStreetDescription("Test Street", way, null)

        assertNotSame(first, second)
    }

    @Test
    fun getOrBuildStreetDescription_differentStreetName_rebuilds() {
        val (gridState, way) = singleWayFixture()
        val geocoder = newGeocoder(gridState)

        val first = geocoder.getOrBuildStreetDescription("Test Street", way, null)
        val second = geocoder.getOrBuildStreetDescription("Other Street", way, null)

        assertNotSame(first, second)
    }

    @Test
    fun getOrBuildStreetDescription_wayNotInCachedStreet_rebuilds() {
        val (gridState, way) = singleWayFixture()
        val (otherGridState, otherWay) = singleWayFixture("Other Street")
        // Reuse the same geocoder/gridState generation but pass in a Way that was never part of
        // the cached street's own way graph - the cache must not be reused just because the name
        // and generation happen to line up.
        val geocoder = newGeocoder(gridState)

        val first = geocoder.getOrBuildStreetDescription("Test Street", way, null)
        val second = geocoder.getOrBuildStreetDescription("Test Street", otherWay, null)

        assertNotSame(first, second)
        // Sanity check the fixtures really did produce distinct Way instances.
        assertNotSame(way, otherWay)
        assertNotSame(gridState, otherGridState)
    }
}
