package org.scottishtecharmy.soundscape.locationprovider

import org.scottishtecharmy.soundscape.locationprovider.bleimu.BleImuHeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.bose.BoseFramesHeadTrackingProvider

/** The input id of the AirPods tracker, which is iOS only and records its yaw (`Y` events). */
const val AIRPODS_INPUT_ID = "airpods"

/** The AirPods' device calibration window: 200 samples at ~25 Hz, about 8 seconds. */
const val AIRPODS_DEVICE_WINDOW_SIZE = 200

/** A head tracker's state after one replayed sample. */
data class HeadTrackerReplayRow(
    val device: String,
    val timeMillis: Long,
    val status: HeadTrackingStatus,
    val yawDegrees: Double?,
    val offsetDegrees: Double?,
    val headingDegrees: Double?,
)

/**
 * One GPX track point's head tracking: each tracker's input recorded since the previous point,
 * and the tracker the point describes with the heading it had reached when the point was written.
 */
data class RecordedHeadTrackerInput(
    val inputs: Map<String, String>,
    val device: String?,
    val recordedHeadingDegrees: Double?,
)

private val HEAD_TRACKING_BLOCK = Regex("<headTracking>(.*?)</headTracking>", RegexOption.DOT_MATCHES_ALL)
// Recordings from before more than one tracker was recorded have no device attribute, and were
// all from a WitMotion
private val INPUT = Regex("<input(?: device=\"([^\"]*)\")?>\n?(.*?)</input>", RegexOption.DOT_MATCHES_ALL)
private val HEADING = Regex("<heading>([^<]*)</heading>")
private val DEVICE = Regex("<device>([^<]*)</device>")

/** The head tracker input in a GpxRecorder recording, one entry per track point that has any. */
fun extractHeadTrackerInput(gpx: String): List<RecordedHeadTrackerInput> =
    HEAD_TRACKING_BLOCK.findAll(gpx).mapNotNull { block ->
        val body = block.groupValues[1]
        val inputs = INPUT.findAll(body).associate { match ->
            match.groupValues[1].ifEmpty { BleImuHeadTrackingProvider.INPUT_ID } to match.groupValues[2]
        }
        if (inputs.isEmpty()) return@mapNotNull null
        // The block's own fields come before any calibration's or input, and those have neither
        val own = body.substringBefore("<calibration>").substringBefore("<input")
        RecordedHeadTrackerInput(
            inputs = inputs,
            device = DEVICE.find(own)?.groupValues?.get(1) ?: inputs.keys.singleOrNull(),
            recordedHeadingDegrees = HEADING.find(own)?.groupValues?.get(1)?.toDoubleOrNull(),
        )
    }.toList()

private val TRACK_POINT = Regex("<trkpt .*?</trkpt>", RegexOption.DOT_MATCHES_ALL)
private val SPEED = Regex("<speed>([^<]*)</speed>")
private val BEARING = Regex("<bearing>([^<]*)</bearing>")
private val BEARING_ACCURACY = Regex("<bearingAccuracyDegrees>([^<]*)</bearingAccuracyDegrees>")

/**
 * Recordings made before the head trackers used bearing accuracy have `L` events with no accuracy
 * field. The fix each came from is a track point in the same file - filtering only moves the
 * position, so its bearing and speed are written identically - so the accuracy can be found from
 * there. Fills it in on every `L` event that can be matched, and `-` on any that can't.
 */
fun addMissingBearingAccuracy(gpx: String, recorded: List<RecordedHeadTrackerInput>): List<RecordedHeadTrackerInput> {
    val accuracies = HashMap<String, String>()
    for (point in TRACK_POINT.findAll(gpx)) {
        val fix = point.value.substringBefore("<extensions>")
        val bearing = BEARING.find(fix)?.groupValues?.get(1) ?: continue
        val speed = SPEED.find(fix)?.groupValues?.get(1) ?: continue
        val accuracy = BEARING_ACCURACY.find(fix)?.groupValues?.get(1) ?: continue
        accuracies["$bearing $speed"] = accuracy
    }
    return recorded.map { point ->
        point.copy(inputs = point.inputs.mapValues { (_, input) ->
            input.lineSequence().joinToString("\n") { line ->
                val fields = line.split(' ')
                if (fields.size == 4 && fields[0] == "L") {
                    "$line ${accuracies["${fields[2]} ${fields[3]}"] ?: "-"}"
                } else {
                    line
                }
            }
        })
    }
}

/**
 * Runs head tracker input recorded by [HeadTrackerInputLog] through fresh trackers, which act on
 * it exactly as the live ones did: each tracker's input goes through a provider of its own kind.
 * Feed it each track point's input in order.
 */
class HeadTrackerReplay {
    private val providers = mutableMapOf<String, CalibratedHeadTrackingProvider>()
    private val connected = mutableSetOf<String>()

    /** Gaps (`G` events) seen so far: after one, the replay may not match the recording. */
    var gaps = 0
        private set

    /** Whether [device] has been seen connecting, from which point its replay matches. */
    fun sawConnect(device: String) = device in connected

    fun headingDegrees(device: String): Double? = providers[device]?.headHeadingFlow?.value?.degrees

    fun feed(point: RecordedHeadTrackerInput, onRow: (HeadTrackerReplayRow) -> Unit = {}) {
        for ((device, input) in point.inputs) {
            val provider = providers.getOrPut(device) {
                createProvider(device).also { it.startReplay() }
            }
            for (line in input.lineSequence()) {
                if (line.isBlank()) continue
                val fields = line.split(' ')
                when (fields[0]) {
                    "S" -> connected.add(device)
                    "G" -> gaps++
                }
                if (provider.replay(fields)) {
                    onRow(
                        HeadTrackerReplayRow(
                            device = device,
                            timeMillis = fields[1].toLong(),
                            status = provider.statusFlow.value,
                            yawDegrees = provider.replayYawDegrees,
                            offsetDegrees = provider.replayOffsetDegrees,
                            headingDegrees = provider.headHeadingFlow.value?.degrees,
                        )
                    )
                }
            }
        }
    }

    private fun createProvider(device: String): CalibratedHeadTrackingProvider {
        val direction = DirectionProvider()
        val location = noLocation()
        return when (device) {
            BleImuHeadTrackingProvider.INPUT_ID -> BleImuHeadTrackingProvider(direction, location)
            BoseFramesHeadTrackingProvider.INPUT_ID -> BoseFramesHeadTrackingProvider(direction, location)
            AIRPODS_INPUT_ID -> yawProvider(AIRPODS_INPUT_ID)
            else -> error("A recording from an unknown head tracker: $device")
        }
    }

    /**
     * Stands in for a tracker that records its yaw rather than its raw data, such as the AirPods,
     * whose own provider can only run on iOS.
     */
    internal class YawReplayProvider(inputId: String, clock: () -> Long) : CalibratedHeadTrackingProvider(
        DirectionProvider(), noLocation(), inputId, AIRPODS_DEVICE_WINDOW_SIZE, clock,
    ) {
        override val isRunning = false
        override fun start() {}
        override fun stop() {}
        fun feedYaw(yawDegrees: Double, nowMillis: Long) = onYawSample(yawDegrees, nowMillis)
    }

    internal companion object {
        fun yawProvider(inputId: String, clock: () -> Long = { 0L }) = YawReplayProvider(inputId, clock)

        /** A LocationProvider for a provider that is only ever replayed, and never reads it. */
        fun noLocation(): LocationProvider = object : LocationProvider() {
            override fun start(accuracy: Accuracy) {}
            override fun destroy() {}
        }
    }
}
