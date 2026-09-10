package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import vector_tile.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A feature is called by its name in the app's language where the map has one, while keeping its
 * local name for matching against the other features which refer to it.
 */
class MvtNameTranslationTest {

    private val localShopName = "大丸梅田店"
    private val localRoadName = "御堂筋"

    private fun parse(nameKeys: List<String>): List<MvtFeature> {
        val shopKeys = listOf(
            "class", "subclass", "name", "name:en", "name:fr", "name:zh-Hans", "name:zh", "name:ja",
            "name_en", "name_int", "name:latin"
        )
        val shopValues = listOf(
            Tile.Value(string_value = "shop"),
            Tile.Value(string_value = "department_store"),
            Tile.Value(string_value = localShopName),
            Tile.Value(string_value = "Daimaru Umeda"),
            Tile.Value(string_value = "Daimaru Umeda (fr)"),
            Tile.Value(string_value = "Daimaru Umeda (zh-Hans)"),
            Tile.Value(string_value = "Daimaru Umeda (zh)"),
            Tile.Value(string_value = localShopName),
            Tile.Value(string_value = "Daimaru Umeda (name_en)"),
            Tile.Value(string_value = "Daimaru Umeda (name_int)"),
            Tile.Value(string_value = "Daimaru Umedaten"),
        )
        val shop = Tile.Feature(
            id = 10L,
            // Each tag is a key index and a value index. The translations come before the name, so
            // a translation can't be judged against the name as it's read.
            tags = listOf(3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 0, 0, 1, 1, 2, 2),
            type = Tile.GeomType.POINT,
            // MoveTo once (1 | 1 shl 3), to 100,100 zigzag encoded
            geometry = listOf(9, 200, 200),
        )

        val roadKeys = listOf("class", "name", "name:en")
        val roadValues = listOf(
            Tile.Value(string_value = "primary"),
            Tile.Value(string_value = localRoadName),
            Tile.Value(string_value = "Midosuji"),
        )
        val road = Tile.Feature(
            id = 20L,
            tags = listOf(0, 0, 1, 1, 2, 2),
            type = Tile.GeomType.LINESTRING,
            // MoveTo 1000,1000 then LineTo (1 shl 3 | 2) 1000 further along x, zigzag encoded
            geometry = listOf(9, 2000, 2000, 10, 2000, 0),
        )

        val tile = Tile(
            listOf(
                Tile.Layer(version = 2, name = "poi", features = listOf(shop), keys = shopKeys, values = shopValues, extent = 4096),
                Tile.Layer(version = 2, name = "transportation", features = listOf(road), keys = roadKeys, values = roadValues, extent = 4096),
            )
        )

        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        return vectorTileToGeoJson(
            8000, 5000, tile, intersectionMap, streetNumberMap, true, 14, nameKeys = nameKeys
        ).flatMap { it.features }.map { it as MvtFeature }
    }

    private fun List<MvtFeature>.shop() =
        assertNotNull(firstOrNull { it.name == localShopName }, "The shop should be one of the features")

    private fun List<MvtFeature>.road() =
        assertNotNull(filterIsInstance<Way>().firstOrNull { it.name == localRoadName }, "The road should be a Way")

    private fun List<MvtFeature>.assertNoNameTranslationProperties() {
        for (feature in this) {
            val keys = feature.properties?.keys ?: continue
            assertTrue(
                keys.none { it.startsWith("name:") || it.startsWith("name_") },
                "Translations are dropped, not kept in $keys"
            )
        }
    }

    @Test
    fun theAppLanguageNameIsUsedAndTheLocalNameKept() {
        val features = parse(nameKeysForLanguage("en", "GB"))

        val shop = features.shop()
        assertEquals("Daimaru Umeda", shop.translatedName)
        assertEquals("Daimaru Umeda", shop.getText(null).text)
        features.assertNoNameTranslationProperties()
    }

    @Test
    fun aRoadIsMatchedByItsLocalNameAndCalledByTheTranslation() {
        val road = parse(nameKeysForLanguage("en", "GB")).road()
        assertEquals("Midosuji", road.translatedName)
        assertEquals("Midosuji", road.getName(null, null, null))
    }

    @Test
    fun aLatinScriptLanguageWithoutATranslationGetsTheRomanisedName() {
        val features = parse(nameKeysForLanguage("de", "DE"))

        val shop = features.shop()
        assertEquals("Daimaru Umedaten", shop.translatedName)
        assertEquals("Daimaru Umedaten", shop.getText(null).text)
        // Which is only a fallback - English has a translation of its own
        assertEquals("Daimaru Umeda", parse(nameKeysForLanguage("en", "GB")).shop().translatedName)
        features.assertNoNameTranslationProperties()
    }

    @Test
    fun theLocalNameIsUsedWithoutATranslation() {
        // Korean has no name:ko here, and isn't written in Latin letters to want name:latin
        val features = parse(nameKeysForLanguage("ko", "KR"))

        val shop = features.shop()
        assertNull(shop.translatedName)
        assertEquals(localShopName, shop.getText(null).text)
        assertEquals(localRoadName, features.road().getName(null, null, null))
        features.assertNoNameTranslationProperties()
    }

    @Test
    fun aTranslationTheSameAsTheLocalNameIsNotKept() {
        assertNull(parse(nameKeysForLanguage("ja", "JP")).shop().translatedName)
    }

    @Test
    fun noLanguageMeansNoTranslation() {
        val features = parse(emptyList())
        assertNull(features.shop().translatedName)
        assertNull(features.road().translatedName)
        features.assertNoNameTranslationProperties()
    }

    @Test
    fun chineseIsTranslatedIntoTheScriptOfTheRegion() {
        assertEquals("Daimaru Umeda (zh-Hans)", parse(nameKeysForLanguage("zh", "CN")).shop().translatedName)
        // There's no name:zh-Hant, so Taiwan gets the plain name:zh
        assertEquals("Daimaru Umeda (zh)", parse(nameKeysForLanguage("zh", "TW")).shop().translatedName)
    }

    @Test
    fun nameKeysForEachLanguage() {
        assertEquals(emptyList(), nameKeysForLanguage(null, null))
        assertEquals(listOf("name:en", "name:latin"), nameKeysForLanguage("en", "GB"))
        assertEquals(listOf("name:pt", "name:latin"), nameKeysForLanguage("pt", "BR"))
        assertEquals(listOf("name:zh-Hans", "name:zh"), nameKeysForLanguage("zh", "CN"))
        assertEquals(listOf("name:zh-Hant", "name:zh"), nameKeysForLanguage("zh", "HK"))
        assertEquals(listOf("name:nb", "name:no", "name:latin"), nameKeysForLanguage("nb", "NO"))
        assertEquals(listOf("name:ja"), nameKeysForLanguage("ja", "JP"))
        assertEquals(listOf("name:sr"), nameKeysForLanguage("sr", "RS"))
    }
}
