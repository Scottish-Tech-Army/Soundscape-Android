package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

/**
 * Build the body text for "Share location". The Soundscape link is universal
 * across platforms; the secondary maps URL differs (Google Maps on Android,
 * Apple Maps on iOS), which is what [mapsName] and [mapsUrlBuilder] are for.
 *
 * The template uses positional placeholders `%1$s` (name), `%2$s` (Soundscape
 * URL) and `%3$s` (maps URL); the literal text "Google Maps" inside the
 * template is rewritten to [mapsName] for platforms that prefer a different
 * mapping app.
 */
fun buildShareLocationText(
    desc: LocationDescription,
    messageTemplate: String,
    mapsName: String,
    mapsUrlBuilder: (latitude: String, longitude: String, encodedName: String) -> String,
): String {
    val latitude = formatCoordinate5(desc.location.latitude)
    val longitude = formatCoordinate5(desc.location.longitude)
    val encodedName = urlEncodeUtf8(desc.name)
    val soundscapeUrl =
        "https://links.soundscape.scottishtecharmy.org/v1/sharemarker?" +
                "lat=$latitude&lon=$longitude&name=$encodedName"
    val mapsUrl = mapsUrlBuilder(latitude, longitude, encodedName)

    // A single regex pass over messageTemplate, rather than four chained .replace() calls: with
    // chained replace, if desc.name itself contained literal placeholder text (e.g. a marker
    // named "%2$s"), inserting it via one replace() would then get matched and rewritten again
    // by a later one - user data getting swept up as if it were template syntax. Matching once
    // against the original template and substituting from a lookup avoids that entirely, since
    // the replacement values are never re-scanned.
    val replacements = mapOf(
        "Google Maps" to mapsName,
        "%1\$s" to desc.name,
        "%2\$s" to soundscapeUrl,
        "%3\$s" to mapsUrl,
    )
    val placeholderPattern = Regex(replacements.keys.joinToString("|") { Regex.escape(it) })
    return placeholderPattern.replace(messageTemplate) { match -> replacements.getValue(match.value) }
}

/**
 * Format a double to exactly 5 decimal places, no locale-specific separators. Built from integer
 * parts because Double.toString switches to exponent form below 1e-3 ("-1.0E-4"), which a
 * coordinate near the equator or the Greenwich meridian can hit.
 */
fun formatCoordinate5(value: Double): String {
    val scaled = kotlin.math.round(kotlin.math.abs(value) * 100000.0).toLong()
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    return "$sign${scaled / 100000}.${(scaled % 100000).toString().padStart(5, '0')}"
}

/** RFC 3986 percent-encoding of UTF-8 bytes; encodes spaces as `%20`. */
internal fun urlEncodeUtf8(value: String): String {
    val bytes = value.encodeToByteArray()
    val builder = StringBuilder(bytes.size)
    for (b in bytes) {
        val c = b.toInt() and 0xFF
        val isUnreserved = (c in 0x30..0x39) || // 0-9
                (c in 0x41..0x5A) || // A-Z
                (c in 0x61..0x7A) || // a-z
                c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
        if (isUnreserved) {
            builder.append(c.toChar())
        } else {
            builder.append('%')
            builder.append(c.toString(16).uppercase().padStart(2, '0'))
        }
    }
    return builder.toString()
}
