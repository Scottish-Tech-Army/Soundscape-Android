package org.scottishtecharmy.soundscape.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path.Companion.toPath
import org.scottishtecharmy.soundscape.geoengine.WANTED_EXTRACT_SCHEMA
import org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.platform.systemFileSystem

fun findExtractPaths(path: String): List<String> {
    val dir = path.toPath()
    return try {
        systemFileSystem.list(dir)
            .filter { it.name.endsWith(".pmtiles") }
            .map { it.toString() }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Check that a .pmtiles extract is safe to hand to MapLibre as a tile source.
 *
 * MapLibre's native PMTilesFileSource gzip-decompresses sections of the extract as it renders.
 * Those decompress calls are not wrapped in a try/catch natively, so a corrupt or truncated
 * extract throws, escapes MapLibre's worker thread, hits std::terminate and aborts the whole
 * process - a native crash we cannot catch from Kotlin.
 *
 * The decompressible sections are spread across the whole file: header, root directory and JSON
 * metadata live at the front, but leaf directories and per-tile gzip data live at the tail. An
 * interrupted/truncated download keeps the front intact, so checking only the metadata is not
 * enough - MapLibre still crashes on a damaged tail. So [PmTilesReader] validates:
 *  1. the header-declared layout fits within the actual file (catches truncation cheaply),
 *  2. the root directory + JSON metadata decompress (front of file, via the constructor and
 *     [PmTilesReader.readMetadata]),
 *  3. the physically-last tile decompresses (tail of file, where truncation/padding lands, via
 *     [PmTilesReader.validateIntegrity]).
 *
 * This does file I/O, so callers should run it off the main thread.
 *
 * The result is cached per path, keyed by file size and last-modified time, so repeated calls for
 * the same unchanged extract (e.g. every time a map is created) don't re-validate. A re-downloaded
 * or deleted-and-replaced extract is re-validated automatically once its size or timestamp change.
 */
fun isPmtilesUsable(path: String): Boolean = validatePmtiles(path)?.usable ?: false

/**
 * The version of the Soundscape tile schema that a .pmtiles extract was built with, from the
 * "soundscape:schema" entry that planetiler-openmaptiles writes into its JSON metadata. Extracts
 * built before the version existed, and any that can't be read, count as 0.
 *
 * Shares [isPmtilesUsable]'s cache, so asking for both costs a single read of the file.
 */
fun readExtractSchema(path: String): Int = validatePmtiles(path)?.schema ?: 0

/**
 * Whether a downloaded extract should be flagged as needing an update. That is only the case when
 * re-downloading it would bring in tile contents that this version of the app makes use of: the
 * server has an extract with a newer schema than the one on the phone ([manifestEntry]'s "schema"),
 * and the app wants at least that newer schema ([WANTED_EXTRACT_SCHEMA]). A newer schema that the
 * app doesn't use yet, or fresh OSM data with the same schema, is not worth prompting for.
 *
 * With no manifest entry - manifest not fetched yet, fetch failed, or the extract withdrawn - there
 * is nothing to update to, so nothing is flagged.
 */
fun isExtractOutdated(
    localSchema: Int,
    manifestEntry: Feature?,
    wantedSchema: Int = WANTED_EXTRACT_SCHEMA,
): Boolean {
    val schemaProp = manifestEntry?.properties?.get("schema") ?: return false
    val serverSchema = (schemaProp as? Number)?.toInt()
        ?: (schemaProp as? String)?.toIntOrNull()
        ?: return false
    return localSchema < minOf(wantedSchema, serverSchema)
}

private fun validatePmtiles(path: String): PmtilesValidation? {
    val okioPath = path.toPath()
    val metadata = try {
        systemFileSystem.metadata(okioPath)
    } catch (_: Exception) {
        return null
    }
    val size = metadata.size
    val lastModified = metadata.lastModifiedAtMillis

    pmtilesUsableCache[path]?.let { cached ->
        if (cached.size == size && cached.lastModified == lastModified) {
            return cached
        }
    }

    var schema = 0
    val usable = try {
        val reader = PmTilesReader(okioPath)
        try {
            schema = parseSchema(reader.readMetadata())
            reader.validateIntegrity(size ?: 0L)
        } finally {
            reader.close()
        }
        true
    } catch (_: Exception) {
        false
    }
    val validation = PmtilesValidation(size, lastModified, usable, schema)
    pmtilesUsableCache[path] = validation
    return validation
}

private fun parseSchema(metadataJson: ByteArray): Int = try {
    Json.parseToJsonElement(metadataJson.decodeToString())
        .jsonObject["soundscape:schema"]
        ?.jsonPrimitive
        ?.content
        ?.toIntOrNull() ?: 0
} catch (_: Exception) {
    0
}

private class PmtilesValidation(
    val size: Long?,
    val lastModified: Long?,
    val usable: Boolean,
    val schema: Int,
)

private val pmtilesUsableCache = mutableMapOf<String, PmtilesValidation>()

/**
 * Manifest "filename" values carry a server-side build prefix ahead of the logical extract name,
 * e.g. "20260820-1354-glasgow-gb.pmtiles". That prefix changes every time the planet-wide map is
 * rebuilt and the extracts regenerated, so an extract downloaded from one build has a different
 * filename from the same extract in a later manifest. Android additionally names its local copies
 * "glasgow-gb.v<timestamp>.pmtiles".
 *
 * [logicalExtractName] reduces any of those - a manifest filename, a remote path, or a local
 * path - to the stable identity of the extract ("glasgow-gb"), so downloaded files can be matched
 * against a manifest that has since been regenerated.
 */
fun logicalExtractName(filename: String): String =
    filename.substringAfterLast('/')
        .removeSuffix(".pmtiles")
        .replace(buildPrefixRegex, "")
        .replace(versionSuffixRegex, "")

private val buildPrefixRegex = Regex("""^\d{8}-[0-9a-zA-Z]+-""")
private val versionSuffixRegex = Regex("""\.v\d+$""")
