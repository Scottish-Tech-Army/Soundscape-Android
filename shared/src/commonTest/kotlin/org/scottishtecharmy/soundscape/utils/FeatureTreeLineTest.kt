package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * getNearbyLine used to silently match nothing when the tree held LineString features - the
 * Line branch of entryNearLine only ever handled Polygon/MultiPolygon and fell through to false.
 *
 * The geometry here mirrors the Edinburgh and Glasgow Main Line viaduct over the M80 at
 * Castlecary, which is the case that exposed it: a bridge is routinely a single 2-point way
 * spanning the whole obstacle, so its own vertices sit a long way back from the road it crosses.
 */
class FeatureTreeLineTest {

    private val ruler = CheapRuler(56.0)

    private fun lineFeature(vararg points: LngLatAlt) = Feature().apply {
        geometry = LineString(*points)
    }

    // A ~600m east-west road, and a ~170m north-south "viaduct" crossing its middle whose two
    // endpoints are ~85m either side of it.
    private val road = lineFeature(
        LngLatAlt(-3.950, 55.9816),
        LngLatAlt(-3.940, 55.9816),
    )
    private val viaduct = LineString(
        LngLatAlt(-3.945, 55.98083),
        LngLatAlt(-3.945, 55.98237),
    )

    @Test
    fun testGetNearbyLineFindsCrossingLineString() {
        val tree = FeatureTree(FeatureCollection().apply { addFeature(road) })
        val results = tree.getNearbyLine(viaduct, 20.0, ruler)
        assertEquals(1, results.features.size, "Expected the crossed road to be found")
    }

    @Test
    fun testPerVertexSearchMissesTheCrossing() {
        // Why the shortlist had to stop querying at the structure's vertices: neither endpoint of
        // the viaduct is anywhere near the road it crosses.
        val tree = FeatureTree(FeatureCollection().apply { addFeature(road) })
        for (vertex in viaduct.coordinates) {
            assertEquals(
                0,
                tree.getNearbyCollection(vertex, 20.0, ruler).features.size,
                "A 20m query at a viaduct endpoint should find nothing - that was the bug",
            )
        }
    }

    @Test
    fun testGetNearbyLineRespectsDistance() {
        val tree = FeatureTree(FeatureCollection().apply { addFeature(road) })
        // A parallel line ~40m north of the road: outside 20m, inside 50m.
        val parallel = LineString(
            LngLatAlt(-3.948, 55.98196),
            LngLatAlt(-3.942, 55.98196),
        )
        assertTrue(
            tree.getNearbyLine(parallel, 20.0, ruler).features.isEmpty(),
            "A line 40m away must not match at 20m",
        )
        assertEquals(1, tree.getNearbyLine(parallel, 50.0, ruler).features.size)
    }
}
