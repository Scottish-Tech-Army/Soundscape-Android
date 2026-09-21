package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCompatibilityMapping

actual fun normalizeUnicode(input: String): String {
    @Suppress("CAST_NEVER_SUCCEEDS")
    return (input as NSString).decomposedStringWithCompatibilityMapping
}
