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
import kotlin.test.assertTrue

/**
 * Tests for [PhotonSearchClient], following the MockEngine pattern established in
 * [OfflineMapManagerTest]: a real Ktor [HttpClient] is backed by a [MockEngine] whose handler
 * both records the [HttpRequestData] it received (for asserting on URL/params/headers) and
 * returns a canned response.
 */
class PhotonSearchClientTest {

    private var capturedRequest: HttpRequestData? = null

    private fun clientReturning(status: HttpStatusCode, body: String): PhotonSearchClient {
        val httpClient = HttpClient(
            MockEngine { request ->
                capturedRequest = request
                respond(content = ByteReadChannel(body), status = status, headers = headersOf())
            },
        )
        return PhotonSearchClient(httpClient, baseUrl = "https://example.test")
    }

    // ----- searchJson() -----

    @Test
    fun searchJson_success_returnsBodyText() = runTest {
        val client = clientReturning(HttpStatusCode.OK, """{"type":"FeatureCollection"}""")

        val result = client.searchJson(query = "coffee")

        assertEquals("""{"type":"FeatureCollection"}""", result)
    }

    @Test
    fun searchJson_usesApiPath() = runTest {
        val client = clientReturning(HttpStatusCode.OK, "{}")

        client.searchJson(query = "coffee")

        assertEquals("/api/", capturedRequest!!.url.encodedPath)
    }

    @Test
    fun searchJson_sendsAllParametersWhenProvided() = runTest {
        val client = clientReturning(HttpStatusCode.OK, "{}")

        client.searchJson(
            query = "coffee",
            latitude = 55.86,
            longitude = -4.25,
            language = "en",
            limit = 10U,
            locationBiasScale = 0.5f,
        )

        val params = capturedRequest!!.url.parameters
        assertEquals("coffee", params.get("q"))
        assertEquals("55.86", params.get("lat"))
        assertEquals("-4.25", params.get("lon"))
        assertEquals("en", params.get("lang"))
        assertEquals("10", params.get("limit"))
        assertEquals("0.5", params.get("location_bias_scale"))
    }

    @Test
    fun searchJson_omitsOptionalParametersWhenNull() = runTest {
        val client = clientReturning(HttpStatusCode.OK, "{}")

        client.searchJson(query = "coffee", latitude = null, longitude = null, language = null)

        val params = capturedRequest!!.url.parameters
        assertEquals("coffee", params.get("q"))
        assertTrue(params.contains("lat") == false, "lat should be omitted, not sent empty")
        assertTrue(params.contains("lon") == false, "lon should be omitted, not sent empty")
        assertTrue(params.contains("lang") == false, "lang should be omitted, not sent empty")
        // limit and location_bias_scale have non-null defaults, so they're always sent.
        assertEquals("5", params.get("limit"))
        assertEquals("0.2", params.get("location_bias_scale"))
    }

    @Test
    fun searchJson_sendsRequiredHeaders() = runTest {
        val client = clientReturning(HttpStatusCode.OK, "{}")

        client.searchJson(query = "coffee")

        assertEquals("max-age=0", capturedRequest!!.headers.get("Cache-Control"))
        assertEquals("keep-alive", capturedRequest!!.headers.get("Connection"))
    }

    @Test
    fun searchJson_notFound_returnsNull() = runTest {
        val client = clientReturning(HttpStatusCode.NotFound, "not found")

        assertNull(client.searchJson(query = "coffee"))
    }

    @Test
    fun searchJson_serverError_returnsNull() = runTest {
        val client = clientReturning(HttpStatusCode.InternalServerError, "boom")

        assertNull(client.searchJson(query = "coffee"))
    }

    // ----- reverseGeocodeJson() -----

    @Test
    fun reverseGeocodeJson_success_returnsBodyText() = runTest {
        val client = clientReturning(HttpStatusCode.OK, """{"type":"FeatureCollection"}""")

        val result = client.reverseGeocodeJson(latitude = 55.86, longitude = -4.25)

        assertEquals("""{"type":"FeatureCollection"}""", result)
    }

    @Test
    fun reverseGeocodeJson_usesReversePath() = runTest {
        val client = clientReturning(HttpStatusCode.OK, "{}")

        client.reverseGeocodeJson(latitude = 55.86, longitude = -4.25)

        assertEquals("/reverse/", capturedRequest!!.url.encodedPath)
    }

    @Test
    fun reverseGeocodeJson_sendsAllParametersWhenProvided() = runTest {
        val client = clientReturning(HttpStatusCode.OK, "{}")

        client.reverseGeocodeJson(latitude = 55.86, longitude = -4.25, language = "en")

        val params = capturedRequest!!.url.parameters
        assertEquals("55.86", params.get("lat"))
        assertEquals("-4.25", params.get("lon"))
        assertEquals("en", params.get("lang"))
    }

    @Test
    fun reverseGeocodeJson_omitsOptionalParametersWhenNull() = runTest {
        val client = clientReturning(HttpStatusCode.OK, "{}")

        client.reverseGeocodeJson(latitude = null, longitude = null, language = null)

        val params = capturedRequest!!.url.parameters
        assertTrue(params.contains("lat") == false)
        assertTrue(params.contains("lon") == false)
        assertTrue(params.contains("lang") == false)
    }

    @Test
    fun reverseGeocodeJson_sendsRequiredHeaders() = runTest {
        val client = clientReturning(HttpStatusCode.OK, "{}")

        client.reverseGeocodeJson(latitude = 55.86, longitude = -4.25)

        assertEquals("max-age=0", capturedRequest!!.headers.get("Cache-Control"))
        assertEquals("keep-alive", capturedRequest!!.headers.get("Connection"))
    }

    @Test
    fun reverseGeocodeJson_notFound_returnsNull() = runTest {
        val client = clientReturning(HttpStatusCode.NotFound, "not found")

        assertNull(client.reverseGeocodeJson(latitude = 55.86, longitude = -4.25))
    }

    @Test
    fun reverseGeocodeJson_serverError_returnsNull() = runTest {
        val client = clientReturning(HttpStatusCode.InternalServerError, "boom")

        assertNull(client.reverseGeocodeJson(latitude = 55.86, longitude = -4.25))
    }
}
