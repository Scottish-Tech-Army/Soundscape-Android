package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import org.scottishtecharmy.soundscape.geoengine.MANIFEST_NAME

class ManifestClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getManifestJson(): String? {
        val url = "${baseUrl.trimEnd('/')}/$MANIFEST_NAME"
        val response = httpClient.get(url)
        return if (response.status.isSuccess()) response.bodyAsText() else null
    }
}
