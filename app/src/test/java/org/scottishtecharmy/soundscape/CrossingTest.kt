package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GridState.Companion.createFromGeoJson
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geoengine.utils.getFovFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad

class CrossingTest {

    @Test
    fun simpleCrossingTest(){
        val gridState = createFromGeoJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)

        // Get all the roads from the tile
        val testRoadsCollectionFromTileFeatureCollection = gridState.getFeatureCollection(TreeId.ROADS)
        // extract crossings for tile
        val crossingsFeatureCollection = gridState.getFeatureCollection(TreeId.CROSSINGS)
        Assert.assertEquals(2, crossingsFeatureCollection.features.size)

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val crossingString = moshi.adapter(FeatureCollection::class.java).toJson(crossingsFeatureCollection)
        println("Crossings in tile: $crossingString")

        // usual fake our device location and heading
        val userGeometry = UserGeometry(
            LngLatAlt(-2.6920313574678403, 51.43745588326692),
            45.0,
            50.0
        )

        // We can reuse the intersection code as crossings are GeoJSON Points just like Intersections
        //  but there will be more complex crossings so I'll need to check some other tiles
        val fovCrossingFeatureCollection = getFovFeatureCollection(
            userGeometry,
            FeatureTree(crossingsFeatureCollection)
        )
        Assert.assertEquals(1, fovCrossingFeatureCollection.features.size)

        val nearestCrossing = FeatureTree(fovCrossingFeatureCollection).getNearestFeature(userGeometry.location)
        val crossingLocation = nearestCrossing!!.geometry as Point
        val distanceToCrossing = userGeometry.location.distance(crossingLocation.coordinates)

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