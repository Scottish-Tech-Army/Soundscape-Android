package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.format.Formatter
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import org.scottishtecharmy.soundscape.utils.StorageUtils.StorageSpace
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import kotlin.text.isEmpty

object StorageUtils {

    const val TAG = "StorageUtils"

    data class StorageSpace(
        val path: String,
        val description: String,
        val isExternal: Boolean,
        val isPrimary: Boolean = false,
        val totalBytes: Long,
        val availableBytes: Long,
        val availableString: String,
        val freeBytes: Long, // For reference, usually availableBytes is what you need
    ) {
        override fun toString(): String {
            return """
                Path: $path
                Type: ${if (isExternal) "External" else "Internal"}${if (isPrimary && isExternal) " (Primary)" else ""}
                Total: $totalBytes
                Available: $availableBytes
                Free: $freeBytes
            """.trimIndent()
        }
    }

    /**
     * Gets free space information for all available external storage volumes
     * using app-specific directories.
     * This is generally the recommended approach for handling external storage.
     */
    fun getExternalStorageSpacesAppSpecific(context: Context): List<StorageSpace> {
        val storageSpaces = mutableListOf<StorageSpace>()
        val externalFilesDirs: Array<File> = context.getExternalFilesDirs(null)
        val primaryExternalStoragePath = try {
            Environment.getExternalStorageDirectory()?.absolutePath
        } catch (_: Exception) { null }


        val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        for (dir in externalFilesDirs) {
            @Suppress("SENSELESS_COMPARISON")
            if (dir == null) {
                // dir can be null if a storage device is not mounted, etc.
                continue
            }

            try {
                val statFs = StatFs(dir.path)
                val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
                val availableBytes = statFs.availableBlocksLong * statFs.blockSizeLong
                val freeBytes = statFs.freeBlocksLong * statFs.blockSizeLong
                val isPrimary = primaryExternalStoragePath != null && dir.absolutePath.startsWith(primaryExternalStoragePath)
                val sv: StorageVolume? = sm.getStorageVolume(dir)
                val description = sv?.getDescription(context) ?: "External Storage"

                storageSpaces.add(
                    StorageSpace(
                        path = dir.absolutePath,
                        description = description,
                        isExternal = true,
                        isPrimary = isPrimary,
                        totalBytes = totalBytes,
                        availableBytes = availableBytes,
                        availableString = Formatter.formatFileSize(context, availableBytes),
                        freeBytes = freeBytes)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting external storage space: ${e.message}")
            }
        }
        return storageSpaces
    }
}

fun getOfflineMapStorage(context: Context): List<StorageSpace> {
    var defaultPath = ""

    // Create a list of available storages
    val storages = mutableListOf<StorageSpace>()

// The DownloadManager can't write to internal storage, so we ignore it. Android devices have
// "external storage" that is emulated on internal storage so it's not an issue.
//    val internalSpace = StorageUtils.getInternalStorageSpace(context)
//    internalSpace?.let {
//        defaultPath = it.path
//        storages.add(internalSpace)
//    }

    val externalAppSpecificSpaces = StorageUtils.getExternalStorageSpacesAppSpecific(context)
    for(storage in externalAppSpecificSpaces) {
        storages.add(storage)
        if (storage.isPrimary)
            defaultPath = storage.path
        else if(defaultPath.isEmpty())
            defaultPath = storage.path
    }

    // Check that the preference is set
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    var path = sharedPreferences.getString(MainActivity.SELECTED_STORAGE_KEY, MainActivity.SELECTED_STORAGE_DEFAULT)
    if((path == null) || path.isEmpty()) {
        // Default path is to the first external storage
        path = defaultPath
        sharedPreferences.edit(commit = true) { putString(MainActivity.SELECTED_STORAGE_KEY, path) }
    }

    // Ensure that the directories exist
    val filesDir = File(path)
    if(filesDir.exists() && filesDir.isDirectory) {
        val downloadsDir = File(path, Environment.DIRECTORY_DOWNLOADS)
        if(!downloadsDir.exists()) {
            downloadsDir.mkdir()
        }
    }

    return storages
}

fun getMetadata(pmtilesPath: String) : Feature? {
    val geojsonFile = File("$pmtilesPath.geojson")
    if (geojsonFile.exists() && geojsonFile.isFile) {
        val adapter = GeoJsonObjectMoshiAdapter()
        val feature = adapter.fromJson(geojsonFile.readText())
        if(feature != null) {
            if(feature.type == "Feature")
                return feature as Feature
        }
    }
    return null
}

fun findExtracts(path: String) : FeatureCollection? {
    // Find any extracts that we have downloaded
    val extractsDir = File(path)
    if (extractsDir.exists() && extractsDir.isDirectory) {
        // Find files within the directory and filter for those ending with ".pmtiles". It is
        // possible to have metadata for .pmtiles files that failed to download, so we have to
        // search for the .pmtiles files first and then get the metadata for them.
        val files = extractsDir.listFiles { file -> file.name.endsWith(".pmtiles") }?.toList() ?: emptyList()
        val extractCollection = FeatureCollection()
        for(file in files) {
            val feature = getMetadata(file.path)
            if(feature != null) {
                // Validate the extract so callers can show whether it is working or
                // damaged. A damaged extract (its metadata won't decompress) would crash
                // MapLibre if used, so it is excluded from the live map; here we flag it
                // via the "usable" property instead. This does file I/O, so callers
                // should run findExtracts off the main thread.
                val properties = feature.properties ?: HashMap()
                properties["usable"] = isPmtilesUsable(file.path)
                feature.properties = properties
                extractCollection.addFeature(feature)
            } else
                println("No metadata")
        }
        return extractCollection
    }
    return null
}

/**
 * Check that a .pmtiles extract is safe to hand to MapLibre as a tile source.
 *
 * MapLibre's native PMTilesFileSource gzip-decompresses sections of the extract as it
 * renders. That decompress call (mbgl::util::decompress, compression.cpp:98) is NOT
 * wrapped in a try/catch in PMTilesFileSource (pmtiles_file_source.cpp), so a corrupt or
 * truncated extract makes it throw std::runtime_error ("unknown compression method"). The
 * exception escapes MapLibre's file-source worker thread, reaches std::terminate and
 * aborts the whole process - a native crash we cannot catch from Kotlin.
 *
 * The decompressible sections are spread across the whole file: header, root directory and
 * JSON metadata live at the FRONT, but the leaf directories and per-tile gzip data live at
 * the TAIL. An interrupted/truncated download keeps the front intact, so checking only the
 * metadata (as we used to) is not enough - MapLibre still crashes on the damaged tail. So
 * we validate three things here, where any exception IS catchable:
 *  1. the header-declared layout fits within the actual file (catches truncation cheaply),
 *  2. the root directory + JSON metadata decompress (front of file),
 *  3. the physically-last tile decompresses (tail of file, where truncation/padding lands).
 *
 * This does file I/O, so callers should run it off the main thread.
 *
 * getSourceUri() re-runs this for every extract every time a map is created, so the result is
 * cached per path and only recomputed when the file changes (its length or last-modified time).
 * A re-downloaded or deleted-and-replaced extract therefore gets re-validated automatically.
 */
private class PmtilesValidation(
    val length: Long,
    val lastModified: Long,
    val usable: Boolean,
)

private val pmtilesUsableCache = ConcurrentHashMap<String, PmtilesValidation>()

fun isPmtilesUsable(path: String): Boolean {
    val file = File(path)
    val length = file.length()
    val lastModified = file.lastModified()

    pmtilesUsableCache[path]?.let { cached ->
        if (cached.length == length && cached.lastModified == lastModified) {
            return cached.usable
        }
    }

    val usable = validatePmtilesExtract(file)
    pmtilesUsableCache[path] = PmtilesValidation(length, lastModified, usable)
    return usable
}

/** The actual (uncached) validation - see [isPmtilesUsable] for what each step guards against. */
private fun validatePmtilesExtract(file: File): Boolean {
    return try {
        FileInputStream(file).channel.use { channel ->
            val header = readPmtilesHeader(channel)
            // 1. Every declared section must lie within the file - catches a short/truncated
            //    download (file.length() < tileDataOffset + tileDataLength) with no decompression.
            validatePmtilesLayout(header, file.length())
            // 2. Force decompression of the root directory (in the Reader constructor) and the
            //    gzip metadata - the front-of-file sections MapLibre touches first.
            ch.poole.geo.pmtiles.Reader(file).use { reader -> reader.metadata }
            // 3. Decompress the very last tile in the file. It sits at the physical end of the
            //    tile-data section, exactly where truncation or zero-padding corruption lands, so
            //    a clean decompress here is strong evidence the tail MapLibre will read is intact.
            //    (Only meaningful for a clustered archive, where the last directory entry is the
            //    highest tile-data offset; our extracts are always clustered.)
            if (header.clustered == PMTILES_CLUSTERED &&
                header.tileEntries > 0 &&
                header.tileDataLength > 0
            ) {
                checkLastPmtilesTileDecompresses(channel, header)
            }
        }
        true
    } catch (e: Exception) {
        Log.e(StorageUtils.TAG, "Rejecting unusable pmtiles extract ${file.path}: ${e.message}")
        false
    }
}

private const val PMTILES_HEADER_LENGTH = 127
private const val PMTILES_VERSION: Byte = 3
private const val PMTILES_CLUSTERED: Byte = 1
private const val PMTILES_COMPRESSION_NONE: Byte = 1
private const val PMTILES_COMPRESSION_GZIP: Byte = 2
private val PMTILES_MAGIC = byteArrayOf(0x50, 0x4D, 0x54, 0x69, 0x6C, 0x65, 0x73) // "PMTiles"

/**
 * The subset of the fixed 127-byte little-endian PMTiles v3 header we need. The reader
 * library keeps these fields private, so we parse the well-known byte offsets ourselves
 * (see ch.poole.geo.pmtiles.Reader.Header for the layout).
 */
private class PmtilesHeader(
    val rootDirOffset: Long,
    val rootDirLength: Long,
    val metadataOffset: Long,
    val metadataLength: Long,
    val leafDirOffset: Long,
    val leafDirLength: Long,
    val tileDataOffset: Long,
    val tileDataLength: Long,
    val tileEntries: Long,
    val clustered: Byte,
    val internalCompression: Byte,
    val tileCompression: Byte,
)

private fun readPmtilesHeader(channel: FileChannel): PmtilesHeader {
    val buffer = ByteBuffer.allocate(PMTILES_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN)
    var read = 0
    while (buffer.hasRemaining()) {
        val count = channel.read(buffer, read.toLong())
        if (count < 0) break
        read += count
    }
    if (read != PMTILES_HEADER_LENGTH) throw IOException("Incomplete PMTiles header: $read bytes")

    val magic = ByteArray(PMTILES_MAGIC.size)
    buffer.position(0)
    buffer.get(magic)
    if (!magic.contentEquals(PMTILES_MAGIC)) throw IOException("Bad PMTiles magic number")
    val version = buffer.get(7)
    if (version != PMTILES_VERSION) throw IOException("Unsupported PMTiles version $version")

    return PmtilesHeader(
        rootDirOffset = buffer.getLong(8),
        rootDirLength = buffer.getLong(16),
        metadataOffset = buffer.getLong(24),
        metadataLength = buffer.getLong(32),
        leafDirOffset = buffer.getLong(40),
        leafDirLength = buffer.getLong(48),
        tileDataOffset = buffer.getLong(56),
        tileDataLength = buffer.getLong(64),
        tileEntries = buffer.getLong(80),
        clustered = buffer.get(96),
        internalCompression = buffer.get(97),
        tileCompression = buffer.get(98),
    )
}

/** Reject the extract unless every header-declared section lies within the file. */
private fun validatePmtilesLayout(header: PmtilesHeader, fileLength: Long) {
    fun requireInBounds(offset: Long, length: Long, name: String) {
        if (offset < 0 || length < 0 || offset + length > fileLength) {
            throw IOException(
                "PMTiles $name section out of bounds (offset=$offset length=$length fileLength=$fileLength)"
            )
        }
    }
    requireInBounds(header.rootDirOffset, header.rootDirLength, "root directory")
    requireInBounds(header.metadataOffset, header.metadataLength, "metadata")
    requireInBounds(header.leafDirOffset, header.leafDirLength, "leaf directory")
    requireInBounds(header.tileDataOffset, header.tileDataLength, "tile data")
}

/**
 * Locate the physically-last tile by descending the last entry of each directory (root ->
 * last leaf -> ... -> last tile), then read and decompress it. Throws if any read or
 * decompress fails, which is what we want - that is the exact failure that would abort
 * MapLibre natively. The directory binary format mirrors ch.poole.geo.pmtiles.Reader.Directory.
 */
private fun checkLastPmtilesTileDecompresses(channel: FileChannel, header: PmtilesHeader) {
    var dirOffset = header.rootDirOffset
    var dirLength = header.rootDirLength
    var depth = 0
    while (true) {
        if (depth++ > 100) throw IOException("PMTiles leaf directory nesting too deep")

        val raw = readPmtilesSection(channel, dirOffset, dirLength)
        val dir = ByteBuffer.wrap(decompressPmtiles(raw, header.internalCompression))
        val entries = readVarLong(dir).toInt()
        if (entries <= 0) return // empty directory - nothing more to validate

        // Four delta/value-coded arrays in order: ids, runLengths, lengths, offsets.
        for (i in 0 until entries) readVarLong(dir) // ids - we only need to advance past them
        val runLengths = LongArray(entries) { readVarLong(dir) }
        val lengths = LongArray(entries) { readVarLong(dir) }
        val offsets = LongArray(entries)
        for (i in 0 until entries) {
            val value = readVarLong(dir)
            offsets[i] = if (value == 0L && i > 0) offsets[i - 1] + lengths[i - 1] else value - 1
        }

        val last = entries - 1
        if (runLengths[last] == 0L) {
            // Leaf-directory pointer: descend into it.
            dirOffset = header.leafDirOffset + offsets[last]
            dirLength = lengths[last]
        } else {
            // Tile entry: read and decompress its bytes.
            val tile = readPmtilesSection(channel, header.tileDataOffset + offsets[last], lengths[last])
            decompressPmtiles(tile, header.tileCompression)
            return
        }
    }
}

/** Read exactly [length] bytes at [offset], throwing if the file is too short. */
private fun readPmtilesSection(channel: FileChannel, offset: Long, length: Long): ByteArray {
    if (length < 0 || length > Int.MAX_VALUE) throw IOException("Bad PMTiles section length $length")
    val buffer = ByteBuffer.allocate(length.toInt())
    var read = 0
    while (buffer.hasRemaining()) {
        val count = channel.read(buffer, offset + read)
        if (count < 0) break
        read += count
    }
    if (read.toLong() != length) throw IOException("Incomplete PMTiles read: $read of $length at $offset")
    return buffer.array()
}

/** Decompress a section the same way the reader/MapLibre do, per the PMTiles compression byte. */
private fun decompressPmtiles(data: ByteArray, compression: Byte): ByteArray {
    return when (compression) {
        PMTILES_COMPRESSION_NONE -> data
        PMTILES_COMPRESSION_GZIP -> GZIPInputStream(data.inputStream()).use { it.readBytes() }
        else -> InflaterInputStream(data.inputStream()).use { it.readBytes() }
    }
}

/** Read one base-128 varint (little-endian groups) and advance the buffer. */
private fun readVarLong(buffer: ByteBuffer): Long {
    var result = 0L
    var shift = 0
    while (true) {
        val b = buffer.get().toLong() and 0xff
        result = result or ((b and 0x7f) shl shift)
        if (b and 0x80L == 0L) break
        shift += 7
    }
    return result
}

fun findExtractPaths(path: String) : List<String> {
    // Find any extracts that we have downloaded
    val extractsDir = File(path)
    if (extractsDir.exists() && extractsDir.isDirectory) {
        // Find files within the directory and filter for those ending with ".pmtiles". It is
        // possible to have metadata for .pmtiles files that failed to download, so we have to
        // search for the .pmtiles files first and then get the metadata for them.
        val fileList = extractsDir.listFiles { file -> file.name.endsWith(".pmtiles") }?.toList()
        if(fileList != null) {
            val returnList = mutableListOf<String>()
            for (file in fileList) {
                returnList += file.path
            }
            return returnList
        }
    }
    return emptyList()
}
