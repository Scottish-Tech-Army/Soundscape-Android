package org.scottishtecharmy.soundscape.platform

import platform.Foundation.NSCaseInsensitiveSearch
import platform.Foundation.NSLocale
import platform.Foundation.NSMakeRange
import platform.Foundation.NSString
import platform.Foundation.compare

actual fun nameCollator(languageTag: String): Comparator<String> {
    // Comparing with a locale is what makes NSString collate the way that language does, as
    // Android's Collator does; without one it compares the characters. Case insensitive but not
    // diacritic insensitive, so that it matches Android's Collator.SECONDARY.
    val locale = NSLocale(localeIdentifier = languageTag)
    return Comparator { a, b ->
        @Suppress("CAST_NEVER_SUCCEEDS")
        val string = a as NSString
        string.compare(b, NSCaseInsensitiveSearch, NSMakeRange(0uL, string.length), locale).toInt()
    }
}
