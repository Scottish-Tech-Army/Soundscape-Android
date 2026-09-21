package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [KmpPhotonSearch]. Rather than faking [PhotonSearchClient] (a concrete class), a real
 * instance is built on top of a [MockEngine]-backed [HttpClient] - the same technique used in
 * [PhotonSearchClientTest] and [OfflineMapManagerTest] - so these tests exercise the real
 * delegation and JSON-parsing wiring end to end.
 */
class KmpPhotonSearchTest {

    private var capturedRequest: HttpRequestData? = null

    private fun searchReturning(status: HttpStatusCode, body: String): KmpPhotonSearch {
        val httpClient = HttpClient(
            MockEngine { request ->
                capturedRequest = request
                respond(content = ByteReadChannel(body), status = status, headers = headersOf())
            },
        )
        return KmpPhotonSearch(PhotonSearchClient(httpClient, baseUrl = "https://example.test"))
    }

    private fun featureCollectionJson(name: String): String = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": {"name": "$name"},
              "geometry": {"type": "Point", "coordinates": [-4.25, 55.86]}
            }
          ]
        }
    """.trimIndent()

    // ----- getSearchResults() -----

    @Test
    fun getSearchResults_success_parsesFeatureCollection() = runTest {
        val search = searchReturning(HttpStatusCode.OK, featureCollectionJson("Glasgow"))

        val result = search.getSearchResults(searchString = "coffee")

        assertEquals(1, result?.features?.size)
        assertEquals("Glasgow", result?.features?.get(0)?.properties?.get("name"))
    }

    @Test
    fun getSearchResults_delegatesToClientSearchJson_hittingApiPath() = runTest {
        val search = searchReturning(HttpStatusCode.OK, featureCollectionJson("Glasgow"))

        search.getSearchResults(searchString = "coffee", latitude = 55.86, longitude = -4.25)

        assertEquals("/api/", capturedRequest?.url?.encodedPath)
        assertEquals("coffee", capturedRequest?.url?.parameters?.get("q"))
    }

    @Test
    fun getSearchResults_clientFailure_returnsNullWithoutParsing() = runTest {
        val search = searchReturning(HttpStatusCode.NotFound, "not found")

        val result = search.getSearchResults(searchString = "coffee")

        assertNull(result)
    }

    // ----- reverseGeocodeLocation() -----

    @Test
    fun reverseGeocodeLocation_success_parsesFeatureCollection() = runTest {
        val search = searchReturning(HttpStatusCode.OK, featureCollectionJson("London"))

        val result = search.reverseGeocodeLocation(latitude = 51.5, longitude = -0.1)

        assertEquals(1, result?.features?.size)
        assertEquals("London", result?.features?.get(0)?.properties?.get("name"))
    }

    @Test
    fun reverseGeocodeLocation_delegatesToClientReverseGeocodeJson_hittingReversePath() = runTest {
        val search = searchReturning(HttpStatusCode.OK, featureCollectionJson("London"))

        search.reverseGeocodeLocation(latitude = 51.5, longitude = -0.1)

        assertEquals("/reverse/", capturedRequest?.url?.encodedPath)
        assertEquals("51.5", capturedRequest?.url?.parameters?.get("lat"))
        assertEquals("-0.1", capturedRequest?.url?.parameters?.get("lon"))
    }

    @Test
    fun reverseGeocodeLocation_clientFailure_returnsNullWithoutParsing() = runTest {
        val search = searchReturning(HttpStatusCode.InternalServerError, "boom")

        val result = search.reverseGeocodeLocation(latitude = 51.5, longitude = -0.1)

        assertNull(result)
    }

    @Test
    fun getSearchResults_malformedJson_returnsNull() = runTest {
        // GeoJsonParser.parseFeatureCollection() catches parse exceptions and returns null itself,
        // so a 200 response with an invalid body must surface as null, not throw.
        val search = searchReturning(HttpStatusCode.OK, "not valid json")

        val result = search.getSearchResults(searchString = "coffee")

        assertNull(result)
    }
}
