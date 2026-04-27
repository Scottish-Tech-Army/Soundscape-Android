package org.scottishtecharmy.soundscape.services.mediacontrol

import org.scottishtecharmy.soundscape.services.SoundscapeService

// This is how the voice command media controls work
class VoiceCommandMediaControls(val service: SoundscapeService) : MediaControlTarget {

    override fun onPlayPause() : Boolean {
        service.triggerVoiceCommand()
        return true
    }
    override fun onNext() : Boolean { return false }
    override fun onPrevious() : Boolean { return false }
}
