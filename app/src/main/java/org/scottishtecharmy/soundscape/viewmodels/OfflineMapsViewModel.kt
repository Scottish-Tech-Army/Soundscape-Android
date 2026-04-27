package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.os.Environment
import android.text.format.Formatter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.OfflineMapsUiState
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.StorageInfo
import org.scottishtecharmy.soundscape.utils.DownloadState
import org.scottishtecharmy.soundscape.utils.OfflineDownloader
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.utils.downloadAndParseManifest
import org.scottishtecharmy.soundscape.utils.findExtracts
import org.scottishtecharmy.soundscape.utils.getOfflineMapStorage
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.HashMap

private fun StorageUtils.StorageSpace.toStorageInfo(): StorageInfo =
    StorageInfo(path = path, description = description, availableString = availableString)

private fun DownloadState.toCommon(): DownloadStateCommon = when (this) {
    is DownloadState.Idle -> DownloadStateCommon.Idle
    is DownloadState.Caching -> DownloadStateCommon.Caching
    is DownloadState.Downloading -> DownloadStateCommon.Downloading(progress)
    is DownloadState.Success -> DownloadStateCommon.Success
    is DownloadState.Canceled -> DownloadStateCommon.Canceled
    is DownloadState.Error -> DownloadStateCommon.Error(message)
}

class OfflineMapsViewModel(
    val appContext: Context,
    private val locationDescription: LocationDescription
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineMapsUiState())
    val uiState: StateFlow<OfflineMapsUiState> = _uiState

    private val offlineDownloader = OfflineDownloader()
    val downloadState: StateFlow<DownloadStateCommon> = offlineDownloader.downloadState
        .map { it.toCommon() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DownloadStateCommon.Idle,
        )

    init {
        viewModelScope.launch {
            val storages = getOfflineMapStorage(appContext)

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
            var path = sharedPreferences.getString(MainActivity.SELECTED_STORAGE_KEY, MainActivity.SELECTED_STORAGE_DEFAULT)!!
            val extractCollection = findExtracts(File(path, Environment.DIRECTORY_DOWNLOADS).path)
            _uiState.value = _uiState.value.copy(
                downloadedExtracts = extractCollection,
                storages = storages.map { it.toStorageInfo() },
                currentPath = path
            )

            val fc = downloadAndParseManifest(appContext)
            if(fc != null) {
                val tree = FeatureTree(fc)

                val location = locationDescription.location
                println("Location $location")
                // Containing polygons gives offline maps that include the current location
                val extracts = tree.getContainingPolygons(location)

                println("Extracts ${extracts.features.size}")
                for(extract in extracts.features) {
                    val size = extract.properties?.get("extract-size") as Double
                    val properties: HashMap<String, Any?> = extract.properties!!
                    properties["extract-size-string"] = Formatter.formatFileSize(appContext, size.toLong())
                    extract.properties = properties

                    Log.d(TAG, "extract: ${extract.properties}")
                }
                _uiState.value = _uiState.value.copy(
                    nearbyExtracts = extracts
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    manifestError = true
                )
            }
        }
    }

    private fun translateToLocalFilenameFrom(filename: String) : String {
        return filename.substringAfter("-").substringAfter("-")
    }

    fun delete(feature: Feature) {
        val filename = feature.properties?.get("filename")
        if(filename != null) {
            val localFilename = translateToLocalFilenameFrom(filename as String)
            val extractsDir = File(_uiState.value.currentPath, Environment.DIRECTORY_DOWNLOADS)
            if (extractsDir.exists() && extractsDir.isDirectory) {
                val files = extractsDir.listFiles { file ->
                    file.name.startsWith(localFilename)
                }?.toList() ?: emptyList()

                // Delete whatever we find
                for(file in files)
                    file.delete()

                refreshExtracts()
            }
        }
    }

    fun download(name: String, feature: Feature) {
        val filename = feature.properties?.get("filename")
        if(filename != null) {
            val localFilename = translateToLocalFilenameFrom(filename as String)
            val path = _uiState.value.currentPath + "/" + Environment.DIRECTORY_DOWNLOADS + "/" +  localFilename

            // Write out the feature metadata to a file
            val adapter = GeoJsonObjectMoshiAdapter()
            val metadataOutputFile = FileOutputStream("$path.geojson")
            metadataOutputFile.write(adapter.toJson(feature).toByteArray())
            metadataOutputFile.close()

            val extractSize = feature.properties?.get("extract-size") as Double?
            val fileUrl = "${BuildConfig.EXTRACT_PROVIDER_URL}$filename"
            offlineDownloader.startDownload(
                fileUrl,
                path,
                extractSize
            )
            _uiState.value = _uiState.value.copy(
                downloadingExtractName = name
            )
        }
    }

    fun cancelDownload() {
        offlineDownloader.cancelDownload()
    }

    fun refreshExtracts() {
        // Update the UI to reflect the deletions
        val extractsDir = File(_uiState.value.currentPath, Environment.DIRECTORY_DOWNLOADS)
        val extractCollection = findExtracts(extractsDir.path)
        _uiState.value = _uiState.value.copy(
            downloadedExtracts = extractCollection
        )
    }

    companion object {
        private const val TAG = "OfflineMapsViewModel"
    }
}