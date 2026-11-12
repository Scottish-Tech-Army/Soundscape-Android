package org.scottishtecharmy.soundscape

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.network.createFileDownloader
import org.scottishtecharmy.soundscape.utils.DownloadState
import org.scottishtecharmy.soundscape.utils.OfflineDownloader
import org.scottishtecharmy.soundscape.utils.deleteOrphanedSidecars
import java.io.File

/**
 * Tests for [OfflineDownloader]: the "partial extract" retry handling (the server caches extracts
 * lazily and can answer a 200 whose body is only the part of the file it has copied so far - when
 * the advertised Content-Length is smaller than the expected extract size, the downloader must
 * back off and retry, like a 503, rather than saving a truncated file) and the versioned-filename
 * cleanup that retires a superseded extract once a re-download publishes successfully.
 */
class OfflineDownloaderTest {

    /** Returns the queued responses in order, then repeats the last one. Counts the calls made. */
    private class FakeResponses(private val responses: List<() -> MockResponse>) {
        var callCount = 0
            private set

        fun next(): MockResponse {
            val response = responses[callCount.coerceAtMost(responses.size - 1)]()
            callCount++
            return response
        }
    }

    private data class MockResponse(
        val status: HttpStatusCode,
        val bytes: ByteArray,
        val advertisedContentLength: Long?,
    )

    private fun body(byteCount: Int): MockResponse {
        val bytes = ByteArray(byteCount) { 'A'.code.toByte() }
        return MockResponse(HttpStatusCode.OK, bytes, bytes.size.toLong())
    }

    /**
     * A body that advertises [advertised] bytes via Content-Length but whose stream only actually
     * contains [actual] bytes - the silent-truncation case an intermediary can produce, or that
     * HTTP/2 would surface as a clean EOF short of Content-Length.
     */
    private fun truncatedBody(advertised: Long, actual: Int): MockResponse =
        MockResponse(HttpStatusCode.OK, ByteArray(actual) { 'A'.code.toByte() }, advertised)

    private fun serviceUnavailable(): MockResponse =
        MockResponse(HttpStatusCode.ServiceUnavailable, ByteArray(0), 0L)

    private fun fakeFileDownloader(fake: FakeResponses) =
        createFileDownloader(
            HttpClient(MockEngine { _ ->
                val response = fake.next()
                val headers = if (response.advertisedContentLength != null) {
                    headersOf(HttpHeaders.ContentLength, response.advertisedContentLength.toString())
                } else {
                    headersOf()
                }
                respond(
                    content = ByteReadChannel(response.bytes),
                    status = response.status,
                    headers = headers,
                )
            })
        )

    /** Run a download against [fake] and block until it reaches a terminal state. */
    private fun runDownload(fake: FakeResponses, extractSize: Double?): Pair<DownloadState, File> {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "offline-dl-${System.nanoTime()}")
        tempDir.mkdirs()
        // Deliberately not a .pmtiles path: the partial-content retry is file-type agnostic, and this
        // keeps the test focused on the retry logic without needing a valid pmtiles body.
        val outputFile = File(tempDir, "extract.bin")

        val downloader = OfflineDownloader(fakeFileDownloader(fake))
        downloader.startDownload("https://example.test/extract", outputFile.path, extractSize)

        val state = runBlocking {
            withTimeout(10_000) {
                downloader.downloadState.first {
                    it is DownloadState.Success || it is DownloadState.Error
                }
            }
        }
        return state to outputFile
    }

    @Test
    fun supersededVersionsAreDeletedAfterPublish() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "offline-dl-${System.nanoTime()}")
        tempDir.mkdirs()
        try {
            // Deliberately not .pmtiles, same as runDownload's outputFile: this test is about the
            // cleanup logic, not pmtiles validation, so it avoids needing a valid pmtiles body.
            // An old version of this extract, plus its metadata sidecar - both should be deleted
            // once the new version publishes successfully.
            val oldExtract = File(tempDir, "glasgow-gb.v1000.bin").apply { writeText("old") }
            val oldSidecar = File(tempDir, "glasgow-gb.v1000.bin.geojson").apply { writeText("{}") }
            // An unrelated extract sharing a name prefix - must survive the cleanup.
            val unrelatedExtract = File(tempDir, "glasgow-gbx.bin").apply { writeText("unrelated") }

            val newOutputFile = File(tempDir, "glasgow-gb.v2000.bin")
            // The download writes its own sidecar once published, and it must survive the
            // post-publish cleanup, which once deleted it (it starts with the logical prefix and
            // its name isn't *exactly* newOutputFile.name), silently hiding every freshly
            // downloaded extract from the offline-maps UI.
            val newSidecar = File(tempDir, "glasgow-gb.v2000.bin.geojson")

            val fake = FakeResponses(listOf({ body(1000) }))
            val downloader = OfflineDownloader(fakeFileDownloader(fake))
            downloader.startDownload(
                "https://example.test/extract",
                newOutputFile.path,
                extractSize = 1000.0,
                logicalBaseName = "glasgow-gb",
                metadataJson = "{}",
            )

            val state = runBlocking {
                withTimeout(10_000) {
                    downloader.downloadState.first {
                        it is DownloadState.Success || it is DownloadState.Error
                    }
                }
            }

            assertTrue("expected Success but was $state", state is DownloadState.Success)
            assertTrue("new version should exist", newOutputFile.exists())
            assertTrue("new version's own sidecar must survive", newSidecar.exists())
            assertTrue("old version should be deleted", !oldExtract.exists())
            assertTrue("old sidecar should be deleted", !oldSidecar.exists())
            assertTrue("unrelated extract must survive", unrelatedExtract.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * The first "Update" of an extract downloaded by the previous, pre-versioning code must also
     * retire it: its on-disk name has no ".vNNN" segment at all (just "<logicalBase>.<ext>"), not
     * "<logicalBase>.v1000.<ext>" like a version this code produced itself.
     */
    @Test
    fun legacyUnversionedExtractIsDeletedOnFirstUpdate() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "offline-dl-${System.nanoTime()}")
        tempDir.mkdirs()
        try {
            val legacyExtract = File(tempDir, "glasgow-gb.bin").apply { writeText("legacy") }
            val legacySidecar = File(tempDir, "glasgow-gb.bin.geojson").apply { writeText("{}") }
            val unrelatedExtract = File(tempDir, "glasgow-gbx.bin").apply { writeText("unrelated") }

            val newOutputFile = File(tempDir, "glasgow-gb.v2000.bin")
            val newSidecar = File(tempDir, "glasgow-gb.v2000.bin.geojson")
            val fake = FakeResponses(listOf({ body(1000) }))
            val downloader = OfflineDownloader(fakeFileDownloader(fake))
            downloader.startDownload(
                "https://example.test/extract",
                newOutputFile.path,
                extractSize = 1000.0,
                logicalBaseName = "glasgow-gb",
                metadataJson = "{}",
            )

            val state = runBlocking {
                withTimeout(10_000) {
                    downloader.downloadState.first {
                        it is DownloadState.Success || it is DownloadState.Error
                    }
                }
            }

            assertTrue("expected Success but was $state", state is DownloadState.Success)
            assertTrue("new version should exist", newOutputFile.exists())
            assertTrue("new version's own sidecar must survive", newSidecar.exists())
            assertTrue("legacy unversioned extract should be deleted", !legacyExtract.exists())
            assertTrue("legacy sidecar should be deleted", !legacySidecar.exists())
            assertTrue("unrelated extract must survive", unrelatedExtract.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun partialContentIsRetriedUntilFullFileArrives() {
        val fake = FakeResponses(
            listOf(
                { body(500) },  // server still caching: only half the bytes
                { body(1000) }, // now the full file
            ),
        )
        val (state, file) = runDownload(fake, extractSize = 1000.0)
        try {
            assertTrue("expected Success but was $state", state is DownloadState.Success)
            assertEquals("should have retried once after the partial response", 2, fake.callCount)
            assertEquals(1000L, file.length())
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun fullContentLengthDownloadsWithoutRetry() {
        val fake = FakeResponses(listOf({ body(1000) }))
        val (state, file) = runDownload(fake, extractSize = 1000.0)
        try {
            assertTrue("expected Success but was $state", state is DownloadState.Success)
            assertEquals("a full-size response must not retry", 1, fake.callCount)
            assertEquals(1000L, file.length())
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun truncatedBodyFailsAndIsNotPublished() {
        // Content-Length advertises the full extract size (so it passes the partial-cache pre-check),
        // but the stream ends after only half the bytes. The downloader must reject this rather than
        // publishing a truncated file.
        val fake = FakeResponses(listOf({ truncatedBody(advertised = 1000L, actual = 500) }))
        val (state, file) = runDownload(fake, extractSize = 1000.0)
        try {
            assertTrue("expected Error but was $state", state is DownloadState.Error)
            assertTrue("a truncated download must not be left on disk", !file.exists())
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun serverCachingResponseIsRetriedUntilReady() {
        val fake = FakeResponses(
            listOf(
                { serviceUnavailable() },
                { body(1000) },
            ),
        )
        val (state, file) = runDownload(fake, extractSize = 1000.0)
        try {
            assertTrue("expected Success but was $state", state is DownloadState.Success)
            assertEquals("should have retried once after the 503", 2, fake.callCount)
            assertEquals(1000L, file.length())
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    /**
     * A download that fails must leave nothing behind - its metadata sidecar used to be written
     * before the download started, and stayed on the phone forever with no extract next to it.
     */
    @Test
    fun failedDownloadLeavesNoSidecar() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "offline-dl-${System.nanoTime()}")
        tempDir.mkdirs()
        try {
            val outputFile = File(tempDir, "helsinki-fi.v2000.bin")
            val fake = FakeResponses(listOf({ truncatedBody(advertised = 1000L, actual = 500) }))
            val downloader = OfflineDownloader(fakeFileDownloader(fake))
            downloader.startDownload(
                "https://example.test/extract",
                outputFile.path,
                extractSize = 1000.0,
                logicalBaseName = "helsinki-fi",
                metadataJson = "{}",
            )

            val state = runBlocking {
                withTimeout(10_000) {
                    downloader.downloadState.first {
                        it is DownloadState.Success || it is DownloadState.Error
                    }
                }
            }

            assertTrue("expected Error but was $state", state is DownloadState.Error)
            assertTrue("no sidecar for a failed download", tempDir.listFiles().isNullOrEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * Regression test for a cancelled download that's slow to actually unwind (e.g. still
     * waiting on a connection, like issue #1288's "still at 0%" UK extract). Job.isActive
     * becomes false the instant cancelDownload() is called - well before the cancelled
     * coroutine's own cleanup (and its belated Canceled state write) has run - so a new
     * download started right after must wait for that cleanup rather than racing it, or its
     * own success can be clobbered by the old download's delayed Canceled write.
     */
    @Test
    fun newDownloadWaitsForPreviousCancellationToFinishUnwinding() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "offline-dl-${System.nanoTime()}")
        tempDir.mkdirs()
        try {
            val ukPath = File(tempDir, "uk-gb.v1.bin").path
            val glasgowPath = File(tempDir, "glasgow-gb.v2.bin").path

            // Signalled once the UK request has received its first 503, so the test can wait
            // for that precisely rather than guessing with a fixed delay.
            val ukFirstResponseReceived = CompletableDeferred<Unit>()
            val httpClient = HttpClient(
                MockEngine { request ->
                    if (request.url.encodedPath.contains("uk")) {
                        // Every attempt reports "still caching" - this drives the UK download
                        // into waitBeforeRetry()'s backoff loop, which only checks for
                        // cancellation once per second (via Thread.sleep(1000) between
                        // ensureActive() calls) rather than at every suspension point. That's
                        // a real, deterministic "slow to unwind" window, unlike relying on
                        // however fast the mock engine happens to propagate cancellation.
                        ukFirstResponseReceived.complete(Unit)
                        respond(
                            content = ByteReadChannel(ByteArray(0)),
                            status = HttpStatusCode.ServiceUnavailable,
                            headers = headersOf(HttpHeaders.ContentLength, "0"),
                        )
                    } else {
                        respond(
                            content = ByteReadChannel(ByteArray(10) { 'A'.code.toByte() }),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentLength, "10"),
                        )
                    }
                },
            )

            val downloader = OfflineDownloader(createFileDownloader(httpClient))
            val observedStates = mutableListOf<DownloadState>()
            val collectJob = CoroutineScope(Dispatchers.Default).launch {
                downloader.downloadState.collect { observedStates.add(it) }
            }

            // extractSize of 10_000_000 makes waitBeforeRetry()'s first-attempt backoff exactly
            // one second (its estimate is extractSize / 10_000_000 seconds).
            downloader.startDownload("https://example.test/uk", ukPath, extractSize = 10_000_000.0)
            runBlocking { withTimeout(5_000) { ukFirstResponseReceived.await() } }
            // Give the UK coroutine time to reach past its one ensureActive() check at the top
            // of waitBeforeRetry() and into the one-second Thread.sleep(1000) - that synchronous
            // path (HttpError branch -> waitBeforeRetry entry -> ensureActive) runs in
            // microseconds, so this is a generous margin, not a race.
            Thread.sleep(20)
            downloader.cancelDownload()
            downloader.startDownload("https://example.test/glasgow", glasgowPath, extractSize = null)

            runBlocking {
                withTimeout(5_000) {
                    downloader.downloadState.first {
                        it is DownloadState.Success || it is DownloadState.Error
                    }
                }
            }
            // Let the UK download's one-second backoff sleep finish, notice the cancellation,
            // and write its belated Canceled state too.
            Thread.sleep(1_200)
            collectJob.cancel()

            val canceledIndex = observedStates.indexOfFirst { it is DownloadState.Canceled }
            val successIndex = observedStates.indexOfFirst { it is DownloadState.Success }
            assertTrue("expected a Canceled state, got $observedStates", canceledIndex >= 0)
            assertTrue("expected a Success state, got $observedStates", successIndex >= 0)
            assertTrue(
                "the Glasgow download's Success must not be overwritten by the UK " +
                    "download's belated Canceled state: $observedStates",
                successIndex > canceledIndex,
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun orphanedSidecarsAreDeleted() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "offline-dl-${System.nanoTime()}")
        tempDir.mkdirs()
        try {
            val extract = File(tempDir, "glasgow-gb.v1000.pmtiles").apply { writeText("x") }
            val sidecar = File(tempDir, "glasgow-gb.v1000.pmtiles.geojson").apply { writeText("{}") }
            val orphan = File(tempDir, "helsinki-fi.v2000.pmtiles.geojson").apply { writeText("{}") }
            val download = File(tempDir, "budapest-hu.v3000.pmtiles.downloading").apply { writeText("x") }

            deleteOrphanedSidecars(tempDir)

            assertTrue(extract.exists())
            assertTrue("an extract's own sidecar must survive", sidecar.exists())
            assertTrue("a sidecar with no extract should be deleted", !orphan.exists())
            assertTrue("a download in progress isn't touched", download.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
