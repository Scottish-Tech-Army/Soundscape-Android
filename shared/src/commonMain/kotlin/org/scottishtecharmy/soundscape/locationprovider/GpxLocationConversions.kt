package org.scottishtecharmy.soundscape.locationprovider

import org.scottishtecharmy.soundscape.geoengine.utils.gpx.GpxTrackPoint
import org.scottishtecharmy.soundscape.platform.parseGpxTimestamp

/**
 * The recorded track point as the fix it was, with a has* flag set for each field the recording
 * actually carried.
 *
 * The counterpart of [org.scottishtecharmy.soundscape.geoengine.utils.GpxRecorder]'s writing, and
 * shared by everything that replays a recording so that a file means one thing wherever it is
 * read. GpxRecorder writes a zero for every field the original fix didn't have, and
 * [org.scottishtecharmy.soundscape.geoengine.utils.gpx.GpxParser] reports a field the file omits
 * entirely as null. Both mean "the receiver didn't report this", and the geoengine has to be told
 * so: a bearing it believes is a bearing it steers audio by.
 */
fun GpxTrackPoint.toSoundscapeLocation(): SoundscapeLocation =
    SoundscapeLocation(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy ?: 0.0f,
        bearing = bearing ?: 0.0f,
        bearingAccuracyDegrees = bearingAccuracyDegrees ?: 0.0f,
        speed = speed ?: 0.0f,
        hasAccuracy = accuracy != null,
        hasBearing = bearing != null,
        hasBearingAccuracy = bearingAccuracyDegrees != null,
        hasSpeed = speed != null,
        // GpxRecorder records no speed accuracy, so a replay has none to offer. GeoEngine's
        // speedFromLocation takes the ungated branch, which is what it does for a live fix whose
        // receiver didn't report one either.
        hasSpeedAccuracy = false,
        timestampMilliseconds = parseGpxTimestamp(time) ?: 0L,
    )
