package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SUFFIX

class VectorTileClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getTile(x: Int, y: Int, z: Int): ByteArray? {
        val url = "${baseUrl.trimEnd('/')}/$PROTOMAPS_SERVER_PATH/$z/$x/$y.$PROTOMAPS_SUFFIX"
        return try {
            val response = httpClient.get(url)
            if (response.status.isSuccess()) response.readRawBytes() else null
        } catch (e: Exception) {
            null
        }
    }
}
