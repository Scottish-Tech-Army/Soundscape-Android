package org.scottishtecharmy.soundscape.geoengine.utils

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

private const val DEG_TO_RAD = PI / 180.0
private const val RAD_TO_DEG = 180.0 / PI

fun Double.normalizeDegrees(): Double {
    val mod = this % 360.0
    return if (mod < 0.0) mod + 360.0 else mod
}

fun circularDifferenceDegrees(a: Double, b: Double): Double {
    val diff = ((a - b) % 360.0 + 540.0) % 360.0 - 180.0
    return diff
}

fun List<Double>.circularMeanDegrees(): Double? {
    if (isEmpty()) return null
    var sumSin = 0.0
    var sumCos = 0.0
    for (deg in this) {
        val rad = deg * DEG_TO_RAD
        sumSin += sin(rad)
        sumCos += cos(rad)
    }
    if (sumSin == 0.0 && sumCos == 0.0) return null
    return (atan2(sumSin, sumCos) * RAD_TO_DEG).normalizeDegrees()
}

fun List<Double>.circularStdDevDegrees(): Double? {
    if (isEmpty()) return null
    var sumSin = 0.0
    var sumCos = 0.0
    for (deg in this) {
        val rad = deg * DEG_TO_RAD
        sumSin += sin(rad)
        sumCos += cos(rad)
    }
    val n = size.toDouble()
    val r = sqrt(sumSin * sumSin + sumCos * sumCos) / n
    if (r <= 0.0 || r > 1.0) return null
    val variance = -2.0 * ln(r)
    return sqrt(variance) * RAD_TO_DEG
}
