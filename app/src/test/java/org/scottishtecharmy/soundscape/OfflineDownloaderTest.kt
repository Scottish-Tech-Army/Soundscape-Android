package org.scottishtecharmy.soundscape

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.network.IDownloadService
import org.scottishtecharmy.soundscape.utils.DownloadState
import org.scottishtecharmy.soundscape.utils.OfflineDownloader
import retrofit2.Response
import java.io.File

/**
 * Tests for [OfflineDownloader]'s "partial extract" handling. The server caches extracts lazily and
 * can answer a 200 whose body is only the part of the file it has copied so far. When the advertised
 * Content-Length is smaller than the expected extract size, the downloader must back off and retry
 * (like a 503) rather than saving a truncated file.
 */
class OfflineDownloaderTest {

    /** Returns the queued responses in order, then repeats the last one. Counts the calls made. */
    private class FakeDownloadService(
        private val responses: List<Response<ResponseBody>>,
    ) : IDownloadService {
        var callCount = 0
            private set

        override suspend fun downloadFile(fileUrl: String): Response<ResponseBody> {
            val response = responses[callCount.coerceAtMost(responses.size - 1)]
            callCount++
            return response
        }
    }

    private fun body(byteCount: Int): ResponseBody =
        ByteArray(byteCount) { 'A'.code.toByte() }
            .toResponseBody("application/octet-stream".toMediaType())

    /** A body whose Content-Length is unknown (-1), as with chunked/gzip transfer. */
    private fun unknownLengthBody(byteCount: Int): ResponseBody =
        object : ResponseBody() {
            private val data = ByteArray(byteCount) { 'A'.code.toByte() }
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength() = -1L
            override fun source(): BufferedSource = Buffer().apply { write(data) }
        }

    /**
     * A body that advertises [advertised] bytes via Content-Length but whose stream ends cleanly
     * after only [actual] bytes - the silent-truncation case an intermediary can produce, or that
     * HTTP/2 would surface as a clean EOF short of Content-Length.
     */
    private fun truncatedBody(advertised: Long, actual: Int): ResponseBody =
        object : ResponseBody() {
            private val data = ByteArray(actual) { 'A'.code.toByte() }
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength() = advertised
            override fun source(): BufferedSource = Buffer().apply { write(data) }
        }

    /** Run a download against [service] and block until it reaches a terminal state. */
    private fun runDownload(service: FakeDownloadService, extractSize: Double?): Pair<DownloadState, File> {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "offline-dl-${System.nanoTime()}")
        tempDir.mkdirs()
        // Deliberately not a .pmtiles path: the partial-content retry is file-type agnostic, and this
        // keeps the test focused on the retry logic without needing a valid pmtiles body.
        val outputFile = File(tempDir, "extract.bin")

        val downloader = OfflineDownloader(service)
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
            // OfflineMapsViewModel.download() writes this sidecar *before* calling startDownload -
            // it must survive the post-publish cleanup, which previously deleted it (it starts
            // with the logical prefix and its name isn't *exactly* newOutputFile.name), silently
            // hiding every freshly downloaded extract from the offline-maps UI.
            val newSidecar = File(tempDir, "glasgow-gb.v2000.bin.geojson").apply { writeText("{}") }

            val service = FakeDownloadService(listOf(Response.success(body(1000))))
            val downloader = OfflineDownloader(service)
            downloader.startDownload(
                "https://example.test/extract",
                newOutputFile.path,
                extractSize = 1000.0,
                logicalBaseName = "glasgow-gb",
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
            val newSidecar = File(tempDir, "glasgow-gb.v2000.bin.geojson").apply { writeText("{}") }
            val service = FakeDownloadService(listOf(Response.success(body(1000))))
            val downloader = OfflineDownloader(service)
            downloader.startDownload(
                "https://example.test/extract",
                newOutputFile.path,
                extractSize = 1000.0,
                logicalBaseName = "glasgow-gb",
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
        val service = FakeDownloadService(
            listOf(
                Response.success(body(500)),  // server still caching: only half the bytes
                Response.success(body(1000)), // now the full file
            ),
        )
        val (state, file) = runDownload(service, extractSize = 1000.0)
        try {
            assertTrue("expected Success but was $state", state is DownloadState.Success)
            assertEquals("should have retried once after the partial response", 2, service.callCount)
            assertEquals(1000L, file.length())
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun fullContentLengthDownloadsWithoutRetry() {
        val service = FakeDownloadService(listOf(Response.success(body(1000))))
        val (state, file) = runDownload(service, extractSize = 1000.0)
        try {
            assertTrue("expected Success but was $state", state is DownloadState.Success)
            assertEquals("a full-size response must not retry", 1, service.callCount)
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
        val service = FakeDownloadService(
            listOf(Response.success(truncatedBody(advertised = 1000L, actual = 500))),
        )
        val (state, file) = runDownload(service, extractSize = 1000.0)
        try {
            assertTrue("expected Error but was $state", state is DownloadState.Error)
            assertTrue("a truncated download must not be left on disk", !file.exists())
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun unknownContentLengthDownloadsWithoutRetry() {
        // Content-Length is -1, so we can't tell if it is partial - we must download it, not retry
        // forever. (Guards against the retry condition being changed to `contentLength < size`.)
        val service = FakeDownloadService(listOf(Response.success(unknownLengthBody(1000))))
        val (state, file) = runDownload(service, extractSize = 999999.0)
        try {
            assertTrue("expected Success but was $state", state is DownloadState.Success)
            assertEquals("an unknown-length response must not retry", 1, service.callCount)
            assertEquals(1000L, file.length())
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }
}
