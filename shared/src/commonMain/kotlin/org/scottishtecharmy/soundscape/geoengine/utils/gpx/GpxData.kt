package org.scottishtecharmy.soundscape.geoengine.utils.gpx

data class GpxData(
    val metadata: GpxMetadata = GpxMetadata(),
    val waypoints: List<GpxWaypoint> = emptyList(),
    val routes: List<GpxRoute> = emptyList(),
    val tracks: List<GpxTrack> = emptyList(),
)

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
