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

import kotlin.jvm.JvmInline

/**
 * An idiomatic value class wrapper for Open Location Codes (Plus Codes).
 *
 * Plus Codes are short, ~10 character codes that can be used instead of street addresses.
 * The codes can be generated and decoded offline, and use a reduced character set that
 * minimises the chance of codes including words.
 */
@JvmInline
value class PlusCode(val value: String) {

    init {
        require(OpenLocationCode.isValidCode(value.uppercase())) {
            "The provided code '$value' is not a valid Open Location Code."
        }
    }

    /**
     * Returns whether this is a full Open Location Code.
     */
    val isFull: Boolean
        get() = OpenLocationCode.isFullCode(value)

    /**
     * Returns whether this is a short Open Location Code.
     */
    val isShort: Boolean
        get() = OpenLocationCode.isShortCode(value)

    /**
     * Returns whether this is a padded Open Location Code, meaning that it contains less than 8 valid digits.
     */
    val isPadded: Boolean
        get() = OpenLocationCode.isPadded(value)

    /**
     * Decodes this Plus Code into a CodeArea encapsulating the latitude/longitude bounding box.
     * @throws IllegalStateException if the code is not a full code.
     */
    fun decode(): CodeArea = OpenLocationCode.decode(value)

    /**
     * Returns a short Plus Code from this full Plus Code created by removing digits,
     * depending on the provided reference point.
     *
     * @param referenceLatitude Degrees.
     * @param referenceLongitude Degrees.
     * @return A shortened PlusCode.
     * @throws IllegalStateException if the code is padded or not full.
     */
    fun shorten(referenceLatitude: Double, referenceLongitude: Double): PlusCode {
        return PlusCode(OpenLocationCode.shorten(value, referenceLatitude, referenceLongitude))
    }

    /**
     * Returns a full Plus Code from this short Plus Code, given the reference location.
     *
     * @param referenceLatitude Degrees.
     * @param referenceLongitude Degrees.
     * @return The nearest matching full PlusCode.
     */
    fun recover(referenceLatitude: Double, referenceLongitude: Double): PlusCode {
        return PlusCode(OpenLocationCode.recover(value, referenceLatitude, referenceLongitude))
    }

    /**
     * Returns whether the bounding box specified by the Open Location Code contains the provided point.
     *
     * @param latitude Degrees.
     * @param longitude Degrees.
     * @return True if the coordinates are contained by the code.
     */
    fun contains(latitude: Double, longitude: Double): Boolean {
        val area = decode()
        return latitude in area.southLatitude..<area.northLatitude &&
               longitude in area.westLongitude..<area.eastLongitude
    }

    override fun toString(): String = value.uppercase()
}
