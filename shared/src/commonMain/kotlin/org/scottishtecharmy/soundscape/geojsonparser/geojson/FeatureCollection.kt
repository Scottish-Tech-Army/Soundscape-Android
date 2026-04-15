package org.scottishtecharmy.soundscape.geojsonparser.geojson

open class FeatureCollection : GeoJsonObject(), Iterable<Feature> {
    var features: ArrayList<Feature> = arrayListOf()

    init {
        type = "FeatureCollection"
    }

    fun addFeature(feature: Feature): FeatureCollection {
        features.add(feature)
        return this
    }

    override fun iterator(): Iterator<Feature> = features.iterator()

    operator fun plusAssign(rhs: Feature) {
        features.add(rhs)
    }

    operator fun plusAssign(rhs: FeatureCollection) {
        features.addAll(rhs)
    }

    fun plusAssignDeduplicate(rhs: FeatureCollection) {
        for(feature in rhs) {
            if(!features.contains(feature))
                features.add(feature)
        }
    }
}
