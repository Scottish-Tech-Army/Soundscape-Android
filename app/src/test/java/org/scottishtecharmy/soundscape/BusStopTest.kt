package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.utils.distance
import org.scottishtecharmy.soundscape.utils.getBusStopsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getFovIntersectionFeatureCollection
import org.scottishtecharmy.soundscape.utils.getNearestIntersection

class BusStopTest {

    @Test
    fun busStopTest(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonIntersectionStraight.intersectionStraightAheadFeatureCollection)

        // extract bus stops for tile
        val busStopFeatureCollection = getBusStopsFeatureCollectionFromTileFeatureCollection(featureCollectionTest!!)
        // There are only two bus stops in this tile
        Assert.assertEquals(2, busStopFeatureCollection.features.size)

        // Bus stops are tricky as they can be mapped very badly and there isn't much we can do
        // about it apart from correcting them individually in OSM and then hoping the backend picks it up correctly.
        // This is a good examples as the points are way off their actual location in the real world.
        // One appears to be in a garden and the other is on the wrong side of the road!
        val busStopString = moshi.adapter(FeatureCollection::class.java).toJson(busStopFeatureCollection)
        println("Bus stops in tile: $busStopString")

        // However we can get some dodgy info for the user so...
        // pretend the device is here, pointing along the road and our field of view
        val currentLocation = LngLatAlt(-2.655732516651227,51.430910659124464)
        val deviceHeading = 225.0
        val fovDistance = 50.0
        // we can reuse the intersection code as bus stops are GeoJSON Points just like Intersections
        val fovBusStopFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            busStopFeatureCollection
        )
        Assert.assertEquals(2, fovBusStopFeatureCollection.features.size)
        // we can detect the nearest bus stop and give a distance/direction but as mentioned above the OSM
        // bus stop location data for this example is rubbish so not sure how useful this is to the user?
        val nearestBusStop = getNearestIntersection(currentLocation, fovBusStopFeatureCollection)
        val busStopLocation = nearestBusStop.features[0].geometry as Point
        val distanceToBusStop = distance(
            currentLocation.latitude,
            currentLocation.longitude,
            busStopLocation.coordinates.latitude,
            busStopLocation.coordinates.longitude
        )
        Assert.assertEquals(9.08, distanceToBusStop, 0.1)

        // Here's the naptan stuff stored in the bus stop properties which might be
        // useful but this is only in the UK so not sure how useful it is?
        /*
        "naptan:NaptanCode": "wsmjgam",
        "naptan:verified": "no",
        "naptan:AtcoCode": "0190NSC30719",
        "naptan:Bearing": "NE",
        "naptan:CommonName": "Bourton Mead",
        "naptan:Street": "Long Ashton Road"
        */
    }

}