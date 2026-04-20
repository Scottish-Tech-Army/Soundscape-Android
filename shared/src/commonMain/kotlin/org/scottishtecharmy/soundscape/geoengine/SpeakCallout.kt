package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.audio.AudioEngine
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings

private const val EARCON_MODE_ENTER = "file:///android_asset/earcons/mode_enter.wav"
private const val EARCON_MODE_EXIT = "file:///android_asset/earcons/mode_exit.wav"

/**
 * Shared implementation of speakCallout used by both Android and iOS service layers.
 * Renders a TrackedCallout through the audio engine with optional mode earcons
 * and distance/heading annotations.
 *
 * @param callout The callout to speak
 * @param addModeEarcon Whether to play mode enter/exit earcons
 * @param audioEngine The audio engine to play through
 * @param lastGeometry The most recent user geometry (for distance/heading calculations)
 * @param ruler A CheapRuler instance for distance calculations
 * @return The handle of the last queued TTS item, or 0 if nothing was queued
 */
fun speakCalloutCommon(
    callout: TrackedCallout?,
    addModeEarcon: Boolean,
    audioEngine: AudioEngine,
    lastGeometry: UserGeometry?,
    ruler: CheapRuler,
): Long {
    if (callout == null) return 0L

    var lastHandle = 0L
    if (addModeEarcon) {
        lastHandle = audioEngine.createEarcon(EARCON_MODE_ENTER, AudioType.STANDARD)
    }

    for (result in callout.positionedStrings) {
        val resultLocation = result.location
        val resultEarcon = result.earcon

        if (resultLocation == null) {
            var type = result.type
            if (type == AudioType.LOCALIZED) type = AudioType.STANDARD
            if (resultEarcon != null)
                audioEngine.createEarcon(resultEarcon, type, 0.0, 0.0, result.heading ?: 0.0)
            lastHandle = audioEngine.createTextToSpeech(result.text, type, 0.0, 0.0, result.heading ?: 0.0)
        } else {
            if (resultEarcon != null) {
                audioEngine.createEarcon(
                    resultEarcon, result.type,
                    resultLocation.latitude, resultLocation.longitude,
                    result.heading ?: 0.0
                )
            }
            val text = if (result.addDistanceAndHeading) {
                lastGeometry?.location?.let { location ->
                    @Suppress("NAME_SHADOWING")
                    var ruler = ruler
                    if (ruler.needsReplacing(location.latitude))
                        ruler = CheapRuler(location.latitude)
                    val distance = ruler.distance(location, resultLocation)
                    val heading = ruler.bearing(location, resultLocation)
                    result.text + ", " + formatDistanceAndDirection(
                        distance, heading,
                        ComposeLocalizedStrings(),
                        lastGeometry.heading()
                    )
                } ?: result.text
            } else {
                result.text
            }
            lastHandle = audioEngine.createTextToSpeech(
                text, result.type,
                resultLocation.latitude, resultLocation.longitude,
                result.heading ?: 0.0
            )
        }
    }

    if (addModeEarcon) {
        audioEngine.createEarcon(EARCON_MODE_EXIT, AudioType.STANDARD)
    }

    // Update callout tracking so it doesn't repeat immediately
    callout.calloutHistory?.add(callout)
    callout.locationFilter?.update(callout.userGeometry)

    return lastHandle
}
