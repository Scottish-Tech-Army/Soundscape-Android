@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.PluralKey
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Encodes (key, args) into a stable readable string instead of real localized text, so tests can
 * assert on exactly which StringKey/args were selected - same pattern as the private
 * FakeLocalizedStrings in CompassFacingDirectionsTest.kt / ManualCalloutsTest.kt (renamed here to
 * avoid a top-level name clash with that file's own private class in the same package).
 */
private class WayNamingFakeLocalizedStrings : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String {
        return if (args.isEmpty()) key.name else "${key.name}(${args.joinToString(",")})"
    }
    override fun getOrNull(key: StringKey, vararg args: Any?): String? = get(key, *args)
    override fun getPlural(key: PluralKey, quantity: Int, vararg args: Any?): String =
        "$key(${args.joinToString(", ")})"

    override fun resolveFeatureClass(key: String): String? = null
}

private fun way(
    name: String? = null,
    start: LngLatAlt,
    end: LngLatAlt,
    featureType: String? = "highway",
    featureValue: String? = "residential",
): Way = Way().apply {
    this.name = name
    this.featureType = featureType
    this.featureValue = featureValue
    geometry = LineString(start, end)
}

private fun poi(name: String, location: LngLatAlt): MvtFeature =
    MvtFeature().apply {
        this.name = name
        geometry = Point(location)
    }

private fun polygonPoi(name: String, center: LngLatAlt, halfSize: Double = 0.01): MvtFeature {
    val nw = LngLatAlt(center.longitude - halfSize, center.latitude + halfSize)
    val ne = LngLatAlt(center.longitude + halfSize, center.latitude + halfSize)
    val se = LngLatAlt(center.longitude + halfSize, center.latitude - halfSize)
    val sw = LngLatAlt(center.longitude - halfSize, center.latitude - halfSize)
    return MvtFeature().apply {
        this.name = name
        geometry = Polygon(arrayListOf(nw, ne, se, sw, nw))
    }
}

private fun waterway(name: String, vararg points: LngLatAlt): MvtFeature =
    MvtFeature().apply {
        this.name = name
        this.featureType = "waterway"
        this.featureValue = "named_waterway"
        this.featureClass = "river"
        geometry = LineString(*points)
    }

private fun waterPolygon(name: String, vararg ring: LngLatAlt): MvtFeature =
    MvtFeature().apply {
        this.name = name
        this.featureType = "water"
        this.featureValue = "named_water_polygon"
        geometry = Polygon(arrayListOf(*ring, ring.first()))
    }

/** [base] moved [east] metres east and [north] metres north, for laying out test geometry. */
private fun offset(base: LngLatAlt, east: Double, north: Double): LngLatAlt =
    getDestinationCoordinate(getDestinationCoordinate(base, 90.0, east), 0.0, north)

/** A GridState holding the given water features, with a ruler valid for [near]. */
private fun waterGridState(near: LngLatAlt, vararg water: MvtFeature): GridState {
    val gridState = GridState()
    gridState.validateContext = false
    gridState.ruler = near.createCheapRuler()
    gridState.featureTrees[TreeId.NAMED_WATERWAYS.id] = FeatureTree(
        FeatureCollection().apply {
            water.filter { it.featureType == "waterway" }.forEach { addFeature(it) }
        }
    )
    gridState.featureTrees[TreeId.NAMED_WATER_POLYGONS.id] = FeatureTree(
        FeatureCollection().apply {
            water.filter { it.featureType == "water" }.forEach { addFeature(it) }
        }
    )
    return gridState
}

class WayNamingTest {

    // ============================================================================================
    // addSidewalk
    // ============================================================================================

    @Test
    fun addSidewalk_notSidewalkOrCrossing_returnsFalseAndLeavesPropertiesUntouched() {
        val origin = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(origin, 0.0, 50.0)
        val road = way(name = null, start = origin, end = end)
        // No "footway"/"bicycle" property and not a cycleway, so isSidewalkOrCrossing() is false.

        val roadTree = FeatureTree(FeatureCollection())
        val ruler = origin.createCheapRuler()

        val found = addSidewalk(road, roadTree, ruler, null)

        assertFalse(found)
        assertNull(road.properties)
        assertNull(road.name)
    }

    @Test
    fun addSidewalk_alreadyHasPavementProperty_returnsTrueImmediately() {
        val origin = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(origin, 0.0, 50.0)
        val road = way(name = null, start = origin, end = end).apply {
            properties = hashMapOf("footway" to "sidewalk", "pavement" to "Already Known Road")
        }

        // Empty tree - if the short-circuit didn't work, this would find nothing and return false.
        val roadTree = FeatureTree(FeatureCollection())
        val ruler = origin.createCheapRuler()

        val found = addSidewalk(road, roadTree, ruler, null)

        assertTrue(found)
        // Name is untouched by the short-circuit path.
        assertNull(road.name)
    }

    @Test
    fun addSidewalk_findsMatchingNamedRoadAtBothEnds_usesFallbackTextWhenStringsNull() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0) // 50m north

        val sidewalk = way(name = null, start = start, end = end).apply {
            properties = hashMapOf("footway" to "sidewalk")
            // addSidewalk's "similar distance at both ends" guard compares delta against
            // currentRoad.length/2, so a real (non-zero) length is required here - production Ways
            // always have this populated by the tile pipeline (WayGenerator.addSegmentFeatureToWay).
            length = 50.0
        }
        // A parallel named road, offset ~3m east at both ends - close enough at both ends (small,
        // consistent delta) to be recognised as "the road this pavement runs alongside".
        val roadStart = getDestinationCoordinate(start, 90.0, 3.0)
        val roadEnd = getDestinationCoordinate(end, 90.0, 3.0)
        val road = way(name = "Main Street", start = roadStart, end = roadEnd)

        val roadTree = FeatureTree(
            FeatureCollection().apply {
                addFeature(sidewalk)
                addFeature(road)
            }
        )
        val ruler = start.createCheapRuler()

        val found = addSidewalk(sidewalk, roadTree, ruler, null)

        assertTrue(found)
        assertEquals("Pavement next to Main Street", sidewalk.name)
        assertEquals("Main Street", sidewalk.properties?.get("pavement"))
    }

    @Test
    fun addSidewalk_findsMatchingNamedRoadAtBothEnds_usesLocalizedStringWhenProvided() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)

        val sidewalk = way(name = null, start = start, end = end).apply {
            properties = hashMapOf("footway" to "sidewalk")
            length = 50.0
        }
        val roadStart = getDestinationCoordinate(start, 90.0, 3.0)
        val roadEnd = getDestinationCoordinate(end, 90.0, 3.0)
        val road = way(name = "Main Street", start = roadStart, end = roadEnd)

        val roadTree = FeatureTree(
            FeatureCollection().apply {
                addFeature(sidewalk)
                addFeature(road)
            }
        )
        val ruler = start.createCheapRuler()

        val found = addSidewalk(sidewalk, roadTree, ruler, WayNamingFakeLocalizedStrings())

        assertTrue(found)
        assertEquals("ConfectNamePavementNextTo(Main Street)", sidewalk.name)
    }

    @Test
    fun addSidewalk_noMatchingRoad_setsEmptyPavementPropertyAndLeavesNameNull() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val sidewalk = way(name = null, start = start, end = end).apply {
            properties = hashMapOf("footway" to "sidewalk")
        }
        // No nearby roads at all.
        val roadTree = FeatureTree(FeatureCollection().apply { addFeature(sidewalk) })
        val ruler = start.createCheapRuler()

        val found = addSidewalk(sidewalk, roadTree, ruler, null)

        assertFalse(found)
        assertNull(sidewalk.name)
        assertEquals("", sidewalk.properties?.get("pavement"))
    }

    @Test
    fun addSidewalk_matchingNamedRoadButDistanceDiffersTooMuch_doesNotMatch() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val sidewalk = way(name = null, start = start, end = end).apply {
            properties = hashMapOf("footway" to "sidewalk")
            length = 50.0
        }
        // Same named road at both ends, but much closer at the start (2m) than at the end (15m) -
        // delta of 13m fails the "similar distance at both ends" check (delta < 5.0).
        val roadStart = getDestinationCoordinate(start, 90.0, 2.0)
        val roadEnd = getDestinationCoordinate(end, 90.0, 15.0)
        val road = way(name = "Main Street", start = roadStart, end = roadEnd)

        val roadTree = FeatureTree(
            FeatureCollection().apply {
                addFeature(sidewalk)
                addFeature(road)
            }
        )
        val ruler = start.createCheapRuler()

        val found = addSidewalk(sidewalk, roadTree, ruler, null)

        assertFalse(found)
        assertEquals("", sidewalk.properties?.get("pavement"))
    }

    // ============================================================================================
    // checkNearbyPoi
    // ============================================================================================

    @Test
    fun checkNearbyPoi_returnsNearestFeature_whenNoExclusion() {
        val location = LngLatAlt(-2.657, 51.430)
        val near = poi("Cafe", getDestinationCoordinate(location, 0.0, 5.0))
        val tree = FeatureTree(FeatureCollection().apply { addFeature(near) })
        val ruler = location.createCheapRuler()

        val result = checkNearbyPoi(tree, location, null, ruler)

        assertEquals(near, result)
    }

    @Test
    fun checkNearbyPoi_skipsExcludedPolygonPoi_returnsNextNearest() {
        val location = LngLatAlt(-2.657, 51.430)
        val nearest = poi("Excluded Park", getDestinationCoordinate(location, 0.0, 2.0))
        val secondNearest = poi("Cafe", getDestinationCoordinate(location, 0.0, 8.0))
        val tree = FeatureTree(
            FeatureCollection().apply {
                addFeature(nearest)
                addFeature(secondNearest)
            }
        )
        val ruler = location.createCheapRuler()

        val result = checkNearbyPoi(tree, location, nearest, ruler)

        assertEquals(secondNearest, result)
    }

    @Test
    fun checkNearbyPoi_onlyNearbyFeatureIsExcluded_returnsNull() {
        val location = LngLatAlt(-2.657, 51.430)
        val only = poi("Excluded Park", getDestinationCoordinate(location, 0.0, 2.0))
        val tree = FeatureTree(FeatureCollection().apply { addFeature(only) })
        val ruler = location.createCheapRuler()

        val result = checkNearbyPoi(tree, location, only, ruler)

        assertNull(result)
    }

    @Test
    fun checkNearbyPoi_emptyTree_returnsNull() {
        val location = LngLatAlt(-2.657, 51.430)
        val tree = FeatureTree(FeatureCollection())
        val ruler = location.createCheapRuler()

        assertNull(checkNearbyPoi(tree, location, null, ruler))
    }

    // ============================================================================================
    // addPoiDestinations
    // ============================================================================================

    @Test
    fun addPoiDestinations_bothDestinationsAlreadySet_returnsFalseWithoutTouchingGridState() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val target = way(name = null, start = start, end = end).apply {
            properties = hashMapOf(
                "destination:backward" to "Existing Start",
                "destination:forward" to "Existing End",
            )
        }
        // validateContext is left at its default (true) - if addPoiDestinations touched
        // gridState.getFeatureTree at all outside a tree context this would throw.
        val gridState = GridState()

        val added = addPoiDestinations(target, gridState, null)

        assertFalse(added)
        assertEquals("Existing Start", target.properties?.get("destination:backward"))
        assertEquals("Existing End", target.properties?.get("destination:forward"))
    }

    @Test
    fun addPoiDestinations_markerNearStartOnly_setsBackwardDestinationOnly() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val target = way(name = null, start = start, end = end)

        val gridState = GridState()
        gridState.validateContext = false
        gridState.markerTree = FeatureTree(
            FeatureCollection().apply { addFeature(poi("The Kiosk", getDestinationCoordinate(start, 180.0, 2.0))) }
        )

        val added = addPoiDestinations(target, gridState, null)

        assertTrue(added)
        assertEquals("The Kiosk", target.properties?.get("destination:backward"))
        assertNull(target.properties?.get("destination:forward"))
    }

    @Test
    fun addPoiDestinations_markerTakesPriorityOverEntranceAtSameEnd() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val target = way(name = null, start = start, end = end)

        val gridState = GridState()
        gridState.validateContext = false
        gridState.markerTree = FeatureTree(
            FeatureCollection().apply { addFeature(poi("The Kiosk", getDestinationCoordinate(start, 180.0, 2.0))) }
        )
        gridState.featureTrees[TreeId.ENTRANCES.id] = FeatureTree(
            FeatureCollection().apply { addFeature(poi("Car Park Entrance", getDestinationCoordinate(start, 180.0, 3.0))) }
        )

        val added = addPoiDestinations(target, gridState, null)

        assertTrue(added)
        assertEquals("The Kiosk", target.properties?.get("destination:backward"))
    }

    @Test
    fun addPoiDestinations_bothEndsInsideSamePolygon_addsNoDestinations() {
        // A path entirely within a park: both ends resolve to the *same* containing polygon, and
        // addPoiDestinations deliberately avoids confecting "to Park" in that case (see the
        // function's doc comment about avoiding confusing confections inside parks).
        val parkCenter = LngLatAlt(-2.657, 51.430)
        val start = getDestinationCoordinate(parkCenter, 180.0, 50.0)
        val end = getDestinationCoordinate(parkCenter, 0.0, 50.0)
        val target = way(name = null, start = start, end = end)

        val gridState = GridState()
        gridState.validateContext = false
        gridState.featureTrees[TreeId.POIS.id] = FeatureTree(
            FeatureCollection().apply { addFeature(polygonPoi("Riverside Park", parkCenter)) }
        )

        val added = addPoiDestinations(target, gridState, null)

        assertFalse(added)
        assertNull(target.properties)
    }

    @Test
    fun addPoiDestinations_wayCrossesPolygonBoundary_setsDestinationOnlyForEndInsidePolygon() {
        val parkCenter = LngLatAlt(-2.657, 51.430)
        // Start is well outside the park's bounding box, end is inside it.
        val start = getDestinationCoordinate(parkCenter, 180.0, 2000.0)
        val end = getDestinationCoordinate(parkCenter, 0.0, 50.0)
        val target = way(name = null, start = start, end = end)

        val gridState = GridState()
        gridState.validateContext = false
        gridState.featureTrees[TreeId.POIS.id] = FeatureTree(
            FeatureCollection().apply { addFeature(polygonPoi("Riverside Park", parkCenter)) }
        )

        val added = addPoiDestinations(target, gridState, null)

        assertTrue(added)
        assertEquals("Riverside Park", target.properties?.get("destination:forward"))
        assertNull(target.properties?.get("destination:backward"))
    }

    @Test
    fun addPoiDestinations_entranceNearEndOnly_setsForwardDestination() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val target = way(name = null, start = start, end = end)

        val gridState = GridState()
        gridState.validateContext = false
        gridState.featureTrees[TreeId.ENTRANCES.id] = FeatureTree(
            FeatureCollection().apply { addFeature(poi("Main Entrance", getDestinationCoordinate(end, 0.0, 2.0))) }
        )

        val added = addPoiDestinations(target, gridState, null)

        assertTrue(added)
        assertEquals("Main Entrance", target.properties?.get("destination:forward"))
        assertNull(target.properties?.get("destination:backward"))
    }

    @Test
    fun addPoiDestinations_noPoisAnywhere_returnsFalse() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val target = way(name = null, start = start, end = end)

        val gridState = GridState()
        gridState.validateContext = false
        // Every tree defaults to empty (FeatureTree(null)).

        val added = addPoiDestinations(target, gridState, null)

        assertFalse(added)
        assertNull(target.properties)
    }

    @Test
    fun addPoiDestinations_followsUnnamedWayChainToFindTrueFarEndBeforeSearchingForPoi() {
        // target -- (2-member intersection B) -- next -- (single-member dead end C)
        // addPoiDestinations should search for a POI near the *far* end of this connected unnamed
        // corridor (near C), not just near target's own end point (B).
        val a = LngLatAlt(-2.657, 51.430)
        val b = getDestinationCoordinate(a, 0.0, 20.0)
        val c = getDestinationCoordinate(b, 0.0, 20.0)

        val intersectionA = Intersection().apply { location = a }
        val intersectionB = Intersection().apply { location = b }
        val intersectionC = Intersection().apply { location = c }

        val target = way(name = null, start = a, end = b).apply {
            intersections[WayEnd.START.id] = intersectionA
            intersections[WayEnd.END.id] = intersectionB
        }
        val next = way(name = null, start = b, end = c).apply {
            intersections[WayEnd.START.id] = intersectionB
            intersections[WayEnd.END.id] = intersectionC
        }
        intersectionA.members = mutableListOf(target)
        intersectionB.members = mutableListOf(target, next) // exactly 2 members -> followWays continues
        intersectionC.members = mutableListOf(next)

        val gridState = GridState()
        gridState.validateContext = false
        gridState.markerTree = FeatureTree(
            FeatureCollection().apply { addFeature(poi("Bus Depot", getDestinationCoordinate(c, 0.0, 2.0))) }
        )

        val added = addPoiDestinations(target, gridState, null)

        assertTrue(added)
        assertEquals("Bus Depot", target.properties?.get("destination:forward"))
        assertNull(target.properties?.get("destination:backward"))
    }

    // ============================================================================================
    // confectNamesForRoad
    // ============================================================================================

    @Test
    fun confectNamesForRoad_alreadyNamedNonCycleway_leavesRoadUntouched() {
        // Note: confectNamesForRoad unconditionally fetches gridState.getFeatureTree(WAYS_SELECTION)
        // *before* checking the "name == null || cycleway" guard (see WayNaming.kt line 244, ahead
        // of the guard on line 246) - despite the comment above it about avoiding rtree searches
        // where possible. So, unlike addPoiDestinations's early-return, this path still needs
        // validateContext = false / a populated tree even though it ends up unused.
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val road = way(name = "Existing Road", start = start, end = end)

        val gridState = GridState()
        gridState.validateContext = false

        confectNamesForRoad(road, gridState, null)

        assertEquals("Existing Road", road.name)
        assertNull(road.properties)
    }

    @Test
    fun confectNamesForRoad_unnamedRoad_pavementMatchFound_setsNameAndSkipsPoiDestinations() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val sidewalk = way(name = null, start = start, end = end, featureValue = "footway").apply {
            properties = hashMapOf("footway" to "sidewalk")
            length = 50.0
        }
        val roadStart = getDestinationCoordinate(start, 90.0, 3.0)
        val roadEnd = getDestinationCoordinate(end, 90.0, 3.0)
        val road = way(name = "Main Street", start = roadStart, end = roadEnd)

        val gridState = GridState()
        gridState.validateContext = false
        gridState.featureTrees[TreeId.ROADS_AND_PATHS.id] = FeatureTree(
            FeatureCollection().apply {
                addFeature(sidewalk)
                addFeature(road)
            }
        )
        // A marker that would satisfy addPoiDestinations too, to prove it's never reached.
        gridState.markerTree = FeatureTree(
            FeatureCollection().apply { addFeature(poi("Some Marker", getDestinationCoordinate(start, 180.0, 2.0))) }
        )

        confectNamesForRoad(sidewalk, gridState, null)

        assertEquals("Pavement next to Main Street", sidewalk.name)
        assertNull(sidewalk.properties?.get("destination:backward"))
    }

    @Test
    fun confectNamesForRoad_unnamedNonSidewalkRoad_fallsBackToPoiDestinations() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val road = way(name = null, start = start, end = end, featureValue = "service")

        val gridState = GridState()
        gridState.validateContext = false
        gridState.featureTrees[TreeId.ROADS_AND_PATHS.id] = FeatureTree(
            FeatureCollection().apply { addFeature(road) }
        )
        gridState.markerTree = FeatureTree(
            FeatureCollection().apply { addFeature(poi("Some Marker", getDestinationCoordinate(start, 180.0, 2.0))) }
        )

        confectNamesForRoad(road, gridState, null)

        assertNull(road.name) // addPoiDestinations doesn't set Way.name, only destination:* tags.
        assertEquals("Some Marker", road.properties?.get("destination:backward"))
    }

    @Test
    fun confectNamesForRoad_namedCycleway_isStillTreatedLikeASidewalkAndRenamed() {
        val start = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(start, 0.0, 50.0)
        val cycleway = way(
            name = "Cycle Path",
            start = start,
            end = end,
            featureType = "highway",
            featureValue = "cycleway",
        ).apply { length = 50.0 }
        val roadStart = getDestinationCoordinate(start, 90.0, 3.0)
        val roadEnd = getDestinationCoordinate(end, 90.0, 3.0)
        val road = way(name = "Riverside Drive", start = roadStart, end = roadEnd)

        val gridState = GridState()
        gridState.validateContext = false
        gridState.featureTrees[TreeId.ROADS_AND_PATHS.id] = FeatureTree(
            FeatureCollection().apply {
                addFeature(cycleway)
                addFeature(road)
            }
        )

        confectNamesForRoad(cycleway, gridState, null)

        // The pre-existing name "Cycle Path" is overwritten because cycleway is special-cased
        // through the same "name == null || cycleway" guard as unnamed roads.
        assertEquals("Pavement next to Riverside Drive", cycleway.name)
    }


    // ============================================================================================
    // addWaterAdjacency
    // ============================================================================================

    // A 200m path running due north, with the water laid out relative to its start.
    private val waterOrigin = LngLatAlt(-4.3053, 55.9319)

    private fun ridingPath(featureClass: String? = "path"): Way =
        way(
            name = null,
            start = waterOrigin,
            end = offset(waterOrigin, 0.0, 200.0),
            featureValue = "path",
        ).apply { this.featureClass = featureClass }

    @Test
    fun addWaterAdjacency_pathFollowingRiver_isNamedAfterIt() {
        val path = ridingPath()
        // River 20m to the east, running the full length of the path and beyond.
        val river = waterway(
            "Allander Water",
            offset(waterOrigin, 20.0, -50.0),
            offset(waterOrigin, 20.0, 250.0),
        )
        val gridState = waterGridState(waterOrigin, river)

        val name = addWaterAdjacency(path, gridState, null)

        assertEquals("Path next to Allander Water", name)
        assertEquals("Path next to Allander Water", path.name)
        assertEquals("Allander Water", path.properties?.get("waterside"))
    }

    @Test
    fun addWaterAdjacency_usesTheConfectNameNextToStringKey() {
        val path = ridingPath()
        val river = waterway(
            "Allander Water",
            offset(waterOrigin, 20.0, -50.0),
            offset(waterOrigin, 20.0, 250.0),
        )
        val gridState = waterGridState(waterOrigin, river)

        val name = addWaterAdjacency(path, gridState, WayNamingFakeLocalizedStrings())

        assertEquals("ConfectNameNextTo(Path,Allander Water)", name)
    }

    @Test
    fun addWaterAdjacency_pathMerelyCrossingRiver_isNotNamedAfterIt() {
        val path = ridingPath()
        // River running east-west, crossing the path near its middle. Close at one sample, far
        // away at every other - this is the false positive the fraction test exists to reject.
        val river = waterway(
            "Allander Water",
            offset(waterOrigin, -200.0, 100.0),
            offset(waterOrigin, 200.0, 100.0),
        )
        val gridState = waterGridState(waterOrigin, river)

        assertNull(addWaterAdjacency(path, gridState, null))
        assertNull(path.name)
        // Memoised as "no match" so the search isn't repeated.
        assertEquals("", path.properties?.get("waterside"))
    }

    @Test
    fun addWaterAdjacency_pathRoundLake_isNamedAfterTheLake() {
        val path = ridingPath()
        // A reservoir whose western shore runs 20m east of the path. The path is ~200m from the
        // polygon's centre, so this only works because distances are measured to the shore.
        val lake = waterPolygon(
            "Craigmaddie Reservoir",
            offset(waterOrigin, 20.0, -50.0),
            offset(waterOrigin, 20.0, 250.0),
            offset(waterOrigin, 320.0, 250.0),
            offset(waterOrigin, 320.0, -50.0),
        )
        val gridState = waterGridState(waterOrigin, lake)

        assertEquals("Path next to Craigmaddie Reservoir", addWaterAdjacency(path, gridState, null))
    }

    @Test
    fun addWaterAdjacency_waterTooFarAway_isIgnored() {
        val path = ridingPath()
        val river = waterway(
            "Allander Water",
            offset(waterOrigin, 60.0, -50.0),
            offset(waterOrigin, 60.0, 250.0),
        )
        val gridState = waterGridState(waterOrigin, river)

        assertNull(addWaterAdjacency(path, gridState, null))
    }

    @Test
    fun addWaterAdjacency_shortStub_isIgnored() {
        // A 20m connector below WATER_ADJACENCY_MIN_LENGTH_METRES: naming it after the river says
        // less about where it goes than the junction it joins.
        val stub = way(
            name = null,
            start = waterOrigin,
            end = offset(waterOrigin, 0.0, 20.0),
            featureValue = "path",
        ).apply { featureClass = "path" }
        val river = waterway(
            "Allander Water",
            offset(waterOrigin, 20.0, -50.0),
            offset(waterOrigin, 20.0, 250.0),
        )
        val gridState = waterGridState(waterOrigin, river)

        assertNull(addWaterAdjacency(stub, gridState, null))
        assertEquals("", stub.properties?.get("waterside"))
    }

    @Test
    fun addWaterAdjacency_wayWithARouteNumber_isLeftAlone() {
        val road = ridingPath(featureClass = "trunk").apply { ref = "A81" }
        val river = waterway(
            "River Kelvin",
            offset(waterOrigin, 20.0, -50.0),
            offset(waterOrigin, 20.0, 250.0),
        )
        val gridState = waterGridState(waterOrigin, river)

        assertNull(addWaterAdjacency(road, gridState, null))
        assertNull(road.name)
    }

    @Test
    fun addWaterAdjacency_severalCandidates_closestByMeanDistanceWins() {
        val path = ridingPath()
        val near = waterway(
            "Allander Water",
            offset(waterOrigin, 8.0, -50.0),
            offset(waterOrigin, 8.0, 250.0),
        )
        val far = waterPolygon(
            "Dougalston Loch",
            offset(waterOrigin, 22.0, -50.0),
            offset(waterOrigin, 22.0, 250.0),
            offset(waterOrigin, 322.0, 250.0),
            offset(waterOrigin, 322.0, -50.0),
        )
        val gridState = waterGridState(waterOrigin, near, far)

        assertEquals("Path next to Allander Water", addWaterAdjacency(path, gridState, null))
    }

    @Test
    fun addWaterAdjacency_riverSplitIntoSegments_isStillMatchedAsOneRiver() {
        val path = ridingPath()
        // The same river arriving as two LineStrings, as it does from a real tile. Neither half
        // runs the length of the path, so grouping by name is what makes this match at all.
        val first = waterway(
            "Allander Water",
            offset(waterOrigin, 20.0, -50.0),
            offset(waterOrigin, 20.0, 100.0),
        )
        val second = waterway(
            "Allander Water",
            offset(waterOrigin, 20.0, 100.0),
            offset(waterOrigin, 20.0, 250.0),
        )
        val gridState = waterGridState(waterOrigin, first, second)

        assertEquals("Path next to Allander Water", addWaterAdjacency(path, gridState, null))
    }

    @Test
    fun addWaterAdjacency_isMemoisedAndNotRecomputed() {
        val path = ridingPath()
        val river = waterway(
            "Allander Water",
            offset(waterOrigin, 20.0, -50.0),
            offset(waterOrigin, 20.0, 250.0),
        )
        val gridState = waterGridState(waterOrigin, river)
        assertEquals("Path next to Allander Water", addWaterAdjacency(path, gridState, null))

        // Empty the trees: a second call that re-ran the search would now find nothing.
        gridState.featureTrees[TreeId.NAMED_WATERWAYS.id] = FeatureTree(null)
        gridState.featureTrees[TreeId.NAMED_WATER_POLYGONS.id] = FeatureTree(null)

        assertEquals("Path next to Allander Water", addWaterAdjacency(path, gridState, null))
    }

    @Test
    fun confectNamesForRoad_unnamedPathBesideRiver_prefersWaterOverPoiDestinations() {
        val path = ridingPath()
        val river = waterway(
            "Allander Water",
            offset(waterOrigin, 20.0, -50.0),
            offset(waterOrigin, 20.0, 250.0),
        )
        val gridState = waterGridState(waterOrigin, river)
        gridState.featureTrees[TreeId.ROADS_AND_PATHS.id] = FeatureTree(
            FeatureCollection().apply { addFeature(path) }
        )
        // A marker that addPoiDestinations would otherwise use, to prove water wins.
        gridState.markerTree = FeatureTree(
            FeatureCollection().apply { addFeature(poi("Some Marker", offset(waterOrigin, 0.0, -2.0))) }
        )

        assertEquals("Path next to Allander Water", confectNamesForRoad(path, gridState, null))
        assertNull(path.properties?.get("destination:backward"))
    }

    // ============================================================================================
    // setDestinationTag
    // ============================================================================================

    @Test
    fun setDestinationTag_forwardsTrue_setsDestinationBackwardKey() {
        val target = Way()
        setDestinationTag(target, forwards = true, tagValue = "Main Street", brunnelOrStepsValue = "")

        assertEquals("Main Street", target.properties?.get("destination:backward"))
        assertNull(target.properties?.get("destination:forward"))
    }

    @Test
    fun setDestinationTag_forwardsFalse_deadEnd_setsDeadEndForwardKey() {
        val target = Way()
        setDestinationTag(target, forwards = false, tagValue = "dead-end", deadEnd = true, brunnelOrStepsValue = "")

        assertEquals("dead-end", target.properties?.get("dead-end:forward"))
        assertNull(target.properties?.get("destination:forward"))
        assertNull(target.properties?.get("destination:backward"))
    }

    @Test
    fun setDestinationTag_emptyTagValue_doesNotSetDestinationOrDeadEndProperty() {
        val target = Way()
        setDestinationTag(target, forwards = true, tagValue = "", brunnelOrStepsValue = "")

        assertNull(target.properties)
    }

    @Test
    fun setDestinationTag_withBrunnelValue_alsoSetsPassesProperty() {
        val target = Way()
        setDestinationTag(target, forwards = true, tagValue = "Main Street", brunnelOrStepsValue = "bridge")

        assertEquals("Main Street", target.properties?.get("destination:backward"))
        assertEquals("bridge", target.properties?.get("passes:backward"))
    }

    @Test
    fun setDestinationTag_emptyTagAndBrunnel_leavesPropertiesNull() {
        val target = Way()
        setDestinationTag(target, forwards = false, tagValue = "", brunnelOrStepsValue = "")

        assertNull(target.properties)
    }

    // ============================================================================================
    // traverseIntersectionsConfectingNames
    // ============================================================================================

    /**
     * Builds a simple named-road-into-unnamed-way intersection X:
     *   anchorW -- namedWay --> X -- unnamedWay --> anchorZ
     * Both anchors are single-member intersections so the chain doesn't extend beyond X, and
     * neither way is a genuine dead end (both ends have a real, non-null Intersection).
     */
    private class NamedJunctionFixture(
        val x: Intersection,
        val namedWay: Way,
        val unnamedWay: Way,
    )

    private fun buildNamedJunctionFixture(unnamedFeatureValue: String = "residential"): NamedJunctionFixture {
        val wLoc = LngLatAlt(-2.657, 51.420)
        val xLoc = LngLatAlt(-2.657, 51.430)
        val zLoc = getDestinationCoordinate(xLoc, 0.0, 20.0)

        val anchorW = Intersection().apply { location = wLoc }
        val x = Intersection().apply { location = xLoc }
        val anchorZ = Intersection().apply { location = zLoc }

        val namedWay = way(name = "Main Street", start = wLoc, end = xLoc).apply {
            intersections[WayEnd.START.id] = anchorW
            intersections[WayEnd.END.id] = x
        }
        val unnamedWay = way(name = null, start = xLoc, end = zLoc, featureValue = unnamedFeatureValue).apply {
            intersections[WayEnd.START.id] = x
            intersections[WayEnd.END.id] = anchorZ
        }
        anchorW.members = mutableListOf(namedWay)
        x.members = mutableListOf(namedWay, unnamedWay)
        anchorZ.members = mutableListOf(unnamedWay)

        return NamedJunctionFixture(x, namedWay, unnamedWay)
    }

    @Test
    fun traverseIntersections_propagatesNamedRoadNameToUnnamedNeighborAsDestination() {
        val fixture = buildNamedJunctionFixture()
        val gridIntersections = hashMapOf(fixture.x.location to fixture.x)

        traverseIntersectionsConfectingNames(gridIntersections)

        // unnamedWay's START is X, so "forwards" is true, which is stored under the "backward" key.
        assertEquals("Main Street", fixture.unnamedWay.properties?.get("destination:backward"))
        assertNull(fixture.unnamedWay.properties?.get("dead-end:backward"))
        assertNull(fixture.unnamedWay.properties?.get("dead-end:forward"))
        // Named ways are left alone by the naming pass.
        assertNull(fixture.namedWay.properties)
    }

    @Test
    fun traverseIntersections_skipsSidewalksAndCrossingsInNamingPass() {
        val fixture = buildNamedJunctionFixture(unnamedFeatureValue = "footway")
        fixture.unnamedWay.properties = hashMapOf("footway" to "sidewalk")
        val gridIntersections = hashMapOf(fixture.x.location to fixture.x)

        traverseIntersectionsConfectingNames(gridIntersections)

        // isSidewalkOrCrossing() causes the naming pass to skip this way entirely.
        assertNull(fixture.unnamedWay.properties?.get("destination:backward"))
        assertNull(fixture.unnamedWay.properties?.get("destination:forward"))
    }

    @Test
    fun traverseIntersections_recordsStepsInPassesProperty() {
        val fixture = buildNamedJunctionFixture()
        fixture.unnamedWay.featureSubClass = "steps"
        val gridIntersections = hashMapOf(fixture.x.location to fixture.x)

        traverseIntersectionsConfectingNames(gridIntersections)

        assertEquals("Main Street", fixture.unnamedWay.properties?.get("destination:backward"))
        assertEquals("steps", fixture.unnamedWay.properties?.get("passes:backward"))
    }

    @Test
    fun traverseIntersections_stopsFollowingOnceItReachesAnotherNamedWay() {
        // X -- unnamedWay --> Y (2 members) -- namedWay2 --> anchor
        // The naming pass should stop as soon as it reaches namedWay2, i.e. only unnamedWay gets
        // tagged with "Main Street" - namedWay2 keeps its own name and is not overwritten.
        val fixture = buildNamedJunctionFixture()
        val yLoc = fixture.unnamedWay.let { (it.geometry as LineString).coordinates.last() }
        val y = Intersection().apply { location = yLoc }
        val farAnchor = Intersection().apply { location = getDestinationCoordinate(yLoc, 0.0, 20.0) }
        val namedWay2 = way(name = "Second Street", start = yLoc, end = farAnchor.location).apply {
            intersections[WayEnd.START.id] = y
            intersections[WayEnd.END.id] = farAnchor
        }
        // Reconnect unnamedWay's far end to Y (2 members: unnamedWay, namedWay2) instead of the
        // single-member anchorZ used by the base fixture.
        fixture.unnamedWay.intersections[WayEnd.END.id] = y
        y.members = mutableListOf(fixture.unnamedWay, namedWay2)
        farAnchor.members = mutableListOf(namedWay2)

        val gridIntersections = hashMapOf(fixture.x.location to fixture.x)

        traverseIntersectionsConfectingNames(gridIntersections)

        assertEquals("Main Street", fixture.unnamedWay.properties?.get("destination:backward"))
        assertNull(namedWay2.properties)
        assertEquals("Second Street", namedWay2.name)
    }

    @Test
    fun traverseIntersections_marksGenuineDeadEnd() {
        val xLoc = LngLatAlt(-2.657, 51.430)
        val wLoc = LngLatAlt(-2.657, 51.420)
        val zLoc = getDestinationCoordinate(xLoc, 0.0, 20.0)

        val anchorW = Intersection().apply { location = wLoc }
        val x = Intersection().apply { location = xLoc }

        val namedWay = way(name = "Main Street", start = wLoc, end = xLoc).apply {
            intersections[WayEnd.START.id] = anchorW
            intersections[WayEnd.END.id] = x
        }
        // deadEndWay has no Intersection at its far end at all - a genuine dead end.
        val deadEndWay = way(name = null, start = xLoc, end = zLoc).apply {
            intersections[WayEnd.START.id] = x
            // intersections[WayEnd.END.id] left null
        }
        anchorW.members = mutableListOf(namedWay)
        x.members = mutableListOf(namedWay, deadEndWay)

        val gridIntersections = hashMapOf(x.location to x)

        traverseIntersectionsConfectingNames(gridIntersections)

        // The dead-end pass runs independently of (and after) the naming pass, using the *inverted*
        // forwards flag - see setDestinationTag call sites in WayNaming.kt lines 317-325 vs 336-339.
        assertEquals("dead-end", deadEndWay.properties?.get("dead-end:forward"))
        assertEquals("Main Street", deadEndWay.properties?.get("destination:backward"))
    }

    @Test
    fun traverseIntersections_accumulatesProcessedIntersectionsIntoProvidedMap() {
        val fixture = buildNamedJunctionFixture()
        val gridIntersections = hashMapOf(fixture.x.location to fixture.x)
        val accumulator = hashMapOf<LngLatAlt, Intersection>()

        traverseIntersectionsConfectingNames(gridIntersections, accumulator)

        assertEquals(1, accumulator.size)
        assertEquals(fixture.x, accumulator[fixture.x.location])
    }
}
