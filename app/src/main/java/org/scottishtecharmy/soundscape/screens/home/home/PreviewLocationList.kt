package org.scottishtecharmy.soundscape.screens.home.home

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

val previewLocationList = listOf(
    LocationDescription(
        name = "Barrowland Ballroom",
        description = "Somewhere in Glasgow",
        location = LngLatAlt(-4.2366753, 55.8552688),
    ),
    LocationDescription(
        name = "King Tut's Wah Wah Hut",
        description = "Somewhere else in Glasgow",
        location = LngLatAlt(-4.2649646, 55.8626180),
    ),
    LocationDescription(
        name = "St. Lukes and the Winged Ox",
        description = "Where else?",
        location = LngLatAlt(-4.2347580, 55.8546320),
    ),
)
