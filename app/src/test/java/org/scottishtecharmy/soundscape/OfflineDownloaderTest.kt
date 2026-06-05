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
