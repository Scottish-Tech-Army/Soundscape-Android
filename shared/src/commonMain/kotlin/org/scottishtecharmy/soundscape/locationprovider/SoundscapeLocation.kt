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
    // Wall-clock time (epoch millis) this fix was captured at - 0 means unknown (e.g. a
    // synthesized/debug location that was never a real fix). Used by GeoEngine to dead-reckon
    // the position forward from this fix using its speed/bearing when audio geometry is updated
    // between location updates - see GeoEngine.createUserGeometry.
    val timestampMilliseconds: Long = 0L,
)
