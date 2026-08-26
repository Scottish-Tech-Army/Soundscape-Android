@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.network.PhotonSearch
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Minimal fake of the platform (Android/iOS) geocoder - just returns whatever it's told to. */
private class FakePlatformGeocoder(
    private val locationNameResult: List<LocationDescription>? = null,
    private val lngLatResult: LocationDescription? = null,
) : SoundscapeGeocoder() {
    override suspend fun getAddressFromLocationName(
        locationName: String,
        nearbyLocation: LngLatAlt,
        localizedStrings: LocalizedStrings?
    ): List<LocationDescription>? = locationNameResult

    override suspend fun getAddressFromLngLat(
        userGeometry: UserGeometry,
        localizedStrings: LocalizedStrings?,
        ignoreHouseNumbers: Boolean
    ): LocationDescription? = lngLatResult
}

/** Minimal fake of [PhotonSearch] - see PhotonGeocoderTest for a more thoroughly exercised copy. */
private class FusedFakePhotonSearch(
    private val searchResult: FeatureCollection? = FeatureCollection(),
    private val reverseResult: FeatureCollection? = FeatureCollection(),
) : PhotonSearch {
    override suspend fun getSearchResults(
        searchString: String,
        latitude: Double?,
        longitude: Double?,
        language: String?,
        limit: UInt,
        bias: Float,
    ): FeatureCollection? = searchResult

    override suspend fun reverseGeocodeLocation(
        latitude: Double?,
        longitude: Double?,
        language: String?,
    ): FeatureCollection? = reverseResult
}

private fun fusedPhotonFeature(location: LngLatAlt, name: String): Feature {
    val feature = Feature()
    feature.geometry = Point(location)
    feature.properties = hashMapOf(
        "name" to name,
        "osm_key" to "amenity",
        "osm_value" to "cafe",
    )
    return feature
}

class FusedGeocoderTest {

    private val origin = LngLatAlt(-2.657, 51.430)

    private fun buildGridState(): GridState {
        val gridState = GridState()
        gridState.validateContext = false
        gridState.ruler = CheapRuler(origin.latitude)
        return gridState
    }

    private fun photonGeocoderReturning(
        vararg features: Feature,
        markResultsAsStreetNumber: Boolean = false,
    ): PhotonGeocoder {
        val fakeSearch = FusedFakePhotonSearch(
            searchResult = FeatureCollection().apply { features.forEach { addFeature(it) } },
            reverseResult = FeatureCollection().apply { features.forEach { addFeature(it) } },
        )
        return PhotonGeocoder(
            fakeSearch,
            processor = {
                // PhotonGeocoder itself only ever populates `featureName` (a TextForFeature) - in
                // production the `.name` field is filled in by the LocationDescription.process()
                // processor that GeoEngine wires up. Mirror that here so FusedGeocoder's use of
                // `.name` when merging results behaves as it does in production.
                it.name = it.featureName?.text ?: it.name
                if (markResultsAsStreetNumber) it.locationType = LocationType.StreetNumber
            },
        )
    }

    private fun photonGeocoderReturningNothing(): PhotonGeocoder =
        PhotonGeocoder(FusedFakePhotonSearch(searchResult = null, reverseResult = null))

    // ---- getAddressFromLocationName -------------------------------------------------------------

    @Test
    fun getAddressFromLocationName_noPlatformGeocoder_returnsPhotonResultsOnly() = runTest {
        val cafe = fusedPhotonFeature(getDestinationCoordinate(origin, 0.0, 5.0), "Costa")
        val fused = FusedGeocoder(buildGridState(), photonGeocoderReturning(cafe))

        val results = fused.getAddressFromLocationName("Costa", origin, null)

        assertEquals(1, results.size)
        assertEquals("Costa", results[0].featureName?.text)
    }

    @Test
    fun getAddressFromLocationName_platformResultsWithoutStreetNumber_areDropped() = runTest {
        val platformResult = LocationDescription(
            name = "Some City",
            location = origin,
            locationType = LocationType.City,
        )
        val cafe = fusedPhotonFeature(getDestinationCoordinate(origin, 0.0, 5.0), "Costa")
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturning(cafe),
            FakePlatformGeocoder(locationNameResult = listOf(platformResult)),
        )

        val results = fused.getAddressFromLocationName("Costa", origin, null)

        // Only a StreetNumber-typed platform result is ever used - everything else is left to Photon.
        assertEquals(1, results.size)
        assertEquals("Costa", results[0].featureName?.text)
    }

    @Test
    fun getAddressFromLocationName_platformStreetNumber_isAddedFirst() = runTest {
        val platformResult = LocationDescription(
            name = "Name Before Rebuild",
            location = origin,
            locationType = LocationType.StreetNumber,
            description = "10 Test Street, Test City",
        )
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturningNothing(),
            FakePlatformGeocoder(locationNameResult = listOf(platformResult)),
        )

        val results = fused.getAddressFromLocationName("10 Test Street", origin, null)

        assertEquals(1, results.size)
        assertSame(platformResult, results[0])
        // The query contains a number, so the name is rebuilt from the description.
        assertEquals("10 Test Street", results[0].name)
    }

    @Test
    fun getAddressFromLocationName_queryWithoutNumber_leavesPlatformStreetNameUnchanged() = runTest {
        val platformResult = LocationDescription(
            name = "Original Name",
            location = origin,
            locationType = LocationType.StreetNumber,
            description = "10 Test Street, Test City",
        )
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturningNothing(),
            FakePlatformGeocoder(locationNameResult = listOf(platformResult)),
        )

        val results = fused.getAddressFromLocationName("Test Street", origin, null)

        assertEquals(1, results.size)
        assertEquals("Original Name", results[0].name)
    }

    @Test
    fun getAddressFromLocationName_nearbyPhotonStreetNumber_mergesIntoPlatformResult() = runTest {
        val platformResult = LocationDescription(
            name = "Platform House",
            location = origin,
            locationType = LocationType.StreetNumber,
        )
        // 50m away - within the 100m merge threshold.
        val photonLocation = getDestinationCoordinate(origin, 90.0, 50.0)
        val photonFeature = fusedPhotonFeature(photonLocation, "Photon House")
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturning(photonFeature, markResultsAsStreetNumber = true),
            FakePlatformGeocoder(locationNameResult = listOf(platformResult)),
        )

        val results = fused.getAddressFromLocationName("House", origin, null)

        // The photon result is merged into the platform result in place, not appended separately.
        assertEquals(1, results.size)
        assertSame(platformResult, results[0])
        assertEquals("Photon House", results[0].name)
        assertEquals(photonLocation, results[0].location)
    }

    @Test
    fun getAddressFromLocationName_distantPhotonStreetNumber_isKeptSeparate() = runTest {
        val platformResult = LocationDescription(
            name = "Platform House",
            location = origin,
            locationType = LocationType.StreetNumber,
        )
        // 500m away - outside the 100m merge threshold.
        val photonLocation = getDestinationCoordinate(origin, 90.0, 500.0)
        val photonFeature = fusedPhotonFeature(photonLocation, "Photon House")
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturning(photonFeature, markResultsAsStreetNumber = true),
            FakePlatformGeocoder(locationNameResult = listOf(platformResult)),
        )

        val results = fused.getAddressFromLocationName("House", origin, null)

        assertEquals(2, results.size)
        assertSame(platformResult, results[0])
        assertEquals("Platform House", results[0].name)
        assertEquals("Photon House", results[1].featureName?.text)
    }

    @Test
    fun getAddressFromLocationName_nonStreetNumberPhotonResults_areAlwaysAppended() = runTest {
        val platformResult = LocationDescription(
            name = "Platform House",
            location = origin,
            locationType = LocationType.StreetNumber,
        )
        // Close by, but not classified as a StreetNumber result - should never merge.
        val photonLocation = getDestinationCoordinate(origin, 90.0, 5.0)
        val photonFeature = fusedPhotonFeature(photonLocation, "Costa")
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturning(photonFeature, markResultsAsStreetNumber = false),
            FakePlatformGeocoder(locationNameResult = listOf(platformResult)),
        )

        val results = fused.getAddressFromLocationName("House", origin, null)

        assertEquals(2, results.size)
        assertEquals("Platform House", results[0].name)
        assertEquals("Costa", results[1].featureName?.text)
    }

    @Test
    fun getAddressFromLocationName_neitherGeocoderReturnsResults_isEmpty() = runTest {
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturningNothing(),
            FakePlatformGeocoder(locationNameResult = null),
        )

        val results = fused.getAddressFromLocationName("Nowhere", origin, null)

        assertTrue(results.isEmpty())
    }

    // ---- getAddressFromLngLat --------------------------------------------------------------------

    @Test
    fun getAddressFromLngLat_noPlatformGeocoder_returnsPhotonResult() = runTest {
        val cafe = fusedPhotonFeature(getDestinationCoordinate(origin, 0.0, 5.0), "Costa")
        val fused = FusedGeocoder(buildGridState(), photonGeocoderReturning(cafe))

        val result = fused.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertSame(cafe, result?.feature)
    }

    @Test
    fun getAddressFromLngLat_platformStreetNumber_isPreferredOverPhoton() = runTest {
        val platformResult = LocationDescription(
            name = "Platform House",
            location = origin,
            locationType = LocationType.StreetNumber,
        )
        val cafe = fusedPhotonFeature(getDestinationCoordinate(origin, 0.0, 5.0), "Costa")
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturning(cafe),
            FakePlatformGeocoder(lngLatResult = platformResult),
        )

        val result = fused.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertSame(platformResult, result)
    }

    @Test
    fun getAddressFromLngLat_platformNonStreetNumber_fallsBackToPhoton() = runTest {
        val platformResult = LocationDescription(
            name = "Some City",
            location = origin,
            locationType = LocationType.City,
        )
        val cafe = fusedPhotonFeature(getDestinationCoordinate(origin, 0.0, 5.0), "Costa")
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturning(cafe),
            FakePlatformGeocoder(lngLatResult = platformResult),
        )

        val result = fused.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertSame(cafe, result?.feature)
    }

    @Test
    fun getAddressFromLngLat_platformReturnsNull_fallsBackToPhoton() = runTest {
        val cafe = fusedPhotonFeature(getDestinationCoordinate(origin, 0.0, 5.0), "Costa")
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturning(cafe),
            FakePlatformGeocoder(lngLatResult = null),
        )

        val result = fused.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertSame(cafe, result?.feature)
    }

    @Test
    fun getAddressFromLngLat_neitherGeocoderReturnsResult_isNull() = runTest {
        val fused = FusedGeocoder(
            buildGridState(),
            photonGeocoderReturningNothing(),
            FakePlatformGeocoder(lngLatResult = null),
        )

        val result = fused.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertNull(result)
    }
}
