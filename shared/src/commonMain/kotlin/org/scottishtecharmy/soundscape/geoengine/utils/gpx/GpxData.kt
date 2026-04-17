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
)
