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

// This is how the menu navigation media controls work
class AudioMenuMediaControls(val audioMenu: AudioMenu?) : MediaControlTarget {

    override fun onPlayPause() : Boolean {
        audioMenu?.select()
        return true
    }
    override fun onNext() : Boolean {
        audioMenu?.next()
        return true
    }
    override fun onPrevious() : Boolean {
        audioMenu?.previous()
        return true
    }
}
