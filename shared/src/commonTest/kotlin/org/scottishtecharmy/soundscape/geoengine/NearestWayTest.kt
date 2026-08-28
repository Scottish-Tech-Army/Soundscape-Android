package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Tests GridState.attachNearestWays(), which associates each POI with the nearest named road at
 * tile load time so that lists of otherwise indistinguishable un-named POIs ("Post Box", "Bench")
 * can say which street each one is on.
 */
class NearestWayTest {

    private val origin = LngLatAlt(-2.657, 51.430)

    /** An east-west road [offset] metres away from [origin] on the given [bearing]. */
    private fun road(name: String?, ref: String? = null, bearing: Double, offset: Double): Way {
        val centre = getDestinationCoordinate(origin, bearing, offset)
        return Way().apply {
            this.name = name
            this.ref = ref
            geometry = LineString(
                getDestinationCoordinate(centre, 270.0, 50.0),
                getDestinationCoordinate(centre, 90.0, 50.0),
            )
        }
    }

    private fun poi(configure: MvtFeature.() -> Unit = {}): MvtFeature =
        MvtFeature().apply {
            geometry = Point(origin)
            configure()
        }

    /**
     * Runs attachNearestWays over a POIS collection, a ROADS tree built from [roads] and a
     * ROADS_AND_PATHS tree built from [paths]. In real tile data ROADS_AND_PATHS is a superset of
     * ROADS; they're kept disjoint here so it's unambiguous which tree an answer came from.
     */
    private fun attach(
        pois: List<MvtFeature>,
        roads: List<Way>,
        paths: List<Way> = emptyList(),
        settlement: String? = null,
    ) {
        val gridState = GridState()
        gridState.validateContext = false
        gridState.ruler = origin.createCheapRuler()
        settlement?.let { name -> gridState.settlementNameProvider = { name } }

        val featureCollections = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
        for (poi in pois) featureCollections[TreeId.POIS.id].addFeature(poi)

        for (road in roads) featureCollections[TreeId.ROADS.id].addFeature(road)
        for (path in paths) featureCollections[TreeId.ROADS_AND_PATHS.id].addFeature(path)

        // processGridState builds one tree per collection, so mirror that here
        val localTrees = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureTree(featureCollections[it]) }

        gridState.attachNearestWays(featureCollections, localTrees)
    }

    @Test
    fun namedRoadIsPreferredOverANearerUnnamedOne() {
        val mainStreet = road(name = "Main Street", bearing = 0.0, offset = 10.0)
        val alley = road(name = null, bearing = 180.0, offset = 5.0)
        val postBox = poi()

        attach(listOf(postBox), listOf(alley, mainStreet))

        assertSame(mainStreet, postBox.nearestWay)
    }

    @Test
    fun nearestOfTwoNamedRoadsWins() {
        val far = road(name = "Far Street", bearing = 0.0, offset = 25.0)
        val near = road(name = "Near Street", bearing = 180.0, offset = 8.0)
        val postBox = poi()

        attach(listOf(postBox), listOf(far, near))

        assertEquals("Near Street", postBox.nearestWay?.name)
    }

    @Test
    fun refIsUsedWhenTheRoadHasNoName() {
        val trunkRoad = road(name = null, ref = "A81", bearing = 0.0, offset = 10.0)
        val postBox = poi()

        attach(listOf(postBox), listOf(trunkRoad))

        assertEquals("A81", postBox.nearestWay?.ref)
    }

    @Test
    fun roadBeyondTheSearchDistanceIsIgnored() {
        val distant = road(name = "Main Street", bearing = 0.0, offset = 60.0)
        val postBox = poi()

        attach(listOf(postBox), listOf(distant))

        assertNull(postBox.nearestWay)
    }

    @Test
    fun poiWithItsOwnStreetGetsNoWayButStillGetsASettlement() {
        // OSM addresses on POIs routinely stop at addr:street with no addr:city, so the
        // settlement is still worth recording even though the street isn't
        val mainStreet = road(name = "Main Street", bearing = 0.0, offset = 10.0)
        val carPark = poi { street = "Kersland Drive" }

        attach(listOf(carPark), listOf(mainStreet), settlement = "Milngavie")

        assertNull(carPark.nearestWay)
        assertEquals("Milngavie", carPark.nearestSettlement)
    }

    @Test
    fun poiWithItsOwnHousenumberGetsNoWayButStillGetsASettlement() {
        val mainStreet = road(name = "Main Street", bearing = 0.0, offset = 10.0)
        val shop = poi { housenumber = "17" }

        attach(listOf(shop), listOf(mainStreet), settlement = "Milngavie")

        assertNull(shop.nearestWay)
        assertEquals("Milngavie", shop.nearestSettlement)
    }

    @Test
    fun housenumberFeatureIsSkipped() {
        val mainStreet = road(name = "Main Street", bearing = 0.0, offset = 10.0)
        val house = poi { superCategory = SuperCategoryId.HOUSENUMBER }

        attach(listOf(house), listOf(mainStreet))

        assertNull(house.nearestWay)
    }

    @Test
    fun largePolygonIsAssociatedByItsEdgeNotItsCentre() {
        // A road 40m north of the centre is out of range of the centre, but only 15m from the
        // polygon's northern edge - which is the bit of it the user is standing at
        val mainStreet = road(name = "Main Street", bearing = 0.0, offset = 40.0)
        val north = getDestinationCoordinate(origin, 0.0, 25.0)
        val south = getDestinationCoordinate(origin, 180.0, 25.0)
        val playground = MvtFeature().apply {
            geometry = Polygon(
                arrayListOf(
                    getDestinationCoordinate(north, 270.0, 25.0),
                    getDestinationCoordinate(north, 90.0, 25.0),
                    getDestinationCoordinate(south, 90.0, 25.0),
                    getDestinationCoordinate(south, 270.0, 25.0),
                    getDestinationCoordinate(north, 270.0, 25.0),
                )
            )
        }

        attach(listOf(playground), listOf(mainStreet))

        assertSame(mainStreet, playground.nearestWay)
    }

    @Test
    fun polygonPoiResolvesFromItsCentroid() {
        val mainStreet = road(name = "Main Street", bearing = 0.0, offset = 20.0)
        val building = MvtFeature().apply {
            val northWest =
                getDestinationCoordinate(getDestinationCoordinate(origin, 0.0, 5.0), 270.0, 5.0)
            geometry = Polygon(
                arrayListOf(
                    northWest,
                    getDestinationCoordinate(getDestinationCoordinate(origin, 0.0, 5.0), 90.0, 5.0),
                    getDestinationCoordinate(getDestinationCoordinate(origin, 180.0, 5.0), 90.0, 5.0),
                    getDestinationCoordinate(getDestinationCoordinate(origin, 180.0, 5.0), 270.0, 5.0),
                    northWest,
                )
            )
        }

        attach(listOf(building), listOf(mainStreet))

        assertSame(mainStreet, building.nearestWay)
    }

    @Test
    fun everyPoiInTheCollectionIsVisited() {
        val mainStreet = road(name = "Main Street", bearing = 0.0, offset = 10.0)
        val first = poi()
        val second = poi()

        attach(listOf(first, second), listOf(mainStreet))

        assertSame(mainStreet, first.nearestWay)
        assertSame(mainStreet, second.nearestWay)
    }

    @Test
    fun namedPathIsUsedWhenThereIsNoNamedRoad() {
        val unnamedRoad = road(name = null, bearing = 0.0, offset = 10.0)
        val walkway = road(name = "Clyde Walkway", bearing = 180.0, offset = 12.0)
        val bench = poi()

        attach(listOf(bench), roads = listOf(unnamedRoad), paths = listOf(walkway))

        assertSame(walkway, bench.nearestWay)
    }

    @Test
    fun namedRoadBeatsANearerNamedPath() {
        val mainStreet = road(name = "Main Street", bearing = 0.0, offset = 25.0)
        val walkway = road(name = "Clyde Walkway", bearing = 180.0, offset = 3.0)
        val bench = poi()

        attach(listOf(bench), roads = listOf(mainStreet), paths = listOf(walkway))

        assertSame(mainStreet, bench.nearestWay)
    }

    @Test
    fun pathBeyondTheSearchDistanceIsIgnored() {
        val walkway = road(name = "Clyde Walkway", bearing = 0.0, offset = 60.0)
        val bench = poi()

        attach(listOf(bench), roads = emptyList(), paths = listOf(walkway))

        assertNull(bench.nearestWay)
    }

    @Test
    fun settlementIsRecordedAlongsideTheWay() {
        val mainStreet = road(name = "Main Street", bearing = 0.0, offset = 10.0)
        val postBox = poi()

        attach(listOf(postBox), listOf(mainStreet), settlement = "Bridgeton")

        assertEquals("Main Street", postBox.nearestWay?.name)
        assertEquals("Bridgeton", postBox.nearestSettlement)
    }

    @Test
    fun settlementIsRecordedEvenWithNoWayNearby() {
        val postBox = poi()

        attach(listOf(postBox), roads = emptyList(), paths = emptyList(), settlement = "Bridgeton")

        assertNull(postBox.nearestWay)
        assertEquals("Bridgeton", postBox.nearestSettlement)
    }

    @Test
    fun noSettlementProviderLeavesTheSettlementUnset() {
        val mainStreet = road(name = "Main Street", bearing = 0.0, offset = 10.0)
        val postBox = poi()

        attach(listOf(postBox), listOf(mainStreet))

        assertNull(postBox.nearestSettlement)
    }
}
