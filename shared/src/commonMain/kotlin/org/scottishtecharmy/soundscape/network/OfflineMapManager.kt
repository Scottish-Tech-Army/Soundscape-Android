package org.scottishtecharmy.soundscape.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import org.scottishtecharmy.soundscape.geoengine.utils.polygonContainsCoordinates
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.utils.findExtractPaths

/**
 * Cross-platform offline map manager. Handles:
 * - Fetching and parsing the extract manifest
 * - Downloading PMTiles extracts
 * - Discovering downloaded extracts on disk
 * - Managing download state for the UI
 */
class OfflineMapManager(
    private val manifestClient: ManifestClient,
    private val fileDownloader: FileDownloaderInterface,
    private val extractBasePath: String,
    private val extractBaseUrl: String,
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Manifest data
    private val _manifest = MutableStateFlow<FeatureCollection?>(null)
    val manifest: StateFlow<FeatureCollection?> = _manifest.asStateFlow()

    // Available extracts from manifest (all of them — UI can filter by location)
    private val _availableExtracts = MutableStateFlow<List<Feature>>(emptyList())
    val availableExtracts: StateFlow<List<Feature>> = _availableExtracts.asStateFlow()

    // Downloaded extracts
    private val _downloadedExtracts = MutableStateFlow<List<String>>(emptyList())
    val downloadedExtracts: StateFlow<List<String>> = _downloadedExtracts.asStateFlow()

    // Download state
    private val _downloadState = MutableStateFlow<DownloadStateCommon>(DownloadStateCommon.Idle)
    val downloadState: StateFlow<DownloadStateCommon> = _downloadState.asStateFlow()

    private var downloadJob: Job? = null

    /**
     * Fetch the manifest and list of downloaded extracts.
     */
    fun refresh() {
        scope.launch {
            // Fetch manifest
            try {
                val json = manifestClient.getManifestJson()
                if (json != null) {
                    val fc = GeoJsonParser.parseFeatureCollection(json)
                    _manifest.value = fc
                    _availableExtracts.value = fc?.features ?: emptyList()
                }
            } catch (e: Exception) {
                println("OfflineMapManager: Error fetching manifest: ${e.message}")
            }

            // Scan for downloaded extracts
            refreshDownloaded()
        }
    }

    fun refreshDownloaded() {
        _downloadedExtracts.value = findExtractPaths(extractBasePath)
    }

    /**
     * Get manifest features whose geometry contains the given location.
     */
    fun getExtractsContaining(location: LngLatAlt): List<Feature> {
        val fc = _manifest.value ?: return emptyList()
        return fc.features.filter { feature ->
            featureContainsLocation(feature, location)
        }
    }

    private fun featureContainsLocation(feature: Feature, location: LngLatAlt): Boolean {
        return when (val geom = feature.geometry) {
            is Polygon -> polygonContainsCoordinates(location, geom)
            is MultiPolygon -> geom.coordinates.any { polygonRings ->
                val poly = Polygon()
                poly.coordinates.addAll(polygonRings)
                polygonContainsCoordinates(location, poly)
            }
            else -> false
        }
    }

    /**
     * Check if an extract is already downloaded.
     */
    fun isDownloaded(filename: String): Boolean {
        val baseName = filename.substringAfterLast("/")
        return _downloadedExtracts.value.any { it.endsWith(baseName) }
    }

    /**
     * Start downloading an extract.
     */
    fun startDownload(filename: String, extractSize: Double? = null) {
        if (downloadJob?.isActive == true) return

        val url = "${extractBaseUrl.trimEnd('/')}/$filename"
        val baseName = filename.substringAfterLast("/")
        val outputPath = "${extractBasePath}/$baseName"

        downloadJob = scope.launch {
            val tempPath = "$outputPath.downloading"

            try {
                var retries = 10
                while (retries > 0) {
                    _downloadState.value = DownloadStateCommon.Caching

                    val result = fileDownloader.download(
                        url = url,
                        destPath = tempPath,
                        scope = this,
                        onProgress = { progress ->
                            _downloadState.value = DownloadStateCommon.Downloading(progress)
                        }
                    )

                    when (result) {
                        is DownloadResultCommon.Success -> {
                            // Rename temp file to final
                            val tempFile = tempPath.toPath()
                            val finalFile = outputPath.toPath()
                            try { FileSystem.SYSTEM.delete(finalFile) } catch (_: Exception) {}
                            FileSystem.SYSTEM.atomicMove(tempFile, finalFile)
                            _downloadState.value = DownloadStateCommon.Success
                            refreshDownloaded()
                            return@launch
                        }
                        is DownloadResultCommon.HttpError -> {
                            if (result.code == 503) {
                                // Server caching — back off and retry
                                var cachingDuration = 15
                                if (retries == 10 && extractSize != null) {
                                    cachingDuration = (extractSize / 10_000_000.0).toInt()
                                }
                                _downloadState.value = DownloadStateCommon.Caching
                                for (i in 0 until cachingDuration) {
                                    ensureActive()
                                    delay(1000)
                                }
                                retries--
                            } else {
                                throw Exception("HTTP ${result.code}: ${result.message}")
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                _downloadState.value = DownloadStateCommon.Canceled
                try { FileSystem.SYSTEM.delete(tempPath.toPath()) } catch (_: Exception) {}
            } catch (e: Exception) {
                _downloadState.value = DownloadStateCommon.Error(e.message ?: "Download failed")
                try { FileSystem.SYSTEM.delete(tempPath.toPath()) } catch (_: Exception) {}
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    /**
     * Delete a downloaded extract.
     */
    fun deleteExtract(path: String) {
        try {
            FileSystem.SYSTEM.delete(path.toPath())
            // Also delete the metadata file if it exists
            try { FileSystem.SYSTEM.delete("$path.geojson".toPath()) } catch (_: Exception) {}
            refreshDownloaded()
        } catch (e: Exception) {
            println("OfflineMapManager: Error deleting extract: ${e.message}")
        }
    }
}
