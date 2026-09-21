package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.geoengine.filters.KalmanLocationFilter
import org.scottishtecharmy.soundscape.geoengine.utils.GpxRecorder
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.GpxLocationStream
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.parseGpx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A recording is only worth sending in if replaying it puts the recorded numbers through the same
 * arithmetic the journey put them through. These cover the half of that which is ours to control:
 * what the recorder writes, and what a replay makes of it.
 */
class GpxReplayTest {

    /** A jittery walk: consecutive fixes a few metres apart, a second apart, 10m accuracy. */
    private fun walk(count: Int = 20): List<SoundscapeLocation> =
        (0 until count).map { i ->
            SoundscapeLocation(
                latitude = 55.9474 + (i * 0.00002) + (if (i % 2 == 0) 0.00003 else -0.00003),
                longitude = -4.3058 + (i * 0.00001),
                accuracy = 10.0f,
                bearing = 90.0f,
                bearingAccuracyDegrees = 15.0f,
                speed = 1.4f,
                hasAccuracy = true,
                hasBearing = true,
                hasBearingAccuracy = true,
                hasSpeed = true,
                timestampMilliseconds = 1_757_000_000_000L + (i * 1000L),
            )
        }

    private suspend fun recordAndReparse(fixes: List<SoundscapeLocation>) =
        GpxRecorder().let { recorder ->
            fixes.forEach { recorder.storeLocation(it) }
            parseGpx(recorder.generateGpx())
        }

    @Test
    fun recordingsAreStampedWithTheRecorderVersionAndTheStreamTheyHold() = runTest {
        val gpx = recordAndReparse(walk(2))

        assertEquals(GpxRecorder.RECORDER_VERSION, gpx.recorderVersion)
        assertEquals(GpxLocationStream.RAW, gpx.locationStream)
    }

    /**
     * Everything written before the marker existed holds Kalman-filtered positions, so an
     * unmarked file has to be read as such - filtering it a second time would smooth an
     * already-smoothed track.
     */
    @Test
    fun aFileWithNoMarkerIsReadAsAV1FilteredRecording() {
        val gpx = parseGpx(
            "<?xml version='1.0' encoding='utf-8'?>\n" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" version=\"1.0\" " +
                "creator=\"Soundscape\">\n" +
                "<trk><trkseg>\n" +
                "<trkpt lat=\"55.9\" lon=\"-4.3\"><time>2026-09-14T07:11:41.240Z</time></trkpt>\n" +
                "</trkseg></trk></gpx>"
        )

        assertEquals(null, gpx.recorderVersion)
        assertEquals(GpxLocationStream.FILTERED, gpx.locationStream)
    }

    /**
     * The point of the whole exercise: a fix replayed out of a recording reaches
     * filteredLocationFlow at exactly the position it reached it at live.
     *
     * Both sides here run KalmanLocationFilter, but only by construction - the live side through
     * the helper every real provider calls, the replay side through the provider the app and the
     * MvtTileTest replays share. If either grows its own arithmetic, or the recorder stops
     * round-tripping a field the filter reads (position, accuracy, capture time), this fails.
     */
    @Test
    fun replayingARecordingReproducesTheFilteredPositionsTheJourneyProduced() = runTest {
        val fixes = walk()

        val liveFilter = KalmanLocationFilter()
        val live = fixes.map { liveFilter.filterPosition(it) }

        val gpx = recordAndReparse(fixes)
        val replayProvider = GpxReplayLocationProvider(gpx.locationStream)
        val replayed = gpx.tracks.first().trackSegments.first().trackPoints.map { point ->
            replayProvider.updateLocation(point.toSoundscapeLocation())
            replayProvider.filteredLocationFlow.value!!
        }

        assertEquals(live.size, replayed.size)
        live.forEachIndexed { i, expected ->
            assertEquals(expected.latitude, replayed[i].latitude, "latitude at $i")
            assertEquals(expected.longitude, replayed[i].longitude, "longitude at $i")
        }
    }

    /**
     * ...and the unfiltered stream survives alongside it. MapMatchFilter and StationaryDetector
     * are handed this one on purpose (see GeoEngine.startMonitoringLocation), so a replay that
     * lost it would be feeding them a smoothed track they never saw live.
     */
    @Test
    fun aRawRecordingReplaysBothStreamsAndTheyDiffer() = runTest {
        val fixes = walk()
        val gpx = recordAndReparse(fixes)
        val provider = GpxReplayLocationProvider(gpx.locationStream)

        var sawADifference = false
        gpx.tracks.first().trackSegments.first().trackPoints.forEachIndexed { i, point ->
            provider.updateLocation(point.toSoundscapeLocation())
            val raw = provider.locationFlow.value!!
            val filtered = provider.filteredLocationFlow.value!!

            // The unfiltered stream is the recording, untouched.
            assertEquals(fixes[i].latitude, raw.latitude, "raw latitude at $i")
            assertEquals(fixes[i].longitude, raw.longitude, "raw longitude at $i")

            if (filtered.latitude != raw.latitude) sawADifference = true
            // Only the position is filtered - the rest of the fix describes what the receiver
            // reported and has to reach both streams unchanged.
            assertEquals(raw.accuracy, filtered.accuracy)
            assertEquals(raw.bearing, filtered.bearing)
            assertEquals(raw.speed, filtered.speed)
            assertEquals(raw.timestampMilliseconds, filtered.timestampMilliseconds)
        }
        assertTrue(sawADifference, "the filter never moved a position - was it applied at all?")
    }

    /**
     * A v1 recording has only the one stream left in it, so both flows carry the recorded point.
     * Filtering it again would be the double-smoothing this marker exists to prevent.
     */
    @Test
    fun aFilteredRecordingIsReplayedUnchangedOnBothStreams() = runTest {
        val fixes = walk()
        val gpx = recordAndReparse(fixes)
        val provider = GpxReplayLocationProvider(GpxLocationStream.FILTERED)

        gpx.tracks.first().trackSegments.first().trackPoints.forEachIndexed { i, point ->
            provider.updateLocation(point.toSoundscapeLocation())
            val raw = provider.locationFlow.value!!
            val filtered = provider.filteredLocationFlow.value!!

            assertEquals(fixes[i].latitude, raw.latitude, "raw latitude at $i")
            assertEquals(raw.latitude, filtered.latitude, "filtered latitude at $i")
            assertEquals(raw.longitude, filtered.longitude, "filtered longitude at $i")
        }
    }

    /**
     * The fields the filter reads have to survive the write/read round trip exactly, or the two
     * sides of the test above would agree only by luck.
     */
    @Test
    fun theFieldsTheFilterReadsRoundTripExactly() = runTest {
        val fixes = walk(5)
        val gpx = recordAndReparse(fixes)

        val points = gpx.tracks.first().trackSegments.first().trackPoints
        assertEquals(fixes.size, points.size)
        points.forEachIndexed { i, point ->
            val fix = point.toSoundscapeLocation()
            assertEquals(fixes[i].latitude, fix.latitude, "latitude at $i")
            assertEquals(fixes[i].longitude, fix.longitude, "longitude at $i")
            assertEquals(fixes[i].accuracy, fix.accuracy, "accuracy at $i")
            assertEquals(
                fixes[i].timestampMilliseconds,
                fix.timestampMilliseconds,
                "timestamp at $i"
            )
        }
    }

    /**
     * A fix whose receiver reported no bearing or speed must come back as such, not as a fix
     * that was pointed due north and standing still - GeoEngine steers audio by a bearing it
     * believes. See GpxRecorder's has* handling.
     */
    @Test
    fun aFixThatCarriedNoBearingOrSpeedReplaysWithThoseFlagsClear() = runTest {
        val gpx = recordAndReparse(
            listOf(
                SoundscapeLocation(
                    latitude = 55.47,
                    longitude = -4.62,
                    accuracy = 77.4f,
                    hasAccuracy = true,
                    hasBearing = false,
                    hasSpeed = false,
                    timestampMilliseconds = 1_757_000_000_000L,
                )
            )
        )

        val fix = gpx.tracks.first().trackSegments.first().trackPoints.first().toSoundscapeLocation()

        assertTrue(fix.hasAccuracy)
        assertEquals(77.4f, fix.accuracy)
        // Omitted from the file, so the replay rebuilds the fix with the flags clear and
        // GeoEngine leaves travelHeading null rather than steering audio due north.
        assertFalse(fix.hasBearing)
        assertFalse(fix.hasBearingAccuracy)
        assertFalse(fix.hasSpeed)
    }
}
