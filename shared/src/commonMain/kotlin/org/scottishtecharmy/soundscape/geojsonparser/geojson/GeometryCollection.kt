package org.scottishtecharmy.soundscape.geojsonparser.geojson

open class GeometryCollection : GeoJsonObject(), Iterable<GeoJsonObject> {
    var geometries: ArrayList<GeoJsonObject> = arrayListOf()

    init {
        type = "GeometryCollection"
    }

    override fun iterator(): Iterator<GeoJsonObject> = geometries.iterator()

    operator fun plusAssign(rhs: GeoJsonObject) {
        geometries.add(rhs)
    }
}
