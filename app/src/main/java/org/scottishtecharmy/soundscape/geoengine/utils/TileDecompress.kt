package org.scottishtecharmy.soundscape.geoengine.utils

import vector_tile.VectorTile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

fun decompressGzip(compressedData: ByteArray): ByteArray? {
    // Create a ByteArrayInputStream from the compressed data
    val byteArrayInputStream = ByteArrayInputStream(compressedData)
    var gzipInputStream: GZIPInputStream? = null
    val outputStream = ByteArrayOutputStream()

    try {
        // Wrap the ByteArrayInputStream with GZIPInputStream
        gzipInputStream = GZIPInputStream(byteArrayInputStream)

        // Buffer for reading decompressed data
        val buffer = ByteArray(1024) // Adjust buffer size as needed
        var len: Int

        // Read from GZIPInputStream and write to ByteArrayOutputStream
        while (gzipInputStream.read(buffer).also { len = it } > 0) {
            outputStream.write(buffer, 0, len)
        }

        return outputStream.toByteArray()

    } catch (e: IOException) {
        // Handle potential IOExceptions during decompression
        e.printStackTrace() // Log the error or handle it appropriately
        return null
    } finally {
        // Ensure streams are closed
        try {
            gzipInputStream?.close()
            outputStream.close()
            byteArrayInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun decompressTile(compressionType: Byte?, rawTileData: ByteArray) : VectorTile.Tile? {
    //println("File reader got a tile for worker $workerIndex")
    when (compressionType) {
        1.toByte() -> {
            // No compression
            return VectorTile.Tile.parseFrom(rawTileData)
        }

        2.toByte() -> {
            // Gzip compression
            val decompressedTile = decompressGzip(rawTileData)
            return VectorTile.Tile.parseFrom(decompressedTile)
        }

        else -> assert(false)
    }
    return null
}
