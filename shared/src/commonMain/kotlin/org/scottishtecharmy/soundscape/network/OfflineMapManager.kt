package org.scottishtecharmy.soundscape.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import org.scottishtecharmy.soundscape.logging.AppLog
import org.scottishtecharmy.soundscape.platform.currentTimeMillis
import org.scottishtecharmy.soundscape.platform.excludeFromCloudBackup
import org.scottishtecharmy.soundscape.platform.systemFileSystem
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.NearbyExtractsState
import org.scottishtecharmy.soundscape.utils.findExtractPaths
import org.scottishtecharmy.soundscape.utils.formatBytes
import org.scottishtecharmy.soundscape.utils.isExtractOutdated
import org.scottishtecharmy.soundscape.utils.isPmtilesUsable
import org.scottishtecharmy.soundscape.utils.logicalExtractName
import org.scottishtecharmy.soundscape.utils.readExtractSchema

private val logger = AppLog.tag("OfflineMapManager")

/**
 * Offline map manager for both platforms. Handles:
 * - Fetching and parsing the extract manifest
 * - Downloading PMTiles extracts
 * - Discovering downloaded extracts on disk
 * - Managing download state for the UI
 *
 * Only the HTTP download ([fileDownloader]) and where the extracts live
 * ([extractBasePathProvider]) differ between platforms.
 *
 * @param extractBasePathProvider the directory holding the extracts. A provider rather than a
 * path because Android lets the user move them to other storage while the app is running.
 * @param onExtractsChanged called once an extract has been published or deleted, so the geo
 * engine can pick up the change straight away instead of the next time the user walks out of the
 * current tile grid.
 */
class OfflineMapManager(
    private val manifestClient: ManifestClient,
    private val fileDownloader: FileDownloaderInterface,
    private val extractBasePathProvider: () -> String,
    private val extractBaseUrl: String,
    private val onExtractsChanged: () -> Unit = {},
) {
    constructor(
        manifestClient: ManifestClient,
        fileDownloader: FileDownloaderInterface,
        extractBasePath: String,
        extractBaseUrl: String,
        onExtractsChanged: () -> Unit = {},
    ) : this(manifestClient, fileDownloader, { extractBasePath }, extractBaseUrl, onExtractsChanged)

    private val extractBasePath: String get() = extractBasePathProvider()

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
     *     the only source that works with no network and no manifest,
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
        combine(_manifest, _downloadedExtracts) { _, _ ->
            // Both are read afresh rather than taken from combine, whose collectors run
            // independently: it could hand over the newly found paths with the manifest from
            // before, so an extract briefly showed with no manifest entry - not outdated, say.
            // refresh() sets the manifest before the paths, so whoever sees one sees both.
            val manifest = _manifest.value
            val paths = _downloadedExtracts.value
            val fc = FeatureCollection()
            for (path in paths) {
                val baseName = path.substringAfterLast("/")
                val logicalName = logicalExtractName(baseName)
                val manifestEntry = manifest.entryForLogicalName(logicalName)
                val metadata = readExtractMetadata(path) ?: manifestEntry
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
                // The schema the extract was built with is read from the file itself, not from
                // its sidecar or manifest entry, which describe whatever the server had at the
                // time.
                props["outdated"] = isExtractOutdated(readExtractSchema(path), manifestEntry)
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
            // Fetch manifest, retrying a few times as a flaky connection often recovers within
            // a second or two.
            var fc: FeatureCollection? = null
            for (attempt in 1..MANIFEST_ATTEMPTS) {
                try {
                    fc = manifestClient.getManifestJson()?.let { GeoJsonParser.parseFeatureCollection(it) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.e(e) { "Error fetching manifest, attempt $attempt" }
                }
                if (fc != null) break
                if (attempt < MANIFEST_ATTEMPTS) delay(MANIFEST_RETRY_DELAY_MS)
            }
            if (fc != null) {
                _manifest.value = fc
                _nearbyExtractsState.value = NearbyExtractsState.Loaded(fc)
            } else {
                logger.w { "Manifest fetch failed" }
                _nearbyExtractsState.value = NearbyExtractsState.Error
            }

            // Scan for downloaded extracts
            refreshDownloaded()
        }
    }

    fun refreshDownloaded() {
        val basePath = extractBasePath
        deleteOrphanedSidecars(basePath)
        val paths = findExtractPaths(basePath)
        // Keep the extracts out of the device's cloud backup. Doing it on every scan rather
        // than only after a download means extracts left by an earlier version of the app are
        // covered too, and the flag is idempotent so re-marking a file we marked last time
        // costs nothing.
        for (path in paths) {
            excludeFromCloudBackup(path)
            excludeFromCloudBackup("$path.geojson")
        }
        _downloadedExtracts.value = paths
    }

    /**
     * Delete the .geojson metadata sidecars which have no extract next to them. Android once
     * wrote the sidecar before a download started and left it behind when the download failed or
     * was cancelled; those orphans are never listed, but would stay on the phone forever.
     */
    private fun deleteOrphanedSidecars(basePath: String) {
        try {
            for (sidecar in systemFileSystem.list(basePath.toPath())) {
                if (!sidecar.name.endsWith(".pmtiles.geojson")) continue
                val extract = basePath.toPath() / sidecar.name.removeSuffix(".geojson")
                if (!systemFileSystem.exists(extract)) systemFileSystem.delete(sidecar)
            }
        } catch (_: Exception) {
        }
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
     * the "usable" and "outdated" flags - are dropped, so that a stale localisation can't outlive a
     * language change.
     */
    private fun writeExtractMetadata(pmtilesPath: String, feature: Feature) {
        try {
            val stored = feature.copyOf()
            stored.properties?.keys?.removeAll(
                setOf("extract-size-string", "extract-size-a11y-string", "usable", "outdated")
            )
            systemFileSystem.write("$pmtilesPath.geojson".toPath()) {
                writeUtf8(GeoJsonParser.toJson(stored))
            }
        } catch (e: Exception) {
            logger.e(e) { "Error writing extract metadata" }
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
                logger.e(e) { "Error deleting old extract" }
            }
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
     * Start downloading the extract described by [feature], a manifest entry or the sidecar of a
     * downloaded extract (for "Update").
     */
    fun startDownload(feature: Feature) {
        val props = feature.properties ?: return
        val filename = props["filename"] as? String ?: return
        startDownload(filename, feature.extractSize(), feature)
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
     *
     * The extract is saved under a filename no previous download has used,
     * "<logical name>.v<timestamp>.pmtiles", and any other version of it is deleted once it is
     * published. Offline map tiles go into MapLibre's ambient cache under URLs that name the
     * extract's path (see OfflineExtractTiles), which we can't invalidate, so new bytes at an old
     * path would be drawn mixed with stale cached tiles.
     */
    fun startDownload(filename: String, extractSize: Double? = null, feature: Feature? = null) {
        val previousJob = downloadJob
        if (previousJob?.isActive == true) return

        val current = _manifest.value.entryForLogicalName(logicalExtractName(filename))
        val currentFilename = (current?.properties?.get("filename") as? String) ?: filename
        val size = current?.extractSize() ?: extractSize
        val metadata = current ?: feature

        val url = "${extractBaseUrl.trimEnd('/')}/$currentFilename"
        val basePath = extractBasePath
        val outputPath =
            "$basePath/${logicalExtractName(currentFilename)}.v${currentTimeMillis()}.pmtiles"

        downloadJob = scope.launch {
            // A cancelled job stops being "active" the instant cancelDownload() is called, well
            // before its coroutine has unwound and run its cleanup (deleting the temp file,
            // writing Canceled). Join it first, or that belated cleanup can delete this
            // download's temp file and overwrite its state (#1288).
            previousJob?.join()
            val tempPath = "$outputPath.downloading"

            try {
                systemFileSystem.createDirectories(basePath.toPath())
                var retries = MAX_DOWNLOAD_ATTEMPTS
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
                            // The server caches extracts lazily and can answer with only the
                            // part of the file it has copied so far. If we received less than
                            // the manifest says the extract is, it isn't ready yet: back off and
                            // retry as for a 503, rather than publishing a truncated extract.
                            val received = systemFileSystem.metadata(tempPath.toPath()).size ?: 0L
                            if (size != null && received < size.toLong()) {
                                logger.d { "Extract not fully cached yet ($received of ${size.toLong()} bytes), retrying" }
                                waitBeforeRetry(firstAttempt = retries == MAX_DOWNLOAD_ATTEMPTS, size = size)
                                retries--
                                continue
                            }
                            // A truncated or corrupt extract crashes MapLibre natively when it
                            // is drawn, so check it before publishing it rather than after.
                            if (!isPmtilesUsable(tempPath)) {
                                throw Exception("Downloaded extract failed validation (corrupt or truncated)")
                            }
                            systemFileSystem.atomicMove(tempPath.toPath(), outputPath.toPath())
                            metadata?.let { writeExtractMetadata(outputPath, it) }
                            // Before publishing Success, so nothing reacting to it can see a
                            // superseded version still on disk.
                            deleteOtherVersionsOf(outputPath)
                            // Refresh before publishing Success, not after. Success is what
                            // releases anything waiting on downloadState, and the natural next
                            // thing for a waiter to do is read downloadedExtracts - which, the
                            // other way round, still held the list from before this download for
                            // the width of one statement. Rare, but it is exactly what
                            // OfflineMapManagerTest.startDownload_success_movesTempFileToFinal-
                            // PathAndUpdatesDownloadedExtracts intermittently caught on CI.
                            refreshDownloaded()
                            notifyExtractsChanged()
                            _downloadState.value = DownloadStateCommon.Success
                            return@launch
                        }

                        is DownloadResultCommon.HttpError -> {
                            if (result.code == 503) {
                                // The server is copying the extract into its cache - back off
                                // and retry.
                                waitBeforeRetry(firstAttempt = retries == MAX_DOWNLOAD_ATTEMPTS, size = size)
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
                // In each terminal branch, delete the temp file before publishing the state:
                // observers treat a terminal state as "clean-up done".
                try {
                    systemFileSystem.delete(tempPath.toPath())
                } catch (_: Exception) {
                }
                _downloadState.value = DownloadStateCommon.Error("Download timed out after retries")
            } catch (e: CancellationException) {
                try {
                    systemFileSystem.delete(tempPath.toPath())
                } catch (_: Exception) {
                }
                _downloadState.value = DownloadStateCommon.Canceled
            } catch (e: Exception) {
                try {
                    systemFileSystem.delete(tempPath.toPath())
                } catch (_: Exception) {
                }
                _downloadState.value = DownloadStateCommon.Error(e.message ?: "Download failed")
            }
        }
    }

    /**
     * Back off before retrying a download the server isn't ready to give us yet. It caches at
     * around 10MB/sec, so the first wait is estimated from the extract's size; after that we
     * poll every 15 seconds.
     */
    private suspend fun waitBeforeRetry(firstAttempt: Boolean, size: Double?) {
        var seconds = 15
        if (firstAttempt && size != null) seconds = (size / 10_000_000.0).toInt()
        _downloadState.value = DownloadStateCommon.Caching
        delay(seconds * 1000L)
    }

    private fun notifyExtractsChanged() {
        try {
            onExtractsChanged()
        } catch (e: Exception) {
            logger.e(e) { "onExtractsChanged failed" }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    /**
     * Delete a downloaded extract.
     */
    fun deleteExtract(path: String) {
        deleteExtractFiles(path)
        refreshDownloaded()
        notifyExtractsChanged()
    }

    private fun deleteExtractFiles(path: String) {
        try {
            systemFileSystem.delete(path.toPath())
            // Also delete the metadata file if it exists
            try {
                systemFileSystem.delete("$path.geojson".toPath())
            } catch (_: Exception) {
            }
        } catch (e: Exception) {
            logger.e(e) { "Error deleting extract" }
        }
    }

    /**
     * Delete every downloaded version of the extract described by the given manifest feature,
     * matched on [logicalExtractName]. The directory is scanned afresh rather than trusting
     * [downloadedExtracts], which may not have been refreshed since the last download.
     */
    fun deleteExtractByFeature(feature: Feature) {
        val filename = feature.properties?.get("filename") as? String ?: return
        val logicalName = logicalExtractName(filename)
        val matches = findExtractPaths(extractBasePath).filter { logicalExtractName(it) == logicalName }
        if (matches.isEmpty()) return
        matches.forEach { deleteExtractFiles(it) }
        refreshDownloaded()
        notifyExtractsChanged()
    }

    private companion object {
        const val MAX_DOWNLOAD_ATTEMPTS = 10
        const val MANIFEST_ATTEMPTS = 4
        const val MANIFEST_RETRY_DELAY_MS = 500L
    }
}

/** Whether the area of an extract - its manifest entry, or its sidecar - contains [location] */
internal fun featureContainsLocation(feature: Feature, location: LngLatAlt): Boolean {
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
