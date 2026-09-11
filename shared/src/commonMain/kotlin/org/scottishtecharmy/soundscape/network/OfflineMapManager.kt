package org.scottishtecharmy.soundscape.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import org.scottishtecharmy.soundscape.geoengine.utils.polygonContainsCoordinates
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.platform.systemFileSystem
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.NearbyExtractsState
import org.scottishtecharmy.soundscape.utils.findExtractPaths
import org.scottishtecharmy.soundscape.utils.formatBytes
import org.scottishtecharmy.soundscape.utils.isPmtilesUsable
import org.scottishtecharmy.soundscape.utils.logicalExtractName

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

    // Manifest-fetch state (Loading until refresh() completes; then Loaded(fc) or Error)
    private val _nearbyExtractsState =
        MutableStateFlow<NearbyExtractsState>(NearbyExtractsState.Loading)
    val nearbyExtractsState: StateFlow<NearbyExtractsState> = _nearbyExtractsState.asStateFlow()

    // Downloaded extracts
    private val _downloadedExtracts = MutableStateFlow<List<String>>(emptyList())
    val downloadedExtracts: StateFlow<List<String>> = _downloadedExtracts.asStateFlow()

    /**
     * Downloaded extracts joined with their metadata (so the UI can show the localised name,
     * cities, size string, etc). For each downloaded `.pmtiles` path the metadata comes from,
     * in order:
     *
     *  1. the `.geojson` sidecar written next to the extract when it was downloaded - this is
     *     the only source that works with no network and no manifest, and is what Android has
     *     always used (see AndroidOfflineMapsManager/findExtracts),
     *  2. the matching manifest entry, for extracts downloaded before the sidecar existed,
     *  3. a stub carrying the filename, if all else fails.
     *
     * Manifest entries are matched on [logicalExtractName], not on the raw filename: manifest
     * filenames carry a build prefix ("20260820-1354-glasgow-gb.pmtiles") that changes every time
     * the extracts are regenerated, so matching the whole name meant a downloaded extract lost its
     * metadata - and showed as "20260820-1354-glasgow-gb" instead of "Glasgow" - as soon as the
     * server published a new build.
     */
    val downloadedExtractsFc: StateFlow<FeatureCollection> =
        combine(_manifest, _downloadedExtracts) { manifest, paths ->
            val fc = FeatureCollection()
            for (path in paths) {
                val baseName = path.substringAfterLast("/")
                val logicalName = logicalExtractName(baseName)
                val metadata = readExtractMetadata(path)
                    ?: manifest.entryForLogicalName(logicalName)
                // Validate the extract so the UI can flag a damaged download. A damaged extract
                // (its metadata won't decompress) would crash MapLibre if used, so it is excluded
                // from the live map (see resolveTileSourceUrl); here we surface it via the
                // "usable" property instead. This does file I/O, but the combine runs on the
                // manager's background scope (Dispatchers.Default), not the main thread.
                val usable = isPmtilesUsable(path)
                // Always a copy, never the manifest's own Feature: the size string is localised
                // and "usable" is local state, and writing either back into the manifest entry
                // would leak downloaded-extract state into the nearby-extracts list.
                val feature = metadata?.copyOf() ?: Feature().apply {
                    properties = hashMapOf(
                        "name" to baseName.removeSuffix(".pmtiles"),
                        "filename" to baseName,
                        "extract-size-string" to "",
                    )
                }
                feature.withSizeString()
                val props = HashMap<String, Any?>(feature.properties ?: emptyMap())
                props["usable"] = usable
                // The row is only rendered when it has a size string, so a feature with no
                // "extract-size" to format still needs an (empty) one, or a downloaded extract
                // would silently vanish from the list.
                props.getOrPut("extract-size-string") { "" }
                feature.properties = props
                fc.addFeature(feature)
            }
            fc
        }.stateIn(scope, SharingStarted.Eagerly, FeatureCollection())

    // Download state
    private val _downloadState = MutableStateFlow<DownloadStateCommon>(DownloadStateCommon.Idle)
    val downloadState: StateFlow<DownloadStateCommon> = _downloadState.asStateFlow()

    private var downloadJob: Job? = null

    /**
     * Fetch the manifest and list of downloaded extracts.
     *
     * Resets [nearbyExtractsState] to [NearbyExtractsState.Loading] at the start so the
     * UI shows the spinner during a re-fetch. On success publishes
     * [NearbyExtractsState.Loaded] wrapping the manifest as a FeatureCollection;
     * any failure publishes [NearbyExtractsState.Error].
     */
    fun refresh() {
        _nearbyExtractsState.value = NearbyExtractsState.Loading
        scope.launch {
            // Fetch manifest
            try {
                val json = manifestClient.getManifestJson()
                val fc = json?.let { GeoJsonParser.parseFeatureCollection(it) }
                if (fc != null) {
                    _manifest.value = fc
                    _nearbyExtractsState.value = NearbyExtractsState.Loaded(fc)
                } else {
                    _nearbyExtractsState.value = NearbyExtractsState.Error
                }
            } catch (e: Exception) {
                println("OfflineMapManager: Error fetching manifest: ${e.message}")
                _nearbyExtractsState.value = NearbyExtractsState.Error
            }

            // Scan for downloaded extracts
            refreshDownloaded()
        }
    }

    fun refreshDownloaded() {
        _downloadedExtracts.value = findExtractPaths(extractBasePath)
    }

    /**
     * Get manifest features whose geometry contains the given location, decorated
     * with a human-readable size string for the UI.
     */
    fun getExtractsContaining(location: LngLatAlt): List<Feature> {
        val fc = _manifest.value ?: return emptyList()
        return fc.features
            .filter { feature -> featureContainsLocation(feature, location) }
            .map { it.withSizeString() }
    }

    /**
     * Returns a copy of the feature with `extract-size-string` populated from
     * `extract-size`, mutating only the properties map.
     */
    private fun Feature.withSizeString(): Feature {
        val sizeProp = properties?.get("extract-size")
        val sizeBytes = (sizeProp as? Number)?.toDouble()
            ?: (sizeProp as? String)?.toDoubleOrNull()
            ?: return this
        val props = HashMap<String, Any?>(properties ?: emptyMap())
        val sizeLong = sizeBytes.toLong()
        val localized = ComposeLocalizedStrings()
        props["extract-size-string"] = formatBytes(sizeLong, localized)
        props["extract-size-a11y-string"] =
            formatBytes(sizeLong, localized, forAccessibility = true)
        properties = props
        return this
    }

    /**
     * The manifest entry for an extract, found by [logicalExtractName] rather than by filename so
     * that a build prefix from an older manifest still resolves to the current entry.
     */
    private fun FeatureCollection?.entryForLogicalName(logicalName: String): Feature? =
        this?.features?.firstOrNull { feature ->
            val filename = feature.properties?.get("filename") as? String
                ?: return@firstOrNull false
            logicalExtractName(filename) == logicalName
        }

    private fun Feature.extractSize(): Double? {
        val sizeProp = properties?.get("extract-size")
        return (sizeProp as? Number)?.toDouble() ?: (sizeProp as? String)?.toDoubleOrNull()
    }

    /**
     * Shallow copy, so the caller can annotate the properties of a manifest or sidecar feature
     * without mutating the original.
     */
    private fun Feature.copyOf(): Feature {
        val original = this
        return Feature().apply {
            geometry = original.geometry
            id = original.id
            bbox = original.bbox
            properties = HashMap(original.properties ?: emptyMap())
        }
    }

    /**
     * Read the metadata sidecar written alongside a downloaded extract by [writeExtractMetadata],
     * or null if there isn't one (extracts downloaded before sidecars existed) or it is unreadable.
     */
    private fun readExtractMetadata(pmtilesPath: String): Feature? {
        return try {
            val json = systemFileSystem.read("$pmtilesPath.geojson".toPath()) { readUtf8() }
            GeoJsonParser.parseFeature(json)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Store the manifest feature for a download next to the extract itself, as Android does. It is
     * what names the extract in the downloaded list from then on, so it has to survive the extract
     * being dropped from a later manifest, the manifest fetch failing, and the app running with no
     * network at all.
     *
     * The properties that are computed locally rather than served - the localised size strings and
     * the "usable" flag - are dropped, so that a stale localisation can't outlive a language change.
     */
    private fun writeExtractMetadata(pmtilesPath: String, feature: Feature) {
        try {
            val stored = feature.copyOf()
            stored.properties?.keys?.removeAll(
                setOf("extract-size-string", "extract-size-a11y-string", "usable")
            )
            systemFileSystem.write("$pmtilesPath.geojson".toPath()) {
                writeUtf8(GeoJsonParser.toJson(stored))
            }
        } catch (e: Exception) {
            println("OfflineMapManager: Error writing extract metadata: ${e.message}")
        }
    }

    /**
     * Remove any other copy of the same extract - an earlier build of it, downloaded under the
     * build-prefixed filename of the manifest it came from - and its sidecar. Without this, an
     * extract re-downloaded after the server regenerated it would leave the old copy on disk,
     * where it would take up space, be listed a second time under the same name, and still be fed
     * to the geo engine.
     */
    private fun deleteOtherVersionsOf(pmtilesPath: String) {
        val logicalName = logicalExtractName(pmtilesPath)
        // Compared by filename, not by whole path: the path being kept was assembled by string
        // concatenation here while the others come from the filesystem, and deleting the file we
        // just downloaded over a difference in how the directory was spelled would be fatal.
        val keep = pmtilesPath.substringAfterLast('/')
        for (path in findExtractPaths(extractBasePath)) {
            if (path.substringAfterLast('/') == keep) continue
            if (logicalExtractName(path) != logicalName) continue
            try {
                systemFileSystem.delete(path.toPath())
                try {
                    systemFileSystem.delete("$path.geojson".toPath())
                } catch (_: Exception) {
                }
            } catch (e: Exception) {
                println("OfflineMapManager: Error deleting old extract: ${e.message}")
            }
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
        val logicalName = logicalExtractName(filename)
        return _downloadedExtracts.value.any { logicalExtractName(it) == logicalName }
    }

    /**
     * Start downloading an extract.
     *
     * [feature] is the manifest entry being downloaded. It is stored as a sidecar next to the
     * downloaded file so the extract can still be named in the UI once the manifest has moved on
     * or is unavailable; a download started without one still works, it just has no metadata to
     * fall back on.
     *
     * The download always goes to whatever the *current* manifest calls this extract, not to
     * [filename] as given. The "Update" button on a downloaded extract hands us the entry the
     * extract was downloaded with, and its filename carries the build prefix of the manifest it
     * came from - a build the server has usually replaced by the time anyone presses Update, so
     * downloading it verbatim fetches a URL that is no longer there. [filename] is only used as-is
     * when the extract isn't in the manifest at all (not fetched yet, or withdrawn).
     */
    fun startDownload(filename: String, extractSize: Double? = null, feature: Feature? = null) {
        if (downloadJob?.isActive == true) return

        val current = _manifest.value.entryForLogicalName(logicalExtractName(filename))
        val currentFilename = (current?.properties?.get("filename") as? String) ?: filename
        val size = current?.extractSize() ?: extractSize
        val metadata = current ?: feature

        val url = "${extractBaseUrl.trimEnd('/')}/$currentFilename"
        val baseName = currentFilename.substringAfterLast("/")
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
                            try {
                                systemFileSystem.delete(finalFile)
                            } catch (_: Exception) {
                            }
                            systemFileSystem.atomicMove(tempFile, finalFile)
                            metadata?.let { writeExtractMetadata(outputPath, it) }
                            deleteOtherVersionsOf(outputPath)
                            // Refresh before publishing Success, not after. Success is what
                            // releases anything waiting on downloadState, and the natural next
                            // thing for a waiter to do is read downloadedExtracts - which, the
                            // other way round, still held the list from before this download for
                            // the width of one statement. Rare, but it is exactly what
                            // OfflineMapManagerTest.startDownload_success_movesTempFileToFinal-
                            // PathAndUpdatesDownloadedExtracts intermittently caught on CI.
                            refreshDownloaded()
                            _downloadState.value = DownloadStateCommon.Success
                            return@launch
                        }

                        is DownloadResultCommon.HttpError -> {
                            if (result.code == 503) {
                                // Server caching — back off and retry
                                var cachingDuration = 15
                                if (retries == 10 && size != null) {
                                    cachingDuration = (size / 10_000_000.0).toInt()
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
                // Retries exhausted (server kept returning 503) without ever succeeding or
                // hitting a non-retryable error - must not fall through silently, or
                // downloadState is left stuck on Caching forever with no indication anything
                // failed.
                _downloadState.value = DownloadStateCommon.Error("Download timed out after retries")
                try {
                    systemFileSystem.delete(tempPath.toPath())
                } catch (_: Exception) {
                }
            } catch (e: CancellationException) {
                _downloadState.value = DownloadStateCommon.Canceled
                try {
                    systemFileSystem.delete(tempPath.toPath())
                } catch (_: Exception) {
                }
            } catch (e: Exception) {
                _downloadState.value = DownloadStateCommon.Error(e.message ?: "Download failed")
                try {
                    systemFileSystem.delete(tempPath.toPath())
                } catch (_: Exception) {
                }
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
            systemFileSystem.delete(path.toPath())
            // Also delete the metadata file if it exists
            try {
                systemFileSystem.delete("$path.geojson".toPath())
            } catch (_: Exception) {
            }
            refreshDownloaded()
        } catch (e: Exception) {
            println("OfflineMapManager: Error deleting extract: ${e.message}")
        }
    }

    /**
     * Delete the downloaded extract corresponding to the given manifest feature, by
     * matching its filename against the known downloaded paths.
     */
    fun deleteExtractByFeature(feature: Feature) {
        val filename = feature.properties?.get("filename") as? String ?: return
        val logicalName = logicalExtractName(filename)
        val match = _downloadedExtracts.value.firstOrNull {
            logicalExtractName(it) == logicalName
        }
        if (match != null) deleteExtract(match)
    }
}
