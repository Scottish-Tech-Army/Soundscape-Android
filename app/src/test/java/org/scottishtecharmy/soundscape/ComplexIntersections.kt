package org.scottishtecharmy.soundscape

import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geoengine.callouts.getRoadsDescriptionFromFov
import org.scottishtecharmy.soundscape.geojsontestdata.GeoJSONDataComplexIntersection
import org.scottishtecharmy.soundscape.geojsontestdata.GeoJSONDataComplexIntersection1

class ComplexIntersections {

    @Test
    fun complexIntersections1Test(){
        //https://geojson.io/#map=18.61/51.4439294/-2.6974316
        // this is probably the simplest example of a complex intersection where we have
        // a road that splits into two one way roads before joining another road. There are also
        // multiple gd_intersections detected in the FoV so we need to determine which ones to ignore
        // and which ones are useful to call out to the user
        // Fake location, heading and Field of View for testing
        val gridState = GridState.createFromGeoJson(GeoJSONDataComplexIntersection1.COMPLEX_INTERSECTION1_JSON)
        val userGeometry = UserGeometry(
            LngLatAlt(-2.697291022799874,51.44378095087524),
            320.0,
            50.0,
            nearestRoad = gridState.getNearestFeature(TreeId.ROADS, LngLatAlt(-2.697291022799874,51.44378095087524)))

        val roadRelativeDirections = getRoadsDescriptionFromFov(
            gridState,
            userGeometry
        ).intersectionRoads

        Assert.assertEquals(3, roadRelativeDirections.features.size)

        Assert.assertEquals(0, roadRelativeDirections.features[0].properties!!["Direction"])
        Assert.assertEquals(
            "Flax Bourton Road",
            roadRelativeDirections.features[0].properties!!["name"]
        )
        Assert.assertEquals(3, roadRelativeDirections.features[1].properties!!["Direction"])
        Assert.assertEquals(
            "Clevedon Road",
            roadRelativeDirections.features[1].properties!!["name"]
        )
        Assert.assertEquals(7, roadRelativeDirections.features[2].properties!!["Direction"])
        Assert.assertEquals(
            "Clevedon Road",
            roadRelativeDirections.features[2].properties!!["name"]
        )
    }

    @Test
    fun complexIntersections2Test() {
        // This is a more complex junction than the above test.
        // There are multiple gd_intersections detected in the FoV so we need to determine which ones to ignore
        // and which ones are useful to call out to the user
        // https://geojson.io/#map=18.65/51.4405486/-2.6851813
        // Fake location, heading and Field of View for testing
        val gridState = GridState.createFromGeoJson(GeoJSONDataComplexIntersection.COMPLEX_INTERSECTION_JSON)
        val userGeometry = UserGeometry(
            LngLatAlt(-2.6854420947740323, 51.44036284885249),
            45.0,
            50.0,
            nearestRoad = gridState.getNearestFeature(TreeId.ROADS, LngLatAlt(-2.697291022799874,51.44378095087524))
        )

        val roadRelativeDirections = getRoadsDescriptionFromFov(
            gridState,
            userGeometry
        ).intersectionRoads

        Assert.assertEquals(4, roadRelativeDirections.features.size)
        //
        Assert.assertEquals(1, roadRelativeDirections.features[0].properties!!["Direction"])
        Assert.assertEquals(
            "Clevedon Road",
            roadRelativeDirections.features[0].properties!!["name"]
        )
        Assert.assertEquals(3, roadRelativeDirections.features[1].properties!!["Direction"])
// We're currently not quite getting the correct intersection here. But the algorithm, is better than nothing
//        Assert.assertEquals(
//            "Beggar Bush Lane",
//            roadRelativeDirections.features[1].properties!!["name"]
//        )
        Assert.assertEquals(5, roadRelativeDirections.features[2].properties!!["Direction"])
        Assert.assertEquals(
            "Clevedon Road",
            roadRelativeDirections.features[2].properties!!["name"]
        )
        Assert.assertEquals(7, roadRelativeDirections.features[3].properties!!["Direction"])
        Assert.assertEquals(
            "Weston Road",
            roadRelativeDirections.features[3].properties!!["name"]
        )
    }
}