package org.scottishtecharmy.soundscape.network

import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection

interface PhotonSearch {
    suspend fun getSearchResults(
        searchString: String,
        latitude: Double? = null,
        longitude: Double? = null,
        language: String? = null,
        limit: UInt = 5U,
        bias: Float = 0.2f,
    ): FeatureCollection?

    suspend fun reverseGeocodeLocation(
        latitude: Double? = null,
        longitude: Double? = null,
        language: String? = null,
    ): FeatureCollection?
}
