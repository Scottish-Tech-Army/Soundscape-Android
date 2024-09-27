package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.utils.distanceToIntersection
import org.scottishtecharmy.soundscape.utils.getFovIntersectionFeatureCollection
import org.scottishtecharmy.soundscape.utils.getNearestIntersection

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

        // I'm just reusing the Intersection functions here for the markers test
        val fovMarkersFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            markersFeatureCollectionTest!!
        )
        // I'm just reusing the Intersection functions here for the markers test
        val nearestMarker = getNearestIntersection(currentLocation, fovMarkersFeatureCollection)
        val nearestPoint = nearestMarker.features[0].geometry as Point
        val nearestMarkerDistance = distanceToIntersection(currentLocation, nearestPoint)

        println("Approaching ${nearestMarker.features[0].properties!!["marker"]} marker at $nearestMarkerDistance meters")


    }


}