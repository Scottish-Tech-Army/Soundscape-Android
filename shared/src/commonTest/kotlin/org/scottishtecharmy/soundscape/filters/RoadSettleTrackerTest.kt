package org.scottishtecharmy.soundscape.filters

import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.RoadSettleTracker
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoadSettleTrackerTest {

    private val origin = LngLatAlt(-4.3170, 55.9420)
    private val ruler = CheapRuler(origin.latitude)

    private fun road(name: String? = null, ref: String? = null) = Way().apply {
        this.name = name
        this.ref = ref
    }

    /** Driving north, [north] metres from [origin], matched to [way]. */
    private fun driving(north: Double, way: Way?) = UserGeometry(
        location = ruler.offset(origin, 0.0, north),
        speed = 12.0,
        mapMatchedWay = way,
    )

    @Test
    fun testNothingMatchedIsSettled() {
        val tracker = RoadSettleTracker()
        tracker.update(driving(0.0, null))
        assertTrue(tracker.settled())
    }

    @Test
    fun testANewRoadSettlesAfterTheSettleDistance() {
        val tracker = RoadSettleTracker()
        val street = road(name = "Kessington Road")
        tracker.update(driving(0.0, street))
        assertFalse(tracker.settled())
        tracker.update(driving(100.0, street))
        assertFalse(tracker.settled())
        tracker.update(driving(160.0, street))
        assertTrue(tracker.settled())
    }

    @Test
    fun testTurningOffStartsAgain() {
        val tracker = RoadSettleTracker()
        tracker.update(driving(0.0, road(name = "Kessington Road")))
        tracker.update(driving(200.0, road(name = "Kessington Road")))
        assertTrue(tracker.settled())
        tracker.update(driving(220.0, road(name = "Rannoch Drive")))
        assertFalse(tracker.settled())
    }

    @Test
    fun testANumberedRoadIsOneRoadThroughItsNameChanges() {
        val tracker = RoadSettleTracker()
        tracker.update(driving(0.0, road(name = "Strathblane Road", ref = "A81")))
        tracker.update(driving(200.0, road(name = "Glasgow Road", ref = "A81")))
        assertTrue(tracker.settled())
    }

    @Test
    fun testLosingTheMatchKeepsTheRoad() {
        val tracker = RoadSettleTracker()
        val street = road(name = "Kessington Road")
        tracker.update(driving(0.0, street))
        tracker.update(driving(100.0, null))
        tracker.update(driving(160.0, street))
        assertTrue(tracker.settled())
    }

    @Test
    fun testAFlickerOntoACrossStreetCarriesOn() {
        val tracker = RoadSettleTracker()
        val hope = road(name = "Hope Street")
        tracker.update(driving(0.0, hope))
        tracker.update(driving(100.0, hope))
        tracker.update(driving(115.0, road(name = "Sauchiehall Street")))
        tracker.update(driving(160.0, hope))
        assertTrue(tracker.settled())
    }

    @Test
    fun testALongerStretchOnAnotherRoadIsATurn() {
        val tracker = RoadSettleTracker()
        tracker.update(driving(0.0, road(name = "Kessington Road")))
        tracker.update(driving(100.0, road(name = "Kessington Road")))
        tracker.update(driving(110.0, road(name = "Rannoch Drive")))
        tracker.update(driving(180.0, road(name = "Rannoch Drive")))
        tracker.update(driving(200.0, road(name = "Kessington Road")))
        assertFalse(tracker.settled())
    }

    @Test
    fun testComingBackToTheSettledRoadIsSettled() {
        val tracker = RoadSettleTracker()
        val a82 = road(name = "Great Western Road", ref = "A82")
        tracker.update(driving(0.0, a82))
        tracker.update(driving(200.0, a82))
        tracker.update(driving(210.0, road()))
        tracker.update(driving(270.0, road()))
        assertFalse(tracker.settled())
        tracker.update(driving(280.0, a82))
        assertTrue(tracker.settled())
    }
}
