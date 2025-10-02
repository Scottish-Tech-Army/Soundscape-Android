package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.text.format.Formatter
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.utils.downloadAndParseManifest
import javax.inject.Inject
import kotlin.collections.HashMap

data class OfflineMapsUiState(
    val isLoading: Boolean = true,

    // Extracts to choose from
    val nearbyExtracts: FeatureCollection? = null,

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

    init {
        viewModelScope.launch {
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
                _uiState.value = _uiState.value.copy(
                    internalStorage = internalSpace,
                    externalStorages = externalAppSpecificSpaces
                )

                soundscapeServiceConnection.serviceBoundState.collect {
                    Log.d(TAG, "serviceBoundState $it")
                    if(it) {
                        soundscapeServiceConnection.getLocationFlow()?.value?.let { androidLocation ->
                            val location = LngLatAlt(androidLocation.longitude, androidLocation.latitude)
                            val extracts = tree.getContainingPolygons(location)
                            for(extract in extracts.features) {
                                val size = extract.properties?.get("extract-size") as Double
                                val properties: HashMap<String, Any?> = extract.properties!!
                                properties["extract-size-string"] = Formatter.formatFileSize(appContext, size.toLong())
                                extract.properties = properties
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

    fun download(activity: MainActivity, feature: Feature) {
        val filename = feature.properties?.get("filename")
        if(filename != null) {
            val fileUrl = "https://commcouncil.scot/$filename"
            activity.offlineDownloader.startDownload(
                fileUrl,
                filename as String,
                "Soundscape offline maps",
                "Downloading $filename extract"
            )
        }
    }

    companion object {
        private const val TAG = "OfflineMapsViewModel"
    }
}