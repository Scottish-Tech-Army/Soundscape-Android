package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geoengine.utils.getCrossingsFromTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadsFeatureCollectionFromTileFeatureCollection

class CrossingTest {

    @Test
    fun simpleCrossingTest(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        // Get all the roads from the tile
        val testRoadsCollectionFromTileFeatureCollection = getRoadsFeatureCollectionFromTileFeatureCollection(
            featureCollectionTest!!
        )
        // extract crossings for tile
        val crossingsFeatureCollection = getCrossingsFromTileFeatureCollection(featureCollectionTest)
        Assert.assertEquals(2, crossingsFeatureCollection.features.size)

        val crossingString = moshi.adapter(FeatureCollection::class.java).toJson(crossingsFeatureCollection)
        println("Crossings in tile: $crossingString")

        // usual fake our device location and heading
        val currentLocation = LngLatAlt(-2.6920313574678403, 51.43745588326692)
        val deviceHeading = 45.0
        val fovDistance = 50.0

        // We can reuse the intersection code as crossings are GeoJSON Points just like Intersections
        //  but there will be more complex crossings so I'll need to check some other tiles
        val fovCrossingFeatureCollection = getFovFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            FeatureTree(crossingsFeatureCollection)
        )
        Assert.assertEquals(1, fovCrossingFeatureCollection.features.size)

        val nearestCrossing = FeatureTree(fovCrossingFeatureCollection).getNearestFeature(currentLocation)
        val crossingLocation = nearestCrossing!!.geometry as Point
        val distanceToCrossing = distance(
            currentLocation.latitude,
            currentLocation.longitude,
            crossingLocation.coordinates.latitude,
            crossingLocation.coordinates.longitude
        )

        // Confirm which road the crossing is on
        val nearestRoadToCrossing = getNearestRoad(
            LngLatAlt(crossingLocation.coordinates.longitude,crossingLocation.coordinates.latitude),
            FeatureTree(testRoadsCollectionFromTileFeatureCollection)
        )

        Assert.assertEquals(24.58, distanceToCrossing, 0.1)
        Assert.assertEquals("Belmont Drive", nearestRoadToCrossing!!.properties?.get("name"))
        Assert.assertEquals("yes", nearestCrossing.properties?.get("tactile_paving"))

    }

}