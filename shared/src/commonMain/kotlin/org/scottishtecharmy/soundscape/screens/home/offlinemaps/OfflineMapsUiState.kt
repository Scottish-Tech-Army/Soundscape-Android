package org.scottishtecharmy.soundscape.screens.home.offlinemaps

import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection

/**
 * Shared UI state for the offline maps screen, used by both Android and iOS so they
 * render identical UI.
 */
data class OfflineMapsUiState(
    val downloadingExtractName: String = "",

    val manifestError: Boolean = false,

    /** Extracts in the manifest near the user's location. */
    val nearbyExtracts: FeatureCollection? = null,

    /** Extracts already downloaded to disk. */
    val downloadedExtracts: FeatureCollection? = null,

    /** Path of the storage volume currently used for downloads. */
    val currentPath: String = "",

    /** All available storage volumes that the user could pick between. */
    val storages: List<StorageInfo> = emptyList(),
)

/**
 * A piece of storage that maps can be downloaded into. Mirrors the bits of
 * `StorageUtils.StorageSpace` that the UI needs, in a platform-agnostic shape.
 */
data class StorageInfo(
    val path: String,
    val description: String,
    val availableString: String,
)
