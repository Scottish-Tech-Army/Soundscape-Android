package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon

expect fun mergePolygons(polygon1: Feature, polygon2: Feature): Feature

fun polygonFeaturesOverlap(feature1: Feature, feature2: Feature): Boolean {
    for (point in (feature1.geometry as Polygon).coordinates[0]) {
        if (polygonContainsCoordinates(point, (feature2.geometry as Polygon)))
            return true
    }
    return false
}

fun mergeAllPolygonsInFeatureCollection(
    polygonFeatureCollection: FeatureCollection
): FeatureCollection {

    val resultantFeatureCollection = FeatureCollection()

    val features = hashMapOf<Any, MutableList<FeatureCollection>>()
    for (feature in polygonFeatureCollection.features) {
        if (feature.geometry.type == "Polygon") {
            val osmId = (feature as MvtFeature).osmId
            if (!features.containsKey(osmId)) {
                features[osmId] = mutableListOf()
            }
            var foundOverlap = false
            for (featureCollection in features[osmId]!!) {
                for (existingFeature in featureCollection) {
                    if (polygonFeaturesOverlap(feature, existingFeature)) {
                        featureCollection.addFeature(feature)
                        foundOverlap = true
                        break
                    }
                }
            }
            if (!foundOverlap) {
                val newFeatureCollection = FeatureCollection()
                newFeatureCollection.addFeature(feature)
                features[osmId]!!.add(newFeatureCollection)
            }
        } else {
            resultantFeatureCollection.addFeature(feature)
        }
    }

    for (featureCollectionList in features) {
        for (featureCollection in featureCollectionList.value) {
            var mergedFeature: Feature? = null
            for ((index, feature) in featureCollection.features.withIndex()) {
                val tempMergedFeature = mergedFeature
                mergedFeature = if (index == 0) {
                    feature
                } else {
                    mergePolygons(mergedFeature!!, feature)
                }
                if (mergedFeature == feature) {
                    if (tempMergedFeature != null)
                        resultantFeatureCollection.addFeature(tempMergedFeature)
                }
            }
            resultantFeatureCollection.addFeature(mergedFeature!!)
        }
    }
    return resultantFeatureCollection
}
