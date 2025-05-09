package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle

class MarkersTest {

    @Test
    fun markersTest() {
        val userGeometry = UserGeometry(
            LngLatAlt(-2.6930121073553437,51.43943095899127),
            10.0,
            50.0
        )

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val markersFeatureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJSONMarkers.MARKERS_FEATURE_COLLECTION)
        val markersTree = FeatureTree(markersFeatureCollectionTest)

        // I'm just reusing the Intersection functions here for the markers test
        val triangle = getFovTriangle(userGeometry)
        val nearestMarker = markersTree.getNearestFeatureWithinTriangle(triangle, userGeometry.ruler)
        val nearestPoint = nearestMarker!!.geometry as Point
        val nearestMarkerDistance = userGeometry.ruler.distance(userGeometry.location, nearestPoint.coordinates)

        println("Approaching ${nearestMarker.properties!!["marker"]} marker at $nearestMarkerDistance meters")


    }


}