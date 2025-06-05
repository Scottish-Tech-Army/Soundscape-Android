package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.circleToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getTriangleForDirection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import java.io.FileOutputStream

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

    private fun getNameForFeature(feature: Feature) : String {
        var name = feature.properties?.get("name") as String?
        if (name == null) {
            val osmClass = feature.properties?.get("class") as String?
            name = osmClass
        }
        return name!!
    }

    @Test
    fun testNearestFeature() {

        val userGeometry = UserGeometry(LngLatAlt(-4.317229, 55.941891),
            0.0,
            50.0)
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val poi = gridState.getFeatureTree(TreeId.POIS)

        val polygons = getRelativeDirectionsPolygons(userGeometry, RelativeDirections.INDIVIDUAL)

        val featuresToDraw = FeatureCollection()
        for((index, polygon) in polygons.features.withIndex()) {

            val nearestFeature = poi.getNearestFeatureWithinTriangle(
                getTriangleForDirection(polygons, index),
                gridState.ruler
            )

            val name = getNameForFeature(nearestFeature!!)
            println("Nearest feature: $name")
            when(index) {
                0 -> assert(name == "bridge")
                1 -> assert(name == "Creature Comforts")
                2 -> assert(name == "The Scottish Gantry")
                3 -> assert(name == "Garvie & Co")
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

        val userGeometry = UserGeometry(LngLatAlt(-4.317229, 55.941891),
            0.0,
            50.0)
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val poi = gridState.getFeatureTree(TreeId.POIS)

        val polygons = getRelativeDirectionsPolygons(userGeometry, RelativeDirections.INDIVIDUAL)

        val featuresToDraw = FeatureCollection()
        for((index, polygon) in polygons.features.withIndex()) {

            val poiFeatures = poi.getNearestCollectionWithinTriangle(
                getTriangleForDirection(polygons, index),
                10,
                gridState.ruler
            )
            // Check that the first returned name is the nearest
            val nearestName = getNameForFeature(poiFeatures.features[0])
            println("Nearest name : $nearestName")
            when(index) {
                0 -> assert(nearestName == "bridge")
                1 -> assert(nearestName == "Creature Comforts")
                2 -> assert(nearestName == "The Scottish Gantry")
                3 -> assert(nearestName == "Garvie & Co")
            }

            val furthestName = getNameForFeature(poiFeatures.features.last())
            println("Furthest name : $furthestName")
            when(index) {
                0 -> assert(furthestName == "Woodburn Way Car Park")
                1 -> assert(furthestName == "No. 1 Boutique")
                2 -> assert(furthestName == "Allander Flooring")
                3 -> assert(furthestName == "Jessie Biscuit")
            }

            featuresToDraw.addFeature(polygon)
            featuresToDraw.plusAssign(poiFeatures)
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
        featuresToDraw.plusAssign(features)
        assert(features.features.size == 39)

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
        outputFile.write(adapter.toJson(featuresToDraw).toByteArray())
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
        joint.plusAssign(info)
        joint.plusAssign(objects)
        joint.plusAssign(places)
        joint.plusAssign(landmarks)
        joint.plusAssign(mobility)
        joint.plusAssign(safety)
        joint.plusAssign(entrances)
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
}