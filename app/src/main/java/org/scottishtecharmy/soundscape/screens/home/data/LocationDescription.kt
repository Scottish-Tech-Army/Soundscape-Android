package org.scottishtecharmy.soundscape.screens.home.data

import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class LocationDescription(
    var name: String? = null,
    var location: LngLatAlt,
    var fullAddress: String? = null,
    var markerObjectId: ObjectId? = null
)
