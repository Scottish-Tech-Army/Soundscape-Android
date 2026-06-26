package org.scottishtecharmy.soundscape

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.scottishtecharmy.soundscape.audio.AudioEngine
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class AudioEngineTest {

    @Test
    fun beaconList() {
        val audioEngine = NativeAudioEngine()
        val beaconTypes = audioEngine.getListOfBeaconTypes()
        Assert.assertEquals(beaconTypes.size, 13)
        Assert.assertEquals("Original", beaconTypes[0])
        Assert.assertEquals("Current", beaconTypes[1])
        Assert.assertEquals("Tactile", beaconTypes[2])
        Assert.assertEquals("Flare", beaconTypes[3])
        Assert.assertEquals("Shimmer", beaconTypes[4])
        Assert.assertEquals("Ping", beaconTypes[5])
        Assert.assertEquals("Drop", beaconTypes[6])
        Assert.assertEquals("Signal", beaconTypes[7])
        Assert.assertEquals("Signal Slow", beaconTypes[8])
        Assert.assertEquals("Signal Very Slow", beaconTypes[9])
        Assert.assertEquals( "Mallet", beaconTypes[10])
        Assert.assertEquals( "Mallet Slow", beaconTypes[11])
        Assert.assertEquals( "Mallet Very Slow", beaconTypes[12])
    }

    private fun moveListener(audioEngine: AudioEngine, duration: Int, noMovement: Boolean = false) {
        val delayMilliseconds: Long = 50
        var orientation = 0.0
        val delta = 360.0 / (duration / delayMilliseconds)
        var time: Long = 0
        while(time <= duration) {
            audioEngine.updateGeometry(
                listenerLatitude = 0.0,
                listenerLongitude = 0.0,
                listenerHeading = orientation,
                focusGained = true,
                duckingAllowed = false,
                proximityNear = 15.0)
            Thread.sleep(delayMilliseconds)
            time += delayMilliseconds
            if(!noMovement)
                orientation += delta
        }
        Log.d(TAG, "Time $time, Orientation $orientation")
    }

    private fun initializeAudioEngine() : NativeAudioEngine {
        // Use the instrumentation targetContext for the assets etc.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audioEngine = NativeAudioEngine()
        audioEngine.initialize(context)

        return audioEngine
    }

    private fun tidyUp(audioEngine : NativeAudioEngine) {
        audioEngine.destroy()
    }

    @Test
    fun soundBeacon() {
        val audioEngine = initializeAudioEngine()

        val beacon = audioEngine.createBeacon(LngLatAlt(1.0, 0.0), true)
        moveListener(audioEngine, 8000)
        audioEngine.destroyBeacon(beacon)

//        audioEngine.createTextToSpeech("Beacon here!", AudioType.LOCALIZED)
//        moveListener(audioEngine, 4000)
//
//        val beacon3 = audioEngine.createBeacon(LngLatAlt(1.0, 0.0), true)
//        moveListener(audioEngine, 4000)
//        audioEngine.destroyBeacon(beacon3)

        tidyUp(audioEngine)
    }

    @Test
    fun allBeacons() {
        val audioEngine = initializeAudioEngine()
        val beaconTypes = audioEngine.getListOfBeaconTypes()

        // Play each of the beacon types, with the orientation of the listener rotating
        // a full 360 degrees
        for(beaconType in beaconTypes) {
            Log.d(TAG, "Test beacon type $beaconType")
            audioEngine.setBeaconType(beaconType)

            val beacon = audioEngine.createBeacon(LngLatAlt(1.0, 0.0), true)
            moveListener(audioEngine, 6000)
            audioEngine.destroyBeacon(beacon)
        }

        tidyUp(audioEngine)
    }

    @Test
    fun queuedSpeech() {
        val audioEngine = initializeAudioEngine()

        audioEngine.createTextToSpeech("First.", AudioType.STANDARD)
        audioEngine.createTextToSpeech("Second.", AudioType.STANDARD)
        audioEngine.createTextToSpeech("Third.", AudioType.STANDARD)
        moveListener(audioEngine, 6000)

        tidyUp(audioEngine)
    }

    @Test
    fun earcon() {
        val audioEngine = initializeAudioEngine()

        audioEngine.createEarcon(NativeAudioEngine.EARCON_CALIBRATION_IN_PROGRESS, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_CALIBRATION_SUCCESS, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_CALLOUTS_ON, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_CALLOUTS_OFF, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_CONNECTION_SUCCESS, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_LOW_CONFIDENCE, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_MODE_ENTER, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_MODE_EXIT, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_OFFLINE, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_ONLINE, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_SENSE_LOCATION, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_SENSE_MOBILITY, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_SENSE_POI, AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_SENSE_SAFETY, AudioType.STANDARD)

        moveListener(audioEngine, 20000)
        tidyUp(audioEngine)
    }

    @Test
    fun textAndEarcon() {
        val audioEngine = initializeAudioEngine()
        audioEngine.createTextToSpeech("Text with ", AudioType.STANDARD)
        audioEngine.createEarcon(NativeAudioEngine.EARCON_OFFLINE, AudioType.STANDARD)
        audioEngine.createTextToSpeech("in the middle.", AudioType.STANDARD)
        moveListener(audioEngine, 8000)
        tidyUp(audioEngine)
    }

    @Test
    fun textWithShutdownRestart() {
        var audioEngine = initializeAudioEngine()
        audioEngine.createTextToSpeech("Text is playing out to test shutdown.", AudioType.STANDARD)
        moveListener(audioEngine, 1000)
        tidyUp(audioEngine)
        audioEngine = initializeAudioEngine()
        audioEngine.createTextToSpeech("Text is playing out again to test shutdown.", AudioType.STANDARD)
        moveListener(audioEngine, 5000)
        tidyUp(audioEngine)
    }


// This test fails on the GitHub action emulator - more investigation required
//    @Test
//    fun speechCapabilities() {
//        val audioEngine = initializeAudioEngine()
//
//        Log.e(TAG, "Languages: " + audioEngine.getAvailableSpeechLanguages().toString())
//
//        val voices = audioEngine.getAvailableSpeechVoices()
//        Log.e(TAG, "Languages: $voices")
//        for(voice in voices) {
//            if(!voice.isNetworkConnectionRequired) {
//                Log.e(TAG, voice.name + " requires no network")
//            }
//        }
//
//        tidyUp(audioEngine)
//    }

    @Test
    fun earconPosition() {
        val audioEngine = initializeAudioEngine()

        audioEngine.updateGeometry(
            listenerLongitude = 1.0,
            listenerLatitude = 1.0,
            listenerHeading = 0.0,
            focusGained = true,
            duckingAllowed = false,
            proximityNear = 15.0
        )
        audioEngine.createEarcon(NativeAudioEngine.EARCON_SENSE_POI, AudioType.LOCALIZED, 1.0, 2.0)
        Thread.sleep(2000)

        var heading = 0.0
        while(heading < 360.0) {
            audioEngine.createEarcon(
                NativeAudioEngine.EARCON_SENSE_POI,
                AudioType.RELATIVE,
                heading = heading
            )
            moveListener(audioEngine, 2000)

            audioEngine.createEarcon(
                NativeAudioEngine.EARCON_SENSE_POI,
                AudioType.COMPASS,
                heading = heading
            )
            moveListener(audioEngine, 2000)

            heading += 90.0
        }

        tidyUp(audioEngine)
    }

    @Test
    fun textPosition() {
        val audioEngine = initializeAudioEngine()
        audioEngine.createTextToSpeech(
            "Position text to the left",
            AudioType.COMPASS,
            heading = -90.0)
        moveListener(audioEngine, 4000, noMovement = true)
        audioEngine.createTextToSpeech(
            "Position text to the right",
            AudioType.COMPASS,
            heading = 90.0)
        moveListener(audioEngine, 4000, noMovement = true)
        tidyUp(audioEngine)
    }

    companion object {
        const val TAG : String = "AudioTestEngine"
    }
}