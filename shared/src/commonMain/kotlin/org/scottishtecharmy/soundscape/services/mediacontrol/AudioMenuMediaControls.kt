package org.scottishtecharmy.soundscape.services.mediacontrol

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
