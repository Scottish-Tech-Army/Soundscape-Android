package org.scottishtecharmy.soundscape.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneWithAbbreviation

@OptIn(ExperimentalForeignApi::class)
private val gpxTimeFormatter = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    locale = NSLocale("en_US_POSIX")
    timeZone = NSTimeZone.timeZoneWithAbbreviation("UTC")
        ?: error("UTC time zone abbreviation not recognized by the platform")
}

actual fun formatGpxTimestamp(epochMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    return gpxTimeFormatter.stringFromDate(date)
}

/**
 * The same instant without the fractional seconds, which exports from other apps write and
 * [gpxTimeFormatter] rejects outright rather than defaulting the missing field.
 */
@OptIn(ExperimentalForeignApi::class)
private val gpxTimeFormatterNoMillis = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    locale = NSLocale("en_US_POSIX")
    timeZone = NSTimeZone.timeZoneWithAbbreviation("UTC")
        ?: error("UTC time zone abbreviation not recognized by the platform")
}

actual fun parseGpxTimestamp(time: String?): Long? {
    val text = time?.trim().orEmpty()
    if (text.isEmpty()) return null

    text.toLongOrNull()?.let { return it }

    val date = gpxTimeFormatter.dateFromString(text)
        ?: gpxTimeFormatterNoMillis.dateFromString(text)
        ?: return null
    return (date.timeIntervalSince1970 * 1000).toLong()
}
