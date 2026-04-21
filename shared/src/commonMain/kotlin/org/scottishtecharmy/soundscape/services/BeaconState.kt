package org.scottishtecharmy.soundscape.services

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class BeaconState(
    val location: LngLatAlt? = null,
    val name: String = "",
    val muteState: Boolean = false,
)
