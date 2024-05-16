package com.kersnazzle.soundscapealpha.geojsonparser.geojson

import com.squareup.moshi.Json

/**
 * A FeatureCollection object has a member
 * with the name "features".  The value of "features" is a JSON array.
 * Each element of the array is a Feature object.  It
 * is possible for this array to be empty.
 *     https://datatracker.ietf.org/doc/html/rfc7946#section-3.3
 */

open class FeatureCollection : GeoJsonObject(), Iterable<Feature> {
    @field:Json(name = "features")
    var features: ArrayList<Feature> = arrayListOf<Feature>()

    init {
        type = "FeatureCollection"
    }

    fun addFeature(feature: Feature): FeatureCollection {
        features.add(feature)
        return this
    }

    override fun iterator(): Iterator<Feature> = features.iterator()

    operator fun plusAssign(rhs: Feature): Unit {
        features.add(rhs)
    }
}