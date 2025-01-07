package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geoengine.utils.distanceToIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTrianglePoints

class MarkersTest {

    @Test
    fun markersTest() {
        val currentLocation = LngLatAlt(-2.6930121073553437,
            51.43943095899127)
        val deviceHeading = 10.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val markersFeatureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJSONMarkers.markersFeatureCollection)
        val markersTree = FeatureTree(markersFeatureCollectionTest)

        // I'm just reusing the Intersection functions here for the markers test
        val points = getFovTrianglePoints(currentLocation, deviceHeading, fovDistance)
        val nearestMarker = markersTree.getNearestFeatureWithinTriangle(
            currentLocation,
            points.left,
            points.right)
        val nearestPoint = nearestMarker!!.geometry as Point
        val nearestMarkerDistance = distanceToIntersection(currentLocation, nearestPoint)

        println("Approaching ${nearestMarker.properties!!["marker"]} marker at $nearestMarkerDistance meters")


    }


}