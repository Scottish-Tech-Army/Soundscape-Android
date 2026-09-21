package org.scottishtecharmy.soundscape.services.mediacontrol

interface MediaControlTarget {
    fun onPlayPause(): Boolean
    fun onNext(): Boolean
    fun onPrevious(): Boolean
}
