package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.geoengine.decimalSeparator
import org.scottishtecharmy.soundscape.geoengine.formatDecimal
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.PluralKey
import kotlin.math.abs
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
        PluralKey.BytesFormatKb,
        PluralKey.BytesFormatMb,
        PluralKey.BytesFormatGb,
        PluralKey.BytesFormatTb,
    )
    val longKeys = arrayOf(
        PluralKey.BytesFormatKbA11y,
        PluralKey.BytesFormatMbA11y,
        PluralKey.BytesFormatGbA11y,
        PluralKey.BytesFormatTbA11y,
    )
    val unitKeys = if (forAccessibility) longKeys else shortKeys
    // Fallback unit suffixes used when no LocalizedStrings is supplied. These units never
    // change shape in English, so the fallback below only needs to pick the byte-count
    // singular ("byte") from the plural ("bytes"), not these.
    val fallbackShort = arrayOf("kB", "MB", "GB", "TB")
    val fallbackLong = arrayOf("kilobytes", "megabytes", "gigabytes", "terabytes")
    val fallbackUnits = if (forAccessibility) fallbackLong else fallbackShort

    if (bytes < 1000) {
        val byteKey = if (forAccessibility) PluralKey.BytesFormatBA11y else PluralKey.BytesFormatB
        val fallbackByteUnit = when {
            !forAccessibility -> "B"
            abs(bytes) == 1L -> "byte"
            else -> "bytes"
        }
        return localized?.getPlural(byteKey, abs(bytes).toInt(), bytes.toString())
            ?: "$bytes $fallbackByteUnit"
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
    val formatted: String
    val quantity: Int
    if (value >= 100.0) {
        // A whole number once rounded ("100 kB"), so it can select a real plural category.
        val rounded = value.roundToInt()
        formatted = rounded.toString()
        quantity = rounded
    } else {
        // Always shown with a decimal digit ("1.0 kB"), so - like "1.4 km" - the fraction
        // never reaches CLDR's `v` operand and which category it lands in is a per-language
        // question. See LocalizedStrings.fractionalPluralQuantity.
        formatted = formatDecimal(
            value,
            decimals = 1,
            separator = separator,
            spaceFractionalDigits = forAccessibility,
        )
        quantity = localized?.fractionalPluralQuantity ?: 2
    }
    return localized?.getPlural(unitKeys[unitIndex], quantity, formatted)
        ?: "$formatted ${fallbackUnits[unitIndex]}"
}
