package org.scottishtecharmy.soundscape.geoengine.utils.pmtiles

import okio.Buffer
import okio.FileHandle
import okio.FileSystem
import okio.GzipSource
import okio.Path
import okio.buffer

class PmTilesReader(path: Path, fileSystem: FileSystem = FileSystem.SYSTEM) : AutoCloseable {

    private val fileHandle: FileHandle = fileSystem.openReadOnly(path)
    private val header = Header()
    private val rootDirectory: Directory
    private val leafCache = LruCache<Long, Directory>(20)
    private val tileCount = mutableListOf(0L)

    val tileCompression: Byte get() = header.tileCompression

    init {
        header.read(fileHandle)
        rootDirectory = Directory()
        rootDirectory.read(
            fileHandle,
            header.rootDirOffset,
            header.rootDirLength,
            header.internalCompression
        )
    }

    fun getTile(zoom: Int, x: Int, y: Int): ByteArray? {
        val id = hilbertZxyToIndex(zoom, x.toLong(), y.toLong()) + getZoomOffset(zoom)
        return rootDirectory.findTile(id)
    }

    override fun close() {
        fileHandle.close()
    }

    private fun getZoomOffset(z: Int): Long {
        val size = tileCount.size
        if (size < z + 1) {
            for (i in size until z + 1) {
                tileCount.add((1L shl (i - 1)) * (1L shl (i - 1)) + tileCount[i - 1])
            }
        }
        return tileCount[z]
    }

    private inner class Directory {
        var ids = LongArray(0)
        var runLengths = LongArray(0)
        var lengths = LongArray(0)
        var offsets = LongArray(0)
        private var cachedTile: ByteArray? = null
        private var cachedTileId: Long = -1

        fun read(handle: FileHandle, offset: Long, length: Long, compression: Byte) {
            cachedTileId = -1
            val raw = readBytes(handle, offset, length.toInt())
            val decompressed = decompress(raw, compression)
            val buf = ByteArrayReader(decompressed)
            val entries = buf.readVarLong().toInt()
            ids = LongArray(entries)
            runLengths = LongArray(entries)
            lengths = LongArray(entries)
            offsets = LongArray(entries)

            var lastId = 0L
            for (i in 0 until entries) {
                val diff = buf.readVarLong()
                lastId += diff
                ids[i] = lastId
            }
            for (i in 0 until entries) {
                runLengths[i] = buf.readVarLong()
            }
            for (i in 0 until entries) {
                lengths[i] = buf.readVarLong()
            }
            for (i in 0 until entries) {
                val value = buf.readVarLong()
                if (value == 0L && i > 0) {
                    offsets[i] = offsets[i - 1] + lengths[i - 1]
                } else {
                    offsets[i] = value - 1
                }
            }
        }

        fun findTile(id: Long): ByteArray? {
            val index = ids.binarySearch(id)
            if (index >= 0) {
                val runLength = runLengths[index]
                return when {
                    runLength == 1L -> readTile(index)
                    runLength > 1L -> getCachedTile(id, index)
                    else -> findTileInLeaf(id, index)
                }
            }
            val prev = -index - 2
            if (prev >= 0) {
                val runLength = runLengths[prev]
                if (runLength > 0) {
                    if (ids[prev] + runLength - 1 >= id) {
                        return getCachedTile(ids[prev], prev)
                    }
                } else {
                    return findTileInLeaf(id, prev)
                }
            }
            return null
        }

        private fun getCachedTile(id: Long, dirIndex: Int): ByteArray {
            if (cachedTileId == id) {
                return cachedTile!!
            }
            val tile = readTile(dirIndex)
            cachedTile = tile
            cachedTileId = id
            return tile
        }

        private fun findTileInLeaf(id: Long, dirIndex: Int): ByteArray? {
            val leafId = ids[dirIndex]
            var leaf = leafCache[leafId]
            if (leaf == null) {
                leaf = Directory()
                leaf.read(
                    fileHandle,
                    header.leafDirOffset + offsets[dirIndex],
                    lengths[dirIndex],
                    header.internalCompression
                )
                leafCache[leafId] = leaf
            }
            return leaf.findTile(id)
        }

        private fun readTile(dirIndex: Int): ByteArray {
            val tileLength = lengths[dirIndex].toInt()
            return readBytes(fileHandle, header.tileDataOffset + offsets[dirIndex], tileLength)
        }
    }

    companion object {
        private const val COMPRESSION_NONE: Byte = 1
        private const val COMPRESSION_GZIP: Byte = 2

        private fun readBytes(handle: FileHandle, offset: Long, length: Int): ByteArray {
            val buffer = Buffer()
            handle.read(offset, buffer, length.toLong())
            return buffer.readByteArray()
        }

        internal fun decompress(data: ByteArray, compression: Byte): ByteArray {
            return when (compression) {
                COMPRESSION_NONE -> data
                COMPRESSION_GZIP -> {
                    val source = Buffer().write(data)
                    GzipSource(source).buffer().readByteArray()
                }
                else -> throw UnsupportedOperationException(
                    "Internal compression $compression not supported"
                )
            }
        }
    }
}

private class Header {
    var tileCompression: Byte = 0
    var internalCompression: Byte = 0
    var rootDirOffset: Long = 0
    var rootDirLength: Long = 0
    var leafDirOffset: Long = 0
    var tileDataOffset: Long = 0

    fun read(handle: FileHandle) {
        val buffer = Buffer()
        handle.read(0L, buffer, HEADER_LENGTH.toLong())
        val bytes = buffer.readByteArray()

        val magic = bytes.copyOfRange(0, 7)
        if (!magic.contentEquals(MAGIC)) {
            throw IllegalStateException("Not a PMTiles file")
        }
        val version = bytes[7]
        if (version != PMTILES_VERSION) {
            throw IllegalStateException("Unsupported PMTiles version $version")
        }
        rootDirOffset = readLittleEndianLong(bytes, 8)
        rootDirLength = readLittleEndianLong(bytes, 16)
        leafDirOffset = readLittleEndianLong(bytes, 40)
        tileDataOffset = readLittleEndianLong(bytes, 56)
        internalCompression = bytes[97]
        tileCompression = bytes[98]
    }

    companion object {
        private const val HEADER_LENGTH = 127
        private const val PMTILES_VERSION: Byte = 3
        private val MAGIC = byteArrayOf(0x50, 0x4D, 0x54, 0x69, 0x6C, 0x65, 0x73)

        private fun readLittleEndianLong(bytes: ByteArray, offset: Int): Long {
            var result = 0L
            for (i in 0 until 8) {
                result = result or ((bytes[offset + i].toLong() and 0xFF) shl (i * 8))
            }
            return result
        }
    }
}

private class ByteArrayReader(private val data: ByteArray) {
    private var position = 0

    fun readVarLong(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            val b = data[position++].toLong() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0L) break
            shift += 7
        }
        return result
    }
}

private fun LongArray.binarySearch(value: Long): Int {
    var low = 0
    var high = size - 1
    while (low <= high) {
        val mid = (low + high) ushr 1
        val midVal = this[mid]
        when {
            midVal < value -> low = mid + 1
            midVal > value -> high = mid - 1
            else -> return mid
        }
    }
    return -(low + 1)
}

internal fun hilbertZxyToIndex(z: Int, x: Long, y: Long): Long {
    val n = 1L shl z
    var rx: Int
    var ry: Int
    var d = 0L
    var mutX = x
    var mutY = y
    var s = n / 2
    while (s > 0) {
        rx = if (mutX and s > 0) 1 else 0
        ry = if (mutY and s > 0) 1 else 0
        d += s * s * ((3 * rx) xor ry).toLong()
        if (ry == 0) {
            if (rx == 1) {
                mutX = n - 1 - mutX
                mutY = n - 1 - mutY
            }
            val t = mutX
            mutX = mutY
            mutY = t
        }
        s /= 2
    }
    return d
}

private class LruCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>()

    operator fun get(key: K): V? {
        val value = map.remove(key) ?: return null
        map[key] = value
        return value
    }

    operator fun set(key: K, value: V) {
        map.remove(key)
        map[key] = value
        if (map.size > maxSize) {
            val eldest = map.keys.first()
            map.remove(eldest)
        }
    }
}
