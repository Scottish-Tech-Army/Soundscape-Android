package org.scottishtecharmy.soundscape.network

import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection

/**
 * KMP-compatible PhotonSearch implementation using PhotonSearchClient (Ktor)
 * and GeoJsonParser (kotlinx.serialization) for JSON → FeatureCollection.
 *
 * This replaces the Android-only PhotonSearchProvider that uses Moshi.
 */
class KmpPhotonSearch(
    private val client: PhotonSearchClient,
) : PhotonSearch {

    override suspend fun getSearchResults(
        searchString: String,
        latitude: Double?,
        longitude: Double?,
        language: String?,
        limit: UInt,
        bias: Float,
    ): FeatureCollection? {
        val json = client.searchJson(
            query = searchString,
            latitude = latitude,
            longitude = longitude,
            language = language,
            limit = limit,
            locationBiasScale = bias,
        ) ?: return null
        return GeoJsonParser.parseFeatureCollection(json)
    }

    override suspend fun reverseGeocodeLocation(
        latitude: Double?,
        longitude: Double?,
        language: String?,
    ): FeatureCollection? {
        val json = client.reverseGeocodeJson(
            latitude = latitude,
            longitude = longitude,
            language = language,
        ) ?: return null
        return GeoJsonParser.parseFeatureCollection(json)
    }
}
