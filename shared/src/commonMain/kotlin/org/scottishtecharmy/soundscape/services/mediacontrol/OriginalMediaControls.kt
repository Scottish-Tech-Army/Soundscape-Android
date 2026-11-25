package org.scottishtecharmy.soundscape.services.mediacontrol

import org.scottishtecharmy.soundscape.utils.SharedAnalytics

class OriginalMediaControls(private val service: MediaControllableService) : MediaControlTarget {

    override fun onPlayPause(): Boolean {
        logMediaButton("playPause", "original")
        service.routeMute()
        return true
    }

    /**
     * Skipping waypoints still comes first on both buttons while a route is playing - that is
     * what they are for mid-route, and the route player answers for itself.
     *
     * With no route, My Location gives its place up to What's Around Me, which comes off
     * Previous: that button makes Soundscape quieter a step at a time instead, the same as it
     * does in audio menu mode, so the one press worth finding in a hurry is the same press in
     * both. My Location is still a button on the home screen and a word to Siri or Gemini.
     */
    override fun onNext(): Boolean {
        logMediaButton("next", "original")
        if (!service.routeSkipNext()) {
            service.whatsAroundMe()
        }
        return true
    }

    override fun onPrevious(): Boolean {
        logMediaButton("previous", "original")
        if (!service.routeSkipPrevious()) {
            service.cycleCalloutDetailAndSay()
        }
        return true
    }
}

internal fun logMediaButton(button: String, mode: String) {
    SharedAnalytics.logEvent("mediaButton", mapOf("button" to button, "mode" to mode))
}
