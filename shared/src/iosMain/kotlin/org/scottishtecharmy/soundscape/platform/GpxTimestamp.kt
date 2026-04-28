package org.scottishtecharmy.soundscape.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneWithAbbreviation

@OptIn(ExperimentalForeignApi::class)
private val gpxTimeFormatter = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    locale = NSLocale("en_US_POSIX")
    timeZone = NSTimeZone.timeZoneWithAbbreviation("UTC")!!
}

actual fun formatGpxTimestamp(epochMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    return gpxTimeFormatter.stringFromDate(date)
}
