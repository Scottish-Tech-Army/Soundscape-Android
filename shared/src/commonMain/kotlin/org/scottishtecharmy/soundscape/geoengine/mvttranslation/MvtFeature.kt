package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature

open class MvtFeature : Feature() {
    var osmId : Long = 0L
    var name : String? = null
    var housenumber : String? = null
    var street : String? = null
    var side : Boolean? = null
    var streetConfidence : Boolean = false
    var featureClass : String? = null
    var featureSubClass : String? = null
    var featureType : String? = null
    var featureValue : String? = null
    var superCategory : SuperCategoryId = SuperCategoryId.UNCATEGORIZED

    fun setProperty(key: String, value: Any) {
        (properties ?: HashMap()).also {
            it[key] = value
            properties = it
        }
    }

    fun copyProperties(other: MvtFeature) {
        osmId = other.osmId
        name = other.name
        housenumber = other.housenumber
        street = other.street
        side = other.side
        streetConfidence = other.streetConfidence
        featureClass = other.featureClass
        featureSubClass = other.featureSubClass
        featureType = other.featureType
        featureValue = other.featureValue
        superCategory = other.superCategory
    }
}
