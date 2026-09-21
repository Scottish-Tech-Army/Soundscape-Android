package org.scottishtecharmy.soundscape.services.mediacontrol

class OriginalMediaControls(private val service: MediaControllableService) : MediaControlTarget {

    override fun onPlayPause(): Boolean {
        service.routeMute()
        return true
    }

    override fun onNext(): Boolean {
        if (!service.routeSkipNext()) {
            service.myLocation()
        }
        return true
    }

    override fun onPrevious(): Boolean {
        if (!service.routeSkipPrevious()) {
            service.whatsAroundMe()
        }
        return true
    }
}
