package org.scottishtecharmy.soundscape.locationprovider

import org.scottishtecharmy.soundscape.geoengine.filters.KalmanLocationFilter
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.platform.currentTimeMillis

/**
 * Smooths a fix's position, and nothing else.
 *
 * Every producer of [LocationProvider.filteredLocationFlow] goes through here - the two Android
 * providers, the iOS one, and the GPX replay provider - so that "the filtered stream" means one
 * thing across platforms and between a live journey and a replay of the recording made on it.
 * A recording is only worth replaying if the replay puts the same numbers through the same
 * arithmetic, and that stopped being true the moment any caller filtered slightly differently.
 *
 * Only latitude and longitude are touched. Accuracy, speed, bearing and the has* flags describe
 * the fix the receiver reported and carry through untouched; [KalmanLocationFilter] has nothing
 * to say about them.
 */
fun KalmanLocationFilter.filterPosition(location: SoundscapeLocation): SoundscapeLocation {
    val filtered = process(
        LngLatAlt(location.longitude, location.latitude),
        filterTimestamp(location),
        location.accuracy.toDouble()
    )
    return location.copy(
        latitude = filtered.latitude,
        longitude = filtered.longitude
    )
}

/**
 * The time [KalmanLocationFilter] should age its estimate to for this fix.
 *
 * The fix's own capture time, so that a replay driven from recorded timestamps grows the
 * covariance by the same intervals the live journey did. Taking the wall clock at the moment the
 * fix happens to be processed instead would make the filter's output depend on delivery latency,
 * which a recording cannot reproduce.
 *
 * Falls back to the wall clock for a fix that carries no capture time, since
 * [org.scottishtecharmy.soundscape.geoengine.filters.KalmanFilter] reads a zero timestamp as
 * "not yet initialised" and would restart the filter on every such fix.
 */
private fun filterTimestamp(location: SoundscapeLocation): Long =
    if (location.timestampMilliseconds != 0L) location.timestampMilliseconds
    else currentTimeMillis()
