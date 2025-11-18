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
import java.io.InputStream
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit

suspend fun downloadAndParseManifest(applicationContext: Context) : Pair<FeatureCollection?, String> {

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

                Pair(manifestReq.await()?.awaitResponse()?.body(), manifestClient.redirect)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading manifest $retry", e)
        }
    }
    // All retries failed
    return Pair(null, "")
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

    private val downloadService: IDownloadService

    init {
        // We want a long timeout here to allow for network caching to happen behind the scenes
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.MINUTES)
            .readTimeout(3, TimeUnit.MINUTES)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://placeholder.com/") // Base URL is required but will be overridden by @Url
            .client(okHttpClient)
            .build()

        downloadService = retrofit.create(IDownloadService::class.java)
    }

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
                    val response = downloadService.downloadFile(fileUrl)
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            saveFile(this, body, tempFile.path) { progress ->
                                _downloadState.value = DownloadState.Downloading(progress)
                            }
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
                        } ?: throw Exception("Response body was null.")
                    } else {
                        if(response.code() == 503) {
                            // The server is likely copying the extract into it's cache and is
                            // asking that we try again a little later. We're going to guess that
                            // the caching runs at around 10MB/sec and as we know the size of the
                            // extract, we can back off appropriately.
                            var cachingDuration = 15
                            if(retries == maxRetries) {
                                if(extractSize != null) {
                                    cachingDuration = (extractSize / 10000000.0).toInt()
                                }
                            }
                            Log.d(TAG, "Wait for $cachingDuration seconds before retrying.")
                            _downloadState.value = DownloadState.Caching
                            while(cachingDuration > 0) {
                                ensureActive()
                                sleep(1000)
                                --cachingDuration
                            }
                            --retries
                        } else {
                            throw Exception("Download failed with code: ${response.code()} and message: ${response.message()}")
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
