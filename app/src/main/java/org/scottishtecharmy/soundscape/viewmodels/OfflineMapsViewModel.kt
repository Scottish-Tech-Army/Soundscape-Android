package org.scottishtecharmy.soundscape.viewmodels

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.text.format.Formatter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import org.scottishtecharmy.soundscape.utils.OfflineDownloader
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.utils.deleteAllProgressFiles
import org.scottishtecharmy.soundscape.utils.downloadAndParseManifest
import org.scottishtecharmy.soundscape.utils.findExtracts
import org.scottishtecharmy.soundscape.utils.getOfflineMapStorage
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.collections.HashMap

data class OfflineMapsUiState(
    val isDownloading: Boolean = false,
    val downloadingExtractName:String = "",

    val downloadProgress: Int = 0,

    // Extracts in manifest to choose from
    val nearbyExtracts: FeatureCollection? = null,

    // Offline extracts in storage
    val downloadedExtracts: FeatureCollection? = null,

    // Storage status
    val currentPath: String = "",
    val storages: List<StorageUtils.StorageSpace> = emptyList()
)

@HiltViewModel
class OfflineMapsViewModel @Inject constructor(
    private val soundscapeServiceConnection: SoundscapeServiceConnection,
    @param:ApplicationContext val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineMapsUiState())
    val uiState: StateFlow<OfflineMapsUiState> = _uiState
    lateinit var offlineDownloader: OfflineDownloader
    var urlRedirect = ""

    init {
        viewModelScope.launch {
            // Create downloader to handle getting any offline maps
            offlineDownloader = OfflineDownloader(appContext)

            val storages = getOfflineMapStorage(appContext)

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
            var path = sharedPreferences.getString(MainActivity.SELECTED_STORAGE_KEY, MainActivity.SELECTED_STORAGE_DEFAULT)!!
            val extractCollection = findExtracts(File(path, Environment.DIRECTORY_DOWNLOADS).path)
            _uiState.value = _uiState.value.copy(
                downloadedExtracts = extractCollection,
                storages = storages,
                currentPath = path
            )

            val (fc, redirect) = downloadAndParseManifest(appContext)
            if(fc != null) {
                urlRedirect = redirect
                val tree = FeatureTree(fc)
                soundscapeServiceConnection.serviceBoundState.collect {
                    Log.d(TAG, "serviceBoundState $it")
                    if(it) {
                        soundscapeServiceConnection.getLocationFlow()?.value?.let { androidLocation ->
                            val location = LngLatAlt(androidLocation.longitude, androidLocation.latitude)
                            // Containing polygons gives offline maps that include the current location
                            val extracts = tree.getContainingPolygons(location)

                            for(extract in extracts.features) {
                                val size = extract.properties?.get("extract-size") as Double
                                val properties: HashMap<String, Any?> = extract.properties!!
                                properties["extract-size-string"] = Formatter.formatFileSize(appContext, size.toLong())
                                extract.properties = properties

                                Log.d(TAG, "extract: ${extract.properties}")
                            }
                            _uiState.value = _uiState.value.copy(
                                nearbyExtracts = extracts,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdates()
    }

    // Add this new property to your ViewModel
    private var progressJob: Job? = null

    // Add this new function to your ViewModel
    private fun startProgressUpdates() {
        progressJob = viewModelScope.launch {
            var delayCount = 4
            while (isActive) { // This loop will run as long as the coroutine is active
                val downloadStatus = offlineDownloader.getDownloadStatus()
                if(downloadStatus != null) {
                    val bytesSoFar = downloadStatus.bytesSoFar.toDouble()
                    val totalBytes = downloadStatus.totalBytes.toDouble()

                    val percentage = if (totalBytes > 0) {
                        ((bytesSoFar / totalBytes) * 100.0).toInt()
                    } else {
                        0
                    }

                    _uiState.value = _uiState.value.copy(
                        downloadProgress = percentage,
                    )

                    if(downloadStatus.managerStatus == DownloadManager.STATUS_FAILED) {
                        // Tidy up after failed download
                        println("Download failed")
                        deleteAllProgressFiles(appContext)
                    }

                    if((downloadStatus.managerStatus == DownloadManager.STATUS_SUCCESSFUL) ||
                        (downloadStatus.managerStatus == DownloadManager.STATUS_FAILED)) {
                        if (delayCount > 0) {
                            // We want to allow the status to be displayed at 100% before moving
                            // back to the overview screen. This should likely be in the UI code
                            // rather than the view model...
                            --delayCount
                        }
                        else {
                            val extractsDir =
                                File(_uiState.value.currentPath, Environment.DIRECTORY_DOWNLOADS)
                            val extractCollection = findExtracts(extractsDir.path)
                            _uiState.value = _uiState.value.copy(
                                downloadedExtracts = extractCollection,
                                isDownloading = false
                            )
                            break
                        }
                    }
                } else {
                    Log.e(TAG, "Download progress is null, it has been cancelled")
                    stopProgressUpdates()
                    break
                }
                delay(1000)
            }
            println("Done monitoring progress")
        }
    }

    // Add this function to stop the updates
    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
        // Reset progress state when stopping
        _uiState.value = _uiState.value.copy(
            isDownloading = false,
            downloadProgress = 0,
        )
    }

    fun delete(feature: Feature) {
        val filename = feature.properties?.get("filename")
        if(filename != null) {
            val extractsDir = File(_uiState.value.currentPath, Environment.DIRECTORY_DOWNLOADS)
            if (extractsDir.exists() && extractsDir.isDirectory) {
                val files = extractsDir.listFiles { file ->
                    file.name.startsWith(filename as String)
                }?.toList() ?: emptyList()

                // Delete whatever we find
                for(file in files)
                    file.delete()

                // Update the UI to reflect the deletions
                val extractCollection = findExtracts(extractsDir.path)
                _uiState.value = _uiState.value.copy(
                    downloadedExtracts = extractCollection
                )
            }
        }
    }

    fun download(name: String, feature: Feature, wifiOnly: Boolean) {
        val filename = feature.properties?.get("filename")
        if(filename != null) {
            val path = _uiState.value.currentPath + "/" + Environment.DIRECTORY_DOWNLOADS + "/" +  filename as String

            // Before starting the download, we delete any previous version
            val file = File(path)
            file.delete()

            // Write out the feature metadata to a file
            val adapter = GeoJsonObjectMoshiAdapter()
            val metadataOutputFile = FileOutputStream("$path.geojson")
            metadataOutputFile.write(adapter.toJson(feature).toByteArray())
            metadataOutputFile.close()

            val fileUrl = "$urlRedirect$filename"
            offlineDownloader.startDownload(
                fileUrl,
                path,
                wifiOnly,
                "Soundscape offline maps",
                "Downloading $filename extract"
            )
            _uiState.value = _uiState.value.copy(
                downloadingExtractName = name,
                isDownloading = true
            )

            startProgressUpdates()
        }
    }

    fun cancelDownload() {
        offlineDownloader.cancelDownload()
    }

    fun midDownload(downloadId: Long) {
        offlineDownloader.midDownload(downloadId)
        _uiState.value = _uiState.value.copy(
            downloadingExtractName = "",
            isDownloading = true
        )
        startProgressUpdates()
    }

    companion object {
        private const val TAG = "OfflineMapsViewModel"
    }
}