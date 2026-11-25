package org.scottishtecharmy.soundscape.geoengine.utils

import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.geoengine.headtracking.CalibrationSource
import org.scottishtecharmy.soundscape.geoengine.headtracking.HeadphoneCalibration
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.GpxLocationStream
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.parseGpx
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingDebug
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingStatus
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.locationprovider.StepSnapshot
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
     * Fixes with no time can't be placed in [GpxRecorder.window], so [GpxRecorder.maxBufferSize]
     * is what stops the buffer growing without bound: past it, the oldest entries go.
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

    private fun fixAt(millis: Long, index: Int) =
        SoundscapeLocation(latitude = index.toDouble(), timestampMilliseconds = millis)

    private fun latitudes(gpx: String) =
        Regex("<trkpt lat=\"([^\"]+)\"").findAll(gpx).map { it.groupValues[1].toDouble() }.toList()

    /**
     * The point of keeping the buffer by time: Android's fused provider sends a fix about every
     * step while walking, and 70 minutes of that at two a second must still leave the last hour,
     * not the last half hour that a 3600-fix buffer held.
     */
    @Test
    fun keepsTheLastHourWhateverTheFixRate() = runTest {
        val recorder = GpxRecorder()
        val start = 1_700_000_000_000L
        val count = 70 * 60 * 2
        for (i in 0 until count) recorder.storeLocation(fixAt(start + i * 500L, i))

        val kept = latitudes(recorder.generateGpx())
        val newest = start + (count - 1) * 500L
        // Everything from exactly an hour before the newest fix onwards, and nothing older
        val firstKept = ((newest - recorder.window.inWholeMilliseconds - start) / 500L).toInt()
        assertEquals(count - firstKept, kept.size)
        assertEquals(firstKept.toDouble(), kept.first())
        assertEquals((count - 1).toDouble(), kept.last())
        assertTrue(kept.size > 3600, "two fixes a second for an hour is more than 3600 fixes")
    }

    /** A slow provider is no longer cut short either: an hour of fixes 2 s apart all stay. */
    @Test
    fun aSlowProviderKeepsItsWholeHour() = runTest {
        val recorder = GpxRecorder()
        val start = 1_700_000_000_000L
        for (i in 0..1800) recorder.storeLocation(fixAt(start + i * 2000L, i))

        assertEquals(1801, latitudes(recorder.generateGpx()).size)
    }

    /**
     * Head tracking goes in an `<extensions>` element, which a replay must read past without it
     * changing the fix - so recordings made while debugging a head tracker still replay.
     */
    @Test
    fun headTrackingIsRecordedAsAnExtensionThatReplaysIgnore() = runTest {
        val location = SoundscapeLocation(
            latitude = 55.9,
            longitude = -4.3,
            bearing = 90.0f,
            speed = 1.2f,
            hasBearing = true,
            hasSpeed = true,
            timestampMilliseconds = 60_000L,
        )
        val plain = GpxRecorder().apply { storeLocation(location) }.generateGpx()
        val recorder = GpxRecorder()
        recorder.headTracking = {
            HeadTrackingDebug(
                status = HeadTrackingStatus.Calibrated,
                yawDegrees = -412.34,
                offsetDegrees = 142.06,
                headingDegrees = 89.7,
                phoneHeadingDegrees = 271.0,
                phoneFlat = false,
                courseDegrees = 90.0,
                calibrations = listOf(
                    HeadphoneCalibration(142.06, 3.21, 59_000L, CalibrationSource.Course)
                ),
            )
        }
        recorder.storeLocation(location)
        val gpx = recorder.generateGpx()

        assertTrue(gpx.contains("<status>Calibrated</status>"))
        assertTrue(gpx.contains("<yaw>-412.3</yaw>"))
        assertTrue(gpx.contains("<offset>142.1</offset>"))
        assertTrue(gpx.contains("<phoneFlat>false</phoneFlat>"))
        assertTrue(gpx.contains("<source>Course</source>"))
        assertTrue(gpx.contains("<time>1970-01-01T00:00:59.000Z</time>"))
        assertEquals(parseGpx(plain), parseGpx(gpx))
    }

    @Test
    fun stepsAreRecordedAsAnExtensionAndReadBack() = runTest {
        val location = SoundscapeLocation(
            latitude = 55.9,
            longitude = -4.3,
            timestampMilliseconds = 60_000L,
        )
        val plain = GpxRecorder().apply { storeLocation(location) }.generateGpx()
        val recorder = GpxRecorder()
        recorder.steps = {
            StepSnapshot(
                detectedSteps = 12,
                lastStepMillis = 59_500L,
                counterSteps = 10,
                counterMillis = 58_000L,
                cadence = 1.876,
            )
        }
        recorder.storeLocation(location)
        val gpx = recorder.generateGpx()

        assertEquals(1, Regex("<extensions>").findAll(gpx).count())
        assertTrue(gpx.contains("<detected>12</detected>"))
        assertTrue(gpx.contains("<lastStep>1970-01-01T00:00:59.500Z</lastStep>"))
        assertTrue(gpx.contains("<counter>10</counter>"))
        assertTrue(gpx.contains("<counterTime>1970-01-01T00:00:58.000Z</counterTime>"))
        assertTrue(gpx.contains("<cadence>1.88</cadence>"))

        // Read back onto the track point, and nothing else about the point changes
        val parsed = parseGpx(gpx)
        val point = parsed.tracks.single().trackSegments.single().trackPoints.single()
        assertEquals(
            StepSnapshot(
                detectedSteps = 12,
                lastStepMillis = 59_500L,
                counterSteps = 10,
                counterMillis = 58_000L,
                cadence = 1.88,
            ),
            point.steps,
        )
        val plainPoint = parseGpx(plain).tracks.single().trackSegments.single().trackPoints.single()
        assertEquals(plainPoint, point.copy(steps = null))
    }
}
