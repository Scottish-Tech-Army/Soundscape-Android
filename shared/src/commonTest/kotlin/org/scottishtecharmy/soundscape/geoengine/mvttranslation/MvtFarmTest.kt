package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import vector_tile.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Farms are taken from the landuse layer as farmyards, named or not, and from the place layer as
 * place=farm, and nothing else in either is.
 */
class MvtFarmTest {

    // A square from 100,100 to 200,200 in tile coordinates, offset by [offset]: MoveTo once, LineTo
    // three times, then ClosePath, the coordinates zigzag encoded
    private fun square(offset: Int) =
        listOf(9, (100 + offset) * 2, (100 + offset) * 2, 26, 200, 0, 0, 200, 199, 0, 15)

    private fun parse(): List<MvtFeature> {
        val keys = listOf("class", "name")
        val values = listOf(
            Tile.Value(string_value = "farmyard"),
            Tile.Value(string_value = "Kettlehill Farm"),
            Tile.Value(string_value = "residential"),
            Tile.Value(string_value = "Craigdhu Estate"),
        )
        val namedFarmyard = Tile.Feature(
            id = 10L, tags = listOf(0, 0, 1, 1), type = Tile.GeomType.POLYGON, geometry = square(0)
        )
        val unnamedFarmyard = Tile.Feature(
            id = 20L, tags = listOf(0, 0), type = Tile.GeomType.POLYGON, geometry = square(1000)
        )
        val farmyardOfDouchlage = Tile.Feature(
            id = 25L, tags = listOf(0, 0), type = Tile.GeomType.POLYGON, geometry = square(1500)
        )
        val estate = Tile.Feature(
            id = 30L, tags = listOf(0, 2, 1, 3), type = Tile.GeomType.POLYGON, geometry = square(2000)
        )
        val placeKeys = listOf("class", "name", "rank")
        val placeValues = listOf(
            Tile.Value(string_value = "farm"),
            Tile.Value(string_value = "Easter Balmuildy"),
            Tile.Value(string_value = "village"),
            Tile.Value(string_value = "Balmore"),
            Tile.Value(string_value = "Lochfauld Farm"),
            Tile.Value(sint_value = 1),
            Tile.Value(string_value = "Kettlehill"),
            Tile.Value(string_value = "Douchlage"),
        )
        // Points: MoveTo once, to 500,500 and 1500,1500 in the tile, and to -100,-100 outside it,
        // where a farm in the tile next door is drawn its label from, zigzag encoded
        val farm = Tile.Feature(
            id = 40L, tags = listOf(0, 0, 1, 1, 2, 5), type = Tile.GeomType.POINT, geometry = listOf(9, 1000, 1000)
        )
        val village = Tile.Feature(
            id = 50L, tags = listOf(0, 2, 1, 3, 2, 5), type = Tile.GeomType.POINT, geometry = listOf(9, 3000, 3000)
        )
        val farmNextDoor = Tile.Feature(
            id = 60L, tags = listOf(0, 0, 1, 4, 2, 5), type = Tile.GeomType.POINT, geometry = listOf(9, 199, 199)
        )
        // Within the named farmyard, at 150,150, and the third unnamed one, at 1650,1650
        val farmInNamedFarmyard = Tile.Feature(
            id = 70L, tags = listOf(0, 0, 1, 6, 2, 5), type = Tile.GeomType.POINT, geometry = listOf(9, 300, 300)
        )
        val farmInUnnamedFarmyard = Tile.Feature(
            id = 80L, tags = listOf(0, 0, 1, 7, 2, 5), type = Tile.GeomType.POINT, geometry = listOf(9, 3300, 3300)
        )
        val tile = Tile(
            listOf(
                Tile.Layer(
                    version = 2, name = "landuse", keys = keys, values = values, extent = 4096,
                    features = listOf(namedFarmyard, unnamedFarmyard, farmyardOfDouchlage, estate)
                ),
                Tile.Layer(
                    version = 2, name = "place", keys = placeKeys, values = placeValues, extent = 4096,
                    features = listOf(farm, village, farmNextDoor, farmInNamedFarmyard, farmInUnnamedFarmyard)
                ),
            )
        )
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        return vectorTileToGeoJson(8000, 5000, tile, intersectionMap, streetNumberMap, true, 14)
            .flatMap { it.features }.map { it as MvtFeature }
    }

    @Test
    fun aNamedFarmyardIsAFarm() {
        val farm = assertNotNull(parse().firstOrNull { it.osmId == 10L })
        assertEquals("Kettlehill Farm", farm.name)
        assertEquals("landuse", farm.featureType)
        assertEquals("farm", farm.featureValue)
        assertEquals(SuperCategoryId.PLACE, farm.superCategory)
    }

    @Test
    fun anUnnamedFarmyardIsStillAFarm() {
        // That there's a farm there is worth knowing whether or not it has a name
        val farm = assertNotNull(parse().firstOrNull { it.osmId == 20L })
        assertNull(farm.name)
        assertEquals("farm", farm.featureValue)
    }

    @Test
    fun theRestOfTheLanduseLayerIsLeftOut() {
        assertTrue(parse().none { it.osmId == 30L })
    }

    @Test
    fun aPlaceFarmIsAFarm() {
        val farm = assertNotNull(parse().firstOrNull { it.osmId == 40L })
        assertEquals("Easter Balmuildy", farm.name)
        assertEquals("place", farm.featureType)
        assertEquals("farm", farm.featureValue)
        assertEquals(SuperCategoryId.PLACE, farm.superCategory)
    }

    @Test
    fun theSettlementsInThePlaceLayerAreLeftOut() {
        // They're read from the lower zoom settlement grid
        assertTrue(parse().none { it.osmId == 50L })
    }

    @Test
    fun aFarmInTheTileNextDoorIsLeftToThatTile() {
        assertTrue(parse().none { it.osmId == 60L })
    }

    @Test
    fun aFarmWithinANamedFarmyardIsThatFarmyard() {
        // A farm mapped both ways is one farm, called what its farmyard is
        val features = parse()
        assertTrue(features.none { it.osmId == 70L })
        assertEquals("Kettlehill Farm", features.single { it.osmId == 10L }.name)
    }

    @Test
    fun anUnnamedFarmyardIsCalledByTheFarmWithinIt() {
        val features = parse()
        assertTrue(features.none { it.osmId == 80L })
        assertEquals("Douchlage", features.single { it.osmId == 25L }.name)
    }
}
