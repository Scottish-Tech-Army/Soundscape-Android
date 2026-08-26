package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.geoengine.decimalSeparator
import org.scottishtecharmy.soundscape.geoengine.formatDecimal
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.math.roundToInt

/**
 * Format a byte count as a short human-readable string ("120 MB", "1.4 GB", etc).
 * Uses 1000-based units to match `android.text.format.Formatter.formatFileSize` so
 * iOS and Android display the same numbers.
 *
 * When `forAccessibility` is true, units are spelled out fully ("megabytes", "gigabytes")
 * so screen readers (notably iOS VoiceOver, which reads "MB" as "M B") pronounce them.
 */
fun formatBytes(
    bytes: Long,
    localized: LocalizedStrings?,
    forAccessibility: Boolean = false,
): String {
    val shortKeys = arrayOf(
        StringKey.BytesFormatKb,
        StringKey.BytesFormatMb,
        StringKey.BytesFormatGb,
        StringKey.BytesFormatTb,
    )
    val longKeys = arrayOf(
        StringKey.BytesFormatKbA11y,
        StringKey.BytesFormatMbA11y,
        StringKey.BytesFormatGbA11y,
        StringKey.BytesFormatTbA11y,
    )
    val unitKeys = if (forAccessibility) longKeys else shortKeys
    // Fallback unit suffixes used when no LocalizedStrings is supplied.
    val fallbackShort = arrayOf("kB", "MB", "GB", "TB")
    val fallbackLong = arrayOf("kilobytes", "megabytes", "gigabytes", "terabytes")
    val fallbackUnits = if (forAccessibility) fallbackLong else fallbackShort

    if (bytes < 1000) {
        val byteKey = if (forAccessibility) StringKey.BytesFormatBA11y else StringKey.BytesFormatB
        val fallbackByteUnit = if (forAccessibility) "bytes" else "B"
        return localized?.get(byteKey, bytes.toString()) ?: "$bytes $fallbackByteUnit"
    }

    var value = bytes.toDouble() / 1000.0
    var unitIndex = 0
    while (value >= 1000.0 && unitIndex < unitKeys.lastIndex) {
        value /= 1000.0
        unitIndex++
    }
    // The loop above compares the raw value, but display rounds to a whole number once
    // value >= 100 - a value like 999.5 passes the raw check (< 1000) but rounds to "1000",
    // which would show as e.g. "1000 kB" instead of correctly advancing to the next unit.
    if (value >= 100.0 && value.roundToInt() >= 1000 && unitIndex < unitKeys.lastIndex) {
        value /= 1000.0
        unitIndex++
    }
    val separator = decimalSeparator(localized, forAccessibility)
    val formatted = if (value >= 100.0) {
        value.roundToInt().toString()
    } else {
        formatDecimal(
            value,
            decimals = 1,
            separator = separator,
            spaceFractionalDigits = forAccessibility,
        )
    }
    return localized?.get(unitKeys[unitIndex], formatted)
        ?: "$formatted ${fallbackUnits[unitIndex]}"
}
