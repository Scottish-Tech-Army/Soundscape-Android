@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.network.PhotonSearch
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.process
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Minimal fake of [PhotonSearch], tracking whether it was ever invoked. */
private class MultiFakePhotonSearch(
    private val searchResult: FeatureCollection? = FeatureCollection(),
    private val reverseResult: FeatureCollection? = FeatureCollection(),
) : PhotonSearch {
    var searchCallCount = 0
    var reverseCallCount = 0

    override suspend fun getSearchResults(
        searchString: String,
        latitude: Double?,
        longitude: Double?,
        language: String?,
        limit: UInt,
        bias: Float,
    ): FeatureCollection? {
        searchCallCount++
        return searchResult
    }

    override suspend fun reverseGeocodeLocation(
        latitude: Double?,
        longitude: Double?,
        language: String?,
    ): FeatureCollection? {
        reverseCallCount++
        return reverseResult
    }
}

/** Minimal fake of [TileSearcher], the local/offline text-search backend. */
private class FakeTileSearcher(private val results: List<LocationDescription> = emptyList()) :
    TileSearcher {
    var callCount = 0

    override fun search(
        location: LngLatAlt,
        searchString: String,
        localizedStrings: LocalizedStrings?,
        settlementNames: Set<String>
    ): List<LocationDescription> {
        callCount++
        return results
    }
}

private fun multiPhotonFeature(location: LngLatAlt, name: String): Feature {
    val feature = Feature()
    feature.geometry = Point(location)
    feature.properties = hashMapOf(
        "name" to name,
        "osm_key" to "amenity",
        "osm_value" to "cafe",
    )
    return feature
}

class MultiGeocoderTest {

    private val origin = LngLatAlt(-2.657, 51.430)

    // The default (unpopulated) GridState has a zero-valued bounding box, i.e. only the exact
    // point (0,0) is considered "within the grid" by GridState.isLocationWithinGrid - used below
    // to exercise both the "within grid" and "outside grid" paths of the local geocoder.
    private val withinDefaultGridLocation = LngLatAlt(0.0, 0.0)

    private fun buildGridState(): GridState {
        val gridState = GridState()
        gridState.validateContext = false
        return gridState
    }

    private fun buildMultiGeocoder(
        gridState: GridState = buildGridState(),
        settlementState: GridState = buildGridState(),
        tileSearch: TileSearcher? = null,
        photonSearch: MultiFakePhotonSearch = MultiFakePhotonSearch(),
        hasNetwork: () -> Boolean = { false },
        geocoderMode: () -> String? = { null },
        // In production (see GeoEngine.kt) MultiGeocoder is always wired up with
        // `LocationDescription.process()` as its processor - that's what turns a matched
        // marker/feature into a LocationDescription with a usable `.name`. Mirror that default
        // here rather than the class's own no-op default, so these tests reflect real behaviour.
        processor: (LocationDescription) -> Unit = { it.process() },
    ): MultiGeocoder {
        val photonGeocoder = PhotonGeocoder(photonSearch)
        return MultiGeocoder(
            gridState = gridState,
            settlementState = settlementState,
            tileSearch = tileSearch,
            photonGeocoder = photonGeocoder,
            processor = processor,
            hasNetwork = hasNetwork,
            geocoderMode = geocoderMode,
        )
    }

    // ---- getAddressFromLocationName: marker search (always runs, independent of network) --------

    @Test
    fun getAddressFromLocationName_matchingMarker_isIncludedRegardlessOfNetworkState() = runTest {
        val gridState = buildGridState()
        val marker = MvtFeature().apply {
            name = "Granny's House"
            geometry = Point(origin)
        }
        gridState.markerTree = FeatureTree(FeatureCollection().apply { addFeature(marker) })
        val multiGeocoder = buildMultiGeocoder(gridState = gridState, hasNetwork = { false })

        val results = multiGeocoder.getAddressFromLocationName("Granny's House", origin, null)

        assertEquals(1, results.size)
        assertEquals("Granny's House", results[0].name)
    }

    @Test
    fun getAddressFromLocationName_nonMatchingMarker_isExcluded() = runTest {
        val gridState = buildGridState()
        val marker = MvtFeature().apply {
            name = "Completely Different Place"
            geometry = Point(origin)
        }
        gridState.markerTree = FeatureTree(FeatureCollection().apply { addFeature(marker) })
        val multiGeocoder = buildMultiGeocoder(gridState = gridState, hasNetwork = { false })

        val results = multiGeocoder.getAddressFromLocationName("Granny's House", origin, null)

        assertTrue(results.isEmpty())
    }

    // ---- getAddressFromLocationName: geocoder switching -------------------------------------------

    @Test
    fun getAddressFromLocationName_noNetwork_usesLocalTileSearcher() = runTest {
        val localResult = LocationDescription(name = "Local Result", location = origin)
        val tileSearch = FakeTileSearcher(listOf(localResult))
        val photonSearch = MultiFakePhotonSearch()
        val multiGeocoder = buildMultiGeocoder(
            tileSearch = tileSearch,
            photonSearch = photonSearch,
            hasNetwork = { false },
        )

        val results = multiGeocoder.getAddressFromLocationName("Test", origin, null)

        assertEquals(listOf(localResult), results)
        assertEquals(1, tileSearch.callCount)
        assertEquals(0, photonSearch.searchCallCount)
    }

    @Test
    fun getAddressFromLocationName_hasNetworkAndModeNotOffline_usesFusedPhotonGeocoder() = runTest {
        val cafe = multiPhotonFeature(getDestinationCoordinate(origin, 0.0, 5.0), "Costa")
        val photonSearch = MultiFakePhotonSearch(
            searchResult = FeatureCollection().apply { addFeature(cafe) }
        )
        val tileSearch = FakeTileSearcher(listOf(LocationDescription(name = "Should Not Be Used", location = origin)))
        val multiGeocoder = buildMultiGeocoder(
            tileSearch = tileSearch,
            photonSearch = photonSearch,
            hasNetwork = { true },
            geocoderMode = { null },
        )

        val results = multiGeocoder.getAddressFromLocationName("Costa", origin, null)

        assertEquals(1, results.size)
        assertEquals("Costa", results[0].featureName?.text)
        assertEquals(1, photonSearch.searchCallCount)
        assertEquals(0, tileSearch.callCount)
    }

    @Test
    fun getAddressFromLocationName_hasNetworkButOfflineMode_usesLocalTileSearcherNotPhoton() = runTest {
        val localResult = LocationDescription(name = "Local Result", location = origin)
        val tileSearch = FakeTileSearcher(listOf(localResult))
        val photonSearch = MultiFakePhotonSearch()
        val multiGeocoder = buildMultiGeocoder(
            tileSearch = tileSearch,
            photonSearch = photonSearch,
            hasNetwork = { true },
            geocoderMode = { "Offline" },
        )

        val results = multiGeocoder.getAddressFromLocationName("Test", origin, null)

        assertEquals(listOf(localResult), results)
        assertEquals(1, tileSearch.callCount)
        assertEquals(0, photonSearch.searchCallCount)
    }

    // ---- getAddressFromLngLat: geocoder switching --------------------------------------------------

    @Test
    fun getAddressFromLngLat_noNetwork_locationOutsideGrid_returnsNull() = runTest {
        // Local geocoder is picked (no network); the default GridState's zero bounding box
        // doesn't contain `origin`, so OfflineGeocoder bails out immediately.
        val multiGeocoder = buildMultiGeocoder(hasNetwork = { false })

        val result = multiGeocoder.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertNull(result)
    }

    @Test
    fun getAddressFromLngLat_noNetwork_locationWithinGrid_usesLocalGeocoder() = runTest {
        val gridState = buildGridState()
        val poiLocation = getDestinationCoordinate(withinDefaultGridLocation, 90.0, 50.0)
        val poi = MvtFeature().apply {
            name = "Local Cafe"
            geometry = Point(poiLocation)
        }
        gridState.featureTrees[TreeId.POIS.id] =
            FeatureTree(FeatureCollection().apply { addFeature(poi) })
        val multiGeocoder = buildMultiGeocoder(gridState = gridState, hasNetwork = { false })

        val result = multiGeocoder.getAddressFromLngLat(
            UserGeometry(location = withinDefaultGridLocation), null, false
        )

        assertEquals("Local Cafe", result?.name)
    }

    @Test
    fun getAddressFromLngLat_hasNetworkModeNotOffline_usesFusedPhotonResultDirectly() = runTest {
        val cafeLocation = getDestinationCoordinate(origin, 0.0, 5.0)
        val cafe = multiPhotonFeature(cafeLocation, "Costa")
        val photonSearch = MultiFakePhotonSearch(
            reverseResult = FeatureCollection().apply { addFeature(cafe) }
        )
        val multiGeocoder = buildMultiGeocoder(
            photonSearch = photonSearch,
            hasNetwork = { true },
            geocoderMode = { null },
        )

        val result = multiGeocoder.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertEquals(cafe, result?.feature)
        assertEquals(1, photonSearch.reverseCallCount)
    }

    @Test
    fun getAddressFromLngLat_hasNetworkFusedReturnsNull_fallsBackToLocalGeocoder() = runTest {
        val gridState = buildGridState()
        val poiLocation = getDestinationCoordinate(withinDefaultGridLocation, 90.0, 50.0)
        val poi = MvtFeature().apply {
            name = "Local Cafe"
            geometry = Point(poiLocation)
        }
        gridState.featureTrees[TreeId.POIS.id] =
            FeatureTree(FeatureCollection().apply { addFeature(poi) })
        // No results at all from Photon, so FusedGeocoder returns null for this lookup.
        val photonSearch = MultiFakePhotonSearch(reverseResult = null)
        val multiGeocoder = buildMultiGeocoder(
            gridState = gridState,
            photonSearch = photonSearch,
            hasNetwork = { true },
            geocoderMode = { null },
        )

        val result = multiGeocoder.getAddressFromLngLat(
            UserGeometry(location = withinDefaultGridLocation), null, false
        )

        // Falls back to the local geocoder rather than surfacing the fused geocoder's null.
        assertNotNull(result)
        assertEquals("Local Cafe", result.name)
    }

    @Test
    fun getAddressFromLngLat_hasNetworkFusedAndLocalBothReturnNull_returnsNull() = runTest {
        val photonSearch = MultiFakePhotonSearch(reverseResult = null)
        val multiGeocoder = buildMultiGeocoder(
            photonSearch = photonSearch,
            hasNetwork = { true },
            geocoderMode = { null },
        )

        // `origin` is outside the default GridState's zero bounding box, so the local fallback
        // also returns null.
        val result = multiGeocoder.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertNull(result)
    }
}
