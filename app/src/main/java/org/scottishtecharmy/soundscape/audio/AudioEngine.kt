package com.scottishtecharmy.soundscape.audio

interface AudioEngine {
    fun createBeacon(latitude: Double, longitude: Double) : Long
    fun destroyBeacon(beaconHandle : Long)
    fun createTextToSpeech(latitude: Double, longitude: Double, text: String) : Long
    fun updateGeometry(listenerLatitude: Double, listenerLongitude: Double, listenerHeading: Double)
    fun setBeaconType(beaconType: Int)
}