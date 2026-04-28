package org.scottishtecharmy.soundscape.geoengine.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.scottishtecharmy.soundscape.geoengine.LocationRecorder
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.platform.currentTimeMillis
import org.scottishtecharmy.soundscape.platform.formatGpxTimestamp

class GpxRecorder : LocationRecorder {

    val bufferMutex = Mutex()
    val maxBufferSize = 3600        // 1 location per second for an hour
    private val buffer: MutableList<SoundscapeLocation> = mutableListOf()

    suspend fun generateGpx(): String {
        val builder = StringBuilder()
        builder.append(GPX_HEADER)
        bufferMutex.withLock {
            val timestamp = formatGpxTimestamp(currentTimeMillis())
            for (location in buffer) {
                builder.append("<trkpt lat=\"").append(location.latitude)
                    .append("\" lon=\"").append(location.longitude).append("\">\n")
                builder.append("<ele>0.0</ele>\n")
                builder.append("<accuracy>").append(location.accuracy).append("</accuracy>\n")
                builder.append("<speed>").append(location.speed).append("</speed>\n")
                builder.append("<bearing>").append(location.bearing).append("</bearing>\n")
                builder.append("<bearingAccuracyDegrees>")
                    .append(location.bearingAccuracyDegrees)
                    .append("</bearingAccuracyDegrees>\n")
                builder.append("<time>").append(timestamp).append("</time>\n")
                builder.append("</trkpt>\n")
            }
        }
        builder.append(GPX_FOOTER)
        return builder.toString()
    }

    override suspend fun storeLocation(location: SoundscapeLocation) {
        bufferMutex.withLock {
            buffer.add(location)
            if (buffer.size > maxBufferSize)
                buffer.drop(1)
        }
    }

    companion object {
        private const val GPX_HEADER =
            "<?xml version='1.0' encoding='utf-8'?>\n" +
            "<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" version=\"1.0\" creator=\"Soundscape\">\n" +
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
