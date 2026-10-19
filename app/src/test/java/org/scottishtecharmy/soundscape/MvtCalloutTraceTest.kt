package org.scottishtecharmy.soundscape

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.callouts.forceLegacyIntersectionSearch

/**
 * The single-trace callout test, in a class of its own as it takes about as long as the rest of
 * MvtTileTest together: a class is the unit the test JVMs (-PtestForks) share out, so this lets
 * it run alongside the others rather than after them.
 */
class MvtCalloutTraceTest {

    /**
     * One trace through the whole callout path, written out as text and as GeoJSON for eyeballing
     * on geojson.io - each callout is the field-of-view triangle that produced it, carrying its
     * spoken lines as properties.
     *
     * A walk rather than a journey: AutoCallout suppresses intersection callouts in a vehicle and
     * for a while afterwards, so a drive or a train exercises everything here except the
     * pedestrian callouts, which are most of them. CentralToBuchananStreet walks through the
     * middle of Glasgow and produces getting on for two hundred callouts, around a quarter of
     * them about junctions.
     *
     * MvtTileTest.testCallouts still covers every trace, vehicles and trains included, nightly.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCalloutsSingleTest() {
        val testFile = "bristol-bus"
        MvtTileTest().testMovingGrid(
            "src/test/res/org/scottishtecharmy/soundscape/gpxFiles/$testFile.gpx",
            "gpxFiles/$testFile.txt",
            "gpxFiles/$testFile.geojson"
        )

        // And again through the old tree-search + Dijkstra intersection path, to diff against.
        forceLegacyIntersectionSearch = true
        try {
            MvtTileTest().testMovingGrid(
                "src/test/res/org/scottishtecharmy/soundscape/gpxFiles/$testFile.gpx",
                "gpxFiles/$testFile-legacy.txt",
                "gpxFiles/$testFile-legacy.geojson"
            )
        } finally {
            forceLegacyIntersectionSearch = false
        }
    }
}
