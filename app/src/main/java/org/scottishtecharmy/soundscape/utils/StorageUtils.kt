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
            // dir can be null if a storage device is not mounted, etc.
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
                Log.e(TAG, "Error getting external storage space for ${dir.path}: ${e.message}", e)
            }
        }
        return storageSpaces
    }
}

fun getOfflineMapStorage(context: Context): List<StorageSpace> {
    var defaultPath = ""

    // Create a list of available storages
    val storages: MutableList<StorageSpace> = emptyList<StorageSpace>().toMutableList()

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
            println(file.path)
            val progressFile = File("${file.path}.progress")
            if (progressFile.exists()) {
                // We're still downloading this file, so don't use it
                println(progressFile.path)
                continue
            }

            val feature = getMetadata(file.path)
            if(feature != null)
                extractCollection.addFeature(feature)
            else
                println("No metadata")
        }
        return extractCollection
    }
    return null
}

fun isMidDownload(path: String) : Long {
    val extractsDir = File(path)
    if (extractsDir.exists() && extractsDir.isDirectory) {
        val files =
            extractsDir.listFiles { file -> file.name.endsWith(".progress") }?.toList()
                ?: emptyList()

        if(files.isNotEmpty()) {
            return files[0].readText().toLong()
        }
    }
    return -1
}