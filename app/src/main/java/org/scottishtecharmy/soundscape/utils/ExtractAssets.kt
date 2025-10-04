package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * This function extracts assets from the APK into the app's `files` directory so that they can
 * be more easily used e.g. we can pass a file: URI to maplibre pointing at the `files` directory
 * but not into the APK assets directly. There's obviously a storage cost associated with this, so
 * it should only be used when absolutely necessary.
 */
fun extractAssets(applicationContext: Context,
                  assetDir: String,
                  destinationDir: String): Boolean {
    try {
        val sdPath: File = applicationContext.filesDir
        val destDirPath: String = sdPath.toString() + addLeadingSlash(destinationDir)
        val destDir = File(destDirPath)

        Log.d("ExtractAssets", "extracting to $destDirPath")

        createDir(destDir)

        val assetManager: AssetManager = applicationContext.assets
        val files = assetManager.list(assetDir)

        for (i in files!!.indices) {
            val absAssetFilePath = addTrailingSlash(assetDir) + files[i]
            val subFiles = assetManager.list(absAssetFilePath)

            if (subFiles!!.isEmpty()) {
                // It is a file
                val destFilePath = addTrailingSlash(destDirPath) + files[i]
                copyAssetFile(applicationContext, absAssetFilePath, destFilePath)
            } else {
                // It is a sub directory
                extractAssets(
                    applicationContext,
                    absAssetFilePath,
                    addTrailingSlash(destinationDir) + files[i]
                )
            }
        }

        return true
    }
    catch (e: Exception) {
        Log.e("ExtractAssets", "Exception caught: $e")
    }
    return false
}

private fun copyAssetFile(applicationContext: Context, assetFilePath:String, destinationFilePath: String?) {

    if(destinationFilePath == null)
        return

    val file = File(destinationFilePath)
    var writeFile = false
    if(file.exists()) {
        // Compare the existing file to see if the asset has changed
        val inputStream: InputStream = applicationContext.assets.open(assetFilePath)
        val existingOutputStream: InputStream = FileInputStream(destinationFilePath)

        val newBuf = ByteArray(1024)
        val existingBuf = ByteArray(1024)
        var len: Int
        while ((inputStream.read(newBuf).also { len = it }) > 0) {
            existingOutputStream.read(existingBuf, 0, len)
            if(!newBuf.contentEquals(existingBuf)) {
                writeFile = true
                break
            }
        }
        inputStream.close()
        existingOutputStream.close()
    }
    else
        writeFile = true

    if(writeFile) {
        val inputStream: InputStream = applicationContext.assets.open(assetFilePath)
        val outputStream: OutputStream = FileOutputStream(destinationFilePath)

        val buf = ByteArray(1024)
        var len: Int
        while ((inputStream.read(buf).also { len = it }) > 0) outputStream.write(buf, 0, len)
        inputStream.close()
        outputStream.close()
    }
}

private fun addTrailingSlash(path: String): String {
    if (path.isEmpty() || path[path.length - 1] != '/') {
        return "$path/"
    }
    return path
}

private fun addLeadingSlash(path: String): String {
    if (path.isEmpty() || path[0] != '/') {
        return "/$path"
    }
    return path
}

private fun createDir(dir: File) {
    if (dir.exists()) {
        if (!dir.isDirectory) {
            throw IOException("Can't create directory, a file is in the way")
        }
    } else {
        dir.mkdirs()
        if (!dir.isDirectory) {
            throw IOException("Unable to create directory")
        }
    }
}

private fun processStyle(applicationContext: Context, offlineStoragePath: String, inputFilename: String, outputFilename: String) {
    val filesDir = applicationContext.filesDir.toString()
    val outputStyleStream =
        File("$filesDir/osm-liberty-accessible/$outputFilename").outputStream()
    val inputStyleStream = File("$filesDir/osm-liberty-accessible/$inputFilename").inputStream()

    // Get locally downloaded files
    val extractCollection = findExtracts(offlineStoragePath)
    var extractFile: File? = null
    if(extractCollection != null) {
        if(extractCollection.features.isNotEmpty()) {
            // Pick first one to use as the source
            val filename = extractCollection.features[0].properties?.get("filename")
            if (filename != null)
                extractFile = File(offlineStoragePath, filename as String)
        }
    }

    var urlReplacement = "${BuildConfig.TILE_PROVIDER_URL}/$PROTOMAPS_SERVER_PATH.json"
    if(extractFile != null)
        urlReplacement = "pmtiles://file://$extractFile"

    inputStyleStream.bufferedReader().useLines { lines ->
        lines.forEach { line ->
           if (line.contains("PROTOMAPS_SERVER_URL")) {
                val newline = line.replace(
                    "PROTOMAPS_SERVER_URL",
                    urlReplacement
                )
                outputStyleStream.write(newline.toByteArray())
            } else {
                outputStyleStream.write(line.toByteArray())
            }
        }
    }
    inputStyleStream.close()
    outputStyleStream.close()
}

fun processMaps(applicationContext: Context, offlineStorage: String) {
    // Extract the maplibre style assets
    extractAssets(applicationContext, "osm-liberty-accessible", "osm-liberty-accessible")

    // Update extracted style.json with protomaps server URI
    processStyle(applicationContext, offlineStorage, "style.json", "processedStyle.json")
    processStyle(applicationContext, offlineStorage, "originalStyle.json", "processedOriginalStyle.json")
}
