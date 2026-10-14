/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Vendored from https://github.com/aughtone/aughtone-openlocationcode 0.0.1-alpha2
 * (a Kotlin port of https://github.com/google/open-location-code). Modified: package renamed.
 */

package org.scottishtecharmy.soundscape.geoengine.utils.openlocationcode

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Core Open Location Code (Plus Codes) encoding and decoding algorithms.
 */
object OpenLocationCode {

    // Provides a normal precision code, approximately 14x14 meters.
    const val CODE_PRECISION_NORMAL = 10

    // The character set used to encode the values.
    const val CODE_ALPHABET = "23456789CFGHJMPQRVWX"

    // A separator used to break the code into two parts to aid memorability.
    const val SEPARATOR = '+'

    // The character used to pad codes.
    const val PADDING_CHARACTER = '0'

    // The number of characters to place before the separator.
    private const val SEPARATOR_POSITION = 8

    // The minimum number of digits in a Plus Code.
    const val MIN_DIGIT_COUNT = 2

    // The max number of digits to process in a Plus Code.
    const val MAX_DIGIT_COUNT = 15

    // Maximum code length using just lat/lng pair encoding.
    private const val PAIR_CODE_LENGTH = 10

    // Number of digits in the grid coding section.
    private const val GRID_CODE_LENGTH = MAX_DIGIT_COUNT - PAIR_CODE_LENGTH

    // The base to use to convert numbers to/from.
    private val ENCODING_BASE = CODE_ALPHABET.length

    // The maximum value for latitude in degrees.
    private const val LATITUDE_MAX = 90L

    // The maximum value for longitude in degrees.
    private const val LONGITUDE_MAX = 180L

    // Number of columns in the grid refinement method.
    private const val GRID_COLUMNS = 4

    // Number of rows in the grid refinement method.
    private const val GRID_ROWS = 5

    // Value to multiple latitude degrees to convert it to an integer with the maximum encoding
    // precision. I.e. ENCODING_BASE**3 * GRID_ROWS**GRID_CODE_LENGTH
    private const val LAT_INTEGER_MULTIPLIER = 8000L * 3125L

    // Value to multiple longitude degrees to convert it to an integer with the maximum encoding
    // precision. I.e. ENCODING_BASE**3 * GRID_COLUMNS**GRID_CODE_LENGTH
    private const val LNG_INTEGER_MULTIPLIER = 8000L * 1024L

    // Value of the most significant latitude digit after it has been converted to an integer.
    private val LAT_MSP_VALUE = LAT_INTEGER_MULTIPLIER * ENCODING_BASE * ENCODING_BASE

    // Value of the most significant longitude digit after it has been converted to an integer.
    private val LNG_MSP_VALUE = LNG_INTEGER_MULTIPLIER * ENCODING_BASE * ENCODING_BASE

    /**
     * Encodes latitude/longitude into a 10-digit Open Location Code.
     *
     * @param latitude The latitude in decimal degrees.
     * @param longitude The longitude in decimal degrees.
     * @return The code.
     */
    fun encode(latitude: Double, longitude: Double): String {
        return encode(latitude, longitude, CODE_PRECISION_NORMAL)
    }

    /**
     * Encodes latitude/longitude into an Open Location Code of the provided length.
     *
     * @param latitude The latitude in decimal degrees.
     * @param longitude The longitude in decimal degrees.
     * @param codeLength The number of digits in the returned code.
     * @return The code.
     */
    fun encode(latitude: Double, longitude: Double, codeLength: Int): String {
        val integers = degreesToIntegers(latitude, longitude)
        return encodeIntegers(integers[0], integers[1], codeLength)
    }

    internal fun encodeIntegers(latOriginal: Long, lngOriginal: Long, codeLengthOriginal: Int): String {
        var lat = latOriginal
        var lng = lngOriginal
        var codeLength = codeLengthOriginal
        // Limit the maximum number of digits in the code.
        codeLength = min(codeLength, MAX_DIGIT_COUNT)
        // Check that the code length requested is valid.
        require(!(codeLength < PAIR_CODE_LENGTH && codeLength % 2 == 1 || codeLength < MIN_DIGIT_COUNT)) {
            "Illegal code length $codeLength"
        }

        // Store the code - we build it in reverse and reorder it afterwards.
        val revCodeBuilder = StringBuilder()
        // Compute the grid part of the code if necessary.
        if (codeLength > PAIR_CODE_LENGTH) {
            for (i in 0 until GRID_CODE_LENGTH) {
                val latDigit = lat % GRID_ROWS
                val lngDigit = lng % GRID_COLUMNS
                val ndx = (latDigit * GRID_COLUMNS + lngDigit).toInt()
                revCodeBuilder.append(CODE_ALPHABET[ndx])
                lat /= GRID_ROWS
                lng /= GRID_COLUMNS
            }
        } else {
            // Note: The reference Java implementation uses Math.pow() here which yields double precision.
            // When translating `lat / Math.pow(GRID_ROWS, GRID_CODE_LENGTH)` to Kotlin KMP, floating point
            // differences could occasionally yield values like 2477599.999999999 which truncate incorrectly toLong().
            // We substitute `Math.pow` with explicit integer arithmetic to guarantee 100% precision.
            var latDivisor = 1L
            var lngDivisor = 1L
            for (i in 0 until GRID_CODE_LENGTH) {
                latDivisor *= GRID_ROWS
                lngDivisor *= GRID_COLUMNS
            }
            lat /= latDivisor
            lng /= lngDivisor
        }
        // Compute the pair section of the code.
        for (i in 0 until PAIR_CODE_LENGTH / 2) {
            revCodeBuilder.append(CODE_ALPHABET[(lng % ENCODING_BASE).toInt()])
            revCodeBuilder.append(CODE_ALPHABET[(lat % ENCODING_BASE).toInt()])
            lat /= ENCODING_BASE
            lng /= ENCODING_BASE
            // If we are at the separator position, add the separator.
            if (i == 0) {
                revCodeBuilder.append(SEPARATOR)
            }
        }
        // Reverse the code.
        val codeBuilder = revCodeBuilder.reverse()

        // If we need to pad the code, replace some of the digits.
        if (codeLength < SEPARATOR_POSITION) {
            for (i in codeLength until SEPARATOR_POSITION) {
                codeBuilder[i] = PADDING_CHARACTER
            }
        }
        return codeBuilder.substring(0, max(SEPARATOR_POSITION + 1, codeLength + 1))
    }

    /**
     * Decodes an Open Location Code into a CodeArea object encapsulating
     * latitude/longitude bounding box.
     *
     * @param code Open Location Code to be decoded.
     * @return A CodeArea object.
     * @throws IllegalArgumentException if the provided code is not a valid Open Location Code.
     */
    fun decode(code: String): CodeArea {
        val uppercaseCode = code.uppercase()
        require(isValidCode(uppercaseCode)) {
            "The provided code '$code' is not a valid Open Location Code."
        }
        check(isFullCode(uppercaseCode)) {
            "Method decode() could only be called on valid full codes, code was $code."
        }
        // Strip padding and separator characters out of the code.
        val clean = uppercaseCode.replace(SEPARATOR.toString(), "").replace(PADDING_CHARACTER.toString(), "")

        // Initialise the values. We work them out as integers and convert them to doubles at the end.
        var latVal = -LATITUDE_MAX * LAT_INTEGER_MULTIPLIER
        var lngVal = -LONGITUDE_MAX * LNG_INTEGER_MULTIPLIER
        // Define the place value for the digits. We'll divide this down as we work through the code.
        var latPlaceVal = LAT_MSP_VALUE
        var lngPlaceVal = LNG_MSP_VALUE
        
        var i = 0
        val limit = min(clean.length, PAIR_CODE_LENGTH)
        while (i < limit) {
            latPlaceVal /= ENCODING_BASE
            lngPlaceVal /= ENCODING_BASE
            latVal += CODE_ALPHABET.indexOf(clean[i]) * latPlaceVal
            lngVal += CODE_ALPHABET.indexOf(clean[i + 1]) * lngPlaceVal
            i += 2
        }
        
        for (j in PAIR_CODE_LENGTH until min(clean.length, MAX_DIGIT_COUNT)) {
            latPlaceVal /= GRID_ROWS
            lngPlaceVal /= GRID_COLUMNS
            val digit = CODE_ALPHABET.indexOf(clean[j])
            val row = digit / GRID_COLUMNS
            val col = digit % GRID_COLUMNS
            latVal += row * latPlaceVal
            lngVal += col * lngPlaceVal
        }
        
        val latitudeLo = latVal.toDouble() / LAT_INTEGER_MULTIPLIER
        val longitudeLo = lngVal.toDouble() / LNG_INTEGER_MULTIPLIER
        val latitudeHi = (latVal + latPlaceVal).toDouble() / LAT_INTEGER_MULTIPLIER
        val longitudeHi = (lngVal + lngPlaceVal).toDouble() / LNG_INTEGER_MULTIPLIER
        
        return CodeArea(
            latitudeLo,
            longitudeLo,
            latitudeHi,
            longitudeHi,
            min(clean.length, MAX_DIGIT_COUNT)
        )
    }

    /**
     * Returns whether the provided string is a valid Open Location code.
     *
     * @param code The code to check.
     * @return True if it is a valid full or short code.
     */
    fun isValidCode(code: String?): Boolean {
        if (code == null || code.length < 2) {
            return false
        }
        val uppercaseCode = code.uppercase()

        // There must be exactly one separator.
        val separatorPosition = uppercaseCode.indexOf(SEPARATOR)
        if (separatorPosition == -1) {
            return false
        }
        if (separatorPosition != uppercaseCode.lastIndexOf(SEPARATOR)) {
            return false
        }
        // There must be an even number of at most 8 characters before the separator.
        if (separatorPosition % 2 != 0 || separatorPosition > SEPARATOR_POSITION) {
            return false
        }

        // Check first two characters: only some values from the alphabet are permitted.
        if (separatorPosition == SEPARATOR_POSITION) {
            // First latitude character can only have first 9 values.
            if (CODE_ALPHABET.indexOf(uppercaseCode[0]) > 8) {
                return false
            }

            // First longitude character can only have first 18 values.
            if (CODE_ALPHABET.indexOf(uppercaseCode[1]) > 17) {
                return false
            }
        }

        // Check the characters before the separator.
        var paddingStarted = false
        for (i in 0 until separatorPosition) {
            if (CODE_ALPHABET.indexOf(uppercaseCode[i]) == -1 && uppercaseCode[i] != PADDING_CHARACTER) {
                // Invalid character.
                return false
            }
            if (paddingStarted) {
                // Once padding starts, there must not be anything but padding.
                if (uppercaseCode[i] != PADDING_CHARACTER) {
                    return false
                }
            } else if (uppercaseCode[i] == PADDING_CHARACTER) {
                paddingStarted = true
                // Short codes cannot have padding
                if (separatorPosition < SEPARATOR_POSITION) {
                    return false
                }
                // Padding can start on even character: 2, 4 or 6.
                if (i != 2 && i != 4 && i != 6) {
                    return false
                }
            }
        }

        // Check the characters after the separator.
        if (uppercaseCode.length > separatorPosition + 1) {
            if (paddingStarted) {
                return false
            }
            // Only one character after separator is forbidden.
            if (uppercaseCode.length == separatorPosition + 2) {
                return false
            }
            for (i in separatorPosition + 1 until uppercaseCode.length) {
                if (CODE_ALPHABET.indexOf(uppercaseCode[i]) == -1) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Returns whether the provided Open Location Code is a full Open Location Code.
     */
    fun isFullCode(code: String): Boolean {
        if (!isValidCode(code)) return false
        val uppercaseCode = code.uppercase()
        return uppercaseCode.indexOf(SEPARATOR) == SEPARATOR_POSITION
    }

    /**
     * Returns whether the provided Open Location Code is a short Open Location Code.
     */
    fun isShortCode(code: String): Boolean {
        if (!isValidCode(code)) return false
        val uppercaseCode = code.uppercase()
        val index = uppercaseCode.indexOf(SEPARATOR)
        return index in 0..<SEPARATOR_POSITION
    }

    /**
     * Returns whether the provided Open Location Code is a padded Open Location Code, meaning that it
     * contains less than 8 valid digits.
     */
    fun isPadded(code: String): Boolean {
        if (!isValidCode(code)) return false
        return code.indexOf(PADDING_CHARACTER) >= 0
    }

    /**
     * Returns short Open Location Code from the full Open Location Code created by removing
     * four or six digits, depending on the provided reference point.
     *
     * @param code The full code to shorten.
     * @param referenceLatitude Degrees.
     * @param referenceLongitude Degrees.
     * @return A short code if possible.
     */
    fun shorten(code: String, referenceLatitude: Double, referenceLongitude: Double): String {
        val uppercaseCode = code.uppercase()
        require(isValidCode(uppercaseCode)) { "Not a valid code: $code" }
        check(isFullCode(uppercaseCode)) { "shorten() method could only be called on a full code." }
        check(!isPadded(uppercaseCode)) { "shorten() method can not be called on a padded code." }

        val codeArea = decode(uppercaseCode)
        val range = max(
            abs(referenceLatitude - codeArea.centerLatitude),
            abs(referenceLongitude - codeArea.centerLongitude)
        )
        // We are going to check to see if we can remove three pairs, two pairs or just one pair of
        // digits from the code.
        for (i in 4 downTo 1) {
            // Check if we're close enough to shorten. The range must be less than 1/2
            // the precision to shorten at all, and we want to allow some safety, so
            // use 0.3 instead of 0.5 as a multiplier.
            if (range < computeLatitudePrecision(i * 2) * 0.3) {
                // We're done.
                return uppercaseCode.substring(i * 2)
            }
        }
        throw IllegalArgumentException("Reference location is too far from the Open Location Code center.")
    }

    /**
     * Returns a full Open Location Code from a short Open Location Code, given the reference location.
     *
     * @param code The short code to recover.
     * @param referenceLat Degrees.
     * @param referenceLng Degrees.
     * @return The nearest matching full code.
     */
    fun recover(code: String, referenceLat: Double, referenceLng: Double): String {
        val uppercaseCode = code.uppercase()
        require(isValidCode(uppercaseCode)) { "Not a valid code: $code" }
        if (isFullCode(uppercaseCode)) {
            return uppercaseCode
        }
        val referenceLatitude = clipLatitude(referenceLat)
        val referenceLongitude = normalizeLongitude(referenceLng)

        val digitsToRecover = SEPARATOR_POSITION - uppercaseCode.indexOf(SEPARATOR)
        // The precision (height and width) of the missing prefix in degrees.
        val prefixPrecision = ENCODING_BASE.toDouble().pow(2 - digitsToRecover / 2)

        // Use the reference location to generate the prefix.
        val recoveredPrefix = encode(referenceLatitude, referenceLongitude).substring(0, digitsToRecover)
        
        // Combine the prefix with the short code and decode it.
        val recovered = recoveredPrefix + uppercaseCode
        val recoveredCodeArea = decode(recovered)
        
        // Work out whether the new code area is too far from the reference location. If it is, we
        // move it. It can only be out by a single precision step.
        var recoveredLatitude = recoveredCodeArea.centerLatitude
        var recoveredLongitude = recoveredCodeArea.centerLongitude

        // Move the recovered latitude by one precision up or down if it is too far from the reference,
        // unless doing so would lead to an invalid latitude.
        val latitudeDiff = recoveredLatitude - referenceLatitude
        if (latitudeDiff > prefixPrecision / 2 && recoveredLatitude - prefixPrecision > -LATITUDE_MAX) {
            recoveredLatitude -= prefixPrecision
        } else if (latitudeDiff < -prefixPrecision / 2 && recoveredLatitude + prefixPrecision < LATITUDE_MAX) {
            recoveredLatitude += prefixPrecision
        }

        // Move the recovered longitude by one precision up or down if it is too far from the
        // reference.
        val longitudeDiff = recoveredCodeArea.centerLongitude - referenceLongitude
        if (longitudeDiff > prefixPrecision / 2) {
            recoveredLongitude -= prefixPrecision
        } else if (longitudeDiff < -prefixPrecision / 2) {
            recoveredLongitude += prefixPrecision
        }

        return encode(recoveredLatitude, recoveredLongitude, recovered.length - 1)
    }

    // Private static methods.

    /**
     * Convert latitude and longitude in degrees into the integer values needed for reliable encoding.
     * (To avoid floating point precision errors.)
     */
    private fun degreesToIntegers(latitude: Double, longitude: Double): LongArray {
        var lat = floor(latitude * LAT_INTEGER_MULTIPLIER).toLong()
        var lng = floor(longitude * LNG_INTEGER_MULTIPLIER).toLong()

        // Clip and normalise values.
        lat += LATITUDE_MAX * LAT_INTEGER_MULTIPLIER
        if (lat < 0) {
            lat = 0
        } else if (lat >= 2 * LATITUDE_MAX * LAT_INTEGER_MULTIPLIER) {
            lat = 2 * LATITUDE_MAX * LAT_INTEGER_MULTIPLIER - 1
        }
        
        lng += LONGITUDE_MAX * LNG_INTEGER_MULTIPLIER
        if (lng < 0) {
            lng = lng % (2 * LONGITUDE_MAX * LNG_INTEGER_MULTIPLIER) + 2 * LONGITUDE_MAX * LNG_INTEGER_MULTIPLIER
        } else if (lng >= 2 * LONGITUDE_MAX * LNG_INTEGER_MULTIPLIER) {
            lng %= 2 * LONGITUDE_MAX * LNG_INTEGER_MULTIPLIER
        }
        return longArrayOf(lat, lng)
    }

    private fun clipLatitude(latitude: Double): Double {
        return min(max(latitude, -LATITUDE_MAX.toDouble()), LATITUDE_MAX.toDouble())
    }

    private fun normalizeLongitude(longitude: Double): Double {
        if (longitude >= -LONGITUDE_MAX && longitude < LONGITUDE_MAX) {
            // longitude is within proper range, no normalization necessary
            return longitude
        }

        // % in Java/Kotlin uses truncated division with the remainder having the same sign as
        // the dividend. For any input longitude < -360, the result of longitude%CIRCLE_DEG
        // will still be negative but > -360, so we need to add 360 and apply % a second time.
        val circleDeg = 2 * LONGITUDE_MAX // 360 degrees
        return (longitude % circleDeg + circleDeg + LONGITUDE_MAX) % circleDeg - LONGITUDE_MAX
    }

    /**
     * Compute the latitude precision value for a given code length. Lengths <= 10 have the same
     * precision for latitude and longitude, but lengths > 10 have different precisions due to the
     * grid method having fewer columns than rows. Copied from the JS implementation.
     */
    private fun computeLatitudePrecision(codeLength: Int): Double {
        if (codeLength <= CODE_PRECISION_NORMAL) {
            return ENCODING_BASE.toDouble().pow(codeLength / -2 + 2)
        }
        return ENCODING_BASE.toDouble().pow(-3.0) / GRID_ROWS.toDouble().pow(codeLength - PAIR_CODE_LENGTH)
    }
}
