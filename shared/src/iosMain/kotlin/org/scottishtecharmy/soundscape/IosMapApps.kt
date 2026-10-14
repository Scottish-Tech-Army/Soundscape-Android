package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.MapApp
import org.scottishtecharmy.soundscape.utils.formatCoordinate5
import org.scottishtecharmy.soundscape.utils.urlEncodeUtf8
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * The map apps Location Details can open a location in. iOS has no equivalent of Android's
 * geo: intent resolution, so each app is a known URL scheme probed with canOpenURL - every
 * [probeScheme] must also be listed under LSApplicationQueriesSchemes in project.yml, or
 * canOpenURL always says no.
 *
 * [MapApp.id] is [key], which is what gets remembered, so keys must never change.
 */
private enum class IosMapApp(
    val key: String,
    val displayName: String,
    /** Null for apps which are always available. */
    val probeScheme: String?,
    val url: (lat: String, lon: String, name: String) -> String,
) {
    APPLE_MAPS("apple", "Apple Maps", null, { lat, lon, name ->
        "https://maps.apple.com/?ll=$lat,$lon&q=${name.ifEmpty { "$lat,$lon" }}"
    }),
    GOOGLE_MAPS("google", "Google Maps", "comgooglemaps", { lat, lon, _ ->
        "comgooglemaps://?q=$lat,$lon&center=$lat,$lon&zoom=17"
    }),
    WAZE("waze", "Waze", "waze", { lat, lon, _ ->
        "waze://?ll=$lat,$lon&navigate=no"
    }),
    CITYMAPPER("citymapper", "Citymapper", "citymapper", { lat, lon, name ->
        "citymapper://directions?endcoord=$lat,$lon&endname=$name"
    }),
    MOOVIT("moovit", "Moovit", "moovit", { lat, lon, name ->
        "moovit://directions?dest_lat=$lat&dest_lon=$lon&dest_name=$name"
    }),
    ORGANIC_MAPS("organicmaps", "Organic Maps", "om", { lat, lon, name ->
        "om://map?v=1&ll=$lat,$lon&n=$name"
    }),
    OSMAND("osmand", "OsmAnd", "osmandmaps", { lat, lon, name ->
        "osmandmaps://?lat=$lat&lon=$lon&z=17&title=$name"
    }),
}

internal fun getIosMapApps(): List<MapApp> =
    IosMapApp.entries
        .filter { app ->
            val scheme = app.probeScheme ?: return@filter true
            val url = NSURL.URLWithString("$scheme://") ?: return@filter false
            UIApplication.sharedApplication.canOpenURL(url)
        }
        .map { MapApp(id = it.key, name = it.displayName) }

internal fun openInIosMapApp(app: MapApp, desc: LocationDescription) {
    val mapApp = IosMapApp.entries.firstOrNull { it.key == app.id } ?: IosMapApp.APPLE_MAPS
    val urlString = mapApp.url(
        formatCoordinate5(desc.location.latitude),
        formatCoordinate5(desc.location.longitude),
        urlEncodeUtf8(desc.name),
    )
    NSURL.URLWithString(urlString)?.let { openExternalUrl(it) }
}
