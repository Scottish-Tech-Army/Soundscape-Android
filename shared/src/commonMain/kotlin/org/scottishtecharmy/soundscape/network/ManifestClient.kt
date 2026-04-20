package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import okio.Buffer
import okio.GzipSource
import okio.buffer
import org.scottishtecharmy.soundscape.geoengine.MANIFEST_NAME

class ManifestClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getManifestJson(): String? {
        val url = "${baseUrl.trimEnd('/')}/$MANIFEST_NAME"
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) return null

        // The manifest is gzip-compressed (.gz) — decompress before returning as text.
        // On Android, an OkHttp interceptor handles this, but on iOS we do it here.
        return if (MANIFEST_NAME.endsWith(".gz")) {
            val bytes = response.readRawBytes()
            val source = Buffer().write(bytes)
            GzipSource(source).buffer().readUtf8()
        } else {
            response.bodyAsText()
        }
    }
}
