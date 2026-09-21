package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.network.DownloadResult
import org.scottishtecharmy.soundscape.network.FileDownloader
import org.scottishtecharmy.soundscape.network.UserAgentInterceptor
import org.scottishtecharmy.soundscape.network.createAndroidFileDownloader
import org.scottishtecharmy.soundscape.network.createAndroidManifestClient
import org.scottishtecharmy.soundscape.utils.OfflineDownloader.Companion.TAG
import java.io.File
import java.lang.Thread.sleep

suspend fun downloadAndParseManifest(applicationContext: Context): FeatureCollection? {

    val manifestClient = createAndroidManifestClient(
        baseUrl = BuildConfig.EXTRACT_PROVIDER_URL,
        userAgent = UserAgentInterceptor.USER_AGENT,
    )
    val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
    val adapter = moshi.adapter(FeatureCollection::class.java)

    for (retry in 1..4) {
        try {
            return withContext(Dispatchers.IO) {
                val json = manifestClient.getManifestJson()
                    ?: throw Exception("Manifest response null")
                adapter.fromJson(json) ?: throw Exception("Manifest parse failed")
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

class OfflineDownloader(injectedFileDownloader: FileDownloader? = null) {

    companion object {
        const val TAG = "OfflineDownloader"
    }

    private var downloadJob: Job? = null
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Injectable so tests can drive crafted responses; production builds the real downloader.
    private val fileDownloader: FileDownloader = injectedFileDownloader ?: createAndroidFileDownloader(
        userAgent = UserAgentInterceptor.USER_AGENT,
    )

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
                    val result = fileDownloader.download(
                        url = fileUrl,
                        destFile = tempFile,
                        scope = this,
                    ) { progress ->
                        _downloadState.value = DownloadState.Downloading(progress)
                    }
                    when (result) {
                        is DownloadResult.Success -> {
                            // The server caches extracts lazily and can answer with only the
                            // part of the file it has copied so far. If what we actually
                            // received is smaller than the extract size we expect from the
                            // manifest, the file isn't ready yet - back off and retry exactly
                            // as we do for a 503, rather than publishing a truncated extract.
                            if (extractSize != null && tempFile.length() < extractSize.toLong()) {
                                Log.d(
                                    TAG,
                                    "Extract not fully cached yet (${tempFile.length()} of ${extractSize.toLong()} bytes), retrying",
                                )
                                waitBeforeRetry(this, firstAttempt = retries == maxRetries, extractSize = extractSize)
                                --retries
                                continue
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
                            // Rename straight to the destination - do NOT delete it first, and
                            // never reuse an existing path for an "Update" re-download (see
                            // logicalBaseName below). MapLibre's native PMTilesFileSource caches
                            // parsed header/directory data per URL *forever*, with no invalidation
                            // hook - so once a pmtiles://file://<path> URL has been opened once,
                            // silently swapping the bytes at that same path (even atomically)
                            // leaves MapLibre reading brand new file content through stale cached
                            // offsets, which can misread garbage and crash with no try/catch on
                            // its worker thread. Publishing each download under a unique path
                            // sidesteps that entirely: MapLibre never sees a URL it has cached
                            // anything for. File.renameTo (no pre-delete) still atomically
                            // replaces a destination that does exist - e.g. a retry of this same
                            // attempt - via the rename(2) syscall, since tempFile and finalFile
                            // are always in the same directory.
                            if (tempFile.renameTo(finalFile)) {
                                Log.i(TAG, "Download successful. File saved to: ${finalFile.path}")
                                // Clean up old versions before publishing Success - an observer
                                // reacting to Success (e.g. refreshing the extract list) should
                                // never see a superseded version still on disk.
                                if (logicalBaseName != null) {
                                    deleteSupersededVersions(finalFile, logicalBaseName)
                                }
                                _downloadState.value = DownloadState.Success
                            } else {
                                throw Exception("Failed to rename file from ${tempFile.name} to ${finalFile.name}")
                            }
                        }

                        is DownloadResult.HttpError -> {
                            if (result.code == 503) {
                                // The server is likely copying the extract into it's cache and is
                                // asking that we try again a little later.
                                waitBeforeRetry(this, firstAttempt = retries == maxRetries, extractSize = extractSize)
                                --retries
                            } else {
                                throw Exception("Download failed with code: ${result.code} and message: ${result.message}")
                            }
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

    fun cancelDownload() {
        if (downloadJob?.isActive == true) {
            downloadJob?.cancel()
        }
    }
}
