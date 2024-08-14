package org.scottishtecharmy.soundscape.audio

import android.speech.tts.Voice
import java.util.Locale

interface AudioEngine {
    fun createBeacon(latitude: Double, longitude: Double) : Long
    fun destroyBeacon(beaconHandle : Long)
    fun createTextToSpeech(latitude: Double, longitude: Double, text: String) : Long
    fun updateGeometry(listenerLatitude: Double, listenerLongitude: Double, listenerHeading: Double)
    fun setBeaconType(beaconType: String)
    fun getListOfBeaconTypes() : Array<String>
    fun getAvailableSpeechLanguages() : Set<Locale>
    fun getAvailableSpeechVoices() : Set<Voice>
}