package org.scottishtecharmy.soundscape.geoengine.utils

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class LocationData() {
    var latitude = ""
    var longitude = ""
    var altitude = ""
    var accuracy = ""
    var speed = ""
    var bearing = ""
    var bearingAccuracyDegrees = ""
    var time = ""
    var heading = ""
}
