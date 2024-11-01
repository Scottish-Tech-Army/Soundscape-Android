package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.getNearestRoad
import org.scottishtecharmy.soundscape.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.traceLineString

class StreetPreviewTest {

    @Test
    fun streetPreviewTest1() {
        // simple first test to move along a road from start to finish
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        val roadFeatureCollectionTest = getRoadsFeatureCollectionFromTileFeatureCollection(
            featureCollectionTest!!)
        val nearestRoadTest = getNearestRoad(
            LngLatAlt(-2.693002695425122,51.43938442591545),
            roadFeatureCollectionTest
        )
        val nearestRoadString = moshi.adapter(FeatureCollection::class.java).toJson(nearestRoadTest)
        // copy and paste into GeoJSON.io
        println("Nearest road/linestring $nearestRoadString")
        val roadTrace = traceLineString(nearestRoadTest, 30.0)
        val roadTraceString = moshi.adapter(FeatureCollection::class.java).toJson(roadTrace)
        // copy and paste into GeoJSON.io
        println("Road trace: $roadTraceString")
    }
}

