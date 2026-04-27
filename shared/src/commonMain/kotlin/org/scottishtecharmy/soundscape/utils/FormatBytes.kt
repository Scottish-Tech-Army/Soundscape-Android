package org.scottishtecharmy.soundscape.utils

import kotlin.math.roundToInt

/**
 * Format a byte count as a short human-readable string ("120 MB", "1.4 GB", etc).
 * Uses 1000-based units to match `android.text.format.Formatter.formatFileSize` so
 * iOS and Android display the same numbers.
 */
fun formatBytes(bytes: Long): String {
    if (bytes < 1000) return "$bytes B"
    val units = arrayOf("kB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1000.0
    var unitIndex = 0
    while (value >= 1000.0 && unitIndex < units.lastIndex) {
        value /= 1000.0
        unitIndex++
    }
    return if (value >= 100.0) {
        "${value.roundToInt()} ${units[unitIndex]}"
    } else {
        val rounded = (value * 10.0).roundToInt() / 10.0
        "$rounded ${units[unitIndex]}"
    }
}
