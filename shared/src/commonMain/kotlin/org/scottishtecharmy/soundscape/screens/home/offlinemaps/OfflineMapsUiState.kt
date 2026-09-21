package org.scottishtecharmy.soundscape.screens.home.offlinemaps

import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * Tri-state for the nearby-extracts manifest. Drives whether the offline maps
 * screen shows a loading spinner, the list of extracts, or an error message.
 */
sealed class NearbyExtractsState {
    object Loading : NearbyExtractsState()
    data class Loaded(val nearbyExtracts: FeatureCollection) : NearbyExtractsState()
    object Error : NearbyExtractsState()
}

/**
 * Shared UI state for the offline maps screen, used by both Android and iOS so they
 * render identical UI.
 */
data class OfflineMapsUiState(
    val downloadingExtractName: String = "",

    /**
     * Manifest fetch / nearby-extracts state. Defaults to [NearbyExtractsState.Loading]
     * so the spinner is shown until the platform-specific manager publishes a
     * [NearbyExtractsState.Loaded] (success) or [NearbyExtractsState.Error] (failure).
     */
    val nearbyExtractsState: NearbyExtractsState = NearbyExtractsState.Loading,

    /** Extracts already downloaded to disk. */
    val downloadedExtracts: FeatureCollection? = null,

    /** Path of the storage volume currently used for downloads. */
    val currentPath: String = "",

    /** All available storage volumes that the user could pick between. */
    val storages: List<StorageInfo> = emptyList(),

    /** Live user GPS location. */
    val userLocation: LngLatAlt? = null,

    /** Live user heading (degrees) for the map symbol rotation. */
    val userHeading: Float = 0.0f,

    /** Search/marker location used to find nearby extracts. */
    val markerLocation: LngLatAlt? = null,
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
