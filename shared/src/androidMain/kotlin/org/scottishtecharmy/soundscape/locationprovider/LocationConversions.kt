package org.scottishtecharmy.soundscape.locationprovider

import android.location.Location

fun Location.toSoundscapeLocation(): SoundscapeLocation {
    return SoundscapeLocation(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        bearing = bearing,
        bearingAccuracyDegrees = if (hasBearingAccuracy()) bearingAccuracyDegrees else 0f,
        speed = speed,
        speedAccuracyMetersPerSecond = if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond else 0f,
        hasAccuracy = hasAccuracy(),
        hasBearing = hasBearing(),
        hasBearingAccuracy = hasBearingAccuracy(),
        hasSpeed = hasSpeed(),
        hasSpeedAccuracy = hasSpeedAccuracy(),
    )
}
