package org.scottishtecharmy.soundscape.platform

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val gpxTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

actual fun formatGpxTimestamp(epochMillis: Long): String =
    gpxTimeFormat.format(Date(epochMillis))

actual fun parseGpxTimestamp(time: String?): Long? {
    val text = time?.trim().orEmpty()
    if (text.isEmpty()) return null

    text.toLongOrNull()?.let { return it }

    return try {
        Instant.parse(text).toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}
