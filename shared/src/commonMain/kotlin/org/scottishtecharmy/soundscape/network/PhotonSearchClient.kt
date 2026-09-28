package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class PhotonSearchClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    suspend fun searchJson(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null,
        language: String? = null,
        limit: UInt = 5U,
        locationBiasScale: Float = 0.2f,
    ): String? = doGet("api/") {
        parameter("q", query)
        parameter("lat", latitude)
        parameter("lon", longitude)
        parameter("lang", language)
        parameter("limit", limit.toInt())
        parameter("location_bias_scale", locationBiasScale)
    }

    suspend fun reverseGeocodeJson(
        latitude: Double? = null,
        longitude: Double? = null,
        language: String? = null,
    ): String? = doGet("reverse/") {
        parameter("lat", latitude)
        parameter("lon", longitude)
        parameter("lang", language)
    }

    /**
     * The places nearest to [latitude], [longitude] which have any of [osmTags], nearest first.
     * This is a reverse geocode filtered by tag rather than a search: /api/ matches its query
     * against names even when it's filtered by tag, so it can't find a pharmacy called "Boots".
     *
     * Each of [osmTags] is in Photon's osm_tag form - "amenity:pharmacy", ":pharmacy" for the
     * value under any key, "!building" to exclude a key - and several are OR'd together, except
     * the exclusions. [radius] is in km; Photon's default is only 1km. [nameFilter] restricts
     * the results to those whose name matches it.
     */
    suspend fun nearbyByTagJson(
        latitude: Double,
        longitude: Double,
        osmTags: List<String>,
        radius: Double? = null,
        limit: UInt? = null,
        nameFilter: String? = null,
        language: String? = null,
    ): String? = doGet("reverse/") {
        parameter("lat", latitude)
        parameter("lon", longitude)
        for (tag in osmTags) parameter("osm_tag", tag)
        parameter("radius", radius)
        parameter("limit", limit?.toInt())
        parameter("query_string_filter", nameFilter)
        parameter("lang", language)
    }

    private suspend fun doGet(
        path: String,
        block: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): String? {
        val url = "${baseUrl.trimEnd('/')}/$path"
        val response = httpClient.get(url) {
            header("Cache-Control", "max-age=0")
            header("Connection", "keep-alive")
            block()
        }
        return if (response.status.isSuccess()) response.bodyAsText() else null
    }
}
