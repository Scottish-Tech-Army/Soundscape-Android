package org.scottishtecharmy.soundscape.geoengine.utils.rulers

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

fun LngLatAlt.createCheapRuler(): CheapRuler = CheapRuler(latitude)
