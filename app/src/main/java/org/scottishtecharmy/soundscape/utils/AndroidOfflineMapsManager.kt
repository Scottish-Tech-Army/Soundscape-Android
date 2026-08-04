package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.preference.PreferenceManager
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.NearbyExtractsState
import java.io.File
import java.io.FileOutputStream

private fun DownloadState.toCommon(): DownloadStateCommon = when (this) {
    is DownloadState.Idle -> DownloadStateCommon.Idle
    is DownloadState.Caching -> DownloadStateCommon.Caching
    is DownloadState.Downloading -> DownloadStateCommon.Downloading(progress)
    is DownloadState.Success -> DownloadStateCommon.Success
    is DownloadState.Canceled -> DownloadStateCommon.Canceled
    is DownloadState.Error -> DownloadStateCommon.Error(message)
}

/**
 * Session-scope facade over the offline-map manifest fetcher, file system index,
 * and OfflineDownloader. Implements the surface area the shared NavGraph
 * expects on iOS via OfflineMapManager: full list of manifest extracts,
 * downloaded extracts as a FeatureCollection, current download state, and
 * refresh / containing-query / start / cancel / delete operations.
 */
class AndroidOfflineMapsManager(
    private val appContext: Context,
    private val soundscapeServiceConnection: SoundscapeServiceConnection,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _nearbyExtractsState =
        MutableStateFlow<NearbyExtractsState>(NearbyExtractsState.Loading)
    val nearbyExtractsState: StateFlow<NearbyExtractsState> = _nearbyExtractsState.asStateFlow()

    private val _downloadedExtractsFc = MutableStateFlow(FeatureCollection())
    val downloadedExtractsFc: StateFlow<FeatureCollection> = _downloadedExtractsFc.asStateFlow()

    private val downloader = OfflineDownloader()
    val downloadState: StateFlow<DownloadStateCommon> = downloader.downloadState
        .map { it.toCommon() }
        .stateIn(scope, SharingStarted.Eagerly, DownloadStateCommon.Idle)

    private var manifestTree: FeatureTree? = null

    init {
        scope.launch {
            downloader.downloadState.collect { state ->
                if (state == DownloadState.Success) {
                    refreshDownloaded()
                    // Make sure the geoengine starts using the newly published extract (and
                    // stops using whatever it superseded) immediately, rather than waiting for
                    // the user to happen to walk outside the current tile grid.
                    soundscapeServiceConnection.refreshOfflineMaps()
                }
            }
        }
    }

    private fun extractsDir(): File {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        val path = sharedPreferences.getString(
            MainActivity.SELECTED_STORAGE_KEY,
            MainActivity.SELECTED_STORAGE_DEFAULT,
        )!!
        return File(path, Environment.DIRECTORY_DOWNLOADS)
    }

    fun refresh() {
        _nearbyExtractsState.value = NearbyExtractsState.Loading
        scope.launch {
            val fc = downloadAndParseManifest(appContext)
            if (fc != null) {
                manifestTree = FeatureTree(fc)
                fc.features.forEach { feature -> annotateExtractSize(feature) }
                _nearbyExtractsState.value = NearbyExtractsState.Loaded(fc)
            } else {
                Log.w(TAG, "Manifest fetch failed")
                _nearbyExtractsState.value = NearbyExtractsState.Error
            }
            refreshDownloaded()
        }
    }

    private fun refreshDownloaded() {
        val dir = extractsDir()
        _downloadedExtractsFc.value = findExtracts(dir.path) ?: FeatureCollection()
    }

    fun getExtractsContaining(location: LngLatAlt): List<Feature> {
        val tree = manifestTree ?: return emptyList()
        return tree.getContainingPolygons(location).features
    }

    /**
     * The stable identity for an extract's on-disk files, e.g. "glasgow-gb" - everything before
     * the ".pmtiles" extension. Every physical file for this extract (the base .pmtiles name from
     * before versioned downloads existed, any "<logicalBase>.v<version>.pmtiles" from
     * [startDownload], and their .geojson sidecars) starts with "$logicalBase.".
     */
    private fun logicalBaseNameFor(filename: String): String {
        val localFilename = filename.substringAfter("-").substringAfter("-")
        return localFilename.removeSuffix(".pmtiles")
    }

    fun startDownload(name: String, feature: Feature) {
        val filename = feature.properties?.get("filename") as? String ?: return
        val logicalBase = logicalBaseNameFor(filename)
        // Never reuse a previous download's filename for a re-download/"Update": MapLibre's
        // native PMTilesFileSource caches parsed header/directory data per pmtiles://file://
        // URL forever, with no way for us to invalidate it, so overwriting an already-opened
        // path leaves it reading new bytes through stale cached offsets - which can crash.
        // Giving each download a unique, never-before-seen filename means MapLibre never
        // revisits a URL it has cached anything for. OfflineDownloader deletes the previous
        // version's files once this one is published (see logicalBaseName below).
        val versionedFilename = "$logicalBase.v${System.currentTimeMillis()}.pmtiles"
        val path = "${extractsDir().path}/$versionedFilename"
        try {
            val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
            val adapter = moshi.adapter(Feature::class.java)
            FileOutputStream("$path.geojson").use {
                it.write(
                    adapter.toJson(feature).toByteArray()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write extract metadata", e)
        }
        val extractSize = (feature.properties?.get("extract-size") as? Number)?.toDouble()
        downloader.startDownload(
            "${BuildConfig.EXTRACT_PROVIDER_URL}$filename",
            path,
            extractSize,
            logicalBaseName = logicalBase,
        )
    }

    fun deleteExtractByFeature(feature: Feature) {
        val filename = feature.properties?.get("filename") as? String ?: return
        val logicalBase = logicalBaseNameFor(filename)
        val dir = extractsDir()
        if (dir.exists() && dir.isDirectory) {
            // "$logicalBase." matches every version of this extract - the pre-versioning
            // "glasgow-gb.pmtiles" as well as any "glasgow-gb.v<version>.pmtiles" - plus their
            // .geojson sidecars, without matching an unrelated "glasgow-gbz.pmtiles".
            dir.listFiles { f -> f.name.startsWith("$logicalBase.") }?.forEach { it.delete() }
            refreshDownloaded()
            // Stop the geoengine using the now-deleted extract immediately, rather than waiting
            // for the user to happen to walk outside the current tile grid.
            soundscapeServiceConnection.refreshOfflineMaps()
        }
    }

    fun cancelDownload() {
        downloader.cancelDownload()
    }

    private fun annotateExtractSize(feature: Feature) {
        val size = (feature.properties?.get("extract-size") as? Number)?.toLong() ?: return
        val props = feature.properties ?: return
        val localized = ComposeLocalizedStrings()
        props["extract-size-string"] = formatBytes(size, localized)
        props["extract-size-a11y-string"] = formatBytes(size, localized, forAccessibility = true)
        feature.properties = props
    }

    companion object {
        private const val TAG = "AndroidOfflineMaps"
    }
}
