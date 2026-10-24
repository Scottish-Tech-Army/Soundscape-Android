package org.scottishtecharmy.soundscape.mapstyle

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okio.Path.Companion.toPath
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.resource.MapResourceError
import org.maplibre.compose.resource.MapResourceLoad
import org.maplibre.compose.resource.MapResourceLoadRequest
import org.maplibre.compose.resource.MapResourceProvider
import org.maplibre.compose.resource.MapResourceRequest
import org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader
import org.scottishtecharmy.soundscape.platform.systemFileSystem

/**
 * Serves the map's tiles from a downloaded .pmtiles extract ourselves, through [PmTilesReader],
 * rather than handing MapLibre a `pmtiles://file://` URL.
 *
 * MapLibre Native's own PMTilesFileSource leaks two file requests, and the mailboxes they own, for
 * every tile it reads: its map of in-flight requests is never emptied once a tile has arrived, and
 * nothing removes an entry when a tile is cancelled. Over hours of following the user round a
 * town, that was tens of megabytes of native memory on the test farm. Tiles served from here go
 * through MapLibre's resource provider instead, which tidies up after each request.
 *
 * The style's vector source points at [tileJsonUrl], whose TileJSON is built from the
 * extract the way PMTilesFileSource built it, and whose tile URLs come back here too.
 */
object OfflineExtractTiles : MapResourceProvider {

    private const val SCHEME = "soundscape-extract"
    private const val TILEJSON_PREFIX = "$SCHEME://tilejson?file="
    private const val TILE_PREFIX = "$SCHEME://tile/"

    /** Readers kept open, so a tile doesn't have to read the header and root directory again. */
    private const val MAX_OPEN_READERS = 4

    private class OpenReader(val reader: PmTilesReader, val size: Long?, val modified: Long?) {
        /** A reader isn't safe to use from two threads at once, and tile loads run in parallel. */
        val lock = Mutex()
    }

    private val readersLock = Mutex()
    private val readers = LinkedHashMap<String, OpenReader>()

    /**
     * Makes [DefaultMapRuntime] use this provider. It has to be called before the first map is
     * created, and later calls do nothing.
     */
    fun install() {
        if (installed) return
        installed = true
        DefaultMapRuntime.configure { resourceProvider = this@OfflineExtractTiles }
    }

    // Only touched from the main thread, where maps are composed.
    private var installed = false

    override fun accepts(request: MapResourceRequest): Boolean = request.url.startsWith("$SCHEME:")

    override suspend fun load(request: MapResourceLoadRequest): MapResourceLoad = load(request.url)

    internal suspend fun load(url: String): MapResourceLoad =
        when {
            url.startsWith(TILEJSON_PREFIX) -> loadTileJson(decodePath(url.removePrefix(TILEJSON_PREFIX)))
            url.startsWith(TILE_PREFIX) -> loadTile(url.removePrefix(TILE_PREFIX))
            else -> MapResourceLoad.Failed(
                MapResourceError.NotFound,
                "Not an offline extract URL: $url",
            )
        }

    private suspend fun loadTileJson(path: String): MapResourceLoad =
        withReader(path) { reader ->
            MapResourceLoad.Bytes(buildTileJson(path, reader).encodeToByteArray())
        } ?: missingExtract(path)

    /** [rest] is `z/x/y?file=<path>`. */
    private suspend fun loadTile(rest: String): MapResourceLoad {
        val query = rest.indexOf("?file=")
        val zxy = (if (query < 0) rest else rest.substring(0, query)).split('/')
        val z = zxy.getOrNull(0)?.toIntOrNull()
        val x = zxy.getOrNull(1)?.toIntOrNull()
        val y = zxy.getOrNull(2)?.toIntOrNull()
        if (query < 0 || z == null || x == null || y == null) {
            return MapResourceLoad.Failed(
                MapResourceError.NotFound,
                "Malformed offline tile URL",
            )
        }
        val path = decodePath(rest.substring(query + "?file=".length))
        return withReader(path) { reader ->
            reader.getDecompressedTile(z, x, y)?.let { MapResourceLoad.Bytes(it) }
                ?: MapResourceLoad.NoContent()
        } ?: missingExtract(path)
    }

    private fun missingExtract(path: String) = MapResourceLoad.Failed(
        MapResourceError.NotFound,
        "Offline extract $path is missing or unreadable",
    )

    /**
     * Runs [action] with an open reader for [path], or returns null if the file can't be opened.
     * A reader is opened again if the file has changed since it was opened, and dropped if it has
     * gone, so a deleted extract isn't held open.
     */
    private suspend fun <T> withReader(path: String, action: (PmTilesReader) -> T): T? {
        val metadata = systemFileSystem.metadataOrNull(path.toPath())
        val open = readersLock.withLock {
            val existing = readers.remove(path)
            if (existing != null &&
                (metadata == null || existing.size != metadata.size ||
                    existing.modified != metadata.lastModifiedAtMillis)
            ) {
                closeLater(existing)
            } else if (existing != null) {
                readers[path] = existing
                return@withLock existing
            }
            if (metadata == null) return@withLock null
            val reader = try {
                PmTilesReader(path.toPath())
            } catch (_: Exception) {
                return@withLock null
            }
            OpenReader(reader, metadata.size, metadata.lastModifiedAtMillis).also { opened ->
                readers[path] = opened
                while (readers.size > MAX_OPEN_READERS) {
                    val eldest = readers.keys.first()
                    readers.remove(eldest)?.let { closeLater(it) }
                }
            }
        } ?: return null
        return open.lock.withLock {
            try {
                action(open.reader)
            } catch (_: Exception) {
                null
            }
        }
    }

    /** Closes every open reader. */
    internal suspend fun closeReaders() {
        val open = readersLock.withLock { readers.values.toList().also { readers.clear() } }
        open.forEach { closeLater(it) }
    }

    /** Closes [open] once any tile read that's using it has finished. */
    private suspend fun closeLater(open: OpenReader) {
        open.lock.withLock { open.reader.close() }
    }

    private fun buildTileJson(path: String, reader: PmTilesReader): String {
        val metadata = try {
            Json.parseToJsonElement(reader.readMetadata().decodeToString()) as? JsonObject
        } catch (_: Exception) {
            null
        } ?: JsonObject(emptyMap())
        val tiles = "${TILE_PREFIX}{z}/{x}/{y}?file=${encodePath(path)}"
        val tileJson = metadata + mapOf(
            "tilejson" to JsonPrimitive("3.0.0"),
            "scheme" to JsonPrimitive("xyz"),
            "tiles" to JsonArray(listOf(JsonPrimitive(tiles))),
            "minzoom" to JsonPrimitive(reader.minZoom),
            "maxzoom" to JsonPrimitive(reader.maxZoom),
            "bounds" to JsonArray(reader.bounds.map { JsonPrimitive(it) }),
            "center" to JsonArray(reader.center.map { JsonPrimitive(it) }),
        )
        return JsonObject(tileJson).toString()
    }

    /** Percent-encodes everything in [path] other than unreserved characters and `/`. */
    internal fun encodePath(path: String): String = buildString {
        for (byte in path.encodeToByteArray()) {
            val c = byte.toInt() and 0xFF
            if (c < 0x80 && (c.toChar().isLetterOrDigit() || c.toChar() in "-._~/")) {
                append(c.toChar())
            } else {
                append('%')
                append(HEX[c shr 4])
                append(HEX[c and 0xF])
            }
        }
    }

    internal fun decodePath(encoded: String): String {
        val bytes = ArrayList<Byte>(encoded.length)
        var i = 0
        while (i < encoded.length) {
            val c = encoded[i]
            if (c == '%' && i + 2 < encoded.length) {
                bytes.add(encoded.substring(i + 1, i + 3).toInt(16).toByte())
                i += 3
            } else {
                bytes.addAll(c.toString().encodeToByteArray().toList())
                i++
            }
        }
        return bytes.toByteArray().decodeToString()
    }

    private const val HEX = "0123456789ABCDEF"

    /** The URL a style's vector source uses to read its tiles from the extract at [path]. */
    fun tileJsonUrl(path: String): String = TILEJSON_PREFIX + encodePath(path)
}
