package org.scottishtecharmy.soundscape.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

@OptIn(ExperimentalForeignApi::class)
actual fun readResourceText(path: String): String {
    // Split "address/worldwide.json" into directory "address" and filename "worldwide.json"
    val lastSlash = path.lastIndexOf('/')
    val directory = if (lastSlash >= 0) path.substring(0, lastSlash) else null
    val filename = if (lastSlash >= 0) path.substring(lastSlash + 1) else path

    // Split filename into name and extension
    val lastDot = filename.lastIndexOf('.')
    val name = if (lastDot >= 0) filename.substring(0, lastDot) else filename
    val ext = if (lastDot >= 0) filename.substring(lastDot + 1) else null

    val bundle = NSBundle.mainBundle
    val filePath = bundle.pathForResource(name, ext, directory)
        ?: error("Resource not found: $path")

    return NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null)
        ?: error("Failed to read resource: $path")
}
