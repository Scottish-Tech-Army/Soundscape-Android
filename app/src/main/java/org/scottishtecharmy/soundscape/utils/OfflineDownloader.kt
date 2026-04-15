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
import com.squareup.moshi.Moshi
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.network.UserAgentInterceptor
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.network.DownloadResult
import org.scottishtecharmy.soundscape.network.FileDownloader
import org.scottishtecharmy.soundscape.network.createAndroidFileDownloader
import org.scottishtecharmy.soundscape.network.createAndroidManifestClient
import org.scottishtecharmy.soundscape.utils.OfflineDownloader.Companion.TAG
import java.io.File
import java.lang.Thread.sleep

suspend fun downloadAndParseManifest(applicationContext: Context) : FeatureCollection? {

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

class OfflineDownloader {

    companion object {
        const val TAG = "OfflineDownloader"
    }

    private var downloadJob: Job? = null
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val fileDownloader: FileDownloader = createAndroidFileDownloader(
        userAgent = UserAgentInterceptor.USER_AGENT,
    )

    fun startDownload(
        fileUrl: String,
        outputFilePath: String,
        extractSize: Double?
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
                            retries = 0
                            // Delete any file that already exists
                            finalFile.delete()
                            // Rename the file on successful completion
                            if (tempFile.renameTo(finalFile)) {
                                _downloadState.value = DownloadState.Success
                                Log.i(TAG, "Download successful. File saved to: ${finalFile.path}")
                            } else {
                                throw Exception("Failed to rename file from ${tempFile.name} to ${finalFile.name}")
                            }
                        }
                        is DownloadResult.HttpError -> {
                            if (result.code == 503) {
                                // The server is likely copying the extract into it's cache and is
                                // asking that we try again a little later. We're going to guess that
                                // the caching runs at around 10MB/sec and as we know the size of the
                                // extract, we can back off appropriately.
                                var cachingDuration = 15
                                if (retries == maxRetries && extractSize != null) {
                                    cachingDuration = (extractSize / 10000000.0).toInt()
                                }
                                Log.d(TAG, "Wait for $cachingDuration seconds before retrying.")
                                _downloadState.value = DownloadState.Caching
                                while (cachingDuration > 0) {
                                    ensureActive()
                                    sleep(1000)
                                    --cachingDuration
                                }
                                --retries
                            } else {
                                throw Exception("Download failed with code: ${result.code} and message: ${result.message}")
                            }
                        }
                    }
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

    fun cancelDownload() {
        if (downloadJob?.isActive == true) {
            downloadJob?.cancel()
        }
    }
}
