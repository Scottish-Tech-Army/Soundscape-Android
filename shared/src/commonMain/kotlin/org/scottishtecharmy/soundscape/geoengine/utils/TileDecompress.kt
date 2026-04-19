@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package org.scottishtecharmy.soundscape.geoengine.utils

import okio.Buffer
import okio.GzipSource
import okio.buffer
import vector_tile.Tile

fun decompressGzip(compressedData: ByteArray): ByteArray? {
    return try {
        val source = Buffer().write(compressedData)
        GzipSource(source).buffer().readByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun decompressTile(compressionType: Byte?, rawTileData: ByteArray): Tile? {
    when (compressionType) {
        1.toByte() -> {
            return Tile.ADAPTER.decode(rawTileData)
        }

        2.toByte() -> {
            val decompressedTile = decompressGzip(rawTileData)
            return decompressedTile?.let { Tile.ADAPTER.decode(it) }
        }

        else -> assert(false)
    }
    return null
}
