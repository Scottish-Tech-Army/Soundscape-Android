package org.scottishtecharmy.soundscape.intents

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.parseGpxFile

/**
 * Pure-Kotlin parser for inbound URLs and route files. Used by both the Android
 * intent path and the iOS onOpenURL path. Platform-specific behaviour (Geocoder
 * upgrades, network redirects, content-resolver streams) lives in the call sites.
 */
object IntentParser {

    private const val SHARE_MARKER_HOST = "links.soundscape.scottishtecharmy.org"
    private const val SHARE_MARKER_PATH = "/v1/sharemarker"
    private const val MAX_ROUTE_FILE_BYTES = 1_000_000

    private val coordinateRegex = Regex("(-?[0-9]+\\.?[0-9]*),(-?[0-9]+\\.?[0-9]*)")

    fun parseUrl(url: String): IncomingIntent? {
        val parsed = parseUri(url) ?: return null

        return when (parsed.scheme) {
            "soundscape" -> parseSoundscape(parsed)
            "geo" -> parseGeo(parsed)
            "https", "http" -> parseShareMarker(parsed)
            else -> null
        }
    }

    fun parseRouteJson(text: String): IncomingIntent.ImportRoute? {
        if (text.length > MAX_ROUTE_FILE_BYTES) return null
        return try {
            val root = Json.parseToJsonElement(text).jsonObject
            val name = root["name"]?.jsonPrimitive?.contentOrNull ?: return null
            val waypoints = root["waypoints"]?.jsonArray ?: return null
            val markers = mutableListOf<MarkerEntity>()
            for (waypoint in waypoints) {
                val marker = waypoint.jsonObject["marker"]?.jsonObject ?: continue
                val location = marker["location"]?.jsonObject ?: continue
                val coordinate = location["coordinate"]?.jsonObject ?: continue
                val latitude = coordinate["latitude"]?.jsonPrimitive?.doubleOrNull ?: continue
                val longitude = coordinate["longitude"]?.jsonPrimitive?.doubleOrNull ?: continue
                val markerName = location["name"]?.jsonPrimitive?.contentOrNull ?: ""
                val estimatedAddress = marker["estimatedAddress"]?.jsonPrimitive?.contentOrNull ?: ""
                markers.add(
                    MarkerEntity(
                        name = markerName,
                        longitude = longitude,
                        latitude = latitude,
                        fullAddress = estimatedAddress,
                    )
                )
            }
            IncomingIntent.ImportRoute(
                RouteWithMarkers(
                    RouteEntity(name = name, description = ""),
                    markers,
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    fun parseGpx(text: String): IncomingIntent.ImportRoute? {
        if (text.length > MAX_ROUTE_FILE_BYTES) return null
        val route = parseGpxFile(text) ?: return null
        return IncomingIntent.ImportRoute(route)
    }

    private fun parseSoundscape(uri: ParsedUri): IncomingIntent? {
        val host = uri.host
        val pathSegments = uri.pathSegments

        // soundscape://feature/{routes|markers}
        if (host == "feature" && pathSegments.isNotEmpty()) {
            val tab = pathSegments[0]
            if (tab == "routes" || tab == "markers") {
                return IncomingIntent.OpenFeature(tab)
            }
            return null
        }

        // soundscape://route/stop  or  soundscape://route/{name}
        if (host == "route") {
            val first = pathSegments.firstOrNull() ?: return null
            if (first == "stop") return IncomingIntent.StopRoute
            return IncomingIntent.StartRouteByName(first)
        }

        // soundscape://location?lat=&lon=&name=  (preferred — used by the iOS
        // Share Extension; avoids putting commas in the host position).
        if (host == "location") {
            val lat = uri.query["lat"]?.toDoubleOrNull()
            val lon = uri.query["lon"]?.toDoubleOrNull()
            if (lat != null && lon != null) {
                val displayName = uri.query["name"]?.takeIf { it.isNotBlank() }
                return IncomingIntent.OpenLatLon(lat, lon, displayName)
            }
            return null
        }

        // soundscape://lat,lon[?name=...]  (legacy form; commas are commonly
        // percent-encoded as %2C by URLComponents, so decode before regex matching).
        val displayName = uri.query["name"]?.takeIf { it.isNotBlank() }
        return matchCoordinate(uri.schemeSpecificPart, displayName)
    }

    private fun parseGeo(uri: ParsedUri): IncomingIntent? {
        return matchCoordinate(uri.schemeSpecificPart, null)
    }

    private fun matchCoordinate(text: String, displayName: String?): IncomingIntent? {
        val decoded = percentDecode(text)
        val match = coordinateRegex.find(decoded) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        return IncomingIntent.OpenLatLon(lat, lon, displayName)
    }

    private fun parseShareMarker(uri: ParsedUri): IncomingIntent? {
        if (uri.host != SHARE_MARKER_HOST) return null
        if (uri.path != SHARE_MARKER_PATH) return null
        val lat = uri.query["lat"]?.toDoubleOrNull() ?: return null
        val lon = uri.query["lon"]?.toDoubleOrNull() ?: return null
        val nickname = uri.query["nickname"]
        val name = uri.query["name"]
        val displayName = when {
            !nickname.isNullOrBlank() -> nickname
            !name.isNullOrBlank() -> name
            else -> "$lat,$lon"
        }
        return IncomingIntent.OpenLocation(
            LocationDescription(
                name = displayName,
                location = LngLatAlt(lon, lat),
            )
        )
    }
}

private data class ParsedUri(
    val scheme: String,
    val host: String,
    val path: String,
    val schemeSpecificPart: String,
    val query: Map<String, String>,
) {
    val pathSegments: List<String>
        get() = path.trim('/').split('/').filter { it.isNotEmpty() }.map { percentDecode(it) }
}

private fun parseUri(input: String): ParsedUri? {
    val schemeEnd = input.indexOf(':')
    if (schemeEnd <= 0) return null
    val scheme = input.substring(0, schemeEnd).lowercase()
    val rest = input.substring(schemeEnd + 1)

    // Strip "//" authority prefix if present
    val withoutAuthorityPrefix = if (rest.startsWith("//")) rest.substring(2) else rest

    // Split off fragment
    val fragmentSplit = withoutAuthorityPrefix.indexOf('#').let {
        if (it >= 0) withoutAuthorityPrefix.substring(0, it) else withoutAuthorityPrefix
    }

    // Split off query
    val queryIdx = fragmentSplit.indexOf('?')
    val beforeQuery = if (queryIdx >= 0) fragmentSplit.substring(0, queryIdx) else fragmentSplit
    val queryString = if (queryIdx >= 0) fragmentSplit.substring(queryIdx + 1) else ""

    val authorityAndPath = beforeQuery
    val pathStart = authorityAndPath.indexOf('/')
    val (authority, path) = if (rest.startsWith("//")) {
        if (pathStart >= 0) {
            authorityAndPath.substring(0, pathStart) to authorityAndPath.substring(pathStart)
        } else {
            authorityAndPath to ""
        }
    } else {
        // No authority — everything is the scheme-specific part
        "" to authorityAndPath
    }

    return ParsedUri(
        scheme = scheme,
        host = authority.substringBefore(':'),
        path = path,
        schemeSpecificPart = if (rest.startsWith("//")) "$authority$path" else authorityAndPath,
        query = parseQuery(queryString),
    )
}

private fun parseQuery(query: String): Map<String, String> {
    if (query.isEmpty()) return emptyMap()
    val result = mutableMapOf<String, String>()
    for (pair in query.split('&')) {
        if (pair.isEmpty()) continue
        val eq = pair.indexOf('=')
        if (eq < 0) {
            result[percentDecode(pair)] = ""
        } else {
            result[percentDecode(pair.substring(0, eq))] = percentDecode(pair.substring(eq + 1))
        }
    }
    return result
}

private fun percentDecode(input: String): String {
    if ('%' !in input && '+' !in input) return input
    val out = StringBuilder()
    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < input.length) {
        val c = input[i]
        when {
            c == '%' && i + 2 < input.length -> {
                val hex = input.substring(i + 1, i + 3)
                val byte = hex.toIntOrNull(16)
                if (byte != null) {
                    bytes.add(byte.toByte())
                    i += 3
                    continue
                }
                flushBytes(out, bytes)
                out.append(c)
                i++
            }
            c == '+' -> {
                flushBytes(out, bytes)
                out.append(' ')
                i++
            }
            else -> {
                flushBytes(out, bytes)
                out.append(c)
                i++
            }
        }
    }
    flushBytes(out, bytes)
    return out.toString()
}

private fun flushBytes(out: StringBuilder, bytes: MutableList<Byte>) {
    if (bytes.isEmpty()) return
    out.append(bytes.toByteArray().decodeToString())
    bytes.clear()
}
