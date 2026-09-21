package org.scottishtecharmy.soundscape.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers

/**
 * The user's whole marker and route library as a set of GPX documents: one per route, plus one
 * holding every marker that isn't on a route.
 *
 * GPX rather than a database dump so users can open their data in other tools, and because it is
 * the format the app already imports. This is the archive itself - how it reaches its destination
 * is somebody else's problem. Settings → Advanced markers and routes zips it and hands it to a
 * share sheet; the iCloud backup wraps the same archive in a JSON envelope and stores it as a
 * single key.
 */
const val GLOBAL_MARKERS_FILE_ROOT = "AllSoundscapeDatabaseMarkersInASingleRoute"

/** Reads the whole library out of [dao] as GPX documents. */
suspend fun buildMarkersAndRoutesArchive(dao: RouteDao): List<NamedGpx> {
    val routes = dao.getAllRoutesWithMarkers()
    val markers = dao.getAllMarkers()
    val allMarkersRoute = RouteWithMarkers(
        route = RouteEntity(0, GLOBAL_MARKERS_FILE_ROOT, ""),
        markers = markers,
    )

    val files = mutableListOf<NamedGpx>()
    val usedNames = mutableMapOf<String, Int>()
    files += namedGpxFor(allMarkersRoute, usedNames)
    for (route in routes) {
        files += namedGpxFor(route, usedNames)
    }
    return files
}

/**
 * Writes an archive back into [dao], returning the number of GPX documents that contributed
 * anything.
 *
 * Markers are merged by location rather than duplicated, so re-importing an archive over a library
 * that already holds some of it updates those markers instead of doubling them up. Documents that
 * don't parse are skipped - recovering most of an archive beats recovering none of it.
 */
suspend fun restoreMarkersAndRoutesArchive(files: List<NamedGpx>, dao: RouteDao): Int {
    var restored = 0
    for (file in files) {
        val parsed = parseGpxFile(file.content) ?: continue
        if (file.filename.contains(GLOBAL_MARKERS_FILE_ROOT)) {
            // Standalone markers are merged first so the per-route documents below can reuse
            // them via insertRouteWithNewMarkers.
            for (marker in parsed.markers) {
                val existingMarker = dao.getMarkerByLocation(marker.longitude, marker.latitude)
                if (existingMarker == null) {
                    dao.insertMarker(marker)
                } else {
                    dao.updateMarker(
                        MarkerEntity(
                            markerId = existingMarker.markerId,
                            name = marker.name,
                            fullAddress = marker.fullAddress,
                            longitude = existingMarker.longitude,
                            latitude = existingMarker.latitude,
                        ),
                    )
                }
            }
            if (parsed.markers.isNotEmpty()) restored += 1
        } else {
            val newRoute = RouteEntity(
                name = parsed.route.name,
                description = parsed.route.description,
            )
            dao.insertRouteWithNewMarkers(newRoute, parsed.markers)
            if (parsed.markers.isNotEmpty()) restored += 1
        }
    }
    return restored
}

private fun namedGpxFor(
    route: RouteWithMarkers,
    usedNames: MutableMap<String, Int>,
): NamedGpx {
    var fileRoot = sanitizeFilename(route.route.name)
    val current = usedNames[fileRoot]
    if (current == null) {
        usedNames[fileRoot] = 0
    } else {
        usedNames[fileRoot] = current + 1
        fileRoot = "${fileRoot}_${current + 1}"
    }
    return NamedGpx(filename = "$fileRoot.gpx", content = generateGpxString(route))
}

internal fun generateGpxString(route: RouteWithMarkers): String = buildString {
    append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    append("<gpx version=\"1.1\" creator=\"Soundscape\">\n")
    append("  <metadata>\n")
    append("    <name>${escapeXml(route.route.name)}</name>\n")
    append("    <desc>${escapeXml(route.route.description)}</desc>\n")
    append("  </metadata>\n")
    for (marker in route.markers) {
        append("      <wpt lat=\"${marker.latitude}\" lon=\"${marker.longitude}\">\n")
        append("        <name>${escapeXml(marker.name)}</name>\n")
        append("        <desc>${escapeXml(marker.fullAddress)}</desc>\n")
        append("      </wpt>\n")
    }
    append("</gpx>")
}

/**
 * Escapes text going into a GPX element.
 *
 * Markers are named by the user and by map data, so "Bob & Alice" and "<no name>" both turn up.
 * Without this they produce a document that isn't XML at all, which the parser rejects on the way
 * back in - the marker isn't mangled, the whole file is lost.
 */
private fun escapeXml(text: String): String = buildString(text.length) {
    for (character in text) {
        when (character) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            else -> append(character)
        }
    }
}

private val INVALID_FILENAME_CHARS = Regex("[/\\\\:*?\"<>|\\x00]")

private fun sanitizeFilename(name: String): String =
    name.replace(INVALID_FILENAME_CHARS, "_").take(100)

/**
 * Packs an archive into a single string, for destinations that store one value rather than a
 * folder of files - the iCloud key-value store, principally.
 *
 * A JSON envelope around the GPX rather than a zip because the destination wants text and because
 * the archives involved are tens of kilobytes; compressing them would buy little and cost the
 * ability to read a backup by eye when diagnosing one.
 */
fun encodeArchiveEnvelope(files: List<NamedGpx>): String {
    val document = buildJsonObject {
        put(ENVELOPE_KEY_VERSION, ARCHIVE_ENVELOPE_VERSION)
        put(
            ENVELOPE_KEY_FILES,
            buildJsonArray {
                for (file in files) {
                    add(
                        buildJsonObject {
                            put(ENVELOPE_KEY_FILENAME, file.filename)
                            put(ENVELOPE_KEY_GPX, file.content)
                        },
                    )
                }
            },
        )
    }
    return document.toString()
}

/** Unpacks [encodeArchiveEnvelope], or returns null if the string isn't one. */
fun decodeArchiveEnvelope(json: String): List<NamedGpx>? {
    val root = try {
        Json.parseToJsonElement(json) as? JsonObject ?: return null
    } catch (t: Throwable) {
        return null
    }

    val version = root[ENVELOPE_KEY_VERSION]?.jsonPrimitive?.intOrNull ?: return null
    if (version > ARCHIVE_ENVELOPE_VERSION) {
        // Written by a newer version of the app. Restoring the parts we understand would quietly
        // drop whatever it added, so refuse rather than half-restore.
        return null
    }

    val files = root[ENVELOPE_KEY_FILES] as? JsonArray ?: return null
    return files.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        NamedGpx(
            filename = obj[ENVELOPE_KEY_FILENAME]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null,
            content = obj[ENVELOPE_KEY_GPX]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null,
        )
    }
}

private const val ARCHIVE_ENVELOPE_VERSION = 1
private const val ENVELOPE_KEY_VERSION = "version"
private const val ENVELOPE_KEY_FILES = "files"
private const val ENVELOPE_KEY_FILENAME = "filename"
private const val ENVELOPE_KEY_GPX = "gpx"
