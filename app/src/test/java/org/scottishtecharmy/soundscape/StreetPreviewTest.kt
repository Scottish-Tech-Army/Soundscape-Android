package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.utils.createTriangleFOV
import org.scottishtecharmy.soundscape.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.utils.getNearestRoad
import org.scottishtecharmy.soundscape.utils.getQuadrants
import org.scottishtecharmy.soundscape.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.traceLineString

class StreetPreviewTest {

    @Test
    fun streetPreviewTest1() {
        // Start of PoC to track along a road from start to finish and generate field of view triangles
        // as the device moves along the road.
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
        // trace along the road with equidistant points
        val roadTrace = traceLineString(nearestRoadTest, 30.0)
        val roadTraceString = moshi.adapter(FeatureCollection::class.java).toJson(roadTrace)
        // copy and paste into GeoJSON.io
        println("Road trace: $roadTraceString")
        val fovFeatureCollection = FeatureCollection()
        var i = 1
        // walk down the road using the Points from the roadTrace FeatureCollection as a track
        for (feature in roadTrace.features.subList(0, roadTrace.features.size - 1)) {
            val currentPoint = feature.geometry as Point
            val currentLocation = LngLatAlt(
                currentPoint.coordinates.longitude,
                currentPoint.coordinates.latitude
            )
            val nextLocation = roadTrace.features[i++].geometry as Point
            // fake the device heading by "looking" at the next Point
            val deviceHeading = bearingFromTwoPoints(
                currentLocation.latitude,
                currentLocation.longitude,
                nextLocation.coordinates.latitude,
                nextLocation.coordinates.longitude
            )
            println("Device Heading: $deviceHeading")
            val fovTriangle = generateFOVTriangle(currentLocation, deviceHeading)
            fovFeatureCollection.addFeature(fovTriangle)
        }
        val fovFeatureCollectionString = moshi.adapter(FeatureCollection::class.java).toJson(fovFeatureCollection)
        // copy and paste into GeoJSON.io
        println("FoV triangles for road trace: $fovFeatureCollectionString")

    }

    private fun generateFOVTriangle(
        currentLocation: LngLatAlt,
        deviceHeading: Double,
        fovDistance: Double = 50.0
    ): Feature {
        // Direction the device is pointing
        val quadrants = getQuadrants(deviceHeading)
        // get the quadrant index from the heading so we can construct a FOV triangle using the correct quadrant
        var quadrantIndex = 0
        for (quadrant in quadrants) {
            val containsHeading = quadrant.contains(deviceHeading)
            if (containsHeading) {
                break
            } else {
                quadrantIndex++
            }
        }
        // Get the coordinate for the "Left" of the FOV
        val destinationCoordinateLeft = getDestinationCoordinate(
            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
            quadrants[quadrantIndex].left,
            fovDistance
        )

        //Get the coordinate for the "Right" of the FOV
        val destinationCoordinateRight = getDestinationCoordinate(
            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
            quadrants[quadrantIndex].right,
            fovDistance
        )

        // We can now construct our FOV polygon (triangle)
        val polygonTriangleFOV = createTriangleFOV(
            destinationCoordinateLeft,
            currentLocation,
            destinationCoordinateRight
        )

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees 35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV
        return featureFOVTriangle
    }
}

