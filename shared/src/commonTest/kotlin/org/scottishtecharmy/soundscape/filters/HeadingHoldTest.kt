package org.scottishtecharmy.soundscape.filters

import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.HeadingHold
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeadingHoldTest {

    private val origin = LngLatAlt(-4.2244, 55.8649)
    private val ruler = CheapRuler(origin.latitude)

    /** Metres east/north of [origin]. */
    private fun at(east: Double, north: Double) = ruler.offset(origin, east, north)

    private fun walking(heading: Double, location: LngLatAlt, millis: Long) = UserGeometry(
        location = location,
        travelHeading = heading,
        speed = 1.3,
        timestampMilliseconds = millis,
    )

    private fun standing(location: LngLatAlt, millis: Long) = UserGeometry(
        location = location,
        speed = 0.0,
        stationary = true,
        timestampMilliseconds = millis,
    )

    @Test
    fun testHoldsTheHeadingThroughAPause() {
        val hold = HeadingHold()
        hold.update(walking(180.0, at(0.0, 0.0), 0L))
        hold.update(standing(at(0.0, -1.0), 1_000L))

        assertEquals(180.0, hold.heading(at(0.0, -1.0), ruler, 30_000L))
    }

    @Test
    fun testNothingToHoldWithoutATravelHeading() {
        val hold = HeadingHold()
        hold.update(standing(at(0.0, 0.0), 0L))
        assertNull(hold.heading(at(0.0, 0.0), ruler, 1_000L))
    }

    @Test
    fun testACourseWhileStandingStillIsNotHeld() {
        // UserGeometry only reports a travel heading while in motion, and the hold takes it from
        // there: a course reported while standing still says nothing about which way anyone faces.
        val hold = HeadingHold()
        hold.update(
            UserGeometry(
                location = at(0.0, 0.0),
                travelHeading = 90.0,
                speed = 0.0,
                timestampMilliseconds = 0L
            )
        )
        assertNull(hold.heading(at(0.0, 0.0), ruler, 1_000L))
    }

    @Test
    fun testExpiresWithTime() {
        val hold = HeadingHold()
        hold.update(walking(180.0, at(0.0, 0.0), 0L))

        assertEquals(
            180.0,
            hold.heading(at(0.0, 0.0), ruler, HeadingHold.MAXIMUM_AGE_MILLISECONDS)
        )
        assertNull(hold.heading(at(0.0, 0.0), ruler, HeadingHold.MAXIMUM_AGE_MILLISECONDS + 1))
    }

    @Test
    fun testExpiresWithDistance() {
        val hold = HeadingHold()
        hold.update(walking(180.0, at(0.0, 0.0), 0L))

        assertEquals(180.0, hold.heading(at(0.0, -20.0), ruler, 15_000L))
        assertNull(hold.heading(at(0.0, -30.0), ruler, 15_000L))
    }

    @Test
    fun testANewerHeadingReplacesTheOldOne() {
        val hold = HeadingHold()
        hold.update(walking(180.0, at(0.0, 0.0), 0L))
        hold.update(walking(90.0, at(0.0, -5.0), 4_000L))

        assertEquals(90.0, hold.heading(at(0.0, -5.0), ruler, 10_000L))
    }

    @Test
    fun testAnyLiveHeadingComesBeforeTheHeldOne() {
        // The phone's compass is where the user is pointing now; the held heading is only a guess
        // from where they were going.
        val withPhone = UserGeometry(
            location = at(0.0, 0.0),
            phoneHeading = 270.0,
            heldHeading = 180.0,
        )
        assertEquals(270.0, withPhone.heading())

        val withNothingElse = UserGeometry(location = at(0.0, 0.0), heldHeading = 180.0)
        assertEquals(180.0, withNothingElse.heading())
    }
}
