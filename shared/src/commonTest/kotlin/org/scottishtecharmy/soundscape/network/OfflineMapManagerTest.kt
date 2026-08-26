package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.NearbyExtractsState
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [OfflineMapManager]. It hard-codes its own `CoroutineScope(Dispatchers.Default + Job())`
 * (not injectable), so tests can't use kotlinx-coroutines-test virtual time - instead they poll
 * real state with a bounded [waitUntil], matching the pattern used in CalloutControllerTest and
 * RoutePlayerTest.
 *
 * [ManifestClient] is a concrete class wrapping a real Ktor [HttpClient], so it's exercised
 * end-to-end here via a [MockEngine]-backed client rather than faked. The manifest is always
 * fetched as gzip (MANIFEST_NAME ends in ".gz"), so response bodies are gzip-compressed with
 * [gzip] to mirror what [ManifestClient.getManifestJson] expects to decompress.
 */
class OfflineMapManagerTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "offline-map-manager-test-${System.nanoTime()}")
        tempDir.mkdirs()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // ---------- test doubles / fixtures ----------

    /** Records every call made to it; [behavior] decides the result (and can write bytes to destPath). */
    private class RecordingFileDownloader(
        private val behavior: suspend (url: String, destPath: String, onProgress: (Int) -> Unit) -> DownloadResultCommon,
    ) : FileDownloaderInterface {
        val calls = mutableListOf<Pair<String, String>>()
        override suspend fun download(
            url: String,
            destPath: String,
            scope: CoroutineScope,
            onProgress: (Int) -> Unit,
        ): DownloadResultCommon {
            calls.add(url to destPath)
            return behavior(url, destPath, onProgress)
        }
    }

    private fun manifestClient(status: HttpStatusCode, bodyBytes: ByteArray): ManifestClient {
        val httpClient = HttpClient(
            MockEngine { _ ->
                respond(content = ByteReadChannel(bodyBytes), status = status, headers = headersOf())
            },
        )
        return ManifestClient(httpClient, baseUrl = "https://example.test")
    }

    /** gzip-compresses [text], matching what [ManifestClient.getManifestJson] decompresses. */
    private fun gzip(text: String): ByteArray {
        val buffer = Buffer()
        GzipSink(buffer).buffer().use { it.writeUtf8(text) }
        return buffer.readByteArray()
    }

    private fun squareFeatureJson(
        name: String,
        filename: String,
        minLng: Double,
        minLat: Double,
        maxLng: Double,
        maxLat: Double,
    ): String = """
        {
          "type": "Feature",
          "properties": {"name": "$name", "filename": "$filename"},
          "geometry": {
            "type": "Polygon",
            "coordinates": [[[$minLng,$minLat],[$maxLng,$minLat],[$maxLng,$maxLat],[$minLng,$maxLat],[$minLng,$minLat]]]
          }
        }
    """.trimIndent()

    private fun multiPolygonFeatureJson(
        name: String,
        filename: String,
        squares: List<DoubleArray>, // each entry: minLng, minLat, maxLng, maxLat
    ): String {
        val rings = squares.joinToString(",") { (minLng, minLat, maxLng, maxLat) ->
            "[[[$minLng,$minLat],[$maxLng,$minLat],[$maxLng,$maxLat],[$minLng,$maxLat],[$minLng,$minLat]]]"
        }
        return """
            {
              "type": "Feature",
              "properties": {"name": "$name", "filename": "$filename"},
              "geometry": {"type": "MultiPolygon", "coordinates": [$rings]}
            }
        """.trimIndent()
    }

    private fun manifestJson(vararg featureJsons: String): String =
        """{"type":"FeatureCollection","features":[${featureJsons.joinToString(",")}]}"""

    private suspend fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!condition()) delay(5)
        }
    }

    private fun noopDownloader() = RecordingFileDownloader { _, _, _ -> DownloadResultCommon.Success }

    // ----- initial state -----

    @Test
    fun initialState_beforeAnyMethodCalled() {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        assertNull(manager.manifest.value)
        assertEquals(NearbyExtractsState.Loading, manager.nearbyExtractsState.value)
        assertEquals(emptyList(), manager.downloadedExtracts.value)
        assertEquals(DownloadStateCommon.Idle, manager.downloadState.value)
    }

    // ----- refresh() -----

    @Test
    fun refresh_success_populatesManifestAndLoadedState() = runBlocking {
        val json = manifestJson(
            squareFeatureJson("Glasgow", "glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
            squareFeatureJson("London", "london-gb.pmtiles", -0.2, 51.4, -0.0, 51.6),
        )
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(json)),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.nearbyExtractsState.value !is NearbyExtractsState.Loading }

        assertEquals(2, manager.manifest.value?.features?.size)
        val loaded = manager.nearbyExtractsState.value as NearbyExtractsState.Loaded
        assertEquals(2, loaded.nearbyExtracts.features.size)
        // refreshDownloaded() also runs as part of refresh(); tempDir is empty.
        assertEquals(emptyList(), manager.downloadedExtracts.value)
    }

    @Test
    fun refresh_httpError_setsErrorStateAndLeavesManifestNull() = runBlocking {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.NotFound, ByteArray(0)),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.nearbyExtractsState.value !is NearbyExtractsState.Loading }

        assertEquals(NearbyExtractsState.Error, manager.nearbyExtractsState.value)
        assertNull(manager.manifest.value)
    }

    @Test
    fun refresh_bodyNotValidGzip_setsErrorState() = runBlocking {
        // Not actually gzip-compressed - GzipSource decompression itself throws.
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, "not gzip data".encodeToByteArray()),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.nearbyExtractsState.value !is NearbyExtractsState.Loading }

        assertEquals(NearbyExtractsState.Error, manager.nearbyExtractsState.value)
        assertNull(manager.manifest.value)
    }

    @Test
    fun refresh_gzipDecompressesToInvalidJson_setsErrorState() = runBlocking {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("not { valid json")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.nearbyExtractsState.value !is NearbyExtractsState.Loading }

        assertEquals(NearbyExtractsState.Error, manager.nearbyExtractsState.value)
        assertNull(manager.manifest.value)
    }

    @Test
    fun refresh_validButEmptyManifest_isLoadedWithZeroFeatures() = runBlocking {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("""{"type":"FeatureCollection","features":[]}""")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.nearbyExtractsState.value !is NearbyExtractsState.Loading }

        // Distinct from the error case: a syntactically valid empty manifest is a successful
        // (if unhelpful) load, not an error.
        val loaded = manager.nearbyExtractsState.value as NearbyExtractsState.Loaded
        assertEquals(0, loaded.nearbyExtracts.features.size)
        assertEquals(0, manager.manifest.value?.features?.size)
    }

    @Test
    fun refresh_alsoScansForDownloadedExtracts() = runBlocking {
        File(tempDir, "already-downloaded.pmtiles").writeText("data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("""{"type":"FeatureCollection","features":[]}""")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.downloadedExtracts.value.isNotEmpty() }

        assertEquals(listOf(File(tempDir, "already-downloaded.pmtiles").path), manager.downloadedExtracts.value)
    }

    // ----- getExtractsContaining() -----

    @Test
    fun getExtractsContaining_beforeManifestLoaded_returnsEmptyList() {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        assertEquals(emptyList(), manager.getExtractsContaining(LngLatAlt(-4.25, 55.85)))
    }

    @Test
    fun getExtractsContaining_returnsOnlyThePolygonThatContainsTheLocation() = runBlocking {
        val json = manifestJson(
            squareFeatureJson("Glasgow", "glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
            squareFeatureJson("London", "london-gb.pmtiles", -0.2, 51.4, -0.0, 51.6),
        )
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(json)),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )
        manager.refresh()
        waitUntil { manager.manifest.value != null }

        val insideGlasgow = manager.getExtractsContaining(LngLatAlt(-4.25, 55.85))
        assertEquals(listOf("Glasgow"), insideGlasgow.map { it.properties?.get("name") })

        val outsideBoth = manager.getExtractsContaining(LngLatAlt(10.0, 10.0))
        assertTrue(outsideBoth.isEmpty())
    }

    @Test
    fun getExtractsContaining_multiPolygon_matchesPointInEitherRing() = runBlocking {
        val json = manifestJson(
            multiPolygonFeatureJson(
                "Archipelago",
                "archipelago.pmtiles",
                listOf(
                    doubleArrayOf(-10.0, 10.0, -9.0, 11.0),
                    doubleArrayOf(20.0, 20.0, 21.0, 21.0),
                ),
            ),
        )
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(json)),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )
        manager.refresh()
        waitUntil { manager.manifest.value != null }

        assertEquals(1, manager.getExtractsContaining(LngLatAlt(-9.5, 10.5)).size)
        assertEquals(1, manager.getExtractsContaining(LngLatAlt(20.5, 20.5)).size)
        assertTrue(manager.getExtractsContaining(LngLatAlt(0.0, 0.0)).isEmpty())
    }

    // ----- isDownloaded() / refreshDownloaded() -----

    @Test
    fun isDownloaded_matchesByBasenameRegardlessOfPathPrefix() {
        File(tempDir, "region.pmtiles").writeText("data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.refreshDownloaded()

        assertTrue(manager.isDownloaded("region.pmtiles"))
        assertTrue(manager.isDownloaded("some/remote/prefix/region.pmtiles"))
        assertFalse(manager.isDownloaded("other.pmtiles"))
    }

    @Test
    fun refreshDownloaded_onlyListsPmtilesFiles() {
        File(tempDir, "region.pmtiles").writeText("data")
        File(tempDir, "region.pmtiles.downloading").writeText("partial")
        File(tempDir, "notes.txt").writeText("irrelevant")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.refreshDownloaded()

        assertEquals(listOf(File(tempDir, "region.pmtiles").path), manager.downloadedExtracts.value)
    }

    // ----- startDownload() / cancelDownload() -----

    @Test
    fun startDownload_success_movesTempFileToFinalPathAndUpdatesDownloadedExtracts() = runBlocking {
        val downloader = RecordingFileDownloader { _, destPath, onProgress ->
            onProgress(500)
            File(destPath).writeText("pmtiles-bytes")
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.startDownload("region.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        assertTrue(File(tempDir, "region.pmtiles").exists())
        assertFalse(File(tempDir, "region.pmtiles.downloading").exists())
        assertEquals(listOf(File(tempDir, "region.pmtiles").path), manager.downloadedExtracts.value)
        assertEquals(
            listOf("https://example.test/extracts/region.pmtiles" to File(tempDir, "region.pmtiles.downloading").path),
            downloader.calls,
        )
    }

    @Test
    fun startDownload_trimsTrailingSlashFromBaseUrl_andStripsPathPrefixFromDestFilename() = runBlocking {
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            File(destPath).writeText("bytes")
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.path,
            "https://example.test/extracts/", // trailing slash
        )

        manager.startDownload("sub/region.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        assertEquals("https://example.test/extracts/sub/region.pmtiles", downloader.calls.single().first)
        assertTrue(File(tempDir, "region.pmtiles").exists()) // "sub/" prefix stripped for the local filename
    }

    @Test
    fun startDownload_whileAlreadyActive_isIgnored() = runBlocking {
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            delay(300)
            File(destPath).writeText("bytes")
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.startDownload("first.pmtiles")
        manager.startDownload("second.pmtiles") // must be a no-op: a download is already active

        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        assertEquals(1, downloader.calls.size)
        assertTrue(downloader.calls.single().first.endsWith("first.pmtiles"))
        assertEquals(listOf(File(tempDir, "first.pmtiles").path), manager.downloadedExtracts.value)
    }

    @Test
    fun startDownload_nonRetryableHttpError_setsErrorStateWithoutRetrying() = runBlocking {
        val downloader = RecordingFileDownloader { _, _, _ -> DownloadResultCommon.HttpError(500, "boom") }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.startDownload("region.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Error }

        val error = manager.downloadState.value as DownloadStateCommon.Error
        assertTrue(error.message.contains("500"))
        assertEquals(1, downloader.calls.size)
        assertFalse(File(tempDir, "region.pmtiles.downloading").exists())
    }

    @Test
    fun startDownload_503ThenSuccess_retriesAndSucceeds() = runBlocking {
        var attempt = 0
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            attempt++
            if (attempt == 1) {
                DownloadResultCommon.HttpError(503, "still caching")
            } else {
                File(destPath).writeText("bytes")
                DownloadResultCommon.Success
            }
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.path,
            "https://example.test/extracts",
        )

        // A small extractSize makes the post-503 backoff on the *first* retry 0 seconds
        // (cachingDuration = (extractSize / 10_000_000).toInt()), so this resolves fast.
        manager.startDownload("region.pmtiles", extractSize = 5_000_000.0)
        waitUntil(timeoutMs = 10_000) { manager.downloadState.value is DownloadStateCommon.Success }

        assertEquals(2, downloader.calls.size)
        assertTrue(File(tempDir, "region.pmtiles").exists())
    }

    @Test
    fun cancelDownload_cancelsInProgressDownload_andDeletesTempFile() = runBlocking {
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            File(destPath).writeText("partial") // simulates bytes already streamed to disk
            delay(30_000) // never resolves on its own; must be cancelled
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.startDownload("region.pmtiles")
        waitUntil { File(tempDir, "region.pmtiles.downloading").exists() }

        manager.cancelDownload()
        waitUntil { manager.downloadState.value is DownloadStateCommon.Canceled }

        assertFalse(File(tempDir, "region.pmtiles.downloading").exists())
        assertFalse(File(tempDir, "region.pmtiles").exists())
    }

    @Test
    fun cancelDownload_withNoActiveDownload_isANoOp() {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )

        manager.cancelDownload() // must not throw

        assertEquals(DownloadStateCommon.Idle, manager.downloadState.value)
    }

    // ----- deleteExtract() -----

    @Test
    fun deleteExtract_removesFileAndSidecarAndRefreshesDownloadedExtracts() {
        File(tempDir, "region.pmtiles").writeText("data")
        File(tempDir, "region.pmtiles.geojson").writeText("{}")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )
        manager.refreshDownloaded()
        assertEquals(1, manager.downloadedExtracts.value.size)

        manager.deleteExtract(File(tempDir, "region.pmtiles").path)

        assertFalse(File(tempDir, "region.pmtiles").exists())
        assertFalse(File(tempDir, "region.pmtiles.geojson").exists())
        assertTrue(manager.downloadedExtracts.value.isEmpty())
    }

    @Test
    fun deleteExtract_nonexistentPath_doesNotThrowOrChangeDownloadedExtracts() {
        File(tempDir, "region.pmtiles").writeText("data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )
        manager.refreshDownloaded()
        assertEquals(1, manager.downloadedExtracts.value.size)

        manager.deleteExtract(File(tempDir, "does-not-exist.pmtiles").path) // must not throw

        // deleteExtract() only calls refreshDownloaded() after a *successful* delete, so a
        // failed delete of a nonexistent file leaves the previously-cached list untouched.
        assertEquals(1, manager.downloadedExtracts.value.size)
        assertTrue(File(tempDir, "region.pmtiles").exists())
    }

    // ----- deleteExtractByFeature() -----

    @Test
    fun deleteExtractByFeature_matchesByExactFilename() {
        File(tempDir, "region.pmtiles").writeText("data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )
        manager.refreshDownloaded()
        val feature = Feature().apply { properties = hashMapOf("filename" to "region.pmtiles") }

        manager.deleteExtractByFeature(feature)

        assertFalse(File(tempDir, "region.pmtiles").exists())
        assertTrue(manager.downloadedExtracts.value.isEmpty())
    }

    /**
     * Manifest "filename" values carry a two-segment server-side prefix (e.g. a date and a hash)
     * ahead of the logical name, matching the on-disk-vs-manifest naming split already used by
     * AndroidOfflineMapsManager.logicalBaseNameFor() (`filename.substringAfter("-").substringAfter("-")`).
     * A downloaded file named just "glasgow-gb.pmtiles" must still be found via that fallback when
     * the manifest's filename is "20240101-abc123-glasgow-gb.pmtiles".
     */
    @Test
    fun deleteExtractByFeature_fallsBackToStrippedPrefixWhenExactNameDoesNotMatch() {
        File(tempDir, "glasgow-gb.pmtiles").writeText("data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )
        manager.refreshDownloaded()
        val feature = Feature().apply {
            properties = hashMapOf("filename" to "20240101-abc123-glasgow-gb.pmtiles")
        }

        manager.deleteExtractByFeature(feature)

        assertFalse(File(tempDir, "glasgow-gb.pmtiles").exists())
        assertTrue(manager.downloadedExtracts.value.isEmpty())
    }

    @Test
    fun deleteExtractByFeature_noFilenameProperty_isANoOp() {
        File(tempDir, "region.pmtiles").writeText("data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )
        manager.refreshDownloaded()

        manager.deleteExtractByFeature(Feature()) // no properties at all - must not throw

        assertTrue(File(tempDir, "region.pmtiles").exists())
        assertEquals(1, manager.downloadedExtracts.value.size)
    }

    @Test
    fun deleteExtractByFeature_noMatchingDownload_isANoOp() {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.path,
            "https://example.test/extracts",
        )
        manager.refreshDownloaded() // nothing downloaded
        val feature = Feature().apply { properties = hashMapOf("filename" to "region.pmtiles") }

        manager.deleteExtractByFeature(feature) // must not throw

        assertTrue(manager.downloadedExtracts.value.isEmpty())
    }
}
