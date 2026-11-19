package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.GzipSink
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.scottishtecharmy.soundscape.geoengine.WANTED_EXTRACT_SCHEMA
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.mapstyle.PmTilesFixture
import org.scottishtecharmy.soundscape.platform.systemFileSystem
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.NearbyExtractsState
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

    private lateinit var tempDir: Path

    @BeforeTest
    fun setUp() {
        tempDir = "build/offlineMapManagerTest-${Random.nextLong().let { if (it < 0) -it else it }}".toPath()
        systemFileSystem.createDirectories(tempDir)
    }

    @AfterTest
    fun tearDown() {
        // OfflineMapManager validates downloaded extracts on a background scope of its own, which
        // can still have one open when a test finishes - and Windows won't delete an open file. So
        // give it a moment to finish rather than failing the test in its clean up.
        runBlocking {
            for (attempt in 1..20) {
                try {
                    systemFileSystem.deleteRecursively(tempDir)
                    return@runBlocking
                } catch (e: IOException) {
                    if (attempt == 20) throw e
                    delay(50)
                }
            }
        }
    }

    private fun writeFile(name: String, text: String) {
        systemFileSystem.write(tempDir / name) { writeUtf8(text) }
    }

    private fun fileExists(name: String): Boolean = systemFileSystem.exists(tempDir / name)

    private fun filePath(name: String): String = (tempDir / name).toString()

    /** A minimal but valid extract, as a download has to pass validation to be published. */
    private val validExtract = PmTilesFixture.build(hasTileEntry = true)

    /**
     * The path of the one downloaded copy of [logicalName], which is saved as
     * "<logicalName>.v<timestamp>.pmtiles", or null if there isn't one.
     */
    private fun downloadedFile(logicalName: String): String? {
        val pattern = Regex("""${Regex.escape(logicalName)}\.v\d+\.pmtiles""")
        val matches = systemFileSystem.list(tempDir).filter { pattern.matches(it.name) }
        assertTrue(matches.size <= 1, "more than one copy of $logicalName: $matches")
        return matches.singleOrNull()?.toString()
    }

    private fun downloadingFiles(): List<String> =
        systemFileSystem.list(tempDir).map { it.name }.filter { it.endsWith(".downloading") }

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
        val sink = GzipSink(buffer).buffer()
        sink.writeUtf8(text)
        sink.close()
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
            tempDir.toString(),
            "https://example.test/extracts",
        )

        assertNull(manager.manifest.value)
        assertEquals(NearbyExtractsState.Loading, manager.nearbyExtractsState.value)
        assertEquals(emptyList(), manager.downloadedExtracts.value)
        assertEquals(DownloadStateCommon.Idle, manager.downloadState.value)
    }

    // ----- refresh() -----

    @Test
    fun refresh_success_populatesManifestAndLoadedState() = runBlocking<Unit> {
        val json = manifestJson(
            squareFeatureJson("Glasgow", "glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
            squareFeatureJson("London", "london-gb.pmtiles", -0.2, 51.4, -0.0, 51.6),
        )
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(json)),
            noopDownloader(),
            tempDir.toString(),
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
    fun refresh_httpError_setsErrorStateAndLeavesManifestNull() = runBlocking<Unit> {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.NotFound, ByteArray(0)),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.nearbyExtractsState.value !is NearbyExtractsState.Loading }

        assertEquals(NearbyExtractsState.Error, manager.nearbyExtractsState.value)
        assertNull(manager.manifest.value)
    }

    @Test
    fun refresh_bodyNotValidGzip_setsErrorState() = runBlocking<Unit> {
        // Not actually gzip-compressed - GzipSource decompression itself throws.
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, "not gzip data".encodeToByteArray()),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.nearbyExtractsState.value !is NearbyExtractsState.Loading }

        assertEquals(NearbyExtractsState.Error, manager.nearbyExtractsState.value)
        assertNull(manager.manifest.value)
    }

    @Test
    fun refresh_gzipDecompressesToInvalidJson_setsErrorState() = runBlocking<Unit> {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("not { valid json")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.nearbyExtractsState.value !is NearbyExtractsState.Loading }

        assertEquals(NearbyExtractsState.Error, manager.nearbyExtractsState.value)
        assertNull(manager.manifest.value)
    }

    @Test
    fun refresh_validButEmptyManifest_isLoadedWithZeroFeatures() = runBlocking<Unit> {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("""{"type":"FeatureCollection","features":[]}""")),
            noopDownloader(),
            tempDir.toString(),
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
    fun refresh_alsoScansForDownloadedExtracts() = runBlocking<Unit> {
        writeFile("already-downloaded.pmtiles", "data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("""{"type":"FeatureCollection","features":[]}""")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.downloadedExtracts.value.isNotEmpty() }

        assertEquals(listOf(filePath("already-downloaded.pmtiles")), manager.downloadedExtracts.value)
    }

    // ----- getExtractsContaining() -----

    @Test
    fun getExtractsContaining_beforeManifestLoaded_returnsEmptyList() {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        assertEquals(emptyList(), manager.getExtractsContaining(LngLatAlt(-4.25, 55.85)))
    }

    @Test
    fun getExtractsContaining_returnsOnlyThePolygonThatContainsTheLocation() = runBlocking<Unit> {
        val json = manifestJson(
            squareFeatureJson("Glasgow", "glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
            squareFeatureJson("London", "london-gb.pmtiles", -0.2, 51.4, -0.0, 51.6),
        )
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(json)),
            noopDownloader(),
            tempDir.toString(),
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
    fun getExtractsContaining_multiPolygon_matchesPointInEitherRing() = runBlocking<Unit> {
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
            tempDir.toString(),
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
        writeFile("region.pmtiles", "data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refreshDownloaded()

        assertTrue(manager.isDownloaded("region.pmtiles"))
        assertTrue(manager.isDownloaded("some/remote/prefix/region.pmtiles"))
        assertFalse(manager.isDownloaded("other.pmtiles"))
    }

    @Test
    fun refreshDownloaded_onlyListsPmtilesFiles() {
        writeFile("region.pmtiles", "data")
        writeFile("region.pmtiles.downloading", "partial")
        writeFile("notes.txt", "irrelevant")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refreshDownloaded()

        assertEquals(listOf(filePath("region.pmtiles")), manager.downloadedExtracts.value)
    }

    // ----- startDownload() / cancelDownload() -----

    @Test
    fun startDownload_success_movesTempFileToFinalPathAndUpdatesDownloadedExtracts() = runBlocking<Unit> {
        val downloader = RecordingFileDownloader { _, destPath, onProgress ->
            onProgress(500)
            systemFileSystem.write(destPath.toPath()) { write(validExtract) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.startDownload("region.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        val path = assertNotNull(downloadedFile("region"))
        assertTrue(downloadingFiles().isEmpty())
        assertEquals(listOf(path), manager.downloadedExtracts.value)
        assertEquals(
            listOf("https://example.test/extracts/region.pmtiles" to "$path.downloading"),
            downloader.calls,
        )
    }

    @Test
    fun startDownload_trimsTrailingSlashFromBaseUrl_andStripsPathPrefixFromDestFilename() = runBlocking<Unit> {
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            systemFileSystem.write(destPath.toPath()) { write(validExtract) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts/", // trailing slash
        )

        manager.startDownload("sub/region.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        assertEquals("https://example.test/extracts/sub/region.pmtiles", downloader.calls.single().first)
        assertNotNull(downloadedFile("region")) // "sub/" prefix stripped for the local filename
    }

    @Test
    fun startDownload_whileAlreadyActive_isIgnored() = runBlocking<Unit> {
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            delay(300)
            systemFileSystem.write(destPath.toPath()) { write(validExtract) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.startDownload("first.pmtiles")
        manager.startDownload("second.pmtiles") // must be a no-op: a download is already active

        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        assertEquals(1, downloader.calls.size)
        assertTrue(downloader.calls.single().first.endsWith("first.pmtiles"))
        assertEquals(listOf(downloadedFile("first")), manager.downloadedExtracts.value)
    }

    @Test
    fun startDownload_nonRetryableHttpError_setsErrorStateWithoutRetrying() = runBlocking<Unit> {
        val downloader = RecordingFileDownloader { _, _, _ -> DownloadResultCommon.HttpError(500, "boom") }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.startDownload("region.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Error }

        val error = manager.downloadState.value as DownloadStateCommon.Error
        assertTrue(error.message.contains("500"))
        assertEquals(1, downloader.calls.size)
        assertTrue(downloadingFiles().isEmpty())
    }

    @Test
    fun startDownload_503ThenSuccess_retriesAndSucceeds() = runBlocking<Unit> {
        var attempt = 0
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            attempt++
            if (attempt == 1) {
                DownloadResultCommon.HttpError(503, "still caching")
            } else {
                systemFileSystem.write(destPath.toPath()) { write(validExtract) }
                DownloadResultCommon.Success
            }
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )

        // A small extractSize makes the post-503 backoff on the *first* retry 0 seconds
        // (extractSize / 10_000_000 seconds), so this resolves fast.
        manager.startDownload("region.pmtiles", extractSize = validExtract.size.toDouble())
        waitUntil(timeoutMs = 10_000) { manager.downloadState.value is DownloadStateCommon.Success }

        assertEquals(2, downloader.calls.size)
        assertNotNull(downloadedFile("region"))
    }

    @Test
    fun cancelDownload_cancelsInProgressDownload_andDeletesTempFile() = runBlocking<Unit> {
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            systemFileSystem.write(destPath.toPath()) { writeUtf8("partial") } // simulates bytes already streamed to disk
            delay(30_000) // never resolves on its own; must be cancelled
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.startDownload("region.pmtiles")
        waitUntil { downloadingFiles().isNotEmpty() }

        manager.cancelDownload()
        waitUntil { manager.downloadState.value is DownloadStateCommon.Canceled }

        assertTrue(downloadingFiles().isEmpty())
        assertNull(downloadedFile("region"))
    }

    @Test
    fun startDownload_afterCancel_waitsForCancelledDownloadToUnwind() = runBlocking<Unit> {
        // #1288: the cancelled download takes a while to unwind (here a NonCancellable
        // delay; in reality a backoff sleep or a slow connection). A retry of the same
        // extract started straight after must not have its temp file deleted or its Success
        // overwritten by the cancelled download's belated cleanup.
        var attempt = 0
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            attempt++
            if (attempt == 1) {
                try {
                    delay(30_000)
                } finally {
                    withContext(NonCancellable) { delay(500) }
                }
                DownloadResultCommon.Success
            } else {
                systemFileSystem.write(destPath.toPath()) { write(validExtract) }
                DownloadResultCommon.Success
            }
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.startDownload("region.pmtiles")
        waitUntil { downloader.calls.size == 1 }
        manager.cancelDownload()
        manager.startDownload("region.pmtiles")

        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }
        // Give the cancelled download's cleanup time to run, were it not joined first.
        delay(700)

        assertEquals(DownloadStateCommon.Success, manager.downloadState.value)
        assertNotNull(downloadedFile("region"))
        assertEquals(2, downloader.calls.size)
    }

    @Test
    fun cancelDownload_withNoActiveDownload_isANoOp() {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.cancelDownload() // must not throw

        assertEquals(DownloadStateCommon.Idle, manager.downloadState.value)
    }

    // ----- deleteExtract() -----

    @Test
    fun deleteExtract_removesFileAndSidecarAndRefreshesDownloadedExtracts() {
        writeFile("region.pmtiles", "data")
        writeFile("region.pmtiles.geojson", "{}")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )
        manager.refreshDownloaded()
        assertEquals(1, manager.downloadedExtracts.value.size)

        manager.deleteExtract(filePath("region.pmtiles"))

        assertFalse(fileExists("region.pmtiles"))
        assertFalse(fileExists("region.pmtiles.geojson"))
        assertTrue(manager.downloadedExtracts.value.isEmpty())
    }

    @Test
    fun deleteExtract_nonexistentPath_doesNotThrowOrChangeDownloadedExtracts() {
        writeFile("region.pmtiles", "data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )
        manager.refreshDownloaded()
        assertEquals(1, manager.downloadedExtracts.value.size)

        manager.deleteExtract(filePath("does-not-exist.pmtiles")) // must not throw

        // deleteExtract() only calls refreshDownloaded() after a *successful* delete, so a
        // failed delete of a nonexistent file leaves the previously-cached list untouched.
        assertEquals(1, manager.downloadedExtracts.value.size)
        assertTrue(fileExists("region.pmtiles"))
    }

    // ----- deleteExtractByFeature() -----

    @Test
    fun deleteExtractByFeature_matchesByExactFilename() {
        writeFile("region.pmtiles", "data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )
        manager.refreshDownloaded()
        val feature = Feature().apply { properties = hashMapOf("filename" to "region.pmtiles") }

        manager.deleteExtractByFeature(feature)

        assertFalse(fileExists("region.pmtiles"))
        assertTrue(manager.downloadedExtracts.value.isEmpty())
    }

    /**
     * Manifest "filename" values carry a two-segment server-side prefix (e.g. a date and a hash)
     * ahead of the logical name, matching the on-disk-vs-manifest naming split already used by
     * [org.scottishtecharmy.soundscape.utils.logicalExtractName].
     * A downloaded file named just "glasgow-gb.pmtiles" must still be found via that fallback when
     * the manifest's filename is "20240101-abc123-glasgow-gb.pmtiles".
     */
    @Test
    fun deleteExtractByFeature_fallsBackToStrippedPrefixWhenExactNameDoesNotMatch() {
        writeFile("glasgow-gb.pmtiles", "data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )
        manager.refreshDownloaded()
        val feature = Feature().apply {
            properties = hashMapOf("filename" to "20240101-abc123-glasgow-gb.pmtiles")
        }

        manager.deleteExtractByFeature(feature)

        assertFalse(fileExists("glasgow-gb.pmtiles"))
        assertTrue(manager.downloadedExtracts.value.isEmpty())
    }

    // ----- downloadedExtractsFc -----

    /**
     * The reported bug: the extracts are regenerated server-side with a new build prefix, so the
     * manifest filename stops matching the name the extract was downloaded under, and the list
     * showed "20240101-abc123-glasgow-gb" instead of "Glasgow".
     */
    @Test
    fun downloadedExtractsFc_namesExtractFromManifestRegeneratedUnderANewPrefix() = runBlocking<Unit> {
        writeFile("20240101-abc123-glasgow-gb.pmtiles", "data")
        val json = manifestJson(
            squareFeatureJson("Glasgow", "20240202-def456-glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
        )
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(json)),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.downloadedExtractsFc.value.features.isNotEmpty() }

        val downloaded = manager.downloadedExtractsFc.value.features.single()
        assertEquals("Glasgow", downloaded.properties?.get("name"))
    }

    /** With a sidecar, the name survives the manifest being unavailable entirely. */
    @Test
    fun downloadedExtractsFc_namesExtractFromSidecarWithNoManifest() = runBlocking<Unit> {
        writeFile("20240101-abc123-glasgow-gb.pmtiles", "data")
        writeFile(
            "20240101-abc123-glasgow-gb.pmtiles.geojson",
            squareFeatureJson("Glasgow", "20240101-abc123-glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
        )
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.ServiceUnavailable, ByteArray(0)),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.downloadedExtractsFc.value.features.isNotEmpty() }

        val downloaded = manager.downloadedExtractsFc.value.features.single()
        assertEquals("Glasgow", downloaded.properties?.get("name"))
    }

    /** No sidecar and no manifest match: fall back to the filename rather than showing nothing. */
    @Test
    fun downloadedExtractsFc_fallsBackToFilenameWhenThereIsNoMetadata() = runBlocking<Unit> {
        writeFile("20240101-abc123-glasgow-gb.pmtiles", "data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(manifestJson())),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.downloadedExtractsFc.value.features.isNotEmpty() }

        val downloaded = manager.downloadedExtractsFc.value.features.single()
        assertEquals("20240101-abc123-glasgow-gb", downloaded.properties?.get("name"))
    }

    /** Annotating a downloaded extract must not write local state into the manifest's own copy. */
    @Test
    fun downloadedExtractsFc_doesNotAnnotateTheManifestFeature() = runBlocking<Unit> {
        writeFile("20240101-abc123-glasgow-gb.pmtiles", "data")
        val json = manifestJson(
            squareFeatureJson("Glasgow", "20240101-abc123-glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
        )
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(json)),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.downloadedExtractsFc.value.features.isNotEmpty() }

        assertEquals(false, manager.downloadedExtractsFc.value.features.single().properties?.get("usable"))
        assertNull(manager.manifest.value?.features?.single()?.properties?.get("usable"))
    }

    /**
     * The test extract has no "soundscape:schema" in it (it isn't even a valid pmtiles file), so
     * it counts as schema 0 - older than a manifest entry built with the schema the app wants.
     */
    @Test
    fun downloadedExtractsFc_flagsExtractOlderThanTheManifestSchema() = runBlocking<Unit> {
        writeFile("20240101-abc123-glasgow-gb.pmtiles", "data")
        val json = manifestJson(
            squareFeatureJson("Glasgow", "20240202-def456-glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9)
                .replace("\"filename\"", "\"schema\": $WANTED_EXTRACT_SCHEMA, \"filename\""),
        )
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(json)),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.downloadedExtractsFc.value.features.isNotEmpty() }

        assertEquals(true, manager.downloadedExtractsFc.value.features.single().properties?.get("outdated"))
    }

    /** A manifest from before the schema existed - i.e. just fresher OSM data - flags nothing. */
    @Test
    fun downloadedExtractsFc_doesNotFlagExtractWhenTheManifestHasNoSchema() = runBlocking<Unit> {
        writeFile("20240101-abc123-glasgow-gb.pmtiles", "data")
        val json = manifestJson(
            squareFeatureJson("Glasgow", "20240202-def456-glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
        )
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(json)),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.downloadedExtractsFc.value.features.isNotEmpty() }

        assertEquals(false, manager.downloadedExtractsFc.value.features.single().properties?.get("outdated"))
    }

    @Test
    fun startDownload_writesMetadataSidecarNextToTheExtract() = runBlocking<Unit> {
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            systemFileSystem.write(destPath.toPath()) { write(validExtract) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )
        val feature = GeoJsonParser.parseFeature(
            squareFeatureJson("Glasgow", "20240101-abc123-glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
        )!!

        manager.startDownload("20240101-abc123-glasgow-gb.pmtiles", feature = feature)
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        val path = assertNotNull(downloadedFile("glasgow-gb"))
        val stored = GeoJsonParser.parseFeature(
            systemFileSystem.read("$path.geojson".toPath()) { readUtf8() },
        )
        assertEquals("Glasgow", stored?.properties?.get("name"))
        assertEquals("20240101-abc123-glasgow-gb.pmtiles", stored?.properties?.get("filename"))
    }

    /**
     * "Update" on a downloaded extract hands back the entry it was downloaded with, whose filename
     * carries the build prefix of the manifest of the day. That build is usually gone from the
     * server by then, so the update has to be made against the current manifest entry.
     */
    @Test
    fun startDownload_updatingAnOlderBuild_downloadsWhatTheManifestCallsItNow() = runBlocking<Unit> {
        writeFile("20240101-abc123-glasgow-gb.pmtiles", "old")
        val json = manifestJson(
            squareFeatureJson("Glasgow", "20240202-def456-glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
        )
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            systemFileSystem.write(destPath.toPath()) { write(validExtract) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(json)),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )
        manager.refresh()
        waitUntil { manager.manifest.value != null }
        val staleFeature = GeoJsonParser.parseFeature(
            squareFeatureJson("Glasgow", "20240101-abc123-glasgow-gb.pmtiles", -4.3, 55.8, -4.2, 55.9),
        )!!

        manager.startDownload("20240101-abc123-glasgow-gb.pmtiles", feature = staleFeature)
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        assertEquals(
            "https://example.test/extracts/20240202-def456-glasgow-gb.pmtiles",
            downloader.calls.single().first,
        )
        val path = assertNotNull(downloadedFile("glasgow-gb"))
        assertFalse(fileExists("20240101-abc123-glasgow-gb.pmtiles"))
        val stored = GeoJsonParser.parseFeature(
            systemFileSystem.read("$path.geojson".toPath()) { readUtf8() },
        )
        assertEquals("20240202-def456-glasgow-gb.pmtiles", stored?.properties?.get("filename"))
    }

    /** No manifest entry to resolve against - fall back to the filename the caller asked for. */
    @Test
    fun startDownload_extractNotInTheManifest_downloadsTheFilenameGiven() = runBlocking<Unit> {
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            systemFileSystem.write(destPath.toPath()) { write(validExtract) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip(manifestJson())),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )
        manager.refresh()
        waitUntil { manager.manifest.value != null }

        manager.startDownload("20240101-abc123-glasgow-gb.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        assertEquals(
            "https://example.test/extracts/20240101-abc123-glasgow-gb.pmtiles",
            downloader.calls.single().first,
        )
        assertNotNull(downloadedFile("glasgow-gb"))
    }

    /**
     * Re-downloading an extract that the server has regenerated gets a new, build-prefixed
     * filename, so the previous copy - which can be hundreds of megabytes - has to be cleaned up
     * explicitly rather than being overwritten in place.
     */
    @Test
    fun startDownload_deletesThePreviousBuildOfTheSameExtract() = runBlocking<Unit> {
        writeFile("20240101-abc123-glasgow-gb.pmtiles", "old")
        writeFile("20240101-abc123-glasgow-gb.pmtiles.geojson", "{}")
        writeFile("london-gb.pmtiles", "unrelated")
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            systemFileSystem.write(destPath.toPath()) { write(validExtract) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.startDownload("20240202-def456-glasgow-gb.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        assertFalse(fileExists("20240101-abc123-glasgow-gb.pmtiles"))
        assertFalse(fileExists("20240101-abc123-glasgow-gb.pmtiles.geojson"))
        val path = assertNotNull(downloadedFile("glasgow-gb"))
        assertTrue(fileExists("london-gb.pmtiles"))
        assertEquals(
            listOf(path, filePath("london-gb.pmtiles")).sorted(),
            manager.downloadedExtracts.value.sorted(),
        )
    }

    // ----- behaviour that came from Android's OfflineDownloader -----

    /**
     * The server caches extracts lazily, and can answer 200 with only the part of the file it has
     * copied so far. Anything shorter than the manifest's size is retried, not published.
     */
    @Test
    fun startDownload_partialContent_isRetriedUntilTheWholeExtractArrives() = runBlocking<Unit> {
        var attempt = 0
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            attempt++
            val bytes = if (attempt == 1) validExtract.copyOf(validExtract.size / 2) else validExtract
            systemFileSystem.write(destPath.toPath()) { write(bytes) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.startDownload("region.pmtiles", extractSize = validExtract.size.toDouble())
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        assertEquals(2, downloader.calls.size)
        val path = assertNotNull(downloadedFile("region"))
        assertEquals(validExtract.size.toLong(), systemFileSystem.metadata(path.toPath()).size)
    }

    /** A corrupt extract would crash MapLibre natively, so it is never published. */
    @Test
    fun startDownload_corruptExtract_isRejectedAndLeavesNothingBehind() = runBlocking<Unit> {
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            systemFileSystem.write(destPath.toPath()) { writeUtf8("not a pmtiles file") }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )
        val feature = GeoJsonParser.parseFeature(
            squareFeatureJson("Region", "region.pmtiles", -4.3, 55.8, -4.2, 55.9),
        )!!

        manager.startDownload("region.pmtiles", feature = feature)
        waitUntil { manager.downloadState.value is DownloadStateCommon.Error }

        assertTrue(systemFileSystem.list(tempDir).isEmpty(), "left behind: ${systemFileSystem.list(tempDir)}")
    }

    /**
     * MapLibre caches offline tiles under URLs naming the extract's path, so downloading the same
     * extract again must never reuse a path, or new tiles would be drawn mixed with stale ones.
     */
    @Test
    fun startDownload_sameExtractTwice_usesANewPathAndRemovesTheOldOne() = runBlocking<Unit> {
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            systemFileSystem.write(destPath.toPath()) { write(validExtract) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.startDownload("glasgow-gb.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }
        val first = assertNotNull(downloadedFile("glasgow-gb"))
        delay(5) // the version is a millisecond timestamp

        manager.startDownload("glasgow-gb.pmtiles")
        waitUntil { downloader.calls.size == 2 && manager.downloadState.value is DownloadStateCommon.Success }
        val second = assertNotNull(downloadedFile("glasgow-gb"))

        assertTrue(first != second, "re-download reused $first")
        assertFalse(systemFileSystem.exists(first.toPath()))
    }

    @Test
    fun refreshDownloaded_deletesSidecarsWithNoExtract() {
        writeFile("glasgow-gb.v1000.pmtiles", "x")
        writeFile("glasgow-gb.v1000.pmtiles.geojson", "{}")
        writeFile("helsinki-fi.v2000.pmtiles.geojson", "{}")
        writeFile("budapest-hu.v3000.pmtiles.downloading", "x")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refreshDownloaded()

        assertTrue(fileExists("glasgow-gb.v1000.pmtiles.geojson"), "an extract's own sidecar must survive")
        assertFalse(fileExists("helsinki-fi.v2000.pmtiles.geojson"), "a sidecar with no extract is deleted")
        assertTrue(fileExists("budapest-hu.v3000.pmtiles.downloading"), "a download in progress isn't touched")
    }

    @Test
    fun deleteExtractByFeature_deletesEveryVersionOfTheExtract() {
        writeFile("glasgow-gb.pmtiles", "legacy")
        writeFile("glasgow-gb.v1000.pmtiles", "x")
        writeFile("glasgow-gb.v1000.pmtiles.geojson", "{}")
        writeFile("glasgow-gbx.pmtiles", "unrelated")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )
        val feature = Feature().apply {
            properties = hashMapOf("filename" to "20240101-abc123-glasgow-gb.pmtiles")
        }

        manager.deleteExtractByFeature(feature)

        assertEquals(listOf("glasgow-gbx.pmtiles"), systemFileSystem.list(tempDir).map { it.name })
        assertEquals(listOf(filePath("glasgow-gbx.pmtiles")), manager.downloadedExtracts.value)
    }

    @Test
    fun onExtractsChanged_isCalledAfterADownloadAndADelete() = runBlocking<Unit> {
        var changes = 0
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            systemFileSystem.write(destPath.toPath()) { write(validExtract) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            tempDir.toString(),
            "https://example.test/extracts",
            onExtractsChanged = { changes++ },
        )

        manager.startDownload("region.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }
        assertEquals(1, changes, "not notified when the download was published")

        manager.deleteExtractByFeature(Feature().apply { properties = hashMapOf("filename" to "region.pmtiles") })
        assertEquals(2, changes, "not notified when the extract was deleted")
    }

    @Test
    fun extractBasePathProvider_isReadOnEveryUse() = runBlocking<Unit> {
        val first = tempDir / "first"
        val second = tempDir / "second"
        var current = first
        val downloader = RecordingFileDownloader { _, destPath, _ ->
            systemFileSystem.write(destPath.toPath()) { write(validExtract) }
            DownloadResultCommon.Success
        }
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            downloader,
            extractBasePathProvider = { current.toString() },
            extractBaseUrl = "https://example.test/extracts",
        )

        // The user moves the extracts to other storage; the next download goes there, and the
        // directory is created if it doesn't exist yet.
        current = second
        manager.startDownload("region.pmtiles")
        waitUntil { manager.downloadState.value is DownloadStateCommon.Success }

        assertFalse(systemFileSystem.exists(first))
        assertEquals(1, systemFileSystem.list(second).count { it.name.endsWith(".pmtiles") })
    }

    @Test
    fun refresh_retriesAFailedManifestFetch() = runBlocking<Unit> {
        var requests = 0
        val httpClient = HttpClient(
            MockEngine { _ ->
                requests++
                if (requests == 1) {
                    respond(content = ByteReadChannel(ByteArray(0)), status = HttpStatusCode.InternalServerError, headers = headersOf())
                } else {
                    respond(content = ByteReadChannel(gzip(manifestJson())), status = HttpStatusCode.OK, headers = headersOf())
                }
            },
        )
        val manager = OfflineMapManager(
            ManifestClient(httpClient, baseUrl = "https://example.test"),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )

        manager.refresh()
        waitUntil { manager.nearbyExtractsState.value !is NearbyExtractsState.Loading }

        assertTrue(manager.nearbyExtractsState.value is NearbyExtractsState.Loaded)
        assertEquals(2, requests)
    }

    @Test
    fun deleteExtractByFeature_noFilenameProperty_isANoOp() {
        writeFile("region.pmtiles", "data")
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )
        manager.refreshDownloaded()

        manager.deleteExtractByFeature(Feature()) // no properties at all - must not throw

        assertTrue(fileExists("region.pmtiles"))
        assertEquals(1, manager.downloadedExtracts.value.size)
    }

    @Test
    fun deleteExtractByFeature_noMatchingDownload_isANoOp() {
        val manager = OfflineMapManager(
            manifestClient(HttpStatusCode.OK, gzip("{}")),
            noopDownloader(),
            tempDir.toString(),
            "https://example.test/extracts",
        )
        manager.refreshDownloaded() // nothing downloaded
        val feature = Feature().apply { properties = hashMapOf("filename" to "region.pmtiles") }

        manager.deleteExtractByFeature(feature) // must not throw

        assertTrue(manager.downloadedExtracts.value.isEmpty())
    }
}
