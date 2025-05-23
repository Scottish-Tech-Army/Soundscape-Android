package org.scottishtecharmy.soundscape

import org.junit.Assert
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geoengine.callouts.getRoadsDescriptionFromFov
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way

class ComplexIntersections {

    //@Test
    fun complexIntersections1Test(){
        //https://geojson.io/#map=18.61/51.4439294/-2.6974316
        // this is probably the simplest example of a complex intersection where we have
        // a road that splits into two one way roads before joining another road. There are also
        // multiple gd_intersections detected in the FoV so we need to determine which ones to ignore
        // and which ones are useful to call out to the user
        // Fake location, heading and Field of View for testing
        val location = LngLatAlt(-2.697291022799874,51.44378095087524)
        val gridState = getGridStateForLocation(location, 1)
        val userGeometry = UserGeometry(
            location,
            320.0,
            50.0,
            mapMatchedWay = gridState.getNearestFeature(TreeId.ROADS, gridState.ruler, LngLatAlt(-2.697291022799874,51.44378095087524)) as Way
        )

        val intersection = getRoadsDescriptionFromFov(
            gridState,
            userGeometry
        ).intersection

        Assert.assertEquals(3, intersection!!.members.size)

        val indexFBR = 1
        val indexCR = 0
        val indexCR2 = 2
        Assert.assertEquals(1, intersection.members[indexFBR].direction(intersection, userGeometry.heading()!!))
        Assert.assertEquals(
            "Flax Bourton Road",
            intersection.members[indexFBR].properties!!["name"]
        )

        Assert.assertEquals(3, intersection.members[indexCR].direction(intersection, userGeometry.heading()!!))
        Assert.assertEquals(
            "Clevedon Road",
            intersection.members[indexCR].properties!!["name"]
        )
        Assert.assertEquals(7, intersection.members[indexCR2].direction(intersection, userGeometry.heading()!!))
        Assert.assertEquals(
            "Clevedon Road",
            intersection.members[indexCR2].properties!!["name"]
        )
    }

    //@Test
    fun complexIntersections2Test() {
        // This is a more complex junction than the above test.
        // There are multiple gd_intersections detected in the FoV so we need to determine which ones to ignore
        // and which ones are useful to call out to the user
        // https://geojson.io/#map=18.65/51.4405486/-2.6851813
        // Fake location, heading and Field of View for testing
        val location = LngLatAlt(-2.6854420947740323, 51.44036284885249)
        val gridState = getGridStateForLocation(location, 1)
        val userGeometry = UserGeometry(
            location,
            45.0,
            50.0,
            mapMatchedWay = gridState.getNearestFeature(TreeId.ROADS, gridState.ruler, location) as Way
        )

        val intersection = getRoadsDescriptionFromFov(
            gridState,
            userGeometry
        ).intersection

        Assert.assertEquals(4, intersection!!.members.size)

        val indexWR1 = 0
        val indexWR2 = 1
        val indexCR = 3
        val indexCR2 = 2
        Assert.assertEquals(1, intersection.members[indexCR].direction(intersection, userGeometry.heading()!!))
        Assert.assertEquals(
            "Clevedon Road",
            intersection.members[indexCR].properties!!["name"]
        )

        Assert.assertEquals(4, intersection.members[indexWR2].direction(intersection, userGeometry.heading()!!))
        Assert.assertEquals(
            "Weston Road",
            intersection.members[indexWR2].properties!!["name"]
        )

        Assert.assertEquals(5, intersection.members[indexCR2].direction(intersection, userGeometry.heading()!!))
        Assert.assertEquals(
            "Clevedon Road",
            intersection.members[indexCR2].properties!!["name"]
        )
        Assert.assertEquals(0, intersection.members[indexWR1].direction(intersection, userGeometry.heading()!!))
        Assert.assertEquals(
            "Weston Road",
            intersection.members[indexWR1].properties!!["name"]
        )
    }
}