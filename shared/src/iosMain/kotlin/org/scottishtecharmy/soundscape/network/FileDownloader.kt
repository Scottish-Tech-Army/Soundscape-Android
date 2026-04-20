package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

/**
 * iOS file downloader using Ktor Darwin engine + Okio for file writing.
 * Downloads the entire response body then writes to disk.
 * For very large files, a streaming approach would be better.
 */
class IosFileDownloader : FileDownloaderInterface {

    private val httpClient = HttpClient(Darwin) {
        expectSuccess = false
        engine {
            configureRequest {
                setTimeoutInterval(300.0) // 5 minutes
            }
        }
    }

    override suspend fun download(
        url: String,
        destPath: String,
        scope: CoroutineScope,
        onProgress: (Int) -> Unit,
    ): DownloadResultCommon {
        scope.ensureActive()

        val response = httpClient.get(url)
        if (!response.status.isSuccess()) {
            return DownloadResultCommon.HttpError(
                response.status.value,
                response.status.description,
            )
        }

        scope.ensureActive()
        onProgress(0)

        val bytes = response.bodyAsBytes()

        scope.ensureActive()
        onProgress(500) // 50% — downloaded, now writing

        val path = destPath.toPath()
        path.parent?.let { parent ->
            try { FileSystem.SYSTEM.createDirectories(parent) } catch (_: Exception) {}
        }

        val sink = FileSystem.SYSTEM.sink(path).buffer()
        try {
            sink.write(bytes)
            sink.flush()
        } finally {
            sink.close()
        }

        onProgress(1000)
        return DownloadResultCommon.Success
    }
}
