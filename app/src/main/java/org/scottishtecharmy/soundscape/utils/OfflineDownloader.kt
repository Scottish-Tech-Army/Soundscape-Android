package org.scottishtecharmy.soundscape.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.network.IManifestDAO
import org.scottishtecharmy.soundscape.network.ManifestClient
import org.scottishtecharmy.soundscape.utils.OfflineDownloader.Companion.TAG
import retrofit2.awaitResponse

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

/**
 * OfflineMapsBroadcastReceiver is declared in AndroidManifest.xml as a receiver. Even if the app
 * is not running it will be able to receive the broadcast when a download completes. This is
 * important because at that point we want to record the download as having succeeded.
  */
class OfflineMapsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val query = DownloadManager.Query().setFilterById(id)
            val cursor = manager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                if (cursor.count > 0) {

                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(statusIndex)
                    val fileIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val file = cursor.getString(fileIndex)
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        println("DOWNLOAD SUCCESS - $file")
                    } else {
                        val messageIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val message = cursor.getInt(messageIndex)
                        println("DOWNLOAD FAILED for $file - $message")
                    }
                }
            } else {
                Log.e(TAG, "Download ID $id not found in DownloadManager query after completion.")
            }
            cursor?.close()
        }
    }
}

class OfflineDownloader(private val context: Context) {

    companion object {
        const val TAG = "OfflineDownloader"
    }

    private var downloadId: Long = -1L

    private fun handleDownloadCompletion(id: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(id)
        val cursor: Cursor? = downloadManager.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON) // For error details
            val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

            val status = cursor.getInt(statusIndex)

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val downloadedFileUriString = cursor.getString(localUriIndex)
                    Log.i(TAG, "Download $id successful. File URI: $downloadedFileUriString")
                    Toast.makeText(context, "Download successful: ${downloadedFileUriString.toUri().lastPathSegment}", Toast.LENGTH_LONG).show()
                }
                DownloadManager.STATUS_FAILED -> {
                    val reason = cursor.getInt(reasonIndex)
                    val reasonText = getDownloadErrorReason(reason)
                    Log.e(TAG, "Download $id failed. Reason: $reasonText (Code: $reason)")
                    Toast.makeText(context, "Download failed: $reasonText", Toast.LENGTH_LONG).show()
                }
                DownloadManager.STATUS_PAUSED -> {
                    Log.w(TAG, "Download $id paused.")
                    Toast.makeText(context, "Download paused", Toast.LENGTH_SHORT).show()
                }
                DownloadManager.STATUS_PENDING -> {
                    Log.i(TAG, "Download $id pending.")
                }
                DownloadManager.STATUS_RUNNING -> {
                    Log.i(TAG, "Download $id running.")
                }
            }
        } else {
            Log.e(TAG, "Download ID $id not found in DownloadManager query after completion.")
        }
        cursor?.close()
    }


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


    fun startDownload(fileUrl: String, outputFileName: String, title: String = "File Download", description: String = "Downloading...") {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(fileUrl.toUri())

        // --- Basic Request Configuration ---
        request.setTitle(title) // Title for the download notification
        request.setDescription(description) // Description for the download notification

        // --- Destination ---
        // Save to app-specific directory on external storage (recommended for app files)
        // This directory is private to your app but on external storage.
        // It's automatically cleaned up when the app is uninstalled.
        val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (destinationDir != null && (!destinationDir.exists() || !destinationDir.isDirectory)) {
            destinationDir.mkdirs() // Ensure the directory exists
        }
        // Setting destination to a file inside your app's external files directory
        // The file will be named outputFileName within the 'downloads' sub-folder of your app's external files dir.
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, outputFileName)

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
        } catch (e: Exception) {
            Log.e(TAG, "Error enqueuing download for $fileUrl", e)
            Toast.makeText(context, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun getDownloadStatus(): Pair<Int, Int>? { // Returns Pair<Status, Reason>
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)
        var result: Pair<Int, Int>? = null
        if (cursor != null && cursor.moveToFirst()) {
            val bytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val bytes = cursor.getInt(bytesIndex)
            val totalBytes = cursor.getInt(totalBytesIndex)
            result = Pair(bytes, totalBytes)
        } else {
            // We reach here if the download was cancelled
        }
        cursor?.close()
        return result
    }

    fun cancelDownload() {
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
            Log.i(TAG, "Attempted to cancel download with ID: $downloadId")
        }
    }
}
