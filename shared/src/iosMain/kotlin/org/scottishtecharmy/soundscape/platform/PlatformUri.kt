package org.scottishtecharmy.soundscape.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSCharacterSet
import platform.Foundation.URLHostAllowedCharacterSet
import platform.Foundation.URLPathAllowedCharacterSet
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.Foundation.stringByRemovingPercentEncoding
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual fun urlEncode(s: String): String {
    // For general purpose encoding similar to Uri.encode, we use a broad set of allowed characters
    // or just use URLQueryAllowedCharacterSet.
    return (s as NSString).stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet) ?: s
}

actual fun urlDecode(s: String): String {
    return (s as NSString).stringByRemovingPercentEncoding() ?: s
}
