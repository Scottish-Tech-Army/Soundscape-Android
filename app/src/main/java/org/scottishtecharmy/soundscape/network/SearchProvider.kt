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
object PhotonSearchProvider {

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

    suspend fun getSearchResults(
        searchString: String,
        latitude: Double? = null,
        longitude: Double? = null,
        language: String? = null,
        limit: UInt = 5U,
        bias: Float = 0.2f,
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

    suspend fun reverseGeocodeLocation(
        latitude: Double? = null,
        longitude: Double? = null,
        language: String? = null,
    ): FeatureCollection? {
        val json = client.reverseGeocodeJson(
            latitude = latitude,
            longitude = longitude,
            language = language,
        ) ?: return null
        return adapter.fromJson(json)
    }
}
