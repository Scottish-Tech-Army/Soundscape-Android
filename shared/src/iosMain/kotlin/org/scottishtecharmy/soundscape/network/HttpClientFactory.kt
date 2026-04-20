package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

fun createIosVectorTileClient(baseUrl: String): VectorTileClient {
    val httpClient = HttpClient(Darwin) {
        expectSuccess = false
    }
    return VectorTileClient(httpClient, baseUrl)
}

fun createIosPhotonSearchClient(baseUrl: String): PhotonSearchClient {
    val httpClient = HttpClient(Darwin) {
        expectSuccess = false
    }
    return PhotonSearchClient(httpClient, baseUrl)
}
