package org.scottishtecharmy.soundscape.network

import com.squareup.moshi.Moshi
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi

/**
 * Uses the photon search engine https://github.com/komoot/photon for geocoding and reverse
 * geocoding. Thin Android-side wrapper around the shared [PhotonSearchClient] that parses the
 * JSON response into a [FeatureCollection] via GeoMoshi.
 */
object PhotonSearchProvider : PhotonSearch {

    private val adapter by lazy {
        GeoMoshi.registerAdapters(Moshi.Builder()).build()
            .adapter(FeatureCollection::class.java)
    }

    private val client: PhotonSearchClient by lazy {
        createAndroidPhotonSearchClient(
            baseUrl = BuildConfig.SEARCH_PROVIDER_URL,
            userAgent = UserAgentInterceptor.USER_AGENT,
        )
    }

    fun getInstance(): PhotonSearchProvider = this

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
        return adapter.fromJson(json)
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
        return adapter.fromJson(json)
    }
}
