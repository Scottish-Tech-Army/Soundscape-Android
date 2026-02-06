package org.scottishtecharmy.soundscape

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.circleToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getTriangleForDirection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import java.io.FileOutputStream
import kotlin.test.assertNotEquals

fun diffFeatureCollections(collection1 : FeatureCollection, collection2: FeatureCollection) {
    val uniqueTo1 = FeatureCollection()
    val uniqueTo2 = FeatureCollection()

    for(feature1 in collection1) {
        var found = false
        for(feature2 in collection2) {
            if(feature1 == feature2) {
                found = true
                break
            }
        }
        if(!found) {
            uniqueTo1.addFeature(feature1)
        }
    }

    for(feature2 in collection2) {
        var found = false
        for(feature1 in collection1) {
            if(feature1 == feature2) {
                found = true
                break
            }
        }
        if(!found) {
            uniqueTo2.addFeature(feature2)
        }
    }

    for(feature in uniqueTo1.features) {
        println("Unique to 1: ${feature.properties}")
    }
    for(feature in uniqueTo2.features) {
        println("Unique to 2: ${feature.properties}")
    }

//        assert(uniqueTo1.features.isEmpty())
//        assert(uniqueTo2.features.isEmpty())
}

class PoiTest {

    private fun getNameForFeature(feature: MvtFeature): String {
        return feature.name ?: feature.featureClass!!
    }

    @Test
    fun testNearestFeature() {

        val userGeometry = UserGeometry(
            LngLatAlt(-4.317229, 55.941891),
            0.0,
            50.0
        )
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val poi = gridState.getFeatureTree(TreeId.POIS)

        val polygons = getRelativeDirectionsPolygons(userGeometry, RelativeDirections.INDIVIDUAL)

        val featuresToDraw = FeatureCollection()
        for ((index, polygon) in polygons.features.withIndex()) {

            val nearestFeature = poi.getNearestFeatureWithinTriangle(
                getTriangleForDirection(polygons, index),
                gridState.ruler
            )

            val name = getNameForFeature(nearestFeature as MvtFeature)
            println("Nearest feature: $name")
            when (index) {
                0 -> assertEquals("bridge", name)
                1 -> assertEquals("Creature Comforts", name)
                2 -> assertEquals("The Scottish Gantry", name)
                3 -> assertEquals("Garvie & Co", name)
            }

            featuresToDraw.addFeature(polygon)
            featuresToDraw.addFeature(nearestFeature)
        }

        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("poi.geojson")
        outputFile.write(adapter.toJson(featuresToDraw).toByteArray())
        outputFile.close()
    }

    @Test
    fun testNearestFeatures() {

        val userGeometry = UserGeometry(
            LngLatAlt(-4.317229, 55.941891),
            0.0,
            50.0
        )
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val poi = gridState.getFeatureTree(TreeId.POIS)

        val polygons = getRelativeDirectionsPolygons(userGeometry, RelativeDirections.INDIVIDUAL)

        val featuresToDraw = FeatureCollection()
        for ((index, polygon) in polygons.features.withIndex()) {

            val poiFeatures = poi.getNearestCollectionWithinTriangle(
                getTriangleForDirection(polygons, index),
                10,
                gridState.ruler
            )
            // Check that the first returned name is the nearest
            val nearestName = getNameForFeature(poiFeatures.features[0] as MvtFeature)
            println("Nearest name : $nearestName")
            when (index) {
                0 -> assertEquals("bridge", nearestName)
                1 -> assertEquals("Creature Comforts", nearestName)
                2 -> assertEquals("The Scottish Gantry", nearestName)
                3 -> assertEquals("Garvie & Co", nearestName)
            }

            val furthestName = getNameForFeature(poiFeatures.features.last() as MvtFeature)
            println("Furthest name : $furthestName")
            when (index) {
                0 -> assertEquals("Woodburn Way Car Park", furthestName)
                1 -> assertEquals("No. 1 Boutique", furthestName)
                2 -> assertEquals("Vivienne Nails & Spa", furthestName)
                3 -> assertEquals("Woodburn Way Car Park", furthestName)
            }

            featuresToDraw.addFeature(polygon)
            featuresToDraw += poiFeatures
        }

        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("poi.geojson")
        outputFile.write(adapter.toJson(featuresToDraw).toByteArray())
        outputFile.close()
    }

    @Test
    fun testFeaturesWithinRadius() {

        val userGeometry = UserGeometry(
            LngLatAlt(-4.317229, 55.941891),
            0.0,
            50.0
        )
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val poi = gridState.getFeatureTree(TreeId.POIS)

        val featuresToDraw = FeatureCollection()
        val features = poi.getNearbyCollection(userGeometry.location, 50.0, gridState.ruler)
        featuresToDraw += features
        //assertEquals(33, features.features.size)

        val circle = Feature()
        circle.geometry = circleToPolygon(
            40,
            userGeometry.location.latitude,
            userGeometry.location.longitude,
            50.0
        )
        featuresToDraw.addFeature(circle)

        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("poi.geojson")
        outputFile.write(adapter.toJson(features).toByteArray())
        outputFile.close()
    }

    @Test
    fun testClassification() {
        // A grid-wide test of the POI classification
        val userGeometry = UserGeometry(
            LngLatAlt(-4.317229, 55.941891),
            0.0,
            50.0
        )
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val poi = gridState.getFeatureTree(TreeId.POIS)
        val features = poi.getAllCollection()

        val info = gridState.getFeatureTree(TreeId.INFORMATION_POIS).getAllCollection()
        val objects = gridState.getFeatureTree(TreeId.OBJECT_POIS).getAllCollection()
        val places = gridState.getFeatureTree(TreeId.PLACE_POIS).getAllCollection()
        val landmarks = gridState.getFeatureTree(TreeId.LANDMARK_POIS).getAllCollection()
        val mobility = gridState.getFeatureTree(TreeId.MOBILITY_POIS).getAllCollection()
        val safety = gridState.getFeatureTree(TreeId.SAFETY_POIS).getAllCollection()
        val entrances = gridState.getFeatureTree(TreeId.ENTRANCES).getAllCollection()

        // Make a FeatureCollection containing all of the super category Features. If the
        // classification hasn't missed anything, then this should include all the POIs.
        val joint = FeatureCollection()
        joint += info
        joint += objects
        joint += places
        joint += landmarks
        joint += mobility
        joint += safety
        joint += entrances
        diffFeatureCollections(features, joint)

        // The differences between these two collections are all un-categorised OSM tags
        //
        // schoolyard
        // politician
        // rail
        // ticket_validator
        // trolley_bay
        // health_food
        // rail bridge
        // Amazon Hub jonty
        // outdoor_seating
    }

    @Test
    fun testBuildingCallout() {
        val userGeometry = UserGeometry(
            LngLatAlt(-4.2468642, 55.8597374),
            0.0,
            50.0
        )
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val poiTree = gridState.getFeatureTree(TreeId.POIS)
        val pois = poiTree.getNearbyCollection(userGeometry.location, 50.0, gridState.ruler)
        for(poi in pois) {
            println("Poi: ${(poi as MvtFeature).name} ${getTextForFeature(null, poi)}")
            assertNotEquals("Unknown", getTextForFeature(null, poi).text)
        }
    }
}