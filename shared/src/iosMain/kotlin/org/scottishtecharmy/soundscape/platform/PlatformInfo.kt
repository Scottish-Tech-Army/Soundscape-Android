package org.scottishtecharmy.soundscape.platform

import platform.Foundation.NSBundle

actual fun appVersionName(): String {
    val bundle = NSBundle.mainBundle
    return bundle.objectForInfoDictionaryKey("AppVersionName") as? String
        ?: bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        ?: "0.0.0"
}

actual val analyticsEnabled: Boolean = true

actual val isIos: Boolean = true
