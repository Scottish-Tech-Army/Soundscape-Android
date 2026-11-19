package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.network.DownloadResultCommon
import org.scottishtecharmy.soundscape.network.GeoJsonParser
import org.scottishtecharmy.soundscape.network.createFileDownloader
import org.scottishtecharmy.soundscape.utils.getMetadata
import java.io.File
import java.io.IOException

/**
 * The Android-only parts of offline maps. The download logic itself is the shared
 * OfflineMapManager, tested in OfflineMapManagerTest; what is left here is Android's HTTP
 * download, and reading the extract sidecars that Android wrote with Moshi before it moved to the
 * shared manager.
 */
class AndroidOfflineMapTest {

    private fun downloader(status: HttpStatusCode, bytes: ByteArray, contentLength: Long) =
        createFileDownloader(
            HttpClient(MockEngine { _ ->
                respond(
                    content = ByteReadChannel(bytes),
                    status = status,
                    headers = headersOf(HttpHeaders.ContentLength, contentLength.toString()),
                )
            })
        )

    private fun download(status: HttpStatusCode, bytes: ByteArray, contentLength: Long): Pair<DownloadResultCommon, File> {
        val dest = File.createTempFile("extract", ".downloading")
        val result = runBlocking {
            downloader(status, bytes, contentLength)
                .download("https://example.test/extract", dest.path, CoroutineScope(Dispatchers.IO)) {}
        }
        return result to dest
    }

    @Test
    fun fullBodyIsSaved() {
        val (result, dest) = download(HttpStatusCode.OK, ByteArray(1000) { 1 }, 1000)
        try {
            assertEquals(DownloadResultCommon.Success, result)
            assertEquals(1000L, dest.length())
        } finally {
            dest.delete()
        }
    }

    @Test
    fun httpErrorIsReported() {
        val (result, dest) = download(HttpStatusCode.ServiceUnavailable, ByteArray(0), 0)
        try {
            assertEquals(503, (result as DownloadResultCommon.HttpError).code)
        } finally {
            dest.delete()
        }
    }

    /**
     * A body that ends short of its Content-Length - what an intermediary truncating the response,
     * or HTTP/2's clean EOF, looks like - must fail, not be saved as a finished download.
     */
    @Test(expected = IOException::class)
    fun bodyShorterThanContentLengthFails() {
        val dest = File.createTempFile("extract", ".downloading")
        try {
            runBlocking {
                downloader(HttpStatusCode.OK, ByteArray(500) { 1 }, 1000)
                    .download("https://example.test/extract", dest.path, CoroutineScope(Dispatchers.IO)) {}
            }
        } finally {
            dest.delete()
        }
    }

    private fun manifestEntry(): Feature = Feature().apply {
        geometry = Polygon(
            arrayListOf(
                LngLatAlt(-4.3, 55.8), LngLatAlt(-4.2, 55.8), LngLatAlt(-4.2, 55.9),
                LngLatAlt(-4.3, 55.9), LngLatAlt(-4.3, 55.8),
            )
        )
        properties = hashMapOf(
            "name" to "Glasgow",
            "filename" to "20260820-1354-glasgow-gb.pmtiles",
            "extract-size" to 123456789,
            "schema" to 3,
            "cities" to listOf("Glasgow", "Paisley"),
        )
    }

    /** Extracts downloaded before the move have sidecars written by Moshi; they must still name them. */
    @Test
    fun sharedParserReadsAMoshiSidecar() {
        val json = GeoMoshi.registerAdapters(Moshi.Builder()).build()
            .adapter(Feature::class.java)
            .toJson(manifestEntry())

        val parsed = GeoJsonParser.parseFeature(json)!!

        assertEquals("Glasgow", parsed.properties?.get("name"))
        assertEquals("20260820-1354-glasgow-gb.pmtiles", parsed.properties?.get("filename"))
        assertEquals(123456789.0, parsed.properties?.get("extract-size").toString().toDouble(), 0.0)
        assertEquals("3", parsed.properties?.get("schema").toString().removeSuffix(".0"))
        assertEquals(listOf("Glasgow", "Paisley"), parsed.properties?.get("cities"))
        assertTrue(parsed.geometry is Polygon)
    }

    /** The support email still lists extracts with Moshi (findExtracts), so it must read the new sidecars. */
    @Test
    fun moshiReadsASharedSidecar() {
        val extract = File.createTempFile("glasgow-gb.v1", ".pmtiles")
        val sidecar = File("${extract.path}.geojson")
        try {
            sidecar.writeText(GeoJsonParser.toJson(manifestEntry()))

            val parsed = getMetadata(extract.path)!!

            assertEquals("Glasgow", parsed.properties?.get("name"))
            assertEquals("20260820-1354-glasgow-gb.pmtiles", parsed.properties?.get("filename"))
        } finally {
            sidecar.delete()
            extract.delete()
        }
    }
}
