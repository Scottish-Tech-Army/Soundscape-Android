package org.scottishtecharmy.soundscape.geoengine.utils.gpx

data class GpxData(
    val metadata: GpxMetadata = GpxMetadata(),
    val waypoints: List<GpxWaypoint> = emptyList(),
    val routes: List<GpxRoute> = emptyList(),
    val tracks: List<GpxTrack> = emptyList(),
    /**
     * Which revision of [org.scottishtecharmy.soundscape.geoengine.utils.GpxRecorder] wrote this
     * file, or null for anything that didn't: a GPX exported from another app, one written by
     * hand, or one recorded before the recorder started stamping a version.
     */
    val recorderVersion: Int? = null,
    /** Which of the two location streams [trackPoints] hold - see [GpxLocationStream]. */
    val locationStream: GpxLocationStream = GpxLocationStream.FILTERED,
)

/**
 * Which of [org.scottishtecharmy.soundscape.locationprovider.LocationProvider]'s two streams a
 * recording's track points were taken from.
 *
 * The geoengine runs on both at once and they are not interchangeable: the grid, callouts and
 * geometry follow the Kalman-filtered position, while StationaryDetector and MapMatchFilter are
 * given the unfiltered one on purpose, because the filter smooths away the very jitter the
 * detector measures (see GeoEngine.startMonitoringLocation). A replay can only put a fix in front
 * of the same component that saw it live if the file says which stream it came from.
 *
 * [RAW] recordings carry the unfiltered fix and the replay derives the filtered position from it,
 * reproducing both streams. [FILTERED] is what recorder v1 wrote - already-smoothed positions,
 * with the unfiltered stream lost - so a replay of one can only offer the same smoothed position
 * to every component, StationaryDetector and MapMatchFilter included. It stays the default
 * because a file with no marker is a v1 recording.
 */
enum class GpxLocationStream {
    RAW,
    FILTERED,
    ;

    companion object {
        /** Parses the `locationStream` attribute, treating anything unrecognised as [FILTERED]. */
        fun fromAttribute(value: String?): GpxLocationStream =
            if (value.equals("raw", ignoreCase = true)) RAW else FILTERED
    }
}

data class GpxMetadata(
    val name: String = "",
    val desc: String = "",
)

data class GpxWaypoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val name: String = "",
    val desc: String = "",
    val ele: Double? = null,
    val time: String? = null,
)

data class GpxRoute(
    val routeName: String = "",
    val routePoints: List<GpxWaypoint> = emptyList(),
)

data class GpxTrack(
    val trackName: String = "",
    val trackSegments: List<GpxTrackSegment> = emptyList(),
)

data class GpxTrackSegment(
    val trackPoints: List<GpxTrackPoint> = emptyList(),
)

data class GpxTrackPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val ele: Double? = null,
    val time: String? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val accuracy: Float? = null,
    /**
     * How far [bearing] can be trusted, in degrees. Null where the recording carries no such
     * figure, which is both a device that doesn't report one and a GPX written by anything other
     * than [org.scottishtecharmy.soundscape.geoengine.utils.GpxRecorder] - the distinction matters
     * because a bearing with no accuracy beside it says nothing about whether the user is moving,
     * only which way they were pointed. See StationaryDetector.
     */
    val bearingAccuracyDegrees: Float? = null,
)
