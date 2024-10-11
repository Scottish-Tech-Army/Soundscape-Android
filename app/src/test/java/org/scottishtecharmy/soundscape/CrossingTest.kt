package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.utils.distance
import org.scottishtecharmy.soundscape.utils.getCrossingsFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getFovIntersectionFeatureCollection
import org.scottishtecharmy.soundscape.utils.getNearestIntersection

class CrossingTest {

    @Test
    fun simpleCrossingTest(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        // extract crossings for tile
        val crossingsFeatureCollection = getCrossingsFromTileFeatureCollection(featureCollectionTest!!)
        Assert.assertEquals(2, crossingsFeatureCollection.features.size)

        val crossingString = moshi.adapter(FeatureCollection::class.java).toJson(crossingsFeatureCollection)
        println("Crossings in tile: $crossingString")

        // usual fake our device location and heading
        val currentLocation = LngLatAlt(-2.6920313574678403, 51.43745588326692)
        val deviceHeading = 45.0
        val fovDistance = 50.0

        // TODO: we can reuse the intersection code as crossings are GeoJSON Points just like Intersections
        //  but there will be more complex crossings so I'll need to check some other tiles
        val fovCrossingFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            crossingsFeatureCollection
        )
        Assert.assertEquals(1, fovCrossingFeatureCollection.features.size)
        val nearestCrossing = getNearestIntersection(currentLocation, fovCrossingFeatureCollection)
        val crossingLocation = nearestCrossing.features[0].geometry as Point
        val distanceToCrossing = distance(
            currentLocation.latitude,
            currentLocation.longitude,
            crossingLocation.coordinates.latitude,
            crossingLocation.coordinates.longitude
        )
        Assert.assertEquals(24.58, distanceToCrossing, 0.1)

        // Simple crossings don't appear to contain much information:
        /**
         * "properties": {
        "crossing:markings": "no",
        "tactile_paving": "yes",
        "highway": "crossing",
        "crossing": "unmarked"
      }
         */

    }

}