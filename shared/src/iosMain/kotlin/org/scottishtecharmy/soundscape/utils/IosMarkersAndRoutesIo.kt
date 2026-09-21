package org.scottishtecharmy.soundscape.utils

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSFileCoordinatorReadingForUploading
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject

/**
 * iOS implementation of [MarkersAndRoutesIo]. Export packs the GPX files into
 * a directory and uses NSFileCoordinator's `.forUploading` option to produce a
 * zip, then hands it to UIActivityViewController. Import uses
 * UIDocumentPickerViewController and reads the picked archive with okio's
 * cross-platform zip filesystem.
 */
class IosMarkersAndRoutesIo : MarkersAndRoutesIo {

    // Holds a strong reference to the active picker delegate. Only mutated on
    // the main thread, so no synchronization is required.
    private var pickerDelegate: DocumentPickerDelegate? = null

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun exportGpxZip(
        files: List<NamedGpx>,
        suggestedFilename: String,
        shareTitle: String,
    ) {
        try {
            val timestamp = currentTimestamp()
            val tmpRoot = NSHomeDirectory() + "/tmp/markers-routes-export-$timestamp"
            val srcDir = "$tmpRoot/$suggestedFilename"
            NSFileManager.defaultManager.createDirectoryAtPath(
                path = srcDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            for (file in files) {
                val outPath = "$srcDir/${file.filename}"
                NSString.create(string = file.content).writeToFile(
                    path = outPath,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null,
                )
            }

            val srcUrl = NSURL.fileURLWithPath(srcDir, isDirectory = true)
            val coordinator = NSFileCoordinator(filePresenter = null)
            val finalPath = "$tmpRoot/$suggestedFilename-$timestamp.zip"
            val finalUrl = NSURL.fileURLWithPath(finalPath)

            // The accessor block runs synchronously, so we capture the result
            // into a flag and use it below rather than coroutining around it.
            var copied = false
            coordinator.coordinateReadingItemAtURL(
                url = srcUrl,
                options = NSFileCoordinatorReadingForUploading,
                error = null,
            ) { newUrl ->
                if (newUrl != null) {
                    NSFileManager.defaultManager.removeItemAtURL(finalUrl, error = null)
                    copied = NSFileManager.defaultManager.copyItemAtURL(
                        srcURL = newUrl,
                        toURL = finalUrl,
                        error = null,
                    )
                }
            }

            if (copied) {
                withContext(Dispatchers.Main) { presentShareSheet(finalUrl) }
            }
        } catch (e: Exception) {
            println("IosMarkersAndRoutesIo: export failed: ${e.message}")
        }
    }

    override suspend fun pickGpxZip(): List<NamedGpx>? {
        val pickedUrl = withContext(Dispatchers.Main) { presentDocumentPicker() }
            ?: return null
        return withContext(Dispatchers.Default) {
            try {
                readZipEntries(pickedUrl)
            } catch (e: Exception) {
                println("IosMarkersAndRoutesIo: import failed: ${e.message}")
                null
            }
        }
    }

    private fun readZipEntries(url: NSURL): List<NamedGpx> {
        val path = url.path ?: return emptyList()
        val zipFs = FileSystem.SYSTEM.openZip(path.toPath())
        val files = mutableListOf<NamedGpx>()
        val stack = ArrayDeque<Path>()
        stack.addLast("/".toPath())
        while (stack.isNotEmpty()) {
            val dir = stack.removeLast()
            val entries: List<Path> = try {
                zipFs.list(dir)
            } catch (_: Exception) {
                emptyList()
            }
            for (entry in entries) {
                val meta = zipFs.metadataOrNull(entry) ?: continue
                if (meta.isDirectory) {
                    stack.addLast(entry)
                } else {
                    val name = entry.name
                    if (name.endsWith(".gpx")) {
                        val source = zipFs.source(entry).buffer()
                        try {
                            files += NamedGpx(filename = name, content = source.readUtf8())
                        } finally {
                            source.close()
                        }
                    }
                }
            }
        }
        return files
    }

    private suspend fun presentDocumentPicker(): NSURL? {
        val deferred = CompletableDeferred<NSURL?>()
        val delegate = DocumentPickerDelegate(deferred)
        val controller = UIDocumentPickerViewController(
            documentTypes = listOf("public.zip-archive"),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
        )
        controller.delegate = delegate
        // Hold a strong reference to the delegate until the picker completes.
        pickerDelegate = delegate
        presentTopViewController(controller)
        return try {
            deferred.await()
        } finally {
            if (pickerDelegate === delegate) pickerDelegate = null
        }
    }

    private class DocumentPickerDelegate(
        private val deferred: CompletableDeferred<NSURL?>,
    ) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>,
        ) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            if (url == null) {
                deferred.complete(null)
                return
            }
            // Importing security-scoped documents requires bookmarking the
            // resource for the duration of the read.
            val accessed = url.startAccessingSecurityScopedResource()
            try {
                deferred.complete(url)
            } finally {
                if (accessed) url.stopAccessingSecurityScopedResource()
            }
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            deferred.complete(null)
        }
    }

    private fun presentShareSheet(fileUrl: NSURL) {
        val controller = UIActivityViewController(
            activityItems = listOf(fileUrl),
            applicationActivities = null,
        )
        presentTopViewController(controller)
    }

    private fun presentTopViewController(viewController: UIViewController) {
        val keyWindow = UIApplication.sharedApplication.windows
            .mapNotNull { it as? UIWindow }
            .firstOrNull { it.isKeyWindow() }
            ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
            ?: return
        var top: UIViewController? = keyWindow.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        top?.presentViewController(viewController, animated = true, completion = null)
    }

    private fun currentTimestamp(): String {
        val formatter = NSDateFormatter().apply { dateFormat = "yyyyMMdd_HHmm" }
        return formatter.stringFromDate(NSDate())
    }
}
