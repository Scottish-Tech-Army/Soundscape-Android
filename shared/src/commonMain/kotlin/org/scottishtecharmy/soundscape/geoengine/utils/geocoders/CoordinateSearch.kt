package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geoengine.utils.openlocationcode.OpenLocationCode
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.intents.percentDecode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

/**
 * The locations that search text describes when it is a coordinate rather than a place name,
 * nearest to [reference] first. Empty when it isn't a coordinate, and the search is then for a
 * name as usual.
 *
 * Understood are:
 *  - map links - `geo:55.9486,-4.3148`, and Google, Apple and OpenStreetMap URLs
 *  - plus codes - `9C7QWMXQ+2V`, or `WMXQ+2V` which is completed near [reference]
 *  - degrees, minutes and seconds with a hemisphere - `55°56'55.0"N 4°18'53.3"W`, and the
 *    degrees and decimal minutes of GPS units - `N55 56.917 W4 18.888`
 *  - a pair of decimal numbers - `55.9486, -4.3148`, or `55,9486 -4,3148` where a comma is the
 *    decimal separator. Nothing says which of these is the latitude, so both ways round are
 *    returned where both are valid.
 */
fun parseCoordinateSearch(text: String, reference: LngLatAlt): List<LngLatAlt> {
    val trimmed = text.trim()
    val candidates = parseLink(trimmed)
        ?: parsePlusCode(trimmed, reference)
        ?: parseHemispheres(trimmed)
        ?: parseNumberPair(trimmed)
        ?: return emptyList()
    return candidates.sortedBy {
        distance(reference.latitude, reference.longitude, it.latitude, it.longitude)
    }
}

/**
 * The coordinate as it's shown in search results, latitude first as most maps show it -
 * "55.9486, -4.3148".
 */
fun formatCoordinate(location: LngLatAlt): String =
    "${formatDegrees(location.latitude)}, ${formatDegrees(location.longitude)}"

private fun formatDegrees(value: Double): String {
    val micro = round(value * 1_000_000).toLong()
    val sign = if (micro < 0) "-" else ""
    val whole = abs(micro) / 1_000_000
    val fraction = (abs(micro) % 1_000_000).toString().padStart(6, '0').trimEnd('0')
    return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole.$fraction"
}

private fun validLocation(latitude: Double, longitude: Double): LngLatAlt? =
    if (abs(latitude) <= 90.0 && abs(longitude) <= 180.0) LngLatAlt(longitude, latitude) else null

private fun String.toDegrees(): Double? = replace(',', '.').toDoubleOrNull()

private const val SIGNED_NUMBER = """[+-]?\d+(?:\.\d+)?"""

// Every one of these puts the latitude first. A marked place comes before the centre of the
// map's view, as a link can have both.
private val linkPatterns = listOf(
    Regex("""^geo:(?://)?($SIGNED_NUMBER),\s*($SIGNED_NUMBER)""", RegexOption.IGNORE_CASE),
    Regex("""[?&](?:q|query|ll|sll|daddr)=(?:loc:)?($SIGNED_NUMBER),[\s+]*($SIGNED_NUMBER)"""),
    Regex("""[?&]mlat=($SIGNED_NUMBER)&mlon=($SIGNED_NUMBER)"""),
    Regex("""[?&]center=($SIGNED_NUMBER),[\s+]*($SIGNED_NUMBER)"""),
    Regex("""@($SIGNED_NUMBER),($SIGNED_NUMBER)"""),
    Regex("""#map=\d+(?:\.\d+)?/($SIGNED_NUMBER)/($SIGNED_NUMBER)"""),
)

private val linkPrefix = Regex("""^(?:geo:|https?://)""", RegexOption.IGNORE_CASE)

/** Null when [text] isn't a link, and empty when it's a link with no coordinate in it */
private fun parseLink(text: String): List<LngLatAlt>? {
    if (!linkPrefix.containsMatchIn(text)) return null
    // Decoding turns a '+' into a space, which would lose the sign of "+55.9"
    val decoded = percentDecode(text.replace("+", "%2B"))
    for (pattern in linkPatterns) {
        val match = pattern.find(decoded) ?: continue
        val latitude = match.groupValues[1].toDoubleOrNull() ?: continue
        val longitude = match.groupValues[2].toDoubleOrNull() ?: continue
        return listOfNotNull(validLocation(latitude, longitude))
    }
    return emptyList()
}

// A plus code, which may be followed by a locality - "WMXQ+2V Glasgow". The locality is ignored,
// so a short code is always completed near the user.
private val plusCodePattern =
    Regex("""^([23456789CFGHJMPQRVWX0]{2,8}\+[23456789CFGHJMPQRVWX]*)(?:[\s,].*)?$""", RegexOption.IGNORE_CASE)

private fun parsePlusCode(text: String, reference: LngLatAlt): List<LngLatAlt>? {
    val code = plusCodePattern.find(text)?.groupValues?.get(1)?.uppercase() ?: return null
    if (!OpenLocationCode.isValidCode(code)) return null
    val fullCode = when {
        OpenLocationCode.isFullCode(code) -> code
        // A short code is missing at least its first four characters, and has at least two after
        // the '+', which keeps "22+" and the like out
        OpenLocationCode.isShortCode(code) &&
            code.indexOf('+') >= 4 && code.length - code.indexOf('+') > 2 ->
            OpenLocationCode.recover(code, reference.latitude, reference.longitude)
        else -> return null
    }
    val area = OpenLocationCode.decode(fullCode)
    return listOfNotNull(validLocation(area.centerLatitude, area.centerLongitude))
}

private const val UNSIGNED_NUMBER = """\d+(?:[.,]\d+)?"""

// Degrees, then optionally minutes and seconds. The unit symbols can be left out, as GPS units
// often do - "55 56.917".
private const val ANGLE =
    """($UNSIGNED_NUMBER)\s*°?\s*(?:($UNSIGNED_NUMBER)\s*'?\s*)?(?:($UNSIGNED_NUMBER)\s*"?)?"""

// The hemisphere letters either all come before the numbers or all after them
private val hemispheresBefore =
    Regex("""^([NSEW])\s*$ANGLE\s*[,;]?\s*([NSEW])\s*$ANGLE$""", RegexOption.IGNORE_CASE)
private val hemispheresAfter =
    Regex("""^$ANGLE\s*([NSEW])\s*[,;]?\s*$ANGLE\s*([NSEW])$""", RegexOption.IGNORE_CASE)

private fun normalizeSymbols(text: String) = text
    .replace('º', '°')
    .replace(Regex("[′’‘´`]"), "'")
    .replace(Regex("[″”“]|''"), "\"")

private fun parseHemispheres(text: String): List<LngLatAlt>? {
    val normalized = normalizeSymbols(text)
    val components = hemispheresBefore.find(normalized)?.groupValues?.let { groups ->
        listOf(
            groups[1] to groups.subList(2, 5),
            groups[5] to groups.subList(6, 9),
        )
    } ?: hemispheresAfter.find(normalized)?.groupValues?.let { groups ->
        listOf(
            groups[4] to groups.subList(1, 4),
            groups[8] to groups.subList(5, 8),
        )
    } ?: return null

    var latitude: Double? = null
    var longitude: Double? = null
    for ((hemisphere, angle) in components) {
        val degrees = angleToDegrees(angle) ?: return null
        when (hemisphere.uppercase()) {
            "N" -> if (latitude == null) latitude = degrees else return null
            "S" -> if (latitude == null) latitude = -degrees else return null
            "E" -> if (longitude == null) longitude = degrees else return null
            "W" -> if (longitude == null) longitude = -degrees else return null
        }
    }
    return listOfNotNull(validLocation(latitude ?: return null, longitude ?: return null))
}

/** Degrees from [parts] of degrees, minutes and seconds, the last two of which may be empty */
private fun angleToDegrees(parts: List<String>): Double? {
    val (degreesText, minutesText, secondsText) = parts
    val degrees = degreesText.toDegrees() ?: return null
    if (minutesText.isEmpty()) return if (secondsText.isEmpty()) degrees else null
    // Only the last of the parts can have a fraction - "55.5° 30'" makes no sense
    if (degrees != floor(degrees)) return null
    val minutes = minutesText.toDegrees() ?: return null
    if (minutes >= 60.0) return null
    if (secondsText.isEmpty()) return degrees + minutes / 60.0
    if (minutes != floor(minutes)) return null
    val seconds = secondsText.toDegrees() ?: return null
    if (seconds >= 60.0) return null
    return degrees + minutes / 60.0 + seconds / 3600.0
}

// Both numbers need a decimal part, so that "12 34" or a house number isn't taken for a
// coordinate. Where the decimal separator is a comma, the numbers can't also be separated by
// just a comma - "55,9,4,3" could be read more than one way.
private val decimalPointPair =
    Regex("""^([+-]?\d+\.\d+)°?\s*[,;\s]\s*([+-]?\d+\.\d+)°?$""")
private val decimalCommaPair =
    Regex("""^([+-]?\d+,\d+)°?(?:\s*;\s*|\s+)([+-]?\d+,\d+)°?$""")

private fun parseNumberPair(text: String): List<LngLatAlt>? {
    val match = decimalPointPair.find(text) ?: decimalCommaPair.find(text) ?: return null
    val first = match.groupValues[1].toDegrees() ?: return null
    val second = match.groupValues[2].toDegrees() ?: return null
    return listOfNotNull(
        validLocation(first, second),
        validLocation(second, first),
    ).distinct()
}
