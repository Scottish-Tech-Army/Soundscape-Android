package org.scottishtecharmy.soundscape.geojsonparser.geojson

open class Point() : GeoJsonObject() {
    var coordinates: LngLatAlt = LngLatAlt()

    init {
        type = "Point"
    }

    constructor(lng: Double, lat: Double, alt: Double? = null) : this() {
        coordinates = LngLatAlt(lng, lat, alt)
    }

    constructor(location: LngLatAlt) : this() {
        coordinates = location
    }
}
