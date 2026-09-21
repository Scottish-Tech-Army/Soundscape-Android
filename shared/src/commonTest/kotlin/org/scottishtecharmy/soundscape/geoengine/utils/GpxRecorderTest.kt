package org.scottishtecharmy.soundscape.geoengine.utils

import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.GpxLocationStream
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GpxRecorderTest {

    @Test
    fun emptyBufferProducesValidHeaderAndFooterWithNoTrackpoints() = runTest {
        val recorder = GpxRecorder()

        val gpx = recorder.generateGpx()

        assertTrue(gpx.startsWith("<?xml version='1.0' encoding='utf-8'?>"))
        assertTrue(gpx.contains("<gpx xmlns=\"http://www.topografix.com/GPX/1/0\""))
        assertTrue(gpx.contains("<trk>"))
        assertTrue(gpx.contains("<trkseg>"))
        assertFalse(gpx.contains("<trkpt"))
        assertTrue(gpx.trimEnd().endsWith("</gpx>"))
    }

    @Test
    fun storedLocationsAppearAsTrkptsWithCorrectFields() = runTest {
        val recorder = GpxRecorder()
        val first = SoundscapeLocation(
            latitude = 51.5,
            longitude = -0.15,
            accuracy = 5.0f,
            bearing = 90.0f,
            bearingAccuracyDegrees = 10.0f,
            speed = 1.5f,
            hasAccuracy = true,
            hasBearing = true,
            hasBearingAccuracy = true,
            hasSpeed = true,
        )
        val second = SoundscapeLocation(
            latitude = 51.6,
            longitude = -0.14,
            accuracy = 6.0f,
            bearing = 180.0f,
            bearingAccuracyDegrees = 15.0f,
            speed = 2.5f,
            hasAccuracy = true,
            hasBearing = true,
            hasBearingAccuracy = true,
            hasSpeed = true,
        )

        recorder.storeLocation(first)
        recorder.storeLocation(second)
        val gpx = recorder.generateGpx()

        // Exactly two trackpoints were recorded.
        val trkptCount = Regex("<trkpt ").findAll(gpx).count()
        assertEquals(2, trkptCount)

        assertTrue(gpx.contains("<trkpt lat=\"51.5\" lon=\"-0.15\">"))
        assertTrue(gpx.contains("<accuracy>5.0</accuracy>"))
        assertTrue(gpx.contains("<speed>1.5</speed>"))
        assertTrue(gpx.contains("<bearing>90.0</bearing>"))
        assertTrue(gpx.contains("<bearingAccuracyDegrees>10.0</bearingAccuracyDegrees>"))

        assertTrue(gpx.contains("<trkpt lat=\"51.6\" lon=\"-0.14\">"))
        assertTrue(gpx.contains("<accuracy>6.0</accuracy>"))
        assertTrue(gpx.contains("<speed>2.5</speed>"))
        assertTrue(gpx.contains("<bearing>180.0</bearing>"))
        assertTrue(gpx.contains("<bearingAccuracyDegrees>15.0</bearingAccuracyDegrees>"))
    }

    /**
     * A field the fix didn't carry is omitted, not written as a placeholder.
     *
     * CoreLocation reports an unavailable course or speed as -1 rather than by a separate flag,
     * so writing the value through put a bearing of -1 degrees in the file. Writing a zero
     * instead only trades one lie for a quieter one: GpxParser reads a present element as a
     * measurement, so the replay would rebuild the fix with hasBearing set and steer audio due
     * north. Absence is the only thing the format can say that means "the receiver didn't report
     * this", and it is what [GpxLocationStream]-aware replays read back.
     */
    @Test
    fun fieldsTheFixDidNotCarryAreOmittedRatherThanWrittenAsAPlaceholder() = runTest {
        val recorder = GpxRecorder()
        recorder.storeLocation(
            SoundscapeLocation(
                latitude = 55.47,
                longitude = -4.62,
                accuracy = 77.4f,
                bearing = -1.0f,
                bearingAccuracyDegrees = -1.0f,
                speed = -1.0f,
                hasAccuracy = true,
                hasBearing = false,
                hasBearingAccuracy = false,
                hasSpeed = false,
            )
        )

        val gpx = recorder.generateGpx()

        assertFalse(gpx.contains("<speed>"))
        assertFalse(gpx.contains("<bearing>"))
        assertFalse(gpx.contains("<bearingAccuracyDegrees>"))
        assertFalse(gpx.contains("-1.0"))
        // The one field the fix did carry is still reported as measured.
        assertTrue(gpx.contains("<accuracy>77.4</accuracy>"))
    }

    /**
     * The marker that tells a replay how to read the rest of the file - see
     * [GpxRecorder.RECORDER_VERSION] and [GpxLocationStream].
     */
    @Test
    fun theHeaderStampsTheRecorderVersionAndTheStreamTheTrackPointsHold() = runTest {
        val gpx = GpxRecorder().generateGpx()

        assertTrue(gpx.contains("recorderVersion=\"${GpxRecorder.RECORDER_VERSION}\""))
        assertTrue(gpx.contains("locationStream=\"raw\""))
    }

    @Test
    fun eachTrkptUsesItsOwnLocationsTimestampNotAGenerationTimeOne() = runTest {
        val recorder = GpxRecorder()
        val first = SoundscapeLocation(latitude = 1.0, timestampMilliseconds = 0L)
        val second = SoundscapeLocation(latitude = 2.0, timestampMilliseconds = 60_000L)

        recorder.storeLocation(first)
        recorder.storeLocation(second)
        val gpx = recorder.generateGpx()

        // Each trkpt carries the time its own location was actually recorded at, not a single
        // shared timestamp computed once when generateGpx() happens to be called.
        assertTrue(gpx.contains("<time>1970-01-01T00:00:00.000Z</time>"))
        assertTrue(gpx.contains("<time>1970-01-01T00:01:00.000Z</time>"))
    }

    /**
     * [GpxRecorder.storeLocation] evicts the oldest entry via `buffer.removeAt(0)` once
     * [GpxRecorder.maxBufferSize] is exceeded, capping the buffer at maxBufferSize (1 hour of
     * 1Hz fixes) rather than growing without bound.
     */
    @Test
    fun exceedingMaxBufferSizeEvictsOldestLocations() = runTest {
        val recorder = GpxRecorder()
        val total = recorder.maxBufferSize + 5

        for (i in 0 until total) {
            recorder.storeLocation(SoundscapeLocation(latitude = i.toDouble()))
        }
        val gpx = recorder.generateGpx()

        val trkptCount = Regex("<trkpt ").findAll(gpx).count()
        assertEquals(recorder.maxBufferSize, trkptCount)

        // The oldest 5 locations (indices 0-4) were evicted to make room...
        for (i in 0 until 5) {
            assertFalse(gpx.contains("<trkpt lat=\"${i.toDouble()}\" lon=\"0.0\">"))
        }
        // ...while the most recently stored location is still present.
        val lastIndex = total - 1
        assertTrue(gpx.contains("<trkpt lat=\"${lastIndex.toDouble()}\" lon=\"0.0\">"))
    }
}
