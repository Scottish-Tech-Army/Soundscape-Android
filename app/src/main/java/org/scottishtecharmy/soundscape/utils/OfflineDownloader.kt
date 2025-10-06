package org.scottishtecharmy.soundscape.utils

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.network.IManifestDAO
import org.scottishtecharmy.soundscape.network.ManifestClient
import org.scottishtecharmy.soundscape.utils.OfflineDownloader.Companion.TAG
import retrofit2.awaitResponse
import java.io.File
import java.io.FileOutputStream

suspend fun downloadAndParseManifest(applicationContext: Context) : FeatureCollection? {

    return withContext(Dispatchers.IO) {
        val manifestClient = ManifestClient(applicationContext)

        val service =
            manifestClient.retrofitInstance?.create(IManifestDAO::class.java)
        val manifestReq =
            async {
                service?.getManifest()
            }
        manifestReq.await()?.awaitResponse()?.body()
    }
}
fun deleteAllProgressFiles(context: Context) {
    // Delete all progress files
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val path = sharedPreferences.getString(
        MainActivity.SELECTED_STORAGE_KEY,
        MainActivity.SELECTED_STORAGE_DEFAULT
    )
    val extractsDir = File(path, Environment.DIRECTORY_DOWNLOADS)
    if (extractsDir.exists() && extractsDir.isDirectory) {
        val files =
            extractsDir.listFiles { file -> file.name.endsWith(".downloadId") }?.toList()
                ?: emptyList()
        for (file in files) {
            Log.d(TAG, "Delete downloadId file: ${file.path}")
            file.delete()
        }
    }
}

class OfflineDownloader(private val context: Context) {

    companion object {
        const val TAG = "OfflineDownloader"
    }

    private var downloadId: Long = -1L

    private fun getDownloadErrorReason(reasonCode: Int): String {
        return when (reasonCode) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot Resume"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Device Not Found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File Already Exists"
            DownloadManager.ERROR_FILE_ERROR -> "File Error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP Data Error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient Space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too Many Redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP Code"
            DownloadManager.ERROR_UNKNOWN -> "Unknown Error"
            else -> "Unknown Error Code: $reasonCode"
        }
    }

    fun startDownload(fileUrl: String, outputFilePath: String, title: String = "File Download", description: String = "Downloading...") {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(fileUrl.toUri())

        // --- Basic Request Configuration ---
        request.setTitle(title) // Title for the download notification
        request.setDescription(description) // Description for the download notification

        val path = File("$outputFilePath.downloading")
        request.setDestinationUri(path.toUri())

        // --- Network Type ---
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        request.setAllowedOverMetered(false) // Allow download over metered connections (e.g. cellular)
        request.setAllowedOverRoaming(false) // Disallow download over roaming

        // --- Notification Visibility ---
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        try {
            downloadId = downloadManager.enqueue(request)
            Log.i(TAG, "Download enqueued with ID: $downloadId for URL: $fileUrl")
            Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()

            // Create a file to store the id and show that download is in progress
            val progressFile = FileOutputStream("$outputFilePath.downloadId")
            progressFile.write("$downloadId".toByteArray())
            progressFile.close()

        } catch (e: Exception) {
            Log.e(TAG, "Error enqueuing download for $fileUrl", e)
            Toast.makeText(context, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    data class DownloadStatus (
        val bytesSoFar: Long,
        val totalBytes: Long,
        val managerStatus: Int
    )
    fun getDownloadStatus(): DownloadStatus? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)
        var result: DownloadStatus? = null
        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(statusIndex)
            var bytes = -1L
            var totalBytes = -1L
            var filePath: String? = null
            if(status != DownloadManager.STATUS_FAILED) {
                val bytesIndex =
                    cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                bytes = cursor.getLong(bytesIndex)
                totalBytes = cursor.getLong(totalBytesIndex)

                println("$bytes/$totalBytes status=$status")
                if(bytes == totalBytes) {
                    val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val localUri = cursor.getString(localUriIndex)
                    val fileUri = localUri.toUri()

                    // We're complete
                    filePath = fileUri.path?.removeSuffix(".downloading")
                    if(filePath != null) {
                        // Remove the progress file
                        val downloadIdFile = File("$filePath.downloadId")
                        val deleteSuccess = downloadIdFile.delete()
                        println("Deleting ${downloadIdFile.path} returned $deleteSuccess")

                        // And rename our complete downloaded file
                        val downloadedFile = File(fileUri.path!!)
                        val renameSuccess = downloadedFile.renameTo(File(filePath))
                        println("Renaming ${downloadedFile.path} returned $renameSuccess")
                    }
                }
            }
            result = DownloadStatus(
                bytes,
                totalBytes,
                status
            )
       } else {
            // We reach here if the download was cancelled
        }
        cursor?.close()

        if(result == null) {
            // Download has failed or was cancelled, delete all progress file
            deleteAllProgressFiles(context)
        }

        return result
    }

    fun cancelDownload() {
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
            Log.i(TAG, "Attempted to cancel download with ID: $downloadId")
        }
    }

    fun midDownload(id: Long) {
        downloadId = id
    }
}
