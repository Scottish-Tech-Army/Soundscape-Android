package org.scottishtecharmy.soundscape.audio

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

/**
 * Stub iOS audio engine. Provides basic TTS via AVSpeechSynthesizer.
 * Spatial audio and beacon support will require integrating the C++ audio engine.
 */
class IosAudioEngine : AudioEngine {

    private val synthesizer = AVSpeechSynthesizer()
    private var nextHandle = 1L
    private var beaconMuted = false

    override fun createBeacon(location: LngLatAlt, headingOnly: Boolean): Long {
        // Beacon audio not yet implemented on iOS
        return nextHandle++
    }

    override fun destroyBeacon(beaconHandle: Long) {
        // No-op stub
    }

    override fun toggleBeaconMute(): Boolean {
        beaconMuted = !beaconMuted
        return beaconMuted
    }

    override fun createTextToSpeech(
        text: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double
    ): Long {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        synthesizer.speakUtterance(utterance)
        return nextHandle++
    }

    override fun createEarcon(
        asset: String,
        type: AudioType,
        latitude: Double,
        longitude: Double,
        heading: Double
    ): Long {
        // Earcon audio not yet implemented on iOS
        return nextHandle++
    }

    override fun clearTextToSpeechQueue() {
        @Suppress("CAST_NEVER_SUCCEEDS")
        synthesizer.stopSpeakingAtBoundary(0L as AVSpeechBoundary)
    }

    override fun getQueueDepth(): Long = 0

    override fun isHandleActive(handle: Long): Boolean = false

    override fun updateGeometry(
        listenerLatitude: Double,
        listenerLongitude: Double,
        listenerHeading: Double?,
        focusGained: Boolean,
        duckingAllowed: Boolean,
        proximityNear: Double
    ) {
        // Spatial audio geometry not yet implemented on iOS
    }

    override fun setBeaconType(beaconType: String) {
        // Stub
    }

    override fun getListOfBeaconTypes(): Array<String> {
        return arrayOf("Original", "Current", "Tactile", "Flare", "Shimmer",
            "Ping", "Drop", "Signal", "Signal Slow", "Signal Very Slow",
            "Mallet", "Mallet Slow", "Mallet Very Slow")
    }

    override fun setSpeechLanguage(language: String): Boolean {
        // AVSpeechSynthesizer picks language automatically from utterance
        return true
    }

    override fun onAllBeaconsCleared() {
        // No-op stub
    }

    override fun setHrtfEnabled(enabled: Boolean) {
        // Stub
    }
}
