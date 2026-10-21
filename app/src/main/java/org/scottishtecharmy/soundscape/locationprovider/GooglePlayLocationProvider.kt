package org.scottishtecharmy.soundscape.locationprovider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.scottishtecharmy.soundscape.geoengine.filters.KalmanLocationFilter

class GooglePlayLocationProvider(context: Context) :
    LocationProvider() {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback

    private val filter = KalmanLocationFilter()

    /**
     * Publishes a fix on both flows. Every fix is published, however inaccurate: dropping the ones
     * that aren't good enough to act on is GeoEngine's job (see LocationProvider.isLocationUsable),
     * so that the GPX recorder and the map still see what the receiver actually reported.
     */
    private fun publishLocation(location: Location) {
        val soundscapeLocation = location.toSoundscapeLocation()
        mutableLocationFlow.value = soundscapeLocation
        mutableFilteredLocationFlow.value = filter.filterPosition(soundscapeLocation)
    }

    init {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        publishLocation(location)
                    }
                }
                .addOnFailureListener { _: Exception ->
                }
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    publishLocation(location)
                }
            }
        }
    }

    override fun destroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    override fun start(accuracy: Accuracy) {

        // Both ask for GPS: Balanced saves its power through the longer interval, not by
        // accepting PRIORITY_BALANCED_POWER_ACCURACY's Wi-Fi and cell fixes. Those are 40-100m
        // out, which is the whole of wake on leave's 40m, so they would wake it while the user
        // sat still, or not as they walked off - and with no Wi-Fi or cell, never arrive at all.
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                accuracy.updateInterval.inWholeMilliseconds
            ).apply {
                setMinUpdateDistanceMeters(accuracy.minimumDistanceM)
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(true)
            }.build(),
            locationCallback,
            Looper.getMainLooper(),
        )
    }
}
