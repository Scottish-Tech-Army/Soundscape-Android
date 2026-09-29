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

/**
 * Coordinates of a decoded Open Location Code.
 *
 * The coordinates include the latitude and longitude of the lower left and upper right corners
 * and the center of the bounding box for the area the code represents.
 */
data class CodeArea(
    val southLatitude: Double,
    val westLongitude: Double,
    val northLatitude: Double,
    val eastLongitude: Double,
    val length: Int
) {
    val latitudeHeight: Double
        get() = northLatitude - southLatitude

    val longitudeWidth: Double
        get() = eastLongitude - westLongitude

    val centerLatitude: Double
        get() = (southLatitude + northLatitude) / 2.0

    val centerLongitude: Double
        get() = (westLongitude + eastLongitude) / 2.0
}
