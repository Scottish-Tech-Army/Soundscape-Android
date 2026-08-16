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
            val featureCollectionsForId = features.getOrPut(osmId) { mutableListOf() }
            var foundOverlap = false
            for (featureCollection in featureCollectionsForId) {
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
                featureCollectionsForId.add(newFeatureCollection)
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
                // index > 0 should imply tempMergedFeature is set (from the first feature or a
                // prior merge), but if that invariant is ever violated, fall back to starting a
                // fresh chain from this feature rather than crash.
                mergedFeature = if (index == 0 || tempMergedFeature == null) {
                    feature
                } else {
                    mergePolygons(tempMergedFeature, feature)
                }
                if (mergedFeature == feature) {
                    if (tempMergedFeature != null)
                        resultantFeatureCollection.addFeature(tempMergedFeature)
                }
            }
            // featureCollection is only ever created with at least one feature already added to
            // it, so the loop above always runs and sets mergedFeature. Skip adding anything if
            // that's ever not the case, rather than crash.
            mergedFeature?.let { resultantFeatureCollection.addFeature(it) }
        }
    }
    return resultantFeatureCollection
}
