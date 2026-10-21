@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.callouts

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * An area POI - a park, a university campus - is announced once when the user reaches it, and
 * not again for as long as they stay in or beside it. Its history entry used to be forgotten by
 * time alone, so walking around Lennox Park, or through St Andrews, brought "At Lennox Park" back
 * every time the POI history expired.
 */
class AreaPoiCalloutTest {

    private val parkCentre = LngLatAlt(-4.316, 55.941)
    private val expiryMs = readCalloutVerbosity(null).poiHistoryExpiryMs

    /** A 400m square park around [parkCentre]. */
    private fun parkGrid(): GridState {
        val grid = GridState().apply { validateContext = false }
        val ring = arrayListOf(
            corner(315.0), corner(45.0), corner(135.0), corner(225.0), corner(315.0)
        )
        val park = MvtFeature().apply {
            name = "Lennox Park"
            superCategory = SuperCategoryId.LANDMARK
            geometry = Polygon(ring)
        }
        grid.featureTrees[TreeId.SELECTED_SUPER_CATEGORIES.id] =
            FeatureTree(FeatureCollection().apply { addFeature(park) })
        return grid
    }

    // 200m north/south/east/west of the centre, so the square's corners are 200*sqrt(2) away.
    private fun corner(bearing: Double) = getDestinationCoordinate(parkCentre, bearing, 282.8)

    private fun fix(location: LngLatAlt, timestamp: Long) = UserGeometry(
        location = location,
        phoneHeading = 0.0,
        speed = 1.0,
        timestampMilliseconds = timestamp,
    )

    /**
     * Runs the fixes through one AutoCallout as GeoEngine would, recording each callout in its
     * history and filter the way speaking it does, and returns the times "Lennox Park" was named.
     */
    private fun parkCallouts(fixes: List<UserGeometry>): Int {
        val autoCallout = AutoCallout(null, null)
        val grid = parkGrid()
        var count = 0
        for (fix in fixes) {
            val callout = autoCallout.updateLocation(fix, grid, GridState()) ?: continue
            callout.calloutHistory?.add(callout)
            callout.locationFilter?.update(callout.userGeometry)
            count += callout.positionedStrings.count { it.text.contains("Lennox Park") }
        }
        return count
    }

    /** A walk in circles around the middle of the park, a fix every 10s, lasting [durationMs]. */
    private fun walkInsideThePark(startMs: Long, durationMs: Long): List<UserGeometry> =
        (0..durationMs / 10_000).map { i ->
            val location = getDestinationCoordinate(parkCentre, (i * 10.0) % 360.0, 100.0)
            fix(location, startMs + i * 10_000)
        }

    @Test
    fun walkingAroundInsideAParkAnnouncesItOnce() {
        val fixes = walkInsideThePark(1_000L, 4 * expiryMs)
        assertEquals(1, parkCallouts(fixes))
    }

    /**
     * Sitting on a bench moves the user less than the POI filter's 5m, so the POI pass doesn't
     * run at all until they get up again. By then the history entry has run out, and it has to be
     * kept fresh before the trim, not after it.
     */
    @Test
    fun gettingUpFromABenchDoesNotAnnounceTheParkAgain() {
        val bench = getDestinationCoordinate(parkCentre, 0.0, 50.0)
        val sitting = (0..(2 * expiryMs) / 10_000).map { i -> fix(bench, 1_000L + i * 10_000) }
        val walkingOn = walkInsideThePark(sitting.last().timestampMilliseconds + 10_000, 60_000)
        assertEquals(1, parkCallouts(sitting + walkingOn))
    }

    /** Away from the park for longer than the expiry, coming back to it announces it again. */
    @Test
    fun returningAfterBeingAwayAnnouncesItAgain() {
        val first = walkInsideThePark(1_000L, 60_000)
        val away = getDestinationCoordinate(parkCentre, 90.0, 1000.0)
        val awayFixes = (1..(expiryMs + 60_000) / 10_000).map { i ->
            fix(away, first.last().timestampMilliseconds + i * 10_000)
        }
        val back = walkInsideThePark(awayFixes.last().timestampMilliseconds + 10_000, 60_000)
        assertEquals(2, parkCallouts(first + awayFixes + back))
    }
}
