package org.scottishtecharmy.soundscape.services.mediacontrol

interface MediaControllableService {
    fun routeMute(): Boolean
    fun routeSkipNext(): Boolean
    fun routeSkipPrevious(): Boolean
    fun myLocation()
    fun whatsAroundMe()
}
