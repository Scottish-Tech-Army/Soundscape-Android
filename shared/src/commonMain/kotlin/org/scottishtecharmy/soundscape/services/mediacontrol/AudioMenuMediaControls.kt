package org.scottishtecharmy.soundscape.services.mediacontrol

class AudioMenuMediaControls(val audioMenu: AudioMenu?) : MediaControlTarget {

    override fun onPlayPause(): Boolean {
        logMediaButton("playPause", "audioMenu")
        audioMenu?.select()
        return true
    }

    override fun onNext(): Boolean {
        logMediaButton("next", "audioMenu")
        audioMenu?.next()
        return true
    }

    /**
     * Not menu navigation: making Soundscape quieter is wanted far more often than stepping
     * backwards through a menu that wraps round anyway, and this way it is one press from
     * wherever the menu happens to be - and the same press as in Original mode.
     */
    override fun onPrevious(): Boolean {
        logMediaButton("previous", "audioMenu")
        audioMenu?.cycleCalloutDetail()
        return true
    }
}
