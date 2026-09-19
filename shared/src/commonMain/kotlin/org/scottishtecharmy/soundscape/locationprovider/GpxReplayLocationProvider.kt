package org.scottishtecharmy.soundscape.locationprovider

import org.scottishtecharmy.soundscape.geoengine.filters.KalmanLocationFilter
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.GpxLocationStream

/**
 * Publishes fixes read back from a recording onto the same two flows a live provider drives.
 *
 * The geoengine reads both streams and treats them differently on purpose (see
 * GeoEngine.startMonitoringLocation), so a replay that publishes one position to both is not
 * replaying the journey - it is handing StationaryDetector and MapMatchFilter a smoothed track
 * they would never have been given live. Given a [GpxLocationStream.RAW] recording this rebuilds
 * the filtered stream with the same [KalmanLocationFilter] the live providers use, so both
 * components see what they saw on the day.
 *
 * A [GpxLocationStream.FILTERED] recording - anything written by recorder v1, or by another app -
 * has only the one stream left in it. Both flows then carry the recorded position unchanged:
 * filtering it again would smooth an already-smoothed track, and the raw fixes are simply gone.
 * That replay is as faithful as such a file allows, which is not entirely.
 */
class GpxReplayLocationProvider(
    private val locationStream: GpxLocationStream
) : LocationProvider() {

    private val filter = KalmanLocationFilter()

    /** Driven by whatever is reading the recording, not by a system service. */
    override fun start(accuracy: Accuracy) {}

    override fun destroy() {}

    override fun updateLocation(newLocation: SoundscapeLocation) {
        mutableLocationFlow.value = newLocation
        mutableFilteredLocationFlow.value = when (locationStream) {
            GpxLocationStream.RAW -> filter.filterPosition(newLocation)
            GpxLocationStream.FILTERED -> newLocation
        }
    }
}
