package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.network.PhotonSearch
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Hand-rolled fake of [PhotonSearch] - a plain suspend interface, trivially fakeable without a
 * mocking library. Captures the arguments of the last call of each method so tests can assert on
 * what PhotonGeocoder passed through, and can be told to throw to exercise the try/catch paths.
 */
private class FakePhotonSearch(
    private val searchResult: FeatureCollection? = FeatureCollection(),
    private val reverseResult: FeatureCollection? = FeatureCollection(),
    private val throwOnSearch: Boolean = false,
    private val throwOnReverse: Boolean = false,
) : PhotonSearch {
    var searchCallCount = 0
    var reverseCallCount = 0
    var lastSearchString: String? = null
    var lastSearchLatitude: Double? = null
    var lastSearchLongitude: Double? = null
    var lastSearchLanguage: String? = null
    var lastReverseLatitude: Double? = null
    var lastReverseLongitude: Double? = null

    override suspend fun getSearchResults(
        searchString: String,
        latitude: Double?,
        longitude: Double?,
        language: String?,
        limit: UInt,
        bias: Float,
    ): FeatureCollection? {
        searchCallCount++
        if (throwOnSearch) throw RuntimeException("simulated network failure")
        lastSearchString = searchString
        lastSearchLatitude = latitude
        lastSearchLongitude = longitude
        lastSearchLanguage = language
        return searchResult
    }

    override suspend fun reverseGeocodeLocation(
        latitude: Double?,
        longitude: Double?,
        language: String?,
    ): FeatureCollection? {
        reverseCallCount++
        if (throwOnReverse) throw RuntimeException("simulated network failure")
        lastReverseLatitude = latitude
        lastReverseLongitude = longitude
        return reverseResult
    }
}

/** Builds a synthetic Photon-style GeoJSON Feature with a Point geometry and osm_key/osm_value tags. */
private fun photonFeature(
    location: LngLatAlt,
    name: String?,
    osmKey: String = "amenity",
    osmValue: String = "cafe",
    extraProperties: Map<String, Any?> = emptyMap(),
): Feature {
    val feature = Feature()
    feature.geometry = Point(location)
    val properties = HashMap<String, Any?>()
    if (name != null) properties["name"] = name
    properties["osm_key"] = osmKey
    properties["osm_value"] = osmValue
    properties.putAll(extraProperties)
    feature.properties = properties
    return feature
}

class PhotonGeocoderTest {

    private val origin = LngLatAlt(-2.657, 51.430)

    // ---- getAddressFromLocationName -----------------------------------------------------------

    @Test
    fun getAddressFromLocationName_nullSearchResult_returnsNull() = runTest {
        val fakeSearch = FakePhotonSearch(searchResult = null)
        val geocoder = PhotonGeocoder(fakeSearch)

        val result = geocoder.getAddressFromLocationName("Tesco", origin, null)

        assertNull(result)
    }

    @Test
    fun getAddressFromLocationName_networkFailure_returnsNull() = runTest {
        val fakeSearch = FakePhotonSearch(throwOnSearch = true)
        val geocoder = PhotonGeocoder(fakeSearch)

        val result = geocoder.getAddressFromLocationName("Tesco", origin, null)

        assertNull(result)
    }

    @Test
    fun getAddressFromLocationName_emptyFeatureCollection_returnsEmptyList() = runTest {
        val fakeSearch = FakePhotonSearch(searchResult = FeatureCollection())
        val geocoder = PhotonGeocoder(fakeSearch)

        val result = geocoder.getAddressFromLocationName("Tesco", origin, null)

        assertEquals(emptyList(), result)
    }

    @Test
    fun getAddressFromLocationName_passesQueryLocationAndLanguageThrough() = runTest {
        val fakeSearch = FakePhotonSearch(searchResult = FeatureCollection())
        val geocoder = PhotonGeocoder(fakeSearch, languageProvider = { "de" })

        geocoder.getAddressFromLocationName("Tesco Express", origin, null)

        assertEquals("Tesco Express", fakeSearch.lastSearchString)
        assertEquals(origin.latitude, fakeSearch.lastSearchLatitude)
        assertEquals(origin.longitude, fakeSearch.lastSearchLongitude)
        assertEquals("de", fakeSearch.lastSearchLanguage)
    }

    @Test
    fun getAddressFromLocationName_logsAnalyticsEvent() = runTest {
        val logged = mutableListOf<String>()
        val fakeSearch = FakePhotonSearch(searchResult = FeatureCollection())
        val geocoder = PhotonGeocoder(fakeSearch, analyticsLogger = { logged.add(it) })

        geocoder.getAddressFromLocationName("Tesco", origin, null)

        assertEquals(listOf("photonGeocode"), logged)
    }

    @Test
    fun getAddressFromLocationName_singleFeature_mapsToLocationDescription() = runTest {
        val featureLocation = getDestinationCoordinate(origin, 45.0, 20.0)
        val feature = photonFeature(featureLocation, "Test Cafe")
        val fakeSearch = FakePhotonSearch(
            searchResult = FeatureCollection().apply { addFeature(feature) }
        )
        val processed = mutableListOf<LocationDescription>()
        val geocoder = PhotonGeocoder(fakeSearch, processor = { processed.add(it) })

        val result = geocoder.getAddressFromLocationName("Cafe", origin, null)

        assertEquals(1, result?.size)
        val description = result!![0]
        assertEquals(LocationSource.PhotonGeocoder, description.source)
        assertEquals(featureLocation, description.location)
        assertEquals(feature, description.feature)
        assertEquals("Test Cafe", description.featureName?.text)
        assertEquals(listOf(description), processed)
    }

    @Test
    fun getAddressFromLocationName_duplicateNameWithin100m_isCollapsed() = runTest {
        val first = photonFeature(getDestinationCoordinate(origin, 0.0, 5.0), "Costa")
        val second = photonFeature(getDestinationCoordinate(origin, 0.0, 10.0), "Costa")
        val fakeSearch = FakePhotonSearch(
            searchResult = FeatureCollection().apply {
                addFeature(first)
                addFeature(second)
            }
        )
        val geocoder = PhotonGeocoder(fakeSearch)

        val result = geocoder.getAddressFromLocationName("Costa", origin, null)

        assertEquals(1, result?.size)
        assertEquals(first, result!![0].feature)
    }

    @Test
    fun getAddressFromLocationName_sameNameOver100mApart_areNotDeduplicated() = runTest {
        val first = photonFeature(getDestinationCoordinate(origin, 0.0, 5.0), "Costa")
        val second = photonFeature(getDestinationCoordinate(origin, 0.0, 500.0), "Costa")
        val fakeSearch = FakePhotonSearch(
            searchResult = FeatureCollection().apply {
                addFeature(first)
                addFeature(second)
            }
        )
        val geocoder = PhotonGeocoder(fakeSearch)

        val result = geocoder.getAddressFromLocationName("Costa", origin, null)

        assertEquals(2, result?.size)
    }

    @Test
    fun getAddressFromLocationName_differentNamesSameLocation_areNotDeduplicated() = runTest {
        val here = getDestinationCoordinate(origin, 0.0, 5.0)
        val first = photonFeature(here, "Costa")
        val second = photonFeature(here, "Starbucks")
        val fakeSearch = FakePhotonSearch(
            searchResult = FeatureCollection().apply {
                addFeature(first)
                addFeature(second)
            }
        )
        val geocoder = PhotonGeocoder(fakeSearch)

        val result = geocoder.getAddressFromLocationName("Coffee", origin, null)

        assertEquals(2, result?.size)
    }

    /**
     * A Photon result with no "name" property must not surface the literal string "null"/"Null"
     * as if it were a real feature name - it should fall back to MvtFeature.getText()'s
     * class-based description. `?.get("name")` returning genuine `null` must stay `null` all the
     * way through to `mvt.name`, not get coerced into the string "null" by an unguarded
     * `.toString()`.
     */
    @Test
    fun getAddressFromLocationName_featureWithoutNameProperty_fallsBackToClassBasedText() =
        runTest {
            val feature = photonFeature(
                getDestinationCoordinate(origin, 0.0, 5.0),
                name = null,
                osmKey = "highway",
                osmValue = "residential",
            )
            val fakeSearch = FakePhotonSearch(
                searchResult = FeatureCollection().apply { addFeature(feature) }
            )
            val geocoder = PhotonGeocoder(fakeSearch)

            val result = geocoder.getAddressFromLocationName("residential road", origin, null)

            assertEquals(1, result?.size)
            // localizedStrings is null here (as it is throughout this test file), so this is
            // MvtFeature.getText()'s "OSM Feature <class> <subClass>" fallback text used when no
            // real localization is available - see its own doc comment. The point of this
            // assertion is what it does NOT say: not "null"/"Null", and it does carry the
            // class-derived "residential_street" text, proving the class-based fallback ran.
            assertEquals("OSM Feature residential_street null", result!![0].featureName?.text)
        }

    // ---- getAddressFromLngLat -------------------------------------------------------------------

    @Test
    fun getAddressFromLngLat_nullReverseResult_returnsNull() = runTest {
        val fakeSearch = FakePhotonSearch(reverseResult = null)
        val geocoder = PhotonGeocoder(fakeSearch)

        val result = geocoder.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertNull(result)
    }

    @Test
    fun getAddressFromLngLat_networkFailure_returnsNull() = runTest {
        val fakeSearch = FakePhotonSearch(throwOnReverse = true)
        val geocoder = PhotonGeocoder(fakeSearch)

        val result = geocoder.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertNull(result)
    }

    @Test
    fun getAddressFromLngLat_emptyFeatureCollection_returnsNull() = runTest {
        val fakeSearch = FakePhotonSearch(reverseResult = FeatureCollection())
        val geocoder = PhotonGeocoder(fakeSearch)

        val result = geocoder.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertNull(result)
    }

    @Test
    fun getAddressFromLngLat_firstFeature_mapsToLocationDescription() = runTest {
        val featureLocation = getDestinationCoordinate(origin, 90.0, 5.0)
        val feature = photonFeature(featureLocation, "10 Test Street")
        val fakeSearch = FakePhotonSearch(
            reverseResult = FeatureCollection().apply { addFeature(feature) }
        )
        val processed = mutableListOf<LocationDescription>()
        val geocoder = PhotonGeocoder(fakeSearch, processor = { processed.add(it) })

        val result = geocoder.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertEquals(LocationSource.PhotonGeocoder, result?.source)
        assertEquals(featureLocation, result?.location)
        assertEquals(listOf(result), processed)
    }

    @Test
    fun getAddressFromLngLat_onlyUsesFirstFeatureOfCollection() = runTest {
        val first = photonFeature(getDestinationCoordinate(origin, 0.0, 5.0), "First")
        val second = photonFeature(getDestinationCoordinate(origin, 0.0, 10.0), "Second")
        val fakeSearch = FakePhotonSearch(
            reverseResult = FeatureCollection().apply {
                addFeature(first)
                addFeature(second)
            }
        )
        val processed = mutableListOf<LocationDescription>()
        val geocoder = PhotonGeocoder(fakeSearch, processor = { processed.add(it) })

        geocoder.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertEquals(1, processed.size)
        assertEquals(first, processed[0].feature)
    }

    @Test
    fun getAddressFromLngLat_usesUserLocationWhenNotMapMatched() = runTest {
        val fakeSearch = FakePhotonSearch(reverseResult = FeatureCollection())
        val geocoder = PhotonGeocoder(fakeSearch)

        geocoder.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertEquals(origin.latitude, fakeSearch.lastReverseLatitude)
        assertEquals(origin.longitude, fakeSearch.lastReverseLongitude)
    }

    @Test
    fun getAddressFromLngLat_prefersMapMatchedLocationOverUserLocation() = runTest {
        val matchedPoint = getDestinationCoordinate(origin, 180.0, 8.0)
        val userGeometry = UserGeometry(
            location = origin,
            mapMatchedLocation = PointAndDistanceAndHeading(point = matchedPoint),
        )
        val fakeSearch = FakePhotonSearch(reverseResult = FeatureCollection())
        val geocoder = PhotonGeocoder(fakeSearch)

        geocoder.getAddressFromLngLat(userGeometry, null, false)

        assertEquals(matchedPoint.latitude, fakeSearch.lastReverseLatitude)
        assertEquals(matchedPoint.longitude, fakeSearch.lastReverseLongitude)
        assertTrue(matchedPoint.latitude != origin.latitude || matchedPoint.longitude != origin.longitude)
    }

    @Test
    fun getAddressFromLngLat_logsAnalyticsEvent() = runTest {
        val logged = mutableListOf<String>()
        val fakeSearch = FakePhotonSearch(reverseResult = FeatureCollection())
        val geocoder = PhotonGeocoder(fakeSearch, analyticsLogger = { logged.add(it) })

        geocoder.getAddressFromLngLat(UserGeometry(location = origin), null, false)

        assertEquals(listOf("photonReverseGeocode"), logged)
    }
}
