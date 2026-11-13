package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.PoiRankStrategy
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.deferredToLocationDescription
import org.scottishtecharmy.soundscape.utils.fuzzyCompare

/**
 * The MultiGeocoder dynamically switches between platform, Photon and Local geocoders depending on
 * the user settings and network availability.
 */
class MultiGeocoder(
    val gridState: GridState,
    settlementState: GridState,
    tileSearch: TileSearcher?,
    private val photonGeocoder: PhotonGeocoder,
    platformGeocoder: SoundscapeGeocoder? = null,
    analyticsLogger: (String) -> Unit = {},
    private val processor: (LocationDescription) -> Unit = {},
    private val hasNetwork: () -> Boolean = { false },
    private val geocoderMode: () -> String? = { null },
    poiStrategy: () -> PoiRankStrategy = { PoiRankStrategy.default },
    /**
     * Recognises a search for a type of place - "pharmacy" - rather than for a name. Null when
     * there's nothing to recognise it with, and every search is then for a name.
     */
    private val categoryMatcher: suspend () -> SearchCategoryMatcher? = { null },
    private val onlineTimeoutWithFallbackMs: Long = ONLINE_TIMEOUT_WITH_FALLBACK_MS,
    private val onlineTimeoutWithoutFallbackMs: Long = ONLINE_TIMEOUT_WITHOUT_FALLBACK_MS,
) : SoundscapeGeocoder() {

    private val fusedGeocoder = FusedGeocoder(gridState, photonGeocoder, platformGeocoder)

    /**
     * Exposed so callers who specifically want the tile-derived answer can ask for it rather than
     * whatever [pickGeocoder] would choose. Somewhere already in the loaded grid - a POI the user
     * just tapped in Places Nearby, say - the offline geocoder is both faster and more relevant
     * than a network round trip, and it works with no signal at all.
     */
    val offlineGeocoder =
        OfflineGeocoder(
            gridState,
            settlementState,
            tileSearch,
            analyticsLogger,
            processor,
            poiStrategy
        )

    private fun pickGeocoder(): SoundscapeGeocoder? {
        val settingsChoice = geocoderMode()
        return if (hasNetwork() && (settingsChoice != "Offline"))
            fusedGeocoder
        else
            offlineGeocoder
    }

    /** Whether a search would be made offline - with no network, or the setting says so */
    fun searchesOffline(): Boolean = pickGeocoder() == offlineGeocoder

    override suspend fun getAddressFromLocationName(
        locationName: String,
        nearbyLocation: LngLatAlt,
        localizedStrings: LocalizedStrings?
    ): List<LocationDescription> {

        val results: MutableList<LocationDescription> = mutableListOf()

        val markers = gridState.markerTree?.getAllCollection()
        if (markers != null) {
            val needle = normalizeForSearch(locationName)
            for (marker in markers) {
                val mvt = marker as MvtFeature
                val name = mvt.name
                if (name != null) {
                    val haystack = normalizeForSearch(name)
                    val score = haystack.fuzzyCompare(needle, true)
                    if (score < 0.25) {
                        val ld = mvt.deferredToLocationDescription(LocationSource.OfflineGeocoder)
                            .also(processor)
                        results.add(ld)
                    }
                }
            }
        }

        // A search for a type of place is also searched for as a name, as there may be a place
        // called that - a bar called "The Pharmacy" - but the places of that type come first
        val categoryMatch = try {
            categoryMatcher()?.match(locationName)
        } catch (e: Exception) {
            null
        }
        val geocoder = pickGeocoder()
        val (categoryResults, geocoderResults) = coroutineScope {
            val categorySearch = categoryMatch?.let { match ->
                async {
                    searchByCategory(match.category, match.remainder, nearbyLocation, localizedStrings)
                }
            }
            val nameSearch = async {
                geocoder?.getAddressFromLocationName(
                    locationName,
                    nearbyLocation,
                    localizedStrings
                )
            }
            Pair(categorySearch?.await().orEmpty(), nameSearch.await().orEmpty())
        }

        results.addAll(categoryResults)
        for (result in geocoderResults) {
            val isDuplicate = categoryResults.any {
                (it.name == result.name) &&
                    (gridState.ruler.distance(it.location, result.location) < 100.0)
            }
            if (!isDuplicate) results.add(result)
        }

        return results
    }

    /**
     * The offline maps are always searched, and Photon too whenever it would be for a name. Photon
     * reaches further, but only has places with a name - it has almost no toilets, benches or
     * post boxes - so the two are merged, nearest first.
     */
    override suspend fun searchByCategory(
        category: SearchCategory,
        name: String?,
        nearbyLocation: LngLatAlt,
        localizedStrings: LocalizedStrings?
    ): List<LocationDescription> = coroutineScope {
        val photonSearch = if (pickGeocoder() == fusedGeocoder) {
            async { photonGeocoder.searchByCategory(category, name, nearbyLocation, localizedStrings) }
        } else null
        val offlineResults =
            offlineGeocoder.searchByCategory(category, name, nearbyLocation, localizedStrings)
        val photonResults = photonSearch?.await().orEmpty()

        val ruler = gridState.ruler
        val merged = offlineResults.toMutableList()
        for (result in photonResults) {
            val isDuplicate = offlineResults.any {
                (it.name == result.name) && (ruler.distance(it.location, result.location) < 100.0)
            }
            if (!isDuplicate) merged.add(result)
        }
        merged.sortedBy { ruler.distance(nearbyLocation, it.location) }
    }

    /**
     * The network geocoders' answer on its own, with no falling back to the offline one and no
     * time limit, or null straight away when [searchesOffline].
     */
    suspend fun getOnlineAddressFromLngLat(
        userGeometry: UserGeometry,
        localizedStrings: LocalizedStrings?,
        ignoreHouseNumbers: Boolean
    ): LocationDescription? {
        if (searchesOffline()) return null
        return fusedGeocoder.getAddressFromLngLat(userGeometry, localizedStrings, ignoreHouseNumbers)
    }

    /**
     * A name or address for where [userGeometry] is, from the network geocoders when they answer
     * in time and otherwise from the offline one.
     *
     * The offline geocoder is asked first. It reads the map data already loaded around the user,
     * so it is quick, but it only has an answer when there is some. Whether it has one decides how
     * long the network is worth waiting for:
     *
     *  - an offline answer, and a network: the network is given a short while to do better
     *  - no offline answer, but a network: it is the only source, so it gets its full timeout
     *  - no network, or the setting says offline: the offline answer is used straight away
     *
     * Without the time limit a poor connection - or one that claims to be up but where DNS never
     * answers - held My Location silent for as long as the slowest network geocoder took, and the
     * platform geocoder has no timeout of its own.
     *
     * [accept] is applied to each answer before it's chosen, so that a caller which can't use an
     * answer - one which is somewhere else, say - gets the other one instead.
     */
    suspend fun getAddressFromLngLat(
        userGeometry: UserGeometry,
        localizedStrings: LocalizedStrings?,
        ignoreHouseNumbers: Boolean,
        accept: (LocationDescription?) -> LocationDescription?,
    ): LocationDescription? = withContext(gridState.treeContext) {
        // On the TreeContext throughout: the offline geocoder needs it, and it means the timeout
        // below is always measured in real time
        val offline = accept(
            offlineGeocoder.getAddressFromLngLat(userGeometry, localizedStrings, ignoreHouseNumbers)
        )
        if (searchesOffline()) return@withContext offline

        // The lookup runs in a scope of its own, and only the wait for it is timed. A timeout
        // around the lookup itself can only end when the lookup lets it, and the platform
        // geocoder doesn't: on Android 12 and earlier it's a blocking call.
        val lookup = CoroutineScope(Job()).async(gridState.treeContext) {
            accept(getOnlineAddressFromLngLat(userGeometry, localizedStrings, ignoreHouseNumbers))
        }
        val timeout =
            if (offline != null) onlineTimeoutWithFallbackMs else onlineTimeoutWithoutFallbackMs
        val online = withTimeoutOrNull(timeout) { lookup.await() }
        lookup.cancel()
        online ?: offline
    }

    override suspend fun getAddressFromLngLat(
        userGeometry: UserGeometry,
        localizedStrings: LocalizedStrings?,
        ignoreHouseNumbers: Boolean
    ): LocationDescription? =
        getAddressFromLngLat(userGeometry, localizedStrings, ignoreHouseNumbers) { it }

    companion object {
        // How long the network geocoders are waited for when the offline one has already
        // answered...
        const val ONLINE_TIMEOUT_WITH_FALLBACK_MS = 4000L

        // ...and when it hasn't, so that they are the only hope of a name. Photon's own HTTP
        // timeout is 10 seconds, and this stops the platform geocoder taking longer still.
        const val ONLINE_TIMEOUT_WITHOUT_FALLBACK_MS = 10000L
    }
}
