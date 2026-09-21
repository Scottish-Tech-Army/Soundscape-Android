package org.scottishtecharmy.soundscape.migration

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.MOBILITY_KEY
import org.scottishtecharmy.soundscape.geoengine.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** What a name lookup came back with. */
sealed interface LegacyMarkerNameResult {
    /**
     * A name, along with how we arrived at it. [source] exists for logging and support: "matched
     * OSM id" and "nearest POI, 34m away" are very different levels of confidence in the same
     * string.
     */
    data class Named(val name: String, val source: String) : LegacyMarkerNameResult

    /**
     * The map data for this location was searched and holds nothing that matches. The marker
     * falls back to the address the legacy app geocoded for it - the best answer available, and
     * no amount of retrying will improve it.
     */
    data object NotFound : LegacyMarkerNameResult

    /**
     * There is no map data for this location to search: no network and no offline map covering
     * it. Unlike [NotFound] this says nothing about the marker, so the import stops rather than
     * name it wrongly - see [importLegacyPayload].
     */
    data object NoTileData : LegacyMarkerNameResult
}

/**
 * Names a legacy marker that has no nickname of its own.
 *
 * Split out from [importLegacyPayload] as an interface so that the importer's tests don't need a
 * tile server, and so the iOS entry point can decide how the tile data gets loaded.
 */
interface LegacyMarkerNameResolver {
    suspend fun resolve(entityKey: String, location: LngLatAlt): LegacyMarkerNameResult
}

/**
 * Works out what a legacy marker was called by looking up the feature it was placed on in the
 * current tile data.
 *
 * ## Why this is needed
 *
 * The legacy app didn't store a name for a marker created from a POI - only the POI's key, e.g.
 * `ft-443758688`. It resolved that key against its own cache of the (now retired) Soundscape tile
 * service every time it drew the marker list, so the name never had to be persisted. We can't
 * follow that path: the service is gone and the cache realm holding the POIs it returned may have
 * been cleared, or may never have covered the marker in the first place. So instead we fetch the
 * current tiles for the marker's location and look the feature up there.
 *
 * ## Matching by OSM id
 *
 * The legacy key is `ft-` followed by an OSM id, e.g. `ft-443758688`. Our tiles are built by
 * planetiler, which encodes an OSM object as `osm_id * 10 + type`, type being 1 for a node, 2 for
 * a way and 3 for a relation. The legacy key doesn't say which of those it was, so both the node
 * and the way candidate are tried; within one tile grid a wrong-type collision is vanishingly
 * unlikely. Relations aren't tried: matching against them found nothing in practice, and every
 * marker that does match is a node or a way.
 *
 * OSM ids are not stable forever, though - an object that gets deleted and redrawn comes back with
 * a new id, and the legacy data was frozen years before the tiles we fetch now. So a miss is
 * expected often enough to need a fallback, which is [nearestNamedPoi]: the marker's coordinates
 * are the POI's own coordinates, so whatever named feature sits at that spot today is almost
 * always the thing the user saved.
 */
class TileLegacyMarkerNamer(
    private val gridState: GridState,
    private val strings: LocalizedStrings?,
    private val log: (String) -> Unit = {},
    /**
     * Total wall-clock this namer is allowed across the whole import.
     *
     * Each new area costs a round of tile fetches, so a user with markers scattered across a
     * country could otherwise be held on the migration screen for a very long time. Once the
     * budget is gone the remaining markers fall back to their estimated address, which is what
     * they'd have got without this lookup anyway. The screen shows progress throughout, so this
     * is a cap on patience rather than a guard against the launch watchdog.
     */
    private val budget: Duration = DEFAULT_BUDGET,
) : LegacyMarkerNameResolver {

    private val started = TimeSource.Monotonic.markNow()
    private var budgetReported = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun resolve(entityKey: String, location: LngLatAlt): LegacyMarkerNameResult {
        val spent = started.elapsedNow()
        // A location already inside the loaded grid costs nothing but a tree search, so it's
        // allowed through even once the budget for fetching new areas has gone. Running out of
        // budget is [NotFound] rather than [NoTileData]: it's us giving up, not the map being
        // unavailable, and retrying the whole import wouldn't make it finish any sooner.
        if (spent > budget && !gridState.isLocationWithinGrid(location)) {
            if (!budgetReported) {
                budgetReported = true
                log("$budget budget spent after $spent - naming the rest from their addresses")
            }
            return LegacyMarkerNameResult.NotFound
        }

        if (!ensureGridCovers(location)) {
            log("no tile data for $location (offline, or outside the map) - can't name $entityKey")
            return LegacyMarkerNameResult.NoTileData
        }

        val candidateIds = protomapsIdsForLegacyEntityKey(entityKey)
        log("$entityKey -> protomaps id candidates $candidateIds")

        return withContext(gridState.treeContext) {
            matchByOsmId(candidateIds)
                ?: nearestNamedPoi(location)
                ?: LegacyMarkerNameResult.NotFound
        }
    }

    /**
     * Loads the tile grid around [location] if it isn't already loaded. Markers are processed in
     * whatever order they come out of the legacy database, so consecutive markers in the same town
     * reuse a grid and only the first of them pays for the tile fetches.
     */
    private suspend fun ensureGridCovers(location: LngLatAlt): Boolean {
        if (gridState.isLocationWithinGrid(location)) return true

        gridState.locationUpdate(location, ALL_CATEGORIES, strings)
        return gridState.isLocationWithinGrid(location)
    }

    /** Must be called from within [GridState.treeContext]. */
    private fun matchByOsmId(candidateIds: List<Long>): LegacyMarkerNameResult.Named? {
        if (candidateIds.isEmpty()) return null

        for (treeId in ID_MATCH_TREES) {
            for (feature in gridState.getFeatureCollection(treeId)) {
                val mvtFeature = feature as? MvtFeature ?: continue
                if (mvtFeature.osmId !in candidateIds) continue

                val text = mvtFeature.getText(strings).text
                log("matched osmId ${mvtFeature.osmId} in ${treeId.description}: \"$text\"")
                if (text.isNotBlank()) {
                    return LegacyMarkerNameResult.Named(text, "OSM id matched in ${treeId.description}")
                }
            }
        }
        return null
    }

    /**
     * Falls back to whatever named feature is at the marker's coordinates - first a polygon the
     * marker sits inside (a park, a building), then the nearest named POI.
     *
     * Must be called from within [GridState.treeContext].
     */
    private fun nearestNamedPoi(location: LngLatAlt): LegacyMarkerNameResult.Named? {
        val ruler = CheapRuler(location.latitude)
        val poiTree = gridState.getFeatureTree(TreeId.POIS)

        for (feature in poiTree.getContainingPolygons(location)) {
            val mvtFeature = feature as? MvtFeature ?: continue
            if (mvtFeature.name.isNullOrEmpty()) continue

            val text = mvtFeature.getText(strings).text
            if (text.isNotBlank()) {
                log("no OSM id match; marker is inside \"$text\"")
                return LegacyMarkerNameResult.Named(text, "inside named POI")
            }
        }

        val nearby = poiTree.getNearestCollection(
            location,
            NEAREST_POI_SEARCH_METRES,
            NEAREST_POI_LOG_COUNT,
            ruler,
            include = { !(it as MvtFeature).name.isNullOrEmpty() },
        )
        if (nearby.features.isEmpty()) {
            log("no OSM id match and no named POI within ${NEAREST_POI_SEARCH_METRES}m")
            return null
        }

        // Log the runners-up as well as the winner: when a marker comes out with the wrong name
        // this is the list that says whether the right one was even a candidate.
        for ((index, feature) in nearby.features.withIndex()) {
            val distance = getDistanceToFeature(location, feature, ruler).distance
            log(
                "  nearby[$index] ${distance.toInt()}m ${(feature as MvtFeature).getText(strings).text}",
            )
        }

        val nearest = nearby.features.first()
        val distance = getDistanceToFeature(location, nearest, ruler).distance
        val text = (nearest as MvtFeature).getText(strings).text
        if (text.isBlank()) return null

        if (distance > NEAREST_POI_ACCEPT_METRES) {
            log("nearest named POI \"$text\" is ${distance.toInt()}m away - too far to trust")
            return null
        }

        return LegacyMarkerNameResult.Named(text, "nearest POI, ${distance.toInt()}m away")
    }

    companion object {
        /**
         * Everything the grid can hold, so that a marker on a mobility POI is found even if the
         * user has that category switched off for callouts.
         */
        private val ALL_CATEGORIES = setOf(PLACES_AND_LANDMARKS_KEY, MOBILITY_KEY)

        /**
         * Trees searched for an OSM id match, in order. POIs first because that's what a marker is
         * normally placed on (transit stops are folded into that tree), then ways, for a marker
         * saved on a road, path or waterway.
         */
        private val ID_MATCH_TREES = listOf(TreeId.POIS, TreeId.ROADS_AND_PATHS)

        /** How far out to look for a named POI when the OSM id doesn't match anything. */
        const val NEAREST_POI_SEARCH_METRES = 250.0

        /**
         * How close that POI has to be before we'll use its name. A marker created from a POI was
         * stored at the POI's own coordinates, so a genuine match is metres away, not hundreds -
         * anything further is more likely to be a different place entirely, and the marker is
         * better off falling back to its estimated address.
         */
        const val NEAREST_POI_ACCEPT_METRES = 50.0

        /** How many of the nearby POIs to log; only the first is ever used. */
        private const val NEAREST_POI_LOG_COUNT = 5

        /** See [budget]. */
        val DEFAULT_BUDGET = 60.seconds
    }
}

/**
 * Translates a legacy `ft-<osmId>` entity key into the feature ids planetiler would have given the
 * same OSM object as a node and as a way.
 *
 * Returns an empty list for keys that aren't OSM-backed at all - legacy `Address` and generic
 * location keys are UUIDs, and there is nothing in a tile to match them against.
 */
internal fun protomapsIdsForLegacyEntityKey(entityKey: String): List<Long> {
    if (!entityKey.startsWith(LEGACY_OSM_KEY_PREFIX)) return emptyList()
    val legacyId = entityKey.removePrefix(LEGACY_OSM_KEY_PREFIX).toLongOrNull() ?: return emptyList()
    return listOf(legacyId * 10 + PLANETILER_NODE, legacyId * 10 + PLANETILER_WAY)
}

private const val LEGACY_OSM_KEY_PREFIX = "ft-"

// planetiler encodes the OSM element type in the last digit of the feature id. 3, for a relation,
// is deliberately absent - see the note on matching in TileLegacyMarkerNamer.
private const val PLANETILER_NODE = 1L
private const val PLANETILER_WAY = 2L
