package org.scottishtecharmy.soundscape.platform

import platform.Foundation.NSBundle

actual fun appVersionName(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "0.0.0"

actual val analyticsEnabled: Boolean = true
