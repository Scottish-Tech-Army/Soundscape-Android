package org.scottishtecharmy.soundscape.services.mediacontrol

class OriginalMediaControls(private val service: MediaControllableService) : MediaControlTarget {

    override fun onPlayPause(): Boolean {
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
        if (!service.routeSkipNext()) {
            service.whatsAroundMe()
        }
        return true
    }

    override fun onPrevious(): Boolean {
        if (!service.routeSkipPrevious()) {
            service.cycleCalloutDetailAndSay()
        }
        return true
    }
}
