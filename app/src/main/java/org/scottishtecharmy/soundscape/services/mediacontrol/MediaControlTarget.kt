package org.scottishtecharmy.soundscape.services.mediacontrol

import org.scottishtecharmy.soundscape.services.SoundscapeService

// An interface that encapsulates the various media control buttons
interface MediaControlTarget {

    fun onPlayPause() : Boolean
    fun onNext() : Boolean
    fun onPrevious() : Boolean
}

// This is how the iOS Soundscape media controls behaved
class OriginalMediaControls(val service: SoundscapeService) : MediaControlTarget {

    override fun onPlayPause() : Boolean {
        service.routeMute()
        return true
    }

    override fun onNext() : Boolean {
        if (!service.routeSkipNext()) {
            // If there's no route playing, callout My Location.
            service.myLocation()
        }
        return true
    }

    override fun onPrevious() : Boolean {
        if(!service.routeSkipPrevious()) {
            // If there's no route playing, callout around me
            service.whatsAroundMe()
        }
        return true
    }
}

// This is how the voice command media controls work
class VoiceCommandMediaControls(val service: SoundscapeService) : MediaControlTarget {

    override fun onPlayPause() : Boolean {
        service.triggerVoiceCommand()
        return true
    }
    override fun onNext() : Boolean { return false }
    override fun onPrevious() : Boolean { return false }
}

// This is how the menu navigation media controls work
class MenuMediaControls(val service: SoundscapeService) : MediaControlTarget {

    override fun onPlayPause() : Boolean {
        return true
    }
    override fun onNext() : Boolean {
        return true
    }
    override fun onPrevious() : Boolean {
        return true
    }
}

