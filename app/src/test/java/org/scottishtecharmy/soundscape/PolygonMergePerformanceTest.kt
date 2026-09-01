package org.scottishtecharmy.soundscape

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.experimental.categories.Category
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.mergePolygons
import org.scottishtecharmy.soundscape.geoengine.utils.polygonFeaturesOverlap
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.locationtech.jts.geom.Polygon as JtsPolygon

/**
 * Times the polygon merge that ProtomapsGridState.fixupCollections performs, comparing the
 * pure-Kotlin clipper against the JTS implementation it replaced.
 *
 * MvtTileTest.testParsing does not cover this: it calls GridState.updateTile directly, and
 * fixupCollections - the only caller of mergePolygons - runs a level up in processGridState.
 * So this test rebuilds what production does, loading real tiles in 2x2 grids exactly as
 * GRID_SIZE dictates, since a polygon only needs merging when adjacent tiles each hold a piece
 * of it.
 *
 * Both implementations run over the same grids in the same process, so the comparison isn't at
 * the mercy of separate JVM startups or a differently-warmed JIT. Only the merge is timed; tile
 * loading is excluded.
 *
 * Nightly-only. It walks a few hundred tiles, and timings taken on a shared CI runner are noisy
 * enough that failing a PR on them would be misleading - the correctness of the merge is covered
 * on every PR by PolygonClipperParityTest and MergePolygonsTest instead. This exists to catch a
 * change of order in the merge cost, which is the thing that would show up in the field as a
 * stutter when the grid reloads.
 */
@Category(NightlyOnlyTest::class)
class PolygonMergePerformanceTest {

    private data class Region(val name: String, val minX: Int, val minY: Int, val tiles: Int)

    // The same places MvtTileTest.testParsing walks, trimmed to a few hundred tiles so the
    // merge cost is what dominates the run rather than tile decoding.
    private val regions = listOf(
        Region("Edinburgh", 16090 / 2, 10207 / 2, 4),
        Region("Bristol", 16128 / 2, 10880 / 2, 16),
        Region("Manchester", 16128 / 2, 10560 / 2, 16),
    )

    @Test
    fun clipperMergeIsNotSlowerThanJts() {
        val gridState = FileGridState()
        gridState.start(offlineExtractPath)
        gridState.checkOfflineMaps()

        var kotlinNanos = 0L
        var jtsNanos = 0L
        var grids = 0
        var polygonsIn = 0
        var kotlinMerges = 0
        var jtsMerges = 0

        for (region in regions) {
            // Walk 2x2 blocks, which is the grid size the geo engine actually loads.
            for (x in region.minX until region.minX + region.tiles step 2) {
                for (y in region.minY until region.minY + region.tiles step 2) {
                    val pois = loadPoiGrid(gridState, x, y)
                    val polygonCount = pois.features.count { it.geometry.type == "Polygon" }
                    if (polygonCount == 0) continue

                    grids++
                    polygonsIn += polygonCount

                    // Run each once to fault everything in before timing this grid, so the
                    // first grid measured isn't paying for class loading and JIT on its own.
                    if (grids == 1) {
                        mergeAll(pois, ::mergePolygons)
                        mergeAll(pois, ::mergePolygonsWithJts)
                    }

                    // Alternate which one goes first. Whichever runs second benefits from the
                    // caches and JIT state the first one leaves behind, and over a hundred-odd
                    // grids that bias is worth more than the difference being measured.
                    val kotlinResult: FeatureCollection
                    val jtsResult: FeatureCollection
                    if (grids % 2 == 0) {
                        var start = System.nanoTime()
                        kotlinResult = mergeAll(pois, ::mergePolygons)
                        kotlinNanos += System.nanoTime() - start
                        start = System.nanoTime()
                        jtsResult = mergeAll(pois, ::mergePolygonsWithJts)
                        jtsNanos += System.nanoTime() - start
                    } else {
                        var start = System.nanoTime()
                        jtsResult = mergeAll(pois, ::mergePolygonsWithJts)
                        jtsNanos += System.nanoTime() - start
                        start = System.nanoTime()
                        kotlinResult = mergeAll(pois, ::mergePolygons)
                        kotlinNanos += System.nanoTime() - start
                    }

                    kotlinMerges += polygonCount - kotlinResult.features.count {
                        it.geometry.type == "Polygon"
                    }
                    jtsMerges += polygonCount - jtsResult.features.count {
                        it.geometry.type == "Polygon"
                    }
                }
            }
        }

        val kotlinMs = kotlinNanos / 1_000_000.0
        val jtsMs = jtsNanos / 1_000_000.0
        println(
            """
            |Polygon merge over $grids real 2x2 tile grids ($polygonsIn polygons in):
            |  pure-Kotlin clipper : ${"%.1f".format(kotlinMs)} ms total, ${"%.3f".format(kotlinMs / grids)} ms per grid, $kotlinMerges polygons absorbed
            |  JTS                 : ${"%.1f".format(jtsMs)} ms total, ${"%.3f".format(jtsMs / grids)} ms per grid, $jtsMerges polygons absorbed
            |  ratio               : ${"%.2f".format(kotlinMs / jtsMs)}x
            """.trimMargin()
        )

        assertTrue(grids > 0, "no grids with polygons were loaded - are the tile fixtures present?")
        // A grid load happens on a background thread every time the user moves far enough, and
        // the whole of fixupCollections used to be a few milliseconds, so the bar that matters
        // is "still negligible against tile decoding", not "beats JTS". Fail only on a change
        // of order, which is what would show up as a stutter in the field.
        assertTrue(
            kotlinMs < jtsMs * 3.0,
            "the Kotlin clipper took ${"%.1f".format(kotlinMs)}ms against JTS's " +
                "${"%.1f".format(jtsMs)}ms, more than three times slower",
        )
    }

    /** The POI collections of a 2x2 tile grid, combined the way processGridState combines them. */
    private fun loadPoiGrid(gridState: FileGridState, x: Int, y: Int): FeatureCollection {
        val combined = FeatureCollection()
        for (tileX in x until x + 2) {
            for (tileY in y until y + 2) {
                val collections = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
                val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
                val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
                val transitIntersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
                runBlocking {
                    gridState.updateTile(
                        tileX, tileY, 0, collections,
                        intersectionMap, streetNumberMap, transitIntersectionMap,
                    )
                }
                combined += collections[TreeId.POIS.id]
            }
        }
        return combined
    }

    /**
     * mergeAllPolygonsInFeatureCollection, with the pairwise union left open so both
     * implementations are driven by identical grouping code and the timing difference is only
     * ever the union itself.
     */
    private fun mergeAll(
        polygons: FeatureCollection,
        merge: (Feature, Feature) -> Feature,
    ): FeatureCollection {
        val result = FeatureCollection()
        val features = hashMapOf<Any, MutableList<FeatureCollection>>()

        for (feature in polygons.features) {
            if (feature.geometry.type != "Polygon") {
                result.addFeature(feature)
                continue
            }
            val groups = features.getOrPut((feature as MvtFeature).osmId) { mutableListOf() }
            var foundOverlap = false
            for (group in groups) {
                for (existing in group) {
                    if (polygonFeaturesOverlap(feature, existing)) {
                        group.addFeature(feature)
                        foundOverlap = true
                        break
                    }
                }
            }
            if (!foundOverlap) {
                result.let { }
                groups.add(FeatureCollection().also { it.addFeature(feature) })
            }
        }

        for ((_, groups) in features) {
            for (group in groups) {
                var merged: Feature? = null
                for ((index, feature) in group.features.withIndex()) {
                    val previous = merged
                    merged = if (index == 0 || previous == null) feature else merge(previous, feature)
                    if (merged == feature && previous != null) result.addFeature(previous)
                }
                merged?.let { result.addFeature(it) }
            }
        }
        return result
    }

    // ---------------------------------------------------------------- the JTS baseline
    // Verbatim from shared/src/androidMain/.../PolygonMerge.android.kt as it was before the
    // clipper replaced it, so this measures the real thing rather than an approximation.

    private fun ringToJts(ring: List<LngLatAlt>, factory: GeometryFactory): LinearRing =
        factory.createLinearRing(
            ring.map { Coordinate(it.longitude, it.latitude) }.toTypedArray()
        )

    private fun toJtsPolygon(polygon: Polygon?): JtsPolygon? {
        if (polygon == null) return null
        val factory = GeometryFactory()
        val outer = polygon.coordinates.firstOrNull()?.let { ringToJts(it, factory) }
        val inner = polygon.getInteriorRings().map { ringToJts(it, factory) }.toTypedArray()
        return factory.createPolygon(outer, inner)
    }

    private fun mergePolygonsWithJts(polygon1: Feature, polygon2: Feature): Feature {
        val first = toJtsPolygon(polygon1.geometry as? Polygon)
        val second = toJtsPolygon(polygon2.geometry as? Polygon)

        val union = first?.union(second)
        if (union !is JtsPolygon) return polygon2

        return MvtFeature().also { feature ->
            feature.properties = polygon1.properties
            feature.type = "Feature"
            (polygon1 as? MvtFeature)?.let { feature.copyProperties(it) }
            feature.geometry = Polygon().also { polygon ->
                polygon.coordinates = arrayListOf(
                    union.exteriorRing.coordinates.mapTo(ArrayList()) {
                        LngLatAlt(it.x, it.y)
                    }
                )
                for (ring in 0 until union.numInteriorRing) {
                    polygon.addInteriorRing(
                        union.getInteriorRingN(ring).coordinates.mapTo(ArrayList()) {
                            LngLatAlt(it.x, it.y)
                        }
                    )
                }
            }
        }
    }

    @Test
    fun bothImplementationsAbsorbTheSamePolygons() {
        // The timing test is only meaningful if the two are doing comparable amounts of work,
        // so check they agree on how many polygons the merge removes.
        val gridState = FileGridState()
        gridState.start(offlineExtractPath)
        gridState.checkOfflineMaps()

        var kotlinTotal = 0
        var jtsTotal = 0
        val region = regions.first { it.name == "Bristol" }
        for (x in region.minX until region.minX + 8 step 2) {
            for (y in region.minY until region.minY + 8 step 2) {
                val pois = loadPoiGrid(gridState, x, y)
                if (pois.features.none { it.geometry.type == "Polygon" }) continue
                kotlinTotal += mergeAll(pois, ::mergePolygons).features.size
                jtsTotal += mergeAll(pois, ::mergePolygonsWithJts).features.size
            }
        }
        println("Feature counts after merging - Kotlin: $kotlinTotal, JTS: $jtsTotal")
        assertEquals(jtsTotal, kotlinTotal, "the two implementations merged different amounts")
    }
}
