package org.scottishtecharmy.soundscape

import org.junit.Assume.assumeTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackerReplay
import org.scottishtecharmy.soundscape.locationprovider.addMissingBearingAccuracy
import org.scottishtecharmy.soundscape.locationprovider.extractHeadTrackerInput
import java.io.File
import kotlin.math.abs

/**
 * Replays the head tracker input in a travel recording and writes what each tracker did at every
 * sample to a CSV beside the recording. Skipped unless HEAD_TRACKER_GPX names a recording; see
 * scripts/replay-head-tracker.sh.
 */
class HeadTrackerReplayTool {

    @Test
    fun replayRecording() {
        val path = System.getenv("HEAD_TRACKER_GPX")
        assumeTrue("HEAD_TRACKER_GPX is not set", !path.isNullOrEmpty())
        val gpxFile = File(path!!)
        val gpx = gpxFile.readText()
        val recorded = addMissingBearingAccuracy(gpx, extractHeadTrackerInput(gpx))
        check(recorded.isNotEmpty()) { "$gpxFile has no head tracker input in it" }

        val replay = HeadTrackerReplay()
        val csv = File(gpxFile.parentFile, gpxFile.nameWithoutExtension + "-headtracker.csv")
        var rows = 0
        var compared = 0
        var mismatches = 0
        val devices = mutableSetOf<String>()
        csv.printWriter().use { out ->
            out.println("device,timeMillis,status,yaw,offset,heading")
            for (point in recorded) {
                replay.feed(point) { row ->
                    rows++
                    devices.add(row.device)
                    out.println(
                        "${row.device},${row.timeMillis},${row.status},${row.yawDegrees ?: ""}," +
                            "${row.offsetDegrees ?: ""},${row.headingDegrees ?: ""}"
                    )
                }
                // The recording rounds to a tenth of a degree
                val device = point.device ?: continue
                if (replay.sawConnect(device) && replay.gaps(device) == 0) {
                    val want = point.recordedHeadingDegrees
                    val got = replay.headingDegrees(device)
                    compared++
                    if ((want == null) != (got == null) ||
                        (want != null && got != null && abs(want - got) > 0.051)
                    ) {
                        mismatches++
                    }
                }
            }
        }
        println("Replayed $rows samples from ${devices.sorted().joinToString()} into $csv")
        for (device in devices.sorted()) {
            if (!replay.sawConnect(device)) {
                println("The recording starts after the $device connected, so its replay won't match")
            }
            val gaps = replay.gaps(device)
            if (gaps > 0) println("$gaps gap(s) in the $device input: it can't match after one")
        }
        println("Heading matched the recording at ${compared - mismatches} of $compared track points")
    }
}
