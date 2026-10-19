package org.scottishtecharmy.soundscape

import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.classTag
import org.scottishtecharmy.soundscape.geoengine.utils.decompressTile
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.OfflineGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.TileSearch
import org.scottishtecharmy.soundscape.geoengine.utils.getXYTile
import org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.process
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Farms, which in the country are as often as not the only place there is to say you're near. They
 * come from landuse=farmyard - the farm buildings and the yard around them - and place=farm, which
 * the tiles have carried since the September 2026 build.
 */
class FarmTest {

    private val kettlehillFarm = LngLatAlt(-4.2800, 55.9451)

    // The middle of Milngavie, a couple of kilometres west of Easter Flucher and Tower Farm
    private val milngavie = LngLatAlt(-4.3060165, 55.9475021)

    @Before
    fun fixtureHasFarmyards() {
        // The test extracts from before farmyards were added to the tiles have none, and these
        // tests can't be run against them
        val reader = PmTilesReader("$offlineExtractPath/glasgow-gb.pmtiles".toPath())
        val (x, y) = getXYTile(kettlehillFarm, MAX_ZOOM_LEVEL)
        val tile = decompressTile(reader.tileCompression, reader.getTile(MAX_ZOOM_LEVEL, x, y)!!)!!
        reader.close()
        assumeTrue(
            "The Glasgow test extract predates farmyards in the tiles",
            tile.layers.any { (it.name == "landuse") && (classTag(it, "farmyard") != null) }
        )
    }

    private fun search(text: String) = runBlocking {
        val gridState = getGridStateForLocation(milngavie, MAX_ZOOM_LEVEL, GRID_SIZE)
        val settlementState = getGridStateForLocation(milngavie, 12, 3)
        OfflineGeocoder(
            gridState,
            settlementState,
            TileSearch(offlineExtractPath, gridState, settlementState),
            processor = { it.process() }
        ).getAddressFromLocationName(text, milngavie, null)!!
    }

    @Test
    fun aFarmyardIsAPlace() {
        val gridState = getGridStateForLocation(kettlehillFarm, MAX_ZOOM_LEVEL, GRID_SIZE)
        val farm = runBlocking {
            kotlinx.coroutines.withContext(gridState.treeContext) {
                gridState.getFeatureTree(TreeId.PLACES_AND_LANDMARKS)
                    .getNearestCollection(kettlehillFarm, 200.0, 50, gridState.ruler)
                    .map { it as MvtFeature }
                    .firstOrNull { it.name == "Kettlehill Farm" }
            }
        }
        assertNotNull(farm)
        assertEquals("farm", farm.featureValue)
    }

    private fun placesNamed(location: LngLatAlt, vararg names: String): List<MvtFeature> {
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, GRID_SIZE)
        return runBlocking {
            kotlinx.coroutines.withContext(gridState.treeContext) {
                gridState.getFeatureTree(TreeId.PLACES_AND_LANDMARKS).getAllCollection()
                    .map { it as MvtFeature }
                    .filter { it.name in names }
            }
        }
    }

    @Test
    fun anUnnamedFarmyardIsCalledByTheFarmWithinIt() {
        // Easter Balmuildy, north of Bishopbriggs, is mapped as place=farm within an unnamed
        // farmyard. It's one farm, the farmyard, called Easter Balmuildy - once, though the point
        // is drawn in the tiles around its own as well, and the farmyard crosses into the next one.
        val farms = placesNamed(LngLatAlt(-4.2702, 55.9170), "Easter Balmuildy")
        assertEquals(1, farms.size)
        assertEquals("landuse", farms[0].featureType)
        assertEquals("farm", farms[0].featureValue)
    }

    @Test
    fun aFarmWithinANamedFarmyardIsThatFarmyard() {
        // Blackhill, in Summerston, is mapped as place=farm within the farmyard Blackhill Farm. It's
        // one farm, called what its farmyard is, to the callouts and to the search alike.
        val blackhill = LngLatAlt(-4.2844, 55.9094)
        assertEquals(listOf("Blackhill Farm"), placesNamed(blackhill, "Blackhill", "Blackhill Farm").map { it.name })
        val results = search("blackhill").map { it.name }
        assertTrue("Blackhill Farm" in results, results.toString())
        assertTrue("Blackhill" !in results, results.toString())
    }

    @Test
    fun aFarmIsFoundByOfflineSearch() {
        // Both are beyond the grid around Milngavie
        assertEquals("Easter Flucher", search("easter flucher").first().name)
        assertEquals("Tower Farm", search("tower farm").first().name)
        // A place=farm, also beyond the grid, within an unnamed farmyard
        assertEquals("Lochfauld Farm", search("lochfauld farm").first().name)
    }
}
