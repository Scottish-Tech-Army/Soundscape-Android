package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * These two cases came from GeoUtilsTest in the app module, where they only ever exercised the
 * JTS-backed Android implementation. Now that mergePolygons is one pure-Kotlin implementation
 * in commonMain they live here, so they run on the JVM and compile and run for
 * iosSimulatorArm64 too - the first coverage this code has ever had on iOS, where until now it
 * was a stub that returned its first argument and silently dropped the second.
 */
class PolygonMergeTest {

    private fun feature(vararg rings: ArrayList<LngLatAlt>): MvtFeature =
        MvtFeature().also { feature ->
            feature.geometry = Polygon().also { polygon ->
                polygon.coordinates = arrayListOf(*rings)
            }
        }

    private fun ring(vararg points: Pair<Double, Double>): ArrayList<LngLatAlt> =
        points.mapTo(ArrayList()) { LngLatAlt(it.first, it.second) }

    @Test
    fun mergingCanCreateAndThenFillAHole() {
        val rectangle = feature(ring(0.0 to 2.0, 1.0 to 2.0, 1.0 to 0.0, 0.0 to 0.0, 0.0 to 2.0))
        // A reversed C that overlaps the rectangle, enclosing a gap between them.
        val reversedC = feature(
            ring(
                0.5 to 2.0, 4.0 to 2.0, 4.0 to 0.0, 0.5 to 0.0, 0.5 to 0.5,
                3.0 to 0.5, 3.0 to 1.5, 0.5 to 1.5, 0.5 to 2.0,
            )
        )

        val merged = mergePolygons(rectangle, reversedC)
        val mergedPolygon = merged.geometry as Polygon
        assertEquals(2, mergedPolygon.coordinates.size, "expected one outer and one inner ring")

        // Merging in a polygon that covers both of the previous ones should fill the hole.
        val covering = Feature().also {
            it.geometry = Polygon().also { polygon ->
                polygon.coordinates =
                    arrayListOf(ring(0.0 to 2.0, 4.0 to 2.0, 4.0 to 0.0, 0.0 to 0.0, 0.0 to 2.0))
            }
        }

        val secondMerge = mergePolygons(merged, covering)
        assertEquals(1, (secondMerge.geometry as Polygon).coordinates.size)
    }

    @Test
    fun splittingASquareDonutAndMergingItKeepsTheDonutWhole() {
        val donut = feature(
            ring(0.0 to 3.0, 3.0 to 3.0, 3.0 to 0.0, 0.0 to 0.0, 0.0 to 3.0),
            ring(1.0 to 1.0, 1.0 to 2.0, 2.0 to 2.0, 2.0 to 1.0, 1.0 to 1.0),
        )
        val slab = feature(ring(2.0 to 3.0, 3.0 to 3.0, 3.0 to 0.0, 2.0 to 0.0, 2.0 to 3.0))

        val merged = mergePolygons(donut, slab)
        assertEquals(2, (merged.geometry as Polygon).coordinates.size, "the hole should survive")
    }

    @Test
    fun mergedFeatureCarriesTheFirstFeaturesIdentity() {
        val first = feature(ring(0.0 to 0.0, 2.0 to 0.0, 2.0 to 1.0, 0.0 to 1.0, 0.0 to 0.0))
        first.osmId = 12345L
        first.name = "Amazon"
        first.setProperty("building", "warehouse")

        val second = feature(ring(1.0 to 0.0, 3.0 to 0.0, 3.0 to 1.0, 1.0 to 1.0, 1.0 to 0.0))
        second.osmId = 12345L

        val merged = mergePolygons(first, second) as MvtFeature
        assertEquals(12345L, merged.osmId)
        assertEquals("Amazon", merged.name)
        assertEquals("warehouse", merged.properties?.get("building"))
        assertEquals("Feature", merged.type)
    }

    @Test
    fun polygonsThatDoNotMergeReturnTheSecondFeatureItself() {
        // mergeAllPolygonsInFeatureCollection detects a failed merge by reference identity, so
        // this has to be polygon2 itself and not a copy of it.
        val first = feature(ring(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0, 0.0 to 0.0))
        val second = feature(ring(5.0 to 5.0, 6.0 to 5.0, 6.0 to 6.0, 5.0 to 6.0, 5.0 to 5.0))

        assertSame(second, mergePolygons(first, second))
    }

    @Test
    fun nonPolygonGeometryIsLeftAlone() {
        val point = Feature()
        val square = feature(ring(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0, 0.0 to 0.0))
        assertSame(square, mergePolygons(point, square))
    }
}
