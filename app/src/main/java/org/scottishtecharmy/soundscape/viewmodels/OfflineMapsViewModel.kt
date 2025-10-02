package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.os.Environment
import android.text.format.Formatter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.OfflineDownloader
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.utils.downloadAndParseManifest
import java.io.File
import javax.inject.Inject
import kotlin.collections.HashMap

data class OfflineMapsUiState(
    val isLoading: Boolean = true,
    val isDownloading: Boolean = false,
    val downloadingExtractName:String = "",

    val downloadProgress: Int = 0,
    val downloadProgressBytes: Pair<Long, Long> = Pair(0L, 0L),

    // Extracts in manifest to choose from
    val nearbyExtracts: FeatureCollection? = null,

    // Offline extracts in storage
    val downloadedExtracts: List<File> = emptyList(),

    // Storage status
    val internalStorage: StorageUtils.StorageSpace? = null,
    val externalStorages: List<StorageUtils.StorageSpace> = emptyList()
)

@HiltViewModel
class OfflineMapsViewModel @Inject constructor(
    private val soundscapeServiceConnection: SoundscapeServiceConnection,
    @ApplicationContext appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineMapsUiState())
    val uiState: StateFlow<OfflineMapsUiState> = _uiState
    lateinit var offlineDownloader: OfflineDownloader

    init {
        viewModelScope.launch {
            // Create downloader to handle getting any offline maps
            offlineDownloader = OfflineDownloader(appContext)

            val fc = downloadAndParseManifest(appContext)
            if(fc != null) {
                val tree = FeatureTree(fc)

                val internalSpace = StorageUtils.getInternalStorageSpace(appContext)
                internalSpace?.let {
                    Log.d("StorageCheck", "Internal Storage:\n$it")
                } ?: Log.e("StorageCheck", "Could not get internal storage info.")

                val externalAppSpecificSpaces = StorageUtils.getExternalStorageSpacesAppSpecific(appContext)
                if (externalAppSpecificSpaces.isNotEmpty()) {
                    externalAppSpecificSpaces.forEach {
                        Log.d("StorageCheck", "External App-Specific Volume:\n$it")
                    }
                }

                // Find any extracts that we have downloaded
                val extractDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                var files: Array<File>? = null
                extractDir?.let { directory ->
                    if(directory.exists() && directory.isDirectory) {
                        // Find the first extract within the directory
                        files = directory.listFiles()
                    }
                }

                _uiState.value = _uiState.value.copy(
                    downloadedExtracts = files?.toList() ?: emptyList(),
                    internalStorage = internalSpace,
                    externalStorages = externalAppSpecificSpaces
                )

                soundscapeServiceConnection.serviceBoundState.collect {
                    Log.d(TAG, "serviceBoundState $it")
                    if(it) {
                        soundscapeServiceConnection.getLocationFlow()?.value?.let { androidLocation ->
                            val location = LngLatAlt(androidLocation.longitude, androidLocation.latitude)
                            // Containing polygons gives offline maps that include the current location
                            val extracts = tree.getContainingPolygons(location)

//                            // Nearest gives offline maps that are closest to the current location
//                            // and can include much more distant ones. More useful for when we add
//                            // support for multiple extracts.
//                            val extracts = tree.getNearestCollection(
//                                location,
//                                1000000.0,
//                                20,
//                                CheapRuler(location.latitude)
//                            )

                            for(extract in extracts.features) {
                                val size = extract.properties?.get("extract-size") as Double
                                val properties: HashMap<String, Any?> = extract.properties!!
                                properties["extract-size-string"] = Formatter.formatFileSize(appContext, size.toLong())
                                extract.properties = properties

                                Log.d(TAG, "extract: ${extract.properties}")
                            }
                            _uiState.value = _uiState.value.copy(
                                nearbyExtracts = extracts,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        //offlineDownloader.unregisterReceiver()
        stopProgressUpdates()
    }

    // Add this new property to your ViewModel
    private var progressJob: Job? = null

    // Add this new function to your ViewModel
    private fun startProgressUpdates() {
        progressJob = viewModelScope.launch {
            while (isActive) { // This loop will run as long as the coroutine is active
                val progress = offlineDownloader.getDownloadStatus()
                if(progress != null) {
                    // The status from DownloadManager is a Pair of (bytesSoFar, totalBytes)
                    val bytesSoFar = progress.first.toLong()
                    val totalBytes = progress.second.toLong()

                    val percentage = if (totalBytes > 0) {
                        ((bytesSoFar * 100) / totalBytes).toInt()
                    } else {
                        0
                    }

                    _uiState.value = _uiState.value.copy(
                        downloadProgress = percentage,
                        downloadProgressBytes = Pair(bytesSoFar, totalBytes)
                    )

                    if(bytesSoFar == totalBytes) {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false
                        )
                    }
                } else {
                    Log.e(TAG, "Download progress is null, it has been cancelled")
                    stopProgressUpdates()
                }
                delay(500)
            }
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
            downloadProgressBytes = Pair(0L, 0L)
        )
    }

    fun download(name: String, feature: Feature) {
        val filename = feature.properties?.get("filename")
        if(filename != null) {
            val fileUrl = "https://commcouncil.scot/$filename"
            offlineDownloader.startDownload(
                fileUrl,
                filename as String,
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

    companion object {
        private const val TAG = "OfflineMapsViewModel"
    }
}