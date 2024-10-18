package org.scottishtecharmy.soundscape.audio

import android.content.SharedPreferences
import android.speech.tts.Voice
import java.util.Locale

interface AudioEngine {
    fun createBeacon(latitude: Double, longitude: Double) : Long
    fun destroyBeacon(beaconHandle : Long)
    fun createTextToSpeech(text: String, latitude: Double = Double.NaN, longitude: Double = Double.NaN) : Long
    fun createEarcon(asset: String, latitude: Double = Double.NaN, longitude: Double = Double.NaN) : Long
    fun clearTextToSpeechQueue()
    fun updateGeometry(listenerLatitude: Double, listenerLongitude: Double, listenerHeading: Double)
    fun setBeaconType(beaconType: String)
    fun getListOfBeaconTypes() : Array<String>
    fun getAvailableSpeechLanguages() : Set<Locale>
    fun getAvailableSpeechVoices() : Set<Voice>
    fun setSpeechLanguage(language : String) : Boolean
    fun updateSpeech(sharedPreferences: SharedPreferences): Boolean
    fun updateBeaconType(sharedPreferences: SharedPreferences): Boolean
}