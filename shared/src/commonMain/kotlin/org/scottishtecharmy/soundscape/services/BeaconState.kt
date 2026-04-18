package org.scottishtecharmy.soundscape.services

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class BeaconState(val location: LngLatAlt? = null, val muteState: Boolean = false)
