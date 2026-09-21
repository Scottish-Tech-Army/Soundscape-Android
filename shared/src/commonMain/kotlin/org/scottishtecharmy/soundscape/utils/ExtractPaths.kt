package org.scottishtecharmy.soundscape.utils

import okio.Path.Companion.toPath
import org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader
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
fun isPmtilesUsable(path: String): Boolean {
    val okioPath = path.toPath()
    val metadata = try {
        systemFileSystem.metadata(okioPath)
    } catch (_: Exception) {
        return false
    }
    val size = metadata.size
    val lastModified = metadata.lastModifiedAtMillis

    pmtilesUsableCache[path]?.let { cached ->
        if (cached.size == size && cached.lastModified == lastModified) {
            return cached.usable
        }
    }

    val usable = try {
        val reader = PmTilesReader(okioPath)
        try {
            reader.readMetadata()
            reader.validateIntegrity(size ?: 0L)
        } finally {
            reader.close()
        }
        true
    } catch (_: Exception) {
        false
    }
    pmtilesUsableCache[path] = PmtilesValidation(size, lastModified, usable)
    return usable
}

private class PmtilesValidation(val size: Long?, val lastModified: Long?, val usable: Boolean)

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
