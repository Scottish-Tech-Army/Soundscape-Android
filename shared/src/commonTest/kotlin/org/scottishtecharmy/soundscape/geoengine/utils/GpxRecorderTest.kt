package org.scottishtecharmy.soundscape.geoengine.utils

import kotlinx.coroutines.test.runTest
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
        )
        val second = SoundscapeLocation(
            latitude = 51.6,
            longitude = -0.14,
            accuracy = 6.0f,
            bearing = 180.0f,
            bearingAccuracyDegrees = 15.0f,
            speed = 2.5f,
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
