package org.scottishtecharmy.soundscape

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.scottishtecharmy.soundscape.audio.AudioEngine
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.junit.Assert
import org.junit.Test

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

    private fun moveListener(audioEngine: AudioEngine, duration: Int) {
        val delayMilliseconds: Long = 50
        var orientation = 0.0
        val delta = 360.0 / (duration / delayMilliseconds)
        var time: Long = 0
        while(time <= duration) {
            audioEngine.updateGeometry(0.0, 0.0, orientation)
            Thread.sleep(delayMilliseconds)
            time += delayMilliseconds
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

        val beacon = audioEngine.createBeacon(1.0, 0.0)
        moveListener(audioEngine, 4000)
        audioEngine.destroyBeacon(beacon)

        audioEngine.createTextToSpeech(1.0, 0.0, "Beacon here!")
        moveListener(audioEngine, 4000)

        val beacon3 = audioEngine.createBeacon(1.0, 0.0)
        moveListener(audioEngine, 4000)
        audioEngine.destroyBeacon(beacon3)

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

            val beacon = audioEngine.createBeacon(1.0, 0.0)
            moveListener(audioEngine, 6000)
            audioEngine.destroyBeacon(beacon)
        }

        tidyUp(audioEngine)
    }

    @Test
    fun queuedSpeech() {
        val audioEngine = initializeAudioEngine()

        audioEngine.createTextToSpeech(1.0, 0.0, "First.")
        audioEngine.createTextToSpeech(1.0, 0.0, "Second.")
        audioEngine.createTextToSpeech(1.0, 0.0, "Third.")
        moveListener(audioEngine, 6000)

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

    companion object {
        const val TAG : String = "AudioTestEngine"
        init {
            System.loadLibrary(BuildConfig.FMOD_LIB)
        }
    }
}