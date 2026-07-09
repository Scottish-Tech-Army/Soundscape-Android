package org.scottishtecharmy.soundscape.navigation

data class GraphHopperRouteRequest(
    val start: NavigationPoint,
    val destination: NavigationPoint,
    val profile: String = "foot",
    val locale: String = "en"
)

object GraphHopperRouteRequestBuilder {
    fun buildRouteUrl(baseUrl: String, request: GraphHopperRouteRequest): String {
        return "${baseUrl.trimEnd('/')}/route" +
            "?profile=${request.profile}" +
            "&point=${request.start.latitude},${request.start.longitude}" +
            "&point=${request.destination.latitude},${request.destination.longitude}" +
            "&locale=${request.locale}" +
            "&instructions=true" +
            "&points_encoded=false" +
            "&ch.disable=true"
    }
}
