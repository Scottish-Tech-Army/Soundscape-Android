package org.scottishtecharmy.soundscape.screens.home.data

import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class LocationDescription(
    var addressName: String? = null,
    var fullAddress: String? = null,
    var distance: String? = null,
    var location: LngLatAlt = LngLatAlt(),
    var markerObjectId: ObjectId? = null
)
