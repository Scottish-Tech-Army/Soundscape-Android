package org.scottishtecharmy.soundscape.geojsonparser.geojson

open class Polygon() : Geometry<ArrayList<LngLatAlt>>() {
    init {
        type = "Polygon"
    }

    constructor(polygon: ArrayList<LngLatAlt>) : this() {
        this.coordinates.add(polygon)
    }

    fun getExteriorRing(): ArrayList<LngLatAlt> {
        assertExteriorRing()
        return coordinates[0]
    }

    fun getInteriorRings(): ArrayList<ArrayList<LngLatAlt>> {
        assertExteriorRing()
        return ArrayList(coordinates.subList(1, coordinates.size))
    }

    fun getInteriorRing(index: Int): ArrayList<LngLatAlt> {
        assertExteriorRing()
        return coordinates[1 + index]
    }

    fun addInteriorRing(points: ArrayList<LngLatAlt>) {
        assertExteriorRing()
        coordinates.add(points)
    }

    fun addInteriorRing(vararg points: LngLatAlt) {
        assertExteriorRing()
        coordinates.add(arrayListOf(*points))
    }

    private fun assertExteriorRing() {
        if (coordinates.isEmpty()) {
            throw RuntimeException("No exterior ring defined.")
        }
    }
}
