package org.scottishtecharmy.soundscape

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.scottishtecharmy.soundscape.audio.AudioEngine
import com.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.junit.Assert
import org.junit.Test

class AudioEngineTest {

    @Test
    fun beaconList() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val audioEngine = NativeAudioEngine()
        val beaconTypes = audioEngine.getListOfBeaconTypes()
        Assert.assertEquals(beaconTypes.size, 13)
        Assert.assertEquals("Classic", beaconTypes[0])
        Assert.assertEquals("New", beaconTypes[1])
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

    private fun moveListener(audioEngine: AudioEngine, latitude: Double, longitude: Double, orientation: Double) {
        var time = 0
        while(time < 3000) {
            audioEngine.updateGeometry(latitude, longitude, orientation)
            Thread.sleep(100)
            time += 100
        }
    }

    @Test
    fun soundBeacon() {
        // Use the instrumentation targetContext for the assets etc.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        org.fmod.FMOD.init(context)

        val audioEngine = NativeAudioEngine()
        audioEngine.initialize(context)

        val beacon = audioEngine.createBeacon(1.0, 0.0)
        moveListener(audioEngine, 0.0, 0.0, 90.0)
        audioEngine.destroyBeacon(beacon)

        val speech_beacon = audioEngine.createTextToSpeech(1.0, 0.0, "Beacon here!")
        moveListener(audioEngine, 0.0, 0.0, -90.0)

        val beacon3 = audioEngine.createBeacon(1.0, 0.0)
        moveListener(audioEngine, 0.0, 0.0, -180.0)
        audioEngine.destroyBeacon(beacon3)

        audioEngine.destroy()
        org.fmod.FMOD.close()
    }

    companion object {
        init {
            System.loadLibrary(BuildConfig.FMOD_LIB)
        }
    }
}