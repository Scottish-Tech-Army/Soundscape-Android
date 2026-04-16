package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class PositionedString(
    val text : String,
    val location : LngLatAlt? = null,
    val earcon : String? = null,
    val type: AudioType = AudioType.STANDARD,
    val heading: Double? = null,
    val addDistanceAndHeading: Boolean = false
)
