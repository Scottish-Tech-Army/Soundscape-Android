package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class Triangle(val origin: LngLatAlt, val left: LngLatAlt, val right: LngLatAlt)
