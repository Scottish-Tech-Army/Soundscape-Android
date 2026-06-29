package org.scottishtecharmy.soundscape.navigation

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GraphHopperRouteResponseFetcher {
    fun fetch(url: String): String
}

interface NavigationRouteProvider {
    fun route(request: GraphHopperRouteRequest): NavigationRoute
}

class GraphHopperRouteProvider(
    private val baseUrl: String,
    private val responseFetcher: GraphHopperRouteResponseFetcher = OkHttpGraphHopperRouteResponseFetcher()
) : NavigationRouteProvider {
    override fun route(request: GraphHopperRouteRequest): NavigationRoute {
        val url = GraphHopperRouteRequestBuilder.buildRouteUrl(baseUrl, request)
        return GraphHopperRouteParser.parse(responseFetcher.fetch(url))
    }
}

class OkHttpGraphHopperRouteResponseFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()
) : GraphHopperRouteResponseFetcher {
    override fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GraphHopper route request failed: HTTP ${response.code}")
            }
            return response.body.string()
        }
    }
}
