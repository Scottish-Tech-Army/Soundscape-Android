package org.scottishtecharmy.soundscape.geoengine

import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.dto.Tile
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.IntersectionType
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.findLineIntersectionPoint
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.getCentralPointForFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getCentroidOfPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geoengine.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.geoengine.utils.pointIsWithinBoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geoengine.utils.traverseIntersectionsConfectingNames
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.network.VectorTileClient
import org.scottishtecharmy.soundscape.platform.ioDispatcher
import kotlin.concurrent.Volatile
import kotlin.time.measureTime

enum class TreeId(
    val id: Int,
    val description: String
) {
    ROADS(0, "Roads"),
    ROADS_AND_PATHS(1, "Roads and Paths"),
    INTERSECTIONS(2, "Intersections"),
    ENTRANCES(3, "Entrances"),
    CROSSINGS(4, "Crossings"),
    POIS(5, "Pois"),
    TRANSIT_STOPS(6, "Transit Stops"),
    INTERPOLATIONS(7, "Interpolations"),
    INFORMATION_POIS(8, "Information POIs"),
    OBJECT_POIS(9, "Object POIs"),
    PLACE_POIS(10, "Place POIs"),
    LANDMARK_POIS(11, "Landmark POIs"),
    MOBILITY_POIS(12, "Mobility POIs"),
    SAFETY_POIS(13, "Safey POIs"),
    PLACES_AND_LANDMARKS(14, "Places and Landmarks"),
    SELECTED_SUPER_CATEGORIES(15, "Selected Super Categories"),
    SETTLEMENT_CITY(16, "Cities"),
    SETTLEMENT_TOWN(17, "Towns"),
    SETTLEMENT_VILLAGE(18, "Villages"),
    SETTLEMENT_HAMLET(19, "Hamlets"),
    TRANSIT(20, "Transit"),
    HOUSENUMBER(21, "House numbers"),
    HIGHWAY_JUNCTIONS(22, "Highway Junctions"),
    NAMED_WATER_POLYGONS(23, "Named Water Polygons"),
    MAX_COLLECTION_ID(24, ""),
    WAYS_SELECTION(id = 24, "Either Roads OR Roads and Paths")
}

fun treeIdToIndex(id: TreeId): TreeId {
    return if (id == TreeId.WAYS_SELECTION)
        TreeId.ROADS_AND_PATHS
    else
        id
}


@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
open class GridState(
    val zoomLevel: Int = MAX_ZOOM_LEVEL,
    val gridSize: Int = GRID_SIZE,
    passedInTreeContext: CloseableCoroutineDispatcher? = null
) {

    // HTTP connection to tile server; set by the platform layer before start().
    var tileClient: VectorTileClient? = null

    // Hook for platform-specific analytics; defaults to no-op
    var analytics: GridStateAnalytics = GridStateAnalytics.NoOp

    @Volatile
    private var centralBoundingBox = BoundingBox()
    private var totalBoundingBox = BoundingBox()
    var ruler = CheapRuler(0.0)
    var featureTrees = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureTree(null) }
    var gridIntersections = hashMapOf<LngLatAlt, Intersection>()
    var gridTransitIntersections = hashMapOf<LngLatAlt, Intersection>()
    var gridStreetNumberTreeMap = hashMapOf<String, FeatureTree>()

    val treeContext = passedInTreeContext ?: newSingleThreadContext("TreeContext")

    var validateContext = true

    // Bumped every time featureTrees/gridStreetNumberTreeMap are rebuilt from a new tile grid (or
    // torn down in stop()). Callers that cache anything derived from those trees - e.g.
    // OfflineGeocoder's per-street StreetDescription cache - key off this value to know when a
    // cached result no longer matches the data it was built from.
    var generation: Int = 0

    // This doesn't naturally belong in GridState, but it's where all the other geo info is. It's
    // a tree of Markers from the database.
    var markerTree: FeatureTree? = null

    open fun start(offlineExtractPath: String = "") {}
    open fun stop() {
        // Clean up the feature trees
        for (tree in featureTrees) {
            tree.tree = null
        }
        centralBoundingBox = BoundingBox()
        generation++
    }

    open fun fixupCollections(featureCollections: Array<FeatureCollection>) {}

    open fun checkOfflineMaps() {}

    /**
     * Forces the next locationUpdate() to recompute the grid from scratch, even if the location
     * hasn't left the current central area. Used when the on-disk offline map extracts have
     * changed (new download published, or an extract deleted) so the change is picked up on the
     * very next location fix instead of waiting for the user to walk outside the current grid.
     */
    fun refreshOfflineMaps() {
        centralBoundingBox = BoundingBox()
    }

    fun isLocationWithinGrid(location: LngLatAlt): Boolean {
        return pointIsWithinBoundingBox(location, totalBoundingBox)
    }

    /**
     * We want to join up Ways that cross tile boundaries.
     *
     * In our initial parsing we created Intersections at every line ending that is at the
     * tile boundary. Now that we have our grid, we can match up pairs of these intersections
     * in the same way that the InterpolatedPointsJoiner does - searching by distance. We add
     * a Way between each pair which is marked as a 'Tile Joiner'. These Ways can be followed
     * between the tiles, but are otherwise transparent. We should make it easy to dispose of
     * them when a new grid is made so that we can reuse the Tiles themselves for the new grid.
     *
     * This is generic across the road and transit (railway) intersection graphs - it just joins
     * whatever Intersections it's given, so the same logic works for either as long as they're
     * kept in separate lists (a road and a railway can share an exact coordinate at a level
     * crossing, and we don't want those two unrelated networks joined together).
     */
    private fun joinTileEdgeIntersections(
        grid: TileGrid,
        newGridIntersections: List<HashMap<LngLatAlt, Intersection>>
    ) {
        if (grid.tiles.size <= 1) return

        // Center of grid is bottom right of first tile
        val gridCenter = getLatLonTileWithOffset(
            grid.tiles[0].tileX,
            grid.tiles[0].tileY,
            zoomLevel,
            1.0, 1.0
        )

        val tileEdgeList = mutableListOf<Intersection>()
        for (intersectionList in newGridIntersections) {
            for (intersection in intersectionList) {
                if (intersection.value.intersectionType == IntersectionType.TILE_EDGE) {
                    // We have an edge - check if it's an internal edge to the grid
                    if (
                        (intersection.value.location.longitude == gridCenter.longitude) or
                        (intersection.value.location.latitude == gridCenter.latitude)
                    ) {
                        // This intersection needs joining, so put add it to our list
                        tileEdgeList.add(intersection.value)
                    }
                }
            }
        }

        // We have our list of intersections to join, so join them
        for (intersection1 in tileEdgeList) {
            for (intersection2 in tileEdgeList) {
                // Don't join to ourselves
                if (intersection1 != intersection2) {
                    // Don't join if already joined
                    if (intersection1.members.size < 2) {
                        // Join if within 1.0m
                        val distance =
                            ruler.distance(intersection1.location, intersection2.location)
                        if (distance < 1.0) {
                            // Join the intersections together
                            val way = Way()
                            way.geometry =
                                LineString(intersection1.location, intersection2.location)
                            way.wayType = WayType.JOINER
                            way.intersections[WayEnd.START.id] = intersection1
                            way.intersections[WayEnd.END.id] = intersection2
                            intersection1.members.add(way)
                            intersection2.members.add(way)
                            break
                        }
                    }
                }
            }
        }
    }

    // Candidate-gathering radius for attachRailwayCrossings - generous since it's only used to
    // shortlist nearby road/path Ways from the rtree before the real geometric intersection test
    // decides the match, not as a proximity threshold in its own right.
    private val railwayCrossingSearchDistanceMetres = 20.0

    /**
     * Attaches crossing_type/crossing_name/crossing_brunnel properties directly onto the road/path
     * Way(s) that geometrically cross a railway, mirroring what extractCrossings (MvtToGeoJson.kt)
     * already does for waterway crossings at MVT-tile-parse time - see its class doc comment for
     * why railway crossings specifically can't be resolved that early: a road and a railway can
     * straddle an MVT tile boundary right at the point they cross, with each tile's raw geometry
     * independently clipped there, so the two clipped pieces can fail to actually touch within
     * either single tile even though the real-world crossing is genuine. Running here instead,
     * against this whole grid's already-merged ROADS_AND_PATHS/TRANSIT Ways (localTrees, built
     * just above), means a crossing near a tile edge has the full, unclipped-relative-to-each-
     * other geometry available from both tiles at once.
     *
     * A genuine grade-separated crossing needs a brunnel tag on one side of the pair - either the
     * road (`brunnel=bridge`/`tunnel`) or, just as commonly, the railway itself (`brunnel=bridge`
     * for a viaduct, with the road/path running underneath left untagged, since OSM only tags the
     * structure that's actually built). Either side supplies the "grade separated" evidence; when
     * only the railway is tagged, the road is inferred to be going under it. A road with neither
     * side tagged is a level crossing, handled separately (an explicit `railway=level_crossing`
     * point), not by this geometric test. TreeId.TRANSIT already excludes subway/buried-tunnel
     * railway lines (see isUnmatchableRailway in MvtToGeoJson.kt), so there's no need to repeat
     * that exclusion here.
     */
    private fun attachRailwayCrossings(
        featureCollections: Array<FeatureCollection>,
        localTrees: Array<FeatureTree>
    ) {
        val railwayWays = featureCollections[TreeId.TRANSIT.id].features.filterIsInstance<Way>()
        if (railwayWays.isEmpty()) return

        val roadTree = localTrees[TreeId.ROADS_AND_PATHS.id]
        for (railway in railwayWays) {
            val railwayGeometry = railway.geometry as? LineString ?: continue
            if (railwayGeometry.coordinates.size < 2) continue
            val railwayBrunnel = railway.properties?.get("brunnel") as? String

            // getNearbyLine only supports Point/Polygon tree entries, not the LineString Ways
            // roadTree holds, so candidates are gathered by querying near each railway vertex
            // instead (getNearbyCollection does support LineString entries).
            val candidates = mutableSetOf<Way>()
            for (point in railwayGeometry.coordinates) {
                val nearby = roadTree.getNearbyCollection(point, railwayCrossingSearchDistanceMetres, ruler)
                for (feature in nearby.features) {
                    (feature as? Way)?.let { candidates.add(it) }
                }
            }

            for (road in candidates) {
                val roadGeometry = road.geometry as? LineString ?: continue
                val roadBrunnel = road.properties?.get("brunnel") as? String
                if (roadBrunnel == null && railwayBrunnel != "bridge") continue
                findLineIntersectionPoint(railwayGeometry.coordinates, roadGeometry.coordinates) ?: continue
                road.setProperty("crossing_type", "railway")
                road.setProperty("crossing_brunnel", roadBrunnel ?: "tunnel")
                railway.name?.let { road.setProperty("crossing_name", it) }
            }
        }
    }

    // How far from a POI we'll look for a road to call it "on". StreetDescription uses 25m for
    // the same job when deciding which houses belong to a street; much beyond 50m and we start
    // attributing POIs to the road on the far side of the block.
    private val nearestWaySearchDistanceMetres = 30.0

    // ~110m of latitude, less of longitude away from the equator. See settlementByCell below.
    private val settlementCellsPerDegree = 1000.0


    /**
     * Supplies the name of the settlement a location is in. Settlements aren't in the high-zoom
     * tiles this grid is built from - the "place" layer only appears at low zoom - so they live in
     * a separate GridState which only GeoEngine can see. It sets this; left null (e.g. in tests,
     * or for the settlement grid itself) no settlement is recorded.
     */
    var settlementNameProvider: ((LngLatAlt) -> String?)? = null

    /**
     * The nearest way to [probe] which identifies itself, or null if there isn't one within
     * [nearestWaySearchDistanceMetres].
     *
     * getNearbyCollection rather than getNearestCollection: the latter passes its metre distance
     * into RTree.nearest as a planar degree bound and asks for an unbounded k, so it ends up
     * walking the whole tree. This is a properly pruned box search, and it already deduplicates
     * the per-segment rtree entries back down to whole Ways.
     */
    /**
     * The point to search around for a POI's surroundings.
     *
     * getCentralPointForFeature handles points and polygons only. fixupCollections merges polygons
     * which straddle a tile boundary into MultiPolygons - exactly the large features (parks,
     * retail parks) most in need of an address - and a handful of POIs are LineStrings, so both
     * get a fallback here rather than being skipped.
     */
    private fun probePointFor(feature: Feature): LngLatAlt? {
        getCentralPointForFeature(feature)?.let { return it }
        when (val geometry = feature.geometry) {
            is MultiPolygon -> {
                val exteriorRing = geometry.coordinates.firstOrNull()?.firstOrNull()
                return exteriorRing?.let { getCentroidOfPolygon(Polygon(it)) }
            }
            // Piers and steps are ways which are also POIs, so they turn up here as LineStrings
            is LineString -> return geometry.coordinates.getOrNull(geometry.coordinates.size / 2)
            else -> return null
        }
    }

    /**
     * How far [feature] reaches from [probe] - zero for a point. Added to the candidate search
     * radius so that a road alongside the far edge of a large feature is still found.
     */
    private fun extentFrom(feature: Feature, probe: LngLatAlt): Double {
        val ring = when (val geometry = feature.geometry) {
            is Polygon -> geometry.coordinates.firstOrNull()
            is MultiPolygon -> geometry.coordinates.firstOrNull()?.firstOrNull()
            is LineString -> geometry.coordinates
            else -> null
        } ?: return 0.0
        return ring.maxOfOrNull { ruler.distance(probe, it) } ?: 0.0
    }

    /**
     * The nearest way to [poi] which identifies itself, or null if there isn't one within
     * [nearestWaySearchDistanceMetres] of it.
     *
     * getNearbyCollection rather than getNearestCollection: the latter passes its metre distance
     * into RTree.nearest as a planar degree bound and asks for an unbounded k, so it ends up
     * walking the whole tree. This is a properly pruned box search, and it already deduplicates
     * the per-segment rtree entries back down to whole Ways.
     */
    private fun nearestNamedWay(
        tree: FeatureTree,
        poi: Feature,
        probe: LngLatAlt,
        extent: Double
    ): Way? {
        var nearest: Way? = null
        var nearestDistance = Double.POSITIVE_INFINITY
        // Candidates are gathered around the POI's centre, widened by how far the POI reaches:
        // a large feature's centre can be well outside the threshold while the feature itself is
        // right beside the road - a playground's middle is 35m from the street its fence is 10m
        // from. The real threshold is applied to the measured distance below.
        for (candidate in tree.getNearbyCollection(probe, nearestWaySearchDistanceMetres + extent, ruler)) {
            val way = candidate as? Way ?: continue
            // Only ways which identify themselves are any use as an address. Read name/ref
            // directly rather than calling getName(), which confects names for un-named ways
            // (more rtree searches) and can decorate them with "to dead end" style suffixes.
            if ((way.name == null) && (way.ref == null)) continue
            // getDistanceToFeature measures a point against a whole feature, polygons included,
            // so going from the way back to the POI judges the POI by how close it actually gets
            // to the road rather than by where its middle happens to be.
            val pointOnWay = getDistanceToFeature(probe, way, ruler).point
            val distance = getDistanceToFeature(pointOnWay, poi, ruler).distance
            if ((distance < nearestWaySearchDistanceMetres) && (distance < nearestDistance)) {
                nearestDistance = distance
                nearest = way
            }
        }
        return nearest
    }

    /**
     * Associates each POI with the nearest named way and the settlement it's in, storing them as
     * MvtFeature.nearestWay and MvtFeature.nearestSettlement.
     *
     * Lists like Places Nearby are full of POIs which OSM never named - post boxes, benches,
     * waste baskets - and which all render as the same generic type label. Knowing which street
     * each one is on is what makes them tellable apart. Doing the association here, once per grid,
     * rather than on demand means it survives for as long as the grid does: the screen can be
     * opened, scrolled, re-sorted and re-opened without repeating any of it, and it's available
     * to any other consumer of the feature.
     *
     * Only the association is done here - no strings are built. Turning these into displayable
     * text stays lazy (LocationDescription.process()), so nothing is formatted for the thousands
     * of POIs which are never looked at.
     *
     * Needs the ROADS and ROADS_AND_PATHS rtrees built above, and must run inside the treeContext
     * like the rest of processGridState.
     */
    internal fun attachNearestWays(
        featureCollections: Array<FeatureCollection>,
        localTrees: Array<FeatureTree>
    ) {
        val roadTree = localTrees[TreeId.ROADS.id]
        val pathTree = localTrees[TreeId.ROADS_AND_PATHS.id]
        val settlementNameProvider = settlementNameProvider
        // The settlement grid is built from the low zoom "place" layer only, so it has no ways to
        // associate anything with and nothing to ask about settlements - don't walk its POIs.
        val hasWays = featureCollections[TreeId.ROADS.id].features.isNotEmpty() ||
                featureCollections[TreeId.ROADS_AND_PATHS.id].features.isNotEmpty()
        if (!hasWays && (settlementNameProvider == null)) return

        // Which settlement a point is in barely changes over a hundred metres - the cascade's
        // radii start at 1km - so POIs are bucketed into ~100m cells and the lookup done once per
        // cell. POIs cluster hard along streets, so this takes most of the work out of what is
        // otherwise four rtree queries each.
        val settlementByCell = mutableMapOf<Pair<Int, Int>, String?>()

        for (feature in featureCollections[TreeId.POIS.id]) {
            val poi = feature as? MvtFeature ?: continue
            if (poi.superCategory == SuperCategoryId.HOUSENUMBER) continue

            // A POI which carries its own OSM street doesn't need one confecting for it - but it
            // still needs a settlement. OSM addresses on POIs very often stop at addr:street with
            // no addr:city, which is how "Kersland Drive Car Park" ends up presented as a bare
            // "Kersland Drive" with no town, so the settlement is recorded either way.
            val hasOwnStreet = (poi.street != null) || (poi.housenumber != null)

            val probe = probePointFor(poi) ?: continue
            val extent = extentFrom(poi, probe)

            // Roads first, and only if there's nothing there do we accept a path. A named path
            // ("Clyde Walkway") places a POI in a park far better than nothing does, but where
            // there's a road it's always the better answer - ROADS deliberately excludes
            // footways, so a POI on a pavement is described by the street, not the pavement.
            if (!hasOwnStreet) {
                poi.nearestWay = nearestNamedWay(roadTree, poi, probe, extent)
                    ?: nearestNamedWay(pathTree, poi, probe, extent)
            }

            if (settlementNameProvider != null) {
                val cell = Pair(
                    (probe.latitude * settlementCellsPerDegree).toInt(),
                    (probe.longitude * settlementCellsPerDegree).toInt()
                )
                poi.nearestSettlement = settlementByCell.getOrPut(cell) {
                    settlementNameProvider(probe)
                }
            }
        }
    }

    /**
     * processGridState is now called from within the single thread that can access the tile grid.
     * This makes it somewhat performance critical. However, by doing this it allows us to
     * disconnect and reconnect the tile grid
     */
    fun processGridState(
        featureCollections: Array<FeatureCollection>,
        enabledCategories: Set<String>,
        newGridIntersections: List<HashMap<LngLatAlt, Intersection>>,
        newGridTransitIntersections: List<HashMap<LngLatAlt, Intersection>>,
        localTrees: Array<FeatureTree>,
        intersectionAccumulator: HashMap<LngLatAlt, Intersection>,
        transitIntersectionAccumulator: HashMap<LngLatAlt, Intersection>,
        grid: TileGrid,
        strings: LocalizedStrings?,
    ) {

        fixupCollections(featureCollections)

        val classifyTiming = measureTime {
            classifyPois(featureCollections, enabledCategories)
        }
        println("Classify took $classifyTiming")

        val rtreeTiming = measureTime {
            // Create rtrees for each feature collection
            for ((index, fc) in featureCollections.withIndex()) {
                localTrees[index] = FeatureTree(fc)
            }
        }
        println("R-Trees took $rtreeTiming")

        // Needs both the ROADS_AND_PATHS and TRANSIT rtrees built above, but nothing else from
        // this function, so it can run before the tile-edge stitching below.
        attachRailwayCrossings(featureCollections, localTrees)

        val nearestWayTiming = measureTime {
            attachNearestWays(featureCollections, localTrees)
        }
        println("Nearest ways took $nearestWayTiming")

        if (featureCollections[TreeId.ROADS_AND_PATHS.id].features.isNotEmpty()) {
            joinTileEdgeIntersections(grid, newGridIntersections)

            //
            // Confect names for un-named ways
            //
            // Start by traversing the way graph which is efficient and adds cut-through and
            // dead-end modifiers to the ways
            for (hashmap in newGridIntersections) {
                traverseIntersectionsConfectingNames(hashmap, intersectionAccumulator)
            }

            // And now update the names for the intersection to include the confections
            for (intersection in intersectionAccumulator) {
                intersection.value.updateName(strings = strings)
            }

// The other confection is done lazily as it's relatively time consuming to do the whole tile at once
//          //And then fill in any remaining names using rtree searches - this is slower and
//            // adds POIs and co-linear names
//            for (road in featureCollections[TreeId.ROADS_AND_PATHS.id]) {
//                confectNamesForRoad(road, this)
//          }
        }

        if (featureCollections[TreeId.TRANSIT.id].features.isNotEmpty()) {
            // Rail lines are normally already named, so unlike roads there's no name confection
            // step needed here - just stitch the Ways across tile boundaries and keep a merged,
            // location-keyed map of the intersections for lookup (mirroring gridIntersections).
            joinTileEdgeIntersections(grid, newGridTransitIntersections)
            for (hashmap in newGridTransitIntersections) {
                for (intersection in hashmap) {
                    transitIntersectionAccumulator[intersection.key] = intersection.value
                }
            }
        }
    }

    /**
     * The tile grid service is called each time the location changes. It checks if the location
     * has moved away from the center of the current tile grid and if it has calculates a new grid.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun locationUpdate(
        location: LngLatAlt,
        enabledCategories: Set<String>,
        strings: LocalizedStrings?,
    ): Boolean {
        // Check if we're still within the central area of our grid
        if (!pointIsWithinBoundingBox(location, centralBoundingBox)) {
            //println("Update central grid area")
            // The current location has moved from within the central area, so get the
            // new grid and the new central area.
            val tileGrid = getTileGrid(location, zoomLevel, gridSize)

            // We have a new centralBoundingBox, so update the tiles
            val featureCollections = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
            val newGridIntersections = mutableListOf<HashMap<LngLatAlt, Intersection>>()
            val newGridTransitIntersections = mutableListOf<HashMap<LngLatAlt, Intersection>>()
            val newGridStreetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
            if (updateTileGrid(
                    tileGrid,
                    featureCollections,
                    newGridIntersections,
                    newGridStreetNumberMap,
                    newGridTransitIntersections
                )
            ) {
                // We have got a new grid, so create our new central region
                centralBoundingBox = tileGrid.centralBoundingBox
                totalBoundingBox = tileGrid.totalBoundingBox

                // Assign rtrees to our shared trees from within the treeContext. All
                // other accesses of featureTrees needs to be from within the same
                // context.
                runBlocking {
                    withContext(treeContext) {

                        val duration = measureTime {
                            ruler = CheapRuler(location.latitude)
                            gridIntersections.clear()
                            gridTransitIntersections.clear()
                            processGridState(
                                featureCollections,
                                enabledCategories,
                                newGridIntersections,
                                newGridTransitIntersections,
                                featureTrees,
                                gridIntersections,
                                gridTransitIntersections,
                                tileGrid,
                                strings
                            )
                            gridStreetNumberTreeMap.clear()
                            for (collection in newGridStreetNumberMap)
                                gridStreetNumberTreeMap[collection.key] =
                                    FeatureTree(collection.value)
                            generation++
                        }
                        println("Time to process grid: $duration")
                    }
                }
                return true
            } else {
                // Updating the tile grid failed, due to a lack of network/server issue. There's
                // nothing that we can do, so simply retry on the next location update.
                return false
            }
        }
        return false
    }

    data class TileUpdateResult(
        val success: Boolean,
        val tile: Tile,
        var collections: Array<FeatureCollection>?,
        var intersections: HashMap<LngLatAlt, Intersection>?,
        var streetNumberMap: HashMap<String, FeatureCollection>?,
        var transitIntersections: HashMap<LngLatAlt, Intersection>? = null
    )

    private suspend fun updateTileGrid(
        tileGrid: TileGrid,
        featureCollections: Array<FeatureCollection>,
        gridIntersections: MutableList<HashMap<LngLatAlt, Intersection>>,
        gridStreetNumberMap: HashMap<String, FeatureCollection>,
        gridTransitIntersections: MutableList<HashMap<LngLatAlt, Intersection>>
    ): Boolean = withContext(ioDispatcher) {

        // Check for an updated list of offline maps
        checkOfflineMaps()

        for ((workerIndex, tile) in tileGrid.tiles.withIndex()) {
            tile.workerIndex = workerIndex
        }

        val deferredUpdates = tileGrid.tiles.map { tile ->
            async {
                var ret = false
                val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
                val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
                val transitIntersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
                val tileCollections = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
                analytics.logCostlyEvent("updateTile")
                for (retry in 1..5) {
                    ret = updateTile(
                        tile.tileX,
                        tile.tileY,
                        tile.workerIndex,
                        tileCollections,
                        intersectionMap,
                        streetNumberMap,
                        transitIntersectionMap
                    )
                    if (ret)
                        break
                }
                if (ret) {
                    TileUpdateResult(
                        true,
                        tile,
                        tileCollections,
                        intersectionMap,
                        streetNumberMap,
                        transitIntersectionMap
                    )
                } else {
                    TileUpdateResult(false, tile, null, null, null, null)
                }
            }
        }

        val results = deferredUpdates.awaitAll()
        if (results.any { !it.success }) {
            return@withContext false // If any tile failed, abort the whole grid update
        }

        // All tiles were processed successfully, now aggregate the results
        for (result in results) {
            result.collections?.let { collections ->
                for ((index, collection) in collections.withIndex()) {
                    featureCollections[index] += collection
                }
            }
            result.intersections?.let { intersections ->
                gridIntersections.add(intersections)
            }
            result.transitIntersections?.let { transitIntersections ->
                gridTransitIntersections.add(transitIntersections)
            }
            result.streetNumberMap?.let { streetNumberMap ->
                // Go through each street in the map and either add it to an existing map for the
                // same street, or add it in it's entirety as a new entry
                for (entry in streetNumberMap) {
                    if (gridStreetNumberMap.containsKey(entry.key))
                        gridStreetNumberMap[entry.key]?.plusAssign(entry.value)
                    else
                        gridStreetNumberMap[entry.key] = entry.value
                }
            }
        }

        return@withContext true
    }

    open suspend fun updateTile(
        x: Int,
        y: Int,
        workerIndex: Int,
        featureCollections: Array<FeatureCollection>,
        intersectionMap: HashMap<LngLatAlt, Intersection>,
        streetNumberMap: HashMap<String, FeatureCollection>,
        transitIntersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
    ): Boolean {
        return false
    }

    fun classifyPois(
        featureCollections: Array<FeatureCollection>,
        enabledCategories: Set<String> = emptySet()
    ) {
        // The FeatureCollection for POIS has been created, but we need to create sub-collections
        // for each of the super-categories along with one for the currently selected super-
        // categories.
        val superCategories = listOf(
            SuperCategoryId.INFORMATION,
            SuperCategoryId.OBJECT,
            SuperCategoryId.PLACE,
            SuperCategoryId.LANDMARK,
            SuperCategoryId.MOBILITY,
            SuperCategoryId.SAFETY,
            SuperCategoryId.SETTLEMENT_CITY,
            SuperCategoryId.SETTLEMENT_TOWN,
            SuperCategoryId.SETTLEMENT_VILLAGE,
            SuperCategoryId.SETTLEMENT_HAMLET,
            SuperCategoryId.HOUSENUMBER,
        )
        val superCategoryCollections = superCategories.associateWith { superCategory ->
            getPoiFeatureCollectionBySuperCategory(
                superCategory,
                featureCollections[TreeId.POIS.id]
            )
        }

        // Create super category feature collections
        var category = superCategoryCollections[SuperCategoryId.INFORMATION]
        featureCollections[TreeId.INFORMATION_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections[SuperCategoryId.OBJECT]
        featureCollections[TreeId.OBJECT_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections[SuperCategoryId.PLACE]
        featureCollections[TreeId.PLACE_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections[SuperCategoryId.LANDMARK]
        featureCollections[TreeId.LANDMARK_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections[SuperCategoryId.MOBILITY]
        featureCollections[TreeId.MOBILITY_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections[SuperCategoryId.SAFETY]
        featureCollections[TreeId.SAFETY_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections[SuperCategoryId.HOUSENUMBER]
        featureCollections[TreeId.HOUSENUMBER.id] = category ?: FeatureCollection()

        // Settlement and their area names
        category = superCategoryCollections[SuperCategoryId.SETTLEMENT_CITY]
        featureCollections[TreeId.SETTLEMENT_CITY.id] = category ?: FeatureCollection()
        category = superCategoryCollections[SuperCategoryId.SETTLEMENT_TOWN]
        featureCollections[TreeId.SETTLEMENT_TOWN.id] = category ?: FeatureCollection()
        category = superCategoryCollections[SuperCategoryId.SETTLEMENT_VILLAGE]
        featureCollections[TreeId.SETTLEMENT_VILLAGE.id] = category ?: FeatureCollection()
        category = superCategoryCollections[SuperCategoryId.SETTLEMENT_HAMLET]
        featureCollections[TreeId.SETTLEMENT_HAMLET.id] = category ?: FeatureCollection()

        // Create a merged collection of places and landmarks, as used by whatsAroundMe and aheadOfMe
        featureCollections[TreeId.PLACES_AND_LANDMARKS.id] += featureCollections[TreeId.PLACE_POIS.id]
        featureCollections[TreeId.PLACES_AND_LANDMARKS.id] += featureCollections[TreeId.LANDMARK_POIS.id]

        // Create merged collection of currently selected super categories
        if (enabledCategories.contains(PLACES_AND_LANDMARKS_KEY)) {
            featureCollections[TreeId.SELECTED_SUPER_CATEGORIES.id] +=
                featureCollections[TreeId.PLACE_POIS.id]
            featureCollections[TreeId.SELECTED_SUPER_CATEGORIES.id] +=
                featureCollections[TreeId.LANDMARK_POIS.id]
        }
        if (enabledCategories.contains(MOBILITY_KEY)) {
            featureCollections[TreeId.SELECTED_SUPER_CATEGORIES.id] +=
                featureCollections[TreeId.MOBILITY_POIS.id]
        }
    }

    // All functions which access the featureTrees need to be running within the treeContext,
    // so assert that this is the case.
    private fun checkContext() {
        // TODO: It feels like there should be a way to check the context, but coroutineContext
        //  is a suspend function and we are not. We can at least check that the current thread
        //  name matches. This should spot any calls using featureTrees which aren't running in
        //  the treeContext.
        if (validateContext) {
            assertRunningInTreeContext()
        }
    }

    fun getFeatureTree(id: TreeId): FeatureTree {
        checkContext()

        return featureTrees[treeIdToIndex(id).id]
    }

    fun getFeatureCollection(
        id: TreeId,
        location: LngLatAlt = LngLatAlt(),
        distance: Double = Double.POSITIVE_INFINITY,
        maxCount: Int = 0
    ): FeatureCollection {
        checkContext()
        val id = treeIdToIndex(id).id
        val result = if (distance == Double.POSITIVE_INFINITY) {
            featureTrees[id].getAllCollection()
        } else {
            val ruler = CheapRuler(location.latitude)
            if (maxCount == 0) {
                featureTrees[id].getNearbyCollection(location, distance, ruler)
            } else {
                featureTrees[id].getNearestCollection(location, distance, maxCount, ruler)
            }
        }
        return result
    }

    fun getNearestFeature(
        id: TreeId,
        ruler: Ruler,
        location: LngLatAlt = LngLatAlt(),
        distance: Double = Double.POSITIVE_INFINITY
    ): Feature? {
        checkContext()
        return featureTrees[treeIdToIndex(id).id].getNearestFeature(location, ruler, distance)
    }

    companion object {
        const val TAG = "GridState"
    }
}

/**
 * Given a valid Tile feature collection this will parse the collection and return a bus stops
 * feature collection. Uses the "bus_stop" feature_value to extract bus stops from GeoJSON.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return A FeatureCollection object that contains only bus stops.
 */
private fun getTransitStopsFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection {
    val transitStopFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        val mvtFeature = feature as MvtFeature
        if (mvtFeature.featureType != "transit") {
            when (mvtFeature.featureValue) {
                "bus_stop", "tram_stop", "subway", "station", "train_station", "ferry_terminal" ->
                    transitStopFeatureCollection.addFeature(feature)
            }
        }
    }
    return transitStopFeatureCollection
}

/**
 * Given a valid Tile feature collection this will parse the collection and return a crossing
 * feature collection. Uses the "crossing" feature_value to extract crossings from GeoJSON.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return A FeatureCollection object that contains only crossings.
 */
private fun getCrossingsFromTileFeatureCollection(tileFeatureCollection: FeatureCollection): FeatureCollection {
    val crossingsFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        val mvtFeature = feature as MvtFeature
        if (mvtFeature.featureType == "highway" && mvtFeature.featureValue == "crossing") {
            crossingsFeatureCollection.addFeature(feature)
        }
    }
    return crossingsFeatureCollection
}

/**
 * Given a valid Tile feature collection this will parse the collection and return an interpolation
 * points feature collection. Uses the "edgePoint" feature_value to extract crossings from GeoJSON.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return A FeatureCollection object that contains only edgePoints
 */
private fun getInterpolationPointsFromTileFeatureCollection(tileFeatureCollection: FeatureCollection): FeatureCollection {
    val interpolationPointsFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        val mvtFeature = feature as MvtFeature
        if (mvtFeature.featureClass == "edgePoint") {
            interpolationPointsFeatureCollection.addFeature(feature)
        }
    }
    return interpolationPointsFeatureCollection
}

/**
 * Given a valid Tile feature collection this will parse the collection and return a highway
 * junction feature collection. Uses the "highway_junction" feature_value to extract junctions
 * from GeoJSON - see [org.scottishtecharmy.soundscape.geoengine.mvttranslation.extractHighwayJunctions].
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return A FeatureCollection object that contains only highway junctions.
 */
private fun getHighwayJunctionsFromTileFeatureCollection(tileFeatureCollection: FeatureCollection): FeatureCollection {
    val junctionsFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        val mvtFeature = feature as MvtFeature
        if (mvtFeature.featureType == "highway" && mvtFeature.featureValue == "highway_junction") {
            junctionsFeatureCollection.addFeature(feature)
        }
    }
    return junctionsFeatureCollection
}

/**
 * Parses out the named `water` layer polygons used for the water-crossing proximity check - see
 * [org.scottishtecharmy.soundscape.geoengine.mvttranslation.extractNamedWaterPolygons].
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return A FeatureCollection object that contains only named water polygons.
 */
private fun getNamedWaterPolygonsFromTileFeatureCollection(tileFeatureCollection: FeatureCollection): FeatureCollection {
    val waterPolygonsFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        val mvtFeature = feature as MvtFeature
        if (mvtFeature.featureType == "water" && mvtFeature.featureValue == "named_water_polygon") {
            waterPolygonsFeatureCollection.addFeature(feature)
        }
    }
    return waterPolygonsFeatureCollection
}

/**
 * Parses out all the Entrances in a tile FeatureCollection using the "gd_entrance_list" feature_type.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return a feature collection object that contains only entrances.
 */
private fun getEntrancesFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection {
    val entrancesFeatureCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        feature.properties?.let { properties ->
            if (properties.contains("entrance")) {
                entrancesFeatureCollection.addFeature(feature)
            }
        }
    }
    return entrancesFeatureCollection
}

/**
 * Parses out all the Points of Interest (POI) in a tile FeatureCollection.
 * @param tileFeatureCollection
 * A FeatureCollection object.
 * @return a Feature collection object that contains only POI.
 */
private fun getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
    tileFeatureCollection: FeatureCollection
): FeatureCollection {
    val poiFeaturesCollection = FeatureCollection()
    for (feature in tileFeatureCollection) {
        var add = true
        val mvtFeature = feature as MvtFeature
        if (mvtFeature.featureType == "highway" || mvtFeature.featureType == "water") {
            add = false
        }
        if (mvtFeature.featureClass == "edgePoint" ||
            mvtFeature.featureClass == "rail" ||
            mvtFeature.featureClass == "transit"
        ) {
            add = false
        }
        if (add) poiFeaturesCollection.addFeature(mvtFeature)
    }

    return poiFeaturesCollection
}

fun processTileFeatureCollection(
    initialFeatureCollections: Array<FeatureCollection>,
    tileFeatureCollection: FeatureCollection
) {

    initialFeatureCollections[TreeId.ENTRANCES.id] += getEntrancesFeatureCollectionFromTileFeatureCollection(
        tileFeatureCollection
    )
    initialFeatureCollections[TreeId.POIS.id] += getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
        tileFeatureCollection
    )
    initialFeatureCollections[TreeId.TRANSIT_STOPS.id] += getTransitStopsFeatureCollectionFromTileFeatureCollection(
        tileFeatureCollection
    )
    initialFeatureCollections[TreeId.CROSSINGS.id] += getCrossingsFromTileFeatureCollection(
        tileFeatureCollection
    )
    initialFeatureCollections[TreeId.INTERPOLATIONS.id] += getInterpolationPointsFromTileFeatureCollection(
        tileFeatureCollection
    )
    initialFeatureCollections[TreeId.HIGHWAY_JUNCTIONS.id] += getHighwayJunctionsFromTileFeatureCollection(
        tileFeatureCollection
    )
    initialFeatureCollections[TreeId.NAMED_WATER_POLYGONS.id] += getNamedWaterPolygonsFromTileFeatureCollection(
        tileFeatureCollection
    )

    // POIS includes bus stops and crossings
    initialFeatureCollections[TreeId.POIS.id].plusAssignDeduplicate(initialFeatureCollections[TreeId.TRANSIT_STOPS.id])
    initialFeatureCollections[TreeId.POIS.id] += initialFeatureCollections[TreeId.CROSSINGS.id]
}
