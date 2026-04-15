package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection

fun getPoiFeatureCollectionBySuperCategory(
    superCategory: SuperCategoryId,
    poiFeatureCollection: FeatureCollection
): FeatureCollection {
    val features = poiFeatureCollection.features.filter { feature ->
        (feature as MvtFeature).superCategory == superCategory
    }
    val tempFeatureCollection = FeatureCollection()
    tempFeatureCollection.features += features
    return tempFeatureCollection
}

fun featureIsInFilterGroup(feature: Feature, filter: String): Boolean {
    val tags = when (filter) {
        "transit" -> listOf("bus_stop", "train_station", "tram_stop", "ferry_terminal", "station")
        "food_and_drink" -> listOf(
            "restaurant", "fast_food", "cafe", "bar", "ice_cream", "pub", "coffee_shop"
        )
        "parks" -> listOf(
            "park", "garden", "green_space", "recreation_area", "playground", "nature_reserve",
            "botanical_garden", "public_garden", "field", "reserve"
        )
        "groceries" -> listOf("supermarket", "convenience", "grocery")
        "banks" -> listOf("bank", "atm")
        else -> emptyList()
    }
    if (tags.isEmpty()) return true
    for (tag in tags) {
        val mvtFeature = feature as MvtFeature
        if (mvtFeature.featureValue == tag) return true
    }
    return false
}

fun isDuplicateByOsmId(existingSet: MutableSet<Any>, feature: MvtFeature): Boolean {
    val osmId = feature.osmId
    if (existingSet.contains(osmId)) return true
    existingSet.add(osmId)
    return false
}

fun deduplicateFeatureCollection(
    outputFeatureCollection: FeatureCollection,
    inputFeatureCollection: FeatureCollection?,
    existingSet: MutableSet<Any>
) {
    inputFeatureCollection?.let { collection ->
        for (feature in collection.features) {
            if (!isDuplicateByOsmId(existingSet, feature as MvtFeature)) {
                outputFeatureCollection.features.add(feature)
            }
        }
    }
}

fun removeDuplicateOsmIds(featureCollection: FeatureCollection): FeatureCollection {
    val processedOsmIds = mutableSetOf<Any>()
    val tempFeatureCollection = FeatureCollection()
    deduplicateFeatureCollection(tempFeatureCollection, featureCollection, processedOsmIds)
    return tempFeatureCollection
}
