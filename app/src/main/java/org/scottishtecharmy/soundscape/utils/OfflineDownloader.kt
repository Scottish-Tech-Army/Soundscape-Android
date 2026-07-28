package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.scottishtecharmy.soundscape.network.UserAgentInterceptor
import okhttp3.ResponseBody
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.network.IDownloadService
import org.scottishtecharmy.soundscape.network.IManifestDAO
import org.scottishtecharmy.soundscape.network.ManifestClient
import org.scottishtecharmy.soundscape.utils.OfflineDownloader.Companion.TAG
import retrofit2.Retrofit
import retrofit2.awaitResponse
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit

suspend fun downloadAndParseManifest(applicationContext: Context) : FeatureCollection? {

    for (retry in 1..4) {
        try {
            return withContext(Dispatchers.IO) {
                val manifestClient = ManifestClient(applicationContext)

                val service =
                    manifestClient.retrofitInstance?.create(IManifestDAO::class.java)
                val manifestReq =
                    async {
                        service?.getManifest()
                    }

                val result = manifestReq.await()?.awaitResponse()?.body()
                    ?: throw Exception("Manifest response null")

                result

            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading manifest $retry", e)
        }
        sleep(500)
    }
    // All retries failed
    Log.e(TAG, "Error downloading manifest after all retries")
    return null
}
// --- Download State Management ---
sealed class DownloadState {
    object Idle : DownloadState()
    object Caching : DownloadState()
    data class Downloading(val progress: Int) : DownloadState() // Progress as a per mil (0-1000)
    object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
    object Canceled : DownloadState()
}

class OfflineDownloader(injectedDownloadService: IDownloadService? = null) {

    companion object {
        const val TAG = "OfflineDownloader"
    }

    private var downloadJob: Job? = null
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Injectable so tests can drive crafted responses; production builds the real service.
    private val downloadService: IDownloadService =
        injectedDownloadService ?: createDefaultDownloadService()

    private fun createDefaultDownloadService(): IDownloadService {
        // We want a long timeout here to allow for network caching to happen behind the scenes
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor())
            .connectTimeout(3, TimeUnit.MINUTES)
            .readTimeout(3, TimeUnit.MINUTES)
            // Pin to HTTP/1.1. OkHttp only enforces Content-Length on HTTP/1.1 (a short
            // body throws "unexpected end of stream"); over HTTP/2 a premature END_STREAM
            // returns a clean EOF, which would let us save a silently truncated extract.
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://placeholder.com/") // Base URL is required but will be overridden by @Url
            .client(okHttpClient)
            .build()

        return retrofit.create(IDownloadService::class.java)
    }

    /**
     * @param outputFilePath Where to publish the download. For a .pmtiles extract this should be
     * a path unique to this download attempt (e.g. with a version/timestamp baked into the
     * filename) - see [logicalBaseName].
     * @param logicalBaseName If set, [outputFilePath]'s filename is expected to be of the form
     * "<logicalBaseName>.<version-and-extension>", e.g. "glasgow-gb.v1699999999999.pmtiles" for a
     * logicalBaseName of "glasgow-gb". Once the download is published, any other file in the same
     * directory whose name starts with "<logicalBaseName>." is deleted - this is what actually
     * retires a previous version of the same extract (see the comment above the rename below for
     * why in-place replacement isn't safe for pmtiles extracts that MapLibre may have opened).
     */
    fun startDownload(
        fileUrl: String,
        outputFilePath: String,
        extractSize: Double?,
        logicalBaseName: String? = null,
    ) {
        if (downloadJob?.isActive == true) {
            Log.w(TAG, "Download is already in progress.")
            return
        }

        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Starting download for URL: $fileUrl")

            val tempFile = File("$outputFilePath.downloading")
            val finalFile = File(outputFilePath)

            try {
                // Ensure parent directories exist
                tempFile.parentFile?.mkdirs()

                val maxRetries = 10
                var retries = maxRetries
                while (retries > 0) {
                    Log.d(TAG, "Download attempt $retries")
                    _downloadState.value = DownloadState.Caching
                    val response = downloadService.downloadFile(fileUrl)
                    if (response.isSuccessful) {
                        val body = response.body() ?: throw Exception("Response body was null.")

                        // The server caches extracts lazily and can answer a 200 with only the
                        // part of the file it has copied so far. If the advertised Content-Length
                        // is smaller than the extract size we expect from the manifest, the file
                        // isn't ready yet - so back off and retry exactly as we do for a 503,
                        // rather than downloading and saving a truncated extract.
                        val contentLength = body.contentLength()
                        if (extractSize != null && contentLength in 0 until extractSize.toLong()) {
                            Log.d(TAG, "Extract not fully cached yet ($contentLength of ${extractSize.toLong()} bytes), retrying")
                            body.close()
                            waitBeforeRetry(this, firstAttempt = retries == maxRetries, extractSize = extractSize)
                            --retries
                            continue
                        }

                        saveFile(this, body, tempFile.path) { progress ->
                            _downloadState.value = DownloadState.Downloading(progress)
                        }
                        // Verify the download is intact before publishing it. A
                        // truncated or corrupt .pmtiles extract crashes MapLibre
                        // natively when opened, so reject it here and let the user
                        // retry rather than persisting a file that will abort the app.
                        if (finalFile.name.endsWith(".pmtiles") &&
                            !isPmtilesUsable(tempFile.path)) {
                            throw Exception("Downloaded extract failed validation (corrupt or truncated)")
                        }
                        retries = 0
                        // Rename straight to the destination - do NOT delete it first, and never
                        // reuse an existing path for an "Update" re-download (see logicalBaseName
                        // below). MapLibre's native PMTilesFileSource caches parsed header/
                        // directory data per URL *forever*, with no invalidation hook - so once a
                        // pmtiles://file://<path> URL has been opened once, silently swapping the
                        // bytes at that same path (even atomically) leaves MapLibre reading brand
                        // new file content through stale cached offsets, which can misread garbage
                        // and crash with no try/catch on its worker thread. Publishing each
                        // download under a unique path sidesteps that entirely: MapLibre never
                        // sees a URL it has cached anything for. File.renameTo (no pre-delete)
                        // still atomically replaces a destination that does exist - e.g. a retry
                        // of this same attempt - via the rename(2) syscall, since tempFile and
                        // finalFile are always in the same directory.
                        if (tempFile.renameTo(finalFile)) {
                            Log.i(TAG, "Download successful. File saved to: ${finalFile.path}")
                            // Clean up old versions before publishing Success - an observer
                            // reacting to Success (e.g. refreshing the extract list) should never
                            // see a superseded version still on disk.
                            if (logicalBaseName != null) {
                                deleteSupersededVersions(finalFile, logicalBaseName)
                            }
                            _downloadState.value = DownloadState.Success
                        } else {
                            throw Exception("Failed to rename file from ${tempFile.name} to ${finalFile.name}")
                        }
                    } else {
                        if(response.code() == 503) {
                            // The server is likely copying the extract into it's cache and is
                            // asking that we try again a little later.
                            waitBeforeRetry(this, firstAttempt = retries == maxRetries, extractSize = extractSize)
                            --retries
                        } else {
                            throw Exception("Download failed with code: ${response.code()} and message: ${response.message()}")
                        }
                    }
                }

                // We exhausted every retry without ever publishing the file (e.g. the server
                // kept returning a partial extract), so surface that rather than leaving the UI
                // stuck on "Caching".
                if (_downloadState.value !is DownloadState.Success) {
                    tempFile.delete()
                    throw Exception("Extract was not ready after $maxRetries attempts")
                }
            } catch (e: CancellationException) {
                // Handle coroutine cancellation
                _downloadState.value = DownloadState.Canceled
                tempFile.delete() // Clean up partial file
                Log.i(TAG, "Download was canceled $e")
            } catch (e: Exception) {
                // Handle other errors (network, file I/O, etc.)
                _downloadState.value =
                    DownloadState.Error(e.message ?: "An unknown error occurred")
                tempFile.delete() // Clean up partial file
                Log.e(TAG, "Download failed", e)
            }
        }
    }

    /**
     * Delete every other file in [finalFile]'s directory whose name starts with
     * "$logicalBaseName." - i.e. every previous version of this extract (its old .pmtiles file(s)
     * and their .geojson sidecars), now superseded by [finalFile]. The leading-dot anchor means
     * "glasgow-gb." matches "glasgow-gb.pmtiles" and "glasgow-gb.v123.pmtiles" but not an unrelated
     * "glasgow-gbz.pmtiles".
     *
     * This is safe to do immediately, unlike the old in-place overwrite: any reader still holding
     * the old file open keeps reading it consistently (POSIX keeps a deleted-but-open file's data
     * intact), and any reader that tries to open the old path fresh afterward gets a clean "not
     * found" - which MapLibre's file source already treats as a normal, handled error - rather than
     * reading wrong-but-present bytes that fail to decompress and crash.
     */
    private fun deleteSupersededVersions(finalFile: File, logicalBaseName: String) {
        val prefix = "$logicalBaseName."
        val siblings = finalFile.parentFile?.listFiles { file ->
            // Exclude finalFile itself AND its own "<finalFile.name>.geojson" sidecar - a plain
            // "!= finalFile.name" check only protects the .pmtiles file, not the sidecar, so this
            // was deleting every download's own just-written metadata immediately after
            // publishing it, silently hiding every new extract from the offline-maps UI (which
            // requires that sidecar to list an extract at all).
            file.name.startsWith(prefix) && !file.name.startsWith(finalFile.name)
        } ?: return
        for (sibling in siblings) {
            if (sibling.delete()) {
                Log.i(TAG, "Deleted superseded extract version: ${sibling.path}")
            } else {
                Log.w(TAG, "Failed to delete superseded extract version: ${sibling.path}")
            }
        }
    }

    /**
     * Back off before retrying a download, the same way we do when the server returns a 503 to
     * say it is still copying the extract into its cache. We guess that the caching runs at around
     * 10MB/sec, so on the first attempt we wait for an estimate based on the extract size; on later
     * attempts we just poll every 15 seconds.
     */
    private fun waitBeforeRetry(scope: CoroutineScope, firstAttempt: Boolean, extractSize: Double?) {
        var cachingDuration = 15
        if (firstAttempt && extractSize != null) {
            cachingDuration = (extractSize / 10000000.0).toInt()
        }
        Log.d(TAG, "Wait for $cachingDuration seconds before retrying.")
        _downloadState.value = DownloadState.Caching
        while (cachingDuration > 0) {
            scope.ensureActive()
            sleep(1000)
            --cachingDuration
        }
    }

    private fun saveFile(scope: CoroutineScope,
                         body: ResponseBody,
                         filePath: String,
                         onProgress: (Int) -> Unit) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val fileReader = ByteArray(4096)
            val fileSize = body.contentLength()
            var fileSizeDownloaded: Long = 0

            inputStream = body.byteStream()
            outputStream = FileOutputStream(filePath)

            while (true) {
                // Check if the coroutine has been cancelled
                scope.ensureActive()

                val read = inputStream.read(fileReader)
                if (read == -1) {
                    break
                }
                outputStream.write(fileReader, 0, read)
                fileSizeDownloaded += read

                // Calculate and emit progress as a value out of 1000
                val progress = ((fileSizeDownloaded * 1000) / fileSize).toInt()
                onProgress(progress)
            }
            outputStream.flush()

            // EOF is not the same as "got everything". If the stream ends short of the
            // advertised Content-Length (e.g. an intermediary truncates the response with
            // a clean close), reject it here so the caller retries rather than publishing a
            // truncated extract.
            if (fileSize >= 0 && fileSizeDownloaded != fileSize) {
                throw IOException("Truncated download: $fileSizeDownloaded of $fileSize bytes")
            }
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    fun cancelDownload() {
        if (downloadJob?.isActive == true) {
            downloadJob?.cancel()
        }
    }
}
