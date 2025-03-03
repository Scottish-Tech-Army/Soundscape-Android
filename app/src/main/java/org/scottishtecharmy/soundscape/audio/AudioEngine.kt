package org.scottishtecharmy.soundscape.audio

import android.content.SharedPreferences
import android.speech.tts.Voice
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import java.util.Locale

interface AudioEngine {
    fun createBeacon(location: LngLatAlt) : Long
    fun destroyBeacon(beaconHandle : Long)
    fun toggleBeaconMute() : Boolean
    fun createTextToSpeech(text: String, type: AudioType, latitude: Double = Double.NaN, longitude: Double = Double.NaN, heading: Double = Double.NaN) : Long
    fun createEarcon(asset: String, type: AudioType, latitude: Double = Double.NaN, longitude: Double = Double.NaN, heading: Double = Double.NaN) : Long
    fun clearTextToSpeechQueue()
    fun getQueueDepth() : Long
    fun updateGeometry(listenerLatitude: Double, listenerLongitude: Double, listenerHeading: Double?)
    fun setBeaconType(beaconType: String)
    fun getListOfBeaconTypes() : Array<String>
    fun getAvailableSpeechLanguages() : Set<Locale>
    fun getAvailableSpeechVoices() : Set<Voice>
    fun setSpeechLanguage(language : String) : Boolean
    fun updateSpeech(sharedPreferences: SharedPreferences): Boolean
    fun updateBeaconType(sharedPreferences: SharedPreferences): Boolean
}