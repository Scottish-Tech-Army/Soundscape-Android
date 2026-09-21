package org.scottishtecharmy.soundscape

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.scottishtecharmy.soundscape.audio.AudioEngine
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.geoengine.UserGeometry.Companion.VEHICLE_SPEED_THRESHOLD_MPS
import org.scottishtecharmy.soundscape.geoengine.utils.extrapolatePositionForward
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * Audible demonstration of [extrapolatePositionForward]: a fixed 3D audio source reads out a
 * long-ish sentence while the listener is walked/driven past it. Location fixes only land once a
 * second (like a real GPS), so between fixes the listener's position fed to the audio engine is
 * dead-reckoned forward from speed/heading. Above [VEHICLE_SPEED_THRESHOLD_MPS] that keeps the
 * beacon's azimuth sweeping smoothly past the listener; at walking speed extrapolation is a no-op
 * (see commit "Don't extrapolate location when travelling at walking speed") so the azimuth steps
 * once per fix instead.
 *
 * There's no audio capture in this harness, so run it and listen - it doesn't assert anything.
 */
class LocationExtrapolationTest {

    private val longSentence =
        "You're passing Marchmont Street on your right, followed by a pedestrian crossing " +
            "and then a small parade of shops including a pharmacy and a bakery."

    private fun initializeAudioEngine(): NativeAudioEngine {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audioEngine = NativeAudioEngine()
        audioEngine.initialize(context)
        return audioEngine
    }

    private fun tidyUp(audioEngine: NativeAudioEngine) {
        audioEngine.destroy()
    }

    /**
     * Walks/drives the listener in a straight line past [sourceLocation], offset to one side so
     * it's passed close by rather than driven straight through. Fresh fixes only land once a
     * second; [extrapolatePositionForward] dead-reckons the listener forward from the last fix in
     * between, at [speedMps] and heading due north.
     */
    private fun movePastLocation(
        audioEngine: AudioEngine,
        sourceLocation: LngLatAlt,
        speedMps: Double,
        durationMilliseconds: Long,
    ) {
        val tickMilliseconds = 50L
        val fixIntervalMilliseconds = 1000L
        val travelHeading = 0.0
        val lateralOffsetMetres = 5.0
        val ruler = sourceLocation.createCheapRuler()

        Log.d(
            TAG,
            "Moving past source at $speedMps m/s - extrapolation " +
                if (speedMps > VEHICLE_SPEED_THRESHOLD_MPS) "active" else "inactive (walking speed)"
        )

        // Point on the route directly abeam the source, so the source is passed rather than
        // walked/driven straight over.
        val closestApproach = ruler.destination(sourceLocation, lateralOffsetMetres, travelHeading + 90.0)
        val totalDistanceMetres = speedMps * (durationMilliseconds / 1000.0)
        val start = ruler.destination(closestApproach, totalDistanceMetres / 2.0, travelHeading + 180.0)

        var lastFixLocation = start
        var lastFixTimestampMilliseconds = System.currentTimeMillis()
        var elapsedMilliseconds = 0L
        while (elapsedMilliseconds <= durationMilliseconds) {
            val nowMilliseconds = System.currentTimeMillis()
            val travelledMetres = speedMps * (elapsedMilliseconds / 1000.0)
            val trueLocation = ruler.destination(start, travelledMetres, travelHeading)

            if (nowMilliseconds - lastFixTimestampMilliseconds >= fixIntervalMilliseconds) {
                lastFixLocation = trueLocation
                lastFixTimestampMilliseconds = nowMilliseconds
            }

            val reportedLocation = extrapolatePositionForward(
                location = lastFixLocation,
                ruler = ruler,
                speed = speedMps,
                heading = travelHeading,
                fixTimestampMilliseconds = lastFixTimestampMilliseconds,
                nowMilliseconds = nowMilliseconds,
            )

            audioEngine.updateGeometry(
                listenerLatitude = reportedLocation.latitude,
                listenerLongitude = reportedLocation.longitude,
                listenerHeading = travelHeading,
                focusGained = true,
                duckingAllowed = false,
                proximityNear = 15.0,
            )

            Thread.sleep(tickMilliseconds)
            elapsedMilliseconds += tickMilliseconds
        }
    }

    @Test
    fun drivingQuicklyPastLocationExtrapolates() {
        val audioEngine = initializeAudioEngine()

        val sourceLocation = LngLatAlt(0.0, 0.0)
        audioEngine.createTextToSpeech(
            longSentence,
            AudioType.LOCALIZED,
            sourceLocation.latitude,
            sourceLocation.longitude,
        )
        // ~15 m/s (~34mph), comfortably above VEHICLE_SPEED_THRESHOLD_MPS, so the listener's
        // position is dead-reckoned forward between the once-a-second fixes and the beacon's
        // azimuth sweeps past smoothly rather than stepping.
        movePastLocation(audioEngine, sourceLocation, speedMps = 15.0, durationMilliseconds = 8000)

        tidyUp(audioEngine)
    }

    @Test
    fun walkingPastLocationDoesNotExtrapolate() {
        val audioEngine = initializeAudioEngine()

        val sourceLocation = LngLatAlt(0.0, 0.0)
        audioEngine.createTextToSpeech(
            longSentence,
            AudioType.LOCALIZED,
            sourceLocation.latitude,
            sourceLocation.longitude,
        )
        // ~1.4 m/s walking pace, below VEHICLE_SPEED_THRESHOLD_MPS, so extrapolatePositionForward
        // returns each fix unchanged and the beacon's azimuth only updates once a second.
        movePastLocation(audioEngine, sourceLocation, speedMps = 1.4, durationMilliseconds = 8000)

        tidyUp(audioEngine)
    }

    companion object {
        const val TAG: String = "LocationExtrapolationTest"
    }
}
