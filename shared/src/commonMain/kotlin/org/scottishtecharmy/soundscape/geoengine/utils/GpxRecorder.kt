package org.scottishtecharmy.soundscape.geoengine.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.scottishtecharmy.soundscape.geoengine.LocationRecorder
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingDebug
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.platform.formatGpxTimestamp

class GpxRecorder : LocationRecorder {

    val bufferMutex = Mutex()
    val maxBufferSize = 3600        // 1 location per second for an hour
    private val buffer: MutableList<Pair<SoundscapeLocation, HeadTrackingDebug?>> = mutableListOf()

    /**
     * Gives the head tracker's state to record with each fix, or null when there's no tracker
     * running. Written as an `<extensions>` element, which GpxParser skips, so a replay is
     * unaffected and RECORDER_VERSION stays as it is.
     */
    var headTracking: (() -> HeadTrackingDebug?)? = null

    suspend fun generateGpx(): String {
        val builder = StringBuilder()
        builder.append(GPX_HEADER)
        bufferMutex.withLock {
            for ((location, head) in buffer) {
                builder.append("<trkpt lat=\"").append(location.latitude)
                    .append("\" lon=\"").append(location.longitude).append("\">\n")
                builder.append("<ele>0.0</ele>\n")
                // A field the fix didn't carry is left out altogether rather than written as a
                // placeholder, because GpxParser reports an absent element as null and a present
                // one as a measurement - the only way a replay can rebuild the has* flags, and so
                // the only way it can tell a receiver that reported due north from one that
                // reported no bearing at all. The geoengine steers audio by a bearing it believes
                // (see GeoEngine.createUserGeometry), so the difference is audible.
                //
                // Writing the value through would be worse still on iOS, where CoreLocation
                // signals an absent course or speed with -1 rather than with a separate flag: the
                // first iOS recording we were sent was a track of -1m/s at a bearing of -1
                // degrees, every point of it.
                appendIfPresent(builder, "accuracy", location.hasAccuracy, location.accuracy)
                appendIfPresent(builder, "speed", location.hasSpeed, location.speed)
                appendIfPresent(builder, "bearing", location.hasBearing, location.bearing)
                appendIfPresent(
                    builder,
                    "bearingAccuracyDegrees",
                    location.hasBearingAccuracy,
                    location.bearingAccuracyDegrees
                )
                builder.append("<time>")
                    .append(formatGpxTimestamp(location.timestampMilliseconds))
                    .append("</time>\n")
                head?.let { appendHeadTracking(builder, it) }
                builder.append("</trkpt>\n")
            }
        }
        builder.append(GPX_FOOTER)
        return builder.toString()
    }

    private fun appendHeadTracking(builder: StringBuilder, head: HeadTrackingDebug) {
        builder.append("<extensions>\n<headTracking>\n")
        builder.append("<status>").append(head.status.name).append("</status>\n")
        head.device?.let { builder.append("<device>").append(it).append("</device>\n") }
        appendDegrees(builder, "heading", head.headingDegrees)
        appendDegrees(builder, "yaw", head.yawDegrees)
        appendDegrees(builder, "offset", head.offsetDegrees)
        appendDegrees(builder, "phoneHeading", head.phoneHeadingDegrees)
        builder.append("<phoneFlat>").append(head.phoneFlat).append("</phoneFlat>\n")
        appendDegrees(builder, "course", head.courseDegrees)
        for (calibration in head.calibrations) {
            builder.append("<calibration>\n")
            builder.append("<source>").append(calibration.source.name).append("</source>\n")
            appendDegrees(builder, "offset", calibration.offsetDegrees)
            appendDegrees(builder, "accuracy", calibration.accuracyDegrees)
            builder.append("<time>")
                .append(formatGpxTimestamp(calibration.timestampMillis))
                .append("</time>\n")
            builder.append("</calibration>\n")
        }
        for ((device, input) in head.inputs) {
            builder.append("<input device=\"").append(device).append("\">\n")
                .append(input).append("</input>\n")
        }
        builder.append("</headTracking>\n</extensions>\n")
    }

    // To a tenth of a degree, left out when there's no value, as for the fix's own fields
    private fun appendDegrees(builder: StringBuilder, element: String, value: Double?) {
        if (value == null) return
        builder.append("<").append(element).append(">")
            .append(kotlin.math.round(value * 10.0) / 10.0)
            .append("</").append(element).append(">\n")
    }

    private fun appendIfPresent(
        builder: StringBuilder,
        element: String,
        present: Boolean,
        value: Float
    ) {
        if (!present) return
        builder.append("<").append(element).append(">")
            .append(value)
            .append("</").append(element).append(">\n")
    }

    override suspend fun storeLocation(location: SoundscapeLocation) {
        bufferMutex.withLock {
            buffer.add(location to headTracking?.invoke())
            if (buffer.size > maxBufferSize)
                buffer.removeAt(0)
        }
    }

    companion object {
        /**
         * Stamped on every recording as the `recorderVersion` attribute, and absent from files
         * written before it existed.
         *
         * 1. (unstamped) Track points held the Kalman-filtered position, and wrote the
         *    provider's raw sentinels - a course of -1 - straight through.
         * 2. Track points hold the unfiltered fix, so that a replay can reconstruct both of the
         *    streams the geoengine runs on rather than only the smoothed one. Fields the fix
         *    didn't carry are written as zero rather than as a sentinel.
         * 3. bearingAccuracyDegrees is on Android's scale on every platform: iOS recordings carry
         *    CoreLocation's courseAccuracy halved, the value the geoengine judged the course by -
         *    see IosLocationProvider's COURSE_ACCURACY_SCALE. A version 2 recording from an
         *    iPhone carries it unscaled.
         *
         * Bump this whenever what a track point means changes, so a replay of an older file can
         * keep treating it the way it was written - see GpxLocationStream.
         */
        const val RECORDER_VERSION = 3

        private const val GPX_HEADER =
            "<?xml version='1.0' encoding='utf-8'?>\n" +
                    "<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" version=\"1.0\" " +
                    "creator=\"Soundscape\" recorderVersion=\"$RECORDER_VERSION\" " +
                    "locationStream=\"raw\">\n" +
                    "<trk>\n" +
                    "<name>Track 0</name>\n" +
                    "<number>0</number>\n" +
                    "<trkseg>\n"

        private const val GPX_FOOTER =
            "</trkseg>\n" +
                    "</trk>\n" +
                    "</gpx>"
    }
}
