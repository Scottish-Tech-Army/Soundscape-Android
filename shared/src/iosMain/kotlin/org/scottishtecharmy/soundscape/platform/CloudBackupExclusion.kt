package org.scottishtecharmy.soundscape.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.numberWithBool

@OptIn(ExperimentalForeignApi::class)
internal actual fun excludeFromCloudBackup(path: String) {
    // setResourceValue writes an extended attribute on the file, so it fails on a path that
    // doesn't exist rather than recording anything for later.
    if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return
    NSURL.fileURLWithPath(path).setResourceValue(
        value = NSNumber.numberWithBool(true),
        forKey = NSURLIsExcludedFromBackupKey,
        error = null,
    )
}
