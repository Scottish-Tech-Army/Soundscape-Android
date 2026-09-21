package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SUFFIX
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [VectorTileClient], following the MockEngine pattern established in
 * [OfflineMapManagerTest] and [PhotonSearchClientTest].
 */
class VectorTileClientTest {

    private var capturedRequest: HttpRequestData? = null

    private fun clientReturning(status: HttpStatusCode, bytes: ByteArray): VectorTileClient {
        val httpClient = HttpClient(
            MockEngine { request ->
                capturedRequest = request
                respond(content = ByteReadChannel(bytes), status = status, headers = headersOf())
            },
        )
        return VectorTileClient(httpClient, baseUrl = "https://example.test")
    }

    private fun clientThrowing(exception: Exception): VectorTileClient {
        val httpClient = HttpClient(
            MockEngine {
                throw exception
            },
        )
        return VectorTileClient(httpClient, baseUrl = "https://example.test")
    }

    @Test
    fun getTile_success_returnsRawBytes() = runTest {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val client = clientReturning(HttpStatusCode.OK, bytes)

        val result = client.getTile(x = 1, y = 2, z = 3)

        assertEquals(bytes.toList(), result?.toList())
    }

    @Test
    fun getTile_buildsExpectedUrl() = runTest {
        val client = clientReturning(HttpStatusCode.OK, byteArrayOf())

        client.getTile(x = 1, y = 2, z = 3)

        assertEquals(
            "https://example.test/$PROTOMAPS_SERVER_PATH/3/1/2.$PROTOMAPS_SUFFIX",
            capturedRequest?.url?.toString(),
        )
    }

    @Test
    fun getTile_trimsTrailingSlashFromBaseUrl() = runTest {
        val httpClient = HttpClient(
            MockEngine { request ->
                capturedRequest = request
                respond(content = ByteReadChannel(byteArrayOf()), status = HttpStatusCode.OK, headers = headersOf())
            },
        )
        val client = VectorTileClient(httpClient, baseUrl = "https://example.test/")

        client.getTile(x = 1, y = 2, z = 3)

        assertEquals(
            "https://example.test/$PROTOMAPS_SERVER_PATH/3/1/2.$PROTOMAPS_SUFFIX",
            capturedRequest?.url?.toString(),
        )
    }

    @Test
    fun getTile_notFound_returnsNull() = runTest {
        val client = clientReturning(HttpStatusCode.NotFound, byteArrayOf())

        assertNull(client.getTile(x = 1, y = 2, z = 3))
    }

    @Test
    fun getTile_serverError_returnsNull() = runTest {
        val client = clientReturning(HttpStatusCode.InternalServerError, byteArrayOf())

        assertNull(client.getTile(x = 1, y = 2, z = 3))
    }

    @Test
    fun getTile_networkException_isCaughtAndReturnsNull() = runTest {
        val client = clientThrowing(RuntimeException("network failure"))

        assertNull(client.getTile(x = 1, y = 2, z = 3))
    }
}
