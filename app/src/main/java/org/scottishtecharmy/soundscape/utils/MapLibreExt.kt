package org.scottishtecharmy.soundscape.utils

import org.maplibre.android.geometry.LatLng
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

fun LngLatAlt.toLatLng(): LatLng = LatLng(latitude, longitude)

fun fromLatLng(loc: LatLng): LngLatAlt = LngLatAlt(loc.longitude, loc.latitude)
