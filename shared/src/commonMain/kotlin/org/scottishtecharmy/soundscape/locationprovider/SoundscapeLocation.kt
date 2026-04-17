package org.scottishtecharmy.soundscape.locationprovider

data class SoundscapeLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val bearing: Float = 0.0f,
    val bearingAccuracyDegrees: Float = 0.0f,
    val speed: Float = 0.0f,
    val speedAccuracyMetersPerSecond: Float = 0.0f,
    val hasAccuracy: Boolean = false,
    val hasBearing: Boolean = false,
    val hasBearingAccuracy: Boolean = false,
    val hasSpeed: Boolean = false,
    val hasSpeedAccuracy: Boolean = false,
)
