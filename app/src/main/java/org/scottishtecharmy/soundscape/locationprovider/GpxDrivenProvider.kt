package org.scottishtecharmy.soundscape.locationprovider

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.GpxData
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.GpxLocationStream
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.GpxTrackPoint
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.parseGpx
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.platform.parseGpxTimestamp
import java.io.InputStream

/**
 * Replays a recorded GPX through the app as though the fixes were arriving from the receiver.
 *
 * The point of the exercise is that a user can send in a recording of something going wrong and
 * we can watch the app do the same thing, so this feeds the geoengine what the recording holds
 * rather than a tidied-up version of it: the recorded accuracy, speed, bearing and timing, at the
 * intervals they actually arrived at, published onto both location flows by
 * [GpxReplayLocationProvider]. Anything this synthesizes instead of replaying is a way for the
 * replay to disagree with the journey.
 *
 * Enabled by developers only - put the file in `assets/` and name it in [REPLAY_ASSET_PATH].
 */
class GpxDrivenProvider {

    /** Replaced in [start] once the recording says which stream it holds. */
    var locationProvider: LocationProvider =
        GpxReplayLocationProvider(GpxLocationStream.FILTERED)
        private set
    var directionProvider = DirectionProvider()
    var audioEngine: NativeAudioEngine? = null

    /**
     * Multiplies the recorded pace: 1.0 replays an hour's journey over an hour, 10.0 does it in
     * six minutes. Fast-forwarding changes what the app does - the geoengine's callout windows
     * and the stationary detector both work in wall-clock time - so a replay being chased for a
     * discrepancy wants this left alone.
     */
    var playbackSpeed = 1.0

    private var parsedGpx: GpxData? = null
    private val coroutineScope = CoroutineScope(Job())

    fun start(context: Context, assetPath: String = REPLAY_ASSET_PATH) {
        if (assetPath.isBlank()) {
            Log.e(TAG, "No GPX asset named - set GpxDrivenProvider.REPLAY_ASSET_PATH")
            return
        }
        parseGpxStream(context.assets.open(assetPath))

        val gpx = parsedGpx ?: return
        val points = gpx.tracks.firstOrNull()
            ?.trackSegments?.firstOrNull()
            ?.trackPoints
            .orEmpty()
        if (points.isEmpty()) {
            Log.e(TAG, "$assetPath holds no track points")
            return
        }

        // Rebuilt for the recording in hand, because whether the filter runs at all depends on
        // which stream the file holds - see GpxReplayLocationProvider.
        val provider = GpxReplayLocationProvider(gpx.locationStream)
        locationProvider = provider
        Log.d(
            TAG,
            "Replaying $assetPath: ${points.size} points, recorder v${gpx.recorderVersion}, " +
                "${gpx.locationStream} stream"
        )

        coroutineScope.launch {
            // Track points are replayed exactly as recorded, never interpolated between. A
            // position the receiver never reported is a position the geoengine never saw, and
            // inventing them at a constant walking pace turned every recording - a train journey
            // included - into a walk.
            points.forEachIndexed { index, point ->
                val location = LngLatAlt(point.longitude, point.latitude)
                val heading = point.bearing?.toDouble()
                    ?: headingToNextPoint(points, index)
                    ?: 0.0

                // The recording carries no phone or head orientation, so the direction of travel
                // stands in for it. It is a stand-in, not a recording: a replay cannot show what
                // the user was pointing at while standing still.
                directionProvider.mutableOrientationFlow.value = DeviceDirection(
                    attitude = FloatArray(4),
                    headingDegrees = heading.toFloat(),
                    headingAccuracyDegrees = point.bearingAccuracyDegrees ?: 0.0F,
                    elapsedRealtimeNanos = 1000000
                )

                provider.updateLocation(point.toSoundscapeLocation())

                // The filtered position, so the audio engine follows what the geoengine acts on.
                provider.filteredLocationFlow.value?.let { filtered ->
                    audioEngine?.updateGeometry(
                        filtered.latitude,
                        filtered.longitude,
                        heading,
                        focusGained = true,
                        duckingAllowed = false,
                        15.0
                    )
                }

                delay(waitBeforeNextPoint(points, index))
            }
            Log.d(TAG, "Replay of $assetPath finished")
        }
    }

    /**
     * How long to hold this fix before publishing the next, from the recorded timestamps.
     *
     * Capped so that a recording with a gap in it - the app backgrounded, the phone asleep -
     * doesn't stall the replay for as long as the user was away, and floored at zero so that
     * points sharing a timestamp (the duplicate fixes iOS reports on start) don't run backwards.
     */
    private fun waitBeforeNextPoint(points: List<GpxTrackPoint>, index: Int): Long {
        val thisTime = parseGpxTimestamp(points[index].time)
        val nextTime = points.getOrNull(index + 1)?.let { parseGpxTimestamp(it.time) }
        if (thisTime == null || nextTime == null) return DEFAULT_INTERVAL_MS

        val interval = ((nextTime - thisTime) / playbackSpeed).toLong()
        return interval.coerceIn(0L, MAX_INTERVAL_MS)
    }

    private fun headingToNextPoint(points: List<GpxTrackPoint>, index: Int): Double? {
        val next = points.getOrNull(index + 1) ?: return null
        return bearingFromTwoPoints(
            LngLatAlt(points[index].longitude, points[index].latitude),
            LngLatAlt(next.longitude, next.latitude)
        )
    }

    fun parseGpxStream(input: InputStream) {
        Log.d(TAG, "Parsing GPX file")

        try {
            parsedGpx = parseGpx(input.bufferedReader().readText())
            if (parsedGpx == null) {
                Log.e(TAG, "Error parsing GPX file")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception whilst parsing GPX file: ${e.message}")
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "GpxDrivenProvider"

        /** Name of the file under `assets/` to replay. Set this to use the provider. */
        const val REPLAY_ASSET_PATH = ""

        /** Used between points where the recording carries no usable timestamps. */
        private const val DEFAULT_INTERVAL_MS = 1000L

        /** See [waitBeforeNextPoint]. */
        private const val MAX_INTERVAL_MS = 10_000L
    }
}
