package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.util.Log
import java.io.File

object StorageUtils {

    const val TAG = "StorageUtils"

    data class StorageSpace(
        val path: String,
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
     * Gets free space information for internal storage (app-specific files directory).
     */
    fun getInternalStorageSpace(context: Context): StorageSpace? {
        return try {
            val internalFilesDir = context.filesDir // App's private internal storage
            if (internalFilesDir == null || internalFilesDir.path.isNullOrEmpty()) {
                Log.e(TAG, "Internal files directory is null or path is empty.")
                return null
            }
            val statFs = StatFs(internalFilesDir.path)
            val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
            val availableBytes = statFs.availableBlocksLong * statFs.blockSizeLong
            val freeBytes = statFs.freeBlocksLong * statFs.blockSizeLong
            StorageSpace(
                path = internalFilesDir.absolutePath,
                isExternal = false,
                isPrimary = false,
                totalBytes = totalBytes,
                availableBytes = availableBytes,
                availableString = Formatter.formatFileSize(context, availableBytes),
                freeBytes = freeBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting internal storage space: ${e.message}", e)
            null
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


        for (dir in externalFilesDirs) {
            // dir can be null if a storage device is not mounted, etc.
            try {
                val statFs = StatFs(dir.path)
                val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
                val availableBytes = statFs.availableBlocksLong * statFs.blockSizeLong
                val freeBytes = statFs.freeBlocksLong * statFs.blockSizeLong
                val isPrimary = primaryExternalStoragePath != null && dir.absolutePath.startsWith(primaryExternalStoragePath)
                storageSpaces.add(
                    StorageSpace(
                        path = dir.absolutePath,
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
