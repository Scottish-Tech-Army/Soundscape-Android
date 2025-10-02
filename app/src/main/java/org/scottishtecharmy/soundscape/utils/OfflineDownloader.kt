package org.scottishtecharmy.soundscape.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.network.IManifestDAO
import org.scottishtecharmy.soundscape.network.ManifestClient
import retrofit2.awaitResponse
import androidx.core.net.toUri

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

class OfflineDownloader(private val context: Context) {

    companion object {
        const val TAG = "OfflineDownloader"
    }

    private var downloadId: Long = -1L

    // BroadcastReceiver to listen for download completion
    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    handleDownloadCompletion(id)
                } else if (intent.action == DownloadManager.ACTION_NOTIFICATION_CLICKED) {
                    // Handle notification click if needed - e.g. open downloads app or your app
                    Log.d(TAG, "Download notification clicked for ID: $id")

                    // Example: Open the Android Downloads UI
                    val downloadsIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                    downloadsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(downloadsIntent)
                }
            }
        }
    }

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
                    // You can now use the downloadedFileUriString (e.g., to get a File path if needed,
                    // or open an InputStream from the content URI)
                    // Example: Get file path if it's in app's directory
                    // val file = File(Uri.parse(downloadedFileUriString).path!!)
                    // if(file.exists()){ Log.d(TAG, "File exists at: ${file.absolutePath}") }
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
            cursor.close()
        } else {
            Log.e(TAG, "Download ID $id not found in DownloadManager query after completion.")
        }
        unregisterReceiver() // Unregister after handling
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

            // Register receiver for download completion and notification click
            registerReceiver()

        } catch (e: Exception) {
            Log.e(TAG, "Error enqueuing download for $fileUrl", e)
            Toast.makeText(context, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun registerReceiver() {
        // Unregister first if already registered to avoid multiple registrations
        unregisterReceiverSilently()

        val intentFilter = IntentFilter().apply {
            addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        }
        // For Android N (API 24) and above, you might need to specify Context.RECEIVER_NOT_EXPORTED
        // if your receiver is not meant to be exported. For older versions, this flag doesn't exist.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onDownloadComplete, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(onDownloadComplete, intentFilter)
        }
        Log.d(TAG, "Download completion receiver registered.")
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(onDownloadComplete)
            Log.d(TAG, "Download completion receiver unregistered.")
        } catch (e: IllegalArgumentException) {
            // Receiver wasn't registered, ignore.
            Log.d(TAG, "Receiver already unregistered or not registered $e")
        }
    }

    private fun unregisterReceiverSilently() {
        try {
            context.unregisterReceiver(onDownloadComplete)
        } catch (_: IllegalArgumentException) {
            // Ignore if not registered
        }
    }


    // Optional: Query download status by ID
    fun getDownloadStatus(id: Long): Pair<Int, Int>? { // Returns Pair<Status, Reason>
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(id)
        val cursor: Cursor? = downloadManager.query(query)
        var result: Pair<Int, Int>? = null
        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
            val status = cursor.getInt(statusIndex)
            val reason = cursor.getInt(reasonIndex)
            result = Pair(status, reason)
            cursor.close()
        }
        return result
    }

    // Optional: Cancel a download
    fun cancelDownload(id: Long) {
        if (id != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(id)
            Log.i(TAG, "Attempted to cancel download with ID: $id")
            // Note: Unregistering receiver is typically done on completion/failure or when
            // the component managing this download is destroyed.
        }
    }
}
