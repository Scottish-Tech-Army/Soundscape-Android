package org.scottishtecharmy.soundscape.locationprovider

import org.scottishtecharmy.soundscape.locationprovider.bleimu.BleImuHeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.bose.BoseFramesHeadTrackingProvider
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeadTrackerReplayTest {

    /** A WitMotion IMU frame: flat and still, or turning about the vertical at [gyroZDps]. */
    private fun witMotionFrame(gyroZDps: Double): ByteArray {
        val values = intArrayOf(
            0, 0, 2048, // accel: 1 g up
            0, 0, (gyroZDps * 32768 / 2000).roundToInt(),
            0, 0, 0,
        )
        val bytes = ByteArray(20)
        bytes[0] = 0x55
        bytes[1] = 0x61
        values.forEachIndexed { i, v ->
            bytes[2 + i * 2] = (v and 0xFF).toByte()
            bytes[3 + i * 2] = (v shr 8).toByte()
        }
        return bytes
    }

    /** A Bose Frames notification holding one game-rotation quaternion turned [yawDegrees]. */
    private fun boseFrame(yawDegrees: Double): ByteArray {
        val half = yawDegrees * kotlin.math.PI / 360.0
        // Quaternion (x, y, z, w) about Z, int16 / 2^14, big endian; sensor id 3 + u16 timestamp
        val q = listOf(0.0, 0.0, kotlin.math.sin(half), kotlin.math.cos(half)).map { (it * 16384).roundToInt() }
        val bytes = ByteArray(3 + 8)
        bytes[0] = 3
        q.forEachIndexed { i, v ->
            bytes[3 + i * 2] = (v shr 8).toByte()
            bytes[4 + i * 2] = (v and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Drives [live] through a synthetic session - still, then turning - while recording, and
     * returns each sample's yaw and heading with the recorded input.
     */
    private fun record(
        live: CalibratedHeadTrackingProvider,
        sample: (Int, Long) -> Unit,
    ): Pair<List<Pair<Double?, Double?>>, List<RecordedHeadTrackerInput>> {
        live.startReplay()
        assertNotNull(live.takeDebugSnapshot(includeInput = true)) // arms the input log
        val rows = mutableListOf<Pair<Double?, Double?>>()
        val recorded = mutableListOf<RecordedHeadTrackerInput>()
        fun snapshot() = live.takeDebugSnapshot(includeInput = true)!!.let {
            if (it.inputs.isNotEmpty()) recorded.add(RecordedHeadTrackerInput(it.inputs, it.device, it.headingDegrees))
        }
        var t = START_MILLIS
        live.onSessionConnected(t)
        // Phone flat (identity attitude) facing 100°
        live.onDirection(DeviceDirection(floatArrayOf(0f, 0f, 0f, 1f), 100f, 5f, 0L), t)
        for (i in 0 until 2_500) {
            t += 10
            if (i % 100 == 0) {
                live.onLocation(
                    SoundscapeLocation(
                        bearing = 100f + i / 100, speed = 1.3f, bearingAccuracyDegrees = 10f,
                        hasBearing = true, hasSpeed = true, hasBearingAccuracy = true,
                    ),
                    t,
                )
                snapshot()
            }
            sample(i, t)
            rows.add(live.replayYawDegrees to live.headHeadingFlow.value?.degrees)
        }
        snapshot()
        return rows to recorded
    }

    private companion object {
        // Ahead of any clock the providers read, as a live recording's times are
        const val START_MILLIS = 1_000_000L
    }

    private fun assertReplayMatches(
        device: String,
        rows: List<Pair<Double?, Double?>>,
        recorded: List<RecordedHeadTrackerInput>,
    ) {
        val replay = HeadTrackerReplay()
        val replayRows = mutableListOf<Pair<Double?, Double?>>()
        for (point in recorded) replay.feed(point) { replayRows.add(it.yawDegrees to it.headingDegrees) }

        assertTrue(replay.sawConnect(device))
        assertEquals(0, replay.gaps(device))
        assertNotNull(rows.last().second, "the live run should have calibrated")
        assertEquals(rows, replayRows)
        assertEquals(recorded.last().recordedHeadingDegrees, replay.headingDegrees(device))
    }

    @Test
    fun aWitMotionReplayReproducesTheLiveHeadingExactly() {
        val live = BleImuHeadTrackingProvider(DirectionProvider(), HeadTrackerReplay.noLocation())
        val (rows, recorded) = record(live) { i, t ->
            live.onBytes(witMotionFrame(if (i < 1_500) 0.0 else 10.0), t)
        }
        assertReplayMatches(BleImuHeadTrackingProvider.INPUT_ID, rows, recorded)
    }

    @Test
    fun aBoseFramesReplayReproducesTheLiveHeadingExactly() {
        val live = BoseFramesHeadTrackingProvider(DirectionProvider(), HeadTrackerReplay.noLocation())
        val (rows, recorded) = record(live) { i, t ->
            live.onBytes(boseFrame(if (i < 1_500) 30.0 else 30.0 + (i - 1_500) * 0.1), t)
        }
        assertReplayMatches(BoseFramesHeadTrackingProvider.INPUT_ID, rows, recorded)
    }

    @Test
    fun anAirPodsReplayReproducesTheLiveHeadingExactly() {
        // The AirPods' own provider is iOS only, so stand in with the replay's
        // with a clock in step with the samples, as the recording needs draining to keep going
        var now = START_MILLIS
        val live = HeadTrackerReplay.yawProvider(AIRPODS_INPUT_ID) { now }
        val (rows, recorded) = record(live) { i, t ->
            now = t
            live.feedYaw(if (i < 1_500) 30.0 else 30.0 + (i - 1_500) * 0.1, t)
        }
        assertReplayMatches(AIRPODS_INPUT_ID, rows, recorded)
    }

    @Test
    fun theInputIsOnlyRecordedWhenAskedFor() {
        val live = BleImuHeadTrackingProvider(DirectionProvider(), HeadTrackerReplay.noLocation())
        live.startReplay()
        live.takeDebugSnapshot(includeInput = true)
        live.onSessionConnected(1_000L)
        live.onBytes(witMotionFrame(0.0), 1_010L)
        // Turning recording off discards what it held, and nothing more is kept
        assertTrue(live.takeDebugSnapshot(includeInput = false)!!.inputs.isEmpty())
        live.onBytes(witMotionFrame(0.0), 1_020L)
        assertTrue(live.takeDebugSnapshot(includeInput = false)!!.inputs.isEmpty())
        assertTrue(live.takeDebugSnapshot(includeInput = true)!!.inputs.isEmpty())
        live.onBytes(witMotionFrame(0.0), 1_030L)
        assertTrue(live.takeDebugSnapshot(includeInput = true)!!.inputs.isNotEmpty())
    }

    @Test
    fun stoppingATrackerStopsItsRecordingWithoutAGap() {
        // As CompositeHeadTrackingProvider stops the others while one is connected
        var now = START_MILLIS
        val bose = BoseFramesHeadTrackingProvider(
            DirectionProvider(), HeadTrackerReplay.noLocation(), clock = { now },
        )
        bose.start()
        assertTrue(bose.takeDebugSnapshot(includeInput = true)!!.inputs.isEmpty()) // arms it
        bose.stop()
        assertNull(bose.takeDebugSnapshot(includeInput = true))
        now += 60_000L
        bose.start()
        // The phone heading arrives as soon as it restarts, before the recorder next asks
        bose.onDirection(null, now)
        bose.takeDebugSnapshot(includeInput = true)
        bose.onDirection(null, now + 1)
        val input = bose.takeDebugSnapshot(includeInput = true)!!.inputs.values.single()
        bose.stop()
        assertTrue(input.lines().none { it.startsWith("G ") }, input)
    }

    @Test
    fun theInputIsFoundInARecording() {
        val gpx = "<trkpt><time>x</time><extensions>\n<headTracking>\n<status>Calibrated</status>\n" +
            "<device>bose</device>\n<heading>12.5</heading>\n<calibration>\n<source>Course</source>\n" +
            "</calibration>\n<input device=\"witmotion\">\nL 4 -\n</input>\n" +
            "<input device=\"bose\">\nS 5\nB 6 03\n</input>\n</headTracking>\n</extensions></trkpt>"
        assertEquals(
            listOf(
                RecordedHeadTrackerInput(
                    inputs = mapOf("witmotion" to "L 4 -\n", "bose" to "S 5\nB 6 03\n"),
                    device = "bose",
                    recordedHeadingDegrees = 12.5,
                )
            ),
            extractHeadTrackerInput(gpx),
        )
    }

    @Test
    fun aRecordingFromBeforeDevicesWereNamedIsAWitMotions() {
        val gpx = "<headTracking>\n<heading>1.0</heading>\n<input>\nS 5\n</input>\n</headTracking>"
        assertEquals(
            listOf(RecordedHeadTrackerInput(mapOf("witmotion" to "S 5\n"), "witmotion", 1.0)),
            extractHeadTrackerInput(gpx),
        )
    }

    @Test
    fun snapshotsOfSeveralTrackersDescribeTheConnectedOneWithEveryonesInput() {
        val searching = HeadTrackingDebug(
            HeadTrackingStatus.Disconnected, null, null, null, null, false, null, emptyList(),
            device = "witmotion", inputs = mapOf("witmotion" to "L 1 -\n"),
        )
        val connected = searching.copy(
            status = HeadTrackingStatus.Calibrated, headingDegrees = 90.0,
            device = "bose", inputs = mapOf("bose" to "B 2 03\n"),
        )
        val merged = mergeDebugSnapshots(listOf(searching, connected))!!
        assertEquals("bose", merged.device)
        assertEquals(90.0, merged.headingDegrees)
        assertEquals(mapOf("witmotion" to "L 1 -\n", "bose" to "B 2 03\n"), merged.inputs)
        assertNull(mergeDebugSnapshots(emptyList()))
    }
}
