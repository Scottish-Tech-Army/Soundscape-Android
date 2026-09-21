package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import vector_tile.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The values of a vector tile's tags come in several types, and a feature's properties are to have
 * each of them as the value it holds.
 */
class MvtTagValueTest {

    @Test
    fun floatAndDoubleTagValuesAreKept() {
        val keys = listOf("class", "subclass", "name", "rating", "height", "levels", "wheelchair")
        val values = listOf(
            Tile.Value(string_value = "shop"),
            Tile.Value(string_value = "convenience"),
            Tile.Value(string_value = "Corner Shop"),
            Tile.Value(float_value = 4.5f),
            Tile.Value(double_value = 198.96),
            Tile.Value(int_value = 39L),
            Tile.Value(bool_value = true),
        )
        val shop = Tile.Feature(
            id = 10L,
            // Each tag is a key index and a value index
            tags = listOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6),
            type = Tile.GeomType.POINT,
            // MoveTo once (1 | 1 shl 3), to 100,100 zigzag encoded
            geometry = listOf(9, 200, 200),
        )
        val tile = Tile(listOf(Tile.Layer(version = 2, name = "poi", features = listOf(shop), keys = keys, values = values, extent = 4096)))

        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        val collections = vectorTileToGeoJson(8000, 5000, tile, intersectionMap, streetNumberMap, true, 14)

        val properties = collections.flatMap { it.features }
            .map { it as MvtFeature }
            .firstOrNull { it.name == "Corner Shop" }
            ?.properties
        assertNotNull(properties, "The shop should be one of the features")
        assertEquals(4.5f, properties["rating"])
        assertEquals(198.96, properties["height"])
        assertEquals(39L, properties["levels"])
        assertEquals(true, properties["wheelchair"])
    }
}
