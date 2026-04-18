package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout

interface GeoEngineListener {
    fun isAudioEngineBusy(): Boolean
    fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long
    fun updateAudioEngineGeometry(userGeometry: UserGeometry)
    fun tileGridUpdated()
    fun updateStreetPreviewBestChoice(bestChoice: StreetPreviewChoice)
    fun announceStreetPreviewBestChoice(bestChoice: StreetPreviewChoice)
    fun getStreetPreviewChoices(): List<StreetPreviewChoice>
    fun getStreetPreviewBestChoice(): StreetPreviewChoice?
    val menuActive: Boolean
}
